#
# SessionTests.R
#
# Copyright (C) 2009-18 by RStudio, Inc.
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

.rs.addJsonRpcHandler("has_shinytest_results", function(appPath, testName) {

   result <- dir.exists(
      file.path(
         appPath,
         "tests",
         paste(testName, "current", sep = "-")
      )
   )

   .rs.scalar(result)
})
