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

echo "[i] Re-signing RStudio binary with entitlements -- ${entype}"
codesign-binary --entitlements "entitlements/rstudio-${entype}.plist" "${package}/Contents/MacOS/RStudio"

echo "[i] Validating signatures"
codesign -vvv --deep --strict "${package}"

