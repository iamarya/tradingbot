package com.ar.autotrade.strategy.swingetf;

import com.ar.autotrade.broker.utils.Utils;
import lombok.Builder;
import lombok.Data;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@Builder
public class SwingPosition {
    String symbol;
    Integer quantity;
    Float total;
    Float avgPrice;
    Float ltp;
    Float currentProfit;
    Float realiseProfit;
    Float currentProfitPer;
    Map<YearMonth, Double> profitMoth;

    private Float getCurrentProfitPer(){
        return Utils.round(currentProfit/total*100d, 2);
    }

    public static List<List<Object>> convert(List<SwingPosition> positionList) {
        List<List<Object>> finalList = positionList.stream().map(x -> {
            List<Object> one = new ArrayList<Object>();
            one.add(SwingUtil.getString(x.getSymbol()));
            one.add(SwingUtil.getString(x.getQuantity()));
            one.add(SwingUtil.getString(x.getTotal()));
            one.add(SwingUtil.getString(x.getAvgPrice()));
            one.add(SwingUtil.getString(x.getLtp()));
            one.add(SwingUtil.getString(x.getCurrentProfit()));
            one.add(SwingUtil.getString(x.getRealiseProfit()));
            one.add(SwingUtil.getString(x.getCurrentProfitPer()));
            return one;
        }).collect(Collectors.toList());
        return finalList;
    }
}
