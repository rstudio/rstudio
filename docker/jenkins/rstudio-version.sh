#!/usr/bin/env bash

# rstudio-version.sh
#
# This script reads, and updates, official RStudio build numbers, based on
# information stored in S3 and observed in the local repository.
#
# The rules observed are as follows:
# 
# 1. Every build of the open source version of RStudio results in a new patch release
#    formatted as:
#
#    major.minor.patch
# 
#    On S3, the file 'oss-patch.csv' contains a list of open source commits and
#    their associated patch versions, both as a baseline for determining future
#    patch versions and so that subsequent Pro builds can determine their
#    derivative versions.
#
# 2. The build number of RStudio Pro is formatted as follows:
#
#    major.minor.patch-suffix
#
#    where "major.minor.patch" is the version of RStudio Open Source from which
#    the Pro repo is derived, and "suffix" is the number of builds of Pro that
#    have been made based on the associated open source version.
#
# The script is typically used by the build script to bump the build versions,
# but it can also be invoked manually. Pass "debug" as the last parameter to
# see what the script would do (in this mode debug output is written and no
# changes are saved to S3).

if [[ "$#" -lt 2 ]]; then
    # TODO: add "set" command to move forward 
    echo "Usage: rstudio-version.sh [get|bump] [major.minor] [debug]"
    exit 1
fi

# read arguments
ACTION=$1
VERSION=$2
if [[ "$3" == "debug" ]]; then
    DEBUG=true
else
    DEBUG=false
fi

function log() { 
    if [[ $DEBUG = true ]]; then 
        echo "$@"
    fi
}

if [[ $DEBUG = false ]]; then
    EXTRA_CP_ARGS=--quiet
fi

# get historical open source patch versions from AWS; this file is a CSV that
# contains each open source commit and the patch version associated with it
aws s3 cp s3://rstudio-ide-build/version/$VERSION/oss-patch.csv /tmp/oss-patch.csv $EXTRA_CP_ARGS

if [[ -e "upstream/VERSION" ]]; then
    # only one upstream commit (RStudio Pro, which is downstream)
    PRO=true
    COMMITS=$(cat upstream/VERSION)
else
    # no upstream commit; retrieve the most recent 100 commits in this repo and
    # format as a bash array (we need to work backwards until we find one in
    # the build history, if any)
    OPEN_SOURCE=true
    COMMITS=($(git log --pretty=format:"%H" -n 100))
fi

# read the CSV listing open source commits and associated patch releases
MAX_PATCH=0
while read HISTORY; do
    ENTRY=(${HISTORY//,/ })
    ENTRY_COMMIT=${ENTRY[0]}
    ENTRY_PATCH=${ENTRY[1]}

    # record the highest patch we've seen so far
    if [[ $MAX_PATCH -lt $ENTRY_PATCH ]]; then
        MAX_PATCH=$ENTRY_PATCH
    fi

    # check this entry to see if it corresponds to a commit in our history
    for i in "${!COMMITS[@]}"; do
        if [[ "${COMMITS[$i]}" == "$ENTRY_COMMIT" ]]; then
            PATCH_INDEX=$i
            PATCH=${ENTRY[1]}
            log "Found patch version $PATCH at revision $PATCH_INDEX ($ENTRY_COMMIT)"
            break
        fi
    done

    # if we found a patch release, we're done
    if [[ -n "$PATCH" ]]; then
        break
    fi
done < /tmp/oss-patch.csv

# did we find a patch version? if not, just use the highest one we found
if [[ -z "$PATCH" ]]; then
    log "Warning: no patch found for commit ${COMMITS[0]}; presuming $MAX_PATCH"
    PATCH=$MAX_PATCH
fi

# for pro, we need to determine the version suffix as well
if [[ $PRO = true ]]; then
    SUFFIX=0
    aws s3 cp s3://rstudio-ide-build/version/$VERSION/pro-suffix.csv /tmp/pro-suffix.csv --quiet
    while read PRO_SUFFIX; do
        ENTRY=(${PRO_SUFFIX//,/ })
        ENTRY_PATCH=${ENTRY[0]}
        ENTRY_SUFFIX=${ENTRY[1]}

        # record the highest suffix we've seen so far for the patch
        if [[ "$PATCH" == "$ENTRY_PATCH" ]] && [[ $SUFFIX -lt $ENTRY_SUFFIX ]]; then
            SUFFIX=$ENTRY_SUFFIX
        fi
    done < /tmp/pro-suffix.csv
fi

# now figure out what we were asked to do
case "$ACTION" in
    get)
    if [[ $OPEN_SOURCE = true ]]; then
        echo "$VERSION.$PATCH"
    elif [[ $PRO = true ]]; then
        echo "$VERSION.$PATCH-$SUFFIX"
    fi
    ;;
    
    bump)

    # record date for timestamp in CSV
    TIMESTAMP=$(date -u '+%Y-%m-%d %H:%M:%S')
        
    if [[ $OPEN_SOURCE = true ]]; then

        # increment to highest observed patch release
        PATCH=$(($MAX_PATCH+1))

        OSS_VERSION="$VERSION.$PATCH"

        log "Creating new patch release $OSS_VERSION"

        # write temporary file marking all commits until the last known one as
        # belonging to this build
        rm -f /tmp/history-prepend.csv && touch /tmp/history-prepend.csv
        for ((i = 0; i <= PATCH_INDEX - 1; i++)); do
            log "Marking commit ${COMMITS[$i]} for patch $OSS_VERSION"     
            echo "${COMMITS[$i]},$PATCH,$TIMESTAMP" >> /tmp/history-prepend.csv
        done

        # now prepend and push to s3
        cat /tmp/history-prepend.csv /tmp/oss-patch.csv > /tmp/oss-updated.csv
        if [[ $DEBUG = true ]]; then
            echo "Push updated patch history to S3 and git"
        else
            # upload to s3
            aws s3 cp /tmp/oss-updated.csv s3://rstudio-ide-build/version/$VERSION/oss-patch.csv --quiet

            # tag the release on git (TODO: need Jenkins creds to do this)
            # git tag "v$OSS_VERSION"
            # git push -q origin "v$OSS_VERSION"
        fi

        # echo newly created version
        echo "$OSS_VERSION"

    elif [[ $PRO = true ]]; then

        # increment to highest observed suffix
        SUFFIX=$(($SUFFIX+1))

        PRO_VERSION="$VERSION.$PATCH-$SUFFIX"

        log "Creating new Pro patch release $PRO_VERSION"

        # prepend and push to s3
        echo "$PATCH,$SUFFIX,$TIMESTAMP" > /tmp/suffix-prepend.csv
        cat /tmp/suffix-prepend.csv /tmp/pro-suffix.csv > /tmp/pro-updated.csv
        if [[ $DEBUG = true ]]; then
            echo "Push updated suffix to S3 and git"
        else
            aws s3 cp /tmp/pro-updated.csv s3://rstudio-ide-build/version/$VERSION/pro-suffix.csv $EXTRA_CP_ARGS

            # tag the release on git (TODO: need Jenkins creds to do this)
            # git tag "v$PRO_VERSION-pro"
            # git push -q origin "v$PRO_VERSION-pro"
        fi

        # echo newly created version
        echo "$PRO_VERSION"
    fi
esac

