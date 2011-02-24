#!/bin/sh

set -e

# We want this to be chatty if you use it at the command line
# but not if you call it from build automation. We could pass
# in an explicit argument if we need more flexibility in the
# future
if [ "$SHLVL" -eq 2 ]
then
    GIT_OPTS=
else
    GIT_OPTS=-q
fi

git pull $GIT_OPTS
git submodule $GIT_OPTS sync
git submodule $GIT_OPTS update --init
cd src/gwt/tools/ace
git submodule $GIT_OPTS sync
cd ../../../..
git submodule $GIT_OPTS update --recursive
