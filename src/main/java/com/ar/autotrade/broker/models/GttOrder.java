package com.ar.autotrade.broker.models;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Data
@Builder
@Slf4j
public class GttOrder {

    @Getter
    public enum GttType{
        TWO_LEG("two-leg"),
        SINGLE("single");

        private final String value;

        GttType(String value) {
            this.value = value;
        }
    }

    String symbol;
    Float triggerPrice;
    Float limitPrice;
    Integer quantity;
    GttType gttType;
    Enums.TransactionType transactionType;
    Float ltp;
    Float stopLossTriggerPrice;
    Float stopLossLimitPrice;


    public void validate() {
        // check for single leg or double leg validations

        if (symbol == null || quantity == null || transactionType == null) {
            throw new RuntimeException("Invalid order " + this);
        }
    }
}
