#!/usr/bin/env bash

# RStudio Docker Compilation Helper
#
# The purpose of this script is to make it easy to compile under Docker on 
# a development machine, so it's easy to iterate when making platform-specific
# changes or configuration changes.
# 
# The syntax is as follows:
#
#     docker-compile.sh IMAGE-NAME FLAVOR-NAME [VERSION]
#
# where the image name is the platform and architecture, the flavor name is
# the kind of package you wish to build (desktop or server), and the version
# is the number to assign to the resulting build.
#
# For example:
# 
#     docker-compile.sh centos7-amd64 desktop 1.2.345
#
# will produce an RPM of RStudio Desktop from the 64bit CentOS 7 container, 
# with build version 1.2.345, in your package/linux/ directory.
#
# For convenience, this script will build the required image if it doesn't
# exist, but for efficiency, this script does not attempt rebuild the image if
# it's already built. If you're iterating on the Dockerfiles themselves, you'll
# want to use "docker build" directly until you're happy with the
# configuration, then use this script to test a build under the new config.
#
# To produce a debug build, set the environment variable CMAKE_BUILD_TYPE to
# "Debug" before invoking the script.

# friendly names for arguments
IMAGE=$1
FLAVOR=$2
VERSION=$3

# abort on error
set -e

# set destination folder
PKG_DIR=$(pwd)/package
mkdir -p "$PKG_DIR"

# move to the repo root (script's grandparent directory)
cd "$(dirname ${BASH_SOURCE[0]})/.."
REPO=$(basename $(pwd))

if [ "${IMAGE:0:7}" = "windows" ]; then
    echo -e "Use win-docker-compile.cmd in a Windows Command Prompt to build for Windows."
    exit 1
fi

# print usage if no argument supplied
if [ -z "$IMAGE" ] || [ -z "$FLAVOR" ]; then
    echo -e "Compiles RStudio inside a Docker container."
    echo -e "Usage: docker-compile.sh image-name flavor-name [version] [variant]\n"
    echo -e "Valid images:\n"
    ls -f docker/jenkins/Dockerfile.* | sed -e 's/.*Dockerfile.//'
    echo -e "\nValid flavors:\n"
    echo -e "desktop"
    echo -e "server"
    exit 1
fi

# check to see if there's already a built image
IMAGEID=`docker images $REPO:$IMAGE --format "{{.ID}}"`
if [ -z "$IMAGEID" ]; then
    echo "No image found for $REPO:$IMAGE."
else
    echo "Found image $IMAGEID for $REPO:$IMAGE."
fi

# get build arg env vars, if any
if [ ! -z "${DOCKER_GITHUB_LOGIN}" ]; then
   BUILD_ARGS="--build-arg GITHUB_LOGIN=${DOCKER_GITHUB_LOGIN}"
fi

# rebuild the image if necessary
docker build --tag "$REPO:$IMAGE" --file "docker/jenkins/Dockerfile.$IMAGE" $BUILD_ARGS .

# infer the package extension from the image name
if [ "${IMAGE:0:6}" = "centos" ]; then
    PACKAGE=RPM
    INSTALLER=yum
elif [ "${IMAGE:0:6}" = "fedora" ]; then
    PACKAGE=RPM
    INSTALLER=yum
elif [ "${IMAGE:0:8}" = "opensuse" ]; then
    PACKAGE=RPM
    INSTALLER=zypper
else
    PACKAGE=DEB
    INSTALLER=debian
fi

if [ -n "$VERSION" ]; then 
    SPLIT=(${VERSION//\./ })
    PATCH="${SPLIT[2]}"
    # determine major and minor versions
    ENV="RSTUDIO_VERSION_MAJOR=${SPLIT[0]} RSTUDIO_VERSION_MINOR=${SPLIT[1]}"

    # supply suffix if embedded in patch
    if [[ $PATCH == *"-"* ]]; then
        PATCH=(${PATCH//-/ })
        ENV="$ENV RSTUDIO_VERSION_PATCH=${PATCH[0]} RSTUDIO_VERSION_SUFFIX=${PATCH[1]}"
    else
        ENV="$ENV RSTUDIO_VERSION_PATCH=$PATCH"
    fi
fi

# set up build flags
ENV="$ENV GIT_COMMIT=$(git rev-parse HEAD)"
ENV="$ENV BUILD_ID=local"

# infer make parallelism
if hash sysctl 2>/dev/null; then
    # macos; we could use `sysctl -n hw.ncpu` but that would likely be too
    # high. Docker for Mac defaults to half that value. Instead, use -j2 
    # to match what we currently do in the official build.
    ENV="$ENV MAKEFLAGS=-j2"
elif hash nproc 2>/dev/null; then
    # linux
    ENV="$ENV MAKEFLAGS=-j$(nproc --all)"
fi

# forward build type if set
if [ ! -z "$CMAKE_BUILD_TYPE" ]; then
    ENV="$ENV CMAKE_BUILD_TYPE=$CMAKE_BUILD_TYPE"
fi

# adjust folder path when building debug
if [ "$CMAKE_BUILD_TYPE" = "Debug" ]; then
    FLAVOR_SUFFIX="-Debug"
fi

# remove previous image if it exists
CONTAINER_ID="build-$REPO-$IMAGE"
echo "Cleaning up container $CONTAINER_ID if it exists..."
docker rm "$CONTAINER_ID" || true

# run compile step
docker run --name "$CONTAINER_ID" -v "$(pwd):/src" "$REPO:$IMAGE" bash -c "mkdir /package && cd /package && $ENV /src/package/linux/make-package ${FLAVOR^} $PACKAGE clean && echo build-${FLAVOR^}-$PACKAGE/*.${PACKAGE,,} && ls build-${FLAVOR^}-$PACKAGE$FLAVOR_SUFFIX/*.${PACKAGE,,}"

# extract logs to get filename (should be on the last line)
PKG_FILENAME=$(docker logs --tail 1 "$CONTAINER_ID")

if [ "${PKG_FILENAME:0:6}" = "build-" ]; then
  docker cp "$CONTAINER_ID:/package/$PKG_FILENAME" "$PKG_DIR"
  echo "Packages produced"
  echo "-----------------"
  echo $PKG_FILENAME
else
  echo "No package found."
fi

# stop the container
docker stop "$CONTAINER_ID"
echo "Container image saved in $CONTAINER_ID."
