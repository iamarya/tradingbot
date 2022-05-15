package com.ar.autotrade.strategy.swingstock;


import com.ar.sheetdb.core.Column;
import com.ar.sheetdb.core.GoogleSheet;
import com.ar.sheetdb.core.Table;
import lombok.*;

import java.time.LocalDate;

@Table(name="swing", id=0)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwingStock extends GoogleSheet {
    @Column(name = "predicted_on", order = 1)
    private LocalDate predictedOn;

    @Column(name = "name", order = 2)
    private String tickName;

    @Column(name = "model_name", order = 3)
    private String modelName;

    @Column(name = "%", order = 4)
    private Double profitPercentage;

    @Column(name = "probability", order = 5)
    private Integer probability;

    @Column(name = "expiry_days", order = 6)
    private Integer expiryAfter;

    @Column(name = "quantity", order = 7)
    private Integer quantity;

    @Column(name = "sl_%", order = 8)
    private Double stopLossPercentage;
    @Column(name = "status", order = 9)
    private SwingStockStatus status;


    @Column(name = "buy_on", order = 10)
    private LocalDate buyOn;

    @Column(name = "buy_id", order = 11)
    private String buyOrderId;

    @Column(name = "buy_price", order = 12)
    private Double buyPrice;

    @Column(name = "sell_on", order = 13)
    private LocalDate sellDate;

    @Column(name = "sell_id", order = 14)
    private String sellOrderId;

    @Column(name = "sell_price", order = 15)
    private Double sellPrice;
}