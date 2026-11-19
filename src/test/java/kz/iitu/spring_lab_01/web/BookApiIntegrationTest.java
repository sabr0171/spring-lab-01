package kz.iitu.spring_lab_01.web;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

// Task 4: one class, one container, the real engine (the tests are in LibraryApiScenario).
// static -> the container is started once for the class, not for every test method.
// @ServiceConnection -> Spring Boot takes the container's JDBC URL, user and password for the DataSource and
// Flyway; nothing is written into the properties. postgres:17-alpine is the image of compose.yaml, slimmed down.
// disabledWithoutDocker = true -> without Docker the class is skipped with that reason instead of breaking the
// build. This computer has no Docker: docs/lab12-evidence.md has the command, the error and what this test
// proves that the @DataJpaTest does not.
@Testcontainers(disabledWithoutDocker = true)
class BookApiIntegrationTest extends LibraryApiScenario {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");
}
