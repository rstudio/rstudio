#!/usr/bin/env bash

# This script copies Quarto release binaries into the RStudio Build Tools
# (rstudio-buildtools) S3 bucket. Presumes you've already got AWS command line
# tools (awscli) installed, and configured with a valid AWS account.

# Modify to set the Quarto version to upload
QUARTO_VERSION=1.7.31


# Check that we're logged in with AWS
aws sts get-caller-identity || aws sso login

BASEURL="https://github.com/quarto-dev/quarto-cli/releases/download/"
AWS_BUCKET="s3://rstudio-buildtools"

PLATFORMS=(
    linux-amd64.tar.gz
    linux-arm64.tar.gz
    linux-rhel7-amd64.tar.gz
    macos.tar.gz
    win.zip
)

for PLATFORM in "${PLATFORMS[@]}"; do

    # Form filename from version and platform
    FILENAME="quarto-${QUARTO_VERSION}-${PLATFORM}"

    # Download from Pandoc release site
    wget -c "${BASEURL}/v${QUARTO_VERSION}/${FILENAME}"

    # Upload to S3 bucket
    aws s3 cp "${FILENAME}" "${AWS_BUCKET}/quarto/${QUARTO_VERSION}/" --acl public-read

done

