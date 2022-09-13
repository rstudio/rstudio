#
# CMakeLists.txt
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
#

if(POLICY CMP0077)
   cmake_policy(SET CMP0077 NEW)
endif()

# define package suffix
set(RSTUDIO_PACKAGE_SUFFIX "-")

# include overlay if it exists
if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/CMakeOverlay.txt")
   include(CMakeOverlay.txt)
endif()

# help install script find arm64 components
set(
   RSESSION_ARM64_PATH
   "${CMAKE_CURRENT_SOURCE_DIR}/build-arm64/src/cpp/session/rsession"
   CACHE INTERNAL "")

# developer-id code signing
if(RSTUDIO_PACKAGE_BUILD AND NOT RSESSION_ALTERNATE_BUILD)

   if(RSTUDIO_ELECTRON)
      configure_and_install("cmake/merge-electron.cmake")
   endif()

   configure_and_install("cmake/prepare-package.cmake")

   if(RSTUDIO_ELECTRON)
      configure_and_install("cmake/codesign-package-electron.cmake")
   else()
      configure_and_install("cmake/codesign-package.cmake")
   endif()

endif()

# package attributes
set(CPACK_PACKAGE_FILE_NAME "${CPACK_PACKAGE_NAME}${RSTUDIO_PACKAGE_SUFFIX}${CPACK_PACKAGE_VERSION}")
string(REPLACE "+" "-" CPACK_PACKAGE_FILE_NAME "${CPACK_PACKAGE_FILE_NAME}")

# create drag and drop installer
set(CPACK_BINARY_DRAGNDROP ON)

include(CPack)
