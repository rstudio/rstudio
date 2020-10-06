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

project (POSTBACK)

# include files
file(GLOB_RECURSE POSTBACK_HEADER_FILES "*.h*")

# source files
set(POSTBACK_SOURCE_FILES
   PostbackMain.cpp
   PostbackOptions.cpp
)

# set include directories
include_directories(
   ${CORE_SOURCE_DIR}/include
   ${SESSION_SOURCE_DIR}/include
)

# define executable
add_stripped_executable(rpostback ${POSTBACK_SOURCE_FILES} ${POSTBACK_HEADER_FILES})

# set link dependencies
target_link_libraries(rpostback
   rstudio-core
   ${CRASHPAD_LIBRARIES}
)

if (RSTUDIO_SERVER)
    target_link_libraries(rpostback
       rstudio-server-core
    )
endif()

# configure postback scripts for development mode
set(POSTBACK_SCRIPT_DIR ${CMAKE_CURRENT_BINARY_DIR}/postback)
file(MAKE_DIRECTORY ${POSTBACK_SCRIPT_DIR})
configure_file(rpostback-editfile ${POSTBACK_SCRIPT_DIR}/rpostback-editfile)
configure_file(rpostback-pdfviewer ${POSTBACK_SCRIPT_DIR}/rpostback-pdfviewer)
configure_file(rpostback-gitssh ${POSTBACK_SCRIPT_DIR}/rpostback-gitssh)
configure_file(rpostback-askpass ${POSTBACK_SCRIPT_DIR}/rpostback-askpass)
configure_file(askpass-passthrough ${POSTBACK_SCRIPT_DIR}/askpass-passthrough)

# put rpostback in a place where it can be found for dev config
if(NOT RSTUDIO_PACKAGE_BUILD)
   set_target_properties(rpostback PROPERTIES RUNTIME_OUTPUT_DIRECTORY ${POSTBACK_SCRIPT_DIR})
endif()

# installation rules
if(NOT RSTUDIO_SESSION_WIN32)
   install(TARGETS rpostback DESTINATION ${RSTUDIO_INSTALL_BIN})
   file(GLOB POSTBACK_SCRIPTS "rpostback-*")
   set(POSTBACK_SCRIPTS ${POSTBACK_SCRIPTS} "askpass-passthrough")
   install(PROGRAMS ${POSTBACK_SCRIPTS}
           DESTINATION ${RSTUDIO_INSTALL_BIN}/postback)
endif()

# if doing a package build on MacOS, reroute dependent libraries to our bundled copies
if(APPLE AND RSTUDIO_PACKAGE_BUILD)

   find_package(OpenSSL REQUIRED QUIET)
   foreach(lib ${OPENSSL_CRYPTO_LIBRARY} ${OPENSSL_SSL_LIBRARY})
      # get the real path to the library (with all symlinks resolved) and filename
      get_filename_component(LIB_PATH "${lib}" REALPATH)
      get_filename_component(LIB_FILE "${LIB_PATH}" NAME)
      # this command does the following:
      # 1. runs otool -L to find the path at which the OpenSSL library is
      #    linked into the executable (this path is unfortunately not canonical
      #    and includes layers of both resolved and unresolved symlinks)
      # 2. extracts the path from the output using grep and awk
      # 3. runs install_name_tool and tells it to replace the path with one that
      #    points to a bundled copy of the same file
      add_custom_command (TARGET rpostback POST_BUILD
         COMMAND install_name_tool -change `otool -L ${CMAKE_CURRENT_BINARY_DIR}/rpostback | grep ${LIB_FILE} | awk '{ print $$1 }'` @executable_path/../Frameworks/${LIB_FILE} ${CMAKE_CURRENT_BINARY_DIR}/rpostback)
   endforeach()

   # do something similar to the above to fix paths to SOCI libraries
   foreach(lib ${SOCI_DEPENDENCIES})
      get_filename_component(LIB_NAME "${lib}" NAME_WE)
      add_custom_command (TARGET rpostback POST_BUILD
          COMMAND install_name_tool -change `otool -L ${CMAKE_CURRENT_BINARY_DIR}/rpostback | grep ${LIB_NAME} | awk '{ print $$1 }'` @executable_path/../Frameworks/$$\(otool -L ${CMAKE_CURRENT_BINARY_DIR}/rpostback | grep ${LIB_NAME} | awk '{ print $$1 }' | xargs basename\) ${CMAKE_CURRENT_BINARY_DIR}/rpostback)
   endforeach()

endif()

