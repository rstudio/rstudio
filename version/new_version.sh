#!/usr/bin/env bash

# new_version.sh
#
# A helper script to update and generate version files for RStudio. 
# It should be run from the root directory of the repository.
#
# This script is intended to be used when branching for release, or for when creating a new release branch 
# to start work on future development for that release. Upon completion, the script will stage the changes.
# The developer should review these changes, then commit to the repository.
#
#   Branching for Release
#   ---------------------
#   This script can be run to update version files on the main branch for the next release after 
#   a release branch has been created.
#
#   In this scenario, the developer may need to determine if a release branch already exists, and specify 
#   the base commit (or initial fork point) for that branch now that work.
#   
#
#   Creating a Release Branch for future work
#   ----------------------------------------- 
#   When creating a new release branch for future work, the script should be run on the release branch 
#   after it has been created.
#
#   In this scenario, it may be acceptable to use the default [base commit sha], since no prior work 
#   will have been done for the new release yet.
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

if [[ "$#" -lt 2 ]]; then
    echo "Usage: new_version.sh <release flower> <calendar version> [base commit sha]"
    echo "       <release flower>   - The name of the release flower (i.e., \"Cherry Blossom\")"
    echo "       <calendar version> - The calendar version (i.e., \"2023.03\")"
    echo "       [base commit sha]  - The base commit sha that should be used for the [flower].BASE_COMMIT file (optional)"
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

BASE_COMMIT_FLOWER_NAME=$(echo ${RELEASE_FLOWER} | tr '[ ]' '-' | tr -d '[:space:]' | tr '[:upper:]' '[:lower:]')
# Next, check if the base commit file already exists. If it does, we need to exit and provide an error message.
BASE_COMMIT_FILE="version/base_commit/${BASE_COMMIT_FLOWER_NAME}.BASE_COMMIT"
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

# Create a NEWS.md file
{
    echo "## RStudio ${CALENDAR_VERSION}.0 \"$RELEASE_FLOWER\" Release Notes"
    echo ""
    echo "### New"
    echo "-"
    echo ""
    echo "### Fixed"
    echo "-"
    echo ""
    echo "### Accessibility Improvements"
    echo "-"
    echo ""
} > NEWS.md


# Create the base commit file using the base commit sha
echo "${BASE_COMMIT_SHA}" > $BASE_COMMIT_FILE

git add version/BUILDTYPE version/RELEASE version/CALENDAR_VERSION NEWS.md $BASE_COMMIT_FILE

# Print a success message
echo "Successfully updated and staged version files and created NEWS.md and $BASE_COMMIT_FILE"
echo "Please commit these changes and push them to the remote repository. i.e., git commit -m \"Update version files for $RELEASE_FLOWER\" && git push"
