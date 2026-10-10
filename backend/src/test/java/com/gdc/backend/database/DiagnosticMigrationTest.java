package com.gdc.backend.database;

import org.flywaydb.core.Flyway;
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
            new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("gdclab_foundation");

    private static Flyway flyway;

    @BeforeAll
    static void migrateFreshDatabase() {
        flyway = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas("gdclab")
                .defaultSchema("gdclab")
                .locations("classpath:db/migration")
                .load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
    }

    @Test
    void freshDatabaseHasSingleCleanFoundationMigration() throws SQLException {
        flyway.validate();
        assertThat(flyway.info().applied()).hasSize(1);
        assertThat(flyway.migrate().migrationsExecuted).isZero();

        try (Connection connection = connection(); var statement = connection.createStatement()) {
            var tables = new HashSet<String>();
            try (var result = statement.executeQuery("""
                    SELECT table_name FROM information_schema.tables
                    WHERE table_schema='gdclab' AND table_type='BASE TABLE'
                    """)) {
                while (result.next()) tables.add(result.getString(1));
            }
            assertThat(tables).contains("branches", "referral_sources", "test_categories", "diagnostic_tests",
                    "registrations", "registration_tests", "payments").doesNotContain("patients");
            assertThat(sequenceExists(connection, "patient_number_seq")).isFalse();
        }
    }

    @Test
    void seedsExpectedDiagnosticCatalogue() throws SQLException {
        try (Connection connection = connection(); var statement = connection.createStatement();
             var result = statement.executeQuery("""
                     SELECT c.category_code, COUNT(*)
                     FROM gdclab.diagnostic_tests t
                     JOIN gdclab.test_categories c ON c.id=t.category_id
                     GROUP BY c.category_code ORDER BY c.category_code
                     """)) {
            assertThat(result.next()).isTrue();
            assertThat(result.getString(1)).isEqualTo("CT_SCAN");
            assertThat(result.getLong(2)).isEqualTo(19);
            assertThat(result.next()).isTrue();
            assertThat(result.getString(1)).isEqualTo("ULTRASOUND");
            assertThat(result.getLong(2)).isEqualTo(22);
            assertThat(result.next()).isFalse();
        }
    }

    @Test
    void databaseAcceptanceSuiteChecksRegistrationTestsAndPayments() throws Exception {
        try (Connection connection = connection(); var statement = connection.createStatement()) {
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
                    try (Connection connection = connection(); var statement = connection.createStatement();
                         var result = statement.executeQuery("SELECT nextval('gdclab.registration_number_seq')")) {
                        result.next();
                        return result.getLong(1);
                    }
                }));
            }
            for (Future<Long> future : futures) numbers.add(future.get(10, TimeUnit.SECONDS));
        }
        assertThat(numbers).hasSize(16);
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static boolean sequenceExists(Connection connection, String name) throws SQLException {
        try (var statement = connection.prepareStatement("""
                SELECT EXISTS (SELECT 1 FROM information_schema.sequences
                WHERE sequence_schema='gdclab' AND sequence_name=?)
                """)) {
            statement.setString(1, name);
            try (var result = statement.executeQuery()) {
                result.next();
                return result.getBoolean(1);
            }
        }
    }

    private static String verificationSql() throws IOException {
        try (var stream = DiagnosticMigrationTest.class.getResourceAsStream(
                "/db/verification/diagnostic_foundation.sql")) {
            if (stream == null) throw new IOException("Missing diagnostic database acceptance suite");
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
