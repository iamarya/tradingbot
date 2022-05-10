package com.ar.autotrade.broker.models;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class GTTOrderResponse {
    String status; //triggered, active, (cancelled, rejected, disabled,) by system ; deleted by user,
    Integer quantity;
    String orderStatus;
    Float price;
    LocalDateTime lastUpdated;
}
