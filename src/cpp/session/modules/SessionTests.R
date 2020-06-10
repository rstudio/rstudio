#
# SessionTests.R
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

.rs.addJsonRpcHandler("has_shinytest_dependencies", function() {

   if (.rs.isPackageInstalled("shinytest")) {
      if (!exists("dependenciesInstalled", envir = asNamespace("shinytest"))) {
         # assume dependencies installed if shinytest does not provide dependencies state
         .rs.scalar(TRUE)
      } else {
         shinytestDepsInstalled <- get("dependenciesInstalled", envir = asNamespace("shinytest"))()
         .rs.scalar(shinytestDepsInstalled)
      }
   } else {
      .rs.scalar(FALSE)
   }

})

.rs.addJsonRpcHandler("has_shinytest_results", function(testFile) {

   # The test result is stored in a directory alongside the test file
   dirExists <- dir.exists(file.path(
      dirname(testFile), 
      paste(tools::file_path_sans_ext(basename(testFile)), "current", sep = "-")))

   # Find the Shiny app directory
   shinyDir <- dirname(testFile)
   if (identical(basename(shinyDir), "shinytests") ||
       identical(basename(shinyDir), "shinytest")) {
      # Newer versions of shinytest store tests in a "shinytest" or "shinytests" folder (depending
      # on version)
      shinyDir <- dirname(shinyDir)
   } 
   if (identical(basename(shinyDir), "tests")) {
      # Move up from the tests folder to the app folder
      shinyDir <- dirname(shinyDir)
   } else {
      stop("Could not find Shiny app for test file ", testFile)
   }

   # Return the discovered application directory, and whether the test exists 
   list(
      appDir = .rs.scalar(shinyDir),
      testDirExists = .rs.scalar(dirExists))
})
