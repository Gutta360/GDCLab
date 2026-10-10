# GDCLab Database Foundation

The schema source of truth is
[V1__create_diagnostic_registration_module.sql](../backend/src/main/resources/db/migration/V1__create_diagnostic_registration_module.sql).
It is a clean-install migration for an empty `gdclab` schema. It is intentionally not compatible with earlier Flyway history.

## Data Model

| Table | Purpose |
| --- | --- |
| `branches` | Active branch master and bill-header contact data. |
| `referral_sources` | DOCTOR, LAB, SELF and OTHER referral masters. |
| `test_categories` | Ordered active diagnostic categories. |
| `diagnostic_tests` | Test code, category, current price and display order. |
| `registrations` | Independent diagnostic visits, including directly owned person details, referral links, branch/referral snapshots and financial totals. |
| `registration_tests` | Selected tests with immutable name/category/price snapshots, discounts and lifecycle status. |
| `payments` | Confirmed collections against a registration. |

Registration directly stores `patient_title`, `patient_name`, `patient_mobile`, optional `patient_aadhaar`, `patient_gender`, optional `patient_email`, optional `patient_date_of_birth`, `patient_age` and optional `patient_address`. These values are authoritative for that visit. Names, mobile numbers and Aadhaar values are not unique, so repeat visits remain independent records.

Branch, referral and test values continue to use snapshots where historical bill/report integrity requires protection from later master edits. Aadhaar is excluded from bill DTOs and templates.

## Sequences

- `registration_number_seq`
- `bill_number_seq`
- `payment_number_seq`
- `diagnostic_test_number_seq`

## Integrity Rules

- Registration and bill numbers use `REG-00001` and `BILL-00001` formats.
- Person name is required; mobile is exactly 10 digits.
- Aadhaar is optional and exactly 12 digits when supplied.
- Gender is `MALE`, `FEMALE` or `OTHER`; age is 0 through 120; DOB cannot be in the future in the India business timezone.
- Branch and selected diagnostic tests are foreign-key protected with `ON DELETE RESTRICT`.
- Doctor and Lab references must have coherent snapshots.
- Diagnostic tests are unique within one registration.
- Test and bill discounts are 0 through 100 percent and persisted totals must match rounded calculations.
- Registration paid/due amounts and OPEN/PARTIALLY_PAID/PAID states must remain coherent.
- COMPLETED tests require a completion timestamp no earlier than registration time.
- Payments are positive, have unique `PAY-00001` numbers and use CASH, UPI, CARD or OTHER.

Useful indexes cover registration date, branch/date, person mobile/date, normalized person name, referral IDs, test lifecycle dates and payment date/mode/registration access paths.

## Seed Catalogue

The migration seeds one branch, SELF referral, two categories and 41 diagnostic tests:

- `CT_SCAN`: 19
- `ULTRASOUND`: 22

## Verification

The native PostgreSQL suite runs with the normal backend tests when Docker is available:

```bash
cd backend
mvn clean test
```

The portable fresh-schema verification uses PGlite:

```bash
npm install --prefix backend/target/foundation-verification --no-audit --no-fund --ignore-scripts @electric-sql/pglite@0.5.8
node database/tests/verify-foundation.mjs
```

Both mechanisms apply the sole V1 to an empty schema and verify table inventory, absence of obsolete person-master objects, 19/22/41 seed counts, repeat visits, registration person validation, referral snapshots, test constraints, payment constraints and restrictive foreign keys.

## Reset Boundary

GDCLab uses PostgreSQL database `gdc` and schema `gdclab`. A foundation reset must stop the GDCLab backend and replace only schema `gdclab`. Never drop database `gdc`, schema `public`, another application's schema, or any PostgreSQL volume.
