package com.ar.autotrade.kite.models;

import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class OrderBook {

    Map<String, List<String>> map = new ConcurrentHashMap<>();
    Map<String, OrderResponse> list = new ConcurrentHashMap<>();
    Map<String, Order> source = new ConcurrentHashMap<>();

}
