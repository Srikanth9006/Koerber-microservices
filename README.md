# Körber Java Microservices Assignment

Two Spring Boot microservices — **Inventory Service** and **Order Service** — that communicate via REST APIs to manage e-commerce stock and order fulfillment.

---

## Architecture Overview

```
┌────────────────────────┐       REST        ┌──────────────────────────┐
│     Order Service      │ ───────────────►  │   Inventory Service      │
│     (port 8082)        │                   │      (port 8081)         │
│                        │ ◄───────────────  │                          │
│  POST /order           │                   │  GET  /inventory/{id}    │
│                        │                   │  POST /inventory/update  │
└────────────────────────┘                   └──────────────────────────┘
         │                                            │
    H2 (orderdb)                               H2 (inventorydb)
    Liquibase                                  Liquibase
```

### Key Design Decisions

- **Factory Pattern** in Inventory Service: `InventoryHandlerFactory` resolves `InventoryHandler` implementations by type. Adding a new strategy (e.g., LIFO, FIFO) only requires implementing `InventoryHandler` and annotating with `@Component` — zero changes to existing code.
- **FEFO (First Expiry, First Out)**: Orders consume batches with the nearest expiry date first.
- **Liquibase**: Manages schema creation and sample data loading automatically at startup.
- **Lombok**: Reduces boilerplate on entities and DTOs.
- **Swagger/OpenAPI**: Auto-generated API docs available at `/swagger-ui.html`.

---

## Project Structure

```
koerber-microservices/
├── pom.xml                          # Parent POM
├── README.md
├── inventory-service/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/koerber/inventory/
│       │   ├── InventoryServiceApplication.java
│       │   ├── controller/InventoryController.java
│       │   ├── service/InventoryService.java              # Interface
│       │   ├── service/impl/DefaultInventoryService.java  # Implementation
│       │   ├── factory/InventoryHandler.java              # Strategy interface
│       │   ├── factory/DefaultInventoryHandler.java       # Default (FEFO) handler
│       │   ├── factory/InventoryHandlerFactory.java       # Factory
│       │   ├── repository/InventoryBatchRepository.java
│       │   ├── model/InventoryBatch.java
│       │   ├── dto/                                       # Request/Response DTOs
│       │   ├── exception/                                 # Custom exceptions + handler
│       │   └── config/                                    # OpenAPI config
│       ├── main/resources/
│       │   ├── application.properties
│       │   └── db/changelog/                              # Liquibase changelogs + CSV
│       └── test/...                                       # Unit + integration tests
└── order-service/
    ├── pom.xml
    └── src/
        ├── main/java/com/koerber/order/
        │   ├── OrderServiceApplication.java
        │   ├── controller/OrderController.java
        │   ├── service/OrderService.java                  # Interface
        │   ├── service/impl/DefaultOrderService.java      # Implementation
        │   ├── client/InventoryClient.java                # RestTemplate HTTP client
        │   ├── repository/OrderRepository.java
        │   ├── model/Order.java, OrderStatus.java
        │   ├── dto/
        │   ├── exception/
        │   └── config/
        ├── main/resources/
        │   ├── application.properties
        │   └── db/changelog/
        └── test/...
```

---

## Prerequisites

- **Java 17** (minimum Java 8)
- **Maven 3.6+**

---

## Setup & Running

### Option 1: Run each service independently

```bash
# Clone the repository
git clone https://github.com/Srikanth9006/Koerber-microservices.git
cd koerber-microservices

# Terminal 1 — Start Inventory Service (port 8081)
cd inventory-service
mvn spring-boot:run

# Terminal 2 — Start Order Service (port 8082)
cd order-service
mvn spring-boot:run
```

> **Important:** Start the Inventory Service **before** the Order Service, as the Order Service communicates with it.

### Option 2: Build all from root

```bash
cd koerber-microservices
mvn clean install

# Then run each JAR:
java -jar inventory-service/target/inventory-service-1.0.0-SNAPSHOT.jar
java -jar order-service/target/order-service-1.0.0-SNAPSHOT.jar
```

---

## API Documentation

Swagger UI is available at runtime:
- Inventory Service: http://localhost:8081/swagger-ui.html
- Order Service:     http://localhost:8082/swagger-ui.html

H2 Console (for inspecting the in-memory DB):
- Inventory: http://localhost:8081/h2-console  (JDBC URL: `jdbc:h2:mem:inventorydb`)
- Orders:    http://localhost:8082/h2-console  (JDBC URL: `jdbc:h2:mem:orderdb`)

---

## Endpoints

### Inventory Service (port 8081)

#### `GET /inventory/{productId}`
Returns inventory batches for a product, sorted by expiry date ascending.

**Example:**
```bash
curl http://localhost:8081/inventory/1005
```
```json
{
  "productId": 1005,
  "productName": "Smartwatch",
  "batches": [
    { "batchId": 5, "quantity": 39, "expiryDate": "2026-03-31" },
    { "batchId": 7, "quantity": 40, "expiryDate": "2026-04-24" },
    { "batchId": 2, "quantity": 52, "expiryDate": "2026-05-30" }
  ]
}
```

**Error (product not found):** `404 Not Found`
```json
{ "error": "No inventory found for productId: 9999" }
```

---

#### `POST /inventory/update`
Deducts quantities from specified batches. Called internally by the Order Service.

**Request:**
```json
{
  "productId": 1005,
  "batchUpdates": [
    { "batchId": 5, "quantityToDeduct": 10 }
  ]
}
```
**Response:** `200 OK` (empty body)

---

### Order Service (port 8082)

#### `POST /order`
Places a new order. The service fetches inventory (FEFO), reserves stock across batches, updates inventory, and persists the order.

**Request:**
```bash
curl -X POST http://localhost:8082/order \
  -H "Content-Type: application/json" \
  -d '{"productId": 1002, "quantity": 3}'
```
```json
{
  "orderId": 11,
  "productId": 1002,
  "productName": "Smartphone",
  "quantity": 3,
  "status": "PLACED",
  "reservedFromBatchIds": [9],
  "message": "Order placed. Inventory reserved."
}
```

**Error (insufficient inventory):** `422 Unprocessable Entity`
```json
{ "error": "Insufficient inventory for productId: 1002. Requested: 500, Available: 112" }
```

**Error (invalid quantity):** `400 Bad Request`
```json
{ "error": "Order quantity must be greater than zero." }
```

---

## Testing

### Run all tests

```bash
# From root
mvn test

# Or per service
cd inventory-service && mvn test
cd order-service && mvn test
```

### Test Coverage

| Service | Test Class | Type | What's tested |
|---|---|---|---|
| Inventory | `InventoryServiceTest` | Unit (Mockito) | Sort by expiry, update deductions, error cases |
| Inventory | `InventoryControllerIntegrationTest` | Integration (@SpringBootTest) | Full HTTP flow, H2, Liquibase data |
| Order | `OrderServiceTest` | Unit (Mockito) | FEFO logic, multi-batch reservation, validation |
| Order | `OrderControllerIntegrationTest` | Integration (@SpringBootTest) | Full HTTP flow with mocked InventoryClient |

---

## Extending the Factory Pattern

To add a new inventory strategy (e.g., LIFO):

1. Create a new class implementing `InventoryHandler`:

```java
@Component
public class LifoInventoryHandler implements InventoryHandler {

    @Override
    public String getHandlerType() { return "LIFO"; }

    @Override
    public InventoryResponse getInventorySortedByExpiry(Long productId) {
        // LIFO-specific logic
    }

    @Override
    public void updateInventory(UpdateInventoryRequest request) {
        // LIFO-specific update
    }
}
```

2. Use it via the factory:

```java
inventoryHandlerFactory.getHandler("LIFO").getInventorySortedByExpiry(productId);
```

No other code changes needed — the factory auto-discovers Spring beans implementing `InventoryHandler`.
