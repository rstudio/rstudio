#!/usr/bin/env bash


# DYLIB_PATHS=$(find $2 -type f -iname "*.dylib" -exec sh -c 'printf "\"%s\" " "${0// /\\ }"' {} ';')
DYLIB_PATHS=$(find $2 -type f -iname "*.dylib" -execdir sh -c 'printf "%s:" "$0"' {} ';')
# DYLIB_PATHS=$(find $2 -type f -iname "*.dylib" -exec sh -c 'printf "\"%s\":" "$0"' {} ';')

# array=()
# while IFS=  read -r -d $'\0'; do
#     array+=("$REPLY")
# done < <(find $2 -type f -iname "*.dylib" -print0)


# echo "${array[@]}"

# echo "$1 $2 $3 $DYLIB_PATHS"
sh $1 $2 $3 "$DYLIB_PATHS"

# sh $1 $2 $3 "${array[@]}"