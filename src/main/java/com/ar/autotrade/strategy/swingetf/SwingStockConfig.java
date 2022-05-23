package com.ar.autotrade.strategy.swingetf;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SwingStockConfig {
    String symbol;
    Float amount;
    Float perChange;
    Boolean enabled;
    Boolean noBuy;
    Float maxAmount;

    public static SwingStockConfig getFromArray(List<Object> row) {

        return SwingStockConfig.builder().symbol(SwingUtil.getStringValue(row.get(0)))
                .amount(SwingUtil.getFloatValue(row.get(1)))
                .perChange(SwingUtil.getFloatValue(row.get(2)))
                .enabled(SwingUtil.getBooleanValue(row.get(3)))
                .noBuy(SwingUtil.getBooleanValue(row.get(4)))
                .maxAmount(SwingUtil.getFloatValue(row.get(5)))
                .build();
    }
}
