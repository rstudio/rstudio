#!/usr/bin/env bash

# Run this script to add node to the PATH.
if [ "$(uname -ms)" = "Darwin arm64" ]; then
	NODE_PATH=$(realpath ../../../dependencies/common/node/16.14.0-arm64/bin)
else
	NODE_PATH=$(realpath ../../../dependencies/common/node/16.14.0/bin)
fi

PATH="${NODE_PATH}:${PATH}"
export PATH

