#
# CMakeLists.txt
#
# Copyright (C) 2022 by Posit Software, PBC
#
# This program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

# set minimum version
cmake_minimum_required(VERSION 3.4.3)

# don't add electron for development mode (since faster to work
# iteratively using "yarn start" and so forth)
if(RSTUDIO_ELECTRON AND NOT RSTUDIO_DEVELOPMENT)
   add_subdirectory(desktop)
endif()
