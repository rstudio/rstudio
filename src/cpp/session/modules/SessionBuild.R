#
# SessionBuild.R
#
# Copyright (C) 2009-12 by RStudio, Inc.
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

setHook("sourceCpp.onBuild", function(file, fromCode, showOutput) {
   .Call(.rs.routines$rs_sourceCppOnBuild, file, fromCode, showOutput)
})

setHook("sourceCpp.onBuildComplete", function(succeeded, output) {
   .Call(.rs.routines$rs_sourceCppOnBuildComplete, succeeded, output)
})

.rs.addFunction("installBuildTools", function(action) {
   response <- .rs.userPrompt(
      "question",
      "Install Build Tools",
      paste(action, " requires installation of additional build tools.\n\n",
      "Do you want to install the additional tools now?", sep = ""))
   if (identical(response, "yes")) {
      .Call(.rs.routines$rs_installBuildTools)
      return(TRUE)
   } else {
      return(FALSE)
   }
})

.rs.addFunction("checkBuildTools", function(action) {

   if (identical(.Platform$pkgType, "mac.binary.mavericks")) {
      # this will auto-prompt to install on mavericks
      .Call(.rs.routines$rs_canBuildCpp)
   } else {
      if (!.Call("rs_canBuildCpp")) {
        .rs.installBuildTools(action)
        FALSE
      }
      else {
        TRUE;
      }
   }
})

.rs.addFunction("withBuildTools", function(code) {
    .rs.addRToolsToPath()
    on.exit(.rs.restorePreviousPath(), add = TRUE)
    force(code)
})

options(buildtools.check = .rs.checkBuildTools)
options(buildtools.with = .rs.withBuildTools)






