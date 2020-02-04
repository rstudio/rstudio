#!/usr/bin/env bash
#
# RStudio Release Notarization (notarize-release.sh)
# 
# Copyright (C) 2009-19 by RStudio, PBC
#
# Unless you have received this program directly from RStudio pursuant
# to the terms of a commercial license agreement with RStudio, then
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
#    APPLE_ID: the ID of the Apple account under which to submit the
#      notarization request
#    APPLE_ID_PASSWORD: An app-specific password (NOT the primary password) for
#      the Apple ID. See details here: https://support.apple.com/en-us/HT204397
#  

if [[ "$#" -lt 1 ]]; then
    echo "Usage: notarize-release.sh [path-to-dmg]"
    exit 1
fi

# Validate environment vars
if [ -z "$APPLE_ID" ]; then
    echo "Please set the environment variable APPLE_ID to the AppleID under which to submit the notarization request."
    exit 1
fi
if [ -z "$APPLE_ID_PASSWORD" ]; then
    echo "Please set the environment variable APPLE_ID_PASSWORD to the password to the account named in the APPLE_ID environment variable."
    exit 1
fi

# Submit the notarization request to Apple
echo "Submitting notarization request using account $APPLE_ID..."
XCRUN_RESULT="$(mktemp)"
xcrun altool --notarize-app \
    --primary-bundle-id "org.rstudio.RStudio" \
    --username $APPLE_ID \
    --password "@env:APPLE_ID_PASSWORD" \
    --file $1 \
    --output-format xml \
    -itc_provider RStudioInc > $XCRUN_RESULT

# Check result
if [ $? -eq 0 ]; then
    # Extract the request UUID from the result
    REQUEST_UUID=$(/usr/libexec/PlistBuddy -c "Print :notarization-upload:RequestUUID" $XCRUN_RESULT)
    echo "Notarization request with UUID $REQUEST_UUID created."
else
    echo "Notarization request submission failed. Server response:"
    cat $XCRUN_RESULT
    exit 1
fi

# Wait for notarization to complete
echo "Waiting for notarization to complete. This will take several minutes."
while true; do 
    sleep 30
    echo "Checking notarization status..."
    xcrun altool --notarization-info $REQUEST_UUID --username $APPLE_ID --password "@env:APPLE_ID_PASSWORD" --output-format xml > $XCRUN_RESULT
    NOTARIZATION_STATUS=$(/usr/libexec/PlistBuddy -c "Print :notarization-info:Status" $XCRUN_RESULT)
    if [ $? -eq 0 ]; then
        if [ "$NOTARIZATION_STATUS" != "in progress" ]; then 
            echo "Notarization ended; result: $NOTARIZATION_STATUS"
            break
        fi
        echo "Notarization still in progress. Waiting 30s to check again."
    else
        echo "Could not determine notarization status; giving up. Server response:" 
        cat $XCRUN_RESULT
        exit 1
    fi
done

# Staple the notarization ticket to the app bundle
if [ "$NOTARIZATION_STATUS" == "success" ]; then
    echo "Notarization successful; stapling ticket to app bundle"
    xcrun stapler staple $1
else
    echo "Notarization failed."
    exit 1
fi

