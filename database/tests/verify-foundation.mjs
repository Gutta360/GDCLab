import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { resolve } from 'node:path';
import { pathToFileURL } from 'node:url';

const modulePath = process.argv[2]
  ?? 'backend/target/foundation-verification/node_modules/@electric-sql/pglite/dist/index.js';
const { PGlite } = await import(pathToFileURL(resolve(modulePath)).href);
const resourceRoot = new URL('../../backend/src/', import.meta.url);
const migration = await readFile(
  new URL('main/resources/db/migration/V1__create_diagnostic_registration_module.sql', resourceRoot),
  'utf8'
);
const verification = await readFile(
  new URL('test/resources/db/verification/diagnostic_foundation.sql', resourceRoot),
  'utf8'
);

const db = new PGlite();
try {
  await db.exec(migration);
  await db.exec(verification);

  const tables = (await db.query(`
    SELECT table_name FROM information_schema.tables
    WHERE table_schema='gdclab' AND table_type='BASE TABLE'
    ORDER BY table_name
  `)).rows.map(row => row.table_name);
  assert.deepEqual(tables, [
    'branches',
    'diagnostic_tests',
    'payments',
    'referral_sources',
    'registration_tests',
    'registrations',
    'test_categories'
  ]);

  const categories = (await db.query(`
    SELECT c.category_code, COUNT(*)::integer AS test_count
    FROM gdclab.diagnostic_tests t
    JOIN gdclab.test_categories c ON c.id=t.category_id
    GROUP BY c.category_code ORDER BY c.category_code
  `)).rows;
  assert.deepEqual(categories, [
    { category_code: 'CT_SCAN', test_count: 19 },
    { category_code: 'ULTRASOUND', test_count: 22 }
  ]);

  const patientSequence = (await db.query(`
    SELECT COUNT(*)::integer AS count FROM information_schema.sequences
    WHERE sequence_schema='gdclab' AND sequence_name='patient_number_seq'
  `)).rows[0].count;
  assert.equal(patientSequence, 0);

  console.log('PASS: clean GDCLab V1 foundation migration and acceptance suite');
  console.log(JSON.stringify(categories));
} finally {
  await db.close();
}
