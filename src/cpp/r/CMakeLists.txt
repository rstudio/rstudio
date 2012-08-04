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

project (R)

# custom R packages for RStudio
add_subdirectory(R/packages)

#include graphics handler subproject
add_subdirectory(session/graphics/handler)

# include files
file(GLOB_RECURSE R_HEADER_FILES "*.h*")

# source files
set (R_SOURCE_FILES
   RErrorCategory.cpp
   RExec.cpp
   RFunctionHook.cpp
   RJson.cpp
   RJsonRpc.cpp
   ROptions.cpp
   RRoutines.cpp
   RSexp.cpp
   RSourceManager.cpp
   RUtil.cpp
   session/RClientMetrics.cpp
   session/RClientState.cpp
   session/RConsoleActions.cpp
   session/RConsoleHistory.cpp
   session/RDiscovery.cpp
   session/RRestartContext.cpp
   session/RSearchPath.cpp
   session/RSessionState.cpp
   session/RSession.cpp
   session/graphics/RGraphicsDevice.cpp
   session/graphics/RGraphicsErrorCategory.cpp
   session/graphics/RGraphicsFileDevice.cpp
   session/graphics/RGraphicsPlot.cpp
   session/graphics/RGraphicsPlotManipulator.cpp
   session/graphics/RGraphicsPlotManipulatorManager.cpp
   session/graphics/RGraphicsPlotManager.cpp
   session/graphics/RGraphicsUtils.cpp
)


# UNIX specific
if (UNIX)

   set(R_SOURCE_FILES ${R_SOURCE_FILES}
      session/REmbeddedPosix.cpp
   )

# Win32 specific
else()

   set(R_SOURCE_FILES ${R_SOURCE_FILES}
      session/REmbeddedWin32.cpp
   )

endif()

# create a config file
configure_file(${CMAKE_CURRENT_SOURCE_DIR}/config.h.in
               ${CMAKE_CURRENT_BINARY_DIR}/config.h)

# include directories
include_directories(
   include
   ${CMAKE_CURRENT_BINARY_DIR}
   ${Boost_INCLUDE_DIRS}
   ${CORE_SOURCE_DIR}/include
   ${LIBR_INCLUDE_DIRS}
)

# define library
add_library(rstudio-r STATIC ${R_SOURCE_FILES} ${R_HEADER_FILES})

# link dependencies
target_link_libraries(rstudio-r
   ${LIBR_LIBRARIES}
   rstudio-core
   rstudio-r-graphics-handler
)

# install rules
if (NOT RSTUDIO_SESSION_WIN64)
   file(GLOB R_SRC_FILES "R/*.R")
   install(FILES ${R_SRC_FILES} DESTINATION ${RSTUDIO_INSTALL_SUPPORTING}/R)
endif()
