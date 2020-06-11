#
# SessionPythonEnvironments.R
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

.rs.addJsonRpcHandler("python_find_interpreters", function()
{
   .rs.python.findPythonInterpreters()
})

.rs.addFunction("python.getPythonVersion", function(pythonPath, defaultOnError = "[unknown]")
{
   tryCatch(
      .rs.python.getPythonVersionImpl(pythonPath),
      error = function(e) defaultOnError
   )
})

.rs.addFunction("python.getPythonVersionImpl", function(pythonPath)
{
   # build command
   code <- "import platform; print(platform.python_version())"
   args <- c("-E", "-c", shQuote(code))
   
   # run it (suppress warnings as system2 will warn on error)
   result <- suppressWarnings(
      system2(pythonPath, args, stdout = TRUE, stderr = TRUE)
   )
   
   # propagate failure as error
   status <- .rs.nullCoalesce(attr(result, "status", exact = TRUE), 0L)
   
   if (!identical(status, 0L))
      .rs.stopf("error retrieving Python version [error code %i]", status)
   
   result
})

.rs.addFunction("python.interpreterInfo", function(path, type, version = NULL)
{
   version <- .rs.nullCoalesce(version, .rs.python.getPythonVersion(path))
   
   .rs.scalarListFromList(list(
      path    = .rs.createAliasedPath(path),
      type    = type,
      version = version,
      valid   = TRUE,
      reason  = NULL
   ))
})

.rs.addFunction("python.invalidInterpreter", function(path, type, reason)
{
   .rs.scalarListFromList(list(
      path    = .rs.createAliasedPath(path),
      type    = type,
      version = NULL,
      valid   = FALSE,
      reason  = reason
   ))
})

.rs.addFunction("python.findPythonInterpreters", function()
{
   c(
      .rs.python.findPythonSystemInterpreters(),
      .rs.python.findPythonVirtualEnvironments(),
      .rs.python.findPythonCondaEnvironments()
   )
})

.rs.addFunction("python.findPythonSystemInterpreters", function()
{
   interpreters <- list()
   
   # look for interpreters on the PATH
   paths <- strsplit(Sys.getenv("PATH"), split = .Platform$path.sep, fixed = TRUE)[[1]]
   for (path in paths) {
      
      pythonPath <- file.path(path, "python")
      if (!file.exists(pythonPath))
         next
      
      info <- .rs.python.interpreterInfo(
         path = pythonPath,
         type = "system",
      )
      
      interpreters[[length(interpreters) + 1]] <- info
      
   }
   
   interpreters
   
})

.rs.addFunction("python.findPythonEnvironments", function()
{
   c(
      .rs.python.findPythonVirtualEnvironments(),
      .rs.python.findPythonCondaEnvironments()
   )
})

.rs.addFunction("python.findPythonCondaEnvironments", function()
{
   envs <- reticulate::conda_list()
   lapply(envs$python, .rs.python.getCondaEnvironmentInfo)
})

.rs.addFunction("python.getCondaEnvironmentInfo", function(pythonPath)
{
   .rs.python.interpreterInfo(
      path = pythonPath,
      type = "conda"
   )
})

.rs.addFunction("python.findPythonVirtualEnvironments", function()
{
   home <- Sys.getenv("WORKON_HOME", unset = "~/.virtualenvs")
   roots <- list.files(home, full.names = TRUE)
   lapply(roots, .rs.python.getVirtualEnvironmentInfo)
})

.rs.addFunction("python.getVirtualEnvironmentInfo", function(envPath)
{
   # form path to Python executable from root
   exeSuffix <- if (Sys.info()[["sysname"]] == "Windows")
      "Scripts/python.exe"
   else
      "bin/python"
   
   # form executable path (ensure it exists)
   exePath <- path.expand(file.path(envPath, exeSuffix))
   if (!file.exists(exePath))
   {
      fmt <- "Python executable '%s' does not exist."
      reason <- sprintf(fmt, exePath)
      return(.rs.python.invalidInterpreter(
         path   = exePath,
         type   = "virtualenv",
         reason = reason
      ))
   }
   
   # if this Python environment has a pyvenv.cfg file, then we can try
   # and use that to determine the version of Python used for this environment
   pyvenvPath <- file.path(envPath, "pyvenv.cfg")
   if (file.exists(pyvenvPath))
   {
      # read config file
      contents <- readLines(pyvenvPath, warn = FALSE)
      
      # find version line
      versionLine <- grep("^version\\s*=", contents, value = TRUE, perl = TRUE)
      
      # trim off version prefix
      version <- gsub("^version\\s*=\\s*", "", versionLine)
      
      # build environment info object
      info <- .rs.python.interpreterInfo(
         path    = exePath,
         type    = "virtualenv",
         version = version
      )
      
      # return it
      return(info)
   }
   
   # virtual environments created by the virtualenv module
   # won't have a .cfg file, so glean the required information from the
   # python executable itself
   .rs.python.interpreterInfo(
      path = exePath,
      type = "virtualenv"
   )
   
})
