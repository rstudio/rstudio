#!/usr/bin/env bash

if [ $# -eq 0 ]; then
    echo "publish-build.sh: utility for adding builds to the latest builds site."
    echo ""
    echo "Creates, commits, and pushes a Markdown file containing the specified build metadata."
    echo ""
    echo "Arguments:"
    echo ""
    echo "--build   What kind of build is being added, as a path. Example: "
    echo "          desktop-pro/windows"
    echo ""
    echo "--arch    The CPU architecture supported by the build. Example: arm64"
    echo ""
    echo "--url     A URL to a location where the build can be downloaded. Example:"
    echo "          https://s3.amazonaws.com/rstudio-ide-build/desktop/windows/RStudio-pro.exe"
    echo ""
    echo "--file    A path to a local copy of the build file. Example:"
    echo "          /tmp/RStudio-pro.exe"
    echo ""
    echo "--version The full build version. Example: 2025.09.0-daily+123"
    echo ""
    echo "--pat     The Github Personal Access Token (PAT) to be used to authorize the commit."
    echo "          May be specified in the environment variable GITHUB_PAT instead."
    echo ""
    echo "--channel (optional) The Channel type for the build. One of Hourly, Daily, Preview, Release."
    echo "          Required for setting to Hourly. If not set, the channel will be determined from"
    echo "          the value in the file: version/BUILDTYPE. Channel names are case sensitive."
    exit 0
fi

# abort on error
set -e


# List of arguments
ARGUMENT_LIST=(
    "build"
    "url"
    "file"
    "version"
    "pat"
    "channel"
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
        --arch)
            arch=$2
            shift 2
            ;;

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

        --version)
            version=$2
            shift 2
            ;;

        --pat)
            pat=$2
            shift 2
            ;;

        --channel)
            channel=$2
            shift 2
            ;;

        *)
            break
            ;;
    esac
done

# Check args
if [ -z "$build" ]; then
    echo "Build not set; specify a build with --build. Example: --build desktop/windows"
    exit 1
fi

if [ -z "$arch" ]; then
    # No architecture set? No problem, just ask uname
    arch=$(uname -m)
fi

if [ -z "$url" ]; then
    echo "URL not set; specify a build with --url. Example: --url https:/s3.amazonaws.com/rstudio-ide-build/desktop/windows/RStudio-pro.exe"
    exit 1
fi

if [ -z "$file" ]; then
    echo "File not set; specify a file with --file. Example: --file /tmp/RStudio-pro.exe"
    exit 1
fi

if [ -z "$version" ]; then
    echo "Version not set; specify a version with --version. Example: --version 2025.09.0-daily+123"
    exit 1
fi

if [ -z "$pat" ]; then
    if [ -z "$GITHUB_PAT" ]; then
        echo "Github Personal Access Token (PAT) not set; specify a PAT with --pat or the GITHUB_PAT environment variable."
        exit 1
    else
        # PAT supplied in environment variable; promote to local
        pat="$GITHUB_PAT"
    fi
fi

# if channel is undefined, we'll just use version/BUILDTYPE. If it is provided,
# we make sure there's no typos in the channel name
if [ ! -z $channel ]; then
    if [[ ! "$channel" =~ ^(Hourly|Daily|Preview|Release)$ ]]; then 
        echo "Channel should be one of Hourly, Daily, Preview, or Release (case sensitive)"
        exit 1
    fi
fi

# Determine file size
size=$(wc -c $file | awk '{print $1}')

# Determine file SHA256 sum
if [[ "$OSTYPE" == "darwin"* ]]; then
  sha256=$(shasum -a 256 $file | awk '{print $1}')
else
  sha256=$(sha256sum $file | awk '{print $1}')
fi

# Form ISO 8601 timestamp
timestamp=$(date +"%Y-%m-%dT%H:%M:%S%z")

# Determine release channel (build type) and flower
RSTUDIO_ROOT_DIR="$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && cd ../.. && pwd )"
if [ -z "$channel" ]; then
    channel="$(cat "$RSTUDIO_ROOT_DIR/version/BUILDTYPE" | tr '[ ]' '-' | tr -d '[:space:]')"
fi
flower="$(cat "$RSTUDIO_ROOT_DIR/version/RELEASE" | tr '[ ]' '-' | tr -d '[:space:]' | tr '[:upper:]' '[:lower:]')"

# Determine commit (use local hash)
pushd $RSTUDIO_ROOT_DIR
commit=$(git rev-parse HEAD)
popd

# Escape + characters in URL (for versioning)
url=$(echo $url | sed -e 's/+/%2B/g')

# Create version stem. This is a very file-safe version of the version: first
# we replace non-alphanumerics with dashes, then collapse multiple dashes to a
# single dash.
version_stem=$(echo $version | sed -e 's/[^a-zA-Z0-9-]/-/g' | sed -e 's/--*/-/g')

filename=$(basename $file)

md_contents="---
type: build
date: $timestamp
link: \"$url\"
filename: \"$filename\"
architecture: \"$arch\"
sha256: \"$sha256\"
channel: \"$channel\"
version: \"$version\"
commit: \"$commit\"
size: $size
---
"

echo "Creating $flower/$build/$version_stem.md..."
echo "$md_contents"
if [[ "$OSTYPE" == "darwin"* ]]; then
  base64_contents=$(echo "$md_contents" | base64 --break=0)
else
  base64_contents=$(echo "$md_contents" | base64 --wrap=0)
fi

# The hourly builds upload to a different "product" directory in the dailies page
if [ $channel == "Hourly" ]; then
  product="rstudio-hourly"
else
  product="rstudio"
fi

githubUrl="https://api.github.com/repos/rstudio/latest-builds/contents/content/$product/$flower/$build/$version_stem.md"
curlOutFname="curl.out"

payload="{\"message\":\"Add $flower build $version in $build\",\"content\":\"$base64_contents\"}"

echo "Sending to Github: $payload"
httpCode=$(curl \
   -X PUT \
   -o $curlOutFname \
   -H "Accept: application/vnd.github.v3+json" \
   -H "Authorization: token $pat" \
   $githubUrl \
   -d "$payload")
echo "Github's Response: "
echo "Http Code : $httpCode"
cat $curlOutFname

if [[ $httpCode -eq 422 ]]; then
   # An http code of 422 indicates a problem, probably that the file already exists and this is an
   # update not a create

   echo "Received a 422 http code, assuming this is actually an update not a create, so we get the existing file's info"
   getShaResponse=$(curl \
      -X GET \
      -H "Accept: application/vnd.github.v3+json" \
      -H "Authorization: token $pat" \
      $githubUrl)
   echo $getShaResponse

   fileSha=$(echo $getShaResponse | jq -r .sha)

   payload="{\"message\":\"Update $flower build $version in $build\",\"content\":\"$base64_contents\",\"sha\":\"$fileSha\"}"
   
   httpCode=$(curl \
      -X PUT \
      -o $curlOutFname \
      -H "Accept: application/vnd.github.v3+json" \
      -H "Authorization: token $pat" \
      $githubUrl \
      -d "$payload")

   echo "Github's Update Response:"
   echo "Http Code : $httpCode"
   cat $curlOutFname
fi

# Retry, sleeping for random 
if [[ $httpCode -eq 409 ]]; then

   retry_count=0
   while [[ $retry_count -lt 5 ]]; do
      echo "Received a 409 error, assuming it's a commit interleaving error"
      # Sleep for a random amount of time between 3-10 seconds: $(($RANDOM%($max-$min+1)+$min))
      # Randomness is in case we're trying to upload at the same time as another process
      sleep $((RANDOM % 6 + 3))
      echo "Retrying the command (retry count: $((retry_count+1)) of $retry_count)"

       httpCode=$(curl \
        -X PUT \
        -o $curlOutFname \
        -H "Accept: application/vnd.github.v3+json" \
        -H "Authorization: token $pat" \
        $githubUrl \
        -d "$payload")

      echo "Github's Response"
      echo "Http Code : $httpCode"
      cat $curlOutFname
      if [[ $httpCode -ne 409 ]]; then
         break
      fi
      retry_count=$((retry_count+1))
   done
fi

if [[ $(($httpCode)) -ge 400 ]]; then
   echo "An unrecoverable error has occured"
   exit 1
fi
