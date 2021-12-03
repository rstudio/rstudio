#!/bin/bash
# This should be run after the dmg is created, and after create-full-package-file-name.js also runs from inside forge.config.js

OUT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)/../out"

FILENAME_EXTENSION="filename"
FILENAME=$(find $OUT_DIR -type f -iname "*.$FILENAME_EXTENSION" -execdir sh -c 'printf "%s\n" "${0%.*}"' {} ';')

RSTUDIO_APP_PATH=$(find $OUT_DIR/make -type f -iname "*.dmg")

mv $RSTUDIO_APP_PATH $OUT_DIR/make/$FILENAME.dmg

rm -rf "$OUT_DIR/$FILENAME.$FILENAME_EXTENSION"
