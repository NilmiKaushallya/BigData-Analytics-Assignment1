package com.kafka.order.service;

import com.kafka.order.Order;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class DlqMonitorService {

    private final AtomicInteger failedCount = new AtomicInteger(0);

    @KafkaListener(topics = "${kafka.topics.dlq}", groupId = "dlq-monitor-group")
    public void monitorDlq(ConsumerRecord<String, Order> record, Acknowledgment acknowledgment) {
        Order order = record.value();
        int count = failedCount.incrementAndGet();

        log.warn("======================================================================");
        log.warn("[DLQ Message #{}]", count);
        log.warn("  Order ID: {}", order.getOrderId());
        log.warn("  Product: {}", order.getProduct());
        log.warn("  Price: ${}", String.format("%.2f", order.getPrice()));
        log.warn("  Error: Permanently failed after maximum retry attempts");
        log.warn("----------------------------------------------------------------------");

        acknowledgment.acknowledge();
    }

    public int getFailedCount() {
        return failedCount.get();
    }
}
