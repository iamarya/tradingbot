package com.ar.autotrade.broker;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest
class BrokerServiceTest {
    @Autowired
    BrokerService broker;

    @Test
    void getPositions() throws IOException {
        broker.start();
        System.out.println(broker.getPositions());
    }
}