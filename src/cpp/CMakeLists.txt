#
# CMakeLists.txt
#
# Copyright (C) 2009-12 by RStudio, Inc.
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

# set compiler
include("${CMAKE_CURRENT_SOURCE_DIR}/../../CMakeCompiler.txt")

# set minimum version
cmake_minimum_required(VERSION 2.6)

project (RSTUDIO_CPP)

# include globals. normally these are included at the root but we also
# include them here to support configuring at the src level (i.e. for
# cpp-development-configurations)
include("${CMAKE_CURRENT_SOURCE_DIR}/../../CMakeGlobals.txt")

# global directives
add_definitions(-DBOOST_ENABLE_ASSERT_HANDLER)

# test directory
set(TESTS_INCLUDE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/tests/cpp" CACHE STRING "Test includes")

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
      # Mavericks and later default to libc++, which is not compatible with the
      # older libstdc++. Figure out if we're on Mavericks, and if so, add 
      # flags to enforce usage of the older library.

      # Figure out what version of Mac OS X this is. Unfortunately 
      # CMAKE_SYSTEM_VERSION does not match uname -r, so get the Mac OS
      # version number another way.
      EXECUTE_PROCESS(COMMAND /usr/bin/sw_vers -productVersion OUTPUT_VARIABLE MACOSX_VERSION OUTPUT_STRIP_TRAILING_WHITESPACE)
      message(STATUS "Mac OS X version: ${MACOSX_VERSION}")
      if(RSTUDIO_USE_LIBCXX)
         message(STATUS "C++ Standard Library: libc++")
         set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -stdlib=libc++")
         set(CMAKE_CXX_LINK_FLAGS "${CMAKE_CXX_LINK_FLAGS} -stdlib=libc++")
      elseif(RSTUDIO_USE_LIBSTDCXX)
         message(STATUS "C++ Standard Library: libstdc++")
         set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -stdlib=libstdc++")
         set(CMAKE_CXX_LINK_FLAGS "${CMAKE_CXX_LINK_FLAGS} -stdlib=libstdc++")
      else()
         if(NOT(${MACOSX_VERSION} VERSION_LESS "10.9"))
            message(STATUS "C++ Standard Library: libstdc++")
            set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -stdlib=libstdc++")
            set(CMAKE_CXX_LINK_FLAGS "${CMAKE_CXX_LINK_FLAGS} -stdlib=libstdc++")
         endif()
      endif()
   endif()

   # gcc hardending options (see: http://wiki.debian.org/Hardening)
   if(NOT APPLE)
      add_definitions(-Wformat -Wformat-security)
      add_definitions(-D_FORTIFY_SOURCE=2)
      add_definitions(-fstack-protector --param ssp-buffer-size=4)
      add_definitions(-pie -fPIE)
      set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -Wl,-z,relro,-z,now")
   endif()

# Win32 specific global directives
else()
   add_definitions(-DWINVER=0x501)

   if(RSTUDIO_SESSION_WIN64)

      # increase stack size to 20MB, avoid mingw auto-importing warning,
      set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -Wl,--stack=0x01400000,--enable-auto-import,--allow-multiple-definition")

      add_definitions(-D_WIN64
                      -D_WIN64_WINNT=0x0501
                      -D_WIN64_IE=0x600
                      -DWIN64_LEAN_AND_MEAN
                      -DBOOST_USE_WINDOWS_H)
   else()

      # increase stack size to 20MB, avoid mingw auto-importing warning,
      # and ensure that we are large address aware
      set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -Wl,--stack=0x01400000,--enable-auto-import,--large-address-aware,--allow-multiple-definition")

      add_definitions(-D_WIN32_WINNT=0x0501
                      -D_WIN32_IE=0x600
                      -DWIN32_LEAN_AND_MEAN
                      -DBOOST_USE_WINDOWS_H)
   endif()

endif()

# determine whether we should statically link boost. we always do this
# unless we are building a non-packaged build on linux (in which case
# boost dynamic libraries are presumed to be installed on the system ldpath)
if(APPLE OR WIN32 OR RSTUDIO_PACKAGE_BUILD)
   set(Boost_USE_STATIC_LIBS ON)
endif()

# default to boost 1.63.0
set(BOOST_VERSION 1.63.0)

# override if necessary
if (RSTUDIO_BOOST_VERSION)
   set(BOOST_VERSION "${RSTUDIO_BOOST_VERSION}")
endif()

# disable Boost Signals deprecation warning
add_definitions(-DBOOST_SIGNALS_NO_DEPRECATION_WARNING)

list(APPEND BOOST_LIBS
   date_time
   filesystem
   iostreams
   program_options
   regex
   signals
   system
   thread
   chrono
   random
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
   set(Boost_COMPILER "-mgw49")
   if(RSTUDIO_SESSION_WIN64)
      set(BOOST_ARCH "64")
   else()
      set(BOOST_ARCH "32")
   endif()
   set(BOOST_ROOT "${RSTUDIO_WINDOWS_DEPENDENCIES_DIR}/boost-1.63-win-rtools33-gcc493/boost${BOOST_ARCH}")
   set(BOOST_INCLUDEDIR "${BOOST_ROOT}/include/boost-1_63")
   find_package(Boost ${BOOST_VERSION} REQUIRED)
   set(BOOST_LIBRARYDIR "${BOOST_ROOT}/lib")
   foreach(BOOST_LIB ${BOOST_LIBS})
      list(APPEND Boost_LIBRARIES "${BOOST_LIBRARYDIR}/libboost_${BOOST_LIB}${Boost_COMPILER}-mt-s-1_63.a")
   endforeach()
   message(STATUS "Using RStudio-provided Boost ${BOOST_VERSION}")
endif()


# add boost as system include directory
include_directories(SYSTEM ${Boost_INCLUDE_DIRS})

# winpty - pseudoterminal support on Windows
if(WIN32)
     if(RSTUDIO_SESSION_WIN64)
        set(WINPTY_ARCH "64")
     else()
        set(WINPTY_ARCH "32")
     endif()

     set(WINPTY_ROOT "${RSTUDIO_WINDOWS_DEPENDENCIES_DIR}/winpty-0.4.2-msys2-2.6.0")
     set(WINPTY_INCLUDEDIR "${WINPTY_ROOT}/${WINPTY_ARCH}/include")
     set(WINPTY_BINDIR_32 "${WINPTY_ROOT}/32/bin")
     set(WINPTY_BINDIR_64 "${WINPTY_ROOT}/64/bin")

     # add winpty as system include directory
     include_directories(SYSTEM ${WINPTY_INCLUDEDIR})
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

# external libraries
add_subdirectory(ext)

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
      if(RSTUDIO_SESSION_WIN64)
         set(LIBR_FIND_WINDOWS_64BIT TRUE)
      endif()
      find_package(LibR REQUIRED)

      # verify we got the required R version
      if(LIBR_FOUND AND RSTUDIO_VERIFY_R_VERSION)
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

      # r library
      add_subdirectory(r)

      # initialize subdirectories
      file(MAKE_DIRECTORY conf)

      # test runner script
      configure_file(rstudio-tests.in ${CMAKE_CURRENT_BINARY_DIR}/rstudio-tests)

      # add desktop subprojects if we aren't building in server only mode
      if(RSTUDIO_DESKTOP)
         add_subdirectory(diagnostics)
         if(NOT APPLE)
            add_subdirectory(desktop)
         else()
            add_subdirectory(desktop-mac)
         endif()
         configure_file(rdesktop-dev.in ${CMAKE_CURRENT_BINARY_DIR}/rdesktop-dev)
         configure_file(rstudio-dev.in ${CMAKE_CURRENT_BINARY_DIR}/rstudio-dev)
         configure_file(conf/rdesktop-dev.conf ${CMAKE_CURRENT_BINARY_DIR}/conf/rdesktop-dev.conf)
      endif()

      # add this after desktop so it is not included in fixup_bundle
      # processing which we do in desktop
      add_subdirectory(session)

      # add server subprojects if we aren't building in desktop only mode
      if(RSTUDIO_SERVER)

         add_subdirectory(server)
         configure_file(rserver-dev.in ${CMAKE_CURRENT_BINARY_DIR}/rserver-dev)
         configure_file(rserver-test.in ${CMAKE_CURRENT_BINARY_DIR}/rserver-test)
         configure_file(conf/rserver-dev.conf ${CMAKE_CURRENT_BINARY_DIR}/conf/rserver-dev.conf)
         configure_file(conf/rsession-dev.conf ${CMAKE_CURRENT_BINARY_DIR}/conf/rsession-dev.conf)

      endif()
   endif()

endif()

