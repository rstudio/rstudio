#
# SessionPythonEnvironments.R
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

.rs.addJsonRpcHandler("python_active_interpreter", function()
{
   pythonPath <- Sys.getenv("RETICULATE_PYTHON", unset = NA)

   if (is.na(pythonPath))
      pythonPath <- Sys.getenv("RETICULATE_PYTHON_FALLBACK", unset = NA)

   info <- if (is.na(pythonPath))
   {
      .rs.python.invalidInterpreter(
         path = pythonPath,
         type = NULL,
         reason = "RETICULATE_PYTHON and RETICULATE_PYTHON_FALLBACK are unset."
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

.rs.addFunction("python.findCondaForEnvironment", function(envPath)
{
   # locate history file
   historyFile <- file.path(envPath, "conda-meta/history")
   if (!file.exists(historyFile))
      return("")

   # read first few lines
   contents <- readLines(historyFile, n = 2L, warn = FALSE)
   if (length(contents) < 2L)
    return("")

   # parse conda path
   line <- substring(contents[2L], 8L)
   index <- regexpr(" ", line, fixed = TRUE)
   if (index == -1L)
      return("")

   conda <- substring(line, 1L, index - 1L)
   if (.rs.platform.isWindows)
      conda <- file.path(dirname(conda), "conda.exe")

   # prefer condabin if it exists
   condabin <- file.path(dirname(conda), "../condabin", basename(conda))
   if (file.exists(condabin))
      conda <- condabin

   # bail if conda wasn't found
   if (!file.exists(conda))
      return("")

   .rs.canonicalizePath(conda)

})

.rs.addFunction("python.findWindowsPython", function()
{
   # if `py` and `python` are not available, nothing we can do
   pythonCommands <- c("py", "python3", "python")
   available <- nzchar(Sys.which(pythonCommands))

   if (!any(available))
      return("")
   else
      pythonCommand <- pythonCommands[available][1]

   # NOTE: ideally, we would just parse the output of 'py --list-paths',
   # but for whatever reason the '*' indicating the default version of
   # Python is omitted when run from RStudio, so we try to explicitly
   # run Python here and then ask where the executable lives.
   #
   # We pass '-E' here just to avoid things like PYTHONPATH potentially
   # influencing how the python session is launched
   pythonPath <- .rs.tryCatch(
      system2(
         command = pythonCommand,
         args    = c("-E", "-Xutf8"),
         input   = "import sys; print(sys.executable)",
         stdout  = TRUE,
         stderr  = TRUE
      )
   )

   if (inherits(pythonPath, "error"))
      return("")

   Encoding(pythonPath) <- "UTF-8"
   pythonPath
})

.rs.addFunction("python.configuredInterpreterPath", function(projectDir)
{
   # on Windows, check if PY_PYTHON is defined; if it is, we should use 'py'
   # to determine the version of python to be used
   if (.rs.platform.isWindows)
   {
      pyPython <- Sys.getenv("PY_PYTHON", unset = NA)
      if (!is.na(pyPython))
      {
         python <- .rs.python.findWindowsPython()
         if (file.exists(python))
            return(python)
      }
   }

   # check some pre-defined environment variables
   vars <- c("RENV_PYTHON", "RETICULATE_PYTHON", "RETICULATE_PYTHON_FALLBACK")
   for (var in vars)
   {
      value <- Sys.getenv(var, unset = NA)
      if (!is.na(value))
         return(value)
   }

   # if this project has a local interpreter, use it
   if (!is.null(projectDir))
   {
      projectPython <- .rs.python.projectInterpreterPath(projectDir)
      if (file.exists(projectPython))
         return(projectPython)
   }

   # check version of Python configured by user
   prefsPython <- .rs.readUiPref("python_path")
   if (file.exists(prefsPython))
      return(path.expand(prefsPython))

   # on Windows, help users find a default version of Python if possible
   if (.rs.platform.isWindows)
   {
     pythonPath <- .rs.python.findWindowsPython()
     if (file.exists(pythonPath))
        return(pythonPath)
   }

   # look for python + python3 on the PATH
   if (!.rs.platform.isWindows)
   {
     python3 <- Sys.which("python3")
     if (nzchar(python3) && python3 != "/usr/bin/python3")
        return(python3)

     python <- Sys.which("python")
     if (nzchar(python) && python != "/usr/bin/python")
     {
       info <- .rs.python.interpreterInfo(python, NULL)
       version <- numeric_version(info$version, strict = FALSE)
       if (!is.na(version) && version >= "3.2")
          return(python)
     }
   }

   # if the user has Anaconda installed, then try auto-activating
   # the base environment of that Anaconda installation
   conda <- .rs.python.findCondaBinary()
   if (file.exists(conda))
   {
     pythonPath <- if (.rs.platform.isWindows) "../python.exe" else "../bin/python"
     python <- file.path(dirname(conda), pythonPath)
     if (file.exists(python))
        return(python)
   }

   # fall back to versions of python in /usr/bin if available
   if (!.rs.platform.isWindows)
   {
     python3 <- Sys.which("python3")
     if (nzchar(python3) && python3 == "/usr/bin/python3")
        return(python3)

     python <- Sys.which("python")
     if (nzchar(python) && python == "/usr/bin/python")
     {
       info <- .rs.python.interpreterInfo(python, NULL)
       version <- numeric_version(info$version, strict = FALSE)
       if (!is.na(version) && version >= "3.2")
          return(python)
     }
   }

   # no python found; return empty string placeholder
   ""

})

.rs.addFunction("python.projectInterpreterPath", function(projectDir)
{
   # check for virtual environments / conda environments
   pythonSuffixes <- if (.rs.platform.isWindows)
      c("Scripts/python.exe", "python.exe")
   else
      c("bin/python3", "bin/python")

   # look within all top-level directories for an environment
   envPaths <- list.dirs(
      path = projectDir,
      full.names = TRUE,
      recursive = FALSE
   )

   for (envPath in envPaths)
   {
      for (pythonSuffix in pythonSuffixes)
      {
         pythonPath <- file.path(envPath, pythonSuffix)
         if (file.exists(pythonPath))
            return(pythonPath)
      }
   }

   ""
})

.rs.addFunction("python.initialize", function(projectDir)
{
   # do nothing if the user hasn't opted in
   activate <- .rs.readUiPref("python_project_environment_automatic_activate")
   if (!identical(activate, TRUE))
      return()

   # find path to python interpreter for this project
   pythonPath <- .rs.python.configuredInterpreterPath(projectDir)
   if (!file.exists(pythonPath))
      return()

   # normalize path (avoid following symlinks)
   pythonPath <- file.path(
      normalizePath(dirname(pythonPath), winslash = "/", mustWork = FALSE),
      basename(pythonPath)
   )

   # add the Python directory to the PATH
   pythonBin <- dirname(pythonPath)
   .rs.prependToPath(pythonBin)

   # if we have a Scripts directory, place that on the PATH as well
   # (primarily for Windows + conda environments)
   scriptsPath <- file.path(pythonBin, "Scripts")
   if (file.exists(scriptsPath))
      .rs.prependToPath(scriptsPath)

   pythonInfo <- .rs.python.getPythonInfo(pythonPath, strict = TRUE)

   # if this is a virtual environment, set VIRTUAL_ENV
   if (identical(pythonInfo$type, "virtualenv"))
   {
      envPath <- dirname(dirname(pythonPath))
      Sys.setenv(VIRTUAL_ENV = envPath)
   }

   # if this is a conda environment, set CONDA_PREFIX
   if (identical(pythonInfo$type, "conda"))
   {
      condaPrefix <- pythonBin
      if (!.rs.platform.isWindows)
         condaPrefix <- dirname(condaPrefix)

      Sys.setenv(CONDA_PREFIX = condaPrefix)

      # also ensure that conda is placed on the PATH
      condaPath <- .rs.python.findCondaForEnvironment(condaPrefix)
      if (file.exists(condaPath))
         .rs.prependToPath(dirname(condaPath))
   }

   # also set RETICULATE_PYTHON_FALLBACK so this python is used by default
   Sys.setenv(RETICULATE_PYTHON_FALLBACK = pythonPath)

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
   # get interpreters from all known sources
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
   paths <- unique(normalizePath(paths, winslash = "/", mustWork = FALSE))

   for (path in paths)
   {

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

      # loop over interpreters and add them
      for (python in pythons)
      {
         # skip simple symlinks. the intention here is to avoid adding
         # all of python, python3, and python3.9 if they all ultimately
         # resolve to the same python executable
         if (!.rs.platform.isWindows)
         {
            link <- Sys.readlink(python)
            if (grepl(pattern, link))
               next
         }

         # add the interpreter
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
      c("bin/python3", "bin/python")
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

.rs.addFunction("python.findCondaBinary", function()
{
   # read from option (primarily for internal use / testing)
   conda <- getOption("rstudio.conda.binary")
   if (!is.null(conda))
   {
      conda <- Sys.which(conda)
      if (nzchar(conda))
         return(conda)
   }

   # look on PATH
   conda <- Sys.which("conda")
   if (nzchar(conda))
      return(conda)

   # look in some default locations
   if (.rs.platform.isWindows)
   {
      condaRoots <- c(
         file.path(Sys.getenv("USERPROFILE"), "anaconda3"),
         file.path(Sys.getenv("SYSTEMDRIVE"), "ProgramData/anaconda3")
      )
   }
   else
   {
      condaRoots <- c(
         "~/opt/anaconda3",
         "~/anaconda3",
         "~/opt/miniconda3",
         "~/miniconda3",
         "/anaconda3",
         "/miniconda3"
      )
   }

   condaSuffix <- if (.rs.platform.isWindows)
      "condabin/conda.bat"
   else
      "condabin/conda"

   condaPaths <- file.path(condaRoots, condaSuffix)
   for (condaPath in condaPaths)
      if (file.exists(condaPath))
         return(path.expand(condaPath))

   ""

})

.rs.addFunction("python.findPythonCondaEnvironments", function()
{
   # look for conda
   conda <- .rs.python.findCondaBinary()
   if (!file.exists(conda))
      return(NULL)

   # ask it for environments
   args <- c("env", "list", "--json", "--quiet")
   tmp <- tempfile()
   output <- system2(conda, args, stdout = TRUE, stderr = tmp)

   status <- .rs.nullCoalesce(attr(output, "status", exact = TRUE), 0L)
   if (!identical(status, 0L)) {
     errors <- paste(readLines(tmp), collapse = "\n")
     .rs.stopf("Error executing %s %s:\n%s", conda, paste(args, collapse = " "), errors)
   }
   json <- .rs.fromJSON(paste(output, collapse = "\n"))
   envList <- unlist(json$envs)

   # prefer unix separators
   if (.rs.platform.isWindows)
      envList <- chartr("\\", "/", envList)

   # ignore certain special environments
   ignorePatterns <- c("/revdep/", "/basilisk/", "/renv/python/condaenvs/")
   pattern <- sprintf("(?:%s)", paste(ignorePatterns, collapse = "|"))
   envList <- grep(pattern, envList, value = TRUE, invert = TRUE)

   # get paths to Python in each environment
   pythonSuffix <- if (.rs.platform.isWindows) "python.exe" else "bin/python"
   pythonPaths <- file.path(envList, pythonSuffix)

   # only keep existing
   pythonPaths <- Filter(file.exists, pythonPaths)

   # drop duplicates, just in case
   pythonPaths <- unique(normalizePath(pythonPaths, winslash = "/"))

   # get information for each environment
   lapply(pythonPaths, .rs.python.getCondaEnvironmentInfo)
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
