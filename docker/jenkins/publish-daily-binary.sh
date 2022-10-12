#!/usr/bin/env bash

# publish-daily-binary.sh

# abort on error
set -e

if [[ "$#" -lt 2 ]]; then
    echo "Usage: publish-daily-binary.sh [https://url/to/binary/rstudio.deb] [identity.pem]"
    exit 1
fi

# get name of package to publish
URL=$1
IDENTITY=$2
FILENAME="${URL##*/}"

# parse the URL into components; save/restore IFS (field separator)
OLDIFS="$IFS"
IFS='/'
read -ra COMPONENTS <<< "$URL"
IFS="$OLDIFS"

# ensure sufficient components
if [[ ${#COMPONENTS[@]} -lt 7 ]]; then
    echo "Unexpected URL format '$URL'"
    exit 1
fi

# parse URL components to extract flavor/OS/platform
FLAVOR=${COMPONENTS[4]}
OS=${COMPONENTS[5]}
# linux uses: /flavor/os/platform but windows/mac only have /flavor/os
if [ "$OS" == "macos" ] || [ "$OS" == "windows" ];  then
   PLATFORM=""
else
   PLATFORM=${COMPONENTS[6]}
fi

# sanity check a URL component to fail faster if the URL is not in the format
# we expect
if [ "$FLAVOR" != "desktop" ] && [ "$FLAVOR" != "server" ] && [ "$FLAVOR" != "electron" ]; then
    echo "Unsupported flavor '$FLAVOR' (expected 'desktop', 'electron', or 'server')"
    exit 1
fi

# macOS => mac for URL
if [ "$OS" == "macos" ];  then
    OS="mac"
fi

# The links for latest now always point to desktop since we don't do all of the gwt desktop builds
if [ "$FLAVOR" == "electron" ] ; then
    FLAVOR="desktop"
fi

# figure out the "latest" package name by replacing the version number with "latest"; for example
# for "rstudio-workbench-2021.09.0-daily-123.pro1.deb", we want "rstudio-workbench-latest.deb"
LATEST=$(echo "$FILENAME" | sed -e 's/[[:digit:]][[:digit:]][[:digit:]][[:digit:]]\.[[:digit:]][[:digit:]]\.[[:digit:]][[:digit:]]*-daily-[[:digit:]][[:digit:]]*\(\.pro[[:digit:]][[:digit:]]*\)*/latest/')
echo "Publishing $FILENAME as daily $FLAVOR build for $OS ($PLATFORM): $LATEST..."

# download the current .htaccess file to a temporary location
HTACCESS=$(mktemp)
SCP_COUNT=0
echo "Fetching .htaccess for update..."
until scp -o StrictHostKeyChecking=no -i $IDENTITY www-data@rstudio.org:/srv/www/rstudio.org/public_html/download/latest/daily/.htaccess $HTACCESS || (( SCP_COUNT++ >= 5))
do
   echo "Error fetching .htaccess - retrying: ${SCP_COUNT}"
   sleep 5
done

if [ "$SCP_COUNT" -ge "5" ] ; then
   echo "Error fetching .htaccess - giving up after ${SCP_COUNT} retries"
   exit
fi

# .htaccess expects URL encoded URLs so replace the + with %2B
ENC_URL=`echo $URL | sed -e 's/+/%2B/'`

if grep "/${FLAVOR}/${OS}/${LATEST}" $HTACCESS > /dev/null ; then

   # replace existing redirect
   sed -i.bak "s/${FLAVOR}\/${OS}\/${LATEST} .*/${FLAVOR}\/${OS}\/${LATEST} ${ENC_URL//\//\\/}/" $HTACCESS

   echo "Updated daily URL https://rstudio.org/download/latest/daily/${FLAVOR}/${OS}/${LATEST} to ${ENC_URL}"
else
   echo "  - daily URL - not found - appending new entry:"
   echo "Redirect 302 /download/latest/daily/${FLAVOR}/${OS}/${LATEST} ${ENC_URL}"
   echo "Redirect 302 /download/latest/daily/${FLAVOR}/${OS}/${LATEST} ${ENC_URL}" >> $HTACCESS
fi

# copy it back up
echo "Uploading new .htaccess..."
SCP_COUNT=0
until scp -o StrictHostKeyChecking=no -i $IDENTITY $HTACCESS www-data@rstudio.org:/srv/www/rstudio.org/public_html/download/latest/daily/.htaccess || (( SCP_COUNT++ >= 5))
do
   echo "Error updating .htaccess with scp - retrying: ${SCP_COUNT}"
   sleep 5
done

if [ "$SCP_COUNT" -ge "5" ] ; then
   echo "Error updating .htaccess - giving up after ${SCP_COUNT} retries"
   exit
fi

# clean up
rm -f $HTACCESS
rm -f $HTACCESS.bak

