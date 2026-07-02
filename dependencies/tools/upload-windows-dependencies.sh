#!/usr/bin/env bash

# This script downloads the Boost + SOCI prebuilt archives produced by the
# "Build Dependencies (Windows, Open Source)" GitHub Actions workflow, and
# copies them into the RStudio Build Tools (rstudio-buildtools) S3 bucket.
# Use it until the workflow can upload to S3 directly; see
# https://github.com/rstudio/rstudio/issues/18124 for details.
#
# Presumes you've got the GitHub CLI (gh) and AWS command line tools (awscli)
# installed, and configured with a valid AWS account.
#
# Usage: upload-windows-dependencies.sh <workflow-run-id>

set -e

if [ -z "$1" ]; then
    echo "Usage: $(basename "$0") <workflow-run-id>"
    echo "Find run ids with: gh run list --workflow os-build-dependencies-windows.yml"
    exit 1
fi

RUN_ID="$1"
AWS_BUCKET="s3://rstudio-buildtools"

# Check that we're logged in with AWS
aws sts get-caller-identity || aws sso login

# Download the prebuilt archives from the workflow run
STAGING=$(mktemp -d)
gh run download "${RUN_ID}" --repo rstudio/rstudio --pattern 'windows-dependencies-*' --dir "${STAGING}"

# Upload Boost archives under Boost/, and SOCI archives at the bucket root,
# matching the paths install-dependencies.cmd downloads from.
find "${STAGING}" -name 'boost-*.zip' | while read -r FILE; do
    aws s3 cp "${FILE}" "${AWS_BUCKET}/Boost/$(basename "${FILE}")" --acl public-read
done

find "${STAGING}" -name 'soci-*.zip' | while read -r FILE; do
    aws s3 cp "${FILE}" "${AWS_BUCKET}/$(basename "${FILE}")" --acl public-read
done

rm -rf "${STAGING}"
