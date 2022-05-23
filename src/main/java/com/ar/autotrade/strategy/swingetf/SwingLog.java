package com.ar.autotrade.strategy.swingetf;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class SwingLog {
    String symbol;
    LocalDateTime buyDate;
    Float buyPrice;
    String buyId;
    Status buyStatus;
    Integer quantity;
    String parentId;

    LocalDateTime sellDate;
    Float sellPrice;
    String sellId;
    Status sellStatus;

    Float profit;
    Status transactionStatus;

    public Boolean getBuyLtp() {
        if(buyLtp!=null){
            return buyLtp;
        }
        if(parentId==null){
            return true;
        }
        return false;
    }

    Boolean buyLtp;



    public static enum Status {
        PENDING, COMPLETE, CANCEL, FAILED;
    }

    private static String getString(LocalDateTime o){
        if(o==null) return "";
        return o.format(formatter);
    }

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static List<List<Object>> convert(List<SwingLog> list) {
        List<List<Object>> finalList = list.stream().map(x -> {
            List<Object> one = new ArrayList<Object>();
            one.add(SwingUtil.getString(x.getSymbol()));
            one.add(getString(x.getBuyDate()));
            one.add(SwingUtil.getString(x.getBuyPrice()));
            one.add(SwingUtil.getString(x.getBuyId()));
            one.add(SwingUtil.getString(x.getBuyStatus()));
            one.add(SwingUtil.getString(x.getParentId()));
            one.add(SwingUtil.getString(x.getQuantity()));
            one.add(getString(x.getSellDate()));
            one.add(SwingUtil.getString(x.getSellPrice()));
            one.add(SwingUtil.getString(x.getSellId()));
            one.add(SwingUtil.getString(x.getSellStatus()));
            one.add(SwingUtil.getString(x.getProfit()));
            one.add(SwingUtil.getString(x.getTransactionStatus()));
            one.add(SwingUtil.getString(x.getBuyLtp()));
            return one;
        }).collect(Collectors.toList());
        return finalList;
    }

    public static SwingLog retrieve(List<Object> row){
        return SwingLog.builder()
                .symbol(SwingUtil.getStringValue(SwingUtil.getAtIndex(row, 0)))
                .buyDate(SwingUtil.getDateTimeValue(SwingUtil.getAtIndex(row, 1)))
                .buyPrice(SwingUtil.getFloatValue(SwingUtil.getAtIndex(row, 2)))
                .buyId(SwingUtil.getStringValue(SwingUtil.getAtIndex(row, 3)))
                .buyStatus(SwingUtil.getStatus(SwingUtil.getAtIndex(row, 4)))
                .parentId(SwingUtil.getStringValue(SwingUtil.getAtIndex(row, 5)))
                .quantity(SwingUtil.getIntValue(SwingUtil.getAtIndex(row, 6)))
                .sellDate(SwingUtil.getDateTimeValue(SwingUtil.getAtIndex(row, 7)))
                .sellPrice(SwingUtil.getFloatValue(SwingUtil.getAtIndex(row, 8)))
                .sellId(SwingUtil.getStringValue(SwingUtil.getAtIndex(row, 9)))
                .sellStatus(SwingUtil.getStatus(SwingUtil.getAtIndex(row, 10)))
                .profit(SwingUtil.getFloatValue(SwingUtil.getAtIndex(row, 11)))
                .transactionStatus(SwingUtil.getStatus(SwingUtil.getAtIndex(row, 12)))
                .buyLtp(SwingUtil.getBooleanValue(SwingUtil.getAtIndex(row, 13)))
                .build();
    }
}
