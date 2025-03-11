#!/usr/bin/env bash

# upload-node.sh
#
# This script copies node.js packages into the RStudio Build Tools
# (rstudio-buildtools) S3 bucket. Presumes you've already got AWS command line
# tools (awscli) installed, and configured with a valid AWS account.

# The node.js version used for building is set by RSTUDIO_NODE_VERSION.

# Modify to set the node.js version to upload
NODE_VERSION="v22.13.1"

# Check that we're logged in with AWS
aws sts get-caller-identity || aws sso login

BASEURL="https://nodejs.org/dist"
AWS_BUCKET="s3://rstudio-buildtools"

PLATFORMS=(
    darwin-arm64.tar.gz
    darwin-x64.tar.gz
    linux-arm64.tar.gz
    linux-x64.tar.gz
    win-x64.zip
    win-arm64.zip
)

for PLATFORM in "${PLATFORMS[@]}"; do

    # Form filename from version and platform
    FILENAME="node-${NODE_VERSION}-${PLATFORM}"

    # Download from node.js release site
    wget -c "${BASEURL}/${NODE_VERSION}/${FILENAME}"

    # Upload to S3 bucket
    aws s3 cp "${FILENAME}" "${AWS_BUCKET}/node/${NODE_VERSION}/" --acl public-read

done

