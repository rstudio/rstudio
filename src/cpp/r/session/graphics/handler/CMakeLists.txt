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

# look for pango cairo on linux
if(UNIX AND NOT APPLE)
   # detect pangocairo
   pkg_check_modules(PANGO_CAIRO pangocairo>=1.14)
   if(PANGO_CAIRO_FOUND)
      set(R_GRAPHICS_HANDLER_SYSTEM_INCLUDE_DIRS ${PANGO_CAIRO_INCLUDE_DIRS})
      set(R_GRAPHICS_HANDLER_SYSTEM_LIBRARIES ${PANGO_CAIRO_LIBRARIES})
      # export system library dirs to the global context so modules
      # which depend on this library can link properly
      set(R_GRAPHICS_HANDLER_SYSTEM_LIBRARY_DIRS ${PANGO_CAIRO_LIBRARY_DIRS} CACHE INTERNAL "")

      # source files
      set(R_GRAPHICS_HANDLER_SOURCE_FILES RCairoGraphicsHandler.cpp)
   endif()
endif()

# shared source files
set(R_GRAPHICS_HANDLER_SOURCE_FILES ${R_GRAPHICS_HANDLER_SOURCE_FILES}
   RGraphicsDevDesc.cpp
   RGraphicsHandler.cpp
   RShadowPngGraphicsHandler.cpp
)

# include directories
include_directories(
   ${R_GRAPHICS_HANDLER_SYSTEM_INCLUDE_DIRS}
   ${Boost_INCLUDE_DIRS}
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
   ${R_GRAPHICS_HANDLER_SYSTEM_LIBRARIES}
   ${LIBR_LIBRARIES}
   rstudio-core
)
