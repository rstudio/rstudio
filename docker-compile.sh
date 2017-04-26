#!/usr/bin/env bash

# friendly names for arguments
IMAGE=$1
FLAVOR=$2
VERSION=$3

# abort on error
set -e

# print usage if no argument supplied
if [ -z "$IMAGE" ] || [ -z "$FLAVOR" ]; then
    echo -e "Compiles RStudio inside a Docker container."
    echo -e "Usage: docker-compile image-name flavor-name [version]\n"
    echo -e "Valid images:\n"
    ls -f docker/jenkins/Dockerfile.* | sed -e 's/.*Dockerfile.//'
    echo -e "\nValid flavors:\n"
    echo -e "desktop"
    echo -e "server"
    exit 1
fi

# make sure we're running from the directory of this script, and infer the 
# name of the repository
cd "$(dirname ${BASH_SOURCE[0]})"
REPO=$(basename $(pwd))

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
docker run -v $(pwd):/src --user jenkins $REPO:$IMAGE bash -c "cd /src/dependencies/linux && ./install-dependencies-$INSTALLER --exclude-qt-sdk && cd /src/package/linux && ./make-$FLAVOR-package $PACKAGE"

