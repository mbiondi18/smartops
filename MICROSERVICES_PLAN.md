# Delivery Platform — Microservices Build Plan

A Yummy-style delivery app built as microservices, to practice real-world data/backend engineering and support the Yummy DE interview.

---

## Locked decisions
- **Domain:** delivery/rides — customers, drivers, orders (restaurants optional later).
- **Services:** Spring Boot (Java 17), one per domain.
- **Data:** database-per-service, all as **separate logical databases on ONE Cloud SQL Postgres instance** (authentic pattern, one bill).
- **Compute:** **Cloud Run** (scale-to-zero) — real deployment, ~$0 idle.
- **Events:** **Pub/Sub** for async inter-service communication + feeding analytics.
- **Frontends:** **THREE separate Next.js apps** — customer, driver, admin.
- **Gateway:** an API gateway (Spring Cloud Gateway) as the single entry point.
- **IaC:** Terraform. **CI/CD:** GitHub Actions per service. **Analytics:** reuse existing BigQuery + dbt (Pub/Sub → BigQuery).
- **Est. cost:** ~$10–25/mo (mostly the one Cloud SQL instance; Cloud Run near-zero at low traffic).

## Architecture
```
customer-web ─┐
driver-web   ─┼─► api-gateway ─┬─► auth-service     ─► auth_db
admin-web    ─┘   (JWT check)  ├─► customer-service ─► customer_db
                                ├─► driver-service   ─► driver_db
                                └─► order-service    ─► order_db
                                          │
                                   Pub/Sub events (order.created, order.assigned,
                                          │          order.delivered, ...)
                                          ├─► other services react
                                          └─► Dataflow/loader ─► BigQuery ─► dbt ─► dashboards
        All services + frontends run on Cloud Run.  All DBs live on ONE Cloud SQL instance.
```

## Services & responsibilities
| Service | Owns (DB) | Key responsibilities | Emits events |
|---|---|---|---|
| **auth-service** | `auth_db` | Register/login, issue JWT with role (CUSTOMER/DRIVER/ADMIN) | `account.created` |
| **customer-service** | `customer_db` | Customer profiles, addresses | `customer.updated` |
| **driver-service** | `driver_db` | Driver profiles, availability, vehicle, rating, location | `driver.available`, `driver.location` |
| **order-service** | `order_db` | Create order, assign driver, status transitions | `order.created`, `order.assigned`, `order.delivered` |
| **api-gateway** | — | Single entry point, routes to services, validates JWT | — |
| *restaurant-service (optional, later)* | `restaurant_db` | Restaurants + menus | `menu.updated` |

## Data model (per service, high level)
- **auth_db.accounts**: id, email, password_hash, role, created_at
- **customer_db.customers**: id, account_id, name, phone, address
- **driver_db.drivers**: id, account_id, name, vehicle, status (OFFLINE/AVAILABLE/BUSY), rating, current_location
- **order_db.orders**: id, customer_id, driver_id, restaurant_id, status (PENDING/ASSIGNED/PICKED_UP/DELIVERED/CANCELLED), total_amount, items(JSON), placed_at, delivered_at

> Note: services reference each other by **ID only** (e.g. order stores `customer_id`, `driver_id`) — never shared tables. They resolve details via API calls or events.

## Communication
- **Synchronous:** frontends → api-gateway → service (REST). Gateway validates JWT and forwards role.
- **Asynchronous:** services publish domain events to Pub/Sub; interested services subscribe. Example order lifecycle:
  1. `order-service` creates order → publishes `order.created`
  2. `driver-service` (or a matching component) reacts → assigns a driver → `order.assigned`
  3. driver delivers → `order.delivered`
  4. a subscriber loads events into BigQuery for analytics (reuses your dbt setup).

## Auth & roles
- `auth-service` issues a JWT containing a `role` claim: CUSTOMER, DRIVER, or ADMIN.
- api-gateway + each service validate the JWT (shared secret or public key).
- Each frontend is scoped to a role; the gateway/services enforce role-based access.

## Repo structure (polyrepo — one git repo per service)
Each service/frontend is its **own git repo** (own CI/CD, own deploy), grouped under a `delivery-platform/` folder locally:
```
delivery-platform/
  auth-service/         (git repo — Spring Boot)
  customer-service/     (git repo)
  driver-service/       (git repo)
  order-service/        (git repo)
  api-gateway/          (git repo)
  customer-web/         (git repo — Next.js)
  driver-web/           (git repo)
  admin-web/            (git repo)
  infra/                (git repo — Terraform: Cloud Run, Cloud SQL, Pub/Sub)
```
Each service has its own Dockerfile, docker-compose (for local dev), and `.github/workflows/`.

## Build order — thin vertical slices (don't build it all at once)
- **Phase 0 — Scaffold:** repo structure, shared conventions (JWT secret, base Dockerfile, Cloud SQL instance with empty DBs via Terraform).
- **Phase 1 — Auth slice (thinnest end-to-end):** `auth-service` + `customer-web` login/register working locally in Docker. Proves the pattern.
- **Phase 2 — Orders:** `order-service` + customer-web can place an order (writes to order_db).
- **Phase 3 — Drivers:** `driver-service` + `driver-web` — driver sees & accepts/delivers orders.
- **Phase 4 — Gateway:** `api-gateway` unifies routing; frontends talk only to the gateway.
- **Phase 5 — Admin:** `admin-web` — overview of orders/drivers/customers.
- **Phase 6 — Events:** wire Pub/Sub between services; publish order lifecycle events; subscriber → BigQuery → dbt (reuse existing analytics).
- **Phase 7 — Deploy for real:** Terraform → Cloud SQL (one instance, 4 DBs) + Cloud Run (all services + frontends) + Pub/Sub. Public URLs.
- **Phase 8 — CI/CD:** GitHub Actions per service → build image → deploy to Cloud Run.

## Local dev
- `docker-compose.yaml` runs all services + one Postgres (multiple DBs) + the three frontends for local development before deploying to Cloud Run.

## Cost control
- Cloud Run scales to zero — deploy freely, pay ~nothing when idle.
- ONE Cloud SQL instance (smallest tier) is the main fixed cost (~$9–15/mo).
- `terraform destroy` the Cloud SQL instance when not actively using it (Cloud Run + Pub/Sub cost ~nothing idle, so those can stay).
