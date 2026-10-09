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

AWS_BUCKET="rstudio-buildtools"

# Check every archive before uploading any
ARCHIVE_PATTERN='^openssl-[0-9]+\.[0-9]+\.[0-9]+(-macos-(arm64|x86_64)\.tar\.gz|\.zip)$'
NAMES=""
for FILE in "$@"; do
    NAME=$(basename "${FILE}")
    if ! [[ "${NAME}" =~ ${ARCHIVE_PATTERN} ]]; then
        echo "error: '${FILE}' is not an OpenSSL archive name the dependency scripts download" >&2
        echo "expected openssl-<version>-macos-<arm64|x86_64>.tar.gz or openssl-<version>.zip" >&2
        exit 1
    fi
    if ! [ -f "${FILE}" ]; then
        echo "error: '${FILE}' does not exist" >&2
        exit 1
    fi
    # both copies would pass the existence check, and the second upload fail
    if [[ " ${NAMES} " == *" ${NAME} "* ]]; then
        echo "error: more than one archive is named '${NAME}'" >&2
        exit 1
    fi
    NAMES="${NAMES} ${NAME}"
done

# Check that we're logged in with AWS
aws sts get-caller-identity || aws sso login

# Check every key before uploading any, so a clash doesn't leave a partial set.
# S3 reports a missing key as 404, or as 403 to a caller without s3:ListBucket;
# anything else (e.g. a timeout) means we can't tell. A 403 for a key that does
# exist is still caught by --if-none-match below.
for FILE in "$@"; do
    NAME=$(basename "${FILE}")
    if HEAD_ERROR=$(aws s3api head-object --bucket "${AWS_BUCKET}" --key "${NAME}" \
            2>&1 > /dev/null); then
        echo "error: 's3://${AWS_BUCKET}/${NAME}' already exists; not replacing it" >&2
        exit 1
    elif [[ "${HEAD_ERROR}" != *"(404)"* && "${HEAD_ERROR}" != *"(403)"* ]]; then
        echo "error: could not check whether 's3://${AWS_BUCKET}/${NAME}' exists: ${HEAD_ERROR}" >&2
        exit 1
    fi
done

# --if-none-match makes S3 itself refuse to replace a key uploaded since the check
for FILE in "$@"; do
    NAME=$(basename "${FILE}")
    aws s3api put-object --bucket "${AWS_BUCKET}" --key "${NAME}" --body "${FILE}" \
        --acl public-read --if-none-match '*' > /dev/null
    echo "Uploaded s3://${AWS_BUCKET}/${NAME}"
done
