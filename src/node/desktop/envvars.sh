#!/usr/bin/env bash

# Run this script to add node to the PATH.
# Node versions below should match RSTUDIO_NODE_VERSION
if [ "$(uname -ms)" = "Darwin arm64" ]; then
	NODE_PATH=$(readlink -fn ../../../dependencies/common/node/20.15.1-arm64/bin)
else
	NODE_PATH=$(realpath ../../../dependencies/common/node/20.15.1/bin)
fi

PATH="${NODE_PATH}:${PATH}"
export PATH

