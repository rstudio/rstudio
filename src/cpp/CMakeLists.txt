#
# CMakeLists.txt
#
# Copyright (C) 2009-18 by RStudio, Inc.
#
# Unless you have received this program directly from RStudio pursuant
# to the terms of a commercial license agreement with RStudio, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

# set minimum version
cmake_minimum_required(VERSION 2.6)

project (RSTUDIO_CPP)

# set compiler
include("${CMAKE_CURRENT_SOURCE_DIR}/../../CMakeCompiler.txt")

# include globals. normally these are included at the root but we also
# include them here to support configuring at the src level (i.e. for
# cpp-development-configurations)
include("${CMAKE_CURRENT_SOURCE_DIR}/../../CMakeGlobals.txt")

# global directives
add_definitions(-DBOOST_ENABLE_ASSERT_HANDLER)

# explicitly do not use new c++ 11 features for websocketpp
# they currently do not work with our source
add_definitions(-D_WEBSOCKETPP_NO_CPP11_MEMORY_=1)

# the non-strict masking option was identified as a potentially crashing 
# issue due to reliance on undefined C++ behavior, and was removed entirely in 
# websocketpp 0.7 (RStudio is using 0.5.1 at this time)
add_definitions(-DWEBSOCKETPP_STRICT_MASKING)

# make websockets build with MSVC
if(WIN32 AND MSVC)
   add_definitions(
      -D_WEBSOCKETPP_NOEXCEPT_
      -D_WEBSOCKETPP_CPP11_CHRONO
   )
endif()

# test directory
set(TESTS_INCLUDE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/tests/cpp" CACHE STRING "Test includes")

# enable c++11
if(NOT MSVC)
   include(CheckCXXCompilerFlag)
   CHECK_CXX_COMPILER_FLAG("-std=c++11" COMPILER_SUPPORTS_CXX11)
   if(NOT COMPILER_SUPPORTS_CXX11)
      message(FATAL_ERROR "The compiler ${CMAKE_CXX_COMPILER} has no C++11 support. Please use a different C++ compiler.")
   else()
      set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")
   endif()
endif()

# UNIX specific global directivies
if(UNIX)
   # cmake modules
   include(CheckFunctionExists REQUIRED)
   include(CheckSymbolExists REQUIRED)

   # compiler flags
   add_definitions(-Wall -pthread)

   if(APPLE)
      add_definitions(-Wno-unknown-warning-option -Wsign-compare -Wno-unused-local-typedefs
                      -Wno-deprecated-declarations)
   endif()

   # workaround boost bug (https://svn.boost.org/trac/boost/ticket/4568)
   # by disabling kqueue support. note that this bug was fixed in boost 1.45
   add_definitions(-DBOOST_ASIO_DISABLE_KQUEUE)

   if(APPLE)
      # if present, set osx deployment target variables from environment vars
      if(NOT $ENV{CMAKE_OSX_SYSROOT} STREQUAL "")
         set(CMAKE_OSX_SYSROOT $ENV{CMAKE_OSX_SYSROOT})
         message(STATUS "Set CMAKE_OSX_SYSROOT to ${CMAKE_OSX_SYSROOT}")
      endif()
      if(NOT $ENV{CMAKE_OSX_DEPLOYMENT_TARGET} STREQUAL "")
         set(CMAKE_OSX_DEPLOYMENT_TARGET $ENV{CMAKE_OSX_DEPLOYMENT_TARGET})
         message(STATUS "Set CMAKE_OSX_DEPLOYMENT_TARGET to ${CMAKE_OSX_DEPLOYMENT_TARGET}")
      endif()
      # Figure out what version of Mac OS X this is. Unfortunately 
      # CMAKE_SYSTEM_VERSION does not match uname -r, so get the Mac OS
      # version number another way.
      EXECUTE_PROCESS(COMMAND /usr/bin/sw_vers -productVersion OUTPUT_VARIABLE MACOSX_VERSION OUTPUT_STRIP_TRAILING_WHITESPACE)
      message(STATUS "Mac OS X version: ${MACOSX_VERSION}")
   endif()

   # gcc hardending options (see: http://wiki.debian.org/Hardening)
   if(NOT APPLE)
      add_definitions(-Wformat -Wformat-security)
      add_definitions(-D_FORTIFY_SOURCE=2)
      add_definitions(-fstack-protector --param ssp-buffer-size=4)
      add_definitions(-fPIC)
      if(CMAKE_BUILD_TYPE STREQUAL Debug)
         add_definitions(-O1)
         add_definitions(-D_GLIBCXX_ASSERTIONS)
      endif()
      set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -pie -Wl,-z,relro,-z,now")
   endif()

   # other useful gcc diagnostics
   if (CMAKE_CXX_COMPILER_ID STREQUAL "GNU")
      add_definitions(-Wlogical-op)
      if(CMAKE_CXX_COMPILER_VERSION VERSION_GREATER 7.0.0)
         add_definitions(-Wduplicated-cond)
         add_definitions(-Wduplicated-branches)
         add_definitions(-Wrestrict)
         add_definitions(-Wnull-dereference)
      endif()
   endif()

# Win32 specific global directives
else()

   # be large address aware
   foreach(TYPE EXE MODULE)
      set(CMAKE_${TYPE}_LINKER_FLAGS "${CMAKE_${TYPE}_LINKER_FLAGS} /largeaddressaware")
   endforeach()

   # increase stack size to 16mb
   set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} /stack:0x1000000")

   # allow for large number of sections
   set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} /bigobj")
   set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} /bigobj")

   # require Windows-7 SP1
   add_definitions(
      -DNOMINMAX
      -DWINVER=0x601
      -D_WIN32_WINNT=0x601
      -D_WIN32_IE=0x601
      -DWIN32_LEAN_AND_MEAN
      -D_WIN64
      -DBOOST_USE_WINDOWS_H)
endif()

# determine whether we should statically link boost. we always do this
# unless we are building a non-packaged build on linux (in which case
# boost dynamic libraries are presumed to be installed on the system ldpath)
if(APPLE OR WIN32 OR RSTUDIO_PACKAGE_BUILD)
   set(Boost_USE_STATIC_LIBS ON)
endif()

# default Boost versions
if (WIN32)
   set(BOOST_VERSION 1.65.1)
else()
   set(BOOST_VERSION 1.63.0)
endif()

# override if necessary
if (RSTUDIO_BOOST_VERSION)
   set(BOOST_VERSION "${RSTUDIO_BOOST_VERSION}")
endif()

# disable Boost Signals deprecation warning
add_definitions(-DBOOST_SIGNALS_NO_DEPRECATION_WARNING)

list(APPEND BOOST_LIBS
   atomic
   chrono
   date_time
   filesystem
   iostreams
   program_options
   random
   regex
   signals
   system
   thread
)

# UNIX BOOST
if(UNIX)
   # prefer static link to our custom built version
   set(RSTUDIO_TOOLS_BOOST /opt/rstudio-tools/boost/boost_1_63_0)
   if(NOT RSTUDIO_USE_SYSTEM_BOOST AND EXISTS ${RSTUDIO_TOOLS_BOOST})
      add_definitions(-DRSTUDIO_BOOST_NAMESPACE=rstudio_boost)

      # find headers
      set(Boost_USE_STATIC_LIBS ON)
      set(BOOST_INCLUDEDIR  ${RSTUDIO_TOOLS_BOOST}/include)
      find_package(Boost ${BOOST_VERSION} REQUIRED)

      # define library list manually (find_package doesn't always pick them up)
      set(BOOST_LIB_DIR ${RSTUDIO_TOOLS_BOOST}/lib)
      foreach(BOOST_LIB ${BOOST_LIBS})
         list(APPEND Boost_LIBRARIES ${BOOST_LIB_DIR}/libboost_${BOOST_LIB}.a)
      endforeach()
      message(STATUS "Using RStudio-provided Boost ${BOOST_VERSION}")
   else()
      add_definitions(-DRSTUDIO_BOOST_NAMESPACE=boost)
      find_package(Boost ${BOOST_VERSION} REQUIRED COMPONENTS ${BOOST_LIBS})
      message(STATUS "Using system Boost ${BOOST_VERSION}")
   endif()

   # WIN32 BOOST
else()
   # hard-code to our own prebuilt boost libs
   add_definitions(-DRSTUDIO_BOOST_NAMESPACE=rstudio_boost)
   set(BOOST_ARCH "64")
   if(NOT CMAKE_BUILD_TYPE STREQUAL "Debug")
      set(BOOST_SUFFIX "vc141-mt-1_65_1.lib")
      set(BOOST_ROOT "${RSTUDIO_WINDOWS_DEPENDENCIES_DIR}/boost-1.65.1-win-msvc141-release-static/boost${BOOST_ARCH}")
   else()
      set(BOOST_SUFFIX "vc141-mt-gd-1_65_1.lib")
      set(BOOST_ROOT "${RSTUDIO_WINDOWS_DEPENDENCIES_DIR}/boost-1.65.1-win-msvc141-debug-static/boost${BOOST_ARCH}")
   endif()
   set(BOOST_INCLUDEDIR "${BOOST_ROOT}/include/boost-1_65_1")
   find_package(Boost ${BOOST_VERSION} REQUIRED)
   set(BOOST_LIBRARYDIR "${BOOST_ROOT}/lib")
   foreach(BOOST_LIB ${BOOST_LIBS})
      list(APPEND Boost_LIBRARIES "${BOOST_LIBRARYDIR}/librstudio_boost_${BOOST_LIB}-${BOOST_SUFFIX}")
   endforeach()
   message(STATUS "Using RStudio-provided Boost ${BOOST_VERSION}: ${BOOST_ROOT}")
endif()


# add boost as system include directory
include_directories(SYSTEM ${Boost_INCLUDE_DIRS})

if(WIN32)
     # winpty - pseudoterminal support on Windows
     set(WINPTY_ARCH "64")
     set(WINPTY_ROOT "${RSTUDIO_WINDOWS_DEPENDENCIES_DIR}/winpty-0.4.3-msys2-2.7.0")
     set(WINPTY_INCLUDEDIR "${WINPTY_ROOT}/${WINPTY_ARCH}/include")
     set(WINPTY_BINDIR_64 "${WINPTY_ROOT}/64/bin")

     # add winpty as system include directory
     include_directories(SYSTEM ${WINPTY_INCLUDEDIR})

     # openssl for Windows
     if(CMAKE_BUILD_TYPE STREQUAL "Debug")
        set(OPENSSL_BUILD_TYPE "debug")
     else()
        set(OPENSSL_BUILD_TYPE "release")
     endif()

     set(OPENSSL_BITNESS "64")
     set(OPENSSL_NAME "openssl-1.0.2m")
     set(OPENSSL_ROOT_DIR "${RSTUDIO_WINDOWS_DEPENDENCIES_DIR}/${OPENSSL_NAME}/${OPENSSL_NAME}-${OPENSSL_BUILD_TYPE}-${OPENSSL_BITNESS}")
     set(OPENSSL_INCLUDE_DIR "${OPENSSL_ROOT_DIR}/include")
     set(OPENSSL_LIBRARY_DIR "${OPENSSL_ROOT_DIR}/lib")
     set(OPENSSL_CRYPTO_LIBRARY "${OPENSSL_LIBRARY_DIR}/libeay32.lib")
     set(OPENSSL_SSL_LIBRARY "${OPENSSL_LIBRARY_DIR}/ssleay32.lib")
     list(APPEND OPENSSL_LIBRARIES "${OPENSSL_CRYPTO_LIBRARY}")
     list(APPEND OPENSSL_LIBRARIES "${OPENSSL_SSL_LIBRARY}")

     include_directories(SYSTEM ${OPENSSL_INCLUDE_DIR})
endif()

# automatically build addins found in the addins subdirectory
# (if another path wasn't already specified)
if(NOT RSTUDIO_ADDINS_PATH)
   set(RSTUDIO_DEFAULT_ADDINS_PATH "${CMAKE_CURRENT_SOURCE_DIR}/addins")
   if(EXISTS ${RSTUDIO_DEFAULT_ADDINS_PATH})
      set(RSTUDIO_ADDINS_PATH ${RSTUDIO_DEFAULT_ADDINS_PATH})
   endif()
endif()

# core library
add_subdirectory(core)

# server core library
if (RSTUDIO_SERVER)
   add_subdirectory(server_core)
endif()

# external libraries
add_subdirectory(ext)

# echo configuration modes
if(RSTUDIO_DEVELOPMENT)
   message(STATUS "Configured to build DEVELOPMENT")
endif()
if(RSTUDIO_SERVER)
   message(STATUS "Configured to build SERVER")
endif()
if (RSTUDIO_DESKTOP)
   message(STATUS "Configured to build DESKTOP")
endif()
if(RSTUDIO_CONFIG_CORE_DEV)
   message(STATUS "Configured to build CORE_DEV")
endif()
if(RSTUDIO_CONFIG_MONITOR_ONLY)
   message(STATUS "Configured to build MONITOR_ONLY")
endif()

# are we in CORE_DEV mode? if so then just add the core/dev project
# otherwise, add the rest of our projects
if(RSTUDIO_CONFIG_CORE_DEV)

   add_subdirectory(core/dev)

else()

   # monitor library
   add_subdirectory(monitor)

   # add overlay if it exists
   if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/CMakeOverlay.txt")
      include(CMakeOverlay.txt)
   endif()

   # proceed if we aren't building rstudio monitor-only
   if(NOT RSTUDIO_CONFIG_MONITOR_ONLY)

      # find LibR
      find_package(LibR REQUIRED)

      # verify we got the required R version
      if(LIBR_FOUND AND RSTUDIO_VERIFY_R_VERSION)
         set(CMAKE_REQUIRED_INCLUDES ${LIBR_INCLUDE_DIRS})
         execute_process(
             COMMAND "${LIBR_EXECUTABLE}" "--vanilla" "--slave" "-e" "cat(as.character(getRversion()))"
             OUTPUT_VARIABLE LIBR_VERSION)
         if (LIBR_VERSION VERSION_LESS "3.0.1")
            message(FATAL_ERROR "Minimum R version (${RSTUDIO_R_MAJOR_VERSION_REQUIRED}.${RSTUDIO_R_MINOR_VERSION_REQUIRED}.${RSTUDIO_R_PATCH_VERSION_REQUIRED}) not found.")
         endif()
      endif()

      # r library
      add_subdirectory(r)

      # initialize subdirectories
      file(MAKE_DIRECTORY conf)

      # test runner script
      if(NOT WIN32)
         configure_file(rstudio-tests.in ${CMAKE_CURRENT_BINARY_DIR}/rstudio-tests)
      endif()

      # add desktop subprojects if we aren't building in server only mode
      if(RSTUDIO_DESKTOP)
         add_subdirectory(diagnostics)
         add_subdirectory(desktop)
         configure_file(conf/rdesktop-dev.conf ${CMAKE_CURRENT_BINARY_DIR}/conf/rdesktop-dev.conf)
         if(NOT WIN32)
            configure_file(rdesktop-dev.in ${CMAKE_CURRENT_BINARY_DIR}/rdesktop-dev)
            if(NOT APPLE)
               configure_file(rstudio-dev.in ${CMAKE_CURRENT_BINARY_DIR}/rstudio-dev)
            else()
               configure_file(rstudio-mac.in ${CMAKE_CURRENT_BINARY_DIR}/rstudio-dev)
            endif()
         else()
            configure_file(rstudio.bat.in ${CMAKE_CURRENT_BINARY_DIR}/rstudio.bat @ONLY)
            configure_file(rstudio-tests.bat.in ${CMAKE_CURRENT_BINARY_DIR}/rstudio-tests.bat @ONLY)
         endif()
      endif()

      # add this after desktop so it is not included in fixup_bundle
      # processing which we do in desktop
      add_subdirectory(session)

      # add server subprojects if we aren't building in desktop only mode
      if(RSTUDIO_SERVER)
         add_subdirectory(server)
         configure_file(rserver-dev.in ${CMAKE_CURRENT_BINARY_DIR}/rserver-dev)
         configure_file(conf/rserver-dev.conf ${CMAKE_CURRENT_BINARY_DIR}/conf/rserver-dev.conf)
         configure_file(conf/rsession-dev.conf ${CMAKE_CURRENT_BINARY_DIR}/conf/rsession-dev.conf)

      endif()
   endif()

endif()

