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

# Set a default CMake build type.
if(NOT DEFINED CMAKE_BUILD_TYPE)
   set(CMAKE_BUILD_TYPE "Debug")
endif()

# Figure out if we're open source or not.
if(EXISTS "${RSTUDIO_PROJECT_ROOT}/upstream")
   set(RSTUDIO_PROJECT_TYPE Workbench CACHE INTERNAL "")
else()
   set(RSTUDIO_PROJECT_TYPE OpenSource CACHE INTERNAL "")
endif()

# If we're open source, then copy the relevant launch.json
# and tasks.json files as appropriate.
if(RSTUDIO_PROJECT_TYPE STREQUAL OpenSource)

   file(
      READ "${RSTUDIO_PROJECT_ROOT}/.vscode/launch.json"
      VSCODE_LAUNCH_JSON)

   string(FIND "${VSCODE_LAUNCH_JSON}"
      "// Please edit the configuration file in .vscode/open-source/launch.json."
      VSCODE_LAUNCH_JSON_OPEN_SOURCE)

   if(NOT VSCODE_LAUNCH_JSON_OPEN_SOURCE EQUAL -1)
      configure_file(
         "${RSTUDIO_PROJECT_ROOT}/.vscode/open-source/launch.json"
         "${RSTUDIO_PROJECT_ROOT}/.vscode/launch.json"
         @ONLY)

      configure_file(
         "${RSTUDIO_PROJECT_ROOT}/.vscode/open-source/tasks.json"
         "${RSTUDIO_PROJECT_ROOT}/.vscode/tasks.json"
         @ONLY)
      endif()

endif()