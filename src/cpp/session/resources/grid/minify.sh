#!/usr/bin/env bash

set -e

COMPILER_JAR=$(realpath "../../../../gwt/tools/compiler/compiler.jar")

# minified js using closure
minify () {
    echo "Minifying $1 to $2"
    java -Xmx128M -jar "${COMPILER_JAR}" --warning_level QUIET --js "$1" --js_output_file "$2"
}

cd datatables/js
minify dataTables.scroller.js dataTables.scroller.min.js
minify jquery.dataTables.js jquery.dataTables.min.js
cd ../..


