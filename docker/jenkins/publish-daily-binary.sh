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
PLATFORM=${COMPONENTS[6]}

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

# figure out the "latest" package name by replacing the version number with "latest"; for example
# for "rstudio-workbench-2021.09.0-daily-123.pro1.deb", we want "rstudio-workbench-latest.deb"
LATEST=$(echo "$FILENAME" | sed -e 's/[[:digit:]][[:digit:]][[:digit:]][[:digit:]]\.[[:digit:]][[:digit:]]\.[[:digit:]][[:digit:]]*-daily-[[:digit:]][[:digit:]]*\(\.pro[[:digit:]][[:digit:]]*\)*/latest/')
echo "Publishing $FILENAME as daily $FLAVOR build for $OS ($PLATFORM): $LATEST..."

# download the current .htaccess file to a temporary location
HTACCESS=$(mktemp)
echo "Fetching .htaccess for update..."
scp -o StrictHostKeyChecking=no -i $IDENTITY www-data@rstudio.org:/srv/www/rstudio.org/public_html/download/latest/daily/.htaccess $HTACCESS

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
scp -o StrictHostKeyChecking=no -i $IDENTITY $HTACCESS www-data@rstudio.org:/srv/www/rstudio.org/public_html/download/latest/daily/.htaccess

# clean up
rm -f $HTACCESS
rm -f $HTACCESS.bak

