# AGENTS.md

## Stack
- Java 21, Spring Boot, Maven
- MongoDB
- Spring Security, OAuth2 (Google), JWT

## Commands
- Build: `mvn clean package`
- Test: `mvn test` (Docker required)
- Run: `mvn spring-boot:run` or `java -jar target/*.jar`
- API: `/swagger-ui.html`

## Architecture
- Layered: controllers → services → repositories → model
- Use Lombok (no manual getters/setters)
- Do not modify or commit secrets in `application.properties`

## Gotchas
- Docker required for tests (Testcontainers)
- Use profiles: `-Dspring.profiles.active=dev|test|docker`

## Rules
- **Atomic Edits:** Prefer 1–2 files; allow more if needed
- **Diff Mindset:** Minimal changes only
- **Integrity:** No invalid or partial code
- **Verification:** Check imports and dependencies
- **Scope:** No unrelated refactors or new libraries

## Behavior
- Keep reasoning concise (1–3 lines)
- Use `@compact` if context is heavy