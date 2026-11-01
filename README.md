# Zito Restaurant Ordering API

Java 21 / Spring Boot 3.5.9 / Maven / MongoDB backend with guest/staff ordering, menu/category/table management, JWT and Google OAuth2, percentage discounts, VAT calculations and STOMP updates.

This is an ordering foundation, not yet a payment-ready restaurant backend. Payments, refunds and fiscal receipts are absent; authorization, pricing and delivery gaps are listed in the implementation plan.

## Documentation

- [Current API reference](docs/API.md): endpoints, authentication, examples, wire formats and limitations.
- Generated OpenAPI: `/v3/api-docs` (JSON), `/v3/api-docs.yaml` (YAML).
- Swagger UI: `/swagger-ui/index.html`. The usual `/swagger-ui.html` entry may require authentication because the allowlist includes only `/swagger-ui/**`.

The contract is generated from controllers/models and enriched in `OpenApi30Config`. It describes implemented behavior, including the broken category lookup. Proposed endpoints are only in the plan; there is no separately maintained static specification.

## Build and run

Install JDK 21, use Maven or the included wrapper (`.\mvnw.cmd` on Windows, `./mvnw` on Unix), and run MongoDB. Docker is required for Testcontainers integration tests.

```powershell
.\mvnw.cmd clean package
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Alternatively run `java -jar target/zito-0.0.2-SNAPSHOT.jar --spring.profiles.active=dev`. Configure the port externally; Spring Boot defaults to 8080 when no override is supplied.

Supply MongoDB, JWT signing key/lifetimes, CORS origins, Google OAuth2 client settings, `module-name` and `api-version` through your local profile or deployment configuration. `application.properties`, `application-dev.properties` and `application-test.properties` are ignored; do not commit credentials. Use profiles `dev`, `test` or `docker`; only docker properties are tracked. The separate `local-dev` profile uses development HTTP Basic and is not a production profile.

Builds with tests need Docker; `-DskipTests` skips test execution. Jib publishing is configured in `pom.xml`; review the image destination before running `jib:build`.

## Verification

Review date: 2026-11-01. See the plan for verification limitations and release gates. The pinned springdoc 2.1.0 predates the version family documented for Boot 3.5; validate and upgrade it before relying on a production spec export.

## License

See LICENSE for the project's AGPL license.
