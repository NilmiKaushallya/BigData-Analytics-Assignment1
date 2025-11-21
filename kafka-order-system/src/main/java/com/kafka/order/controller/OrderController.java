package com.kafka.order.controller;

import com.kafka.order.service.OrderProducerService;
import com.kafka.order.service.PriceAggregationService;
import com.kafka.order.service.DlqMonitorService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderProducerService producerService;
    private final PriceAggregationService aggregationService;
    private final DlqMonitorService dlqMonitorService;

    @PostMapping("/produce")
    public ResponseEntity<Map<String, String>> produceOrder(@RequestBody OrderRequest request) {
        producerService.produceOrder(request.getOrderId(), request.getProduct(), request.getPrice());
        Map<String, String> response = new HashMap<>();
        response.put("message", "Order produced successfully");
        response.put("orderId", request.getOrderId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/produce/random")
    public ResponseEntity<Map<String, String>> produceRandomOrders(@RequestParam(defaultValue = "10") int count) {
        new Thread(() -> producerService.produceRandomOrders(count)).start();
        Map<String, String> response = new HashMap<>();
        response.put("message", "Started producing " + count + " random orders");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> getStatistics() {
        StatisticsResponse stats = new StatisticsResponse(
                aggregationService.getTotalOrders().get(),
                aggregationService.getTotalPrice().get(),
                aggregationService.getRunningAverage(),
                dlqMonitorService.getFailedCount()
        );
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/statistics/reset")
    public ResponseEntity<Map<String, String>> resetStatistics() {
        aggregationService.reset();
        Map<String, String> response = new HashMap<>();
        response.put("message", "Statistics reset successfully");
        return ResponseEntity.ok(response);
    }

    @Data
    @AllArgsConstructor
    public static class OrderRequest {
        private String orderId;
        private String product;
        private float price;
    }

    @Data
    @AllArgsConstructor
    public static class StatisticsResponse {
        private int totalOrders;
        private double totalPrice;
        private double runningAverage;
        private int failedOrders;
    }
}
