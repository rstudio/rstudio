#!/usr/bin/env bash
set -e

# minified js using closure
minify () {
    echo "Minifying $1 to $2"
    CC_OPTS="--warning_level QUIET"
    java -Xmx128M -jar "../../../../../../gwt/tools/compiler/compiler.jar" $CC_OPTS --js $1 --js_output_file $2
}

minify dataTables.scroller.js dataTables.scroller.min.js
minify jquery.dataTables.js jquery.dataTables.min.js


