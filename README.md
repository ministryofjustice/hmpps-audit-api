# hmpps-audit-api

[![repo standards badge](https://img.shields.io/badge/endpoint.svg?&style=flat&logo=github&url=https%3A%2F%2Foperations-engineering-reports.cloud-platform.service.justice.gov.uk%2Fapi%2Fv1%2Fcompliant_public_repositories%2Fhmpps-audit-api)](https://operations-engineering-reports.cloud-platform.service.justice.gov.uk/public-report/hmpps-audit-api "Link to report")
[![Docker Repository on ghcr](https://img.shields.io/badge/ghcr.io-repository-2496ED.svg?logo=docker)](https://ghcr.io/ministryofjustice/https://audit-api)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://audit-api-dev.hmpps.service.justice.gov.uk/swagger-ui.html)

The Audit service listens for AuditEvent messages on its queue and stores the message details in a postgres database.  It also provides endpoints to add and view the audit events.


## Contents

1. [Building, Testing and Running](readme/build_test_run.md)
2. [Maintenance](readme/maintenance.md)
3. [Inserting Data](readme/inserting_data.md)
4. [SQS Queue Admin](readme/queue_admin.md)
