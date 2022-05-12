package com.ar.autotrade.strategy.swingstock;

import com.ar.autotrade.broker.BrokerService;
import com.ar.sheetdb.core.Db;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class SwingStockStrategy {

    @Autowired
    BrokerService api;

    @Autowired
    Db db;

    public void init(){
        List<SwingStock> list = db.getAll(SwingStock.class);
        System.out.println(list);
        SwingStock inst = SwingStock.builder().buyOn(LocalDate.now()).buyOrderId(123).buyPrice(123.23).expiryAfter(14).modelName("no")
                .predictedOn(LocalDate.now()).probability(69).profitPercentage(2.3).stopLossPercentage(2.3).quantity(10)
                .status(SwingStockStatus.GTT_PLACED).tickName("TCS").build();
        db.add(inst);
        inst.setRow(19); db.update(inst);
    }

    @PostConstruct
    public void run() {
        List<SwingStock> list = db.getAll(SwingStock.class);
        List<SwingStock> workingList = list.stream().filter(x -> {
            if (x.getStatus() == SwingStockStatus.EXPIRED ||
                    x.getStatus() == SwingStockStatus.COMPLETE ||
                    x.getStatus() == SwingStockStatus.STOPLOSS ||
                    x.getStatus() == SwingStockStatus.FAILED) {
                return false;
            }
            return true;
        }).collect(Collectors.toList());

        workingList.forEach(item->{
            if(item.getStatus() ==SwingStockStatus.PREDICTED){
                actionsAfterPredicted(item);
            } else if(item.getStatus()==SwingStockStatus.BUY_PLACED){
                actionsAfterBought(item);
            } else if(item.getStatus()==SwingStockStatus.GTT_PLACED){
                actionsAfterEnd(item);
            } else if(item.getStatus()==SwingStockStatus.SELL_PLACED){
                actionsAfterSell(item);
            }
        });
    }

    private void actionsAfterPredicted(SwingStock item) {
        // broker: put a AMO;
        // gsheet: change status to BUY_PLACED
    }
    private void actionsAfterBought(SwingStock item) {
        // check if buy triggered
            // broker: then place gtt order
            // gsheet: update with buy date, gtt oider id, buy price
            // *gsheet: if buy failed then make status to fail
    }

    private void actionsAfterEnd(SwingStock item) {
        // check if gtt (profit/loss) triggered
            // *gsheet: gttfailed; sellorder failed -> make staus to fail todo(put sell AMO)
            // gsheet: put sell price, date, orderid and update status to COMPLETE/ STOPLOSS
        // check if gtt expired
            // broker: cancel gtt
            // broker: put sell AMO
            // ghseet: status to SELL_PLACED
    }

    private void actionsAfterSell(SwingStock item) {
        // check if sell triggered
            // gsheet: update with sell date, sell oider id, sell price, EXPIRED
            // *gsheet: if sell failed then make status to FAILED
    }
}
