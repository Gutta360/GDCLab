BEGIN;

CREATE FUNCTION pg_temp.assert_true(condition BOOLEAN, description TEXT) RETURNS VOID
LANGUAGE plpgsql AS $function$
BEGIN
    IF condition IS DISTINCT FROM TRUE THEN
        RAISE EXCEPTION 'Database acceptance check failed: %', description;
    END IF;
END;
$function$;

CREATE FUNCTION pg_temp.expect_rejected(statement TEXT, expected_states TEXT[]) RETURNS VOID
LANGUAGE plpgsql AS $function$
DECLARE
    actual_state TEXT;
    rejected BOOLEAN := FALSE;
BEGIN
    BEGIN
        EXECUTE statement;
    EXCEPTION WHEN OTHERS THEN
        GET STACKED DIAGNOSTICS actual_state = RETURNED_SQLSTATE;
        PERFORM pg_temp.assert_true(actual_state = ANY(expected_states),
            'expected SQLSTATE ' || array_to_string(expected_states, ' or ') || ', received ' || actual_state);
        rejected := TRUE;
    END;
    PERFORM pg_temp.assert_true(rejected, 'expected rejection with SQLSTATE ' || array_to_string(expected_states, ' or '));
END;
$function$;

CREATE FUNCTION pg_temp.expect_rejected(statement TEXT, expected_state TEXT) RETURNS VOID
LANGUAGE SQL AS $function$
    SELECT pg_temp.expect_rejected(statement, ARRAY[expected_state]);
$function$;

DO $verify$
DECLARE
    expected_indexes TEXT[] := ARRAY[
        'idx_diagnostic_tests_category_active_order',
        'idx_registrations_datetime', 'idx_registrations_patient_datetime',
        'idx_registrations_branch_datetime', 'idx_registrations_referring_doctor',
        'idx_registrations_referring_lab', 'idx_registration_tests_registered_at',
        'idx_registration_tests_completed_at', 'idx_registration_tests_test_registered_at',
        'idx_payments_datetime', 'idx_payments_registration', 'idx_payments_mode_datetime'
    ];
    new_tables TEXT[] := ARRAY[
        'branches', 'referral_sources', 'test_categories', 'diagnostic_tests',
        'registrations', 'registration_tests', 'payments'
    ];
BEGIN
    PERFORM pg_temp.assert_true((
        SELECT COUNT(*) = 7 FROM information_schema.tables
        WHERE table_schema = 'gdclab' AND table_name = ANY(new_tables)
    ), 'all seven new tables exist in gdclab');
    PERFORM pg_temp.assert_true((
        SELECT COUNT(*) = 0 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = ANY(new_tables)
    ), 'business tables are not created in public');
    PERFORM pg_temp.assert_true((
        SELECT COUNT(*) = 7 FROM information_schema.columns
        WHERE table_schema = 'gdclab' AND table_name = ANY(new_tables)
          AND column_name = 'id' AND is_identity = 'YES' AND identity_generation = 'BY DEFAULT'
    ), 'identity primary keys follow Patient conventions');
    PERFORM pg_temp.assert_true((
        SELECT COUNT(*) = 0 FROM information_schema.columns
        WHERE table_schema = 'gdclab' AND table_name = ANY(new_tables)
          AND column_name LIKE '%aadhaar%'
    ), 'identification is confined to Patient master');
    PERFORM pg_temp.assert_true((
        SELECT COUNT(*) = 16 AND BOOL_AND(is_nullable = 'YES')
        FROM information_schema.columns
        WHERE table_schema = 'gdclab' AND table_name = ANY(new_tables)
          AND column_name IN ('created_by', 'updated_by', 'received_by', 'completed_by')
    ), 'staff attribution is nullable without backend authentication');
    PERFORM pg_temp.assert_true((
        SELECT COUNT(*) = 8 AND BOOL_AND(confdeltype = 'r')
        FROM pg_constraint WHERE connamespace = 'gdclab'::regnamespace AND contype = 'f'
    ), 'all eight relationships restrict destructive deletion');
    PERFORM pg_temp.assert_true((
        SELECT COUNT(*) = 57 FROM pg_constraint
        WHERE connamespace = 'gdclab'::regnamespace AND contype = 'c'
    ), 'all Patient and diagnostic CHECK constraints exist');
    PERFORM pg_temp.assert_true((
        SELECT COUNT(*) = 9 FROM pg_constraint
        WHERE connamespace = 'gdclab'::regnamespace AND contype = 'u'
    ), 'Patient and new business uniqueness constraints exist');
    PERFORM pg_temp.assert_true((
        SELECT COUNT(*) = CARDINALITY(expected_indexes) FROM pg_indexes
        WHERE schemaname = 'gdclab' AND indexname = ANY(expected_indexes)
    ), 'all twelve reporting and foreign-key indexes exist');
    PERFORM pg_temp.assert_true((
        SELECT COUNT(*) = 4 AND BOOL_AND(start_value = 1 AND increment_by = 1 AND cache_size = 1)
        FROM pg_sequences WHERE schemaname = 'gdclab' AND sequencename IN (
            'registration_number_seq', 'bill_number_seq', 'payment_number_seq', 'diagnostic_test_number_seq'
        )
    ), 'all four concurrency-safe business sequences exist');
    PERFORM pg_temp.assert_true((
        SELECT last_value = 41 AND is_called FROM gdclab.diagnostic_test_number_seq
    ), 'future Test Master numbering starts after the 41 seed codes');
END;
$verify$;

DO $verify$
BEGIN
    PERFORM pg_temp.assert_true((SELECT COUNT(*) = 41 FROM gdclab.diagnostic_tests), 'exactly 41 tests');
    PERFORM pg_temp.assert_true((SELECT COUNT(*) = 2 FROM gdclab.test_categories), 'two initial categories');
    PERFORM pg_temp.assert_true((
        WITH expected(test_number, category_code, test_name, current_price, display_order) AS (VALUES
            (1, 'CT_SCAN', 'CT Brain', 3000.00, 1),
            (2, 'CT_SCAN', 'CECT Brain', 4500.00, 2),
            (3, 'CT_SCAN', 'CT PNS', 3500.00, 3),
            (4, 'CT_SCAN', 'CT Facial Bones', 4000.00, 4),
            (5, 'CT_SCAN', 'CT Temporal Bones', 4000.00, 5),
            (6, 'CT_SCAN', 'CT Neck', 4000.00, 6),
            (7, 'CT_SCAN', 'CECT Neck', 5500.00, 7),
            (8, 'CT_SCAN', 'CT Chest (Accident Case)', 5000.00, 8),
            (9, 'CT_SCAN', 'HRCT Chest', 4000.00, 9),
            (10, 'CT_SCAN', 'CECT Chest', 7000.00, 10),
            (11, 'CT_SCAN', 'CT Abdomen', 5000.00, 11),
            (12, 'CT_SCAN', 'CECT Abdomen (Biphasic)', 7000.00, 12),
            (13, 'CT_SCAN', 'CECT Abdomen (Triple Phase)', 9000.00, 13),
            (14, 'CT_SCAN', 'CT KUB', 4500.00, 14),
            (15, 'CT_SCAN', 'CT Urogram', 5500.00, 15),
            (16, 'CT_SCAN', 'CT Joints (Foot, Ankle, Knee, Hip, Spine, Wrist, Elbow, Shoulder)', 4000.00, 16),
            (17, 'CT_SCAN', 'CT Whole Spine', 7000.00, 17),
            (18, 'CT_SCAN', 'CT Denta Scan', 3000.00, 18),
            (19, 'CT_SCAN', 'CT Angiograms', 10000.00, 19),
            (20, 'ULTRASOUND', 'Abdomen & Pelvis', 1000.00, 1),
            (21, 'ULTRASOUND', 'Pelvis (TVS)', 1000.00, 2),
            (22, 'ULTRASOUND', 'Pregnancy (OBG)', 1000.00, 3),
            (23, 'ULTRASOUND', 'Pregnancy (OBG) Twins', 2000.00, 4),
            (24, 'ULTRASOUND', 'TIFFA', 2000.00, 5),
            (25, 'ULTRASOUND', 'Twins TIFFA', 3000.00, 6),
            (26, 'ULTRASOUND', 'Fetal Doppler (Twins)', 3000.00, 7),
            (27, 'ULTRASOUND', 'Fetal Doppler With OBG', 2000.00, 8),
            (28, 'ULTRASOUND', 'NT Scan', 1600.00, 9),
            (29, 'ULTRASOUND', 'Follicular Study', 2000.00, 10),
            (30, 'ULTRASOUND', 'AFI', 500.00, 11),
            (31, 'ULTRASOUND', 'Neck', 1600.00, 12),
            (32, 'ULTRASOUND', 'Thyroid', 1600.00, 13),
            (33, 'ULTRASOUND', 'Soft Parts', 1600.00, 14),
            (34, 'ULTRASOUND', 'Neuro Sonogram', 1600.00, 15),
            (35, 'ULTRASOUND', 'Scrotum', 1600.00, 16),
            (36, 'ULTRASOUND', 'Doppler Arterial', 2000.00, 17),
            (37, 'ULTRASOUND', 'Doppler Venous', 2000.00, 18),
            (38, 'ULTRASOUND', 'Single Limb Both Study (A+V)', 3500.00, 19),
            (39, 'ULTRASOUND', 'Renal Doppler', 2000.00, 20),
            (40, 'ULTRASOUND', 'Carotid Doppler', 2000.00, 21),
            (41, 'ULTRASOUND', 'Review Scan (1 Week)', 500.00, 22)
        )
        SELECT COUNT(*) = 41
        FROM expected e
        JOIN gdclab.diagnostic_tests t ON t.test_code = 'TST-' || LPAD(e.test_number::TEXT, 5, '0')
        JOIN gdclab.test_categories c ON c.id = t.category_id
        WHERE c.category_code = e.category_code AND t.test_name = e.test_name
          AND t.current_price = e.current_price AND t.display_order = e.display_order
          AND t.is_active AND c.is_active AND t.version = 0
    ), 'all approved names, codes, categories, prices and display orders match');
    PERFORM pg_temp.assert_true((
        SELECT COUNT(*) = 1 FROM gdclab.branches
        WHERE branch_code = 'SBD-NANDIGAMA' AND branch_name = 'SRI BALAJI DIAGNOSTICS'
          AND phone IS NULL AND mobile IS NULL AND is_active
    ), 'branch is persisted without invented contact numbers');
    PERFORM pg_temp.assert_true((
        SELECT COUNT(*) = 1 FROM gdclab.referral_sources
        WHERE referral_code = 'SELF' AND referral_type = 'SELF' AND name = 'Self'
    ), 'Self lookup exists without requiring referral links on a visit');
END;
$verify$;

DO $verify$
DECLARE
    person_id BIGINT;
    branch_id BIGINT;
    doctor_id BIGINT;
    lab_id BIGINT;
    visit_id BIGINT;
    line_id BIGINT;
    master_test_id BIGINT;
    category_id BIGINT;
    combination INTEGER;
    instant TIMESTAMPTZ;
    instants TIMESTAMPTZ[] := ARRAY[
        TIMESTAMPTZ '2026-10-01 00:00:00+05:30',
        TIMESTAMPTZ '2026-09-30 23:59:59.999999+05:30',
        TIMESTAMPTZ '2026-10-02 00:00:00+05:30',
        TIMESTAMPTZ '2026-10-01 23:59:59.999999+05:30'
    ];
BEGIN
    INSERT INTO gdclab.patients (
        patient_id, first_name, last_name, gender, age, mobile, referred_by,
        consultation_fee, registration_date, aadhaar_no
    ) VALUES (
        'P-95000', 'Database', 'Fixture', 'FEMALE', 0, '7777770010', 'SELF',
        0.00, TIMESTAMPTZ '2026-10-01 00:00:00+05:30', '012345678901'
    ) RETURNING id INTO person_id;

    PERFORM pg_temp.assert_true((
        SELECT age = 0 AND address IS NULL AND aadhaar_no = '012345678901'
        FROM gdclab.patients WHERE id = person_id
    ), 'newborn, optional address and leading zero identification');
    PERFORM pg_temp.expect_rejected(format('UPDATE gdclab.patients SET age = -1 WHERE id = %s', person_id), '23514');
    PERFORM pg_temp.expect_rejected(format('UPDATE gdclab.patients SET age = 121 WHERE id = %s', person_id), '23514');
    PERFORM pg_temp.expect_rejected(format('UPDATE gdclab.patients SET aadhaar_no = %L WHERE id = %s', '01234567890', person_id), '23514');
    PERFORM pg_temp.expect_rejected(format('UPDATE gdclab.patients SET aadhaar_no = %L WHERE id = %s', '0123456789012', person_id), '22001');
    PERFORM pg_temp.expect_rejected(format('UPDATE gdclab.patients SET aadhaar_no = %L WHERE id = %s', '1234ABCD9012', person_id), '23514');
    PERFORM pg_temp.expect_rejected(format('UPDATE gdclab.patients SET aadhaar_no = %L WHERE id = %s', '', person_id), '23514');
    PERFORM pg_temp.expect_rejected(format('UPDATE gdclab.patients SET mobile = NULL WHERE id = %s', person_id), '23502');
    PERFORM pg_temp.expect_rejected(format('UPDATE gdclab.patients SET mobile = %L WHERE id = %s', '777777001', person_id), '23514');
    PERFORM pg_temp.expect_rejected(format(
        'UPDATE gdclab.patients SET date_of_birth = (CURRENT_TIMESTAMP AT TIME ZONE ''Asia/Kolkata'')::DATE + 1 WHERE id = %s', person_id), '23514');
    UPDATE gdclab.patients SET date_of_birth = (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Kolkata')::DATE
    WHERE id = person_id;
    UPDATE gdclab.patients SET date_of_birth = NULL, aadhaar_no = NULL WHERE id = person_id;
    PERFORM pg_temp.assert_true((SELECT aadhaar_no IS NULL FROM gdclab.patients WHERE id = person_id), 'identification is optional');
    UPDATE gdclab.patients SET is_active = FALSE WHERE id = person_id;
    PERFORM pg_temp.expect_rejected($sql$
        INSERT INTO gdclab.patients (
            patient_id, first_name, gender, age, mobile, referred_by, consultation_fee, registration_date
        ) VALUES ('P-95001', ' database ', 'FEMALE', 0, '7777770010', 'SELF', 0.00, CURRENT_TIMESTAMP)
    $sql$, '23505');
    UPDATE gdclab.patients SET is_active = TRUE WHERE id = person_id;

    SELECT id INTO branch_id FROM gdclab.branches WHERE branch_code = 'SBD-NANDIGAMA';
    UPDATE gdclab.branches SET address = 'Original branch address' WHERE id = branch_id;
    INSERT INTO gdclab.referral_sources (referral_code, referral_type, name, qualification)
    VALUES ('DOC-FIXTURE', 'DOCTOR', 'Dr Original', 'MBBS, DGO') RETURNING id INTO doctor_id;
    INSERT INTO gdclab.referral_sources (referral_code, referral_type, name)
    VALUES ('LAB-FIXTURE', 'LAB', 'Original Lab') RETURNING id INTO lab_id;

    FOR combination IN 0..3 LOOP
        INSERT INTO gdclab.registrations (
            registration_no, bill_no, patient_id, branch_id, registration_datetime,
            referring_doctor_id, referring_lab_id, patient_name_snapshot, patient_age_snapshot,
            patient_gender_snapshot, patient_mobile_snapshot, branch_code_snapshot,
            branch_name_snapshot, branch_address_snapshot, referring_doctor_name_snapshot,
            referring_doctor_qualification_snapshot, referring_lab_name_snapshot,
            bill_amount, final_bill_amount, due_amount
        ) VALUES (
            'REG-' || (95000 + combination)::TEXT, 'BILL-' || (95000 + combination)::TEXT,
            person_id, branch_id, instants[1],
            CASE WHEN combination IN (1, 3) THEN doctor_id END,
            CASE WHEN combination IN (2, 3) THEN lab_id END,
            'Database Fixture', 0, 'FEMALE', '7777770010', 'SBD-NANDIGAMA',
            'SRI BALAJI DIAGNOSTICS', 'Original branch address',
            CASE WHEN combination IN (1, 3) THEN 'Dr Original' END,
            CASE WHEN combination IN (1, 3) THEN 'MBBS, DGO' END,
            CASE WHEN combination IN (2, 3) THEN 'Original Lab' END,
            1000.00, 1000.00, 1000.00
        ) RETURNING id INTO visit_id;
    END LOOP;
    PERFORM pg_temp.assert_true((SELECT COUNT(*) = 4 FROM gdclab.registrations WHERE patient_id = person_id), 'four referral combinations reuse one Patient');
    PERFORM pg_temp.assert_true((
        SELECT patient_address_snapshot IS NULL AND created_by IS NULL AND updated_by IS NULL
        FROM gdclab.registrations WHERE id = visit_id
    ), 'registration address and staff attribution remain optional');
    PERFORM pg_temp.expect_rejected(format('UPDATE gdclab.registrations SET referring_doctor_name_snapshot = NULL WHERE id = %s', visit_id), '23514');
    PERFORM pg_temp.expect_rejected(format('UPDATE gdclab.registrations SET referring_lab_name_snapshot = NULL WHERE id = %s', visit_id), '23514');
    PERFORM pg_temp.expect_rejected(format('UPDATE gdclab.registrations SET patient_age_snapshot = -1 WHERE id = %s', visit_id), '23514');
    PERFORM pg_temp.expect_rejected(format('UPDATE gdclab.registrations SET patient_mobile_snapshot = NULL WHERE id = %s', visit_id), '23502');
    PERFORM pg_temp.expect_rejected(format('UPDATE gdclab.registrations SET registration_no = %L WHERE id = %s', 'REG-95000', visit_id), '23505');
    PERFORM pg_temp.expect_rejected(format('UPDATE gdclab.registrations SET bill_no = %L WHERE id = %s', 'BILL-95000', visit_id), '23505');

    SELECT id, diagnostic_tests.category_id INTO master_test_id, category_id
    FROM gdclab.diagnostic_tests WHERE test_code = 'TST-00022';
    INSERT INTO gdclab.registration_tests (
        registration_id, test_id, test_name_snapshot, category_code_snapshot,
        category_name_snapshot, test_price, total_amount, registered_at
    ) VALUES (
        visit_id, master_test_id, 'Pregnancy (OBG)', 'ULTRASOUND', 'Ultrasound',
        1000.00, 1000.00, instants[1]
    ) RETURNING id INTO line_id;
    PERFORM pg_temp.expect_rejected(format($sql$
        INSERT INTO gdclab.registration_tests (
            registration_id, test_id, test_name_snapshot, category_code_snapshot,
            category_name_snapshot, test_price, total_amount, registered_at
        ) SELECT registration_id, test_id, test_name_snapshot, category_code_snapshot,
                 category_name_snapshot, test_price, total_amount, registered_at
          FROM gdclab.registration_tests WHERE id = %s
    $sql$, line_id), '23505');
    PERFORM pg_temp.expect_rejected(format('UPDATE gdclab.registration_tests SET discount_percentage = 101 WHERE id = %s', line_id), '23514');
    PERFORM pg_temp.expect_rejected(format('UPDATE gdclab.registration_tests SET total_amount = 900 WHERE id = %s', line_id), '23514');
    PERFORM pg_temp.expect_rejected(format('UPDATE gdclab.registration_tests SET test_price = ''NaN'' WHERE id = %s', line_id), '23514');
    UPDATE gdclab.registration_tests SET discount_percentage = 10.00, discount_amount = 100.00, total_amount = 900.00
    WHERE id = line_id;
    UPDATE gdclab.registrations SET bill_amount = 900.00, discount_percentage = 5.00,
        discount_amount = 45.00, final_bill_amount = 855.00, due_amount = 855.00 WHERE id = visit_id;
    PERFORM pg_temp.assert_true((SELECT final_bill_amount = 855.00 FROM gdclab.registrations WHERE id = visit_id), 'test and bill discounts apply once each');
    UPDATE gdclab.registration_tests SET test_price = 333.33, discount_percentage = 12.34,
        discount_amount = 41.13, total_amount = 292.20 WHERE id = line_id;
    PERFORM pg_temp.assert_true((SELECT total_amount = 292.20 FROM gdclab.registration_tests WHERE id = line_id), 'two-decimal HALF_UP rounding');
    UPDATE gdclab.registration_tests SET test_price = 0.05, discount_percentage = 10.00,
        discount_amount = 0.01, total_amount = 0.04 WHERE id = line_id;
    PERFORM pg_temp.assert_true((SELECT discount_amount = 0.01 FROM gdclab.registration_tests WHERE id = line_id), 'half-cent discount rounds up');
    UPDATE gdclab.registration_tests SET test_price = 1000.00, discount_percentage = 0.00,
        discount_amount = 0.00, total_amount = 1000.00 WHERE id = line_id;
    UPDATE gdclab.registrations SET bill_amount = 1000.00, discount_percentage = 0.00,
        discount_amount = 0.00, final_bill_amount = 1000.00, due_amount = 1000.00 WHERE id = visit_id;
    PERFORM pg_temp.expect_rejected(format('UPDATE gdclab.registrations SET discount_percentage = -1 WHERE id = %s', visit_id), '23514');
    PERFORM pg_temp.expect_rejected(format('UPDATE gdclab.registrations SET discount_percentage = 101 WHERE id = %s', visit_id), '23514');
    PERFORM pg_temp.expect_rejected(format('UPDATE gdclab.registrations SET discount_amount = 100 WHERE id = %s', visit_id), '23514');
    PERFORM pg_temp.expect_rejected(format('UPDATE gdclab.registrations SET final_bill_amount = 900 WHERE id = %s', visit_id), '23514');
    PERFORM pg_temp.expect_rejected(format('UPDATE gdclab.registrations SET due_amount = 900 WHERE id = %s', visit_id), '23514');
    PERFORM pg_temp.expect_rejected(format('UPDATE gdclab.registrations SET status = ''PAID'' WHERE id = %s', visit_id), '23514');
    PERFORM pg_temp.expect_rejected(format('UPDATE gdclab.registrations SET bill_amount = ''NaN'' WHERE id = %s', visit_id), '23514');
    PERFORM pg_temp.expect_rejected(format('UPDATE gdclab.registrations SET paid_amount = 1000.01, due_amount = -0.01, status = ''PAID'' WHERE id = %s', visit_id), '23514');
    UPDATE gdclab.registrations SET paid_amount = 400.00, due_amount = 600.00, status = 'PARTIALLY_PAID' WHERE id = visit_id;
    UPDATE gdclab.registrations SET paid_amount = 1000.00, due_amount = 0.00, status = 'PAID' WHERE id = visit_id;
    UPDATE gdclab.registrations SET discount_percentage = 100.00, discount_amount = 1000.00,
        final_bill_amount = 0.00, paid_amount = 0.00, due_amount = 0.00 WHERE id = visit_id;
    PERFORM pg_temp.assert_true((SELECT status = 'PAID' AND due_amount = 0.00 FROM gdclab.registrations WHERE id = visit_id), 'fully discounted bill may be PAID without collection');
    UPDATE gdclab.registrations SET discount_percentage = 0.00, discount_amount = 0.00,
        final_bill_amount = 1000.00, due_amount = 1000.00, status = 'OPEN' WHERE id = visit_id;

    PERFORM pg_temp.expect_rejected(format('UPDATE gdclab.registration_tests SET status = ''COMPLETED'' WHERE id = %s', line_id), '23514');
    UPDATE gdclab.registration_tests SET status = 'IN_PROGRESS' WHERE id = line_id;
    UPDATE gdclab.registration_tests SET status = 'COMPLETED', completed_at = registered_at + INTERVAL '1 hour' WHERE id = line_id;
    PERFORM pg_temp.assert_true((SELECT completed_by IS NULL FROM gdclab.registration_tests WHERE id = line_id), 'completion does not require a fake authenticated user');
    PERFORM pg_temp.expect_rejected(format('UPDATE gdclab.registration_tests SET completed_at = registered_at - INTERVAL ''1 second'' WHERE id = %s', line_id), '23514');
    PERFORM pg_temp.expect_rejected(format('UPDATE gdclab.registration_tests SET status = ''REGISTERED'' WHERE id = %s', line_id), '23514');
    PERFORM pg_temp.expect_rejected(format('UPDATE gdclab.registration_tests SET status = ''UNKNOWN'' WHERE id = %s', line_id), '23514');
    INSERT INTO gdclab.registration_tests (
        registration_id, test_id, test_name_snapshot, category_code_snapshot, category_name_snapshot,
        test_price, total_amount, registered_at, status
    ) SELECT r.id, master_test_id, 'Pregnancy (OBG)', 'ULTRASOUND', 'Ultrasound',
             1000.00, 1000.00, instants[1], 'CANCELLED'
      FROM gdclab.registrations r WHERE r.registration_no = 'REG-95000';
    PERFORM pg_temp.assert_true((
        SELECT COUNT(*) = 2 AND COUNT(completed_at) = 1 FROM gdclab.registration_tests
        WHERE test_id = master_test_id
    ), 'registered and conducted tests are distinct');

    INSERT INTO gdclab.registration_tests (
        registration_id, test_id, test_name_snapshot, category_code_snapshot,
        category_name_snapshot, test_price, total_amount, registered_at
    ) SELECT r.id, t.id, t.test_name, c.category_code, c.category_name,
             t.current_price, t.current_price, instants[1]
      FROM gdclab.registrations r
      CROSS JOIN gdclab.diagnostic_tests t
      JOIN gdclab.test_categories c ON c.id = t.category_id
      WHERE r.registration_no = 'REG-95001' AND t.test_code IN ('TST-00001', 'TST-00022');
    UPDATE gdclab.registrations SET bill_amount = 4000.00, final_bill_amount = 4000.00,
        due_amount = 4000.00 WHERE registration_no = 'REG-95001';
    PERFORM pg_temp.assert_true((
        SELECT COUNT(*) = 2 AND SUM(total_amount) = 4000.00
        FROM gdclab.registration_tests rt JOIN gdclab.registrations r ON r.id = rt.registration_id
        WHERE r.registration_no = 'REG-95001'
    ), 'one visit supports multiple distinct tests from different categories');

    INSERT INTO gdclab.payments (payment_no, registration_id, amount, payment_mode, payment_datetime)
    VALUES ('PAY-95000', visit_id, 200.00, 'CASH', instants[1]),
           ('PAY-95001', visit_id, 300.00, 'UPI', instants[1]),
           ('PAY-95002', visit_id, 100.00, 'CARD', instants[1]),
           ('PAY-95003', visit_id, 400.00, 'OTHER', instants[1]);
    PERFORM pg_temp.assert_true((
        SELECT COUNT(*) = 4 AND SUM(amount) = 1000.00
           AND SUM(amount) FILTER (WHERE payment_mode = 'CASH') = 200.00
           AND SUM(amount) FILTER (WHERE payment_mode = 'UPI') = 300.00
           AND SUM(amount) FILTER (WHERE payment_mode = 'CARD') = 100.00
           AND SUM(amount) FILTER (WHERE payment_mode = 'OTHER') = 400.00
           AND BOOL_AND(received_by IS NULL)
        FROM gdclab.payments WHERE registration_id = visit_id
    ), 'multiple collections, authoritative aggregate inputs and nullable staff');
    PERFORM pg_temp.expect_rejected(format('UPDATE gdclab.payments SET amount = 0 WHERE registration_id = %s', visit_id), '23514');
    PERFORM pg_temp.expect_rejected(format('UPDATE gdclab.payments SET amount = -1 WHERE registration_id = %s', visit_id), '23514');
    PERFORM pg_temp.expect_rejected(format('UPDATE gdclab.payments SET amount = ''NaN'' WHERE registration_id = %s', visit_id), '23514');
    PERFORM pg_temp.expect_rejected(format('UPDATE gdclab.payments SET payment_mode = ''UNKNOWN'' WHERE registration_id = %s', visit_id), '23514');
    PERFORM pg_temp.expect_rejected('UPDATE gdclab.payments SET payment_no = ''PAY-95000'' WHERE payment_no = ''PAY-95001''', '23505');
    PERFORM pg_temp.expect_rejected(format('UPDATE gdclab.payments SET registration_id = -1 WHERE registration_id = %s', visit_id), '23503');
    UPDATE gdclab.registrations SET paid_amount = (SELECT SUM(amount) FROM gdclab.payments WHERE registration_id = visit_id),
        due_amount = 0.00, status = 'PAID' WHERE id = visit_id;

    FOREACH instant IN ARRAY instants LOOP
        combination := array_position(instants, instant);
        INSERT INTO gdclab.registrations (
            registration_no, bill_no, patient_id, branch_id, registration_datetime,
            patient_name_snapshot, patient_age_snapshot, patient_gender_snapshot,
            patient_mobile_snapshot, branch_code_snapshot, branch_name_snapshot,
            branch_address_snapshot, bill_amount, final_bill_amount, due_amount
        ) VALUES (
            'REG-' || (95100 + combination)::TEXT, 'BILL-' || (95100 + combination)::TEXT,
            person_id, branch_id, instant, 'Database Fixture', 0, 'FEMALE', '7777770010',
            'SBD-NANDIGAMA', 'SRI BALAJI DIAGNOSTICS', 'Original branch address', 1000.00, 1000.00, 1000.00
        );
    END LOOP;
    SET LOCAL TIME ZONE 'UTC';
    PERFORM pg_temp.assert_true((
        SELECT COUNT(*) = 2 FROM gdclab.registrations
        WHERE registration_no LIKE 'REG-951%'
          AND registration_datetime >= TIMESTAMPTZ '2026-10-01 00:00:00+05:30'
          AND registration_datetime < TIMESTAMPTZ '2026-10-02 00:00:00+05:30'
    ), 'inclusive start and exclusive next-day India midnight');
    PERFORM pg_temp.assert_true((
        SELECT COUNT(*) = 4 FROM gdclab.registrations
        WHERE registration_no LIKE 'REG-951%'
          AND registration_datetime >= TIMESTAMPTZ '2026-09-28 00:00:00+05:30'
          AND registration_datetime < TIMESTAMPTZ '2026-10-05 00:00:00+05:30'
    ), 'weekly business-date range');
    PERFORM pg_temp.assert_true((
        SELECT COUNT(*) = 3 FROM gdclab.registrations
        WHERE registration_no LIKE 'REG-951%'
          AND registration_datetime >= TIMESTAMPTZ '2026-10-01 00:00:00+05:30'
          AND registration_datetime < TIMESTAMPTZ '2026-11-01 00:00:00+05:30'
    ), 'monthly business-date range');
    PERFORM pg_temp.assert_true((
        SELECT COUNT(*) = 2 FROM gdclab.registrations
        WHERE registration_no LIKE 'REG-951%'
          AND registration_datetime >= TIMESTAMPTZ '2026-10-01 12:00:00+05:30'
          AND registration_datetime < TIMESTAMPTZ '2026-10-02 12:00:00+05:30'
    ), 'custom timestamp range');
    PERFORM pg_temp.assert_true((
        SELECT COUNT(*) = 1 AND SUM(test_price) = 1000.00
            AND SUM(discount_amount) = 0.00 AND SUM(total_amount) = 1000.00
        FROM gdclab.registration_tests
        WHERE completed_at >= TIMESTAMPTZ '2026-10-01 00:00:00+05:30'
          AND completed_at < TIMESTAMPTZ '2026-10-02 00:00:00+05:30'
          AND status = 'COMPLETED' AND test_name_snapshot = 'Pregnancy (OBG)'
          AND category_code_snapshot = 'ULTRASOUND'
    ), 'conducted-test summary uses completed_at');
    PERFORM pg_temp.assert_true((
        SELECT COUNT(*) = 1 FROM (
            SELECT id FROM gdclab.payments WHERE registration_id = visit_id
            ORDER BY payment_datetime DESC, id DESC LIMIT 1 OFFSET 1
        ) page
    ), 'deterministic pagination does not redefine whole-result payment totals');

    UPDATE gdclab.patients SET first_name = 'Changed', last_name = 'Person',
        age = 40, mobile = '7777770020', address = 'Changed patient address' WHERE id = person_id;
    UPDATE gdclab.branches SET branch_name = 'Changed Branch', address = 'Changed branch address',
        phone = '08678-123456', mobile = '7777770030' WHERE id = branch_id;
    UPDATE gdclab.referral_sources SET name = 'Changed Doctor', qualification = 'MD' WHERE id = doctor_id;
    UPDATE gdclab.referral_sources SET name = 'Changed Lab' WHERE id = lab_id;
    UPDATE gdclab.diagnostic_tests SET test_name = 'Changed Test', current_price = 1200.00 WHERE id = master_test_id;
    UPDATE gdclab.test_categories SET category_code = 'SONOGRAPHY', category_name = 'Changed Category' WHERE id = category_id;
    PERFORM pg_temp.assert_true((
        SELECT patient_name_snapshot = 'Database Fixture' AND patient_age_snapshot = 0
           AND patient_mobile_snapshot = '7777770010' AND patient_address_snapshot IS NULL
           AND branch_name_snapshot = 'SRI BALAJI DIAGNOSTICS' AND branch_address_snapshot = 'Original branch address'
           AND branch_phone_snapshot IS NULL AND branch_mobile_snapshot IS NULL
           AND referring_doctor_name_snapshot = 'Dr Original' AND referring_doctor_qualification_snapshot = 'MBBS, DGO'
           AND referring_lab_name_snapshot = 'Original Lab' AND final_bill_amount = 1000.00
        FROM gdclab.registrations WHERE id = visit_id
    ), 'patient, branch, doctor and lab edits preserve the issued bill');
    PERFORM pg_temp.assert_true((
        SELECT test_name_snapshot = 'Pregnancy (OBG)' AND test_price = 1000.00
           AND category_code_snapshot = 'ULTRASOUND' AND category_name_snapshot = 'Ultrasound'
        FROM gdclab.registration_tests WHERE id = line_id
    ), 'test name, price and category snapshots survive master changes');
    -- PostgreSQL versions report blocked RESTRICT deletions with either integrity-violation code.
    PERFORM pg_temp.expect_rejected(format('DELETE FROM gdclab.patients WHERE id = %s', person_id), ARRAY['23503', '23001']);
    PERFORM pg_temp.expect_rejected(format('DELETE FROM gdclab.branches WHERE id = %s', branch_id), ARRAY['23503', '23001']);
    PERFORM pg_temp.expect_rejected(format('DELETE FROM gdclab.referral_sources WHERE id = %s', doctor_id), ARRAY['23503', '23001']);
    PERFORM pg_temp.expect_rejected(format('DELETE FROM gdclab.referral_sources WHERE id = %s', lab_id), ARRAY['23503', '23001']);
    PERFORM pg_temp.expect_rejected(format('DELETE FROM gdclab.registrations WHERE id = %s', visit_id), ARRAY['23503', '23001']);
    PERFORM pg_temp.expect_rejected(format('DELETE FROM gdclab.diagnostic_tests WHERE id = %s', master_test_id), ARRAY['23503', '23001']);
    PERFORM pg_temp.expect_rejected(format('DELETE FROM gdclab.test_categories WHERE id = %s', category_id), ARRAY['23503', '23001']);
END;
$verify$;

ROLLBACK;
