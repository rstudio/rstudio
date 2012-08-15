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

project (R_GRAPHICS_HANDLER)

# include files
file(GLOB_RECURSE R_GRAPHICS_HANDLER_HEADER_FILES "*.h*")


# shared source files
set(R_GRAPHICS_HANDLER_SOURCE_FILES
   RGraphicsDevDesc.cpp
   RGraphicsHandler.cpp
   RShadowPngGraphicsHandler.cpp
)

# include directories
include_directories(
   ${CORE_SOURCE_DIR}/include
   ${R_SOURCE_DIR}/include
   ${LIBR_INCLUDE_DIRS}
)

# define library
add_library(rstudio-r-graphics-handler STATIC 
   ${R_GRAPHICS_HANDLER_SOURCE_FILES} 
   ${R_GRAPHICS_HANDLER_HEADER_FILES})

# link dependencies
target_link_libraries(rstudio-r-graphics-handler
   ${LIBR_LIBRARIES}
   rstudio-core
)
