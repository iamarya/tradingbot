package com.ar.autotrade.kite.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class Enums {
    @AllArgsConstructor
    @Getter
    public enum Interval {
        MIN("minute"),
        DAY("day"),
        MIN3("3minute"),
        MIN5("5minute"),
        MIN10("10minute"),
        MIN15("15minute"),
        MIN30("30minute"),
        MIN60("60minute");
        private String text;
    }

    public enum TransactionType {BUY, SELL}

    @AllArgsConstructor
    @Getter
    public enum Variety {
        @JsonProperty("regular") REGULAR("regular"),
        @JsonProperty("amo") AMO("amo"),
        @JsonProperty("co") CO("co"),
        @JsonProperty("bo") BO("bo");
        private String text;
    }

    @AllArgsConstructor
    @Getter
    public enum OrderType {
        @JsonProperty("MARKET") MARKET("MARKET"),
        @JsonProperty("LIMIT") LIMIT("LIMIT"),
        @JsonProperty("SL") SL("SL"),
        @JsonProperty("SL-M") SLM("SL-M");
        private String text;
    }

    @AllArgsConstructor
    @Getter
    public enum Status {
        @JsonProperty("COMPLETE") COMPLETE("COMPLETE"),
        @JsonProperty("REJECTED") REJECTED("REJECTED"),
        @JsonProperty("CANCELLED") CANCELLED("CANCELLED"),
        @JsonProperty("OPEN") OPEN("OPEN"),
        @JsonProperty("PENDING") PENDING("PENDING"),
        @JsonProperty("VALIDATION PENDING") VALIDATION_PENDING("VALIDATION PENDING"),
        @JsonProperty("PUT ORDER REQUEST RECEIVED") PUT_ORDER_REQUEST_RECEIVED("PUT ORDER REQUEST RECEIVED"),
        @JsonProperty("OPEN PENDING") OPEN_PENDING("OPEN PENDING"),
        @JsonProperty("MODIFY VALIDATION PENDING") MODIFY_VALIDATION_PENDING("MODIFY VALIDATION PENDING"),
        @JsonProperty("MODIFY PENDING") MODIFY_PENDING("MODIFY PENDING"),
        @JsonProperty("TRIGGER PENDING") TRIGGER_PENDING("TRIGGER PENDING"),
        @JsonProperty("CANCEL PENDING") CANCEL_PENDING("CANCEL PENDING"),
        @JsonProperty("AMO REQ RECEIVED") AMO_REQ_RECEIVED("AMO REQ RECEIVED");
        private String text;

    } // there are other values as well

    public enum Product {
        CNC,  //delivery
        NRML,  //overnight trading of futures and options less margin
        MIS //intraday
    }

    public enum OrderTask{
        CREATE_CHILD_SL_TARGET_PENDING,
        CREATE_CHILD_SL_TARGET_DONE
    }

    public enum Validity {DAY, IOC}

}
