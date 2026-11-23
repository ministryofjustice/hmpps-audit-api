[< Back](../README.md)
---

## Building

To build the project (without tests):
```
./gradlew clean build -x test
```

To rebuild the docker image locally after building the project (perhaps after some new changes), run:
```
docker build -t quay.io/hmpps/hmpps-audit-api:latest .
```

## Testing 

### LocalStack

Tests need [LocalStack](https://localstack.cloud/) to be up and running, this can be done manually.

```bash
TMPDIR=/private$TMPDIR docker compose up localstack
```

### Test Database

The tests are run against a H2 database

### Run the test suite

```
./gradlew test
```

## Running

For local development of this service, to run all the dependencies (including [LocalStack](https://localstack.cloud/) to emulate the AWS SQS service):


```bash
TMPDIR=/private$TMPDIR  docker compose up --scale hmpps-audit-api=0
```

And then using gradle:
```
./gradlew bootRun --args='--spring.profiles.active=dev,localstack'
```

The service can be run similarly within IntelliJ by running the main class with the following VM options:
```
-Dspring.profiles.active=dev,localstack
```


### Spring profiles

- `dev` – local development only (H2, LocalStack, port 8081), configured in `application-dev.yml`.
  Not used in any deployed environment.
- `aws-testing` – active only in the deployed dev environment (set via `SPRING_PROFILES_ACTIVE` in
  `helm_deploy/values-dev.yaml`). Use `@Profile("aws-testing")` for beans that should only exist there
  (e.g. `DevTestingController`), and `application-aws-testing.yaml` for property overrides.
