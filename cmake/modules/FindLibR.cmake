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
#   LIBR_INCLUDE_DIRS
#   LIBR_DOC_DIR
#   LIBR_LIBRARIES

# ===========================================================================
# 1. Check for user-provided LIBR_HOME or environment variable
# ===========================================================================

if(DEFINED ENV{LIBR_HOME} AND NOT LIBR_HOME)
   set(LIBR_HOME "$ENV{LIBR_HOME}")
endif()

# ===========================================================================
# 2. Platform-specific discovery of LIBR_HOME (if not already set)
# ===========================================================================

if(NOT LIBR_HOME)

   if(APPLE)

      # Look for R framework
      find_library(_LIBR_FRAMEWORK R)
      if(_LIBR_FRAMEWORK MATCHES ".*\\.framework")
         set(LIBR_HOME "${_LIBR_FRAMEWORK}/Resources")
      else()
         # Non-framework R (e.g., Homebrew)
         if(_LIBR_FRAMEWORK)
            get_filename_component(_LIBR_FRAMEWORK_REAL "${_LIBR_FRAMEWORK}" REALPATH)
            get_filename_component(_LIBR_FRAMEWORK_DIR "${_LIBR_FRAMEWORK_REAL}" PATH)
            set(_R_EXECUTABLE "${_LIBR_FRAMEWORK_DIR}/../bin/R")
            if(EXISTS "${_R_EXECUTABLE}")
               execute_process(
                  COMMAND "${_R_EXECUTABLE}" "--vanilla" "-s" "-e" "cat(R.home())"
                  OUTPUT_VARIABLE LIBR_HOME
               )
            endif()
         endif()
      endif()

   elseif(WIN32)

      # Check Windows registry
      get_filename_component(LIBR_HOME
         "[HKEY_LOCAL_MACHINE\\SOFTWARE\\R-core\\R;InstallPath]" ABSOLUTE)

      # Verify path exists
      if(NOT EXISTS "${LIBR_HOME}")
         message(STATUS "R path from registry '${LIBR_HOME}' doesn't exist, searching C:/R")
         file(GLOB _R_INSTALLATIONS "C:/R/*" LIST_DIRECTORIES TRUE)
         if(_R_INSTALLATIONS)
            list(GET _R_INSTALLATIONS 0 LIBR_HOME)
            message(STATUS "Found R installation at '${LIBR_HOME}'")
         else()
            set(LIBR_HOME "")
         endif()
      endif()

   else()

      # Unix: find R in PATH and query for home
      find_program(_R_EXECUTABLE R)
      if(_R_EXECUTABLE)
         execute_process(
            COMMAND "${_R_EXECUTABLE}" "--vanilla" "-s" "-e" "cat(R.home())"
            OUTPUT_VARIABLE LIBR_HOME
         )
      endif()

   endif()

endif()

# ===========================================================================
# 3. Find R executable and determine canonical LIBR_HOME
# ===========================================================================

# At this point LIBR_HOME might be:
#   - The actual R home (where etc/, library/, etc. live)
#   - An installation prefix (where bin/R lives, but R.home() returns something else)
#   - Not set (discovery failed)

if(NOT LIBR_HOME)
   message(FATAL_ERROR "Could not find R installation. Please set LIBR_HOME.")
endif()

# Find the R executable - try both ${LIBR_HOME}/bin/R and ${LIBR_HOME}/R
if(EXISTS "${LIBR_HOME}/bin/R")
   set(_R_EXECUTABLE "${LIBR_HOME}/bin/R")
elseif(EXISTS "${LIBR_HOME}/R")
   set(_R_EXECUTABLE "${LIBR_HOME}/R")
else()
   message(FATAL_ERROR "Could not find R executable in '${LIBR_HOME}'")
endif()

# Query R for the canonical home directory
execute_process(
   COMMAND "${_R_EXECUTABLE}" "--vanilla" "-s" "-e" "cat(R.home())"
   OUTPUT_VARIABLE _LIBR_HOME_CANONICAL
   RESULT_VARIABLE _R_HOME_RESULT
)
if(NOT _R_HOME_RESULT EQUAL 0 OR NOT _LIBR_HOME_CANONICAL)
   message(FATAL_ERROR "Failed to query R.home() from '${_R_EXECUTABLE}'")
endif()

# Use the canonical home
set(LIBR_HOME "${_LIBR_HOME_CANONICAL}" CACHE PATH "R home directory" FORCE)
message(STATUS "Found R: ${LIBR_HOME}")

# ===========================================================================
# 4. Query R for paths (shared across all platforms)
# ===========================================================================

set(LIBR_EXECUTABLE "${_R_EXECUTABLE}" CACHE PATH "R executable")
get_filename_component(LIBR_BIN_DIR "${LIBR_EXECUTABLE}" PATH CACHE)

# Query R for include directory
execute_process(
   COMMAND "${LIBR_EXECUTABLE}" "--vanilla" "-s" "-e" "cat(R.home('include'))"
   OUTPUT_VARIABLE LIBR_INCLUDE_DIRS
)
set(LIBR_INCLUDE_DIRS "${LIBR_INCLUDE_DIRS}" CACHE PATH "R include directory")

# Query R for doc directory
execute_process(
   COMMAND "${LIBR_EXECUTABLE}" "--vanilla" "-s" "-e" "cat(R.home('doc'))"
   OUTPUT_VARIABLE LIBR_DOC_DIR
)
set(LIBR_DOC_DIR "${LIBR_DOC_DIR}" CACHE PATH "R doc directory")

# Query R for lib directory
execute_process(
   COMMAND "${LIBR_EXECUTABLE}" "--vanilla" "-s" "-e" "cat(R.home('lib'))"
   OUTPUT_VARIABLE LIBR_LIB_DIR
)
set(LIBR_LIB_DIR "${LIBR_LIB_DIR}" CACHE PATH "R lib directory")

# ===========================================================================
# 5. Find libraries
# ===========================================================================

# Build list of hint paths for library search
set(_LIBR_LIBRARY_HINTS
   "${LIBR_LIB_DIR}"
)

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
find_library(LIBR_CORE_LIBRARY NAMES R HINTS ${_LIBR_LIBRARY_HINTS})
if(LIBR_CORE_LIBRARY)
   set(LIBR_LIBRARIES ${LIBR_CORE_LIBRARY})
else()
   message(STATUS "Could not find libR shared library")
endif()

# Find BLAS library (optional - R may use system BLAS)
find_library(LIBR_BLAS_LIBRARY NAMES Rblas HINTS ${_LIBR_LIBRARY_HINTS})
if(LIBR_BLAS_LIBRARY)
   list(APPEND LIBR_LIBRARIES ${LIBR_BLAS_LIBRARY})
endif()

# Find LAPACK library (optional - R may use system LAPACK)
find_library(LIBR_LAPACK_LIBRARY NAMES Rlapack HINTS ${_LIBR_LIBRARY_HINTS})
if(LIBR_LAPACK_LIBRARY)
   list(APPEND LIBR_LIBRARIES ${LIBR_LAPACK_LIBRARY})
endif()

# Find Rgraphapp (Windows only)
if(WIN32)
   find_library(LIBR_GRAPHAPP_LIBRARY NAMES Rgraphapp HINTS ${_LIBR_LIBRARY_HINTS})
   if(LIBR_GRAPHAPP_LIBRARY)
      list(APPEND LIBR_LIBRARIES ${LIBR_GRAPHAPP_LIBRARY})
   endif()
endif()

# Cache the final library list
if(LIBR_LIBRARIES)
   set(LIBR_LIBRARIES ${LIBR_LIBRARIES} CACHE PATH "R runtime libraries")
endif()

# ===========================================================================
# 6. Validation and cleanup
# ===========================================================================

include(FindPackageHandleStandardArgs)
find_package_handle_standard_args(LibR DEFAULT_MSG
   LIBR_HOME
   LIBR_EXECUTABLE
   LIBR_INCLUDE_DIRS
   LIBR_LIBRARIES
   LIBR_DOC_DIR
)

mark_as_advanced(
   LIBR_CORE_LIBRARY
   LIBR_BLAS_LIBRARY
   LIBR_LAPACK_LIBRARY
   LIBR_GRAPHAPP_LIBRARY
)
