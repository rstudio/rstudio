#!/usr/bin/env bash

# Create .env file for docker-compose

# exit 1 if not 2 arguments
if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <OS-ARCH>"
    exit 1
fi

# Check if current directory is either rstudio or rstudio-pro and exit otherwise using basename
if [ "$(basename $(pwd))" != "rstudio" ] && [ "$(basename $(pwd))" != "rstudio-pro" ]; then
    echo "Error: This script must be run from either the rstudio or rstudio-pro directory"
    exit 1
fi

# Set OS-ARCH
OS_ARCH=$1
ARCH=$(echo $OS_ARCH | cut -d'-' -f2)

# Get the branch name from the release file make lowercase and replace spaces with -
BRANCH_NAME=$(cat version/RELEASE | tr '[:upper:]' '[:lower:]' | tr ' ' '-')

# Get username and set USERNAME variable
USERNAME=$(whoami)

# Set IMAGE_PREFIX if the repo is rstudio-pro using git
if git remote -v | grep -q rstudio-pro; then
    IMAGE_PREFIX="pro-"
else
    IMAGE_PREFIX=""
fi

# Set AWS_ARCH based on ARCH. x86_64 stays the same, arm64 becomes aarch64
if [ "$ARCH" == "x86_64" ]; then
    AWS_ARCH="x86_64"
elif [ "$ARCH" == "arm64" ]; then
    AWS_ARCH="aarch64"
fi

# Set NODE_PATH
source dependencies/tools/rstudio-tools.sh
RSTUDIO_NODE_PATH=/opt/rstudio-tools/dependencies/common/node/${RSTUDIO_NODE_VERSION}/bin

CONTAINER_PATH=dev-$OS_ARCH

# Set variables for docker-compose.yml
echo "BRANCH_NAME=$BRANCH_NAME" > .devcontainer/$CONTAINER_PATH/.env
echo "USERNAME=$USERNAME" >> .devcontainer/$CONTAINER_PATH/.env
echo "IMAGE_PREFIX=$IMAGE_PREFIX" >> .devcontainer/$CONTAINER_PATH/.env
echo "OS_ARCH=$OS_ARCH" >> .devcontainer/$CONTAINER_PATH/.env
echo "AWS_ARCH=$AWS_ARCH" >> .devcontainer/$CONTAINER_PATH/.env
echo "RSTUDIO_NODE_PATH=$RSTUDIO_NODE_PATH" >> .devcontainer/$CONTAINER_PATH/.env
