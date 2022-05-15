package com.ar.autotrade.broker.models;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Builder
@Slf4j
public class Order {
    String symbol;
    Enums.TransactionType transactionType;
    Enums.Variety variety;
    Enums.OrderType orderType;
    Float triggerPrice;
    Float limitPrice;
    Integer quantity;
    Enums.Product product;
    String tag; //max 20 character

    public void validate() {
        if (symbol == null || quantity == null || transactionType == null || variety == null || orderType == null || product == null) {
            throw new RuntimeException("Invalid order " + this);
        }
        if (orderType == Enums.OrderType.SL || orderType == Enums.OrderType.SLM) {
            if (triggerPrice == null) {
                throw new RuntimeException("Invalid order " + this);
            }
        }
        if (orderType == Enums.OrderType.LIMIT || orderType == Enums.OrderType.SL) {
            if (limitPrice == null) {
                throw new RuntimeException("Invalid order " + this);
            }
        }
        if (tag != null && tag.length() > 20) {
            throw new RuntimeException("Invalid tag length " + this);
        }
    }
}
