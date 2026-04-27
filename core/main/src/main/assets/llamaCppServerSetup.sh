#!/bin/bash

# Exit immediately if a command fails
set -e

# Check arguments
if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <directory_name> <zip_url>"
    exit 1
fi

DIR_NAME="$1"
ZIP_URL="$2"

echo "Directory: $DIR_NAME"
echo "Zip URL: $ZIP_URL"

# 🔥 Always recreate directory (remove if exists)
echo "Recreating directory $DIR_NAME..."
rm -rf "$DIR_NAME"
mkdir -p "$DIR_NAME"

# Move into directory
cd "$DIR_NAME"

# Extract filename
ZIP_FILE=$(basename "$ZIP_URL")

# 🔥 Always download fresh
echo "Downloading zip file..."
wget -O "$ZIP_FILE" "$ZIP_URL"

echo "Extracting zip..."
unzip -o "$ZIP_FILE"

echo "Setup complete."