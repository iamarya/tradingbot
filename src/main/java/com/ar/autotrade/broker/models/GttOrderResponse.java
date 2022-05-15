package com.ar.autotrade.broker.models;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class GttOrderResponse {


    public enum Status{
        TRIGGERED("triggered"),
        ACTIVE("active");
        //triggered, active, (cancelled, rejected, disabled,) by system ; deleted by user,

        private final String value;

        Status(String value) {
            this.value = value;
        }
        public static Status getFromValue(String value) {
            for (Status status : Status.values()) {
                if (status.value.equals(value)) {
                    return status;
                }
            }
            return null;
        }
    }
    Status status;
    Integer quantity;
    String orderStatus;
    Float price;
    LocalDateTime lastUpdated;
}
