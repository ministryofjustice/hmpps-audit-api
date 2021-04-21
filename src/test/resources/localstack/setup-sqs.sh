#!/usr/bin/env bash
export AWS_ACCESS_KEY_ID=foobar
export AWS_SECRET_ACCESS_KEY=foobar
export AWS_DEFAULT_REGION=eu-west-2

aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name hmpps_audit_dlq
aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name hmpps_audit_queue
aws --endpoint-url=http://localhost:4566 sqs set-queue-attributes --queue-url "http://localhost:4566/queue/hmpps_audit_queue" --attributes '{"RedrivePolicy":"{\"maxReceiveCount\":\"3\", \"deadLetterTargetArn\":\"arn:aws:sqs:eu-west-2:000000000000:hmpps_audit_dlq\"}"}'

echo All Ready