# SmartOps Hub

A cloud-native task management API with AI-powered analysis, built with Spring Boot, PostgreSQL, and the Claude API. Deployed on Kubernetes with full CI/CD and observability.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3 (Java 17) |
| Database | PostgreSQL |
| AI | Claude API (Anthropic) |
| Containers | Docker + Docker Compose |
| Orchestration | Kubernetes (AKS / GKE) |
| IaC | Terraform |
| CI/CD | GitHub Actions / Azure DevOps |
| Observability | Prometheus + Grafana |

---

## Getting Started (Phase 1 — Local)

### Prerequisites
- Java 17+
- Maven 3.9+
- Docker + Docker Compose

### Run with Docker Compose

```bash
# Start PostgreSQL + the app
docker-compose up -d

# Check logs
docker-compose logs -f app

# Stop everything
docker-compose down
```

The API will be available at: http://localhost:8080
Swagger UI: http://localhost:8080/swagger-ui.html

### Run locally (without Docker)

```bash
# Start only PostgreSQL
docker-compose up -d postgres

# Run the app
cd backend
mvn spring-boot:run
```

---

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| POST | /api/tasks | Create a task |
| GET | /api/tasks | List all tasks |
| GET | /api/tasks?status=PENDING | Filter by status |
| GET | /api/tasks/{id} | Get task by ID |
| PUT | /api/tasks/{id} | Update a task |
| DELETE | /api/tasks/{id} | Delete a task |
| POST | /api/tasks/{id}/analyse | Trigger AI analysis |

### Example: Create a task

```bash
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Review pull request",
    "description": "Review and merge the feature branch for user auth",
    "priority": "HIGH"
  }'
```

### Example: Trigger AI analysis

```bash
curl -X POST http://localhost:8080/api/tasks/1/analyse
```

---

## Running Tests

```bash
cd backend

# Unit tests only
mvn test -Dtest="*ServiceTest"

# All tests (includes integration tests)
mvn verify
```

---

## Enable AI Analysis (Phase 2)

Set these environment variables:

```bash
export ANTHROPIC_API_KEY=your_key_here
export ANTHROPIC_API_ENABLED=true
```

Or add to docker-compose.yml environment section.

---

## Project Phases

- [x] Phase 1 — Spring Boot API + PostgreSQL + Tests
- [ ] Phase 2 — Claude API integration
- [ ] Phase 3 — Full Docker containerisation
- [ ] Phase 4 — Terraform infrastructure
- [ ] Phase 5 — Kubernetes deployment
- [ ] Phase 6 — CI/CD pipeline
- [ ] Phase 7 — Observability
- [ ] Phase 8 — Python automation scripts
- [ ] Phase 9 — BigQuery data layer

---

## Architecture

```
┌─────────────┐     ┌──────────────────┐     ┌─────────────┐
│   Client    │────▶│  Spring Boot API  │────▶│  PostgreSQL │
└─────────────┘     └──────────────────┘     └─────────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │   Claude API    │
                    │  (Anthropic)    │
                    └─────────────────┘
```
