#
# CMakeLists.txt
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

include(ExternalProject)
include(FetchContent)

if(NOT DEFINED CMAKE_POLICY_VERSION_MINIMUM)
   set(CMAKE_POLICY_VERSION_MINIMUM "3.5" CACHE STRING "" FORCE)
endif()

cmake_policy(SET CMP0048 NEW)
set(FETCHCONTENT_QUIET TRUE)
set(CMAKE_WARN_DEPRECATED FALSE)


#
# Declare an external dependency.
#
# This function accepts the following single-value arguments:
#
# COMMENT:     A one-line description of the dependency, usually from the dependency itself.
# VERSION:     The version of the dependency to be used.
# REPOSITORY:  The (git) repository to be used when retrieving the dependency.
# REVISION:    The (git) revision / commit hash to be used when retrieving the dependency.
# CUSTOM:      Optional; when TRUE, the project is retrieved but not automatically added to the build.
# PLATFORMS:   Optional; a list of platforms for which this dependency should be retrieved and built.
#
# Use the CUSTOM argument when you want to fetch an external dependency, but need to manually
# set up a build target based on that package's sources. This is mainly useful for projects which
# don't provide the requisite CMake infrastructure, or for projects which define a CMakeLists.txt
# which we want or need to ignore.
#
function(dependency)

   set(_NAME "${ARGV0}")
   cmake_parse_arguments(PARSE_ARGV 1 "" "" "COMMENT;CUSTOM;VERSION;REPOSITORY;REVISION;CXXFLAGS" "PLATFORMS")

   set(${_NAME}_VERSION    "${_VERSION}"    CACHE INTERNAL "")
   set(${_NAME}_REPOSITORY "${_REPOSITORY}" CACHE INTERNAL "")
   set(${_NAME}_REVISION   "${_REVISION}"   CACHE INTERNAL "")
   set(${_NAME}_CUSTOM     "${_CUSTOM}"     CACHE INTERNAL "")
   set(${_NAME}_PLATFORMS  "${_PLATFORMS}"  CACHE INTERNAL "")
   set(${_NAME}_CXXFLAGS   "${_CXXFLAGS}"   CACHE INTERNAL "")

   if(_PLATFORMS)
      foreach(_PLATFORM IN LISTS _PLATFORMS)
         if(_PLATFORM)
            set(${_NAME}_ENABLED TRUE)
            break()
         endif()
      endforeach()
   else()
      set(${_NAME}_ENABLED TRUE)
   endif()

   set(${_NAME}_ENABLED ${${_NAME}_ENABLED} CACHE INTERNAL "")

   string(REPLACE "." ";" _VERSION_LIST "${_VERSION}")
   list(APPEND _VERSION_LIST 0 0 0)
   list(GET _VERSION_LIST 0 _VERSION_MAJOR)
   list(GET _VERSION_LIST 1 _VERSION_MINOR)
   list(GET _VERSION_LIST 2 _VERSION_PATCH)

   set(${_NAME}_VERSION_MAJOR "${_VERSION_MAJOR}" CACHE INTERNAL "")
   set(${_NAME}_VERSION_MINOR "${_VERSION_MINOR}" CACHE INTERNAL "")
   set(${_NAME}_VERSION_PATCH "${_VERSION_PATCH}" CACHE INTERNAL "")

   set(
      ${_NAME}_VERSION_MAJMIN
      "${_VERSION_MAJOR}.${_VERSION_MINOR}"
      CACHE INTERNAL "")

endfunction()


#
# Global settings protection for FetchContent dependencies.
#
# Some dependencies (e.g. libgit2) set global cache variables like
# CMAKE_C_STANDARD, BUILD_SHARED_LIBS, etc. as part of their CMakeLists.txt.
# These leak into the parent project and can break the build.
#
# save_global_settings() / restore_global_settings() snapshot and restore
# the listed variables around the fetch() call to prevent this.
#
set(_PROTECTED_GLOBAL_SETTINGS
   BUILD_SHARED_LIBS
   CMAKE_C_STANDARD
   CMAKE_C_EXTENSIONS
   CMAKE_CXX_STANDARD
   CMAKE_CXX_EXTENSIONS
)

macro(save_global_settings)
   foreach(_VAR IN LISTS _PROTECTED_GLOBAL_SETTINGS)
      if(DEFINED CACHE{${_VAR}})
         set(_SAVED_${_VAR} "${${_VAR}}")
         set(_SAVED_${_VAR}_DEFINED TRUE)
      else()
         set(_SAVED_${_VAR}_DEFINED FALSE)
      endif()
   endforeach()
endmacro()

macro(restore_global_settings)
   foreach(_VAR IN LISTS _PROTECTED_GLOBAL_SETTINGS)
      if(_SAVED_${_VAR}_DEFINED)
         set(${_VAR} "${_SAVED_${_VAR}}" CACHE STRING "" FORCE)
      else()
         unset(${_VAR} CACHE)
      endif()
   endforeach()
endmacro()


function(fetch)

   set(_INDEX 0)
   while(_INDEX LESS ${ARGC})

      math(EXPR _INDEX0 "${_INDEX} + 0")
      math(EXPR _INDEX1 "${_INDEX} + 1")
      math(EXPR _INDEX  "${_INDEX} + 2")

      list(GET ARGV ${_INDEX0} _NAME)
      list(GET ARGV ${_INDEX1} _PREFIX)

      if(${_PREFIX}_CUSTOM)
         set(_SOURCE_SUBDIR "_ignored")
      else()
         set(_SOURCE_SUBDIR ".")
      endif()

      if(${_PREFIX}_ENABLED)
         if(RSTUDIO_USE_SYSTEM_DEPENDENCIES OR RSTUDIO_USE_SYSTEM_${_PREFIX})
            if(CMAKE_VERSION VERSION_GREATER "3.24")
               find_package("${_NAME}" "${${_PREFIX}_VERSION}" REQUIRED GLOBAL)
            else()
               find_package("${_NAME}" "${${_PREFIX}_VERSION}" REQUIRED)
            endif()
         else()
            set(${_PREFIX}_FETCHED TRUE)

            FetchContent_Declare("${_NAME}"
               GIT_REPOSITORY "${${_PREFIX}_REPOSITORY}"
               GIT_TAG        "${${_PREFIX}_REVISION}"
               GIT_SHALLOW    ON
               SOURCE_SUBDIR  "${_SOURCE_SUBDIR}"
               EXCLUDE_FROM_ALL)
         endif()
      endif()

   endwhile()

   set(_INDEX 0)
   while(_INDEX LESS ${ARGC})

      math(EXPR _INDEX0 "${_INDEX} + 0")
      math(EXPR _INDEX1 "${_INDEX} + 1")
      math(EXPR _INDEX  "${_INDEX} + 2")

      list(GET ARGV ${_INDEX0} _NAME)
      list(GET ARGV ${_INDEX1} _PREFIX)

      if(${_PREFIX}_FETCHED)
         message(STATUS "Fetching dependency ${_NAME} ${${_PREFIX}_VERSION}")
         set(_CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS}")
         set(CMAKE_CXX_FLAGS "${${_PREFIX}_CXXFLAGS} ${CMAKE_CXX_FLAGS}")
         FetchContent_MakeAvailable("${_NAME}")
         set(CMAKE_CXX_FLAGS "${_CMAKE_CXX_FLAGS}")
         message(STATUS "Fetching dependency ${_NAME} ${${_PREFIX}_VERSION} - success")
         set("${_PREFIX}_SOURCE_DIR" "${CMAKE_BINARY_DIR}/_deps/${_NAME}-src" CACHE INTERNAL "")
         set("${_PREFIX}_BINARY_DIR" "${CMAKE_BINARY_DIR}/_deps/${_NAME}-build" CACHE INTERNAL "")
      endif()

   endwhile()

endfunction()



# dtl
dependency(DTL
   COMMENT    "dtl provides the functions for comparing two sequences have arbitrary type."
   VERSION    "1.21"
   REPOSITORY "https://github.com/cubicdaiya/dtl"
   REVISION   "32567bb9ec704f09040fb1ed7431a3d967e3df03" # pragma: allowlist secret
   CUSTOM     TRUE
)


# fmt
dependency(FMT
   COMMENT    "{fmt} is an open-source formatting library providing a fast and safe alternative to C stdio and C++ iostreams."
   VERSION    "11.1.4"
   REPOSITORY "https://github.com/fmtlib/fmt"
   REVISION   "123913715afeb8a437e6388b4473fcc4753e1c9a" # pragma: allowlist secret
)

set(FMT_INSTALL OFF)


# libgit2
dependency(LIBGIT2
   COMMENT    "libgit2 is a portable, pure C implementation of the Git core methods."
   VERSION    "1.9.2"
   REPOSITORY "https://github.com/libgit2/libgit2"
   REVISION   "ca225744b992bf2bf24e9a2eb357ddef78179667" # pragma: allowlist secret
)

set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(BUILD_CLI OFF CACHE BOOL "" FORCE)
set(BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(USE_SSH OFF CACHE BOOL "" FORCE)
set(USE_HTTPS OFF CACHE BOOL "" FORCE)
set(USE_ICONV OFF CACHE BOOL "" FORCE)
set(USE_NTLMCLIENT OFF CACHE BOOL "" FORCE)
set(USE_BUNDLED_ZLIB OFF CACHE BOOL "" FORCE)
set(REGEX_BACKEND "builtin" CACHE STRING "" FORCE)


# gsl-lite
dependency(GSL_LITE
   COMMENT    "gsl-lite is an implementation of the C++ Core Guidelines Support Library originally based on Microsoft GSL."
   VERSION    "0.42.0"
   REPOSITORY "https://github.com/gsl-lite/gsl-lite"
   REVISION   "21751fb0473473e27ffb1f280543885ed65447a8" # pragma: allowlist secret
)


# gtest and gmock
dependency(GTEST
   COMMENT    "Google Test is a C++ test framework based on the xUnit architecture."
   VERSION    "1.17.0"
   REPOSITORY "https://github.com/google/googletest"
   REVISION   "52eb8108c5bdec04579160ae17225d66034bd723" # pragma: allowlist secret
)

# Disable installation of gtest/gmock - these are test dependencies only
set(INSTALL_GTEST OFF CACHE BOOL "" FORCE)
set(INSTALL_GMOCK OFF CACHE BOOL "" FORCE)

# Force gtest to use the same runtime library as the rest of the project on Windows
if(WIN32)
   set(gtest_force_shared_crt ON CACHE BOOL "" FORCE)
endif()


# hunspell
dependency(HUNSPELL
   COMMENT    "Hunspell is a free spell checker and morphological analyzer library and command-line tool, licensed under LGPL/GPL/MPL tri-license."
   VERSION    "1.7.2"
   REPOSITORY "https://github.com/hunspell/hunspell"
   REVISION   "2969be996acad84b91ab3875b1816636fe61a40e" # pragma: allowlist secret
   CUSTOM     TRUE
)


# rapidjson
dependency(RAPIDJSON
   COMMENT    "A fast JSON parser/generator for C++ with both SAX/DOM style API"
   VERSION    "1.1.0"
   REPOSITORY "https://github.com/Tencent/rapidjson"
   REVISION   "24b5e7a8b27f42fa16b96fc70aade9106cf7102f" # pragma: allowlist secret
   CUSTOM     TRUE
)


# tl-expected
dependency(TL_EXPECTED
   COMMENT    "Single header implementation of std::expected with functional-style extensions."
   VERSION    "1.1.0"
   REPOSITORY "https://github.com/TartanLlama/expected"
   REVISION   "292eff8bd8ee230a7df1d6a1c00c4ea0eb2f0362" # pragma: allowlist secret
)

set(EXPECTED_BUILD_TESTS OFF)


# utfcpp
dependency(UTFCPP
   COMMENT    "UTF8-CPP: UTF-8 with C++ in a Portable Way"
   VERSION    "4.0.8"
   REPOSITORY "https://github.com/nemtrif/utfcpp"
   REVISION   "f9319195dfddf369f68f18e7c0039b3f351797fd" # pragma: allowlist secret
   CUSTOM     TRUE
)


# websocketpp
dependency(WEBSOCKETPP
   COMMENT    "WebSocket++ is a header only C++ library that implements RFC6455 The WebSocket Protocol."
   VERSION    "0.8.3"
   REPOSITORY "https://github.com/amini-allight/websocketpp"
   REVISION   "ee8cf4257e001d939839cff5b1766a835b749cd6" # pragma: allowlist secret
   CUSTOM     TRUE
)


# yaml-cpp
dependency(YAML_CPP
   COMMENT    "yaml-cpp is a YAML parser and emitter in C++ matching the YAML 1.2 spec."
   VERSION    "0.8.0"
   REPOSITORY "https://github.com/jbeder/yaml-cpp"
   REVISION   "f7320141120f720aecc4c32be25586e7da9eb978" # pragma: allowlist secret
)

# help yaml-cpp 0.8.0 find <cstdint> during compilation
if(NOT MSVC AND NOT APPLE)
   file(WRITE "${CMAKE_CURRENT_BINARY_DIR}/yaml-cpp.h" "#include <cstdint>")
   set(YAML_CPP_CXXFLAGS "-include ${CMAKE_CURRENT_BINARY_DIR}/yaml-cpp.h")
endif()


# zlib
dependency(ZLIB
   COMMENT    "zlib is a general purpose data compression library."
   VERSION    "1.3.1"
   REPOSITORY "https://github.com/madler/zlib"
   REVISION   "51b7f2abdade71cd9bb0e7a373ef2610ec6f9daf" # pragma: allowlist secret
   PLATFORMS  WIN32
)

save_global_settings()

fetch(
   dtl          DTL
   fmt          FMT
   gsl-lite     GSL_LITE
   libgit2      LIBGIT2
   gtest        GTEST
   hunspell     HUNSPELL
   rapidjson    RAPIDJSON
   tl-expected  TL_EXPECTED
   utfcpp       UTFCPP
   websocketpp  WEBSOCKETPP
   yaml-cpp     YAML_CPP
   zlib         ZLIB)

restore_global_settings()


# Create dtl target.
add_library(rstudio-dtl INTERFACE EXCLUDE_FROM_ALL)
target_include_directories(rstudio-dtl INTERFACE "${DTL_SOURCE_DIR}")


# Create hunspell target.
file(GLOB HUNSPELL_HEADER_FILES "${HUNSPELL_SOURCE_DIR}/src/hunspell/*.h*")
file(GLOB HUNSPELL_SOURCE_FILES "${HUNSPELL_SOURCE_DIR}/src/hunspell/*.c*")
add_library(rstudio-hunspell STATIC ${HUNSPELL_SOURCE_FILES} ${HUNSPELL_HEADER_FILES})
set_target_properties(rstudio-hunspell PROPERTIES LINKER_LANGUAGE CXX)
target_include_directories(rstudio-hunspell SYSTEM AFTER INTERFACE "${HUNSPELL_SOURCE_DIR}/src")
target_compile_definitions(rstudio-hunspell PUBLIC HUNSPELL_STATIC=1)

if(WIN32)
   target_include_directories(rstudio-hunspell SYSTEM AFTER PRIVATE "${HUNSPELL_PREFIX_DIR}/msvc")
   target_compile_options(rstudio-hunspell PRIVATE /wd4244 /wd4267)
   target_compile_options(rstudio-hunspell INTERFACE /wd4996)
else()
   target_compile_options(rstudio-hunspell PRIVATE -Wno-deprecated-declarations -Wno-sign-compare -Wno-unused-but-set-variable)
endif()


# Create rapidjson target.
add_library(rstudio-rapidjson INTERFACE EXCLUDE_FROM_ALL)
target_compile_definitions(rstudio-rapidjson INTERFACE "-DRAPIDJSON_NO_SIZETYPEDEFINE")
target_include_directories(rstudio-rapidjson INTERFACE "${RAPIDJSON_SOURCE_DIR}/include")


# Create utfcpp target.
add_library(rstudio-utfcpp INTERFACE EXCLUDE_FROM_ALL)
target_include_directories(rstudio-utfcpp INTERFACE "${UTFCPP_SOURCE_DIR}/source")


# Create websocketpp target.
add_library(rstudio-websocketpp INTERFACE EXCLUDE_FROM_ALL)
target_include_directories(rstudio-websocketpp INTERFACE "${WEBSOCKETPP_SOURCE_DIR}")

