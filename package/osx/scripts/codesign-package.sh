#!/usr/bin/env bash

#
# codesign-package.sh
#
# Copyright (C) 2022 by Posit Software, PBC
#
# Unless you have received this program directly from Posit Software pursuant
# to the terms of a commercial license agreement with Posit Software, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

set -e

if [ "$#" = "0" ] || [ "$1" = "--help" ]; then
	echo "Usage: codesign-package.sh [package] [codesign arguments...]"
	exit 0
fi

# read the package directory
package="$1"
shift

# recurse into hidden directories (that is, .webpack)
shopt -s nullglob
shopt -s dotglob

codesign_args=("$@")

retry () {

	local status=0

	for _ in 1 2 3; do

		if "$@"; then
			status=0
			break
		else
			status=$?
			sleep 1
		fi

	done

	return "${status}"

}

codesign-binary () {

	retry codesign "${codesign_args[@]}" "$@"

}

codesign-file () {

	retry codesign "${codesign_args[@]}" --entitlements entitlements/default.plist "$@"

}

codesign-directory () {

	# first, recurse into directories
	for FILE in "$1"/*; do
		if [ -d "${FILE}" ]; then
			codesign-directory "${FILE}"
		fi
	done

	# now, sign files
	for FILE in "$1"/*; do
		if [ -f "${FILE}" ]; then
			codesign-file "${FILE}"
		fi
	done

}

# remove Finder detritus that interferes with codesigning
echo "[i] Cleaning Finder metadata from ${package}"
find "${package}" -name '.DS_Store' -delete || true

echo "[i] Running codesign on package: ${package}"
codesign-directory "${package}"

entype="${RSESSION_ENTITLEMENTS_TYPE-adhoc}"
for executable in rsession rsession-arm64; do
	path="${package}/Contents/Resources/app/bin/${executable}"
	if [ -e "${path}" ]; then
		entitlements=entitlements/rsession-${entype}.plist
		echo "[i] Re-signing ${executable} with entitlements -- ${entype}"
		codesign-binary --entitlements "${entitlements}" "${path}"
	fi
done

# Must precede the Contents/MacOS/RStudio re-sign below: that re-seals the bundle,
# repairing the resource seal which re-signing a nested binary invalidates.
#
# bin/node is installed unconditionally (src/cpp/session/CMakeLists.txt), so sign it
# with no existence test -- a layout change should fail the build. A skipped re-sign
# would leave node validly signed with the wrong entitlements, and codesign --strict
# checks seals, not entitlement content, so nothing downstream would notice.
echo "[i] Re-signing node with entitlements -- ${entype}"
codesign-binary --entitlements "entitlements/node-${entype}.plist" \
	"${package}/Contents/Resources/app/bin/node/bin/node"

# bin/node-arm64 is copied only for universal builds (package/osx/cmake/prepare-package.cmake).
path="${package}/Contents/Resources/app/bin/node-arm64/bin/node"
if [ -e "${path}" ]; then
	echo "[i] Re-signing node-arm64 with entitlements -- ${entype}"
	codesign-binary --entitlements "entitlements/node-${entype}.plist" "${path}"
fi

echo "[i] Re-signing RStudio binary with entitlements -- ${entype}"
codesign-binary --entitlements "entitlements/rstudio-${entype}.plist" "${package}/Contents/MacOS/RStudio"

echo "[i] Validating signatures"
codesign -vvv --deep --strict "${package}"

