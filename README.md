# spring-lab-01

RWPSF 3305 — Web Application Development with Spring Framework.
Sabyr Daryn, IT1-2411 IS.

## Lab 10 — the security subsystem (variant 6: a Content-Security-Policy)

| What | Where |
|---|---|
| `SecurityFilterChain`, the CSP header, `PasswordEncoder` (BCrypt) | `src/main/java/kz/iitu/spring_lab_01/security/SecurityConfiguration.java` |
| `@EnableMethodSecurity` | `security/MethodSecurityConfiguration.java` |
| `UserDetailsService` over `app_user` / `user_role` | `security/DatabaseUserDetailsService.java`, `security/AppUser.java` |
| `@PreAuthorize("hasRole('ADMIN')")` | `catalog/BookService.java` (create, replace, delete), `security/UserAccountService.java` |
| Administrator area | `web/AdminController.java`, `templates/admin/users.html` |
| Who is logged in (`sec:authorize`, `sec:authentication`) | `templates/fragments/header.html` |
| CSP demonstration (an inline script the policy blocks) | `templates/index.html`, `static/js/csp-check.js` |
| Migrations (Flyway owns the schema) | `src/main/resources/db/migration/V1`, `V2` (catalogue), `V5__security.sql` |

Access rules:

| Request | Anonymous | `user` | `admin` |
|---|---|---|---|
| `GET /` | 200 | 200 | 200 |
| `GET /books` | 302 → `/login` | 200 | 200 |
| `POST /books` (create) | 302 → `/login` (403 without a CSRF token) | 403 | 302 → `/books` |
| `GET /admin` | 302 → `/login` | 403 | 200 |
| `GET /nowhere` (no rule) | 302 → `/login` | 403 | 403 |

Test users (teaching passwords only): `user` / `user123` (USER), `admin` / `admin123` (USER, ADMIN),
stored as BCrypt hashes.

Every response carries

```
Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:;
                         object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'
```

The start page contains an inline `<script>` on purpose: with the header it does not run, and the browser
console (F12 → Console) shows which directive refused it.

## Running

**With an installed PostgreSQL 16+** — create the database once, then run `SpringLab01Application`:

```
createdb -U postgres springlab
./mvnw spring-boot:run
```

The connection is `jdbc:postgresql://localhost:5432/springlab`, user `postgres`, password `postgres` by default.
Real values belong in environment variables (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`) or in
`./config/application.properties`, which is git-ignored.

**With Docker** — `docker compose up -d` starts PostgreSQL 17 with the `springlab` database, then run as above.

**Without PostgreSQL or Docker** — an embedded PostgreSQL 17 is started for you:

```
./mvnw spring-boot:test-run          (Windows: mvnw.cmd spring-boot:test-run)
```

It listens on `localhost:5432` (database `springlab`, user `postgres`, any password) while the application runs,
so a database client can connect to it. It starts empty every time and Flyway migrates it.
Stop the application with Ctrl+C (or the Stop button in IDEA) so that the database is stopped too.

Open http://localhost:8080/.

## Tests

```
./mvnw test
```

46 integration tests run against an embedded PostgreSQL 17 (no Docker needed): the access matrix,
login against the database, BCrypt hashes, CSRF, `@PreAuthorize`, a scenario where a URL rule alone would
not have been enough (`UrlRuleIsNotEnoughTest`) and the Content-Security-Policy (`ContentSecurityPolicyTest`).
