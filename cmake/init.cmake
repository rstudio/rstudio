#
# init.cmake
#
# Copyright (C) 2024 by Posit Software, PBC
#
# Unless you have received this program directly from Posit Software pursuant
# to the terms of a commercial license agreement with Posit Software, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#

# Store the project root directory.
get_filename_component(RSTUDIO_PROJECT_ROOT ${CMAKE_CURRENT_LIST_DIR} DIRECTORY)
set(RSTUDIO_PROJECT_ROOT "${RSTUDIO_PROJECT_ROOT}" CACHE INTERNAL "")

# Set up the CMake module path.
set(CMAKE_MODULE_PATH "${RSTUDIO_PROJECT_ROOT}/cmake/modules/")

