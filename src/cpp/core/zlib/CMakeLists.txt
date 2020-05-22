#
# CMakeLists.txt
#
# Copyright (C) 2020 by RStudio, PBC
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

# NOTE: this is an embedded version of zlib (used on Windows only) based
# on zlib 1.2.6 from http://zlib.net/zlib-1.2.6.tar.gz. To update to a new
# version of zlib you can just copy the .h and .c files from the root of
# the zlib distribution (but note that rather than copying zconf.h you should
# copy zconf.h.cmakein so zconf.h can by dynamically configured

project(CORE_ZLIB C)

include(CheckTypeSize)
include(CheckFunctionExists)
include(CheckIncludeFile)
include(CheckCSourceCompiles)

# disable gcc visiblity attribute (not supported by some mingw versinos)
set(EXTRA_COMPILE_DEFINITIONS "NO_VIZ")

if(MSVC)
  # disable noisy warnings; third-party code we try to avoid changing
  # C4013: using undefined function (assumes extern returning int)
  set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} /wd4013")
endif()

# add largefile support if we can
check_type_size(off64_t OFF64_T)
if(HAVE_OFF64_T)
   set(EXTRA_COMPILE_DEFINITIONS
       "${EXTRA_COMPILE_DEFINITIONS}; _LARGEFILE64_SOURCE=1")
endif()


#
# Check for fseeko
#
check_function_exists(fseeko HAVE_FSEEKO)
if(NOT HAVE_FSEEKO)
    add_definitions(-DNO_FSEEKO)
endif()

#
# Check for unistd.h
#
check_include_file(unistd.h Z_HAVE_UNISTD_H)

configure_file(${CMAKE_CURRENT_SOURCE_DIR}/zconf.h.cmakein
               ${CMAKE_CURRENT_SOURCE_DIR}/zconf.h @ONLY)

set(ZLIB_PUBLIC_HDRS
    zconf.h
    zlib.h
)
set(ZLIB_PRIVATE_HDRS
    crc32.h
    deflate.h
    gzguts.h
    inffast.h
    inffixed.h
    inflate.h
    inftrees.h
    trees.h
    zutil.h
)
set(ZLIB_SRCS
    adler32.c
    compress.c
    crc32.c
    deflate.c
    gzclose.c
    gzlib.c
    gzread.c
    gzwrite.c
    inflate.c
    infback.c
    inftrees.c
    inffast.c
    trees.c
    uncompr.c
    zutil.c
)

# parse the full version number from zlib.h and include in ZLIB_FULL_VERSION
file(READ ${CMAKE_CURRENT_SOURCE_DIR}/zlib.h _zlib_h_contents)
string(REGEX REPLACE ".*#define[ \t]+ZLIB_VERSION[ \t]+\"([0-9A-Za-z.]+)\".*"
    "\\1" ZLIB_FULL_VERSION ${_zlib_h_contents})
 

# define library
add_library(rstudio-core-zlib STATIC ${ZLIB_SRCS} ${ZLIB_PUBLIC_HDRS} ${ZLIB_PRIVATE_HDRS})
define_source_file_names(rstudio-core-zlib)

# set extra compile definitions
set_property(TARGET rstudio-core-zlib
             PROPERTY COMPILE_DEFINITIONS ${EXTRA_COMPILE_DEFINITIONS})

# link dependencies
target_link_libraries(rstudio-core-zlib)
