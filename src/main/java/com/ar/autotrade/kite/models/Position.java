package com.ar.autotrade.kite.models;

public class Position {
    Boolean isIntraday;

    public Boolean getIntraday() {
        return isIntraday;
    }

    public void setIntraday(Boolean intraday) {
        isIntraday = intraday;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Float getAvgPrice() {
        return avgPrice;
    }

    public void setAvgPrice(Float avgPrice) {
        this.avgPrice = avgPrice;
    }

    public Float getLtp() {
        return ltp;
    }

    public void setLtp(Float ltp) {
        this.ltp = ltp;
    }

    public Float getPl() {
        return pl;
    }

    public void setPl(Float pl) {
        this.pl = pl;
    }

    public Float getChange() {
        return change;
    }

    public void setChange(Float change) {
        this.change = change;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    Integer quantity;
    Float avgPrice;
    Float ltp;
    Float pl;
    Float change;
    String symbol;

    @Override
    public String toString() {
        return "Position{" +
                "isIntraday=" + isIntraday +
                ", quantity=" + quantity +
                ", avgPrice=" + avgPrice +
                ", ltp=" + ltp +
                ", pl=" + pl +
                ", change=" + change +
                ", symbol='" + symbol + '\'' +
                '}';
    }
}
