#
# FindLibR.cmake
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
# Exports:
#   LIBR_FOUND
#   LIBR_HOME
#   LIBR_EXECUTABLE
#   LIBR_BIN_DIR
#   LIBR_INCLUDE_DIRS
#   LIBR_DOC_DIR
#   LIBR_LIB_DIR
#   LIBR_LIBRARIES

# ===========================================================================
# 1. Find the R executable
# ===========================================================================

# If LIBR_HOME is set, use it to locate R. The final LIBR_HOME value
# is canonicalized via R.home().
if(DEFINED ENV{LIBR_HOME} AND NOT LIBR_HOME)
   set(LIBR_HOME "$ENV{LIBR_HOME}")
endif()

unset(_R_EXECUTABLE CACHE)

if(LIBR_HOME)

   # User provided LIBR_HOME -- find R inside it.
   if(WIN32)
      if(LIBR_FIND_WINDOWS_32BIT)
         set(_LIBR_BIN_HINTS "${LIBR_HOME}/bin/i386" "${LIBR_HOME}/bin")
      else()
         set(_LIBR_BIN_HINTS "${LIBR_HOME}/bin/x64" "${LIBR_HOME}/bin")
      endif()
      find_program(_R_EXECUTABLE NAMES R R.exe Rterm.exe
         HINTS ${_LIBR_BIN_HINTS}
         NO_DEFAULT_PATH)
   else()
      find_program(_R_EXECUTABLE NAMES R
         HINTS "${LIBR_HOME}/bin"
         NO_DEFAULT_PATH)
   endif()

else()

   # Try platform-specific discovery mechanisms first.
   if(APPLE)

      # Look for R framework
      find_library(_LIBR_FRAMEWORK R)
      if(_LIBR_FRAMEWORK MATCHES ".*\\.framework")
         set(_R_EXECUTABLE "${_LIBR_FRAMEWORK}/Resources/bin/R")
      elseif(_LIBR_FRAMEWORK)
         # Non-framework R (e.g., Homebrew) -- resolve symlink to find bin/R
         get_filename_component(_LIBR_FRAMEWORK_REAL "${_LIBR_FRAMEWORK}" REALPATH)
         get_filename_component(_LIBR_FRAMEWORK_DIR "${_LIBR_FRAMEWORK_REAL}" PATH)
         set(_R_EXECUTABLE "${_LIBR_FRAMEWORK_DIR}/../bin/R")
      endif()

   elseif(WIN32)

      # Check Windows registry for install path
      get_filename_component(_R_INSTALL_PATH
         "[HKEY_LOCAL_MACHINE\\SOFTWARE\\R-core\\R;InstallPath]" ABSOLUTE)

      if(NOT EXISTS "${_R_INSTALL_PATH}")
         message(STATUS "R path from registry '${_R_INSTALL_PATH}' doesn't exist, searching C:/R")
         file(GLOB _R_INSTALLATIONS "C:/R/*" LIST_DIRECTORIES TRUE)
         if(_R_INSTALLATIONS)
            list(GET _R_INSTALLATIONS 0 _R_INSTALL_PATH)
            message(STATUS "Found R installation at '${_R_INSTALL_PATH}'")
         else()
            set(_R_INSTALL_PATH "")
         endif()
      endif()

      if(_R_INSTALL_PATH)
         if(LIBR_FIND_WINDOWS_32BIT)
            set(_LIBR_BIN_HINTS "${_R_INSTALL_PATH}/bin/i386" "${_R_INSTALL_PATH}/bin")
         else()
            set(_LIBR_BIN_HINTS "${_R_INSTALL_PATH}/bin/x64" "${_R_INSTALL_PATH}/bin")
         endif()
         find_program(_R_EXECUTABLE NAMES R R.exe Rterm.exe
            HINTS ${_LIBR_BIN_HINTS}
            NO_DEFAULT_PATH)
      endif()

   endif()

   # Fallback for all platforms: find R on the PATH.
   if(NOT _R_EXECUTABLE OR NOT EXISTS "${_R_EXECUTABLE}")
      find_program(_R_EXECUTABLE NAMES R R.exe Rterm.exe)
   endif()

endif()

if(NOT _R_EXECUTABLE OR NOT EXISTS "${_R_EXECUTABLE}")
   message(FATAL_ERROR "Could not find R executable. Please set LIBR_HOME.")
endif()

# ===========================================================================
# 2. Query R for all paths (shared across all platforms)
# ===========================================================================

execute_process(
   COMMAND "${_R_EXECUTABLE}" "--vanilla" "-s" "-e" "cat(R.home())"
   OUTPUT_VARIABLE LIBR_HOME
   RESULT_VARIABLE _R_RESULT
)
if(NOT _R_RESULT EQUAL 0 OR NOT LIBR_HOME)
   message(FATAL_ERROR "Failed to query R.home() from '${_R_EXECUTABLE}'")
endif()
set(LIBR_HOME "${LIBR_HOME}" CACHE PATH "R home directory" FORCE)

set(LIBR_EXECUTABLE "${_R_EXECUTABLE}" CACHE PATH "R executable" FORCE)
get_filename_component(LIBR_BIN_DIR "${LIBR_EXECUTABLE}" PATH)
set(LIBR_BIN_DIR "${LIBR_BIN_DIR}" CACHE FILEPATH "R bin directory" FORCE)

execute_process(
   COMMAND "${LIBR_EXECUTABLE}" "--vanilla" "-s" "-e" "cat(R.home('include'))"
   OUTPUT_VARIABLE LIBR_INCLUDE_DIRS
   RESULT_VARIABLE _R_RESULT
)
if(NOT _R_RESULT EQUAL 0 OR NOT LIBR_INCLUDE_DIRS)
   message(FATAL_ERROR "Failed to query R.home('include') from '${LIBR_EXECUTABLE}'")
endif()
set(LIBR_INCLUDE_DIRS "${LIBR_INCLUDE_DIRS}" CACHE PATH "R include directory" FORCE)

execute_process(
   COMMAND "${LIBR_EXECUTABLE}" "--vanilla" "-s" "-e" "cat(R.home('doc'))"
   OUTPUT_VARIABLE LIBR_DOC_DIR
   RESULT_VARIABLE _R_RESULT
)
if(NOT _R_RESULT EQUAL 0 OR NOT LIBR_DOC_DIR)
   message(FATAL_ERROR "Failed to query R.home('doc') from '${LIBR_EXECUTABLE}'")
endif()
set(LIBR_DOC_DIR "${LIBR_DOC_DIR}" CACHE PATH "R doc directory" FORCE)

execute_process(
   COMMAND "${LIBR_EXECUTABLE}" "--vanilla" "-s" "-e" "cat(R.home('lib'))"
   OUTPUT_VARIABLE LIBR_LIB_DIR
   RESULT_VARIABLE _R_RESULT
)
if(NOT _R_RESULT EQUAL 0 OR NOT LIBR_LIB_DIR)
   message(FATAL_ERROR "Failed to query R.home('lib') from '${LIBR_EXECUTABLE}'")
endif()
set(LIBR_LIB_DIR "${LIBR_LIB_DIR}" CACHE PATH "R lib directory" FORCE)

# ===========================================================================
# 3. Find libraries
# ===========================================================================

set(_LIBR_LIBRARY_HINTS "${LIBR_LIB_DIR}")

if(WIN32)
   # Windows: determine architecture and add bin paths
   if(LIBR_FIND_WINDOWS_32BIT)
      set(LIBR_ARCH "i386")
   else()
      set(LIBR_ARCH "x64")
   endif()
   list(APPEND _LIBR_LIBRARY_HINTS
      "${LIBR_HOME}/bin/${LIBR_ARCH}"
      "${LIBR_HOME}/bin"
   )

   # Generate .lib files from DLLs
   execute_process(
      COMMAND "${LIBR_HOME}/bin/${LIBR_ARCH}/Rscript.exe" "dll2lib.R" "${CMAKE_C_COMPILER}"
      WORKING_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}/tools"
      OUTPUT_VARIABLE _DLL2LIB_STDOUT
      ERROR_VARIABLE _DLL2LIB_STDERR
      RESULT_VARIABLE _DLL2LIB_RESULT
   )
   if(NOT _DLL2LIB_RESULT EQUAL 0)
      message(STATUS "dll2lib.R output: ${_DLL2LIB_STDOUT}")
      message(STATUS "dll2lib.R error: ${_DLL2LIB_STDERR}")
      message(FATAL_ERROR "Failed to generate .lib files for R DLLs")
   endif()
endif()

# Find core R library
unset(LIBR_CORE_LIBRARY CACHE)
find_library(LIBR_CORE_LIBRARY NAMES R HINTS ${_LIBR_LIBRARY_HINTS})
if(LIBR_CORE_LIBRARY)
   set(LIBR_LIBRARIES ${LIBR_CORE_LIBRARY})
else()
   message(WARNING "Could not find libR shared library")
endif()

if(WIN32)
   # Find Windows-specific R libraries
   unset(LIBR_BLAS_LIBRARY CACHE)
   find_library(LIBR_BLAS_LIBRARY NAMES Rblas HINTS ${_LIBR_LIBRARY_HINTS})
   if(LIBR_BLAS_LIBRARY)
      list(APPEND LIBR_LIBRARIES ${LIBR_BLAS_LIBRARY})
   endif()

   unset(LIBR_LAPACK_LIBRARY CACHE)
   find_library(LIBR_LAPACK_LIBRARY NAMES Rlapack HINTS ${_LIBR_LIBRARY_HINTS})
   if(LIBR_LAPACK_LIBRARY)
      list(APPEND LIBR_LIBRARIES ${LIBR_LAPACK_LIBRARY})
   endif()

   unset(LIBR_GRAPHAPP_LIBRARY CACHE)
   find_library(LIBR_GRAPHAPP_LIBRARY NAMES Rgraphapp HINTS ${_LIBR_LIBRARY_HINTS})
   if(LIBR_GRAPHAPP_LIBRARY)
      list(APPEND LIBR_LIBRARIES ${LIBR_GRAPHAPP_LIBRARY})
   endif()
endif()

# Cache the final library list
if(LIBR_LIBRARIES)
   set(LIBR_LIBRARIES ${LIBR_LIBRARIES} CACHE PATH "R runtime libraries" FORCE)
endif()

# ===========================================================================
# 4. Validation
# ===========================================================================

include(FindPackageHandleStandardArgs)
find_package_handle_standard_args(LibR DEFAULT_MSG
   LIBR_HOME
   LIBR_EXECUTABLE
   LIBR_INCLUDE_DIRS
   LIBR_LIBRARIES
   LIBR_DOC_DIR
   LIBR_LIB_DIR
)

mark_as_advanced(
   LIBR_CORE_LIBRARY
   LIBR_BLAS_LIBRARY
   LIBR_LAPACK_LIBRARY
   LIBR_GRAPHAPP_LIBRARY
)
