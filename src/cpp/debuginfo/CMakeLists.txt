#
# CMakeLists.txt
#
# Copyright (C) 2025 by Posit Software, PBC
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

configure_file(
   "${CMAKE_CURRENT_SOURCE_DIR}/debuginfo"
   "${CMAKE_CURRENT_BINARY_DIR}/debuginfo"
   @ONLY
)

install(
   PROGRAMS "${CMAKE_CURRENT_BINARY_DIR}/debuginfo"
   DESTINATION "${RSTUDIO_INSTALL_BIN}"
)