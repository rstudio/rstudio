#!/usr/bin/env bash

# upload-copilot-language-server.sh
#
# This script copies the "Copilot Language Server" package into the RStudio Build Tools
# (rstudio-buildtools) S3 bucket. Presumes you've already got AWS command line
# tools (awscli) installed, and configured with a valid AWS account.

# Exit on error, undefined vars, pipe failures
set -euo pipefail

# Can pass version as first argument, or fallback to hardcoded value
COPILOT_VERSION="${1:-1.364.0}"

# Validate version format 
if [[ ! "${COPILOT_VERSION}" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
   echo "Error: Invalid version format. Expected format: X.Y.Z" >&2
   exit 1
fi

BASEURL="https://github.com/github/copilot-language-server-release/releases/download"
AWS_BUCKET="s3://rstudio-buildtools"
FILENAME="copilot-language-server-js-${COPILOT_VERSION}.zip"
S3_PATH="${AWS_BUCKET}/copilot-language-server/${COPILOT_VERSION}/"

# check if command exists
command_exists() {
   command -v "$1" >/dev/null 2>&1
}

# Function to cleanup on exit
cleanup() {
   if [[ -f "${FILENAME}" ]]; then
      rm -f "${FILENAME}"
      echo "🧹 Cleaned up temporary file: ${FILENAME}"
   fi
}

# Set up cleanup trap
trap cleanup EXIT

# Check dependencies
if ! command_exists aws; then
   echo "Error: AWS CLI is not installed or not in PATH" >&2
   exit 1
fi

if ! command_exists wget; then
   echo "Error: wget is not installed or not in PATH" >&2
   exit 1
fi

# Check AWS authentication
echo "🔒 Checking AWS authentication..."
if ! aws sts get-caller-identity >/dev/null 2>&1; then
   echo "AWS authentication required. Attempting to login..."
   if ! aws sso login; then
      echo "Error: Failed to authenticate with AWS" >&2
      exit 1
   fi
fi

# Download the file
echo "⬇️ Downloading ${FILENAME}..."
if ! wget -c "${BASEURL}/${COPILOT_VERSION}/${FILENAME}"; then
   echo "Error: Failed to download ${FILENAME}" >&2
   exit 1
fi

# Upload to S3
echo "⬆️ Uploading to S3: ${S3_PATH}"
if ! aws s3 cp "${FILENAME}" "${S3_PATH}" --acl public-read; then
   echo "Error: Failed to upload to S3" >&2
   exit 1
fi

echo "✅ Successfully uploaded Copilot Language Server ${COPILOT_VERSION} to S3"
