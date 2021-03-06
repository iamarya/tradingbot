package com.ar.autotrade.broker.utils;

import com.ar.autotrade.broker.models.Enums;

public class Utils {
    public static Float round(double value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (float) Math.round(value * scale) / scale;
    }

    public static Float floor(double value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (float) Math.floor(value * scale) / scale;
    }

    public static Float ceil(double value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (float) Math.ceil(value * scale) / scale;
    }

    public static String getPriceAsString(Float price){
        return String.format("%.1f",round(price,1));
    }

    public static void sleep(long time){
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static Float getFloat(Double d) {
        if(d== null) {
            return null;
        }
        return d.floatValue();
    }

    public static Float getGttLimitPriceUsingLtp(Double buyPrice, Double profitPercentage, Float ltp) {
        var target = buyPrice + buyPrice*profitPercentage/100;

        return (float) target;
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

    public static Float getTriggerPriceFromPrice(Float limitPrice, Enums.TransactionType type) {
        if(type== Enums.TransactionType.BUY) {
            return floor(limitPrice - limitPrice * 0.05f/100, 1);
        }
        else {
            return ceil(limitPrice + limitPrice * 0.05f/100, 1);
        }
    }

    public static Float getGttStopLossPriceUsingLtp(Double buyPrice, Double stopLossPercentage, Float ltp) {
        var target =  (buyPrice/(100+stopLossPercentage))*100;
        return (float) target;
    }
}
