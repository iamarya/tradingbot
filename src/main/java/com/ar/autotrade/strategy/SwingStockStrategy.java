package com.ar.autotrade.strategy;

import com.ar.autotrade.kite.KiteAppApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class SwingStockStrategy {

    @Autowired
    KiteAppApi api;

    public void init(){

    }

    @PostConstruct
    public void run(){
        System.out.println("######################");
    }
}
