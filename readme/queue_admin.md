[< Back](../README.md)
---

## SQS Queue Admin

This application uses the Hmpps Queue Library which provides endpoints to retry DLQ messages and purge queues. The
endpoints should appear in the Open API docs.

The available endpoints are protected by a role that can be found in configuration property `hmpps.sqs.queueAdminRole`.
