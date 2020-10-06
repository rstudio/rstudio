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

.rs.addJsonRpcHandler("python_active_interpreter", function()
{
   pythonPath <- Sys.getenv("RETICULATE_PYTHON", unset = NA)
   
   info <- if (is.na(pythonPath))
   {
      .rs.python.invalidInterpreter(
         path = pythonPath,
         type = NULL,
         reason = "RETICULATE_PYTHON is unset."
      )
   }
   else
   {
      .rs.python.getPythonInfo(pythonPath, strict = TRUE)
   }
   
   .rs.scalarListFromList(info)
})

.rs.addJsonRpcHandler("python_find_interpreters", function()
{
   .rs.python.findPythonInterpreters()
})

.rs.addJsonRpcHandler("python_interpreter_info", function(pythonPath)
{
   .rs.python.describeInterpreter(pythonPath)
})

.rs.addFunction("python.execute", function(python, code)
{
   python <- normalizePath(python, winslash = "/", mustWork = TRUE)
   
   args <- c("-E", "-c", shQuote(code))
   result <- suppressWarnings(
      system2(python, args, stdout = TRUE, stderr = TRUE)
   )
   
   # propagate failure as error
   status <- .rs.nullCoalesce(attr(result, "status", exact = TRUE), 0L)
   if (!identical(status, 0L))
      .rs.stopf("error retrieving Python version [error code %i]", status)
   
   paste(result, collapse = "\n")
})

.rs.addFunction("python.getPythonVersion", function(pythonPath)
{
   .rs.python.execute(pythonPath, "import platform; print(platform.python_version())")
})

.rs.addFunction("python.getPythonDescription", function(pythonPath)
{
   .rs.python.execute(pythonPath, "import sys; print(sys.version)")
})

.rs.addFunction("python.getPythonInfo", function(path, strict)
{
   # default to concluding python binary path == requested path
   pythonPath <- path
   
   # if this is the path to an existing file, use the directory path
   info <- file.info(path, extra_cols = FALSE)
   if (identical(info$isdir, FALSE))
      path <- dirname(path)
   
   # if this is the path to the 'root' of an environment,
   # start from the associated bin / scripts directory
   binExt <- if (.rs.platform.isWindows) "Scripts" else "bin"
   binPath <- file.path(path, binExt)
   if (file.exists(binPath))
      path <- binPath
      
   # now form the python path
   if (!strict)
   {
      exe <- if (.rs.platform.isWindows) "python.exe" else "python"
      pythonPath <- file.path(path, exe)
   }
   
   # if this python doesn't exist, bail
   if (!file.exists(pythonPath))
   {
      info <- .rs.python.invalidInterpreter(
         path = pythonPath,
         type = NULL,
         reason = "There is no Python interpreter available at this location."
      )
      
      return(info)
   }
   
   # check for conda environment
   # (look for folders normally seen in conda installations)
   condaFiles <- c("../conda-meta", "../condabin")
   condaPaths <- file.path(path, condaFiles)
   if (any(file.exists(condaPaths)))
      return(.rs.python.interpreterInfo(pythonPath, "conda"))
 
   # check for virtual environment
   # (look for files normally seen in virtual envs)
   venvFiles <- c("activate", "pyvenv.cfg", "../pyvenv.cfg")
   venvPaths <- file.path(path, venvFiles)
   if (any(file.exists(venvPaths)))
      return(.rs.python.interpreterInfo(pythonPath, "virtualenv"))
   
   # default to assuming a system interpreter
   .rs.python.interpreterInfo(pythonPath, "system")
   
})

.rs.addFunction("python.interpreterInfo", function(path, type)
{
   # defaults for version, description
   valid <- TRUE
   version <- "[unknown]"
   description <- "[unknown]"
   
   # determine interpreter version
   version <- tryCatch(
      .rs.python.getPythonVersion(path),
      error = function(e) {
         valid <<- FALSE
         conditionMessage(e)
      }
   )
   
   # determine interpreter description
   description <- tryCatch(
      .rs.python.getPythonDescription(path),
      error = function(e) {
         valid <<- FALSE
         conditionMessage(e)
      }
   )
   
   list(
      path        = .rs.createAliasedPath(path),
      type        = type,
      version     = version,
      description = description,
      valid       = valid,
      reason      = NULL
   )
})

.rs.addFunction("python.invalidInterpreter", function(path, type, reason)
{
   list(
      path        = .rs.createAliasedPath(path),
      type        = type,
      version     = NULL,
      description = NULL,
      valid       = FALSE,
      reason      = reason
   )
})

.rs.addFunction("python.findPythonInterpreters", function()
{
   interpreters <- c(
      .rs.python.findPythonSystemInterpreters(),
      .rs.python.findPythonVirtualEnvironments(),
      .rs.python.findPythonCondaEnvironments()
   )
   
   default <- Sys.getenv("RETICULATE_PYTHON", unset = "")
   
   list(
      python_interpreters = .rs.scalarListFromList(interpreters),
      default_interpreter = .rs.scalar(default)
   )
   
})

.rs.addFunction("python.findPythonSystemInterpreters", function()
{
   interpreters <- list()
   
   # look for interpreters on the PATH
   paths <- strsplit(Sys.getenv("PATH"), split = .Platform$path.sep, fixed = TRUE)[[1]]
   for (path in paths) {
      
      # create pattern matching interpreter paths
      pattern <- if (.rs.platform.isWindows)
         "^python[[:digit:].]*exe$"
      else
         "^python[[:digit:].]*$"
      
      # look for python installations
      pythons <- list.files(
         path       = path,
         pattern    = pattern,
         full.names = TRUE
      )
      
      # loop over interpreters and add
      for (python in pythons) {
         info <- .rs.python.getPythonInfo(python, strict = TRUE)
         interpreters[[length(interpreters) + 1]] <- info
      }
      
   }
   
   interpreters
   
})

.rs.addFunction("python.findPythonCondaEnvironments", function()
{
   envs <- tryCatch(
      reticulate::conda_list(),
      error = identity
   )
   
   if (inherits(envs, "error"))
      return(list())
   
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
   exeSuffix <- if (.rs.platform.isWindows)
      "Scripts/python.exe"
   else
      "bin/python"
   
   # form executable path (ensure it exists)
   exePath <- file.path(envPath, exeSuffix)
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
   
   .rs.python.interpreterInfo(
      path = exePath,
      type = "virtualenv"
   )
   
})

.rs.addFunction("python.describeInterpreter", function(pythonPath)
{
   info <- .rs.python.getPythonInfo(pythonPath, strict = TRUE)
   .rs.scalarListFromList(info)
})

.rs.registerPackageLoadHook("reticulate", function(...)
{
   python <- .rs.readUiPref("python_path")
   .rs.reticulate.usePython(python)
})
