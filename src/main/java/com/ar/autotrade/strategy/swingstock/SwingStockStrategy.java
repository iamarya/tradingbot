package com.ar.autotrade.strategy.swingstock;

import com.ar.autotrade.broker.BrokerService;
import com.ar.sheetdb.core.Db;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

@Component
public class SwingStockStrategy {

    @Autowired
    BrokerService api;

    @Autowired
    Db db;

    public void init(){
        List<SwingStock> list = db.getAll(SwingStock.class);
        System.out.println(list);
    }

    @PostConstruct
    public void run(){
        System.out.println("######################");
        init();
    }
}
