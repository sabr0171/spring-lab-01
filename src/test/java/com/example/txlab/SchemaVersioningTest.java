package com.example.txlab;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.output.MigrateResult;
import org.flywaydb.core.api.output.ValidateResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Task E: schema versioning with Flyway. */
class SchemaVersioningTest extends AbstractLabTest {

    @Test
    void applicationStartupAppliedAllMigrations() {
        // Rows without a version are Flyway's own markers (e.g. "<< Flyway Schema History table created >>")
        List<Map<String, Object>> history = jdbc.queryForList("""
                SELECT "version", "description", "success"
                FROM "flyway_schema_history" WHERE "version" IS NOT NULL ORDER BY "installed_rank"
                """);

        history.forEach(row -> log.info(">>> history: {}", row));
        assertThat(history).extracting(row -> row.get("version")).containsExactly("1", "2", "3", "4");
        assertThat(history).allSatisfy(row -> assertThat(row.get("success")).isEqualTo(true));
    }

    @Test
    void restartDoesNotReapplyMigrationsAndNewMigrationIsApplied(@TempDir Path dir) {
        String url = "jdbc:h2:file:" + dir.resolve("restart-demo").toAbsolutePath();

        MigrateResult emptyDatabase = flyway(url, "classpath:db/migration").migrate();
        log.info(">>> empty database: executed {} migrations, schema version {}",
                emptyDatabase.migrationsExecuted, emptyDatabase.targetSchemaVersion);
        assertThat(emptyDatabase.migrationsExecuted).isEqualTo(4);

        MigrateResult restart = flyway(url, "classpath:db/migration").migrate();
        log.info(">>> restart: executed {} migrations", restart.migrationsExecuted);
        assertThat(restart.migrationsExecuted).isZero();

        // A new migration (V5, kept in test resources) appears next to the already applied ones
        MigrateResult withNewMigration = flyway(url, "classpath:db/migration", "classpath:db/lab-extra").migrate();
        log.info(">>> after adding V5: executed {} migrations, schema version {}",
                withNewMigration.migrationsExecuted, withNewMigration.targetSchemaVersion);
        assertThat(withNewMigration.migrationsExecuted).isEqualTo(1);
        assertThat(withNewMigration.targetSchemaVersion).isEqualTo("5");
    }

    @Test
    void editingAnAppliedMigrationIsDetected(@TempDir Path dir) throws IOException {
        Path migrations = copyMigrationsTo(dir.resolve("migrations"));
        String url = "jdbc:h2:file:" + dir.resolve("edit-demo").toAbsolutePath();
        String location = "filesystem:" + migrations.toAbsolutePath();

        flyway(url, location).migrate();

        // Someone "fixes" V1 after it was applied: Alice's opening balance changes
        Path v1 = migrations.resolve("V1__create_accounts.sql");
        Files.writeString(v1, Files.readString(v1).replace("1000.00", "2000.00"));

        ValidateResult validation = flyway(url, location).validateWithResult();
        log.info(">>> validation after editing V1: successful={}, invalid={}",
                validation.validationSuccessful, validation.invalidMigrations.size());
        assertThat(validation.validationSuccessful).isFalse();

        assertThatThrownBy(() -> flyway(url, location).migrate())
                .isInstanceOf(FlywayException.class)
                .hasMessageContaining("checksum mismatch")
                .satisfies(ex -> log.info(">>> migrate refused: {}", ex.getMessage()));
    }

    private static Flyway flyway(String url, String... locations) {
        return Flyway.configure().dataSource(url, "sa", "").locations(locations).load();
    }

    private static Path copyMigrationsTo(Path target) throws IOException {
        Files.createDirectories(target);
        Resource[] scripts = new PathMatchingResourcePatternResolver().getResources("classpath:db/migration/*.sql");
        for (Resource script : scripts) {
            try (InputStream in = script.getInputStream()) {
                Files.writeString(target.resolve(script.getFilename()), new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return target;
    }
}
