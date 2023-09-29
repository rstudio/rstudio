#
# OsRelease.cmake
#
# Copyright (C) 2022 by Posit Software, PBC
#
# This program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

# reads and parses /etc/os-release into CMake variables
file(STRINGS "/etc/os-release" OS_RELEASE)
foreach(LINE ${OS_RELEASE})
	string(FIND "${LINE}" "=" INDEX)
	string(SUBSTRING "${LINE}" 0 "${INDEX}" KEY)
	math(EXPR INDEX "${INDEX} + 1")
	string(SUBSTRING "${LINE}" "${INDEX}" -1 VALUE)
	separate_arguments(VALUE UNIX_COMMAND "${VALUE}")
	set("OS_RELEASE_${KEY}" "${VALUE}" CACHE INTERNAL "/etc/os-release: ${KEY}")
endforeach()

