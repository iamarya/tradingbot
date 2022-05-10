package com.ar.autotrade.kite.utils;

import okhttp3.Headers;

import java.util.Map;

public class Utils {
    public static Headers getKiteHeaders(String userName){
        return Headers.of(Map.ofEntries(Map.entry("authority", "kite.zerodha.com")
                ,Map.entry("pragma", "no-cache")
                ,Map.entry("cache-control", "no-cache")
                ,Map.entry("x-kite-version", "2.7.0")
                ,Map.entry("accept", "application/json, text/plain, */*")
                ,Map.entry("sec-ch-ua-mobile", "?0")
                ,Map.entry("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.182 Safari/537.36")
                ,Map.entry("sec-ch-ua", "\"Chromium\";v=\"88\", \"Google Chrome\";v=\"88\", \";Not A Brand\";v=\"99\"")
                ,Map.entry("sec-fetch-site", "same-origin")
                ,Map.entry("sec-fetch-mode", "cors")
                ,Map.entry("sec-fetch-dest", "empty")
                ,Map.entry("referer", "https://kite.zerodha.com/dashboard")
                ,Map.entry("accept-language", "en-GB,en-US;q=0.9,en;q=0.8")
                ,Map.entry("x-kite-userid", userName)));
    }

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
