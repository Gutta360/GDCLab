# GDCLab Phase 2 Database Foundation

The exact schema, seed rows and column definitions are in
[V2__create_diagnostic_registration_module.sql](../backend/src/main/resources/db/migration/V2__create_diagnostic_registration_module.sql).
V1 remains immutable. The repository contained only V1 before this change.
This phase adds database support only; Patient DTOs/forms and diagnostic APIs
will be integrated in later approved phases. No backend authentication or
deployment configuration changes are included.

## Tables and Relationships

| Table | Purpose and relationships |
| --- | --- |
| `patients` | Existing person master. V2 adds nullable title, Aadhaar, email and DOB, permits a null address, and accepts age 0..120. Existing mobile, identity, duplicate, soft-delete and legacy referral rules remain. DOB is checked against today's India business date; deriving age belongs to the service. |
| `branches` | Persisted bill header, address and optional contact information. A branch has many registrations. |
| `referral_sources` | Unified DOCTOR/LAB/SELF/OTHER master. Registration has separate nullable `referring_doctor_id` and `referring_lab_id`; neither means Self. Checking the expected master type belongs to Phase 3. |
| `test_categories` | Extensible category master with display order and active status. Each category has many diagnostic tests. |
| `diagnostic_tests` | Readable test code, category, current price, order and active status. Each master test may appear in many visits. |
| `registrations` | One diagnostic visit with unique Registration/Bill numbers, patient, branch, both optional referrals, financial totals and historical snapshots. One Patient has many independent registrations. |
| `registration_tests` | One distinct master test per registration. Stores historical test name, category and price, per-test discounts, business timestamps and execution status. |
| `payments` | Multiple confirmed collections per registration, each with its own number, amount, mode and payment business timestamp. |

All seven new tables have identity primary keys, created/updated timestamps,
nullable created/updated staff attribution and a version column defaulting to
zero. `received_by` and `completed_by` are also nullable. Timestamps use
`TIMESTAMP(6) WITH TIME ZONE`; audit timestamps are separate from registration,
registered-test, completed-test and payment reporting timestamps.

Registration preserves patient title/name/age/gender/mobile/optional address,
branch code/name/address/contact, independent Doctor name/qualification and
Lab name. Registration Test preserves test name, category code/name and price.
These stored values do not follow later master edits. Aadhaar is only in
Patient master as `VARCHAR(12)`; no Aadhaar snapshot, report field or barcode
image column is introduced.

## All Constraints

`NOT NULL` declarations and lengths are specified directly in V2. Optional
fields include Patient address and new Patient fields; registration patient
address; Doctor/Lab links and their corresponding snapshots; optional master
contact/qualification fields; completion timestamp/staff; payment reference,
remarks and staff attribution. Branch bill-header address remains required.

There are 57 CHECK constraints after V2: eight on Patient, and 49 on the new
tables. V2 replaces the Patient age CHECK and adds two new Patient CHECKs.
The following lists give every resulting named business constraint; the
unchanged Flyway metadata constraints are not part of this domain catalogue.

| Table | Primary, unique and foreign-key constraints |
| --- | --- |
| `patients` | Retained: `pk_patients`, `uk_patients_patient_id`. The existing unique expression index `uk_patients_first_name_mobile` also remains unchanged. |
| `branches` | `pk_branches`, `uk_branches_branch_code`. |
| `referral_sources` | `pk_referral_sources`, `uk_referral_sources_referral_code`. |
| `test_categories` | `pk_test_categories`, `uk_test_categories_category_code`. |
| `diagnostic_tests` | `pk_diagnostic_tests`, `uk_diagnostic_tests_test_code`, `fk_diagnostic_tests_category`. |
| `registrations` | `pk_registrations`, `uk_registrations_registration_no`, `uk_registrations_bill_no`, `fk_registrations_patient`, `fk_registrations_branch`, `fk_registrations_referring_doctor`, `fk_registrations_referring_lab`. |
| `registration_tests` | `pk_registration_tests`, `uk_registration_tests_registration_test` on `(registration_id,test_id)`, `fk_registration_tests_registration`, `fk_registration_tests_test`. |
| `payments` | `pk_payments`, `uk_payments_payment_no`, `fk_payments_registration`. |

All eight new foreign keys use `ON DELETE RESTRICT`. Master deactivation and
Patient soft deletion remain available without deleting historical records.

| Table | CHECK constraint names and protections |
| --- | --- |
| `patients` | Retained `chk_patients_gender`, `chk_patients_mobile`, `chk_patients_referred_by`, `chk_patients_doctor_referral`, `chk_patients_consultation_fee`; replaced `chk_patients_age` (0..120); added `chk_patients_aadhaar_no` (null or exactly 12 digits), `chk_patients_date_of_birth` (null or no later than today's `Asia/Kolkata` date). |
| `branches` | `chk_branches_code`, `chk_branches_name`, `chk_branches_address` reject blank required text; `chk_branches_mobile` validates an optional 10-digit mobile; `chk_branches_version` requires a nonnegative version. |
| `referral_sources` | `chk_referral_sources_code`, `chk_referral_sources_type`, `chk_referral_sources_name`, `chk_referral_sources_mobile`, `chk_referral_sources_version` validate nonblank text, allowed type, optional mobile and version. |
| `test_categories` | `chk_test_categories_code`, `chk_test_categories_name`, `chk_test_categories_display_order`, `chk_test_categories_version` validate nonblank text and nonnegative order/version. |
| `diagnostic_tests` | `chk_diagnostic_tests_code`, `chk_diagnostic_tests_name`, `chk_diagnostic_tests_price`, `chk_diagnostic_tests_display_order`, `chk_diagnostic_tests_version` validate code format, name, finite nonnegative price and nonnegative order/version. |
| `registrations` | `chk_registrations_registration_no`, `chk_registrations_bill_no` validate readable identifiers; `chk_registrations_patient_name`, `chk_registrations_patient_age`, `chk_registrations_patient_gender`, `chk_registrations_patient_mobile` protect patient snapshots; `chk_registrations_branch_snapshot`, `chk_registrations_doctor_snapshot`, `chk_registrations_lab_snapshot` require coherent snapshots; `chk_registrations_amounts`, `chk_registrations_discount_percentage`, `chk_registrations_discount_calculation`, `chk_registrations_final_amount`, `chk_registrations_payment_balance` protect amounts and calculations; `chk_registrations_status`, `chk_registrations_status_balance`, `chk_registrations_version` protect status/balance combinations and version. |
| `registration_tests` | `chk_registration_tests_snapshot`, `chk_registration_tests_amounts`, `chk_registration_tests_discount_percentage`, `chk_registration_tests_discount_calculation`, `chk_registration_tests_total`, `chk_registration_tests_display_order`, `chk_registration_tests_status`, `chk_registration_tests_completion`, `chk_registration_tests_version` protect snapshots, finite amounts, discounts, totals, order, status, completion timestamp consistency and version. |
| `payments` | `chk_payments_payment_no`, `chk_payments_amount`, `chk_payments_mode`, `chk_payments_version` require a readable number, finite positive amount, CASH/UPI/CARD/OTHER mode and nonnegative version. |

All monetary columns use `NUMERIC(10,2)` and are constrained to the finite
0..99,999,999.99 range; payments must be greater than zero. The upper bound
also excludes PostgreSQL numeric `NaN`. Percentages use `NUMERIC(5,2)` and
range from 0 through 100.

The row-level financial model is explicit:

```text
test discount amount = ROUND(test price * test discount percentage / 100, 2)
test total           = test price - test discount amount
bill amount          = SUM(test totals)                 [Phase 3 service]
bill discount amount = ROUND(bill amount * bill discount percentage / 100, 2)
final bill amount    = bill amount - bill discount amount
paid amount          = SUM(confirmed payment amounts)   [Phase 3 service]
due amount           = final bill amount - paid amount
```

Positive monetary rounding is equivalent to Java `BigDecimal` scale two with
`HALF_UP`. OPEN means no payment and positive due; PARTIALLY_PAID means positive
paid and due; PAID means zero due, including a fully discounted bill.
CANCELLED is an allowed registration status and retains all amount/balance
checks; operational cancellation/refund policy is reserved for later work.

REGISTERED/IN_PROGRESS/CANCELLED tests have no completion timestamp or user.
COMPLETED requires `completed_at >= registered_at`; its user may be null.
Valid transition sequencing is a future service responsibility.

CHECK constraints do not aggregate other rows. Phase 3 must enforce at least
one test, active master selection, correct Doctor/Lab master types, DOB-derived
age, server-owned snapshots/prices, child-to-parent total reconciliation,
permitted status transitions and trusted staff attribution when available.
Payment creation must lock its Registration and recompute persisted payment
totals in the same transaction to prevent concurrent overpayment. No triggers
or application business implementation have been added in this phase.

## All Indexes and Purposes

Primary and unique constraints automatically create their supporting indexes.
V1 indexes, including Patient duplicate/name/mobile indexes, remain unchanged.
The following twelve explicitly created V2 indexes cover known lookups and
business-date reporting:

| Index | Columns | Purpose |
| --- | --- | --- |
| `idx_diagnostic_tests_category_active_order` | `category_id,is_active,display_order,id` | Category/active selector and category foreign-key lookup. |
| `idx_registrations_datetime` | `registration_datetime DESC,id DESC` | Date-range registration search with a deterministic tie-breaker. |
| `idx_registrations_patient_datetime` | `patient_id,registration_datetime DESC,id DESC` | Returning-patient history and Patient foreign-key lookup. |
| `idx_registrations_branch_datetime` | `branch_id,registration_datetime DESC,id DESC` | Branch registration search and Branch foreign-key lookup. |
| `idx_registrations_referring_doctor` | `referring_doctor_id`, nonnull rows only | Doctor foreign-key checks without indexing Self/Lab-only nulls. |
| `idx_registrations_referring_lab` | `referring_lab_id`, nonnull rows only | Lab foreign-key checks without indexing Self/Doctor-only nulls. |
| `idx_registration_tests_registered_at` | `registered_at DESC,id DESC` | Registered-test business-date reporting. |
| `idx_registration_tests_completed_at` | `completed_at DESC,id DESC`, nonnull rows only | Conducted-test reporting excluding uncompleted rows. |
| `idx_registration_tests_test_registered_at` | `test_id,registered_at DESC,id DESC` | Master-test/date filtering and Test foreign-key lookup. |
| `idx_payments_datetime` | `payment_datetime DESC,id DESC` | Payment reconciliation by business date. |
| `idx_payments_registration` | `registration_id` | Summing collections under a locked Registration and foreign-key lookup. |
| `idx_payments_mode_datetime` | `payment_mode,payment_datetime DESC,id DESC` | Mode/date collection searches. |

The unique `(registration_id,test_id)` index supports registration-to-test
lookup, so a separate registration-only child index is unnecessary. A
standalone status or active boolean index is not introduced; these values have
low cardinality. Substring name search indexes can be evaluated against real
query plans in later phases without requiring extensions for this foundation.

## Sequences and Business Identifiers

| Sequence | Business ID | State after migration |
| --- | --- | --- |
| `patient_number_seq` | Existing `P-00001` convention | Retained without resetting. |
| `registration_number_seq` | `REG-00001` | First allocation is 1. |
| `bill_number_seq` | `BILL-00001` | First allocation is 1. |
| `payment_number_seq` | `PAY-00001` | First allocation is 1. |
| `diagnostic_test_number_seq` | `TST-00001` | Seeds occupy 1..41; next allocation is 42. |

All new business sequences are BIGINT, increment by one and cache one. The
seven new identity primary keys also have PostgreSQL-managed identity
sequences. Phase 3 will allocate business numbers using schema-qualified
`nextval` calls and minimum-width five-digit formatting that does not truncate
larger numbers. Unique constraints remain the database safeguard. Sequence
gaps on rollback are expected; numbers are never derived from `MAX`.

## Initial Master Data

Branch `SBD-NANDIGAMA` is SRI BALAJI DIAGNOSTICS at Govt. Hospital Road,
Nandigama - 521185, Krishna Dt. A.P. Contact numbers are null because none were
supplied. The Self referral lookup is seeded; a Self visit has both Doctor and
Lab links null. Doctor/Lab names are not invented.

All 41 tests are active. Category display orders are CT Scan 1 and Ultrasound
2; test display order follows the supplied list within each category.

| Test Code | Category | Test Name | Price (INR) |
| --- | --- | --- | ---: |
| TST-00001 | CT_SCAN | CT Brain | 3000.00 |
| TST-00002 | CT_SCAN | CECT Brain | 4500.00 |
| TST-00003 | CT_SCAN | CT PNS | 3500.00 |
| TST-00004 | CT_SCAN | CT Facial Bones | 4000.00 |
| TST-00005 | CT_SCAN | CT Temporal Bones | 4000.00 |
| TST-00006 | CT_SCAN | CT Neck | 4000.00 |
| TST-00007 | CT_SCAN | CECT Neck | 5500.00 |
| TST-00008 | CT_SCAN | CT Chest (Accident Case) | 5000.00 |
| TST-00009 | CT_SCAN | HRCT Chest | 4000.00 |
| TST-00010 | CT_SCAN | CECT Chest | 7000.00 |
| TST-00011 | CT_SCAN | CT Abdomen | 5000.00 |
| TST-00012 | CT_SCAN | CECT Abdomen (Biphasic) | 7000.00 |
| TST-00013 | CT_SCAN | CECT Abdomen (Triple Phase) | 9000.00 |
| TST-00014 | CT_SCAN | CT KUB | 4500.00 |
| TST-00015 | CT_SCAN | CT Urogram | 5500.00 |
| TST-00016 | CT_SCAN | CT Joints (Foot, Ankle, Knee, Hip, Spine, Wrist, Elbow, Shoulder) | 4000.00 |
| TST-00017 | CT_SCAN | CT Whole Spine | 7000.00 |
| TST-00018 | CT_SCAN | CT Denta Scan | 3000.00 |
| TST-00019 | CT_SCAN | CT Angiograms | 10000.00 |
| TST-00020 | ULTRASOUND | Abdomen & Pelvis | 1000.00 |
| TST-00021 | ULTRASOUND | Pelvis (TVS) | 1000.00 |
| TST-00022 | ULTRASOUND | Pregnancy (OBG) | 1000.00 |
| TST-00023 | ULTRASOUND | Pregnancy (OBG) Twins | 2000.00 |
| TST-00024 | ULTRASOUND | TIFFA | 2000.00 |
| TST-00025 | ULTRASOUND | Twins TIFFA | 3000.00 |
| TST-00026 | ULTRASOUND | Fetal Doppler (Twins) | 3000.00 |
| TST-00027 | ULTRASOUND | Fetal Doppler With OBG | 2000.00 |
| TST-00028 | ULTRASOUND | NT Scan | 1600.00 |
| TST-00029 | ULTRASOUND | Follicular Study | 2000.00 |
| TST-00030 | ULTRASOUND | AFI | 500.00 |
| TST-00031 | ULTRASOUND | Neck | 1600.00 |
| TST-00032 | ULTRASOUND | Thyroid | 1600.00 |
| TST-00033 | ULTRASOUND | Soft Parts | 1600.00 |
| TST-00034 | ULTRASOUND | Neuro Sonogram | 1600.00 |
| TST-00035 | ULTRASOUND | Scrotum | 1600.00 |
| TST-00036 | ULTRASOUND | Doppler Arterial | 2000.00 |
| TST-00037 | ULTRASOUND | Doppler Venous | 2000.00 |
| TST-00038 | ULTRASOUND | Single Limb Both Study (A+V) | 3500.00 |
| TST-00039 | ULTRASOUND | Renal Doppler | 2000.00 |
| TST-00040 | ULTRASOUND | Carotid Doppler | 2000.00 |
| TST-00041 | ULTRASOUND | Review Scan (1 Week) | 500.00 |

The CT Scan catalogue totals 99,500.00 across 19 tests. Ultrasound totals
39,100.00 across 22 tests. Seed inserts are executed once under Flyway's
versioned migration history. Future master corrections use a later migration
or an approved administration flow; an applied V2 must not be edited.

## Verification

The shared acceptance suite is
[diagnostic_foundation.sql](../backend/src/test/resources/db/verification/diagnostic_foundation.sql).
It checks every supplied test/code/category/price/order, schema placement,
constraints/indexes/sequences, newborn/optional-address/identification rules,
legacy duplicate behavior, all referral combinations, repeated visits and
multiple distinct tests per visit,
discount rounding, valid/invalid balances and status combinations, multiple
payment modes, completion timestamps, historical snapshots, restrictive
foreign keys, date boundaries and aggregation/pagination inputs. Its fixtures
and temporary assertion functions are rolled back.

[DiagnosticMigrationTest.java](../backend/src/test/java/com/gdc/backend/database/DiagnosticMigrationTest.java)
runs Flyway against an isolated PostgreSQL 16 Testcontainer. It checks a V1
upgrade with an existing Patient, unchanged V1 checksum/data, a fresh install,
validation, a no-op second migration, the shared SQL acceptance suite, and
unique sequence allocation across concurrent connections. It never connects
to application or production databases and does not start Spring Boot.

Run from `backend`:

```bash
mvn test
```

If Docker is unavailable, a PostgreSQL WASM execution check can run without
changing the frontend/backend package dependencies. From the repository root:

```bash
npm install --prefix backend/target/phase2-verification --no-audit --no-fund --ignore-scripts @electric-sql/pglite@0.5.8
node database/tests/verify-v2.mjs
```

The runtime is installed only under ignored `backend/target`; each execution
uses two disposable in-memory databases. It applies V1 and V2 transactionally,
checks original Patient columns on upgrade, and runs the same SQL suite on
fresh and upgraded databases. This does not substitute for Flyway/JDBC
verification or multi-connection concurrency tests.

Local verification on 2026-10-06:

- PostgreSQL 18.3 through PGlite: fresh install and existing-Patient upgrade
  both passed, including all 41 exact seed rows and the shared acceptance suite.
- `mvn test`: compilation/build passed; six tests discovered, one passed and
  five skipped because Docker is unavailable. Four skips are the new native
  migration tests; the fifth is the existing application-context test.
- V1 is unchanged. No live/deployed database was migrated or inspected.
- Doctor/Lab type checks, transaction-level financial aggregation/locking,
  authentication and application integration have not been implemented.

Phase 2 ends here; Phase 3 requires separate review and approval.
