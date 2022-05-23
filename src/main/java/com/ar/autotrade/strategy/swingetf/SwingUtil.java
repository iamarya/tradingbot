package com.ar.autotrade.strategy.swingetf;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class SwingUtil {

    public static Integer getIntValue(Object o) {
        if (o == null || o.toString().equalsIgnoreCase(""))
            return null;
        return Integer.parseInt(o.toString());
    }

    public static String getStringValue(Object o) {
        if (o == null || o.toString().equalsIgnoreCase(""))
            return null;
        return o.toString();
    }

    public static Float getFloatValue(Object o) {
        if (o == null || o.toString().equalsIgnoreCase(""))
            return null;
        return Float.parseFloat(o.toString());
    }

    public static SwingLog.Status getStatus(Object o) {
        if (o == null || o.toString().equalsIgnoreCase(""))
            return null;
        return SwingLog.Status.valueOf(o.toString());
    }

    public static Object getAtIndex(List row, int i) {
        Object o;
        try {
            o = row.get(i);
        } catch (Exception e) {
            o= null;
        }
        return o;
    }

    public static Boolean getBooleanValue(Object o) {
        if (o == null || o.toString().equalsIgnoreCase(""))
            return null;
        if(o.toString().equalsIgnoreCase("true")){
            return true;
        } else{
            return false;
        }
    }

    public static String getString(Object o){
        if(o==null) return "";
        return o.toString();
    }

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static LocalDateTime getDateTimeValue(Object o) {
        if (o == null || o.toString().equalsIgnoreCase(""))
            return null;
        return LocalDateTime.parse(o.toString(), formatter);
    }
}
