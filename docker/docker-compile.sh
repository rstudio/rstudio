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
# the kind of package you wish to build (desktop, electron or server), and the
# version is the number to assign to the resulting build.
#
# For example:
# 
#     docker-compile.sh rhel8 desktop 1.2.345
#
# will produce an RPM of RStudio Desktop from the RHEL8 container, 
# with build version 1.2.345, in your package/linux/ directory.
#
# For convenience, this script will build the required image if it doesn't
# exist, but for efficiency, this script does not attempt rebuild the image if
# it's already built. If you're iterating on the Dockerfiles themselves, you'll
# want to use "docker build" directly until you're happy with the
# configuration, then use this script to test a build under the new config.
#
# By default, the script will build packages that match the architecture of the
# host machine -- i.e. you will get AMD64 builds or ARM64 builds depending on 
# your processor's architecture. To force a mismatch, set the environment 
# variable CONTAINER_ARCH to "amd64" or "arm64".
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
cd "$(dirname "${BASH_SOURCE[0]}")/.."
REPO=$(basename "$(pwd)")

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
    echo -e "electron"
    echo -e "server"
    exit 1
fi

# set container architecture from system architecture (if unspecified)
SYSTEM_ARCH=$(uname -m)
if [ -z "$CONTAINER_ARCH" ]; then
   if [ "$SYSTEM_ARCH" = "aarch64" ] || [ "$SYSTEM_ARCH" = "arm64" ]; then
      CONTAINER_ARCH="arm64"
   else
      CONTAINER_ARCH="amd64"
   fi
else
   echo "Building package for ${CONTAINER_ARCH} (system architecture is ${SYSTEM_ARCH})"
fi

# form image tag from container architecture
IMAGE_TAG="${IMAGE}-${CONTAINER_ARCH}"

# check to see if there's already a built image
IMAGEID=$(docker images "$REPO:$IMAGE_TAG" --format "{{.ID}}")
if [ -z "$IMAGEID" ]; then
    echo "No image found for $REPO:$IMAGE_TAG."
else
    echo "Found image $IMAGEID for $REPO:$IMAGE_TAG."
fi

# add architecture to build arguments
BUILD_ARGS="--build-arg ARCH=${CONTAINER_ARCH}"

# get build arg env vars, if any
if [ ! -z "${DOCKER_GITHUB_LOGIN}" ]; then
   BUILD_ARGS="${BUILD_ARGS} GITHUB_LOGIN=${DOCKER_GITHUB_LOGIN}"
fi

# rebuild the image if necessary
docker build                                \
  --tag "$REPO:$IMAGE_TAG"                  \
  --file "docker/jenkins/Dockerfile.$IMAGE" \
  $BUILD_ARGS                               \
  .

# infer the package extension from the image name
if [ "${IMAGE:0:6}" = "centos" ]; then
    PACKAGE=RPM
    INSTALLER=yum
elif [ "${IMAGE:0:6}" = "fedora" ]; then
    PACKAGE=RPM
    INSTALLER=yum
elif [ "${IMAGE:0:4}" = "rhel" ]; then
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
    SPLIT=(${VERSION//[-+]/ })
    FIRST_HALF=(${SPLIT[0]//\./ })

    # determine major and minor versions
    ENV="RSTUDIO_VERSION_MAJOR=${FIRST_HALF[0]} RSTUDIO_VERSION_MINOR=${FIRST_HALF[1]} RSTUDIO_VERSION_PATCH=${FIRST_HALF[2]}"

    # supply suffix
    if [[ ${#SPLIT[@]} -gt 2 ]]; then
        ENV="$ENV RSTUDIO_VERSION_SUFFIX=-${SPLIT[1]}+${SPLIT[2]}"
    else
        ENV="$ENV RSTUDIO_VERSION_SUFFIX=+${SPLIT[1]}"
    fi
fi

# set up build flags
ENV="$ENV GIT_COMMIT=$(git rev-parse HEAD)"
ENV="$ENV BUILD_ID=local"

# infer make parallelism
if [ "$(uname)" = "Darwin" ]; then
    # macos; Docker for Mac defaults to half of host's cores
    JVALUE=$(( `sysctl -n hw.ncpu` / 2 ))
    ENV="$ENV MAKEFLAGS=-j${JVALUE}"
elif hash nproc 2>/dev/null; then
    # linux
    ENV="$ENV MAKEFLAGS=-j$(nproc --all)"
fi

# forward build type if set
if [ -n "$CMAKE_BUILD_TYPE" ]; then
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

# form build command ---

# create and enter package build directory
CMD="mkdir /package &&"
CMD="${CMD} cd /package &&"

# perform the actual compile step w/ clean
CMD="${CMD} $ENV /src/package/linux/make-package ${FLAVOR^} ${PACKAGE} clean &&"

# output the name of the built package (will be captured later)
CMD="${CMD} echo build-${FLAVOR^}-${PACKAGE}/*.${PACKAGE,,} &&"
CMD="${CMD} ls build-${FLAVOR^}-${PACKAGE}${FLAVOR_SUFFIX}/*.${PACKAGE,,}"

echo "Running build command:"
echo "${CMD}"

# run compile step!
docker run                 \
  --name "$CONTAINER_ID"   \
  --volume "$(pwd):/src"   \
  "$REPO:$IMAGE" bash -c "${CMD}"

# extract logs to get filename (should be on the last line)
PKG_FILENAME=$(docker logs --tail 1 "$CONTAINER_ID")

if [ "${PKG_FILENAME:0:6}" = "build-" ]; then
  docker cp "$CONTAINER_ID:/package/$PKG_FILENAME" "$PKG_DIR"
  echo "Packages produced"
  echo "-----------------"
  echo "$PKG_FILENAME"
else
  echo "No package found."
fi

# stop the container
docker stop "$CONTAINER_ID"
echo "Container image saved in $CONTAINER_ID."

