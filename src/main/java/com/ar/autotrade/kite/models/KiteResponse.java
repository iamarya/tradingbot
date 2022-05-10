package com.ar.autotrade.kite.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KiteResponse<T> {
    String status;
    String message;
    T data;
}
