#!/bin/bash

usage() {
    echo "Usage:"
    echo "  /bin/sh query-audit-events.sh --s3-bucket <bucket> --database <db> --table <table> --workgroup <workgroup> --by-id <audit-event-id>"
    echo "  /bin/sh query-audit-events.sh --s3-bucket <bucket> --database <db> --table <table> --workgroup <workgroup> --by-user-date <year> <month> <day> <user>"
    exit 1
}

if [ "$#" -lt 8 ]; then
    usage
fi

if [ "$1" != "--s3-bucket" ]; then usage; fi
S3_BUCKET_NAME=$2
shift 2

if [ "$1" != "--database" ]; then usage; fi
DATABASE_NAME=$2
shift 2

if [ "$1" != "--table" ]; then usage; fi
TABLE_NAME=$2
shift 2

if [ "$1" != "--workgroup" ]; then usage; fi
WORKGROUP=$2
shift 2

if ! aws s3 ls "s3://$S3_BUCKET_NAME" > /dev/null 2>&1; then
    echo "Error: S3 bucket '$S3_BUCKET_NAME' does not exist or is not accessible."
    exit 1
fi

FLAG=$1
shift

case "$FLAG" in
    --by-id)
        if [ "$#" -ne 1 ]; then usage; fi
        ID=$1
        QUERY="SELECT * FROM $DATABASE_NAME.$TABLE_NAME WHERE id = '$ID' AND NOT (year = '2024' AND month = '8');"
        ;;

    --by-user-date)
        if [ "$#" -ne 4 ]; then usage; fi
        YEAR=$1
        MONTH=$2
        DAY=$3
        USER=$4
        QUERY="SELECT * FROM $DATABASE_NAME.$TABLE_NAME WHERE year = '$YEAR' AND month = '$MONTH' AND day = '$DAY' AND user = '$USER';"
        ;;

    *)
        usage
        ;;
esac

OUTPUT_LOCATION="s3://$S3_BUCKET_NAME/query_results/"

echo "Bucket name: $S3_BUCKET_NAME"
echo "Output location: $OUTPUT_LOCATION"
echo "Query: $QUERY"
echo "Workgroup: $WORKGROUP"

QUERY_EXECUTION_ID=$(aws athena start-query-execution \
    --query-string "$QUERY" \
    --query-execution-context Database=$DATABASE_NAME \
    --work-group "$WORKGROUP" \
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
