package com.example.txlab.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Task E: prints Flyway's history table at startup, so every run shows which migrations are applied. */
@Component
public class SchemaHistoryLogger implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaHistoryLogger.class);

    private final JdbcTemplate jdbc;

    public SchemaHistoryLogger(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info(">>> flyway_schema_history:");
        jdbc.query("""
                SELECT "installed_rank", "version", "description", "installed_on", "success"
                FROM "flyway_schema_history" ORDER BY "installed_rank"
                """, rs -> {
            log.info(">>>   rank={} version={} description='{}' installed_on={} success={}",
                    rs.getInt("installed_rank"), rs.getString("version"), rs.getString("description"),
                    rs.getTimestamp("installed_on"), rs.getBoolean("success"));
        });
    }
}
