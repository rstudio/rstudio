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

cmake_minimum_required(VERSION 3.6.3)
project (RSTUDIO_GWT)

# define output dirs
if("${GWT_BIN_DIR}" STREQUAL "")
   set(GWT_BIN_DIR "bin")
endif()
if("${GWT_WWW_DIR}" STREQUAL "")
   set(GWT_WWW_DIR "www")
endif()
if("${GWT_EXTRAS_DIR}" STREQUAL "")
   set(GWT_EXTRAS_DIR "extras")
endif()

# allow opt in / out of GWT build
# mainly for faster iteration in builds
if(NOT DEFINED GWT_BUILD)
   set(GWT_BUILD Yes)
endif()

if(DEFINED ENV{GWT_BUILD})
   set(GWT_BUILD $ENV{GWT_BUILD})
endif()

if(DEFINED ENV{GWT_COPY})
   set(GWT_COPY $ENV{GWT_COPY})
endif()

# set main module (allow override from envvar)
set(GWT_MAIN_MODULE "RStudio")
if(DEFINED ENV{GWT_MAIN_MODULE})
   set(GWT_MAIN_MODULE $ENV{GWT_MAIN_MODULE})
endif()

# memory settings (allow override from env)
set (GWT_XMX "-Xmx1536M")
if(DEFINED ENV{GWT_XMX})
   set(GWT_XMX $ENV{GWT_XMX})
endif()
set (GWT_XSS "-Xss16M")
if(DEFINED ENV{GWT_XSS})
   set(GWT_XSS $ENV{GWT_XSS})
endif()

# GWT output style (allow override from env). The production compile defaults
# to OBFUSCATED (smallest output, unreadable symbols); set GWT_STYLE=PRETTY
# (or DETAILED) so compiled-JS stack traces -- e.g. the uncaught client
# exceptions captured by the e2e automation agent -- carry readable Java symbol
# names. Intended for CI / e2e builds; release builds leave this unset.
#
# Only the bare style value is threaded through to Ant (via the gwt.style
# property); build.xml assembles the "-style X" argument itself. Passing the
# single-token value avoids embedding a space in an argument that must survive
# the CMake -> shell -> Ant boundary.
set(GWT_STYLE "")
if(DEFINED ENV{GWT_STYLE})
   set(GWT_STYLE $ENV{GWT_STYLE})
   message(STATUS "Using GWT style: ${GWT_STYLE}")
endif()

if(GWT_BUILD)
   message(STATUS "Configured to build GWT")
   message(STATUS "Using GWT module: org.rstudio.studio.${GWT_MAIN_MODULE}")
   set(GWT_COPY Yes)

   find_program(ANT
      NAMES ant
      PATHS "/opt/homebrew/bin" "/usr/local/bin")

   if(ANT)
      message(STATUS "Using ant: ${ANT}")
   else()
      message(FATAL_ERROR "ant not found (required to build GWT)")
   endif()

   # copy GWT www directory to build tree
   if(RSTUDIO_PACKAGE_BUILD AND NOT GWT_WWW_DIR STREQUAL www)
      file(
         COPY www
         DESTINATION "${GWT_WWW_DIR}/.."
         PATTERN rstudio EXCLUDE)
   endif()
   
   # depend on Java source files
   file(GLOB_RECURSE GWT_SOURCE_FILES CONFIGURE_DEPENDS "${CMAKE_CURRENT_SOURCE_DIR}/src/*.java")

   # generated during GWT build command
   set(GWT_BUILD_TIMESTAMP "${CMAKE_CURRENT_BINARY_DIR}/timestamp")

   # define GWT build
   add_custom_command(
      OUTPUT
         "${GWT_BUILD_TIMESTAMP}"
      DEPENDS
         "${GWT_SOURCE_FILES}"
      WORKING_DIRECTORY
         "${CMAKE_CURRENT_SOURCE_DIR}"
      COMMENT
         "Building GWT sources"
      COMMAND
         ${ANT}
         -Dbuild.dir="${GWT_BIN_DIR}"
         -Dextras.dir="${GWT_EXTRAS_DIR}"
         -Dwww.dir="${GWT_WWW_DIR}"
         -Dgwt.style="${GWT_STYLE}"
         -Dgwt.xmx="${GWT_XMX}"
         -Dgwt.xss="${GWT_XSS}"
         -Dgwt.main.module="org.rstudio.studio.${GWT_MAIN_MODULE}"
      COMMAND
         "${CMAKE_COMMAND}" -E touch "${GWT_BUILD_TIMESTAMP}")

   # invoke ant to build
   add_custom_target(
      gwt_build ALL
      DEPENDS "${GWT_BUILD_TIMESTAMP}")

endif()

if(NOT DEFINED GWT_COPY)
   set(GWT_COPY Yes)
endif()

if(GWT_COPY)
   message(STATUS "Configured to install GWT")

   # set JAVA_HOME variable for configure below
   set(JAVA_HOME $ENV{JAVA_HOME})
   
   # create test script and copy to binary directory with executable permissions
   configure_file(
      ${CMAKE_CURRENT_SOURCE_DIR}/gwt-unit-tests.sh.in
      ${CMAKE_CURRENT_BINARY_DIR}${CMAKE_FILES_DIRECTORY}/gwt-unit-tests.sh
      @ONLY)

   file(COPY ${CMAKE_CURRENT_BINARY_DIR}${CMAKE_FILES_DIRECTORY}/gwt-unit-tests.sh
             DESTINATION ${CMAKE_CURRENT_BINARY_DIR}
             FILE_PERMISSIONS OWNER_READ OWNER_WRITE OWNER_EXECUTE GROUP_READ
             GROUP_EXECUTE WORLD_READ WORLD_EXECUTE)

   # install local web resources
   # we exclude WEB-INF here just in case a prior GWT build updated this;
   # JavaScript source maps (e.g. panmirror's) are excluded since they are
   # only useful when debugging with browser devtools
   install(
      DIRECTORY www
      DESTINATION "${RSTUDIO_INSTALL_SUPPORTING}"
      PATTERN "WEB-INF" EXCLUDE
      PATTERN ".gitignore" EXCLUDE
      REGEX "\\.js\\.map$" EXCLUDE)

   # install compiled GWT artefacts
   install(
      DIRECTORY "${GWT_WWW_DIR}"
      DESTINATION "${RSTUDIO_INSTALL_SUPPORTING}"
      PATTERN "WEB-INF" EXCLUDE
      PATTERN ".gitignore" EXCLUDE
      REGEX "\\.js\\.map$" EXCLUDE)

   # copy symbol maps, gzipped to save space when supported -- they are only
   # read when deobfuscating client-side stack traces for the log, and
   # GwtSymbolMaps.cpp reads both the plain and gzipped forms. only the
   # *.symbolMap files themselves are gzipped; any other files in the folder
   # (e.g. the .symbolMapOffset read by ServerEval.cpp) are installed as-is
   if(CMAKE_VERSION VERSION_LESS "3.18")
      install(
         DIRECTORY "${GWT_EXTRAS_DIR}/rstudio/symbolMaps/"
         DESTINATION ${RSTUDIO_INSTALL_SUPPORTING}/www-symbolmaps)
   else()
      install(
         DIRECTORY "${GWT_EXTRAS_DIR}/rstudio/symbolMaps/"
         DESTINATION ${RSTUDIO_INSTALL_SUPPORTING}/www-symbolmaps
         PATTERN "*.symbolMap" EXCLUDE)

      # GWT_EXTRAS_DIR may be relative to this source directory (as
      # install(DIRECTORY) would resolve it); the install-time code below
      # runs with a different working directory and needs an absolute path
      get_filename_component(GWT_EXTRAS_ABSOLUTE_DIR "${GWT_EXTRAS_DIR}"
         ABSOLUTE BASE_DIR "${CMAKE_CURRENT_SOURCE_DIR}")

      # locate the gzip helper relative to this file: the top-level source
      # directory need not be the repository root (e.g. cmake -S src)
      get_filename_component(GZIP_HELPER_SCRIPT
         "${CMAKE_CURRENT_SOURCE_DIR}/../../cmake/gzip-file.cmake" ABSOLUTE)
      install(CODE "
         # resolve symlinks (the folder exists by install time): the helper
         # runs with this directory as its working directory, and
         # file(ARCHIVE_CREATE) misresolves paths from symlinked directories
         get_filename_component(SYMBOL_MAPS_SOURCE_DIR
            \"${GWT_EXTRAS_ABSOLUTE_DIR}/rstudio/symbolMaps\" REALPATH)
         set(SYMBOL_MAPS_DIR \"\$ENV{DESTDIR}\${CMAKE_INSTALL_PREFIX}/${RSTUDIO_INSTALL_SUPPORTING}/www-symbolmaps\")
         file(MAKE_DIRECTORY \"\${SYMBOL_MAPS_DIR}\")
         file(GLOB SYMBOL_MAPS \"\${SYMBOL_MAPS_SOURCE_DIR}/*.symbolMap\")
         foreach(SYMBOL_MAP \${SYMBOL_MAPS})
            get_filename_component(SYMBOL_MAP_NAME \"\${SYMBOL_MAP}\" NAME)
            message(STATUS \"Compressing: \${SYMBOL_MAP_NAME}\")
            execute_process(
               COMMAND \"\${CMAKE_COMMAND}\"
                  \"-DINPUT=\${SYMBOL_MAP_NAME}\"
                  \"-DOUTPUT=\${SYMBOL_MAPS_DIR}/\${SYMBOL_MAP_NAME}.gz\"
                  -P \"${GZIP_HELPER_SCRIPT}\"
               WORKING_DIRECTORY \"\${SYMBOL_MAPS_SOURCE_DIR}\"
               RESULT_VARIABLE GZIP_RESULT)
            if(NOT GZIP_RESULT EQUAL 0)
               message(FATAL_ERROR \"Failed to compress \${SYMBOL_MAP_NAME}\")
            endif()
         endforeach()
      ")
   endif()

endif()

