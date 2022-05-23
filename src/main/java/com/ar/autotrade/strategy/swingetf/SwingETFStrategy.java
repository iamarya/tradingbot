package com.ar.autotrade.strategy.swingetf;

import com.ar.autotrade.broker.BrokerService;
import com.ar.autotrade.broker.models.Enums;
import com.ar.autotrade.broker.models.GttOrder;
import com.ar.autotrade.broker.models.GttOrderResponse;
import com.ar.autotrade.broker.models.Quote;
import com.ar.autotrade.broker.utils.Utils;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ClearValuesRequest;
import com.google.api.services.sheets.v4.model.ClearValuesResponse;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
//@Component
/*
 * Dont run this during the market hour. Problem is if GTT has triggered and order is still open.
 * Then it is not handeled yet.
 * */
public class SwingETFStrategy {
    private static final String APPLICATION_NAME = "SWING_TRADING_STRATEGY";


    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */

    static Sheets service;
    static String spreadsheetId;

    @Autowired
    BrokerService broker;

    @Value("${gsheet.credential}")
    private String credential;

    private List<SwingPosition> positionList = new ArrayList();

    @PostConstruct
    public void start() throws IOException, GeneralSecurityException {
        if (!broker.isStarted()) {
            broker.start();
            log.debug("Kite started in strategy");
        }
        main();
        System.out.println("END");
        System.exit(0);
    }

    /*
     * Entry point of the strategy. If need to process only one instrument change the filter line
     * */
    public void main() throws IOException, GeneralSecurityException {
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        spreadsheetId = "1MgIdUC3PIZao6M5fm_6wabvacx9kvaTf4dtW9AejXJg";
        service = getSheetService();
        List<SwingStockConfig> configList = getStocksConfig();
        configList = configList.stream().filter(x -> x.enabled)
                //.filter(x-> x.getSymbol().equalsIgnoreCase("PIDILITIND"))
                .collect(Collectors.toList());
        // looop config list
        for (SwingStockConfig config : configList) {
            try {
                TimeUnit.SECONDS.sleep(1);
                processSingle(config);
            } catch (IOException | InterruptedException e) {
                log.error("error processing {}", config, e);
            }
        }
        if (configList != null && configList.size() > 0) {
            SwingPosition total = SwingPosition.builder()
                    .total(Utils.round(positionList.stream().mapToDouble(SwingPosition::getTotal).sum(), 2))
                    .currentProfit(Utils.round(positionList.stream().mapToDouble(SwingPosition::getCurrentProfit).sum(), 2))
                    .realiseProfit(Utils.round(positionList.stream().mapToDouble(SwingPosition::getRealiseProfit).sum(), 2))
                    .build();
            positionList.add(total);
            writePositionList();
            Map<YearMonth, Double> profitMonth = positionList.stream()
                    .filter(x->x.getProfitMoth()!=null)
                    .flatMap(x -> x.getProfitMoth().entrySet().stream())
                    .collect(Collectors.groupingBy(x -> x.getKey(), Collectors.summingDouble(x -> x.getValue())));
            writeProfitList(profitMonth);
        }
    }

    private Sheets getSheetService() throws GeneralSecurityException, IOException {
        JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
        List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);
        NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        GoogleCredentials googleCredentials;
        try (InputStream inputSteam = new FileInputStream(credential)) {
            googleCredentials = GoogleCredentials.fromStream(inputSteam).createScoped(SCOPES);
        }
        return new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, new HttpCredentialsAdapter(googleCredentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private void writeProfitList(Map<YearMonth, Double> profitMonth) throws IOException {
        int counter = 0;
        boolean failed = true;
        while (counter < 5 && failed) {
            try {
                counter = counter + 1;
                final String range = "POSITION" + "!A20:B100";
                ClearValuesRequest requestBody = new ClearValuesRequest();
                Sheets.Spreadsheets.Values.Clear request = service.spreadsheets().values().clear(spreadsheetId, range, requestBody);
                ClearValuesResponse response = request.execute();
                List<List<Object>> updatedList = profitMonth.entrySet().stream()
                        .map(x-> {
                            List<Object> list = List.of(x.getKey().toString(), Utils.round(x.getValue(), 2).toString());
                            return list;
                        })
                        .collect(Collectors.toList());
                ValueRange body = new ValueRange()
                        .setValues(updatedList);
                UpdateValuesResponse result =
                        service.spreadsheets().values().update(spreadsheetId, range, body)
                                .setValueInputOption("RAW")
                                .execute();
                log.debug("# profit sheet updated {}, {}", result.getUpdatedCells());
                failed = false;
            } catch (IOException e) {
                log.error("***Error writing to google {}", counter);
            }
        }
    }

    private void writePositionList() throws IOException {
        final String range = "POSITION" + "!A2:H18";
        ClearValuesRequest requestBody = new ClearValuesRequest();
        Sheets.Spreadsheets.Values.Clear request = service.spreadsheets().values().clear(spreadsheetId, range, requestBody);
        ClearValuesResponse response = request.execute();
        List<List<Object>> updatedList = SwingPosition.convert(positionList);
        ValueRange body = new ValueRange()
                .setValues(updatedList);
        UpdateValuesResponse result =
                service.spreadsheets().values().update(spreadsheetId, range, body)
                        .setValueInputOption("RAW")
                        .execute();
        log.debug("# position sheet updated {}, {}", result.getUpdatedCells());
    }

    /*
     * It will process each instrument by using the config
     * */
    private void processSingle(SwingStockConfig swingStockConfig) throws IOException {
        List<SwingLog> logList = getLogList(swingStockConfig.getSymbol());
        log.debug("\n\nsymbol {}", swingStockConfig);
        log.debug("list log {}", logList);
        updateNoBuyIfAmountExceed(logList, swingStockConfig);
        // if no logs then buy at market price/ LTP
        boolean buyLtpFlag = checkIfNoLogThenBuyLtp(swingStockConfig, logList);
        boolean pendingExecuted = checkIfPendingExecuted(swingStockConfig, logList);
        // then write the buy order into
        if (buyLtpFlag || pendingExecuted) {
            writeLoglist(swingStockConfig.getSymbol(), logList);
        }
        preparePositionList(swingStockConfig, logList);
    }

    private void updateNoBuyIfAmountExceed(List<SwingLog> logList, SwingStockConfig swingStockConfig) {
        Double total = logList.stream().filter(x -> x.getBuyStatus() == SwingLog.Status.COMPLETE &&
                        (x.sellStatus == null || x.sellStatus != SwingLog.Status.COMPLETE))
                .mapToDouble(x -> x.getBuyPrice() * x.getQuantity()).sum();
        if (total >= swingStockConfig.getMaxAmount()) {
            swingStockConfig.setNoBuy(true);
            log.info("Setting NoBuy true for {} as max amount exceeded {}", swingStockConfig.getSymbol(), total);
        }
    }

    /*
     * Create the data for position sheet
     * */
    private void preparePositionList(SwingStockConfig swingStockConfig, List<SwingLog> logList) {
        Integer quantity = logList.stream().filter(x -> x.getBuyStatus() == SwingLog.Status.COMPLETE &&
                        (x.sellStatus == null || x.sellStatus != SwingLog.Status.COMPLETE))
                .mapToInt(x -> x.getQuantity())
                .sum();
        Double total = logList.stream().filter(x -> x.getBuyStatus() == SwingLog.Status.COMPLETE &&
                        (x.sellStatus == null || x.sellStatus != SwingLog.Status.COMPLETE))
                .mapToDouble(x -> x.getBuyPrice() * x.getQuantity()).sum();
        Double avgPrice = null;
        if (quantity != null && quantity > 0) {
            avgPrice = total / quantity;
        }
        Float ltp = null;
        Double currProfit = null;
        try {
            Quote quote = broker.getQuote(swingStockConfig.getSymbol());
            ltp = quote.getLtp();
            currProfit = quantity * ltp - total;
        } catch (IOException e) {

        }
        Double realisedProfit = logList.stream().filter(x -> x.getBuyStatus() == SwingLog.Status.COMPLETE &&
                        x.sellStatus != null && x.sellStatus == SwingLog.Status.COMPLETE)
                .mapToDouble(x -> (x.getSellPrice() - x.getBuyPrice()) * x.getQuantity()).sum();
        SwingPosition swingPos = SwingPosition.builder().symbol(swingStockConfig.getSymbol())
                .quantity(quantity)
                .avgPrice(avgPrice == null ? null : Utils.round(Utils.getFloat(avgPrice), 2))
                .ltp(ltp)
                .currentProfit(currProfit == null ? null : Utils.round(currProfit.floatValue(), 2))
                .total(total == null ? null : Utils.round(total.floatValue(), 2))
                .realiseProfit(realisedProfit == null ? null : Utils.round(realisedProfit.floatValue(), 2)).build();
        positionList.add(swingPos);
        // todo: monthwise profit calculation
        Map<YearMonth, Double> mapOfProfit = logList.stream()
                .filter(x-> x.getSellDate()!=null && x.getProfit()!=null)
                .collect(Collectors.groupingBy(x -> YearMonth.of(x.getSellDate().getYear(), x.getSellDate().getMonth()),
                        Collectors.summingDouble(x -> x.getProfit())));
        swingPos.setProfitMoth(mapOfProfit);
    }

    /*
     * After the first buy order, this method will execute everytime to check if the pending order is
     * processes or missed/ failed. Then create new orders accordingly.
     * */
    private boolean checkIfPendingExecuted(SwingStockConfig swingStockConfig, List<SwingLog> logList) throws IOException {
        List<SwingLog> pendingList = logList.stream().filter(x -> x.getBuyStatus() == SwingLog.Status.PENDING ||
                x.getSellStatus() == SwingLog.Status.PENDING).collect(Collectors.toList());
        SwingLog buyPending = pendingList.stream().filter(x -> x.getBuyStatus() == SwingLog.Status.PENDING).findFirst().orElse(null);
        SwingLog sellPending = pendingList.stream().filter(x -> x.getSellStatus() == SwingLog.Status.PENDING).findFirst().orElse(null);
        boolean noBuyTriggered = false;
        GttOrderResponse buyR = null, sellR = null;
        if (buyPending != null) {
            buyR = broker.getGTTOrder(buyPending.getBuyId());
            if (checkIfNoBuyTriggeredCancel(swingStockConfig, buyPending, buyR)) {
                buyPending = null;
                buyR = null;
                noBuyTriggered = true;
            }
        }
        if (sellPending != null) {
            sellR = broker.getGTTOrder(sellPending.getSellId());
        }
        boolean bothExecuted = processBothExecuted(swingStockConfig, buyPending, sellPending, buyR, sellR, logList);
        if (bothExecuted) {
            sellPending = null;
            sellR = null;
        }
        boolean buyListUpdated = processOnlyBuyExecuted(swingStockConfig, buyPending, sellPending, buyR, sellR, logList);
        boolean sellListUpdated = processOnlySellExecuted(swingStockConfig, buyPending, sellPending, buyR, sellR, logList);
        //listUpdated = processBothExecuted(buyPending, sellPending, buyR, sellR, logList);
        boolean pendingFailed = checkIfPendingFailedOrMissed(swingStockConfig, buyPending, sellPending, buyR, sellR, logList);
        log.warn("checkIfPendingFailedOrMissed executed");
        return noBuyTriggered || bothExecuted || buyListUpdated || sellListUpdated || pendingFailed;
    }

    private boolean checkIfNoBuyTriggeredCancel(SwingStockConfig swingStockConfig, SwingLog buyPending, GttOrderResponse buyR) throws IOException {
        if (swingStockConfig.noBuy) {
            if (buyR.getStatus() == GttOrderResponse.Status.ACTIVE) {
                broker.cancelGTTOrder(buyPending.getBuyId());
                buyR = broker.getGTTOrder(buyPending.getBuyId());
                buyPending.setBuyStatus(SwingLog.Status.CANCEL);
                buyPending.setTransactionStatus(SwingLog.Status.CANCEL);
                return true;
            }
        }
        return false;
    }

    private boolean processBothExecuted(SwingStockConfig swingStockConfig, SwingLog buyPending, SwingLog sellPending, GttOrderResponse buyR, GttOrderResponse sellR, List<SwingLog> logList) {
        if (buyPending == null || sellPending == null || sellR == null || buyR == null) {
            return false;
        }
        if (sellR.getStatus() == GttOrderResponse.Status.TRIGGERED &&
                sellR.getTriggerStatus().equalsIgnoreCase("success") &&
                buyR.getStatus() == GttOrderResponse.Status.TRIGGERED &&
                buyR.getTriggerStatus().equalsIgnoreCase("success")) {
            // both executed
            log.error("both buy and sell triggered.{}: {}", buyPending, sellPending);
            sellPending.setSellPrice(sellR.getPrice());
            sellPending.setSellDate(sellR.getLastUpdated());
            sellPending.setSellStatus(SwingLog.Status.COMPLETE);
            sellPending.setTransactionStatus(SwingLog.Status.COMPLETE);
            sellPending.setProfit(Utils.round((sellPending.getSellPrice() - sellPending.getBuyPrice()) * sellPending.getQuantity(), 2));
            //update buypending
            buyPending.setParentId(sellPending.getParentId());
            return true;
        }
        return false;
    }

    /*
     *
     * */
    private boolean checkIfPendingFailedOrMissed(SwingStockConfig swingStockConfig, SwingLog buyPending, SwingLog sellPending, GttOrderResponse buyR, GttOrderResponse sellR, List<SwingLog> logList) throws IOException {
        if (buyPending != null && (buyPending.getBuyStatus() == SwingLog.Status.COMPLETE
                || buyPending.getBuyStatus() == SwingLog.Status.CANCEL)) {
            return false;
        }
        boolean updatedBuyLtp = false, updatedBuy = false, updatedSell = false;
        Quote quote = broker.getQuote(swingStockConfig.getSymbol());
        if (buyPending == null ||
                ((buyPending.getParentId() == null || buyPending.getBuyLtp()) &&
                        (buyPending.getBuyDate().isBefore(LocalDateTime.now().minusDays(3)) ||
                                buyPending.getBuyStatus() == SwingLog.Status.FAILED))) {
            updatedBuyLtp = updateLTPBuyPendingIfFailedOrMissed(swingStockConfig, buyPending, sellPending, logList);
        } else if (buyPending != null && (buyPending.getBuyStatus() == SwingLog.Status.FAILED ||
                quote.getLtp() < buyPending.getBuyPrice())) {
            updatedBuy = updateBuyPendingIfFailedOrMissed(swingStockConfig, buyPending, logList, quote);
        }
        if (sellPending != null && (sellPending.getSellStatus() == SwingLog.Status.FAILED ||
                quote.getLtp() > sellPending.getSellPrice())) {
            updatedSell = updateSellPendingIfFailedOrMissed(swingStockConfig, sellPending, quote);
        }
        return updatedBuyLtp || updatedBuy || updatedSell;
    }

    private boolean updateLTPBuyPendingIfFailedOrMissed(SwingStockConfig swingStockConfig, SwingLog buyPending, SwingLog sellPending, List<SwingLog> logList) throws IOException {
        boolean updated = false;
        if (buyPending != null) {
            if (buyPending.getBuyStatus() != SwingLog.Status.FAILED) {
                broker.cancelGTTOrder(buyPending.getBuyId());
            }
            buyPending.setBuyStatus(SwingLog.Status.CANCEL);
            buyPending.setTransactionStatus(SwingLog.Status.CANCEL);
            updated = true;
        }
        // buy new order
        if (swingStockConfig.noBuy) {
            return updated;
        }
        SwingLog swingLog = buyAtLTP(swingStockConfig);
        if (buyPending != null) {
            swingLog.setParentId(buyPending.getParentId());
        }
        if (buyPending == null && sellPending != null) {
            swingLog.setParentId(sellPending.getBuyId());
        }
        logList.add(swingLog);
        log.warn("parent cancel previous place new order at LTP");
        return true;
    }

    private boolean updateBuyPendingIfFailedOrMissed(SwingStockConfig swingStockConfig, SwingLog buyPending, List<SwingLog> logList, Quote quote) throws IOException {
        if (buyPending.getBuyStatus() == SwingLog.Status.COMPLETE) {
            return false;
        }
        if (buyPending.getBuyStatus() != SwingLog.Status.FAILED) {
            log.warn("cancel old gtt order {}", buyPending);
            broker.cancelGTTOrder(buyPending.getBuyId());
            buyPending.setBuyStatus(SwingLog.Status.CANCEL);
            buyPending.setTransactionStatus(SwingLog.Status.CANCEL);
            if (swingStockConfig.noBuy) {
                return true;
            }
        }
        if (swingStockConfig.noBuy) {
            return false;
        }
        SwingLog parentLog = logList.stream().filter(x -> x.getBuyId().equals(buyPending.getParentId())).findFirst().orElse(null);
        Float buyPrice;
        if (parentLog != null) {
            buyPrice = Utils.round(100 / (100 + swingStockConfig.getPerChange()) * parentLog.getBuyPrice(), 1);
        } else {
            buyPrice = Utils.floor(getTriggerValueNearTo(quote.getLtp(), Enums.TransactionType.BUY), 1);
        }
        if (buyPrice > quote.getLtp()) {
            log.debug("buy price > ltp, so buying at ltp");
            buyPrice = Utils.floor(getTriggerValueNearTo(quote.getLtp(), Enums.TransactionType.BUY), 1);
        }
        Integer buyQuantity = getQuantity(swingStockConfig, buyPrice);
        Float triggerValue = Utils.getTriggerPriceFromPrice(buyPrice, Enums.TransactionType.BUY);
        GttOrder gttOrder = GttOrder.builder().symbol(buyPending.getSymbol()).limitPrice(buyPrice)
                .ltp(quote.getLtp()).gttType(GttOrder.GttType.SINGLE).transactionType(Enums.TransactionType.BUY)
                .quantity(buyQuantity).triggerPrice(triggerValue).build();

        String buyId = broker.addGTTOrder(gttOrder);
        // update buyPending
        log.warn("old buypeding {}", buyPending);
        buyPending.setBuyPrice(buyPrice);
        buyPending.setBuyStatus(SwingLog.Status.PENDING);
        buyPending.setTransactionStatus(SwingLog.Status.PENDING);
        buyPending.setBuyId(buyId);
        buyPending.setBuyDate(LocalDateTime.now());
        // no change in parent id
        buyPending.setQuantity(buyQuantity);
        log.warn("updated buypeding {}", buyPending);
        return true;
    }

    private boolean updateSellPendingIfFailedOrMissed(SwingStockConfig swingStockConfig, SwingLog sellPending, Quote quote) throws IOException {
        if (sellPending.getSellStatus() == SwingLog.Status.COMPLETE) {
            return false;
        }
        if (sellPending.getSellStatus() != SwingLog.Status.FAILED) {
            broker.cancelGTTOrder(sellPending.getSellId());
        }
        Float sellPrice = Utils.round(sellPending.getBuyPrice() + sellPending.getBuyPrice() * swingStockConfig.getPerChange() / 100, 1);
        if (sellPrice < quote.getLtp()) {
            log.debug("sell price < ltp, so selling at ltp");
            sellPrice = Utils.ceil(getTriggerValueNearTo(quote.getLtp(), Enums.TransactionType.SELL), 1);
        }
        Float triggerValue = Utils.getTriggerPriceFromPrice(sellPrice, Enums.TransactionType.SELL);
        GttOrder gttOrder = GttOrder.builder().symbol(sellPending.getSymbol()).limitPrice(sellPrice)
                .ltp(quote.getLtp()).gttType(GttOrder.GttType.SINGLE).transactionType(Enums.TransactionType.SELL)
                .quantity(sellPending.getQuantity()).triggerPrice(triggerValue).build();

        String sellId = broker.addGTTOrder(gttOrder);
        log.warn("old sellPending {}", sellPending);
        sellPending.setSellStatus(SwingLog.Status.PENDING);
        sellPending.setSellPrice(sellPrice);
        sellPending.setSellDate(LocalDateTime.now());
        sellPending.setSellId(sellId);
        log.warn("updated sellPending {}", sellPending);
        return true;
    }

    private boolean processOnlySellExecuted(SwingStockConfig swingStockConfig, SwingLog buyPending, SwingLog sellPending, GttOrderResponse buyR, GttOrderResponse sellR, List<SwingLog> logList) throws IOException {
        if (sellPending == null) {
            return false;
        }
        if (sellR != null && sellR.getStatus()== GttOrderResponse.Status.TRIGGERED &&
                sellR.getTriggerStatus().equalsIgnoreCase("success") &&
                buyR != null &&
                buyR.getStatus()== GttOrderResponse.Status.TRIGGERED &&
                buyR.getTriggerStatus().equalsIgnoreCase("success")) {
            // both executed
            log.error("both buy and sell triggered.{}: {} Handled in processBothExecuted", buyPending, sellPending);
            return false;
        }
        /*if ((sellR.getStatus().equalsIgnoreCase("triggered") &&
                sellR.getOrderStatus().equalsIgnoreCase("success") &&
                //todo check if the order is not executed or holding position is not correct
                )) {
            log.error("The order is triggered but order is still open.{}: {}", sellPending, sellR);
            return false;
        }*/
        if ((sellR.getStatus()== GttOrderResponse.Status.TRIGGERED &&
                !sellR.getTriggerStatus().equalsIgnoreCase("success")) ||
                sellR.getStatus()== GttOrderResponse.Status.DELETED||
                sellR.getStatus()== GttOrderResponse.Status.CANCELLED ||
                sellR.getStatus()== GttOrderResponse.Status.REJECTED) {
            log.error("The order is triggered but order didn't executed, please look.{}: {}", sellPending, sellR);
            sellPending.setSellStatus(SwingLog.Status.FAILED);
            // should not happen, may be AMO is good choice or
            // place new sell pending order
            return true;
        }
        if (sellR.getStatus()== GttOrderResponse.Status.TRIGGERED &&
                sellR.getTriggerStatus().equalsIgnoreCase("success")) {
            // only sell is executed
            if (buyPending != null && buyPending.getBuyId() != null) {
                broker.cancelGTTOrder(buyPending.getBuyId());
                buyPending.setBuyStatus(SwingLog.Status.CANCEL);
                buyPending.setTransactionStatus(SwingLog.Status.CANCEL);
            }
            // update sell order
            sellPending.setSellPrice(sellR.getPrice());
            sellPending.setSellDate(sellR.getLastUpdated());
            sellPending.setSellStatus(SwingLog.Status.COMPLETE);
            sellPending.setProfit(Utils.round((sellPending.getSellPrice() - sellPending.getBuyPrice()) * sellPending.getQuantity(), 2));
            sellPending.setTransactionStatus(SwingLog.Status.COMPLETE);
            // add new buy order
            Quote quote = broker.getQuote(sellPending.getSymbol());
            // add new sell order
            SwingLog parentLog = logList.stream().filter(x -> x.getBuyId().equals(sellPending.getParentId())).findFirst().orElse(null);
            if (parentLog != null && parentLog.getTransactionStatus() == SwingLog.Status.COMPLETE) {
                log.error("Parent is complete, should not happen parent {}: sellPending {}", parentLog, sellPending);
            }
            if (parentLog == null) {
                return true; //return from here add new buy log from ifmissed logic at ltp
            }
            if (parentLog != null) {
                Float sellPrice = Utils.round(parentLog.getBuyPrice() + parentLog.getBuyPrice() * swingStockConfig.getPerChange() / 100, 1);
                if (sellPrice < quote.getLtp()) {
                    log.debug("sell price < ltp, so selling at ltp");
                    sellPrice = Utils.ceil(getTriggerValueNearTo(quote.getLtp(), Enums.TransactionType.SELL), 1);
                }
                Float triggerValue = Utils.getTriggerPriceFromPrice(sellPrice, Enums.TransactionType.SELL);
                GttOrder gttOrder = GttOrder.builder().symbol(sellPending.getSymbol()).limitPrice(sellPrice)
                        .ltp(quote.getLtp()).gttType(GttOrder.GttType.SINGLE).transactionType(Enums.TransactionType.SELL)
                        .quantity(sellPending.getQuantity()).triggerPrice(triggerValue).build();

                String sellId = broker.addGTTOrder(gttOrder);
                parentLog.setSellStatus(SwingLog.Status.PENDING);
                parentLog.setSellPrice(sellPrice);
                parentLog.setSellDate(LocalDateTime.now());
                parentLog.setSellId(sellId);
            }
            if (!swingStockConfig.noBuy) {
                Float buyPrice = Utils.round(100 / (100 + swingStockConfig.getPerChange()) * sellPending.getSellPrice(), 1);
                boolean buyLtp = false;
                if (buyPrice > quote.getLtp()) {
                    buyLtp = true;
                }
                // check if sell is too far then buy at ltp
                if (parentLog != null && parentLog.getSellPrice() > quote.getLtp() + quote.getLtp() * 2 * swingStockConfig.getPerChange()) {
                    buyLtp = true;
                }
                if (buyLtp) {
                    log.debug("buy price > ltp, so buying at ltp");
                    buyPrice = Utils.floor(getTriggerValueNearTo(quote.getLtp(), Enums.TransactionType.BUY), 1);
                }
                Integer buyQuantity = getQuantity(swingStockConfig, buyPrice);

                Float triggerValue = Utils.getTriggerPriceFromPrice(buyPrice, Enums.TransactionType.BUY);
                GttOrder gttOrder = GttOrder.builder().symbol(sellPending.getSymbol()).limitPrice(buyPrice)
                        .ltp(quote.getLtp()).gttType(GttOrder.GttType.SINGLE).transactionType(Enums.TransactionType.BUY)
                        .quantity(sellPending.getQuantity()).triggerPrice(triggerValue).build();

                String buyId = broker.addGTTOrder(gttOrder);
                SwingLog newBuyLog = SwingLog.builder()
                        .symbol(sellPending.getSymbol())
                        .buyPrice(buyPrice)
                        .buyId(buyId)
                        .buyDate(LocalDateTime.now())
                        .buyStatus(SwingLog.Status.PENDING)
                        .parentId(sellPending.getParentId())
                        .quantity(buyQuantity)
                        .transactionStatus(SwingLog.Status.PENDING)
                        .buyLtp(buyLtp)
                        .build();
                logList.add(newBuyLog);
            }
            return true;
        }
        return false;
    }

    private boolean processOnlyBuyExecuted(SwingStockConfig swingStockConfig, SwingLog buyPending, SwingLog sellPending, GttOrderResponse buyR, GttOrderResponse sellR, List<SwingLog> logList) throws IOException {
        if (buyPending == null) {
            return false;
        }
        if (sellR != null && sellR.getStatus()== GttOrderResponse.Status.TRIGGERED &&
                sellR.getTriggerStatus().equalsIgnoreCase("success") &&
                buyR.getStatus()== GttOrderResponse.Status.TRIGGERED &&
                buyR.getTriggerStatus().equalsIgnoreCase("success")) {
            // both executed
            log.error("both buy and sell triggered. Already handle in processBothExecuted {}: {}", buyPending, sellPending);
            return false;
        }
        /*if ((buyR.getStatus().equalsIgnoreCase("triggered") &&
                buyR.getOrderStatus().equalsIgnoreCase("success"))
                // && todo check if the order i snot executed or position size not matching
        ) {
            log.error("The order is triggered but order is still open.{}: {}", buyPending, buyR);
            return false;
        }*/
        if ((buyR.getStatus()== GttOrderResponse.Status.TRIGGERED &&
                !buyR.getTriggerStatus().equalsIgnoreCase("success")) ||
                buyR.getStatus()== GttOrderResponse.Status.DELETED ||
                buyR.getStatus()== GttOrderResponse.Status.CANCELLED ||
                buyR.getStatus()== GttOrderResponse.Status.REJECTED) {
            log.error("The order is triggered but order didn't executed, please look.{}: {}", buyPending, buyR);
            buyPending.setBuyStatus(SwingLog.Status.FAILED);
            // place new buy order (you may overwrite)
            return true;
        }
        if (buyR.getStatus()== GttOrderResponse.Status.TRIGGERED &&
                buyR.getTriggerStatus().equalsIgnoreCase("success")) {
            // only buy is executed
            if (sellPending != null && sellPending.getSellId() != null) {
                broker.cancelGTTOrder(sellPending.getSellId());
                sellPending.setSellStatus(SwingLog.Status.CANCEL);
            }
            // update buy order
            buyPending.setBuyPrice(buyR.getPrice());
            buyPending.setBuyDate(buyR.getLastUpdated());
            buyPending.setBuyStatus(SwingLog.Status.COMPLETE);
            // add new buy order
            Quote quote = broker.getQuote(buyPending.getSymbol());
            if (!swingStockConfig.noBuy) {
                Float buyPrice = Utils.round(100 / (100 + swingStockConfig.getPerChange()) * buyPending.getBuyPrice(), 1);
                if (buyPrice > quote.getLtp()) {
                    log.debug("buy price > ltp, so buying at ltp");
                    buyPrice = Utils.floor(getTriggerValueNearTo(quote.getLtp(), Enums.TransactionType.BUY), 1);
                }
                Integer buyQuantity = getQuantity(swingStockConfig, buyPrice);
                Float triggerValue = Utils.getTriggerPriceFromPrice(buyPrice, Enums.TransactionType.BUY);
                GttOrder gttOrder = GttOrder.builder().symbol(sellPending.getSymbol()).limitPrice(buyPrice)
                        .ltp(quote.getLtp()).gttType(GttOrder.GttType.SINGLE).transactionType(Enums.TransactionType.BUY)
                        .quantity(sellPending.getQuantity()).triggerPrice(triggerValue).build();

                String buyId = broker.addGTTOrder(gttOrder);
                SwingLog newBuyLog = SwingLog.builder()
                        .symbol(buyPending.getSymbol())
                        .buyPrice(buyPrice)
                        .buyId(buyId)
                        .buyDate(LocalDateTime.now())
                        .buyStatus(SwingLog.Status.PENDING)
                        .parentId(buyPending.getBuyId())
                        .quantity(buyQuantity)
                        .transactionStatus(SwingLog.Status.PENDING)
                        .buyLtp(false)
                        .build();
                logList.add(newBuyLog);
            }
            // add new sell order
            Float sellPrice = Utils.round(buyPending.getBuyPrice() + buyPending.getBuyPrice() * swingStockConfig.getPerChange() / 100, 1);
            if (sellPrice < quote.getLtp()) {
                log.debug("sell price < ltp, so selling at ltp");
                sellPrice = Utils.ceil(getTriggerValueNearTo(quote.getLtp(), Enums.TransactionType.SELL), 1);
            }
            Float triggerValue = Utils.getTriggerPriceFromPrice(sellPrice, Enums.TransactionType.SELL);
            GttOrder gttOrder = GttOrder.builder().symbol(sellPending.getSymbol()).limitPrice(sellPrice)
                    .ltp(quote.getLtp()).gttType(GttOrder.GttType.SINGLE).transactionType(Enums.TransactionType.SELL)
                    .quantity(sellPending.getQuantity()).triggerPrice(triggerValue).build();

            String sellId = broker.addGTTOrder(gttOrder);
            buyPending.setSellStatus(SwingLog.Status.PENDING);
            buyPending.setSellPrice(sellPrice);
            buyPending.setSellDate(LocalDateTime.now());
            buyPending.setSellId(sellId);
            return true;
        }
        return false;
    }

    private boolean checkIfNoLogThenBuyLtp(SwingStockConfig swingStockConfig, List<SwingLog> logList) throws IOException {
        boolean buyLtp = false;
        if (logList.size() == 0) {
            buyLtp = true;
        }
        List<SwingLog> pendingList = logList.stream().filter(x -> x.getBuyStatus() == SwingLog.Status.PENDING ||
                x.getSellStatus() == SwingLog.Status.PENDING).collect(Collectors.toList());
        if (pendingList == null || pendingList.size() == 0) {
            buyLtp = true;
        }
        if (!buyLtp) {
            return false;
        }
        if (swingStockConfig.noBuy) {
            return false;
        }
        // buy at ltp
        SwingLog log = buyAtLTP(swingStockConfig);
        logList.add(log);
        // update list
        return true;
    }

    private SwingLog buyAtLTP(SwingStockConfig swingStockConfig) throws IOException {
        if (swingStockConfig.noBuy == true) {
            return null;
        }
        Quote quote = broker.getQuote(swingStockConfig.getSymbol());
        Float price = Utils.floor(getTriggerValueNearTo(quote.getLtp(), Enums.TransactionType.BUY), 1);
        Integer quantity = getQuantity(swingStockConfig, price);

        Float triggerValue = Utils.getTriggerPriceFromPrice(price, Enums.TransactionType.BUY);
        GttOrder gttOrder = GttOrder.builder().symbol(quote.getSymbol()).limitPrice(price)
                .ltp(quote.getLtp()).gttType(GttOrder.GttType.SINGLE).transactionType(Enums.TransactionType.BUY)
                .quantity(quantity).triggerPrice(triggerValue).build();

        String id = broker.addGTTOrder(gttOrder);
        return SwingLog.builder()
                .symbol(swingStockConfig.getSymbol())
                .buyPrice(price)
                .buyId(id)
                .buyDate(LocalDateTime.now())
                .buyStatus(SwingLog.Status.PENDING)
                .parentId(null)
                .quantity(quantity)
                .transactionStatus(SwingLog.Status.PENDING)
                .buyLtp(true)
                .build();
    }

    private Integer getQuantity(SwingStockConfig swingStockConfig, Float price) {
        return Double.valueOf(Math.ceil(swingStockConfig.getAmount() / price)).intValue();
    }

    private Float getTriggerValueNearTo(Float ltp, Enums.TransactionType type) {
        //"Trigger price was too close to the last price. (difference should be more than 0.25%)"
        if (type == Enums.TransactionType.BUY) {
            Float triggerValue = ltp - ltp * .26f / 100;
            if (Math.abs(ltp - triggerValue) < .1) {
                //(difference should be more than 0.09)
                triggerValue = ltp - .1f;
            }
            return triggerValue;
        } else {
            Float triggerValue = ltp + ltp * .26f / 100;
            if (Math.abs(ltp - triggerValue) < .1) {
                triggerValue = ltp + .1f;
            }
            return triggerValue;
        }
    }

    private void writeLoglist(String symbol, List<SwingLog> logList) throws IOException {
        int count =0; boolean failed=true;
        while (count<5 && failed) {
            try {
                count=count+1;
                final String range = symbol + "!A2:N1000";
                List<List<Object>> updatedList = SwingLog.convert(logList);
                ValueRange body = new ValueRange()
                        .setValues(updatedList);
                UpdateValuesResponse result =
                        service.spreadsheets().values().update(spreadsheetId, range, body)
                                .setValueInputOption("RAW")
                                .execute();
                failed= false;
                log.debug("******************* Sheet updated {}, {}", symbol, result.getUpdatedCells());
            } catch (IOException e) {
                log.error("***Error writing to google {} {}", count, symbol);
            }
        }
    }

    private List<SwingLog> getLogList(String symbol) throws IOException {
        int count = 0;
        boolean failed = true;
        List<SwingLog> list = null;
        while (count < 5 && failed) {
            try {
                count = count + 1;
                final String range = symbol + "!A2:N1000";
                ValueRange response = service.spreadsheets().values()
                        .get(spreadsheetId, range)
                        .execute();
                List<List<Object>> values = response.getValues();
                list = new ArrayList<>();
                if (values != null && !values.isEmpty()) {
                    for (List row : values) {
                        if (row == null || row.isEmpty()) {
                            continue;
                        }
                        SwingLog log = SwingLog.retrieve(row);
                        list.add(log);
                    }
                }
                failed = false;
            } catch (IOException e) {
                log.error("***Error reading to google {} {}", count, symbol);
            }
        }
        return list;
    }

    public List<SwingStockConfig> getStocksConfig() throws IOException {
        int counter = 0;
        boolean failed = true;
        List<SwingStockConfig> list = null;
        while (counter < 5 && failed) {
            try {
                counter = counter + 1;
                final String range = "SYMBOLS!A2:F100";
                ValueRange response = service.spreadsheets().values()
                        .get(spreadsheetId, range)
                        .execute();
                List<List<Object>> values = response.getValues();
                list = new ArrayList<>();
                if (values != null && !values.isEmpty()) {
                    for (List row : values) {
                        SwingStockConfig config = SwingStockConfig.getFromArray(row);
                        list.add(config);
                    }
                }
                failed = false;
            } catch (IOException e) {
                log.error("***Error reading config to google {}", counter);
            }
        }
        return list;
    }

    private HttpRequestInitializer setHttpTimeout(final HttpRequestInitializer requestInitializer) {
        return new HttpRequestInitializer() {
            @Override
            public void initialize(HttpRequest httpRequest) throws IOException {
                requestInitializer.initialize(httpRequest);
                httpRequest.setConnectTimeout(3 * 60000);  // 3 minutes connect timeout
                httpRequest.setReadTimeout(3 * 60000);  // 3 minutes read timeout
            }
        };

    }
}