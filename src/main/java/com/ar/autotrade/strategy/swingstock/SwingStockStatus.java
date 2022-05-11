package com.ar.autotrade.strategy.swingstock;

public enum SwingStockStatus {
    PREDICTED("predicted"),
    BUY_PLACED("buy_placed"),
    GTT_PLACED("gtt_placed"),
    EXPIRED("expired"),
    COMPLETE("complete"),
    STOPLOSS("stoploss");

    private final String label;

    SwingStockStatus(String label) {
        this.label = label;
    }

    public static SwingStockStatus fromString(String text) {
        for (SwingStockStatus it : SwingStockStatus.values()) {
            if (it.label.equalsIgnoreCase(text)) {
                return it;
            }
        }
        return null;
    }
}
