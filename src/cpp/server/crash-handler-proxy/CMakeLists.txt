#
# CMakeLists.txt
#
# Copyright (C) 2020 by RStudio, PBC
#

project (CRASH_HANDLER_PROXY)

# include files
file(GLOB_RECURSE CRASH_HANDLER_PROXY_HEADER_FILES "*.h*")

# source files
set(CRASH_HANDLER_PROXY_SOURCE_FILES
   CrashHandlerProxyMain.cpp
)

# set include directories
include_directories(
   ${Boost_INCLUDE_DIRS}
   ${CORE_SOURCE_DIR}/include
   ${SHARED_CORE_SOURCE_DIR}/include
)


# define executable
add_stripped_executable(crash-handler-proxy
                        ${CRASH_HANDLER_PROXY_SOURCE_FILES} ${CRASH_HANDLER_PROXY_HEADER_FILES})

# set link dependencies
target_link_libraries(crash-handler-proxy
   rstudio-core
)

# installation rules
install(TARGETS crash-handler-proxy DESTINATION ${RSTUDIO_INSTALL_BIN})

