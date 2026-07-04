#
# windows-checks.cmake
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
# Up-front validation of the Windows build environment. These checks turn the
# common toolchain misconfigurations into a single, actionable failure during
# CMake configure -- instead of a cryptic MSVC/linker error deep in the build.
#
# Included from the top-level CMakeLists.txt after globals.cmake, so that
# RSTUDIO_SESSION_WIN32 (and the other RSTUDIO_* variables) are already set.
#

if(NOT WIN32)
   return()
endif()

# 1. Compiler must be MSVC (cl.exe). The boost/SOCI prebuilts are keyed to
#    MSVC_TOOLSET_VERSION, which only MSVC sets; a stray Clang/GCC on the PATH
#    otherwise gets picked up and yields a blank msvc<toolset> dependency path.
if(NOT MSVC)
   message(FATAL_ERROR
      "A non-MSVC compiler was detected (${CMAKE_CXX_COMPILER}), but the "
      "RStudio Windows build requires the MSVC (cl.exe) toolset.\n"
      "Configure from a Visual Studio developer command prompt, or run "
      "src/cpp/tools/windows-dev.cmd first, so cl.exe is on the PATH.")
endif()

# 2. The toolset must match the Boost/SOCI prebuilts, which are pinned to
#    MSVC 14.5 (Visual Studio 2026) by install-dependencies.cmd. Failing here
#    with the real requirement beats the downstream "prebuilts not found"
#    error, whose advice (re-run install-dependencies.cmd) cannot help a
#    VS 2022 user: that script only ever installs msvc145 prebuilts.
#    Keep this pin in sync with dependencies/windows/install-dependencies.cmd.
if(NOT MSVC_TOOLSET_VERSION EQUAL 145)
   message(FATAL_ERROR
      "The RStudio Windows build requires the MSVC 14.5 toolset (Visual "
      "Studio 2026), but toolset ${MSVC_TOOLSET_VERSION} was detected.\n"
      "Install Visual Studio 2026 (see dependencies/windows/"
      "Install-RStudio-Prereqs.ps1) and configure from its developer "
      "command prompt.")
endif()

# 3. Compiler architecture must match the target. The default target is 64-bit
#    (we add -D_WIN64 / -D_AMD64_ and link boost64); the SessionWin32 target is
#    32-bit. A mismatch produces errors like
#    "'size_t': redefinition; different basic types".
if(RSTUDIO_SESSION_WIN32 AND NOT CMAKE_SIZEOF_VOID_P EQUAL 4)
   message(FATAL_ERROR
      "The SessionWin32 target needs a 32-bit (x86) MSVC compiler, but a "
      "${CMAKE_SIZEOF_VOID_P}-pointer-byte compiler was detected.\n"
      "Use the 'x86 Native Tools Command Prompt for VS'.")
elseif(NOT RSTUDIO_SESSION_WIN32 AND NOT CMAKE_SIZEOF_VOID_P EQUAL 8)
   message(FATAL_ERROR
      "The Windows build needs a 64-bit (x64) MSVC compiler, but a "
      "${CMAKE_SIZEOF_VOID_P}-pointer-byte compiler was detected.\n"
      "Use the 'x64 Native Tools Command Prompt for VS 2026', or run "
      "src/cpp/tools/windows-dev.cmd (which sets -arch=amd64).")
endif()

# 4. Ninja is the supported, CI-tested generator. The Visual Studio multi-config
#    generator can link (see the archive-output pinning in src/cpp/ext), but its
#    staging/packaging/test layout is not exercised, so warn rather than fail.
if(NOT CMAKE_GENERATOR MATCHES "Ninja")
   message(WARNING
      "Generator '${CMAKE_GENERATOR}' is not the supported Windows generator. "
      "RStudio's Windows build is tested only with Ninja; reconfigure with "
      "-G Ninja if you hit staging, packaging, or test-layout issues.")
endif()
