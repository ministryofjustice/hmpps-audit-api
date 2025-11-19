[< Back](../README.md)
---

## Inserting Data

Audit events can easily be added to the database by using the audit queue.

Payload should be in the format
```
operationId = App Insights operation Id (String)
what = audit type (String),
when =  1970-01-01T00:00:00 (data/time),
who = user identifier (String),
service = service name (String),
details = details (String)
```

#### Example audit event payload
```
{
  "operationId": "badea6d876c62e2f5264c94c7b50875e",
  "what": "COURT_REGISTER_BUILDING_UPDATE",
  "when": "2021-04-03T10:15:30Z",
  "who": "bobby.beans",
  "service": "court-register",
  "details": "{\"courtId\":\"AAAMH1\",\"buildingId\":936,\"building\":{\"id\":936,\"courtId\":\"AAAMH1\",\"buildingName\":\"Main Court Name Changed\"}}"
}
```

#### To send and audit event to localstack for local testing

Install the AWS CLI (`brew install awscli` on a Mac)
Ensure localstack is running (`TMPDIR=/private$TMPDIR docker compose up localstack`)

To send the above payload:

```sh
aws --region=eu-west-2 --endpoint-url=http://localhost:4566 sqs send-message \
    --queue-url http://localstack:4576/queue/mainQueue\
    --message-body '{
                      "operationId": "badea6d876c62e2f5264c94c7b50875e",
                      "what": "COURT_REGISTER_BUILDING_UPDATE",
                      "when": "2021-04-03T10:15:30Z",
                      "who": "bobby.beans",
                      "service": "court-register",
                      "details": "{\"courtId\":\"AAAMH1\",\"buildingId\":936,\"building\":{\"id\":936,\"courtId\":\"AAAMH1\",\"buildingName\":\"Main Court Name Changed\"}}"
                   }'
```
