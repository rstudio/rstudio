#!/usr/bin/env bash

#
# rstudio-tools.sh -- Bash toolkit used in dependency scripts
#
# Copyright (C) 2022 by Posit Software, PBC
#
# Unless you have received this program directly from Posit pursuant
# to the terms of a commercial license agreement with Posit, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#

# Generic Tools ----

section () {
	echo -e "\033[1m\033[36m==>\033[39m $*\033[0m"
}

info () {
	echo -e "\033[1m[I]\033[0m $*"
}

warn () {
	echo -e "\033[1m\033[31m[W]\033[0m $*"
}

error () {

	echo -e "\033[1m\033[31m[E]\033[0m $*"

	if [ "${BASH_SUBSHELL}" -eq 0 ]; then
		return 1
	else
		exit 1
	fi

}

set-default () {

	if [ "$#" = "0" ]; then
		cat <<- EOF
		Usage: set-default var val

		Set the value of a variable if it is unset.
		EOF
		return 0
	fi

	local VAR="$1"
	local VAL="$2"

	if [ -z "${!VAR}" ]; then
		eval "$VAR=$VAL"
	fi

}

rerun-as-root () {
	exec sudo -E /bin/bash "$0" "$@"
}

sudo-if-necessary-for () {

	# If the directory does not exist, try to create it
	if ! [ -e "$1" ]; then
		if mkdir -p "$1" &> /dev/null; then
			return 0
		fi
	fi

	# Otherwise, check if the directory is writable
	if ! [ -w "$1" ]; then
		echo "Execution of '$0' requires root privileges for access to '$1'"
		shift
		rerun-as-root "$@"
	fi

}

mkdir-sudo-if-necessary () {

	# If the directory does not exist, try to create it without sudo
	if ! [ -e "$1" ]; then
		mkdir -p "$1" &> /dev/null || true
	fi

	# Still not there, create with sudo
	if ! [ -e "$1" ]; then
		sudo -E mkdir -p "$1"
	fi

}

find-file () {

	if [ "$#" -lt 2 ] || [ "$1" = "--help" ]; then
		echo "Usage: find-file <var> [paths]"
		return 0
	fi

	local VAR="$1"
	shift

	if [ -n "${!VAR}" ]; then
		info "Found ${VAR}: ${!VAR} [cached]"
		return 0
	fi

	local FILE
	for FILE in "$@"; do
		if [ -e "${FILE}" ]; then
			info "Found ${VAR}: ${FILE}"
			echo "${VAR}=${FILE}"
			eval "${VAR}=${FILE}"
			return 0
		fi
	done

	error "could not find file"

}

# Aliases over 'pushd' and 'popd' just to suppress printing of
# the directory stack on stdout
pushd () {
	command pushd "$@" > /dev/null
}

popd () {
	command popd "$@" > /dev/null
}

# Detect whether a command is available (e.g. program on the PATH,
# Bash builtin, or otherwise)
has-command () {
	command -v "$1" &> /dev/null
}

has-program () {
	command -v "$1" &> /dev/null
}

find-program () {

	if [ "$#" -lt 2 ] || [ "$1" = "--help" ]; then
		echo "Usage: find-program <var> <program> [paths...]"
		return 0
	fi

	# read variable
	local VAR="$1"
	shift

	# if the variable is already set, bail
	if [ -n "${!VAR}" ]; then
		info "Found ${VAR}: ${!VAR} [cached]"
		return 0
	fi

	# read program name
	local PROGRAM="$1"
	shift

	# search for program in specified paths
	for DIR in "$@"; do
		local VAL="${DIR}/${PROGRAM}"
		if [ -f "${VAL}" ]; then
			info "Found ${VAR}: ${VAL}"
			eval "${VAR}=${VAL}"
			return 0
		fi
	done

	# if we couldn't find it, look for copy on the PATH
	local CANDIDATE=$(which "${PROGRAM}")
	if [ -n "${CANDIDATE}" ]; then
		info "Found ${PROGRAM}: ${CANDIDATE}"
		eval "${VAR}=${CANDIDATE}"
		return 0
	fi

	# failed to find program
	error "could not find program '${PROGRAM}'"

}

require-program () {
	if ! has-program "$1"; then
		error "required program '$1' is not available on the PATH"
	fi
}

is-verbose () {
	[ -n "${VERBOSE}" ] && [ "${VERBOSE}" != "0" ]
}

is-m1-mac () {
	[ "$(arch)" = "arm64" ] && [ "$(uname)" = "Darwin" ]
}

# Download a single file
download () {

	if [ "$#" -eq 0 ]; then
		echo "usage: download src [dst]"
		return 1
	fi

	# Compute source path
	local SRC="$1"

	# Compute destination path
	local DST
	if [ "$#" -eq 1 ]; then
		DST="$(basename "$SRC")"
	else
		DST="$2"
	fi

	if is-verbose; then
		echo -e "Downloading:\n- '$SRC' -> '$DST'"
	fi

	# Invoke downloader
	if has-command curl; then
		curl -L -f -C - "$SRC" > "$DST"
	elif has-command wget; then
		wget -c "$SRC" -O "$DST"
	else
		echo "no downloader detected on this system (requires 'curl' or 'wget')"
		return 1
	fi

}

# Extract an archive
extract () {
	local FILE="$1"

	if is-verbose; then
		echo "Extracting '$FILE' ..."
	fi

	case "${FILE}" in

		*.deb)
			dpkg -x "${FILE}"
		;;

		*.rpm)
			rpm2cpio "${FILE}" | cpio -idmv
		;;

		*.tar.gz)
			tar -xf "${FILE}"
		;;

		*.tar.xz)
			tar -xf "${FILE}"
		;;

		*.gz)
			gunzip "${FILE}"
		;;

		*.zip)
			unzip -o "${FILE}"
		;;

		*)
			echo "Don't know how to extract file '${FILE}'"
			return 1
		;;

	esac
}

# Platform Detection ----

platform () {

	# Detect macOS variants
	if [ "$(uname)" = "Darwin" ]; then
		echo "darwin"
		return 0
	fi

	# Detect platform ID using /etc/os-release when available
	if [ -f /etc/os-release ]; then
		local ID="$(. /etc/os-release; echo "$ID")"
		if [ -n "${ID}" ]; then
			echo "${ID}"
			return 0
		fi
	fi

	# Detect platform using /etc/redhat-release when available
	if [ -f /etc/redhat-release ]; then

		# Detect CentOS
		if grep -siq "centos" /etc/redhat-release; then
			echo "centos"
			return 0
		fi

		# Detect Fedora
		if grep -siq "fedora" /etc/redhat-release; then
			echo "fedora"
			return 0
		fi

		# Detect Rocky Linux (used for RHEL8)
		if grep -siq "rocky" /etc/redhat-release; then
			echo "rocky"
			return 0
		fi

		# Warn about other RedHat flavors we don't yet recognize
		echo "unrecognized redhat variant '$(cat /etc/redhat-release)'"
		return 1
	fi

	echo "unrecognized platform detected"
	return 1
}

ubuntu-codename () {
	# try reading codename from /etc/os-release
	local CODENAME="$(. /etc/os-release; echo "${UBUNTU_CODENAME}")"
	if [ -n "${CODENAME}" ]; then
		echo "${CODENAME}"
		return 0
	fi

	# hard-coded values for older Ubuntu
	case "$(os-version)" in

	12.04) echo "precise" ;;
	12.10) echo "quantal" ;;
	13.04) echo "raring"  ;;
	13.10) echo "saucy"   ;;
	14.04) echo "trusty"  ;;
	14.10) echo "utopic"  ;;
	15.04) echo "vivid"   ;;
	15.10) echo "wily"    ;;
	16.04) echo "xenial"  ;;
	*)     echo "unknown"; return 1 ;;

	esac

	return 0
}

# Get the operating system version (as a number)
os-version () {

	if [ -f /etc/os-release ]; then
		local VERSION_ID="$(. /etc/os-release; echo "${VERSION_ID}")"
		if [ -n "${VERSION_ID}" ]; then
			echo "${VERSION_ID}"
			return 0
		fi
	fi

	if [ -f /etc/redhat-release ]; then
		grep -oE "[0-9]+([_.-][0-9]+)*" /etc/redhat-release
		return 0
	fi

	if has-command sw_vers; then
		sw_vers -productVersion
		return 0
	fi

	echo "don't know how to infer OS version for platform '$(platform)'"
	return 1

}

os-version-part () {

	local VERSION="$(os-version | cut -d"." -f"$1")"

	if [ -n "${VERSION}" ]; then
		echo "${VERSION}"
		return 0
	fi

	echo "0"
	return 0

}

os-version-major () {
	os-version-part 1
}

os-version-minor () {
	os-version-part 2
}

os-version-patch () {
	os-version-part 3
}

# Helper functions for quickly checking platform types
is-mac () {
	[ "$(uname)" = "Darwin" ]
}

is-macos () {
	[ "$(uname)" = "Darwin" ]
}

is-darwin () {
	[ "$(uname)" = "Darwin" ]
}

is-linux () {
	[ "$(uname)" = "Linux" ]
}

is-redhat () {
	[ -f /etc/redhat-release ]
}

is-centos () {
	[ "$(platform)" = "centos" ]
}

is-rhel () {
	[ "$(platform)" = "rocky" ]
}

is-fedora () {
	[ "$(platform)" = "fedora" ]
}

is-opensuse () {
	[ "$(platform)" = "opensuse" ]
}

is-ubuntu () {
	[ "$(platform)" = "ubuntu" ]
}

is-jenkins () {
	[ -n "${JENKINS_URL}" ]
}

# pick a default RSTUDIO_TOOLS_ROOT location
#
# prefer using home folder on Jenkins where we might not be able
# to access files at /opt and will lack sudo
if [ -z "${RSTUDIO_TOOLS_ROOT}" ]; then
	if is-jenkins && [ "$(arch)" = "arm64" ]; then
		RSTUDIO_TOOLS_ROOT="$HOME/opt/rstudio-tools/$(uname -m)"
	else
		RSTUDIO_TOOLS_ROOT="/opt/rstudio-tools/$(uname -m)"
	fi
fi

export RSTUDIO_TOOLS_ROOT

# version of node.js used for building
export RSTUDIO_NODE_VERSION="16.14.0"

# create a copy of a file in the same folder with .original extension
save-original-file () {

	local ORIGINAL_FILE="$1"
	local SAVED_FILE="$1.original"
	cp $ORIGINAL_FILE $SAVED_FILE
}

# restore a file previously saved with save-original-file
restore-original-file () {

	local ORIGINAL_FILE="$1"
	local SAVED_FILE="$1.original"
	local MODIFIED_FILE="$1.modified"

	rm -f $MODIFIED_FILE
	mv $ORIGINAL_FILE $MODIFIED_FILE
	mv $SAVED_FILE $ORIGINAL_FILE
	rm -f $MODIFIED_FILE
}