package com.ar.autotrade.broker.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BrokerResponse<T> {
    String status;
    String message;
    T data;
}
