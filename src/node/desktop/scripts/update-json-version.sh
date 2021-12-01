#!/bin/bash

if [ "$1" = "" ]; then
    RED=$(tput setaf 1)
    echo "${RED}Error when trying to change Electron's version"
    echo "${RED}Please provide a version. Usage example:"
    echo "${RED}./update-json-version.sh 1.2.3"
else
    GREEN=$(tput setaf 2)

    SCRIPT_DIR="$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
    cd "${SCRIPT_DIR}/../";
    
    yarn json -I -f package.json -e "this.version=\"$1\""
    echo "${GREEN}Electron's Version successfully updated to $1"
fi
