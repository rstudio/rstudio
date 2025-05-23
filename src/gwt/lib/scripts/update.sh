#!/usr/bin/env bash

set -e

ELEMENTAL2_VERSION="1.2.3"
JSINTEROP_VERSION="1.0.3"

download () {
	echo -- Downloading $1
	curl -L -f -O "$1"
}

# Download elemental2.
mkdir -p elemental2
cd elemental2
download "https://repo1.maven.org/maven2/com/google/elemental2/elemental2-core/${ELEMENTAL2_VERSION}/elemental2-core-${ELEMENTAL2_VERSION}.jar"
download "https://repo1.maven.org/maven2/com/google/elemental2/elemental2-dom/${ELEMENTAL2_VERSION}/elemental2-dom-${ELEMENTAL2_VERSION}.jar"
download "https://repo1.maven.org/maven2/com/google/elemental2/elemental2-promise/${ELEMENTAL2_VERSION}/elemental2-promise-${ELEMENTAL2_VERSION}.jar"
cd ..

# Download jsinterop.
mkdir -p jsinterop
cd jsinterop
download "https://repo1.maven.org/maven2/com/google/jsinterop/base/${JSINTEROP_VERSION}/base-${JSINTEROP_VERSION}.jar"
cd ..

# Download jspecify.
#
# While all of the JARs above bundle both binary (.class) files and source (.java) files into the same archive,
# jspecify does not. GWT needs access to the .java sources for compilation, so we make sure to download both parts.
mkdir -p jspecify
cd jspecify
download "https://repo.maven.apache.org/maven2/org/jspecify/jspecify/1.0.0/jspecify-1.0.0.jar"
download "https://repo.maven.apache.org/maven2/org/jspecify/jspecify/1.0.0/jspecify-1.0.0-sources.jar"
cd ..


