#
# test-prefs
#
# Copyright (C) 2009-19 by RStudio, Inc.
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

