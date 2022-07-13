# hmpps-audit-api

[![CircleCI](https://circleci.com/gh/ministryofjustice/hmpps-audit-api/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/audit-api)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://audit-api-dev.hmpps.service.justice.gov.uk/swagger-ui.html)

The Audit service listens for AuditEvent messages on its queue and stores the message details in a postgres database.  It also provides endpoints to add and view the audit events.

### Building

```./gradlew build```

### Running

`localstack` is used to emulate the AWS SQS service. Run the tests with the environment `HMPPS_SQS_PROVIDER=localstack`.

Running all services (including localstack) except this application (hence allowing you to run this in the IDE)

```bash
TMPDIR=/private$TMPDIR  docker-compose up --scale hmpps-audit-api=0
```

### Inserting Data
AuditEvents can easily be added to the database by using the audit queue.

Payload should be in the format
```
operationId = App Insights operation Id (String)
what = audit type (String),
when =  1970-01-01T00:00:00 (data/time),
who = user identifier (String),
service = service name (String),
details = details (String)
```
#### example Audit Event
```
    "operationId": "badea6d876c62e2f5264c94c7b50875e",
    "what": "COURT_REGISTER_BUILDING_UPDATE",
    "when": "2021-04-03T10:15:30Z",
    "who": "bobby.beans",
    "service": "court-register",
    "details": "{\"courtId\":\"AAAMH1\",\"buildingId\":936,\"building\":{\"id\":936,\"courtId\":\"AAAMH1\",\"buildingName\":\"Main Court Name Changed\"}}"
```
## Testing

Tests need localstack to be up and running, this can be done manually.

```bash
TMPDIR=/private$TMPDIR docker-compose up localstack
```

### Test Database

The tests run against a H2 database

### Queue Admin

This application uses the Hmpps Queue Library which provides endpoints to retry DLQ messages and purge queues. The
endpoints should appear in the Open API docs.

The available endpoints are protected by a role that can be found in configuration property `hmpps.sqs.queueAdminRole`.
