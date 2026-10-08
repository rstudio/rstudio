#!/usr/bin/env bash

# This script copies OpenSSL archives built by dependencies/osx/build-openssl
# (openssl-<version>-macos-<arch>.tar.gz) or
# dependencies/windows/install-openssl (openssl-<version>.zip) into the
# RStudio Build Tools (rstudio-buildtools) S3 bucket, at the paths the
# dependency scripts download from. Presumes you've got the AWS command line
# tools (awscli) installed, and configured with a valid AWS account.
#
# Archives already in the bucket are not replaced: builders that downloaded the
# old one would keep it, so publish a new version instead.
#
# Usage: upload-openssl.sh <archive>...

set -e

if [ "$#" -eq 0 ]; then
    echo "Usage: $(basename "$0") <archive>..."
    echo "e.g.   $(basename "$0") ../osx/openssl/openssl-3.5.9-macos-*.tar.gz"
    exit 1
fi

AWS_BUCKET="s3://rstudio-buildtools"

# Check every archive before uploading any
for FILE in "$@"; do
    NAME=$(basename "${FILE}")
    if ! [[ "${NAME}" =~ ^openssl-[0-9]+\.[0-9]+\.[0-9]+(-macos-(arm64|x86_64)\.tar\.gz|\.zip)$ ]]; then
        echo "error: '${FILE}' is not an OpenSSL archive name the dependency scripts download" >&2
        echo "expected openssl-<version>-macos-<arm64|x86_64>.tar.gz or openssl-<version>.zip" >&2
        exit 1
    fi
    if ! [ -f "${FILE}" ]; then
        echo "error: '${FILE}' does not exist" >&2
        exit 1
    fi
done

# Check that we're logged in with AWS
aws sts get-caller-identity || aws sso login

for FILE in "$@"; do
    NAME=$(basename "${FILE}")
    if aws s3api head-object --bucket "${AWS_BUCKET#s3://}" --key "${NAME}" > /dev/null 2>&1; then
        echo "error: '${AWS_BUCKET}/${NAME}' already exists; not replacing it" >&2
        exit 1
    fi
done

for FILE in "$@"; do
    aws s3 cp "${FILE}" "${AWS_BUCKET}/$(basename "${FILE}")" --acl public-read
done
