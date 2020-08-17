#!/usr/bin/env bash

# This script copies Pandoc release binaries into the RStudio Build Tools
# (rstudio-buildtools) S3 bucket. Presumes you've already got AWS command line
# tools (awscli) installed, and configured with a valid AWS account.
# 
# Pandoc is not shy about changing the formulation of their paths and download
# filenames, so tweaking for new releases is expected.

# Modify to set the Pandoc version to upload
PANDOC_VERSION=2.10.1

for PLATFORM in linux-amd64.tar.gz macOS.zip windows-x86_64.zip; do

    # Form filename from version and platform
    FILENAME=pandoc-$PANDOC_VERSION-$PLATFORM

    # Download from Pandoc release site
    wget https://github.com/jgm/pandoc/releases/download/$PANDOC_VERSION/$FILENAME

    # Upload to S3 bucket
    aws s3 cp $FILENAME s3://rstudio-buildtools/pandoc/$PANDOC_VERSION/ --acl public-read

    # Clean up
    rm $FILENAME
done
