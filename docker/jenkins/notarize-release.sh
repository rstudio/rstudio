#!/usr/bin/env bash
#
# RStudio Release Notarization (notarize-release.sh)
# 
# Copyright (C) 2022 by Posit Software, PBC
#
# Unless you have received this program directly from Posit Software pursuant
# to the terms of a commercial license agreement with Posit Software, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
# Notarizes an RStudio release using the public Apple notary service.
#
# Usage: 
#
#    notarize-release.sh [path-to-dmg]
#
# where path-to-dmg is a relative or absolute path to the DMG file to notarize.
#
# Expects the following environment variables to be present:
#  
#    APPLE_ID_USR: the ID of the Apple account under which to submit the
#      notarization request
#    APPLE_ID_PSW: An app-specific password (NOT the primary password) for
#      the Apple ID. See details here: https://support.apple.com/en-us/HT204397
#  

if [[ "$#" -lt 1 ]]; then
    echo "Usage: notarize-release.sh [path-to-dmg]"
    exit 1
fi

# Validate environment vars
if [ -z "${APPLE_ID_USR}" ]; then
    echo "Please set the environment variable APPLE_ID_USR to the AppleID under which to submit the notarization request."
    exit 1
fi
if [ -z "${APPLE_ID_PSW}" ]; then
    echo "Please set the environment variable APPLE_ID_PSW to the password to the account named in the APPLE_ID_USR environment variable."
    exit 1
fi

# Submit the notarization request to Apple
echo "Submitting notarization request using account ${APPLE_ID_USR} ..."

log=/tmp/rstudio-notarization-$$.log
mkfifo "${log}"
trap 'rm -f ${log}' EXIT

# The team-id uniquely identifies the organization with Apple. It does not need to be generated again and is found in the Apple developer account.
xcrun notarytool submit --wait   \
    --apple-id "${APPLE_ID_USR}" \
    --password "${APPLE_ID_PSW}" \
    --team-id FYF2F5GFX4         \
    --progress                   \
    "$1" > "${log}" 2>&1 &

# Stream logs, and collect the UUID used for the submission.
while IFS= read -r line < "${log}"; do
    
    echo "${line}"
    
    if [[ "${line}" =~ "id:" ]]; then
        uuid=$(echo "${line}" | cut -d: -f2 | xargs)
    fi

    if [[ "${line}" =~ "Current status: Invalid" ]]; then
        status="invalid"
    fi

    if [[ "${line}" =~ "Processing complete" ]]; then
        break
    fi

done

# Check for success.
if [[ "${status}" = "invalid" ]]; then

    echo
    echo ----------------------------------------
    xcrun notarytool log             \
        --apple-id "${APPLE_ID_USR}" \
        --password "${APPLE_ID_PSW}" \
        --team-id FYF2F5GFX4         \
        "${uuid}"
    echo ----------------------------------------
    echo

    echo "Notarization failed."
    exit 1

fi

# Staple the result to DMG file (allows offline verification of notarization)
xcrun stapler staple "$1"
