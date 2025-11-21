package com.kafka.order.service;

import com.kafka.order.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class OrderProducerService {

    private final KafkaTemplate<String, Order> kafkaTemplate;
    private final String ordersTopic;
    private final Random random = new Random();
    
    private static final String[] PRODUCTS = {
        "Laptop", "Mouse", "Keyboard", "Monitor", "Headphones",
        "Webcam", "USB Cable", "SSD", "RAM", "Graphics Card"
    };

    public OrderProducerService(KafkaTemplate<String, Order> kafkaTemplate,
                                @Value("${kafka.topics.orders}") String ordersTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.ordersTopic = ordersTopic;
    }

    public void produceOrder(String orderId, String product, float price) {
        Order order = Order.newBuilder()
                .setOrderId(orderId)
                .setProduct(product)
                .setPrice(price)
                .build();

        CompletableFuture<SendResult<String, Order>> future = kafkaTemplate.send(ordersTopic, orderId, order);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Produced order: {} - {} - ${}", orderId, product, String.format("%.2f", price));
            } else {
                log.error("Failed to produce order {}: {}", orderId, ex.getMessage());
            }
        });
    }

    public void produceRandomOrders(int count) {
        log.info("Starting to produce {} random orders...", count);
        
        for (int i = 0; i < count; i++) {
            String orderId = "ORD-" + (1000 + i);
            String product = PRODUCTS[random.nextInt(PRODUCTS.length)];
            float price = (float) (Math.round((10.0 + random.nextDouble() * 990.0) * 100.0) / 100.0);
            
            produceOrder(orderId, product, price);
            
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        log.info("Finished producing {} orders", count);
    }
}
