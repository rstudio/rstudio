#!/usr/bin/env bash

# Run this script to add node to the PATH.
NODE_PATH=$(realpath ../../../dependencies/common/node/16.14.0/bin)
PATH="${NODE_PATH}:${PATH}"

