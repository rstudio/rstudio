#
# SessionBuild.R
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

setHook("sourceCpp.onBuild", function(file, fromCode, showOutput)
{
   .Call("rs_sourceCppOnBuild", file, fromCode, showOutput, PACKAGE = "(embedding)")
})

setHook("sourceCpp.onBuildComplete", function(succeeded, output)
{
   .Call("rs_sourceCppOnBuildComplete", succeeded, output, PACKAGE = "(embedding)")
})

.rs.addFunction("installBuildTools", function(action)
{
   fmt <- .rs.trimWhitespace("
%s requires installation of additional build tools.

Do you want to install the additional tools now?
")
   
   response <- .rs.userPrompt(
      "question",
      "Install Build Tools",
      sprintf(fmt, action)
   )
   
   if (!identical(response, "yes"))
      return(FALSE)
   
   .Call("rs_installBuildTools", PACKAGE = "(embedding)")
   return(TRUE)
   
})

.rs.addFunction("checkBuildTools", function(action)
{
   # TODO: should this check for other flavors of mac.binary?
   if (identical(.Platform$pkgType, "mac.binary.mavericks"))
      return(.Call("rs_canBuildCpp", PACKAGE = "(embedding)"))
   
   ok <- .Call("rs_canBuildCpp", PACKAGE = "(embedding)")
   if (ok)
      return(TRUE)
   
   .rs.installBuildTools(action)
   FALSE
})

.rs.addFunction("withBuildTools", function(code)
{
    .rs.addRToolsToPath()
    on.exit(.rs.restorePreviousPath(), add = TRUE)
    force(code)
})

options(buildtools.check = function(action)
{
   .rs.checkBuildTools(action)
})

options(buildtools.with = function(code)
{
   .rs.withBuildTools(code)
})

.rs.addFunction("websiteOutputDir", function(siteDir)
{
   siteGenerator <- rmarkdown::site_generator(siteDir)
   if (!is.null(siteGenerator))
      if (siteGenerator$output_dir != ".")
         file.path(siteDir, siteGenerator$output_dir)
      else
         siteDir
   else
      siteDir
})

.rs.addFunction("builtWithRtoolsGcc493", function()
{
   identical(.Platform$OS.type, "windows") &&
      getRversion() >= "3.3" && 
      .rs.haveRequiredRSvnRev(70462)
})

.rs.addFunction("readShinytestResultRds", function(rdsPath)
{
   failures <- Filter(function(e) !identical(e$pass, TRUE), readRDS(rdsPath)$results)
   sapply(failures, function(e) e$name)
})

.rs.addFunction("findShinyTestsDir", function(appDir)
{
   tryCatch(
      
      expr = shinytest:::findTestsDir(
         appDir = appDir,
         mustExit = FALSE,
         quiet = TRUE
      ),
      
      error = function(e) {
         file.path(appDir, "tests")
      }
      
   )
   
})

