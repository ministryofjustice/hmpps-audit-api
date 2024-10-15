#!/bin/bash

usage() {
    echo "Usage:"
    echo "  ./query-audit-events.sh --by-user-date <year> <month> <day> <user>"
    echo "  ./query-audit-events.sh --by-id <audit-event-id>"
    exit 1
}

if [ "$#" -lt 2 ]; then
    usage
fi

FLAG=$1

case "$FLAG" in
    --by-id)
        if [ "$#" -ne 2 ]; then
            usage
        fi
        ID=$2
        QUERY="SELECT * FROM audit_event WHERE id = '$ID' AND NOT (year = '2024' AND month = '8');"
        ;;

    --by-user-date)
        if [ "$#" -ne 5 ]; then
            usage
        fi
        YEAR=$2
        MONTH=$3
        DAY=$4
        USER=$5
        QUERY="SELECT * FROM audit_event WHERE year = '$YEAR' AND month = '$MONTH' AND day = '$DAY' AND user = '$USER';"
        ;;

    *)
        usage
        ;;
esac

S3_BUCKET_NAME=$(aws s3api list-buckets --query "Buckets[?starts_with(Name, 'cloud-platform')].Name | [0]" --output text)
OUTPUT_LOCATION="s3://$S3_BUCKET_NAME/query_results/"
DATABASE="audit_dev"

echo "Bucket name: $S3_BUCKET_NAME"
echo "Output location: $OUTPUT_LOCATION"
echo "Query: $QUERY"

QUERY_EXECUTION_ID=$(aws athena start-query-execution \
    --query-string "$QUERY" \
    --query-execution-context Database=$DATABASE \
    --result-configuration OutputLocation=$OUTPUT_LOCATION \
    --output text --query 'QueryExecutionId')

if [ -z "$QUERY_EXECUTION_ID" ]; then
    echo "Failed to start the Athena query."
    exit 1
fi

echo "Query execution ID: $QUERY_EXECUTION_ID"

MAX_RETRIES=12
RETRY_COUNT=0

echo "Waiting for query to complete..."
QUERY_STATUS="RUNNING"
while [ "$QUERY_STATUS" = "RUNNING" ] || [ "$QUERY_STATUS" = "QUEUED" ]; do
    if [ "$RETRY_COUNT" -ge "$MAX_RETRIES" ]; then
        echo "Query timed out after 1 minute."
        exit 1
    fi
    sleep 5
    RETRY_COUNT=$((RETRY_COUNT+1))
    QUERY_STATUS=$(aws athena get-query-execution --query-execution-id $QUERY_EXECUTION_ID --output text --query 'QueryExecution.Status.State')
    echo "Query status: $QUERY_STATUS"
done

if [ "$QUERY_STATUS" = "FAILED" ]; then
    echo "Query failed. Check Athena logs for details."
    exit 1
fi

FILENAME="$QUERY_EXECUTION_ID.csv"
aws s3 cp "$OUTPUT_LOCATION$FILENAME" .

if [ -f "$FILENAME" ]; then
    cat "$FILENAME"
else
    echo "Failed to download result CSV."
    exit 1
fi
