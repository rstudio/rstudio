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
channel="$(cat "$RSTUDIO_ROOT_DIR/version/BUILDTYPE" | tr '[ ]' '-' | tr -d '[:space:]')"
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

githubUrl="https://api.github.com/repos/rstudio/test-release-repo/contents/content/rstudio/$flower/$build/$version_stem.md"
curl_out_fname="curl.out"

# Sha is only required in the payload if it's an update, removing the sha field gives a clearer
# error message
payload="{\"message\":\"Add $flower build $version in $build\",\"content\":\"$base64_contents\"}"
echo "Sending to Github: $payload"

command="curl \
   -X PUT \
   -w %{http_code}% \
   -o $curl_out_fname \
   -H \"Accept: application/vnd.github.v3+json\" \
   -H \"Authorization: token $pat\" \
   $githubUrl \
   -d \"$payload\""


http_code=$($command)

echo "Github's Response: "
echo "Http Code : $http_code"
cat $curl_out_fname

# We get a null from jq if the message field doesn't exist. This field appears when there's been a problem with
# our upload. We'll assume it's because the sha field is missing and this is an update, however we've
# printed the response in the event there's some other issue
if [[ $http_code -eq 422 ]]; then

   echo "We need up perform an update, so we get the existing file's info"
   sha_http_code=$(curl \
      -X GET \
      -H "Accept: application/vnd.github.v3+json" \
      -H "Authorization: token $pat" \
      $githubUrl \
      -d "$payload")
   echo $getShaResponse

   updateSha=$(echo $getShaResponse | jq -r .sha)

   updatePayload="{\"message\":\"Update $flower build $version in $build\",\"content\":\"$base64_contents\",\"sha\":\"$updateSha\"}"
   
   updateResponse=$(curl \
      -X PUT \
      -H "Accept: application/vnd.github.v3+json" \
      -H "Authorization: token $pat" \
      $githubUrl \
      -d "$updatePayload")

   echo "Github's Update Response:"
   echo $updateResponse

   updateErrorMessage=$(echo $updateResponse | jq -r .message)

   # If there's still an issue, give up, return non zero exit code
   if [[ $updateMessage != "null" ]]; then
      exit 1;
   fi
elif [[$http_code -eq 409]]; then
    echo "Received a 409 error, assuming it's a commit interleaving error, we'll back off for 3 seconds and retry".
    sleep 3

    #retry the command
    echo "Retrying the command"
    retry_http_code=$($command)
    cat $curl_out_fname
fi
