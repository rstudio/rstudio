#
# SessionPythonEnvironments.R
#
# Copyright (C) 2021 by RStudio, PBC
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

.rs.addFunction("python.projectInterpreterPath", function(projectDir)
{
   venvPath <- file.path(projectDir, "env")
   pythonSuffix <- if (.rs.platform.isWindows) "Scripts/python.exe" else "bin/python"
   file.path(venvPath, pythonSuffix)
})

.rs.addFunction("python.initialize", function(projectDir)
{
   # do nothing if the user hasn't opted in
   activate <- .rs.readUiPref("python_project_environment_automatic_activate")
   if (!identical(activate, TRUE))
      return()
   
   # nothing to do if we don't have a project
   if (is.null(projectDir) || !file.exists(projectDir))
      return()
   
   # if the user has set RETICULATE_PYTHON, assume they're taking control
   reticulatePython <- Sys.getenv("RETICULATE_PYTHON", unset = NA)
   if (!is.na(reticulatePython))
      return()
   
   # get path to project interpreter (bail if it doesn't exist)
   pythonPath <- .rs.python.projectInterpreterPath(projectDir)
   if (!file.exists(pythonPath))
      return()
   
   # normalize path (avoid following symlinks)
   pythonPath <- file.path(
      normalizePath(dirname(pythonPath), winslash = "/", mustWork = FALSE),
      basename(pythonPath)
   )
 
   # add the Python directory to the PATH
   oldPath <- Sys.getenv("PATH")
   pythonBin <- normalizePath(dirname(pythonPath))
   newPath <- paste(pythonBin, oldPath, sep = .Platform$path.sep)
   Sys.setenv(PATH = newPath)
   
   # if this is a virtual environment, set VIRTUAL_ENV
   pythonInfo <- .rs.python.getPythonInfo(pythonPath, strict = TRUE)
   if (identical(pythonInfo$type, "virtualenv")) {
      envPath <- dirname(dirname(pythonPath))
      Sys.setenv(VIRTUAL_ENV = envPath)
   }
   
   # also set RETICULATE_PYTHON so this python is used by default
   Sys.setenv(RETICULATE_PYTHON = pythonPath)
   
   # return path to python
   invisible(pythonPath)
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
   # prefer UTF-8 path when possible
   path <- enc2utf8(path)
   
   # prefer unix-style slashes
   path <- chartr("\\", "/", path)
   
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
   interpreters <- unname(c(
      .rs.python.findPythonProjectInterpreters(),
      .rs.python.findPythonSystemInterpreters(),
      .rs.python.findPythonInterpretersInKnownLocations(),
      .rs.python.findPythonPyenvInterpreters(),
      .rs.python.findPythonVirtualEnvironments(),
      .rs.python.findPythonCondaEnvironments()
   ))
   
   default <- Sys.getenv("RETICULATE_PYTHON", unset = "")
   
   list(
      python_interpreters = .rs.scalarListFromList(interpreters),
      default_interpreter = .rs.scalar(default)
   )
   
})

.rs.addFunction("python.findPythonProjectInterpreters", function()
{
   projectDir <- .rs.getProjectDirectory()
   if (is.null(projectDir))
      return(list())
   
   candidateDirs <- list.files(
      path = projectDir,
      all.files = TRUE,
      full.names = TRUE
   )
   
   pythonSuffix <- if (.rs.platform.isWindows) "Scripts/python.exe" else "bin/python"
   candidatePaths <- file.path(candidateDirs, pythonSuffix)
   pythonPaths <- Filter(file.exists, candidatePaths)
   lapply(pythonPaths, .rs.python.getPythonInfo, strict = TRUE)
})

.rs.addFunction("python.findPythonSystemInterpreters", function()
{
   interpreters <- list()
   
   # look for interpreters on the PATH
   paths <- strsplit(Sys.getenv("PATH"), split = .Platform$path.sep, fixed = TRUE)[[1]]
   for (path in paths) {
      
      # skip fake broken Windows Python interpreters
      skip <-
         .rs.platform.isWindows &&
         grepl("AppData\\Local\\Microsoft\\WindowsApps", path, fixed = TRUE)
      
      if (skip)
         next
      
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
      for (python in pythons)
      {
         info <- .rs.python.getPythonInfo(python, strict = TRUE)
         interpreters[[length(interpreters) + 1]] <- info
      }
      
   }
   
   interpreters
   
})

.rs.addFunction("python.findPythonInterpretersInKnownLocations", function()
{
   # set of discovered paths
   pythonPaths <- character()
   
   # Python root paths we'll search in
   roots <- if (.rs.platform.isWindows) {
      
      # find path to Windows local app data folder
      localAppData <- local({
         
         path <- Sys.getenv("LOCALAPPDATA", unset = NA)
         if (!is.na(path))
            return(path)
         
         profile <- Sys.getenv("USERPROFILE", unset = NA)
         if (!is.na(profile))
            return(file.path(profile, "AppData\\Local"))
         
         ""
         
      })
      
      # system-installs of Python might be in one of these folders
      drive <- Sys.getenv("SYSTEMDRIVE", unset = "C:")
      suffixes <- c("/", "/Program Files", "/Program Files (x86)")

      c(
         paste0(drive, suffixes),
         file.path(localAppData, "Programs/Python")
      )
      
   } else {
      
      c(
         "/opt/python",
         "/opt/local/python",
         "/usr/local/opt/python"
      )
      
   }
   
   # also include those defined via option
   roots <- c(roots, getOption("rstudio.python.installationPath"))
      
   # check and see if any of these roots point directly
   # to a particular version of Python.
   #
   # on Windows, Python is typically installed in 'Scripts/python.exe'
   # for virtual environments, and 'python.exe' for Anaconda + standalone
   suffixes <- if (.rs.platform.isWindows) {
      c("Scripts/python.exe", "python.exe")
   } else {
      c("bin/python", "bin/python3")
   }
   
   paths <- vapply(roots, function(root) {
      paths <- file.path(root, suffixes)
      paths[file.exists(paths)][1]
   }, FUN.VALUE = character(1))
   
   # collect the discovered python paths, if any
   exists <- !is.na(paths)
   pythonPaths <- c(pythonPaths, paths[exists])
   roots <- roots[!exists]
   
   # treat any remaining root directories as versioned roots
   roots <- list.files(roots, full.names = TRUE)
   
   # check and see if any of these roots point directly
   # to a particular version of Python
   paths <- vapply(roots, function(root) {
      paths <- file.path(root, suffixes)
      paths[file.exists(paths)][1]
   }, FUN.VALUE = character(1))
   
   # collect the discovered python paths, if any
   exists <- !is.na(paths)
   pythonPaths <- c(pythonPaths, paths[exists])
   
   # return the discovered paths
   lapply(pythonPaths, .rs.python.getPythonInfo, strict = TRUE)
   
})

.rs.addFunction("python.findPythonPyenvInterpreters", function()
{
   root <- Sys.getenv("PYENV_ROOT", unset = "~/.pyenv")
   
   # on Windows, Python interpreters are normally part of pyenv-windows
   if (.rs.platform.isWindows)
      root <- file.path(root, "pyenv-win")
   
   # get path to roots of Python installations
   versionsPath <- file.path(root, "versions")
   pythonRoots <- list.files(versionsPath, full.names = TRUE)
   
   # form path to Python binaries
   stem <- if (.rs.platform.isWindows) "python.exe" else "bin/python"
   pythonPaths <- file.path(pythonRoots, stem)
   
   # exclude anything that doesn't exist for some reason
   pythonPaths <- pythonPaths[file.exists(pythonPaths)]
   
   # get interpreter info for each found
   lapply(pythonPaths, .rs.python.getPythonInfo, strict = TRUE)
   
})

.rs.addFunction("python.findPythonCondaEnvironments", function()
{
   envs <- tryCatch(
      reticulate::conda_list(),
      error = identity
   )
   
   if (inherits(envs, "error"))
      return(list())
   
   # ignore environments found in revdep folders
   envs <- envs[grep("/revdep/", envs$python, invert = TRUE), ]
   
   # ignore basilisk environments
   envs <- envs[grep("/basilisk/", envs$python, invert = TRUE), ]
   
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
