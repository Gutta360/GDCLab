BEGIN;

CREATE OR REPLACE FUNCTION pg_temp.assert_true(condition BOOLEAN, message TEXT)
RETURNS VOID LANGUAGE plpgsql AS $$
BEGIN
    IF condition IS DISTINCT FROM TRUE THEN
        RAISE EXCEPTION 'assertion failed: %', message;
    END IF;
END;
$$;

CREATE OR REPLACE FUNCTION pg_temp.expect_rejected(command TEXT, expected_states TEXT[])
RETURNS VOID LANGUAGE plpgsql AS $$
BEGIN
    EXECUTE command;
    RAISE EXCEPTION 'expected statement to be rejected: %', command;
EXCEPTION WHEN OTHERS THEN
    IF SQLSTATE = 'P0001' OR NOT (SQLSTATE = ANY(expected_states)) THEN
        RAISE;
    END IF;
END;
$$;

CREATE OR REPLACE FUNCTION pg_temp.expect_rejected(command TEXT, expected_state TEXT)
RETURNS VOID LANGUAGE sql AS $$
    SELECT pg_temp.expect_rejected(command, ARRAY[expected_state]);
$$;

DO $verify$
DECLARE
    branch_id BIGINT;
    doctor_id BIGINT;
    lab_id BIGINT;
    first_registration_id BIGINT;
    second_registration_id BIGINT;
    test_id BIGINT;
    registration_test_id BIGINT;
BEGIN
    PERFORM pg_temp.assert_true(to_regclass('gdclab.patients') IS NULL, 'patients table must not exist');
    PERFORM pg_temp.assert_true(to_regclass('gdclab.patient_number_seq') IS NULL, 'patient sequence must not exist');
    PERFORM pg_temp.assert_true((SELECT COUNT(*)=41 FROM gdclab.diagnostic_tests), '41 tests seeded');
    PERFORM pg_temp.assert_true((
        SELECT COUNT(*)=19 FROM gdclab.diagnostic_tests t
        JOIN gdclab.test_categories c ON c.id=t.category_id WHERE c.category_code='CT_SCAN'
    ), '19 CT tests seeded');
    PERFORM pg_temp.assert_true((
        SELECT COUNT(*)=22 FROM gdclab.diagnostic_tests t
        JOIN gdclab.test_categories c ON c.id=t.category_id WHERE c.category_code='ULTRASOUND'
    ), '22 ultrasound tests seeded');

    SELECT id INTO branch_id FROM gdclab.branches WHERE branch_code='SBD-NANDIGAMA';
    INSERT INTO gdclab.referral_sources(referral_code,referral_type,name,qualification)
    VALUES ('DOC-VERIFY','DOCTOR','Dr Verify','MD') RETURNING id INTO doctor_id;
    INSERT INTO gdclab.referral_sources(referral_code,referral_type,name)
    VALUES ('LAB-VERIFY','LAB','Verify Lab') RETURNING id INTO lab_id;

    INSERT INTO gdclab.registrations(
        registration_no,bill_no,patient_title,patient_name,patient_mobile,patient_aadhaar,
        patient_gender,patient_email,patient_date_of_birth,patient_age,patient_address,
        branch_id,registration_datetime,referring_doctor_id,referring_lab_id,
        branch_code_snapshot,branch_name_snapshot,branch_address_snapshot,
        referring_doctor_name_snapshot,referring_doctor_qualification_snapshot,referring_lab_name_snapshot,
        bill_amount,final_bill_amount,due_amount
    ) VALUES (
        'REG-95000','BILL-95000','Mrs','Keerthi','9999999999','123456789012',
        'FEMALE','keerthi@example.com',DATE '2000-01-01',26,'Visit address',
        branch_id,TIMESTAMPTZ '2026-10-01 10:00:00+05:30',doctor_id,lab_id,
        'SBD-NANDIGAMA','SRI BALAJI DIAGNOSTICS','Original branch address',
        'Dr Verify','MD','Verify Lab',1000.00,1000.00,1000.00
    ) RETURNING id INTO first_registration_id;

    INSERT INTO gdclab.registrations(
        registration_no,bill_no,patient_name,patient_mobile,patient_gender,patient_age,
        branch_id,registration_datetime,branch_code_snapshot,branch_name_snapshot,
        branch_address_snapshot,bill_amount,final_bill_amount,due_amount
    ) VALUES (
        'REG-95001','BILL-95001','Keerthi','9999999999','FEMALE',26,
        branch_id,TIMESTAMPTZ '2026-10-02 10:00:00+05:30','SBD-NANDIGAMA',
        'SRI BALAJI DIAGNOSTICS','Original branch address',1000.00,1000.00,1000.00
    ) RETURNING id INTO second_registration_id;

    PERFORM pg_temp.assert_true((
        SELECT COUNT(*)=2 FROM gdclab.registrations
        WHERE patient_name='Keerthi' AND patient_mobile='9999999999'
    ), 'same person may have independent visits');
    PERFORM pg_temp.assert_true((
        SELECT patient_address IS NULL AND patient_aadhaar IS NULL
        FROM gdclab.registrations WHERE id=second_registration_id
    ), 'optional person fields remain optional');

    PERFORM pg_temp.expect_rejected(format(
        'UPDATE gdclab.registrations SET patient_name=%L WHERE id=%s',' ',first_registration_id),'23514');
    PERFORM pg_temp.expect_rejected(format(
        'UPDATE gdclab.registrations SET patient_mobile=%L WHERE id=%s','999999999',first_registration_id),'23514');
    PERFORM pg_temp.expect_rejected(format(
        'UPDATE gdclab.registrations SET patient_aadhaar=%L WHERE id=%s','1234ABCD9012',first_registration_id),'23514');
    PERFORM pg_temp.expect_rejected(format(
        'UPDATE gdclab.registrations SET patient_date_of_birth=CURRENT_DATE+1 WHERE id=%s',first_registration_id),'23514');
    PERFORM pg_temp.expect_rejected(format(
        'UPDATE gdclab.registrations SET patient_age=-1 WHERE id=%s',first_registration_id),'23514');
    PERFORM pg_temp.expect_rejected(format(
        'UPDATE gdclab.registrations SET referring_doctor_name_snapshot=NULL WHERE id=%s',first_registration_id),'23514');
    PERFORM pg_temp.expect_rejected(format(
        'UPDATE gdclab.registrations SET discount_percentage=101 WHERE id=%s',first_registration_id),'23514');

    SELECT id INTO test_id FROM gdclab.diagnostic_tests WHERE test_code='TST-00022';
    INSERT INTO gdclab.registration_tests(
        registration_id,test_id,test_name_snapshot,category_code_snapshot,category_name_snapshot,
        test_price,total_amount,registered_at
    ) VALUES (
        first_registration_id,test_id,'Pregnancy (OBG)','ULTRASOUND','Ultrasound',
        1000.00,1000.00,TIMESTAMPTZ '2026-10-01 10:00:00+05:30'
    ) RETURNING id INTO registration_test_id;

    PERFORM pg_temp.expect_rejected(format(
        'UPDATE gdclab.registration_tests SET discount_percentage=101 WHERE id=%s',registration_test_id),'23514');
    PERFORM pg_temp.expect_rejected(format(
        'UPDATE gdclab.registration_tests SET status=%L WHERE id=%s','COMPLETED',registration_test_id),'23514');
    UPDATE gdclab.registration_tests SET status='COMPLETED',completed_at=registered_at+INTERVAL '1 hour'
    WHERE id=registration_test_id;

    INSERT INTO gdclab.payments(payment_no,registration_id,amount,payment_mode,payment_datetime)
    VALUES ('PAY-95000',first_registration_id,400.00,'CASH',TIMESTAMPTZ '2026-10-01 10:00:00+05:30'),
           ('PAY-95001',first_registration_id,600.00,'UPI',TIMESTAMPTZ '2026-10-01 11:00:00+05:30');
    PERFORM pg_temp.assert_true((
        SELECT COUNT(*)=2 AND SUM(amount)=1000.00 FROM gdclab.payments
        WHERE registration_id=first_registration_id
    ), 'multiple payments remain supported');
    PERFORM pg_temp.expect_rejected(format(
        'UPDATE gdclab.payments SET amount=0 WHERE registration_id=%s',first_registration_id),'23514');
    PERFORM pg_temp.expect_rejected(format(
        'DELETE FROM gdclab.registrations WHERE id=%s',first_registration_id),ARRAY['23503','23001']);
    PERFORM pg_temp.expect_rejected(format(
        'DELETE FROM gdclab.branches WHERE id=%s',branch_id),ARRAY['23503','23001']);
END;
$verify$;

ROLLBACK;
