package com.ar.autotrade.broker.models;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class GttOrderResponse {


    public enum Status {
        TRIGGERED("triggered"),
        ACTIVE("active"),
        CANCELLED("cancelled"),
        DELETED("deleted");
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
    String triggerStatus;
    Float price; // put actual sell price from order
    LocalDateTime lastUpdated;
    String triggerId;
    GttOrder.GttType gttType;

    LocalDateTime triggeredOn;
    String limitOrderId;
    Enums.Status limitOrderStatus;
    TriggerType triggerType;

    public enum TriggerType {
        STOPLOSS("stoploss"),
        TARGET("target");

        private final String value;

        TriggerType(String value) {
            this.value = value;
        }

        public static TriggerType getFromValue(String value) {
            for (TriggerType type : TriggerType.values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
            return null;
        }
    }
}
