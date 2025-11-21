# Kafka Order System - Spring Boot Java Implementation

A complete Kafka-based order processing system built with **Spring Boot 3.2** and **Java 17**, featuring Avro serialization, real-time price aggregation, retry logic, and Dead Letter Queue (DLQ) support.

---

## Quick Start

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- Docker Desktop

### Setup and Run

**Option 1: Automated (Recommended)**
```bash
setup.bat
mvn spring-boot:run
```

**Option 2: Manual**
```bash
# 1. Start Kafka infrastructure
docker-compose up -d

# 2. Wait 60 seconds for services to initialize

# 3. Build and run
mvn clean compile
mvn spring-boot:run
```

### Test the API
```bash
# Produce 50 random orders
curl -X POST "http://localhost:8080/api/orders/produce/random?count=50"

# Check statistics
curl http://localhost:8080/api/orders/statistics
```

---

## ✅ Features

- **Avro Serialization** with Confluent Schema Registry
- **Real-time Aggregation** - Running average of order prices
- **Retry Logic** - Automatic retry up to 3 times with 5-second delays
- **Dead Letter Queue** - Permanently failed messages stored in DLQ
- **REST API** - Easy interaction via HTTP endpoints
- **Spring Boot 3.2** - Modern Java enterprise framework

---

## 📁 Project Structure

```
kafka-order-system/
├── src/main/
│   ├── java/com/kafka/order/
│   │   ├── KafkaOrderSystemApplication.java
│   │   ├── config/
│   │   │   ├── KafkaTopicConfig.java
│   │   │   └── KafkaConsumerConfig.java
│   │   ├── controller/
│   │   │   └── OrderController.java
│   │   └── service/
│   │       ├── OrderProducerService.java
│   │       ├── OrderConsumerService.java
│   │       ├── PriceAggregationService.java
│   │       └── DlqMonitorService.java
│   └── resources/
│       ├── application.yml
│       └── avro/
│           └── order.avsc
├── pom.xml
├── docker-compose.yml
├── setup.bat
├── run.bat
└── test-api.bat
```

---

## 🔌 REST API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/orders/produce` | Produce a single order |
| POST | `/api/orders/produce/random?count=N` | Produce N random orders |
| GET | `/api/orders/statistics` | Get processing statistics |
| POST | `/api/orders/statistics/reset` | Reset statistics |

### Example: Produce Single Order
```bash
curl -X POST http://localhost:8080/api/orders/produce \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORD-1001",
    "product": "Laptop",
    "price": 599.99
  }'
```

---

## 📊 Order Message Schema (Avro)

```json
{
  "type": "record",
  "name": "Order",
  "namespace": "com.kafka.order",
  "fields": [
    {"name": "orderId", "type": "string"},
    {"name": "product", "type": "string"},
    {"name": "price", "type": "float"}
  ]
}
```

---

## 🔄 System Architecture

```
REST API
  ↓
OrderProducerService
  ↓
[orders topic] ← Schema Registry (Avro)
  ↓
OrderConsumerService
  ├─ Success → PriceAggregationService (running average)
  └─ Failure ↓
     [orders-retry topic]
       ├─ Retry 1
       ├─ Retry 2
       └─ Retry 3 ↓
          [orders-dlq topic]
            ↓
          DlqMonitorService
```

---

## 📝 Console Output Example

```
2025-11-20 21:00:01.234 INFO  Produced order: ORD-1001 - Laptop - $599.99
2025-11-20 21:00:01.345 INFO  ✓ Processing order: ORD-1001 - Laptop - $599.99
2025-11-20 21:00:01.346 INFO  → Running Average Price: $599.99 (Total Orders: 1)

2025-11-20 21:00:02.123 ERROR ✗ Failed to process order ORD-1002: Simulated temporary failure
2025-11-20 21:00:02.235 INFO    → Sent to retry queue (attempt 1/3)

2025-11-20 21:00:10.456 WARN  [DLQ Message #1]
2025-11-20 21:00:10.456 WARN    Order ID: ORD-1003
2025-11-20 21:00:10.456 WARN    Product: Keyboard
2025-11-20 21:00:10.456 WARN    Price: $89.99
```

---

## 🛠️ Configuration

Edit `src/main/resources/application.yml`:

```yaml
kafka:
  topics:
    orders: orders
    retry: orders-retry
    dlq: orders-dlq
  retry:
    max-attempts: 3
    delay-seconds: 5
```

---

## 🧪 Monitoring Kafka

**List all topics:**
```bash
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092
```

**View messages in orders topic:**
```bash
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic orders \
  --from-beginning
```

**Check consumer group status:**
```bash
docker exec -it kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe \
  --group order-consumer-group
```

---

## 🛑 Stopping the System

```bash
# Stop application
Ctrl+C

# Stop Kafka infrastructure
docker-compose down

# Stop and remove all data
docker-compose down -v
```

---

## 🐛 Troubleshooting

| Issue | Solution |
|-------|----------|
| Schema Registry connection refused | Wait 60 seconds after `docker-compose up -d` |
| Avro classes not found | Run `mvn clean compile` |
| Port 8080 already in use | Change port in `application.yml` |
| Docker not running | Start Docker Desktop |

---

## 🔧 Technology Stack

- **Java**: 17
- **Spring Boot**: 3.2.0
- **Spring Kafka**: Latest
- **Apache Avro**: 1.11.3
- **Confluent Kafka**: 7.5.0
- **Maven**: 3.6+
- **Docker**: Latest

---

