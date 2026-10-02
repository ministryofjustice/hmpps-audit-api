# AGENTS.md

Guidance for AI coding agents (and humans) working in this repository.

## Project overview

`hmpps-audit-api` is a Kotlin/Spring Boot service that listens for `AuditEvent`
messages on an SQS queue, stores them in a Postgres database, and exposes
REST endpoints to add and query audit events. It is part of the MoJ HMPPS
estate and uses standard HMPPS Spring Boot tooling (`hmpps-kotlin-spring-boot-starter`,
`hmpps-sqs-spring-boot-starter`).

## Tech stack

- Kotlin (JVM toolchain 25), Spring Boot
- Spring Data JPA + Flyway migrations (Postgres in prod, H2 in tests)
- Spring Security with OAuth2 resource server
- AWS SQS (via LocalStack locally), S3, Athena
- Gradle (Kotlin DSL) build, `./gradlew` wrapper
- ktlint for Kotlin code style

## Repository layout

- `src/main/kotlin/uk/gov/justice/digital/hmpps/hmppsauditapi/` — application code
  - `config/` — Spring configuration
  - `health/` — health indicators
  - `jpa/` + `jpa/model/` — JPA entities/repositories
  - `model/` — domain models
  - `resource/` + `resource/model/`, `resource/swagger/` — REST controllers and DTOs
  - `exception/` — exception handling
  - `listeners/` + `listeners/model/` — SQS message listeners
  - `services/` — business logic
- `src/main/resources/db/` — Flyway migrations (`audit`, `audit_h2`, `audit_postgres`)
- `src/test/kotlin/...` — tests mirroring the main package structure
- `readme/` — detailed docs (build/test/run, maintenance, inserting data, queue admin)
- `helm_deploy/` — Helm chart for deployment
- `.github/workflows/` — CI pipelines (build/test, security scans, deploy)

## Build, test, and run

Build without tests:
```bash
./gradlew clean build -x test
```

Run the test suite (requires LocalStack running; tests use H2 for the database):
```bash
TMPDIR=/private$TMPDIR docker compose up localstack
./gradlew test
```

Run a single test class/method:
```bash
./gradlew test --tests "uk.gov.justice.digital.hmpps.hmppsauditapi.SomeClassTest"
```

Lint / formatting (ktlint):
```bash
./gradlew ktlintCheck
./gradlew ktlintFormat
```

Run the app locally (dependencies via docker compose, app via Gradle):
```bash
TMPDIR=/private$TMPDIR docker compose up --scale hmpps-audit-api=0
./gradlew bootRun --args='--spring.profiles.active=dev,localstack'
```

See `readme/build_test_run.md`, `readme/maintenance.md`, `readme/inserting_data.md`,
and `readme/queue_admin.md` for more detail.

## Conventions

- Follow existing package structure and naming conventions (`hmppsauditapi` under
  `uk.gov.justice.digital.hmpps`).
- Keep Kotlin code ktlint-clean; run `./gradlew ktlintFormat` before committing if unsure.
- Add Flyway migrations under `src/main/resources/db/` rather than editing existing ones.
- Mirror new test files under `src/test/kotlin/...` matching the main package path.
- Prefer the smallest Gradle command that validates a change (e.g. a targeted `--tests`
  filter) over running the full suite when iterating.

## PRs and commits

- Keep changes focused and avoid unrelated refactors.
- Update relevant docs in `readme/` when behaviour, build, or run steps change.
