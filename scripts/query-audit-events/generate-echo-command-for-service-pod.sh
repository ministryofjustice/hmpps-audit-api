#!/bin/bash

INPUT_FILE="query-audit-events.sh"
OUTPUT_FILE="query-audit-events-command.txt"

if [ ! -f "$INPUT_FILE" ]; then
    echo "Error: $INPUT_FILE not found!"
    exit 1
fi

echo -n "echo '" > "$OUTPUT_FILE"

while IFS= read -r line; do
    escaped_line=$(echo "$line" | sed "s/'/'\\\\''/g")
    echo "$escaped_line" >> "$OUTPUT_FILE"
done < "$INPUT_FILE"

echo "' > query-audit-events.sh" >> "$OUTPUT_FILE"

echo "Generated: $OUTPUT_FILE"
