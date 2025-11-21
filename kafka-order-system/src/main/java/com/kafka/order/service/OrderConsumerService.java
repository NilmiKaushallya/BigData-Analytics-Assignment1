package com.kafka.order.service;

import com.kafka.order.Order;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
//import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class OrderConsumerService {

    private final KafkaTemplate<String, Order> kafkaTemplate;
    private final PriceAggregationService aggregationService;
    private final String retryTopic;
    private final String dlqTopic;
    private final int maxRetries;
    private final ConcurrentHashMap<String, Integer> retryCount = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public OrderConsumerService(KafkaTemplate<String, Order> kafkaTemplate,
                                PriceAggregationService aggregationService,
                                @Value("${kafka.topics.retry}") String retryTopic,
                                @Value("${kafka.topics.dlq}") String dlqTopic,
                                @Value("${kafka.retry.max-attempts}") int maxRetries) {
        this.kafkaTemplate = kafkaTemplate;
        this.aggregationService = aggregationService;
        this.retryTopic = retryTopic;
        this.dlqTopic = dlqTopic;
        this.maxRetries = maxRetries;
    }

    @KafkaListener(topics = "${kafka.topics.orders}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeOrders(ConsumerRecord<String, Order> record, Acknowledgment acknowledgment) {
        processOrder(record, acknowledgment, 0);
    }

    @KafkaListener(topics = "${kafka.topics.retry}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeRetryOrders(ConsumerRecord<String, Order> record, Acknowledgment acknowledgment) {
        int retries = getRetryCount(record);
        processOrder(record, acknowledgment, retries);
    }

    private void processOrder(ConsumerRecord<String, Order> record, Acknowledgment acknowledgment, int retries) {
        Order order = record.value();
        String orderId = order.getOrderId().toString();

        try {
            if (random.nextDouble() < 0.1) {
                throw new RuntimeException("Simulated temporary failure");
            }

            log.info("✓ Processing order: {} - {} - ${}", 
                    orderId, order.getProduct(), String.format("%.2f", order.getPrice()));
            
            aggregationService.addOrder(order.getPrice());
            
            retryCount.remove(orderId);
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("✗ Failed to process order {}: {}", orderId, e.getMessage());
            
            int currentRetry = retries + 1;
            retryCount.put(orderId, currentRetry);

            if (currentRetry < maxRetries) {
                sendToRetry(order, currentRetry);
                acknowledgment.acknowledge();
                
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            } else {
                sendToDlq(order, e.getMessage());
                retryCount.remove(orderId);
                acknowledgment.acknowledge();
            }
        }
    }

    private void sendToRetry(Order order, int retryAttempt) {
        log.info("  → Sent to retry queue (attempt {}/{})", retryAttempt, maxRetries);
        kafkaTemplate.send(retryTopic, order.getOrderId().toString(), order);
    }

    private void sendToDlq(Order order, String errorMessage) {
        log.error("  → Sent to DLQ after {} failed attempts", maxRetries);
        kafkaTemplate.send(dlqTopic, order.getOrderId().toString(), order);
    }

    private int getRetryCount(ConsumerRecord<String, Order> record) {
        for (org.apache.kafka.common.header.Header header : record.headers()) {
            if ("retry_count".equals(header.key())) {
                return Integer.parseInt(new String(header.value(), StandardCharsets.UTF_8));
            }
        }
        return 0;
    }
}
