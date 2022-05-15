package com.ar.autotrade.strategy.swingstock;

import com.ar.autotrade.broker.BrokerService;
import com.ar.autotrade.broker.models.Enums;
import com.ar.autotrade.broker.models.GttOrder;
import com.ar.autotrade.broker.models.GttOrderResponse;
import com.ar.autotrade.broker.models.Order;
import com.ar.autotrade.broker.utils.Utils;
import com.ar.sheetdb.core.Db;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class SwingStockStrategy {

    @Autowired
    BrokerService broker;

    @Autowired
    Db db;

    public void init(){
        List<SwingStock> list = db.getAll(SwingStock.class);
        System.out.println(list);
        SwingStock inst = SwingStock.builder().buyOn(LocalDate.now()).buyOrderId("123").buyPrice(123.23).expiryAfter(14).modelName("no")
                .predictedOn(LocalDate.now()).probability(69).profitPercentage(2.3).stopLossPercentage(2.3).quantity(10)
                .status(SwingStockStatus.GTT_PLACED).tickName("TCS").build();
        db.add(inst);
        inst.setRow(19); db.update(inst);
    }

    @PostConstruct
    public void run() throws IOException {
        if(!broker.isStarted()){
            broker.start();
        }
        List<SwingStock> list = db.getAll(SwingStock.class);
        List<SwingStock> workingList = list.stream().filter(x -> {
            if (x.getStatus() == SwingStockStatus.PREDICTED ||
                    x.getStatus() == SwingStockStatus.BUY_PLACED ||
                    x.getStatus() == SwingStockStatus.GTT_PLACED ||
                    x.getStatus() == SwingStockStatus.SELL_PLACED) {
                return true;
            }
            return false;
        }).collect(Collectors.toList());
        for (SwingStock item : workingList) {
            if (item.getStatus() == SwingStockStatus.PREDICTED) {
                actionsAfterPredicted(item);
            } else if (item.getStatus() == SwingStockStatus.BUY_PLACED) {
                actionsAfterBought(item);
            } else if (item.getStatus() == SwingStockStatus.GTT_PLACED) {
                actionsAfterEnd(item);
            } else if (item.getStatus() == SwingStockStatus.SELL_PLACED) {
                actionsAfterSell(item);
            }
        }
    }

    private void actionsAfterPredicted(SwingStock item) throws IOException {
        // broker: put a AMO;
        var amoOrder = Order.builder().orderType(Enums.OrderType.MARKET).quantity(item.getQuantity())
                        .symbol(item.getTickName()).product(Enums.Product.CNC)
                        .transactionType(Enums.TransactionType.BUY)
                                .variety(Enums.Variety.AMO).build();
        amoOrder.validate();
        var response= broker.addOrder(amoOrder);
        // gsheet: change status to BUY_PLACED
        item.setStatus(SwingStockStatus.BUY_PLACED);
        item.setBuyOrderId(response.getOrderId());
        db.update(item);
    }

    private void actionsAfterBought(SwingStock item) throws IOException {
        var buyOrder = broker.getOrder(item.getBuyOrderId());
        var quote= broker.getQuote(item.getTickName());
        // check if buy triggered
        if(buyOrder.getData().getStatus() == Enums.Status.COMPLETE) {
            // broker: then place gtt order
            Float limitPrice = Utils.getGttLimitPriceUsingLtp(item.getBuyPrice(), item.getProfitPercentage(), quote.getLtp());
            Float triggerPrice = Utils.getTriggerPriceFromPrice(limitPrice, Enums.TransactionType.SELL);
            Float stopLossPrice = Utils.getGttStopLossPriceUsingLtp(item.getBuyPrice(), item.getStopLossPercentage(), quote.getLtp());
            Float stopLossTriggerPrice = Utils.getTriggerPriceFromPrice(stopLossPrice, Enums.TransactionType.SELL);
            var gttOrder = GttOrder.builder().quantity(item.getQuantity())
                    .symbol(item.getTickName()).transactionType(Enums.TransactionType.SELL)
                    .gttType(GttOrder.GttType.TWO_LEG)
                    .triggerPrice(triggerPrice).limitPrice(limitPrice)
                    .stopLossTriggerPrice(stopLossTriggerPrice).stopLossLimitPrice(stopLossPrice)
                    .ltp(quote.getLtp()).build();
            var gttOrderId = broker.addGTTOrder(gttOrder);
            // gsheet: update with buy date, gtt oider id, buy price
            item.setStatus(SwingStockStatus.GTT_PLACED);
            item.setBuyOn(buyOrder.getData().getOrderTimestamp().toLocalDate());
            item.setBuyPrice(buyOrder.getData().getAveragePrice().doubleValue());
            item.setSellOrderId(gttOrderId);
            db.update(item);
        } else if(buyOrder.getData().getStatus()== Enums.Status.CANCELLED||
                buyOrder.getData().getStatus()== Enums.Status.REJECTED){
            // *gsheet: if buy failed then make status to fail
            item.setStatus(SwingStockStatus.BUY_FAILED);
            db.update(item);
        }
    }

    private void actionsAfterEnd(SwingStock item) throws IOException {
        // check if gtt (profit/loss) triggered
        var gtt = broker.getGTTOrder(item.getSellOrderId());
        if(gtt.getStatus()== GttOrderResponse.Status.TRIGGERED) {
            // *gsheet: gttfailed; sellorder failed -> make staus to fail todo(put sell AMO)
            // todo check if loss is triggered ot profit is triggered
            // gsheet: put sell price, date, orderid and update status to COMPLETE/ STOPLOSS

            return;
        }
        // gtt failed
        if(gtt.getStatus()== GttOrderResponse.Status.CANCELLED) {
            item.setStatus(SwingStockStatus.GTT_FAILED);
            db.update(item);
            return;
        }
        if(item.getExpiryAfter()==null){
            // never expire
            return;
        }
        if(item.getPredictedOn().plusDays(item.getExpiryAfter()).isBefore(LocalDate.now())) {
            // check if gtt expired
            // broker: cancel gtt
            broker.cancelGTTOrder(item.getSellOrderId());
            // broker: put sell AMO
            var amoOrder = Order.builder().orderType(Enums.OrderType.MARKET).quantity(item.getQuantity())
                    .symbol(item.getTickName()).product(Enums.Product.CNC)
                    .transactionType(Enums.TransactionType.SELL)
                    .variety(Enums.Variety.AMO).build();
            amoOrder.validate();
            var res = broker.addOrder(amoOrder);
            // ghseet: status to SELL_PLACED
            item.setSellOrderId(res.getOrderId());
            item.setStatus(SwingStockStatus.SELL_PLACED);
            db.update(item);
        }
    }

    private void actionsAfterSell(SwingStock item) throws IOException {
        // check if sell triggered
        var orderResponse = broker.getOrder(item.getSellOrderId());
        if(orderResponse.getData().getStatus()== Enums.Status.COMPLETE) {
            // gsheet: update with sell date,  sell price, EXPIRED
            item.setSellPrice(orderResponse.getData().getAveragePrice().doubleValue());
            item.setSellDate(orderResponse.getData().getOrderTimestamp().toLocalDate());
            item.setStatus(SwingStockStatus.COMPLETE);
            // *gsheet: if sell failed then make status to FAILED
        }
    }
}
