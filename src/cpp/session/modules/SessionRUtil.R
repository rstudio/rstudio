#
# SessionRUtil.R
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

.rs.addFunction("isNullExternalPointer", function(object)
{
   .Call("rs_isNullExternalPointer", object, PACKAGE = "(embedding)")
})

.rs.addFunction("readIniFile", function(filePath)
{
   as.list(.Call("rs_readIniFile", filePath, PACKAGE = "(embedding)"))
})

.rs.addFunction("runAsyncRProcess", function(
   code,
   workingDir  = getwd(),
   onStarted   = function() {},
   onContinue  = function() {},
   onStdout    = function(output) {},
   onStderr    = function(output) {},
   onCompleted = function(exitStatus) {})
{
   callbacks <- list(
      started   = onStarted,
      continue  = onContinue,
      stdout    = onStdout,
      stderr    = onStderr,
      completed = onCompleted
   )
   
   .Call(
      "rs_runAsyncRProcess",
      as.character(code),
      normalizePath(workingDir, mustWork = TRUE),
      as.list(callbacks),
      PACKAGE = "(embedding)"
   )
})

#' Run an R script in a separate R process, via `system2()`.
#' 
#' Run `code` in a child \R process, launched via `system2()`.
#' 
#' @param callback An \R function, to be executed within the child process.
#'   It should take a single parameter, which represents the `data` to
#'   be supplied to the callback.
#' 
#' @param data An optional list of side-car data, to be referenced from
#'   `code` via the `data` argument. These arguments will be applied to
#'   the callback via `do.call(callback, data)`, and so should normally
#'   be a named list mapping argument names to their appropriate values.
#'   
#' @param workingDir An optional working directory in which `code`
#'   should be run.
#' 
#' @param libPaths The library paths to be set and used by the child process.
#'   By default, the parent's library paths are used.
#'
#' @param ... Optional arguments passed to `system2()`.
#' 
.rs.addFunction("executeFunctionInChildProcess", function(callback, 
                                                          data = list(),
                                                          workingDir = NULL,
                                                          libPaths = .libPaths(),
                                                          ...)
{
   # create and move to directory we'll use to stage our scripts
   scriptDir <- tempfile("rstudio-script-")
   dir.create(scriptDir, recursive = TRUE, showWarnings = FALSE)
   owd <- setwd(scriptDir)
   
   # clean up when we're done
   on.exit({
      setwd(owd)
      unlink(scriptDir, recursive = TRUE)
   }, add = TRUE)
   
   # set R_LIBS so that library paths are propagated to child process
   rlibs <- Sys.getenv("R_LIBS", unset = NA)
   Sys.setenv(R_LIBS = paste(libPaths, collapse = .Platform$path.sep))
   on.exit({
      if (is.na(rlibs))
         Sys.unsetenv("R_LIBS")
      else
         Sys.setenv(R_LIBS = rlibs)
   }, add = TRUE)
   
   # create data bundle powering script
   bundle <- list(
      callback   = callback,
      data       = data,
      workingDir = workingDir
   )
   
   # define runner script (will load data and execute user-defined callback)
   script <- quote({
      
      # read side-car data file
      bundle <- readRDS("bundle.rds")
      
      # move to requested working directory
      workingDir <- bundle[["workingDir"]]
      if (!is.null(workingDir)) {
         dir.create(workingDir, recursive = TRUE, showWarnings = FALSE)
         owd <- setwd(workingDir)
         on.exit(setwd(owd), add = TRUE)
      }
      
      # retrieve callback data
      callback <- bundle[["callback"]]
      data     <- bundle[["data"]]
      
      # execute callback
      do.call(callback, data)
      
   })
   
   # write bundle to file
   # (suppress 'may not be available when loading' warnings)
   suppressWarnings(saveRDS(bundle, file = "bundle.rds"))
   
   # write script to file
   writeLines(deparse(script), con = "script.R")
   
   # form path to R
   exe <- if (Sys.info()[["sysname"]] == "Windows") "R.exe" else "R"
   r <- file.path(R.home("bin"), exe)
   
   # form command line arguments
   args <- c("--vanilla", "-s", "-f", shQuote("script.R"))
   
   # run the script
   system2(r, args, ...)
   
})
