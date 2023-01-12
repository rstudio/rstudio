#!/usr/bin/env bash

# new_version.sh
#
# A helper script to update and generate version files for RStudio. 
# It should be run from the root directory of the repository.
#
# Running the script
# ------------------
# ./new_version.sh <release flower> <calendar version> [base commit sha]
#     i.e., ./new_version.sh "Cherry Blossom" 2023.03 [base commit sha]
#
# The base commit sha is optional. If it is not provided, the script will use the current commit hash.
#   A base commit sha SHOULD be provided if a release branch for the calendar version already exists, 
#   and should be the commit hash of the fork point for the release branch. 
#   This is to ensure we "count" the work that was done on the release branch before starting builds.
#
# This script updates the following files:
# - version/BUILDTYPE: Changes the BUILDTYPE to "Daily"
# - version/RELEASE: Updates the release flower name
# - version/CALENDAR_VERSION: Updates the calendar version
#
# This script also generates the following files:
# - NEWS.md: Creates a new NEWS.md file for the release
# - version/base_commit/<calendar version>.BASE_COMMIT: Adds a new base commit for build number generation

# abort on error
set -e

if [[ "$#" -lt 1 ]]; then
    echo "Usage: new_version.sh [release flower] [calendar version]"
    exit 1
fi

# read arguments
RELEASE_FLOWER=$1
CALENDAR_VERSION=$2

# Read the base commit sha if it was provided
if [[ "$#" -eq 3 ]]; then
    BASE_COMMIT_SHA=$3
else
    BASE_COMMIT_SHA=$(git rev-parse HEAD)
fi

# First check if NEWS.md exists. If it does, we need to exit and provide an error message.
if [ -e "NEWS.md" ]; then
    echo "The NEWS.md file already exists." >&2
    exit 1
fi

# Next, check if the base commit file already exists. If it does, we need to exit and provide an error message.
BASE_COMMIT_FILE="version/base_commit/${CALENDAR_VERSION}.BASE_COMMIT"
if [ -e $BASE_COMMIT_FILE ]; then
    echo "The $BASE_COMMIT_FILE file already exists." >&2
    exit 1
fi

# Update the BUILDTYPE file
echo "Daily" > version/BUILDTYPE

# Update the RELEASE file
echo "${RELEASE_FLOWER}" > version/RELEASE

# Update the CALENDAR_VERSION file
echo "${CALENDAR_VERSION}" > version/CALENDAR_VERSION

# Create an empty NEWS.md file
touch NEWS.md

# Create the base commit file using the base commit sha
echo "${BASE_COMMIT_SHA}" > $BASE_COMMIT_FILE

# Print a success message
echo "Successfully updated version files and created NEWS.md and $BASE_COMMIT_FILE"