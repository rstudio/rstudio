#!/usr/bin/env bash

# This script assembles the MathJax 4 bundle used by RStudio (see
# build-mathjax4) and uploads it as a zip to the RStudio Build Tools
# (rstudio-buildtools) S3 bucket. Presumes you've already got AWS command
# line tools (awscli) installed, and configured with a valid AWS account.

set -e

# Modify to set the MathJax version to upload
MATHJAX4_VERSION=4.1.3

AWS_BUCKET="s3://rstudio-buildtools"

WORK_DIR="$(mktemp -d)"
trap 'rm -rf "${WORK_DIR}"' EXIT

# Assemble the pruned bundle (creates ${WORK_DIR}/mathjax-4)
"$(dirname "${BASH_SOURCE[0]}")/build-mathjax4" "${WORK_DIR}" "${MATHJAX4_VERSION}"

# Zip it up; the archive should unpack to a 'mathjax-4' directory
FILENAME="mathjax-${MATHJAX4_VERSION}.zip"
(cd "${WORK_DIR}" && zip -r -9 -q "${FILENAME}" "mathjax-4")

# Upload to S3 bucket
aws s3 cp "${WORK_DIR}/${FILENAME}" "${AWS_BUCKET}/" --acl public-read
