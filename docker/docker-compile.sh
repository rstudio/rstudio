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

# friendly names for arguments
IMAGE=$1
FLAVOR=$2
VERSION=$3

# abort on error
set -e

# move to the repo root (script's grandparent directory)
cd "$(dirname ${BASH_SOURCE[0]})/.."
REPO=$(basename $(pwd))

# print usage if no argument supplied
if [ -z "$IMAGE" ] || [ -z "$FLAVOR" ]; then
    echo -e "Compiles RStudio inside a Docker container."
    echo -e "Usage: docker-compile.sh image-name flavor-name [version]\n"
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
    echo "No image found for $REPO:$IMAGE. Building..."
    docker build --tag "$REPO:$IMAGE" --file "docker/jenkins/Dockerfile.$IMAGE" .
else
    echo "Found image $IMAGEID for $REPO:$IMAGE"
fi

# infer the package extension from the image name
if [ "${IMAGE:0:6}" = "centos" ]; then
    PACKAGE=RPM
    INSTALLER=yum
else
    PACKAGE=DEB
    INSTALLER=debian
fi

if [ -n "$VERSION" ]; then 
    SPLIT=(${VERSION//\./ })
    ENV="RSTUDIO_VERSION_MAJOR=${SPLIT[0]} RSTUDIO_VERSION_MINOR=${SPLIT[1]} RSTUDIO_VERSION_PATCH=${SPLIT[2]}"
fi

# run compile step
docker run --rm -v $(pwd):/src $REPO:$IMAGE bash -c "cd /src/dependencies/linux && ./install-dependencies-$INSTALLER --exclude-qt-sdk && cd /src/package/linux && $ENV ./make-$FLAVOR-package $PACKAGE"

