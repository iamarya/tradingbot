package com.ar.autotrade.strategy.swingstock;


import com.ar.sheetdb.core.Column;
import com.ar.sheetdb.core.GoogleSheet;
import com.ar.sheetdb.core.Table;
import lombok.Data;

import java.time.LocalDate;

@Table(name="swing", id=0)
@Data
public class SwingStock extends GoogleSheet {
    @Column(name = "predicted_on", order = 1)
    public LocalDate predictedOn;

    @Column(name = "name", order = 2)
    public String tickName;

    @Column(name = "model_name", order = 3)
    public String modelName;

    @Column(name = "%", order = 4)
    public Double profitPercentage;

    @Column(name = "probability", order = 5)
    public Integer probability;

    @Column(name = "expiry_days", order = 6)
    public Integer expiryAfter;

    @Column(name = "buy_on", order = 7)
    public LocalDate buyOn;

    @Column(name = "buy_id", order = 8)
    public Integer buyOrderId;

    @Column(name = "buy_price", order = 9)
    public Double buyPrice;

    @Column(name = "quantity", order = 10)
    public Integer quantity;

    @Column(name = "sl_%", order = 11)
    public Double stopLossPercentage;

    @Column(name = "sell_on", order = 12)
    public LocalDate sellDate;

    @Column(name = "sell_id", order = 13)
    public Integer sellOrderId;

    @Column(name = "sell_price", order = 14)
    public Double sellPrice;

    @Column(name = "status", order = 15)
    public SwingStockStatus status;

}