#!/usr/bin/env bash
#
# RStudio Release Notarization (notarize-release.sh)
# 
# Copyright (C) 2020 by RStudio, PBC
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

# Counter for the number of queries we have made
QUERIES=0

# Wait for notarization to complete
echo "Waiting for notarization to complete. This will take several minutes."
while true; do 

    # Wait for Apple to do its thing
    sleep 30

    # Use xcrun to inquire about the status of the notarization request we just made
    ((QUERIES=QUERIES+1))
    echo "Checking notarization status (query $QUERIES)..."
    xcrun altool --notarization-info $REQUEST_UUID --username $APPLE_ID --password "@env:APPLE_ID_PASSWORD" --output-format xml > $XCRUN_RESULT

    # Use PlistBuddy (ships with macOS) to parse the XML and extract the status
    NOTARIZATION_STATUS=$(/usr/libexec/PlistBuddy -c "Print :notarization-info:Status" $XCRUN_RESULT)

    if [ $? -eq 0 ]; then
        if [ "$NOTARIZATION_STATUS" != "in progress" ]; then 
            echo "Notarization ended; result: $NOTARIZATION_STATUS"
            break
        fi
        echo "Notarization still in progress. Waiting 30s to check again."
    else
        # Sometimes Apple will give us a request UUID, but then deny it exists
        # when we query its status. This is somewhat rare (perhaps around 5% of
        # notarization requests); we hypothesize that it's because the
        # notarization request UUID may take more than 30s to fully register
        # with Apple's servers. To work around the problem, we retry again with
        # the same UUID.
        ERROR_CODE=$(/usr/libexec/PlistBuddy -c "Print :product-errors:0:code" $XCRUN_RESULT)
        if [ "$ERROR_CODE" -eq "1519" ]; then
            # Error code 1519 = "Could not find the RequestUUID."
            echo "Notarization request UUID not ready. Waiting 30s to try again."
        else 
            # Some other error, which is not something we know how to handle
            # and should consequently result in termination.
            echo "Could not determine notarization status; giving up with unknown error $ERROR_CODE. Server response:" 
            cat $XCRUN_RESULT
            exit 1
        fi
    fi
    
    # We don't want to hang the build indefinitely, so stop now if we have
    # spent more than 60 minutes waiting for a response (Apple indicates that
    # this should actually take 5 minutes).
    #
    # https://developer.apple.com/documentation/xcode/notarizing_macos_software_before_distribution/customizing_the_notarization_workflow
    if [ $QUERIES -gt 120 ]; then
        echo "Notarization not ready after an hour; giving up."
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

