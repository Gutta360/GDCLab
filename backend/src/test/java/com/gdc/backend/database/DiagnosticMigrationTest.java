package com.gdc.backend.database;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class DiagnosticMigrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("phase2");

    private static final String LEGACY_PATIENT_QUERY = """
            SELECT id, patient_id, first_name, last_name, gender, age, mobile, address,
                   referred_by, doctor_name, consultation_fee, registration_date,
                   is_active, deleted_at, created_at, created_by, updated_at, updated_by, version
            FROM gdclab.patients WHERE patient_id = 'P-90000'
            """;

    private static Integer v1Checksum;
    private static List<String> legacyPatient;

    @BeforeAll
    static void upgradeExistingV1Database() throws SQLException {
        Flyway v1 = configuration(POSTGRES.getJdbcUrl())
                .target(MigrationVersion.fromVersion("1")).load();
        assertThat(v1.migrate().migrationsExecuted).isEqualTo(1);
        v1Checksum = v1.info().current().getChecksum();

        try (Connection connection = connection(POSTGRES.getJdbcUrl());
             var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO gdclab.patients (
                        patient_id, first_name, last_name, gender, age, mobile, address,
                        referred_by, consultation_fee, registration_date
                    ) VALUES (
                        'P-90000', 'Legacy', 'Patient', 'FEMALE', 30, '9999990001',
                        'Original address', 'SELF', 25.00, TIMESTAMPTZ '2026-10-01 00:00:00+05:30'
                    )
                    """);
            legacyPatient = readLegacyPatient(connection);
        }

        assertThat(configuration(POSTGRES.getJdbcUrl()).load().migrate().migrationsExecuted)
                .isEqualTo(1);
    }

    @Test
    void upgradePreservesLegacyPatientChecksumAndDoesNotRepeatSeeds() throws SQLException {
        Flyway flyway = configuration(POSTGRES.getJdbcUrl()).load();
        flyway.validate();
        assertThat(flyway.info().applied()).hasSize(2);
        assertThat(flyway.info().applied()[0].getChecksum()).isEqualTo(v1Checksum);
        assertThat(flyway.migrate().migrationsExecuted).isZero();

        try (Connection connection = connection(POSTGRES.getJdbcUrl())) {
            assertThat(readLegacyPatient(connection)).isEqualTo(legacyPatient);
            assertThat(countTests(connection)).isEqualTo(41);
        }
    }

    @Test
    void sharedDatabaseAcceptanceSuiteChecksSeedsConstraintsSnapshotsAndReporting() throws Exception {
        try (Connection connection = connection(POSTGRES.getJdbcUrl());
             var statement = connection.createStatement()) {
            statement.execute(verificationSql());
        }
    }

    @Test
    void freshDatabaseRunsBothMigrationsAndValidates() throws Exception {
        try (Connection connection = connection(POSTGRES.getJdbcUrl());
             var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE phase2_fresh");
        }

        String freshUrl = "jdbc:postgresql://" + POSTGRES.getHost() + ":"
                + POSTGRES.getMappedPort(5432) + "/phase2_fresh";
        Flyway flyway = configuration(freshUrl).load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(2);
        flyway.validate();
        assertThat(flyway.migrate().migrationsExecuted).isZero();

        try (Connection connection = connection(freshUrl);
             var statement = connection.createStatement()) {
            assertThat(countTests(connection)).isEqualTo(41);
            statement.execute(verificationSql());
        }
    }

    @Test
    void sequenceAllocationsAreUniqueAcrossConcurrentConnections() throws Exception {
        var numbers = new HashSet<Long>();
        try (var executor = Executors.newFixedThreadPool(4)) {
            List<Future<Long>> futures = new ArrayList<>();
            for (int i = 0; i < 16; i++) {
                futures.add(executor.submit(() -> {
                    try (Connection connection = connection(POSTGRES.getJdbcUrl());
                         var statement = connection.createStatement();
                         var result = statement.executeQuery(
                                 "SELECT nextval('gdclab.registration_number_seq')")) {
                        result.next();
                        return result.getLong(1);
                    }
                }));
            }
            for (Future<Long> future : futures) {
                numbers.add(future.get(10, TimeUnit.SECONDS));
            }
        }
        assertThat(numbers).hasSize(16);
    }

    private static FluentConfiguration configuration(String url) {
        return Flyway.configure()
                .dataSource(url, POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas("gdclab")
                .defaultSchema("gdclab")
                .target(MigrationVersion.fromVersion("2"))
                .locations("classpath:db/migration");
    }

    private static Connection connection(String url) throws SQLException {
        return DriverManager.getConnection(url, POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static List<String> readLegacyPatient(Connection connection) throws SQLException {
        try (var statement = connection.createStatement();
             var result = statement.executeQuery(LEGACY_PATIENT_QUERY)) {
            assertThat(result.next()).isTrue();
            List<String> values = new ArrayList<>();
            for (int column = 1; column <= result.getMetaData().getColumnCount(); column++) {
                values.add(result.getString(column));
            }
            return values;
        }
    }

    private static long countTests(Connection connection) throws SQLException {
        try (var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT COUNT(*) FROM gdclab.diagnostic_tests")) {
            result.next();
            return result.getLong(1);
        }
    }

    private static String verificationSql() throws IOException {
        try (var stream = DiagnosticMigrationTest.class.getResourceAsStream(
                "/db/verification/diagnostic_foundation.sql")) {
            if (stream == null) {
                throw new IOException("Missing diagnostic database acceptance suite");
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
