#
# test-prefs
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

context("prefs")

test_that("preference values can be set", {
   # create a new temporary value for the code completion pref
   oldVal <- .rs.readUiPref("code_completion_delay")
   newVal <- oldVal + 50L

   # set it to a new value and ensure that it sticks
   .rs.writeUiPref("code_completion_delay", newVal)
   expect_equal(.rs.readUiPref("code_completion_delay"), newVal)

   # restore the old value and ensure that it sticks
   .rs.writeUiPref("code_completion_delay", oldVal)
   expect_equal(.rs.readUiPref("code_completion_delay"), oldVal)
})

test_that("default values for preferences are respected", {
   oldVal <- .rs.readUiPref("num_spaces_for_tab")

   # set it to a new value and ensure that it sticks
   newVal <- oldVal + 2L
   .rs.writeUiPref("num_spaces_for_tab", newVal)
   expect_equal(.rs.readUiPref("num_spaces_for_tab"), newVal)

   # remove the value entirely and ensure it falls back to the default (this presumes the default is
   # 2, but we are unlikely to change the historical default of this pref!)
   .rs.removePref("num_spaces_for_tab")
   expect_equal(.rs.readUiPref("num_spaces_for_tab"), 2)

   # restore the old value if it wasn't the default
   if (oldVal != 2) {
      .rs.writeUiPref("num_spaces_for_tab", oldVal)
   }
})

test_that("rstudio API prefs are separate from IDE prefs", {
   # read an old value from the RStudio preference
   oldVal <- .rs.api.readRStudioPreference("code_completion_delay")
   newVal <- oldVal + 50L

   # save the new value as a regular (non-RStudio) pref
   .rs.api.writePreference("code_completion_delay", newVal)

   # ensure that the new value was written correctly
   prefVal <- .rs.api.readPreference("code_completion_delay")
   expect_equal(prefVal, newVal)

   # ensure that the original RStudio preference is untouched
   prefVal <- .rs.api.readRStudioPreference("code_completion_delay")
   expect_equal(prefVal, oldVal)
})
