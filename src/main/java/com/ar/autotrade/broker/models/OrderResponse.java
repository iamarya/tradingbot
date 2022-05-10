package com.ar.autotrade.broker.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderResponse {
    @JsonProperty("order_id")
    String orderId;
    Enums.Variety variety;
    Enums.Status status;
    @JsonProperty("tradingsymbol")
    String tradingSymbol;
    @JsonProperty("transaction_type")
    Enums.TransactionType transactionType;
    @JsonProperty("order_type")
    Enums.OrderType orderType;
    Enums.Product product;
    Float price;
    Integer quantity;
    @JsonProperty("trigger_price")
    Float triggerPrice;
    @JsonProperty("average_price")
    Float averagePrice;
    @JsonProperty("pending_quantity")
    Integer pendingQuantity;
    @JsonProperty("filled_quantity")
    Integer filledQuantity;
    @JsonProperty("disclosed_quantity")
    Integer disclosedQuantity;

    @JsonProperty("order_timestamp")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime orderTimestamp;
    @JsonProperty("tag")
    String tag;

    // not from response fields for tracking
    Enums.OrderTask task; //not yet used
    String parentOrderId;

    public OrderResponse copy(){
        OrderResponse copy = new OrderResponse();
        copy.setParentOrderId(this.getParentOrderId());
        copy.setOrderId(this.getOrderId());
        copy.setStatus(this.getStatus());
        copy.setAveragePrice(this.getAveragePrice());
        copy.setDisclosedQuantity(this.getDisclosedQuantity());
        copy.setFilledQuantity(this.getFilledQuantity());
        copy.setOrderTimestamp(this.getOrderTimestamp());
        copy.setOrderType(this.getOrderType());
        copy.setPendingQuantity(this.getPendingQuantity());
        copy.setPrice(this.getPrice());
        copy.setProduct(this.getProduct());
        copy.setQuantity(this.getQuantity());
        copy.setTag(this.getTag());
        copy.setTradingSymbol(this.getTradingSymbol());
        copy.setTask(this.task);
        copy.setVariety(this.getVariety());
        copy.setTransactionType(this.getTransactionType());
        copy.setTriggerPrice(this.getTriggerPrice());
        return copy;
    }

    public void update(OrderResponse detail) {
        if (detail.getStatus() != null) {
            this.status = detail.getStatus();
        }
        if (detail.getAveragePrice() != null && !detail.getAveragePrice().equals(0)) {
            this.averagePrice = detail.getAveragePrice();
        }
        if (detail.getTag() != null && !detail.getTag().isEmpty()) {
            this.tag = detail.getTag();
        }
        if (detail.getPendingQuantity() != null) {
            this.pendingQuantity = detail.getPendingQuantity();
        }
        if (detail.getFilledQuantity() != null) {
            this.filledQuantity = detail.getFilledQuantity();
        }
        if (detail.getQuantity() != null) {
            this.quantity = detail.getQuantity();
        }
        if(detail.getVariety()!=null){
            this.variety = detail.getVariety();
        }
        if (detail.getTradingSymbol() != null && !detail.getTradingSymbol().isEmpty()) {
            this.tradingSymbol = detail.getTradingSymbol();
        }
        if(detail.getTransactionType()!=null){
            this.transactionType = detail.getTransactionType();
        }
        if(detail.getOrderType()!=null){
            this.orderType = detail.getOrderType();
        }
        if(detail.getProduct()!=null){
            this.product = detail.getProduct();
        }
        if (detail.getPrice() != null && !detail.getPrice().equals(0)) {
            this.price = detail.getPrice();
        }
        if (detail.getTriggerPrice() != null && !detail.getTriggerPrice().equals(0)) {
            this.triggerPrice = detail.getTriggerPrice();
        }
        if (detail.getDisclosedQuantity() != null) {
            this.disclosedQuantity = detail.getDisclosedQuantity();
        }
        if (detail.getOrderTimestamp() != null) {
            this.orderTimestamp = detail.getOrderTimestamp();
        }
    }
}
