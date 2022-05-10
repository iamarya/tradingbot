package com.ar.autotrade.broker.models;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class Quote {
    String symbol;
    Integer instrumentToken;
    LocalDateTime dateTime;
    Float ltp;
    Float perChange;
    @Builder.Default
    Boolean index = false;
    Float volume;
}
