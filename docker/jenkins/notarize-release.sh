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
#    notarize-release.sh [path-to-dmg-or-zip]
#
# where the argument is a relative or absolute path to the DMG (or, for the
# in-app updater's artifact, the zip) to notarize.
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
trap 'rm -f ${log}' EXIT

# The team-id uniquely identifies the organization with Apple. It does not need to be generated again and is found in the Apple developer account.
# Output streams to the console and to a log we inspect afterwards.
xcrun notarytool submit --wait   \
    --apple-id "${APPLE_ID_USR}" \
    --password "${APPLE_ID_PSW}" \
    --team-id FYF2F5GFX4         \
    --progress                   \
    "$1" 2>&1 | tee "${log}"
status=${PIPESTATUS[0]}

uuid=$(grep -m1 -E "^\s*id:" "${log}" | cut -d: -f2 | xargs)

# notarytool reports a rejected submission in its output rather than its exit
# status, so require an explicit acceptance as well as a clean exit before
# calling this a success for either artifact type.
if [[ "${status}" -ne 0 ]] || ! grep -q "status: Accepted" "${log}"; then

    if [[ -n "${uuid}" ]]; then
        echo
        echo ----------------------------------------
        xcrun notarytool log             \
            --apple-id "${APPLE_ID_USR}" \
            --password "${APPLE_ID_PSW}" \
            --team-id FYF2F5GFX4         \
            "${uuid}"
        echo ----------------------------------------
        echo
    fi

    echo "Notarization failed (notarytool exit status ${status})."
    exit 1

fi

# Staple the result to the DMG (allows offline verification of notarization).
# A zip cannot carry a ticket; the updater's zip is verified online by
# Gatekeeper, and the app inside it carries the same signature as the DMG's.
case "$1" in
    *.zip)
        echo "Not stapling '$1': zip archives cannot hold a notarization ticket."
        ;;
    *)
        xcrun stapler staple "$1"
        ;;
esac
