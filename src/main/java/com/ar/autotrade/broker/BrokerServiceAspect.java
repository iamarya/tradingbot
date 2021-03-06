package com.ar.autotrade.broker;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Aspect
@Configuration
public class BrokerServiceAspect {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Around("execution(* com.ar.autotrade.broker.BrokerService.get*(..)) " +
            " && execution(* com.ar.autotrade.broker.BrokerService.add*(..)) " +
            " && execution(* com.ar.autotrade.broker.BrokerService.cancel*(..))")
    public void reLoginIfExpired(ProceedingJoinPoint joinPoint) {
        logger.info(" Check for user access ");
        try {
            joinPoint.proceed();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        logger.info(" Allowed execution for {}", joinPoint);
    }
}