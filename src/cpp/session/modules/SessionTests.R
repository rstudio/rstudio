#
# SessionTests.R
#
# Copyright (C) 2022 by Posit Software, PBC
#
# Unless you have received this program directly from Posit Software pursuant
# to the terms of a commercial license agreement with Posit Software, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

.rs.addJsonRpcHandler("has_shinytest2_results", function(testFile) {

   # Find the Shiny app directory by walking up from the test file.
   # shinytest2 stores tests in: <appDir>/tests/testthat/
   shinyDir <- dirname(testFile)
   if (identical(basename(shinyDir), "testthat")) {
      shinyDir <- dirname(shinyDir)
   }
   if (identical(basename(shinyDir), "tests")) {
      shinyDir <- dirname(shinyDir)
   } else {
      stop("Could not find Shiny app for test file ", testFile)
   }

   # testthat 3 records pending snapshot diffs as files matching '*.new.*'
   # (e.g. snapshot.new.png) under tests/testthat/_snaps/. Their presence is
   # what 'snapshot_review()' will offer to compare.
   snapsDir <- file.path(shinyDir, "tests", "testthat", "_snaps")
   hasPendingDiffs <- length(list.files(
      snapsDir,
      pattern = "\\.new\\.",
      recursive = TRUE)) > 0

   list(
      appDir = .rs.scalar(shinyDir),
      testDirExists = .rs.scalar(hasPendingDiffs))
})
