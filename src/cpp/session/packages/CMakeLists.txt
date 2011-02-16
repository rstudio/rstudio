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

# RStudio package
set(RSTUDIO_OUTPUT ${PACKAGES_LIBRARY_DIR}/RStudio/DESCRIPTION)
file(GLOB_RECURSE RSTUDIO_DEPENDENCIES RStudio/*.*)
add_custom_command(OUTPUT ${RSTUDIO_OUTPUT}
                   DEPENDS ${RSTUDIO_DEPENDENCIES}
                   WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
                   COMMAND R
                   ARGS CMD INSTALL -l ${PACKAGES_LIBRARY_DIR} RStudio)

# add target for building packages
add_custom_target(build_packages ALL
                  DEPENDS ${RSTUDIO_OUTPUT})

# install RStudio package
install(DIRECTORY ${PACKAGES_LIBRARY_DIR}/RStudio
        DESTINATION ${RSTUDIO_INSTALL_SUPPORTING}/R/library)





