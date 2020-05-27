#
# test-shiny.R
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

context("shiny")

test_that("shiny viewer type set by preference sticks", {
   # read the current value of the preference
   old <- .rs.api.readRStudioPreference("shiny_viewer_type")

   # set the viewer type to external browser (via pref)
   .rs.api.writeRStudioPreference("shiny_viewer_type", "browser")

   # set the viewer type to browser and verify that it was done as we requested
   .rs.setShinyViewerType("browser")
   expect_equal(.rs.getShinyViewerType(), "browser")

   # set the viewer type to external window (via pref)
   .rs.api.writeRStudioPreference("shiny_viewer_type", "window")

   # setting the type via the pref should trigger an update of the underlying R viewer function
   expect_equal(.rs.getShinyViewerType(), "window")

   # restore old value
   .rs.api.writeRStudioPreference("shiny_viewer_type", old)
})

