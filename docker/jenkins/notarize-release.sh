#!/usr/bin/env bash
#
# RStudio Release Notarization (notarize-release.sh)
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

# Submit the notarization request to Apple
echo "Submitting notarization request..."
UPLOAD_RESULT=$(xcrun altool --notarize-app \
    --primary-bundle-id "org.rstudio.RStudio" \
    --username $APPLE_ID \
    --password "@env:APPLE_ID_PASSWORD" \
    --file $1 \
    -itc_provider RStudioInc)

# Extract the request UUID from the result
REQUEST_UUID=`/usr/libexec/PlistBuddy -c "Print :notarization-upload:RequestUUID" $(UPLOAD_RESULT)`
echo "Notarization request with UUID $REQUEST_UUID created."

# Wait for notarization to complete
echo "Waiting for notarization to complete. This will take several minutes."
while true; do 
    echo "Checking notarization status..."
    STATUS_RESULT=$(/usr/bin/xcrun altool --notarization-info $REQUEST_UUID --username $APPLE_ID --password "@env:APPLE_ID_PASSWORD" --output-format xml)
    NOTARIZATION_STATUS=`/usr/libexec/PlistBuddy -c "Print :notarization-info:Status" $(STATUS_RESULT)`
    if [ $NOTARIZATION_STATUS != "in progress" ]; then 
        echo "Notarization ended; result: $NOTARIZATION_STATUS"
        break
    fi
    echo "Notarization still in progress. Waiting 30s to check again."
    sleep 30;
done

# Staple the notarization ticket to the app bundle
if [ $NOTARIZATION_STATUS == "success"]; then
    echo "Notarization successful; stapling ticket to app bundle"
    xcrun stapler staple $1
fi

