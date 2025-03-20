#
# compiler.cmake
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

# include guard
if(RSTUDIO_CMAKE_COMPILER_INCLUDED)
   return()
endif()
set(RSTUDIO_CMAKE_COMPILER_INCLUDED YES)

# require position independent code for CMake targets
set(CMAKE_POSITION_INDEPENDENT_CODE Yes)

# use C++17
set(CMAKE_CXX_STANDARD 17)

# use clang on osx
if(APPLE)

  if(NOT DEFINED CMAKE_C_COMPILER)
    set(CMAKE_C_COMPILER /usr/bin/cc)
  endif()

  if(NOT DEFINED CMAKE_CXX_COMPILER)
    set(CMAKE_CXX_COMPILER /usr/bin/c++)
  endif()

  if(NOT CMAKE_OSX_DEPLOYMENT_TARGET)
    set(RSTUDIO_TOOLS_SCRIPT "${RSTUDIO_PROJECT_ROOT}/dependencies/tools/rstudio-tools.sh")
    execute_process(
      COMMAND bash -c "source ${RSTUDIO_TOOLS_SCRIPT} && echo $MACOSX_DEPLOYMENT_TARGET"
      OUTPUT_VARIABLE MACOSX_DEPLOYMENT_TARGET
      OUTPUT_STRIP_TRAILING_WHITESPACE
      COMMAND_ERROR_IS_FATAL ANY)
    set(CMAKE_OSX_DEPLOYMENT_TARGET "${MACOSX_DEPLOYMENT_TARGET}")
  endif()

endif()

if(MSVC)

  # keep some deprecated tools around
  add_definitions(-D_HAS_AUTO_PTR_ETC=1 -D_SILENCE_ALL_CXX17_DEPRECATION_WARNINGS=1)

  # assume sources are utf-8
  set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} /utf-8")

  # disable C4800 warning; this is very noisy, rarely useful, and was completely removed
  # in Visual Studio 2017 (we're currently using VS 2015).
  set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} /wd4800")

  # disable C4091 warning: 'keyword' : ignored on left of 'type' when no variable is declared
  # generates a lot of warning noise in files from clang
  set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} /wd4091")

  # disable C4068 warning: unknown pragma
  # these warnings are being triggered in the MSVC-supplied headers and we aren't touching those
  set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} /wd4068")

  # disable C4003 warning: not enough arguments for function-like macro invocation
  # C99, C++11 and above allow you to call function-like macros which accept 1 argument
  # without any parameters, since macro arguments are allowed to be empty
  set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} /wd4003")

  # embed debug information into the generated objects
  # (otherwise we can run into annoying PDB errors during compilation)
  string(REGEX REPLACE "/Zi" "/Z7" CMAKE_C_FLAGS_DEBUG "${CMAKE_C_FLAGS_DEBUG}")
  string(REGEX REPLACE "/Zi" "/Z7" CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG}")

  string(REGEX REPLACE "/Zi" "/Z7" CMAKE_C_FLAGS_RELWITHDEBINFO "${CMAKE_C_FLAGS_RELWITHDEBINFO}")
  string(REGEX REPLACE "/Zi" "/Z7" CMAKE_CXX_FLAGS_RELWITHDEBINFO "${CMAKE_CXX_FLAGS_RELWITHDEBINFO}")

  # ensure that we're using linker flags compatible with
  # the version of Boost that will be linked in
  if(NOT CMAKE_BUILD_TYPE STREQUAL "Debug")
     set(ITERATOR_DEBUG_LEVEL 0)
     set(LINKER_FLAG "/MD")
  else()
     set(ITERATOR_DEBUG_LEVEL 2)
     add_definitions(-D_DEBUG)
     set(LINKER_FLAG "/MDd")
  endif()

  foreach(RELEASE_TYPE "" "_DEBUG" "_RELEASE" "_MINSIZEREL" "_RELWITHDEBINFO")
    foreach(FLAG CMAKE_C_FLAGS CMAKE_CXX_FLAGS)
      string(REGEX REPLACE "/MDd?" "${LINKER_FLAG}" ${FLAG}${RELEASE_TYPE} "${${FLAG}${RELEASE_TYPE}}")
    endforeach()
  endforeach()

  # disable CMake's automatic manifest generation (we always provide our own)
  foreach(TYPE EXE MODULE SHARED)
    set(CMAKE_${TYPE}_LINKER_FLAGS "${CMAKE_${TYPE}_LINKER_FLAGS} /MANIFEST:NO")
  endforeach()

  # multi-process compilation
  set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} /MP")

  # incremental linking
  set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} /INCREMENTAL")

  # silence some warnings (mostly out of our control) + set debug level
  add_definitions(
    -D_CRT_NONSTDC_NO_DEPRECATE
    -D_CRT_SECURE_NO_WARNINGS
    -D_SCL_SECURE_NO_WARNINGS
    -D_ITERATOR_DEBUG_LEVEL=${ITERATOR_DEBUG_LEVEL})

   # silence R Complex.h warnings
   add_definitions(-DR_LEGACY_RCOMPLEX)

else()

  # Use --as-needed when RSTUDIO_CONFIG_MONITOR_ONLY
  if(RSTUDIO_CONFIG_MONITOR_ONLY)
      foreach(TYPE EXE MODULE SHARED)
        set(CMAKE_${TYPE}_LINKER_FLAGS "${CMAKE_${TYPE}_LINKER_FLAGS} -Wl,--as-needed -Wl,--no-undefined -Wl,--no-allow-shlib-undefined")
      endforeach()
  endif()

endif()

if(NOT DEFINED WINDRES)
  set(WINDRES windres.exe)
endif()

# avoid colored output (seems unreliable in cmd.exe terminal)
if(WIN32)
  set(CMAKE_COLOR_MAKEFILE OFF)
endif()

