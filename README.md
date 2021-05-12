# hmpps-audit-api

[![CircleCI](https://circleci.com/gh/ministryofjustice/hmpps-audit-api/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/audit-api)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://audit-api-dev.hmpps.service.justice.gov.uk/swagger-ui.html)

The Audit service listens for AuditEvent messages on its queue and stores the message details in a postgres database.  It also provides endpoints to add and view the audit events.

### Building

```./gradlew build```

### Running

`localstack` is used to emulate the AWS SQS service. When running the integration tests this will be started automatically. If you want the tests to use an already running version of `localstack` run the tests with the environment `AWS_PROVIDER=localstack`. This has the benefit of running the test quicker without the overhead of starting the `localstack` container.

Any commands in `localstack/setup-sqs.sh` will be run when `localstack` starts, so this should contain commands to create the appropriate queues.

Running all services (including localstack) except this application (hence allowing you to run this in the IDE)

```bash
TMPDIR=/private$TMPDIR  docker-compose up --scale hmpps-audit-api=0
```

### Inserting Data
AuditEvents can easily be added to and retrieved from the database by using the /audit POST and GET endpoints, respectively.

## Testing
Tests need localstack to be up and running, this can be done manually.
```bash
TMPDIR=/private$TMPDIR docker-compose up localstack
```
### Test Database
The tests run against a H2 database


