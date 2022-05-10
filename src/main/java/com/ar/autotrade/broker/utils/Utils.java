package com.ar.autotrade.broker.utils;

import okhttp3.Headers;

import java.util.Map;

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
}
