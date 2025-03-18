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

function(dependency)

   set(_NAME "${ARGV0}")
   cmake_parse_arguments(PARSE_ARGV 1 "" "" "COMMENT;VERSION;REPOSITORY;REVISION" "")

   set(${_NAME}_VERSION    "${_VERSION}"    CACHE INTERNAL "")
   set(${_NAME}_REPOSITORY "${_REPOSITORY}" CACHE INTERNAL "")
   set(${_NAME}_REVISION   "${_REVISION}"   CACHE INTERNAL "")

   string(REPLACE "." ";" _VERSION_LIST "${_VERSION}")
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


# expected
dependency(EXPECTED
   COMMENT    "Single header implementation of std::expected with functional-style extensions."
   VERSION    "1.1.0"
   REPOSITORY "https://github.com/TartanLlama/expected"
   REVISION   "292eff8bd8ee230a7df1d6a1c00c4ea0eb2f0362") # pragma: allowlist secret

set(EXPECTED_BUILD_TESTS OFF)


# fmt
dependency(FMT
   COMMENT    "{fmt} is an open-source formatting library providing a fast and safe alternative to C stdio and C++ iostreams."
   VERSION    "11.1.4"
   REPOSITORY "https://github.com/fmtlib/fmt"
   REVISION   "123913715afeb8a437e6388b4473fcc4753e1c9a") # pragma: allowlist secret


# gsl-lite
dependency(GSL_LITE
   COMMENT    "gsl-lite is an implementation of the C++ Core Guidelines Support Library originally based on Microsoft GSL."
   VERSION    "0.42.0"
   REPOSITORY "https://github.com/gsl-lite/gsl-lite"
   REVISION   "21751fb0473473e27ffb1f280543885ed65447a8") # pragma: allowlist secret


# hunspell
dependency(HUNSPELL
   COMMENT    "Hunspell is a free spell checker and morphological analyzer library and command-line tool, licensed under LGPL/GPL/MPL tri-license."
   VERSION    "1.7.2"
   REPOSITORY "https://github.com/hunspell/hunspell"
   REVISION   "2969be996acad84b91ab3875b1816636fe61a40e" # pragma: allowlist secret
)


# websocketpp
dependency(WEBSOCKETPP
   COMMENT    "WebSocket++ is a header only C++ library that implements RFC6455 The WebSocket Protocol."
   VERSION    "0.8.3"
   REPOSITORY "https://github.com/amini-allight/websocketpp"
   REVISION   "a11fa6fe4937ca251753e90b1836a68bf57768a9") # pragma: allowlist secret


# yaml-cpp
dependency(YAML_CPP
   COMMENT    "yaml-cpp is a YAML parser and emitter in C++ matching the YAML 1.2 spec."
   VERSION    "0.8.0"
   REPOSITORY "https://github.com/jbeder/yaml-cpp"
   REVISION   "f7320141120f720aecc4c32be25586e7da9eb978") # pragma: allowlist secret


function(fetch)

   set(_INDEX 0)
   while(_INDEX LESS ${ARGC})

      math(EXPR _INDEX0 "${_INDEX} + 0")
      math(EXPR _INDEX1 "${_INDEX} + 1")
      math(EXPR _INDEX  "${_INDEX} + 2")

      list(GET ARGV ${_INDEX0} _NAME)
      list(GET ARGV ${_INDEX1} _PREFIX)

      if(RSTUDIO_USE_SYSTEM_${_PREFIX})
         find_package("${_NAME}" "${${_PREFIX}_VERSION}" REQUIRED)
      else()
         FetchContent_Declare("${_NAME}"
            GIT_REPOSITORY "${${_PREFIX}_REPOSITORY}"
            GIT_TAG        "${${_PREFIX}_REVISION}"
            GIT_SHALLOW    ON
            EXCLUDE_FROM_ALL)
      endif()

   endwhile()

   set(_INDEX 0)
   while(_INDEX LESS ${ARGC})

      math(EXPR _INDEX0 "${_INDEX} + 0")
      math(EXPR _INDEX1 "${_INDEX} + 1")
      math(EXPR _INDEX  "${_INDEX} + 2")

      list(GET ARGV ${_INDEX0} _NAME)
      list(GET ARGV ${_INDEX1} _PREFIX)

      if(NOT RSTUDIO_USE_SYSTEM_${_PREFIX})
         FetchContent_MakeAvailable("${_NAME}")
         set("${_PREFIX}_SOURCE_DIR" "${CMAKE_BINARY_DIR}/_deps/${_NAME}-src" CACHE INTERNAL "")
         set("${_PREFIX}_BINARY_DIR" "${CMAKE_BINARY_DIR}/_deps/${_NAME}-build" CACHE INTERNAL "")
      endif()

   endwhile()

endfunction()


fetch(
   expected     EXPECTED
   fmt          FMT
   gsl-lite     GSL_LITE
   hunspell     HUNSPELL
   websocketpp  WEBSOCKETPP
   yaml-cpp     YAML_CPP)


# Create websocketpp target.
add_library(rstudio-websocketpp INTERFACE)
target_include_directories(rstudio-websocketpp INTERFACE "${websocketpp_SOURCE_DIR}")


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
