#!/usr/bin/env bash

if [ $# -eq 0 ]; then
    echo "publish-build.sh: utility for adding builds to the latest builds site."
    echo ""
    echo "Creates, commits, and pushes a Markdown file containing the specified build metadata."
    echo ""
    echo "Arguments:"
    echo ""
    echo "--build   What kind of build is being added, as a path. Example: "
    echo "          rstudio/flower-name/desktop-pro/windows"
    echo ""
    echo "--url     A URL to a location where the build can be downloaded. Example:"
    echo "          https://s3.amazonaws.com/rstudio-ide-build/desktop/windows/RStudio-pro.exe"
    echo ""
    echo "--file    A path to a local copy of the build file. Example:"
    echo "          /tmp/RStudio-pro.exe"
    echo ""
    echo "--commit  The long SHA1 hash of the Git commit on which the build is based. Example:"
    echo "          17cb842363757497c5901c9482ace78ae16c2cbc"
    echo ""
    echo "--channel The build channel. One of Daily, Preview, or Release."
    echo ""
    echo "--version The full build version. Example: 2025.09.0-daily+123"
    exit 0
fi


# List of arguments
ARGUMENT_LIST=(
    "build"
    "url"
    "file"
    "commit"
    "channel"
    "version"
)

# Parse arguments with getopt
opts=$(getopt \
    --longoptions "$(printf "%s:," "${ARGUMENT_LIST[@]}")" \
    --name "$(basename "$0")" \
    --options "" \
    -- "$@"
)

# Apply to variables
while [[ $# -gt 0 ]]; do
    case "$1" in
        --build)
            build=$2
            shift 2
            ;;

        --url)
            url=$2
            shift 2
            ;;

        --file)
            file=$2
            shift 2
            ;;

        --commit)
            commit=$2
            shift 2
            ;;

        --channel)
            channel=$2
            shift 2
            ;;

        --version)
            version=$2
            shift 2
            ;;
        *)
            break
            ;;
    esac
done

# Check args
if [ -z "$build" ]; then
    echo "Build not set; specify a build with --build. Example: --build rstudio/flower-name/desktop/windows"
    exit 1
fi

if [ -z "$url" ]; then
    echo "URL not set; specify a build with --url. Example: --url https://s3.amazonaws.com/rstudio-ide-build/desktop/windows/RStudio-pro.exe"
    exit 1
fi

if [ -z "$file" ]; then
    echo "File not set; specify a file with --file. Example: --file /tmp/RStudio-pro.exe"
    exit 1
fi

if [ -z "$commit" ]; then
    echo "Commit not set; specify a Git commit with --commit. Example: --commit 17cb842363757497c5901c9482ace78ae16c2cbc"
    exit 1
fi

if [ -z "$channel" ]; then
    echo "Channel not set; specify a channel with --channel. Example: --channel Daily"
    exit 1
fi

if [ -z "$version" ]; then
    echo "Version not set; specify a version with --version. Example: --version 2025.09.0-daily+123"
    exit 1
fi

# Determine file size
size=$(wc -c $file| awk '{print $1}')

# Determine file SHA256 sum
sha256=$(sha256sum $file| awk '{print $1}')

# Form ISO 8601 timestamp
timestamp=$(date +"%Y-%m-%dT%H:%M:%S%z")

# Create version stem. This is a very file-safe version of the version: first
# we replace non-alphanumerics with dashes, then collapse multiple dashes to a
# single dash.
version_stem=$(echo $version | sed -e 's/[^a-zA-Z0-9-]/-/g' | sed -e 's/--*/-/g')

filename=$(basename $file)

echo "$platform/$version_stem.md"
echo "---"
echo "type: build"
echo "link: \"$url\""
echo "filename: \"$filename\""
echo "sha256: \"$sha256\""
echo "channel: \"$channel\""
echo "version: \"$version\""
echo "commit: \"$commit\""
echo "size: \"$size\""
echo "date: \"$timestamp\""
echo "---"


