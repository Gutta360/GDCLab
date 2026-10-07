import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { resolve } from 'node:path';
import { pathToFileURL } from 'node:url';

const modulePath = process.argv[2]
  ?? 'backend/target/phase2-verification/node_modules/@electric-sql/pglite/dist/index.js';
const { PGlite } = await import(pathToFileURL(resolve(modulePath)).href);
const resourceRoot = new URL('../../backend/src/', import.meta.url);
const v1 = await readFile(new URL('main/resources/db/migration/V1__create_patient_module.sql', resourceRoot), 'utf8');
const v2 = await readFile(new URL('main/resources/db/migration/V2__create_diagnostic_registration_module.sql', resourceRoot), 'utf8');
const verification = await readFile(new URL('test/resources/db/verification/diagnostic_foundation.sql', resourceRoot), 'utf8');

async function migrate(db, sql) {
  await db.exec('BEGIN');
  try {
    await db.exec(sql);
    await db.exec('COMMIT');
  } catch (error) {
    await db.exec('ROLLBACK');
    throw error;
  }
}

async function verify(mode) {
  const db = new PGlite();
  try {
    await migrate(db, v1);
    let previous;
    if (mode === 'existing-patient upgrade') {
      await db.exec(`
        INSERT INTO gdclab.patients (
          patient_id, first_name, last_name, gender, age, mobile, address,
          referred_by, consultation_fee, registration_date
        ) VALUES (
          'P-90000', 'Legacy', 'Patient', 'FEMALE', 30, '9999990001',
          'Original address', 'SELF', 25.00, TIMESTAMPTZ '2026-10-01 00:00:00+05:30'
        )
      `);
      previous = (await db.query("SELECT * FROM gdclab.patients WHERE patient_id = 'P-90000'")).rows[0];
    }
    await migrate(db, v2);
    if (previous) {
      const updated = (await db.query("SELECT * FROM gdclab.patients WHERE patient_id = 'P-90000'")).rows[0];
      for (const [key, value] of Object.entries(previous)) {
        assert.deepEqual(updated[key], value, `Legacy column changed: ${key}`);
      }
      for (const key of ['title', 'aadhaar_no', 'email', 'date_of_birth']) {
        assert.equal(updated[key], null, `New field was populated unexpectedly: ${key}`);
      }
    }
    await db.exec(verification);
    const { rows } = await db.query(`
      SELECT c.category_code, COUNT(*)::integer AS test_count,
             SUM(t.current_price)::text AS catalogue_total
      FROM gdclab.diagnostic_tests t
      JOIN gdclab.test_categories c ON c.id = t.category_id
      GROUP BY c.category_code ORDER BY c.category_code
    `);
    const version = (await db.query('SHOW server_version')).rows[0].server_version;
    console.log(`PASS: ${mode}; PostgreSQL ${version}; shared database acceptance suite`);
    console.log(JSON.stringify(rows));
  } finally {
    await db.close();
  }
}

try {
  await verify('fresh installation');
  await verify('existing-patient upgrade');
  console.log('Flyway/JDBC and concurrent-connection checks require the native PostgreSQL Testcontainers suite.');
} catch (error) {
  console.error(`Database verification failed: ${error.message}; SQLSTATE ${error.code ?? 'n/a'}; constraint ${error.constraint ?? 'n/a'}`);
  process.exitCode = 1;
}
