#!/usr/bin/env bash

# Creates a "_dev" locale, which is a copy of the _en locale with PREFIX prepended
# onto all strings for easy spotting in the UI
# 
# Running this script will result in a "XXX_dev.properties" file for each 
# "XXX_en.properties" file in this directory and its subdirectories

# Note: run this from /src/gwt/src, otherwise you'll create extra properties files
# from places that are generated code rather than input code

PREFIX="@"

for src in $(find . -name "*en.properties"); do
  tgt=$(echo "$src" | sed -e 's/_en.properties$/_dev.properties/')
  echo "Copying $src -> $tgt"
  if [ -f "$tgt" ]; then
  	echo "WARNING: Target $tgt already exists - skipping"
  else
    # "copy" src with sed, replacing all lines of "X = Y" with "X = {PREFIX}Y"
    if [[ "$OSTYPE" = "darwin"* ]]; then
      sed -E "s/^([^\x0D=]*)=([ ]*)([^\x0D]*)/\1=\2${PREFIX}\3/" $src > $tgt
    else
      sed -E "s/^([^\n=]*)=([ ]*)([^\n]*)\$/\1=\2${PREFIX}\3/" $src > $tgt
    fi
  fi
done
