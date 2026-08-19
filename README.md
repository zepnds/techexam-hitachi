# Distributed Notification Platform

A distributed, high-throughput notification delivery platform supporting **Email**, **SMS**, **Push Notification**, and **Slack** channels. Built with Java 21, Spring Boot 3, Spring WebFlux, Idempotency Engine, NATS JetStream, Complex Event Processing (CEP), PostgreSQL, and Docker Compose.

---

## 🚀 Quick Start & Docker Deployment

### 1. Initial Deployment
To compile the Maven reactor modules and launch all Docker containers in detached mode:

```bash
./deploy.sh
```

### 2. Full Redeployment (Clean Teardown & Rebuild)
To gracefully stop existing containers, clean Maven build artifacts, rebuild Docker images, and re-launch all microservices:

```bash
./redeploy.sh
```

---

## 🛠 Active Ports & Services

| Service | Port | Description |
| :--- | :--- | :--- |
| **`backend-gateway`** | `8080` | Spring WebFlux Reactive API Gateway (Idempotency, Edge Rate Limiting, Correlation ID tracking) |
| **`backend-core`** | `8081` | Core Notification Microservice & Event Dispatch Engine |
| **`NATS JetStream`** | `4222` / `8222` | NATS High-Performance JetStream Event Streaming Hub & Monitoring UI |
| **`PostgreSQL`** | `5432` | Relational Database for Notification Records & Transition Audit Logs |

---

## 🔑 Key API Endpoints (Gateway Port 8080)

- **`POST /notifications`**: Submit notification (optional `Idempotency-Key` header).
- **`GET /notifications/{id}/status`**: Query notification execution status and delivery history.
