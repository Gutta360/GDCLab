# GDCLab

GDCLab is a diagnostic registration, testing, billing and payments application built with Angular, Spring Boot, PostgreSQL and Flyway.

Each Registration is an independent diagnostic visit and directly owns its person details. There is no separate person master or cross-visit identity record. The same name or mobile number may appear on any number of registrations.

## Backend

```bash
cd backend
mvn spring-boot:run
```

The backend listens on port `8090` and uses PostgreSQL database `gdc`, schema `gdclab`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/gdc?currentSchema=gdclab
spring.datasource.username=gdc_user
spring.datasource.password=${DB_PASSWORD}
spring.flyway.schemas=gdclab
spring.flyway.default-schema=gdclab
spring.jpa.hibernate.ddl-auto=validate
```

Flyway has one clean-install migration:

```text
backend/src/main/resources/db/migration/V1__create_diagnostic_registration_module.sql
```

It creates Branch, Referral Source, Test Category, Diagnostic Test, Registration, Registration Test and Payment storage and seeds 19 CT Scan plus 22 Ultrasound tests.

## Frontend

```bash
cd frontend
npm ci
npm start
```

Production build for the Nginx `/gdclab/` path:

```bash
npm run build -- --configuration production --base-href /gdclab/
```

Navigation is Home, Registration, Tests, Payments and Logout. New Registration captures person details directly before referral, test, billing and payment details.

## Deployment

Repository deployment helpers target only GDCLab. The backend readiness endpoint is `GET /api/v1/branches`. See [database/PHASE2.md](database/PHASE2.md) for the clean schema and verification workflow.

GDCLab shares the `gdc` database with other applications. Never drop the database or the `public` schema. A foundation reset must drop and recreate only schema `gdclab` while the GDCLab backend is stopped.
