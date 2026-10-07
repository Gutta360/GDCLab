# GDCLab

GDCLab is a lightweight laboratory clone of GDC2.0. It preserves the same Angular, Spring Boot, PostgreSQL, Flyway, Docker, and Nginx deployment shape, but only the Patient module is functional.

Removed modules such as appointments, treatments, pharmacy, payments, dashboard, calendar, clinical, and reports are not present in the backend. The Angular shell keeps Patient plus placeholder tabs that render `Coming Soon`.

## Backend

```bash
cd backend
mvn spring-boot:run
```

The backend listens on port `8090`.

Default database configuration:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/gdc?currentSchema=gdclab
spring.datasource.username=gdc_user
spring.datasource.password=${DB_PASSWORD}
spring.flyway.schemas=gdclab
spring.flyway.default-schema=gdclab
```

Flyway creates and uses only the `gdclab` schema. The immutable
`V1__create_patient_module.sql` defines Patient master data;
`V2__create_diagnostic_registration_module.sql` adds the diagnostic database
foundation. See [Phase 2](database/PHASE2.md) for the schema, constraints,
all 41 seeded tests and migration verification. Diagnostic APIs and screens
will be integrated in later approved phases.

## Frontend

```bash
cd frontend
npm ci
npm start
```

For production under the Nginx `/gdclab/` path:

```bash
npm run build -- --configuration production --base-href /gdclab/
```

## Nginx

Use `docker/nginx-gdclab.conf` as an additional location block inside the existing GDC2.0 server configuration. It serves the GDCLab frontend from `/var/www/gdclab` and proxies `/gdclab/api/` to `localhost:8090`.

## Deployment

Backend:

```bash
cd backend
SERVER=user@host DB_PASSWORD=... ./deploy-backend.sh
```

Frontend:

```bash
cd frontend
SERVER=user@host ./deploy-frontend.sh
```

Install `backend/gdclab-backend.service` as the independent systemd service for GDCLab. Keep the existing GDC2.0 service unchanged.

## Docker

```bash
DB_PASSWORD=... docker compose up --build
```

The compose file starts only the GDCLab backend and points it at the existing PostgreSQL server through `host.docker.internal`, using the `gdclab` schema.
