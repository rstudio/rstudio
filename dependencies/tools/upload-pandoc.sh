#!/usr/bin/env bash

# This script copies Pandoc release binaries into the RStudio Build Tools
# (rstudio-buildtools) S3 bucket. Presumes you've already got AWS command line
# tools (awscli) installed, and configured with a valid AWS account.
# 
# Pandoc is not shy about changing the formulation of their paths and download
# filenames, so tweaking for new releases is expected.

# Modify to set the Pandoc version to upload
PANDOC_VERSION=2.16.2

BASEURL="https://github.com/jgm/pandoc/releases/download/"
AWS_BUCKET="s3://rstudio-buildtools"

PLATFORMS=(
    linux-amd64.tar.gz
    linux-arm64.tar.gz
    macOS.zip
    windows-x86_64.zip
)

for PLATFORM in "${PLATFORMS[@]}"; do

    # Form filename from version and platform
    FILENAME="pandoc-${PANDOC_VERSION}-${PLATFORM}"

    # Download from Pandoc release site
    wget "${BASEURL}/${PANDOC_VERSION}/${FILENAME}"

    # Upload to S3 bucket
    aws s3 cp "${FILENAME}" "${AWS_BUCKET}/pandoc/${PANDOC_VERSION}/" --acl public-read

    # Clean up
    rm -f "${FILENAME}"

done

