#!/usr/bin/env bash

# Run this script to add node to the PATH.
if [ "$(uname -ms)" = "Darwin arm64" ]; then
	NODE_PATH=$(readlink -fn ../../../dependencies/common/node/18.12.1-arm64/bin)
else
	NODE_PATH=$(realpath ../../../dependencies/common/node/18.12.1/bin)
fi

PATH="${NODE_PATH}:${PATH}"
export PATH

