#
# CMakeLists.txt
#
# Copyright (C) 2009-11 by RStudio, Inc.
#
# This program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

cmake_minimum_required(VERSION 2.6)
project (RSTUDIO_CPP)

# include globals (normally these are included at the root but we also
# include them here to support cpp only configurations for development)
include("${CMAKE_CURRENT_SOURCE_DIR}/../../CMakeGlobals.txt")

# UNIX specific global directivies
if(UNIX)
   # cmake modules
   include(CheckFunctionExists REQUIRED)
   include(CheckSymbolExists REQUIRED)

   # need pkgconfig for pango cairo on linux
   if(NOT APPLE)
      find_package(PkgConfig REQUIRED)
   endif()

   # compiler flags
   add_definitions(-Wall -pthread)

   # workaround boost bug (https://svn.boost.org/trac/boost/ticket/4568)
   # by disabling kqueue support. note that this bug was fixed in boost 1.45
   add_definitions(-DBOOST_ASIO_DISABLE_KQUEUE)

   # if present, set osx deployment target variables from environment vars
   if(APPLE)
      if(NOT $ENV{CMAKE_OSX_SYSROOT} STREQUAL "")
         set(CMAKE_OSX_SYSROOT $ENV{CMAKE_OSX_SYSROOT})
         message(STATUS "Set CMAKE_OSX_SYSROOT to ${CMAKE_OSX_SYSROOT}")
      endif()
      if(NOT $ENV{CMAKE_OSX_DEPLOYMENT_TARGET} STREQUAL "")
         set(CMAKE_OSX_DEPLOYMENT_TARGET $ENV{CMAKE_OSX_DEPLOYMENT_TARGET})
         message(STATUS "Set CMAKE_OSX_DEPLOYMENT_TARGET to ${CMAKE_OSX_DEPLOYMENT_TARGET}")
      endif()
   endif()

# Win32 specific global directives
else()
   add_definitions(-DWINVER=0x501)

   if(RSTUDIO_SESSION_WIN64)

      # increase stack size to 10MB, avoid mingw auto-importing warning,
      set(CMAKE_EXE_LINKER_FLAGS ${CMAKE_EXE_LINKER_FLAGS}
         -Wl,--stack=0x00a00000,--enable-auto-import)

      add_definitions(-D_WIN64
                      -D_WIN64_WINNT=0x0501
                      -D_WIN64_IE=0x600
                      -DWIN64_LEAN_AND_MEAN
                      -DBOOST_USE_WINDOWS_H)
   else()

      # increase stack size to 10MB, avoid mingw auto-importing warning,
      # and ensure that we are large address aware
      set(CMAKE_EXE_LINKER_FLAGS ${CMAKE_EXE_LINKER_FLAGS}
         -Wl,--stack=0x00a00000,--enable-auto-import,--large-address-aware)

      add_definitions(-D_WIN32_WINNT=0x0501
                      -D_WIN32_IE=0x600
                      -DWIN32_LEAN_AND_MEAN)
   endif()

endif()

# on osx, boost::enable_if<boost::filesystem::is_basic_path checking in
# filesystem (see operations.hpp) seem to cause a hard abort in release
# mode if the file doesn't exist. so disable this check on all platforms
# (for consistency) by disabling support for wide filenames
add_definitions(-DBOOST_FILESYSTEM_NARROW_ONLY -DBOOST_FILESYSTEM2_NARROW_ONLY)

# find boost and required libraries
if(RSTUDIO_PACKAGE_BUILD)
   set(Boost_USE_STATIC_LIBS ON)
endif()
set(BOOST_VERSION 1.40.0)
list(APPEND BOOST_LIBS
   date_time
   filesystem
   iostreams
   program_options
   regex
   signals
   system
   thread
)

# UNIX BOOST
if(UNIX)
   # for package builds hard-code to /usr/local/include/boost
   if(RSTUDIO_PACKAGE_BUILD AND EXISTS /usr/local/include/boost)
      # first find headers
      set(BOOST_INCLUDEDIR /usr/local/include)
      find_package(Boost ${BOOST_VERSION} REQUIRED)

      # define list of libraries manually
      set(BOOST_LIB_DIR /usr/local/lib)
      foreach(BOOST_LIB ${BOOST_LIBS})
         list(APPEND Boost_LIBRARIES ${BOOST_LIB_DIR}/libboost_${BOOST_LIB}.a)
      endforeach()
   else()
      find_package(Boost ${BOOST_VERSION} REQUIRED COMPONENTS ${BOOST_LIBS})
   endif()

# WIN32 BOOST
else()
     # hard-code to our own prebuilt boost libs
     set(Boost_USE_STATIC_LIBS ON)
     set(RSTUDIO_WINDOWS_DEPENDENCIES_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../dependencies/windows")
     if(RSTUDIO_SESSION_WIN64)
        set(BOOST_ROOT "${RSTUDIO_WINDOWS_DEPENDENCIES_DIR}/boost-win/boost64")
     else()
        set(BOOST_ROOT "${RSTUDIO_WINDOWS_DEPENDENCIES_DIR}/boost-win/boost32")
     endif()
     set(BOOST_INCLUDEDIR "${BOOST_ROOT}/include/boost-1_44")

   find_package(Boost ${BOOST_VERSION} REQUIRED COMPONENTS ${BOOST_LIBS})
endif()

# find LibR
if(RSTUDIO_SESSION_WIN64)
   set(LIBR_FIND_WINDOWS_64BIT TRUE)
endif()
find_package(LibR REQUIRED)

# verify we got the required R version
if(LIBR_FOUND)
   include(CheckCSourceRuns)
   set(CMAKE_REQUIRED_INCLUDES ${LIBR_INCLUDE_DIRS})
   check_c_source_runs("
     #include <Rversion.h>
     int main()
     {
        int meetsRequirement = R_VERSION >= R_Version(${RSTUDIO_R_MAJOR_VERSION_REQUIRED},${RSTUDIO_R_MINOR_VERSION_REQUIRED},${RSTUDIO_R_PATCH_VERSION_REQUIRED});
        return !meetsRequirement;
     }"
     LIBR_MINIMUM_VERSION)
   if(NOT LIBR_MINIMUM_VERSION)
      message(FATAL_ERROR "Minimum R version (${RSTUDIO_R_MAJOR_VERSION_REQUIRED}.${RSTUDIO_R_MINOR_VERSION_REQUIRED}.${RSTUDIO_R_PATCH_VERSION_REQUIRED}) not found.")
   endif()
endif()

# initialize subdirectories
file(MAKE_DIRECTORY conf)

# core subprojects
add_subdirectory(core)
add_subdirectory(r)

# add desktop subprojects if we aren't building in server only mode
if(RSTUDIO_DESKTOP)

   add_subdirectory(desktop)
   configure_file(rdesktop-dev ${CMAKE_CURRENT_BINARY_DIR}/rdesktop-dev)
   configure_file(conf/rdesktop-dev.conf ${CMAKE_CURRENT_BINARY_DIR}/conf/rdesktop-dev.conf)

endif()

# add this after desktop and server so it is not included in fixup_bundle
# processing which we do in destkop
add_subdirectory(session)

# add server subprojects if we aren't building in desktop only mode
if(RSTUDIO_SERVER)

   add_subdirectory(server)

   configure_file(rserver-dev ${CMAKE_CURRENT_BINARY_DIR}/rserver-dev)
   configure_file(conf/rserver-dev.conf ${CMAKE_CURRENT_BINARY_DIR}/conf/rserver-dev.conf)
   configure_file(conf/rsession-dev.conf ${CMAKE_CURRENT_BINARY_DIR}/conf/rsession-dev.conf)

   add_subdirectory(session/postback)
   configure_file(session/postback/rpostback-pdfviewer session/postback)

endif()

