#!/bin/sh

set -e

git pull
git submodule sync
git submodule update --init
cd src/gwt/tools/ace
git submodule sync
cd ../../../..
git submodule update --recursive

