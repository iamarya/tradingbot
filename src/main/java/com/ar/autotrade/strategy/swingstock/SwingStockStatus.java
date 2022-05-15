package com.ar.autotrade.strategy.swingstock;

public enum SwingStockStatus {
    PREDICTED,
    BUY_PLACED,
    GTT_PLACED,
    SELL_PLACED,
    EXPIRED,
    COMPLETE,
    STOPLOSS,
    BUY_FAILED,
    GTT_FAILED,
    SELL_FAILED, // may not happen ever
    CANCEL; // if set from ui no action will be taken after
}
