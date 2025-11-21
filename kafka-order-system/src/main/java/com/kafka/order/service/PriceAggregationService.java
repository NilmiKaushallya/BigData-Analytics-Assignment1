package com.kafka.order.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
@Getter
public class PriceAggregationService {

    private final AtomicInteger totalOrders = new AtomicInteger(0);
    private final AtomicReference<Double> totalPrice = new AtomicReference<>(0.0);

    public void addOrder(float price) {
        int orders = totalOrders.incrementAndGet();
        double newTotal = totalPrice.updateAndGet(current -> current + price);
        double average = newTotal / orders;
        
        log.info("→ Running Average Price: ${} (Total Orders: {})", 
                String.format("%.2f", average), orders);
    }

    public double getRunningAverage() {
        int orders = totalOrders.get();
        if (orders == 0) return 0.0;
        return totalPrice.get() / orders;
    }

    public void reset() {
        totalOrders.set(0);
        totalPrice.set(0.0);
        log.info("Statistics reset");
    }
}
