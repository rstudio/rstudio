#!/usr/bin/env bash

set -e

# read canonical list of flavors from Jenkinsfile
SOURCEDIR=$(dirname "${BASH_SOURCE[0]}")
CONTAINERS=$(cat $SOURCEDIR/../Jenkinsfile | grep "\[os: ")
HOSTLIST=$(echo "$CONTAINERS" | cut -d \' -f 2 | tr '\n' ' ')
PLATFORMLIST=$(echo "$CONTAINERS" | cut -d \' -f 4 | tr '\n' ' ')
FLAVORLIST=$(echo "$CONTAINERS" | cut -d \' -f 6 | tr '\n' ' ')

# convert to formal arrays
IFS=' ' read -a HOSTS <<< "$HOSTLIST"
IFS=' ' read -a PLATFORMS <<< "$PLATFORMLIST"
IFS=' ' read -a FLAVORS <<< "$FLAVORLIST"

# iterate through array and perform a docker compile
for ((i = 0; i < ${#HOSTS[@]}; i++))
do
    echo "=== Building platform $((i+1))/${#HOSTS[@]}: ${HOSTS[$i]} ${FLAVORS[$i]} ==="
    "$SOURCEDIR/docker-compile.sh" "${HOSTS[$i]}-${PLATFORMS[$i]}" "${FLAVORS[$i]}"
done
