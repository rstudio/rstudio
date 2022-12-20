#!/usr/bin/env bash

# rstudio-version.sh
#
# This script reads official RStudio build numbers based on
# information observed in the local repository.
#
# The rules observed are as follows:
#
# 1. Every build of the open source version of RStudio results in a new patch release
#    formatted as:
#
#    <YYYY>.<MM>.<patch>[-(dev|daily|preview)]+<build number>
#
#    <YYYY>.<MM>
#    ===========
#    The first two sections, year and month, indicate the date when the branch was 
#    initially released. For development, daily, and preview branches, it's always 
#    the expected year and month of release. In the event that the release takes place 
#    early or late, the year and month will be updated accordingly.
#
#    <patch>
#    =======
#    The patch version is incremented with each subsequent release of the branch.
#
#    - Patch version 0: The first release from the branch.
#    - Patch version 1: The second release from the branch.
#    ...
#    - Patch version N: The N + 1 release from the branch.
#
#    [-(dev|daily|preview)]
#    ======================
#    This optional section indicates that the build is not a stable release build, and 
#    also indicates the degree of instability. Local development builds will include -dev 
#    in the version (least stable). Daily builds will include -daily, and preview builds will 
#    include -preview (most stable). Only stable releases should omit this value.
#
#    <build number>
#    ==============
#    The build number indicates the number of commits that have been introduced on the 
#    branch since work on the release has started. In the local repository, the file 
#    'version/base_commit/CALENDAR_VERSION.BASE_COMMIT' contains the commit hash of the 
#    base (starting) commit from which work on that calendar version started. This is 
#    used to determine the number of commits that have been made since the base commit, 
#    which is used as the patch version.
#
# 2. The build number of RStudio Pro is formatted as follows:
#
#    <YYYY>.<MM>.<patch>[-(dev|daily|preview)]+<build number>.pro<pro suffix>
#
#    All release designators match the open source version, based on the open source
#    commit hash stored in the upstream/VERSION file, with the addition of the pro suffix.
#
#    .pro<pro suffix>
#    ================
#    The pro suffix is generated in a similar manner to <build number> for the open source
#    version, but it is based on the number of commits to the pro branch since open source
#    was merged into the pro branch. In the local repository, the file upstream/VERSION
#    contains the commit hash is used for this determination.
#
# The script is typically used by the build script to bump the build versions,
# but it can also be invoked manually. Pass "debug" as the last parameter to
# see what the script would do (in this mode debug output is written and no
# changes are saved to S3).

# abort on error
set -e

RSTUDIO_ROOT_DIR="$(readlink -f $(dirname "${BASH_SOURCE[0]}")/../..)"

if [[ "$#" -lt 1 ]]; then
    echo "Usage: rstudio-version.sh [patch] [debug]"
    exit 1
fi

# read arguments
PATCH=$1
if [[ "$2" == "debug" ]]; then
    DEBUG=true
else
    DEBUG=false
fi

function log() {
    if [[ $DEBUG = true ]]; then
        echo "$@"
    fi
}

function buildType() {
    if [ -e "$RSTUDIO_ROOT_DIR/version/BUILDTYPE" ]; then
        BUILD_TYPE="$(cat "$RSTUDIO_ROOT_DIR/version/BUILDTYPE" | tr '[ ]' '-' | tr -d '[:space:]' | tr '[:upper:]' '[:lower:]')"
        if [[ -n "$BUILD_TYPE" && "$BUILD_TYPE" != "release" ]]; then
            echo "-${BUILD_TYPE}"
        else
            echo ""
        fi
    else
        echo "The $RSTUDIO_ROOT_DIR/version/BUILDTYPE file does not exist. A build version could not be generated" >&2
        exit 1
    fi
}

function flower() {
    if [ -e "$RSTUDIO_ROOT_DIR/version/RELEASE" ]; then
        echo "$(cat "$RSTUDIO_ROOT_DIR/version/RELEASE" | tr '[ ]' '-' | tr -d '[:space:]' | tr '[:upper:]' '[:lower:]')"
    else
        echo "The $RSTUDIO_ROOT_DIR/version/RELEASE file does not exist. A build version could not be generated" >&2
        exit 1
    fi
}

function calver() {
    if [ -e "$RSTUDIO_ROOT_DIR/version/CALENDAR_VERSION" ]; then
        echo "$(cat "$RSTUDIO_ROOT_DIR/version/CALENDAR_VERSION" | tr -d '[:space:]')"
    else
        echo "The $RSTUDIO_ROOT_DIR/version/CALENDAR_VERSION file does not exist. A build version could not be generated" >&2
        exit 1
    fi
}

function baseCommit() {
    BASE_COMMIT_FILE="$RSTUDIO_ROOT_DIR/version/base_commit/$(calver).BASE_COMMIT"
    if [ -e $BASE_COMMIT_FILE ]; then
        echo "$(cat "$BASE_COMMIT_FILE" | tr -d '[:space:]')"
    else
        echo "The ${BASE_COMMIT_FILE} file does not exist. A build version could not be generated" >&2
        exit 1
    fi
}

VERSION="$(calver).${PATCH}$(buildType)"

BASE_COMMIT=$(baseCommit)
log "BASE_COMMIT: $BASE_COMMIT"

if [[ -e "$RSTUDIO_ROOT_DIR/upstream/VERSION" ]]; then
    PRO=true
    OPEN_SOURCE_COMMIT=$(cat "$RSTUDIO_ROOT_DIR/upstream/VERSION")
    BUILD_NO=$(git rev-list ${BASE_COMMIT}..${OPEN_SOURCE_COMMIT} --count)
    SUFFIX=$(git rev-list ${OPEN_SOURCE_COMMIT}..HEAD --ancestry-path --count)
else
    OPEN_SOURCE=true
    BUILD_NO=$(git rev-list ${BASE_COMMIT}..HEAD --count)
fi

if [[ $OPEN_SOURCE = true ]]; then
    echo "$VERSION+$BUILD_NO"
    elif [[ $PRO = true ]]; then
    echo "$VERSION+$BUILD_NO.pro$SUFFIX"
fi
