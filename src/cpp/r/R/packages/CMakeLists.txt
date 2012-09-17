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

project(PACKAGES)

# set library dir and ensure it exsits
set(PACKAGES_LIBRARY_DIR ${CMAKE_CURRENT_BINARY_DIR}/library)
file(MAKE_DIRECTORY ${PACKAGES_LIBRARY_DIR})

# manipulate package
configure_file(${CMAKE_CURRENT_SOURCE_DIR}/manipulate/DESCRIPTION.in
               ${CMAKE_CURRENT_SOURCE_DIR}/manipulate/DESCRIPTION)

set(MANIPULATE_OUTPUT ${PACKAGES_LIBRARY_DIR}/manipulate/DESCRIPTION)
file(GLOB_RECURSE MANIPULATE_DEPENDENCIES manipulate/*.*)
set_source_files_properties(${MANIPULATE_DEPENDENCIES} PROPERTIES
                            SYMBOLIC TRUE)
add_custom_command(OUTPUT ${MANIPULATE_OUTPUT}
                   DEPENDS ${MANIPULATE_DEPENDENCIES}
                   WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
                   COMMAND R
                   ARGS CMD INSTALL -l ${PACKAGES_LIBRARY_DIR} manipulate)

# rstudio package
configure_file(${CMAKE_CURRENT_SOURCE_DIR}/rstudio/DESCRIPTION.in
               ${CMAKE_CURRENT_SOURCE_DIR}/rstudio/DESCRIPTION)

set(RSTUDIO_OUTPUT ${PACKAGES_LIBRARY_DIR}/rstudio/DESCRIPTION)
file(GLOB_RECURSE RSTUDIO_DEPENDENCIES rstudio/*.*)
set_source_files_properties(${RSTUDIO_DEPENDENCIES} PROPERTIES
                            SYMBOLIC TRUE)
add_custom_command(OUTPUT ${RSTUDIO_OUTPUT}
                   DEPENDS ${RSTUDIO_DEPENDENCIES}
                   WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
                   COMMAND R
                   ARGS CMD INSTALL -l ${PACKAGES_LIBRARY_DIR} rstudio)


# add target for building packages
add_custom_target(build_packages ALL
                  DEPENDS ${MANIPULATE_OUTPUT} ${RSTUDIO_OUTPUT})

# install manipulate package
install(DIRECTORY ${PACKAGES_LIBRARY_DIR}/manipulate
        DESTINATION ${RSTUDIO_INSTALL_SUPPORTING}/R/library)

# install rstudio package
install(DIRECTORY ${PACKAGES_LIBRARY_DIR}/rstudio
        DESTINATION ${RSTUDIO_INSTALL_SUPPORTING}/R/library)





