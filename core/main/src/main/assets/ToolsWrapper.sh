#!/bin/sh

# Check arguments
if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <binary_path> '<json_params>'"
    exit 1
fi

BINARY_PATH="$1"
JSON_PARAMS="$2"

# Run the binary with JSON string
OUTPUT=$("$BINARY_PATH" "$JSON_PARAMS")

echo "==Result=="
printf "%s" "$OUTPUT"
echo
echo "==Result=="