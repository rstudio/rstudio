#!/usr/bin/env bash

# upload-copilot-language-server.sh
#
# This script copies the "Copilot Language Server" packages into the RStudio Build Tools
# (rstudio-buildtools) S3 bucket. Presumes you've already got AWS command line
# tools (awscli) installed, and configured with a valid AWS account.

# Modify to set the Copilot Language Server version to upload
COPILOT_VERSION="1.322.0"

# Check that we're logged in with AWS
aws sts get-caller-identity || aws sso login

BASEURL="https://github.com/github/copilot-language-server-release/releases/download"
AWS_BUCKET="s3://rstudio-buildtools"

PLATFORMS=(
    darwin-arm64
    darwin-x64
    linux-arm64
    linux-x64
    win32-x64
    js
)

for PLATFORM in "${PLATFORMS[@]}"; do
    FILENAME="copilot-language-server-${PLATFORM}-${COPILOT_VERSION}".zip
    wget -c "${BASEURL}/${COPILOT_VERSION}/${FILENAME}"
    aws s3 cp "${FILENAME}" "${AWS_BUCKET}/copilot-language-server/${COPILOT_VERSION}/" --acl public-read
    rm "$FILENAME"
done
