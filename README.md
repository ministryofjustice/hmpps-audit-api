# hmpps-audit-api

[![CircleCI](https://circleci.com/gh/ministryofjustice/hmpps-audit-api/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/hmpps-audit-api)
[![Docker Repository on Quay](https://img.shields.io/badge/quay.io-repository-2496ED.svg?logo=docker)](https://quay.io/repository/hmpps/hmpps-audit-api)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://audit-api-dev.hmpps.service.justice.gov.uk/swagger-ui.html)

The Audit service listens for AuditEvent messages on its queue and stores the message details in a postgres database.  It also provides endpoints to add and view the audit events.


## Contents

1. [Building, Testing and Running](readme/build_test_run.md)
2. [Maintenance](readme/maintenance.md)
3. [Inserting Data](readme/inserting_data.md)
4. [SQS Queue Admin](readme/queue_admin.md)
