#
# Tools.R
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

# target environment for rstudio supplemental tools
# (guard against attempts to duplicate this environment)
.rs.Env <- if ("tools:rstudio" %in% search()) {
   as.environment("tools:rstudio")
} else {
   attach(NULL, name = "tools:rstudio")
}

# allow access to tools env from helper function
assign(".rs.toolsEnv", function()
{
   .rs.Env
}, envir = .rs.Env)

# target environment for symbol lookup, with all necessary base packages
assign(".rs.symbolLookupEnv", function()
{
   new.env(parent = .rs.toolsEnv())
}, envir = .rs.Env)

# platform detection
assign(".rs.platform.isUnix",    .Platform$OS.type == "unix",         envir = .rs.Env)
assign(".rs.platform.isWindows", .Platform$OS.type == "windows",      envir = .rs.Env)
assign(".rs.platform.isLinux",   Sys.info()[["sysname"]] == "Linux",  envir = .rs.Env)
assign(".rs.platform.isMacos",   Sys.info()[["sysname"]] == "Darwin", envir = .rs.Env)

#' Add a function to the 'tools:rstudio' environment.
#'
#' This environment is placed on the search path, and so is accessible and
#' readable during regular evaluation in an R session.
#'
#' @param name The name of the R function. The prefix '.rs.' will be pre-pended
#'   to the supplied function name.
#'
#' @param FN The \R function to be added.
#'
#' @param attrs An optional list of attributes, to be set on the function.
#'
#' @param envir An optional environment, to be set as the enclosing environment
#'   for the function `f`. By default, newly-defined functions use the
#'   'tools:rstudio' environment as the parent function. For functions which
#'   might be exposed to users (e.g. via options), you may want to instead use
#'   the base environment to avoid issues with serialization.
assign(".rs.addFunction", function(name, FN, attrs = list(), envir = .rs.toolsEnv())
{
   # add optional attributes
   for (attrib in names(attrs))
      attr(FN, attrib) <- attrs[[attrib]]

   # ensure function evaluates in requested environment
   environment(FN) <- envir

   # assign in tools env
   fullName <- paste(".rs.", name, sep = "")
   assign(fullName, FN, envir = .rs.toolsEnv())

}, envir = .rs.Env)

# force helper function to also execute in tools environment
environment(.rs.Env[[".rs.addFunction"]]) <- .rs.Env

# add a global (non-scoped) variable to the tools:rstudio environment
.rs.addFunction("addGlobalVariable", function(name, var)
{
   envir <- .rs.toolsEnv()
   environment(var) <- envir
   assign(name, var, envir = envir)
})

# add a global (non-scoped) function to the tools:rstudio environment
.rs.addFunction("addGlobalFunction", function(name, FN)
{
   envir <- .rs.toolsEnv()
   environment(FN) <- envir
   assign(name, FN, envir = envir)
})

# add an rpc handler to the tools:rstudio environment
.rs.addFunction("addApiFunction", function(name, FN)
{
   fullName <- paste("api.", name, sep = "")
   envir <- .rs.symbolLookupEnv()
   environment(FN) <- envir
   .rs.addFunction(fullName, FN, envir = envir)
})

# for functions which might act as error handlers
.rs.addFunction("addErrorHandlerFunction", function(name, type, handler)
{
   attrs <- list(
      rstudioErrorHandler = TRUE,
      hideFromDebugger    = TRUE,
      errorHandlerType    = type
   )

   .rs.addFunction(name, handler, attrs, envir = .rs.toolsEnv())
})

.rs.addFunction("setVar", function(name, var)
{
   envir <- .rs.toolsEnv()
   if (!is.null(var))
      environment(var) <- envir

   fullName <- paste(".rs.", name, sep = "")
   assign(fullName, var, envir = envir)
})

.rs.addFunction("defineVar", function(name, var)
{
   envir <- .rs.toolsEnv()
   if (!is.null(var))
      environment(var) <- envir

   fullName <- paste(".rs.", name, sep = "")
   if (!exists(fullName, envir = envir, inherits = FALSE))
      assign(fullName, var, envir = envir)
})

.rs.addFunction("clearVar", function(name)
{
   envir <- .rs.toolsEnv()
   fullName <- paste(".rs.", name, sep = "")
   remove(list = fullName, pos = envir)
})

.rs.addFunction("getVar", function(name)
{
   envir <- .rs.toolsEnv()
   fullName <- paste(".rs.", name, sep = "")
   envir[[fullName]]
})

.rs.addFunction("hasVar", function(name)
{
   envir <- .rs.toolsEnv()
   fullName <- paste(".rs.", name, sep = "")
   exists(fullName, envir = envir)
})

.rs.addFunction("defineHookImpl", function(package, binding, hook, all)
{
   # get reference to original function definition
   namespace <- getNamespace(package)
   original <- get(binding, envir = namespace)

   # update our hook to match definition of original
   environment(hook) <- environment(original)
   formals(hook) <- formals(original)

   # replace binding in search path environment
   envir <- as.environment(paste("package", package, sep = ":"))
   .rs.replaceBindingImpl(envir, binding, hook)

   # also replace the binding in the package namespace if requested
   if (all)
   {
      .rs.registerPackageAttachedHook(package, function(...)
      {
         .rs.replaceBindingImpl(namespace, binding, hook)
      })
   }
})

.rs.addFunction("defineGlobalHook", function(package, binding, hook, when = TRUE)
{
   if (when)
      .rs.defineHookImpl(package, binding, hook, all = TRUE)
})

.rs.addFunction("defineHook", function(package, binding, hook, when = TRUE)
{
   if (when)
      .rs.defineHookImpl(package, binding, hook, all = FALSE)
})

.rs.addFunction("setOption", function(name, value)
{
   data <- list(value)
   names(data) <- name
   options(data)
})

.rs.addFunction("setOptionDefault", function(name, value)
{
   # if the option is already set, nothing to do
   if (!is.null(getOption(name)))
      return(FALSE)

   # otherwise, set it
   data <- list(value)
   names(data) <- name
   options(data)

   TRUE
})

.rs.addFunction("evalInGlobalEnv", function(code)
{
   eval(
      parse(text = code),
      envir = globalenv()
   )
})

# attempts to restore the global environment from a file
# on success, returns an empty string; on failure, returns
# the error message
.rs.addFunction("restoreGlobalEnvFromFile", function(path)
{
   Encoding(path) <- "UTF-8"

   # avoid path encoding issues by moving to directory first
   if (!file.exists(path))
      return(paste(path, "does not exist"))

   owd <- setwd(dirname(path))
   on.exit(setwd(owd), add = TRUE)

   status <- try(
      load(basename(path), envir = .GlobalEnv),
      silent = TRUE
   )

   if (!inherits(status, "try-error"))
      return("")

   # older versions of R don't provide a 'condition' attribute
   # for 'try()' errors
   condition <- attr(status, "condition")
   if (is.null(condition)) {
      if (is.character(status))
         return(paste(c(status), collapse = "\n"))
      else
         return("Unknown Error")
   }

   # has condition, but no message? should not happen
   if (!"message" %in% names(condition))
      return("Unknown Error")

   paste(condition$message, collapse = "\n")
})

# save current state of options() to file
.rs.addFunction("saveOptions", function(filename)
{
   # get reference to current options
   opt <- options()

   # don't attempt to serialize cpp11 preserve env as it may
   # contain recursive R objects which cause crashes when serialized
   #
   # https://github.com/rstudio/rstudio-pro/issues/2052
   opt$cpp11_preserve_env  <- NULL
   opt$cpp11_preserve_xptr <- NULL

   # first write to sidecar file, and then rename that file
   # (don't let failed serialization leave behind broken workspace)
   sidecarFile <- paste(filename, "incomplete", sep = ".")

   # remove an old sidecar file if any -- these would be leftover from
   # a previously-failed attempt to save the session
   unlink(sidecarFile)

   status <- tryCatch(
      suppressWarnings(save(opt, file = sidecarFile)),
      error = identity
   )

   # if we manage to catch the error (might not be possible for stack overflow)
   # then clean up the sidecar file and re-propagate the error (caller will take
   # care of reporting further errors to user)
   if (inherits(status, "error"))
   {
      unlink(sidecarFile)
      stop(status)
   }

   # save completed successfully -- rename sidecar file to final location
   file.rename(sidecarFile, filename)
})

# restore options() from file
.rs.addFunction("restoreOptions", function(filename)
{
   load(filename)
   options(opt)
})

# save current state of .libPaths() to file
.rs.addFunction("saveLibPaths", function(filename)
{
  libPaths <- .libPaths()
  save(libPaths, file = filename)
})

# restore .libPaths() from file
.rs.addFunction("restoreLibPaths", function(filename)
{
  load(filename)
  .libPaths(libPaths)
})


# try to determine if devtools::dev_mode is on
.rs.addFunction("devModeOn", function(){

  # determine devmode path (devtools <= 0.6 hard-coded it)
  devToolsPath <- getOption("devtools.path")
  if (is.null(devToolsPath))
    if ("devtools" %in% .packages())
      devToolsPath <- "~/R-dev"

  # no devtools path
  if (is.null(devToolsPath))
    return (FALSE)

  # is the devtools path active?
  devToolsPath <- .rs.normalizePath(devToolsPath, winslash = "/", mustWork = FALSE)
  devToolsPath %in% .libPaths()
})

.rs.addFunction("isDevPackage", function(name) {
   "pkgload" %in% loadedNamespaces() && pkgload::is_dev_package(name)
})

# load a package by name
.rs.addFunction("loadPackage", function(packageName, lib)
{
   # when R loads package dependencies through a call to `library()`, dependent
   # packages will be searched for on the current library paths rather than the
   # library path(s) passed to the lib.loc argument. for that reason, it is
   # prudent to set the library paths explicitly when loading a package.
   #
   # note that we want to preserve the existing library paths as well since
   # dependencies may live within a separate library path from the package to be
   # loaded; it should suffice to place the requested library at the front of
   # the library paths
   if (nzchar(lib)) {
      libPaths <- .libPaths()
      .libPaths(c(lib, libPaths))
      on.exit(.libPaths(libPaths), add = TRUE)
   }

   library(packageName, character.only = TRUE)
})

# unload a package by name
.rs.addFunction("unloadPackage", function(packageName)
{
   pkg = paste("package:", packageName, sep="")
   detach(pos = match(pkg, search()))
})

.rs.addFunction("getPackageVersion", function(packageName)
{
   v <- suppressWarnings(utils:::packageDescription(packageName,
                                              fields="Version"))
   package_version(v)
})

# save an environment to a file
.rs.addFunction("saveEnvironment", function(env, filename)
{
   # suppress warnings emitted here, as they are not actionable
   # by the user (and seem to be harmless)
   suppressWarnings(
      save(list = ls(envir = env, all.names = TRUE),
           file = filename,
           envir = env)
   )

   invisible (NULL)
})

.rs.addFunction("disableSaveCompression", function()
{
  options(save.defaults=list(ascii=FALSE, compress=FALSE))
  options(save.image.defaults=list(ascii=FALSE, safe=TRUE, compress=FALSE))
})

.rs.addFunction("attachDataFile", function(filename, name, pos = 2)
{
   if (!file.exists(filename))
      stop(gettextf("file '%s' not found", filename), domain = NA)

   .Internal(attach(NULL, pos, name))
   load(filename, envir = as.environment(pos))

   invisible (NULL)
})

.rs.addGlobalFunction("RStudioGD", function()
{
   .Call("rs_createGD")
})

# set our graphics device as the default and cause it to be created/set
.rs.addFunction("initGraphicsDevice", function()
{
   options(device="RStudioGD")
   grDevices::deviceIsInteractive("RStudioGD")
})

.rs.addFunction("activateGraphicsDevice", function()
{
   invisible(.Call("rs_activateGD"))
})

.rs.addFunction("newDesktopGraphicsDevice", function()
{
   sysName <- Sys.info()[['sysname']]
   if (identical(sysName, "Windows"))
      windows()
   else if (identical(sysName, "Darwin"))
      quartz()
   else if (capabilities("X11"))
      X11()
   else {
      warning("Unable to create a new graphics device ",
              "(RStudio device already active and only a ",
              "single RStudio device is supported)",
              call. = FALSE)
   }
})

# record an object to a file
.rs.addFunction("saveGraphicsSnapshot", function(snapshot, filename)
{
   # make a copy of the snapshot into plot and set its metadata in a way
   # that is compatible with recordPlot
   plot = snapshot
   attr(plot, "version") <- as.character(getRversion())
   class(plot) <- "recordedplot"

   save(plot, file=filename)
})

.rs.addFunction("GEplayDisplayList", function()
{
   tryCatch(
      .Call("rs_GEplayDisplayList"),
      error = function(e) warning(e)
   )
})

.rs.addFunction("GEcopyDisplayList", function(fromDevice)
{
   tryCatch(
      .Call("rs_GEcopyDisplayList", fromDevice),
      error = function(e) warning(e)
   )
})

# record an object to a file
.rs.addFunction("saveGraphics", function(filename)
{
   plot <- grDevices::recordPlot()
   save(plot, file = filename)
})

# restore an object from a file
.rs.addFunction("restoreGraphics", function(filename)
{
   # load the 'plot' object
   envir <- new.env(parent = emptyenv())
   load(filename, envir = envir)
   plot <- envir$plot

   # restore native symbols
   dlls <- getLoadedDLLs()
   rVersion <- getRversion()
   wasPairlist <- is.pairlist(plot[[1]])

   # convert to list (iterating large pairlist in R is slow; especially
   # since we need to update the data structure as we read through)
   items <- as.list(plot[[1]])

   # iterate through and update native symbols (this is necessary as the
   # saved object will contain native routines with incorrect or null
   # addresses; we need to re-discover the correct address for the routines
   # required in generating the plot)
   restored <- lapply(items, function(item) {

      # extract saved symbol
      symbol <- item[[2]][[1]]
      if (!inherits(symbol, "NativeSymbolInfo"))
         return(item)

      # extract associated package name
      name <- if (is.null(symbol$package))
         symbol$dll[["name"]]
      else
         symbol$package[["name"]]

      # re-construct the required symbol
      nativeSymbol <- getNativeSymbolInfo(
         name    = symbol$name,
         PACKAGE = dlls[[name]]
      )

      # replace the old symbol
      item[[2]][[1]] <- nativeSymbol
      item

   })

   # turn back into pairlist after
   if (wasPairlist)
      restored <- as.pairlist(restored)

   # update plot items
   if (!is.null(restored))
      plot[[1]] <- restored

   # tag plot with process pid
   plotPid <- attr(plot, "pid")
   if (is.null(plotPid) || (plotPid != Sys.getpid()))
      attr(plot, "pid") <- Sys.getpid()

   # if this plot depends on the grid package, and we don't have a graphics
   # device open, then we'll have to create a new one
   # https://github.com/rstudio/rstudio/issues/2919
   for (item in plot) {
      name <- attr(item, "pkgName", exact = TRUE)
      if (is.character(name) && name[[1L]] %in% c("grid", "ggplot2")) {
         if (requireNamespace("grid", quietly = TRUE))
            grid::grid.newpage()
         break
      }
   }

   # we suppressWarnings so that R doesn't print a warning if we restore
   # a plot saved from a previous version of R (which will occur if we
   # do a resume after upgrading the version of R on the server)
   suppressWarnings(grDevices::replayPlot(plot))

})

# generate a uuid
.rs.addFunction("createUUID", function()
{
  .Call("rs_createUUID")
})

# check the current R architecture
.rs.addFunction("getRArch", function()
{
   .Platform$r_arch
})

# pager
.rs.addFunction("pager", function(files, header, title, delete.file)
{
   for (i in 1:length(files)) {
      if ((i > length(header)) || !nzchar(header[[i]]))
         fileTitle <- title
      else
         fileTitle <- header[[i]]

      .Call("rs_showFile", fileTitle, files[[i]], delete.file)
   }
})

.rs.addFunction("canonicalizePath", function(path, winslash = "/")
{
   file.path(
      normalizePath(dirname(path), winslash = winslash, mustWork = FALSE),
      basename(path),
      fsep = winslash
   )
})

# alias for normalizePath function
.rs.addFunction("normalizePath", normalizePath)

# alias for path.package function
.rs.addFunction("pathPackage", path.package)

# handle viewing a pdf differently on each platform:
#  - windows: shell.exec
#  - mac: Preview
#  - linux: getOption("pdfviewer")
.rs.addFunction("shellViewPdf", function(path)
{
   sysName <- Sys.info()[['sysname']]

   if (identical(sysName, "Windows"))
   {
     shell.exec(path)
   }
   else
   {
      # force preview on osx to workaround acrobat reader crashing bug
      if (identical(sysName, "Darwin"))
         cmd <- paste("open", "-a", "Preview")
      else
         cmd <- shQuote(getOption("pdfviewer"))
      system(paste(cmd, shQuote(path)), wait = FALSE)
   }
})

.rs.addFunction("replaceBindingImpl", function(envir, binding, value)
{
   if (exists(binding, envir = envir, inherits = FALSE))
   {
      if (bindingIsLocked(binding, envir))
      {
         unlockBinding(binding, envir)
         on.exit(lockBinding(binding, envir), add = TRUE)
      }

      assign(binding, value, envir = envir)
   }
})

.rs.addFunction("replaceBinding", function(binding, package, override)
{
   # override in namespace
   if (!requireNamespace(package, quietly = TRUE))
      stop(sprintf("Failed to load namespace for package '%s'", package))

   namespace <- asNamespace(package)

   # get reference to original binding
   original <- get(binding, envir = namespace)

   # replace the binding
   if (is.function(override))
      environment(override) <- namespace

   .rs.replaceBindingImpl(namespace, binding, override)

   # if package is attached, override there as well
   searchPathName <- paste("package", package, sep = ":")
   if (searchPathName %in% search()) {
      env <- as.environment(searchPathName)
      if (exists(binding, envir = env))
         .rs.replaceBindingImpl(env, binding, override)
   }

   # return original
   original
})

# hook an internal R function
.rs.addFunction("replacePackageBinding", function(name,
                                                  package,
                                                  hook,
                                                  namespace = FALSE)
{
   # replace in environment on search path
   envir <- as.environment(paste("package", package, sep = ":"))
   .rs.replaceBindingImpl(envir, name, hook)

   # remap in function namespace if requested as well
   if (namespace)
   {
      envir <- asNamespace(package)
      .rs.replaceBindingImpl(envir, name, hook)
   }
})

.rs.addFunction("registerHook", function(name,
                                         package,
                                         hookFactory,
                                         namespace = FALSE)
{
   # ensure the package is loaded and attached (since we need to modify
   # the version of the function normally placed on the search path)
   library(package, character.only = TRUE, quietly = TRUE)

   # construct search path environment name for package
   packageName <- paste("package", package, sep = ":")

   # get original version of function (bail if it doesn't exist)
   original <- base::get(name, packageName, mode = "function")
   if (is.null(original)) {
      fmt <- "internal error: function %s not found"
      msg <- sprintf(fmt, shQuote(name))
      stop(msg, call. = FALSE)
   }

   # new function definition
   new <- hookFactory(original)

   # replace the bindings
   .rs.replacePackageBinding(name, package, new, namespace)
})

.rs.addFunction("callAs", function(name, f, ...)
{
   # TODO: figure out how to print the args (...) as part of the message

   # run the original function (f). setup condition handlers solely so that
   # we can correctly print the name of the function called in error
   # and warning messages -- otherwise R prints "original(...)"
   withCallingHandlers(
      tryCatch(
         f(...),
         error = function(e)
         {
            cat("Error in ", name, " : ", e$message, "\n", sep = "")
         }
      ),
      warning = function(w)
      {
         if (getOption("warn") >= 0)
            cat("Warning in ", name, " :\n  ",  w$message, "\n", sep = "")
         invokeRestart("muffleWarning")
      }
   )
})

# replacing an internal R function
.rs.addFunction("registerReplaceHook", function(name, package, hook, namespace = FALSE)
{
   hookFactory <- function(original) function(...) .rs.callAs(name, hook,  original, ...)
   .rs.registerHook(name, package, hookFactory, namespace)
})

# notification that an internal R function was called
.rs.addFunction("registerNotifyHook", function(name, package, hook, namespace = FALSE)
{
   hookFactory <- function(original) function(...)
   {
      # call hook after original is executed
      on.exit(hook(...))

      # call original
      .rs.callAs(name, original, ...)
   }
   .rs.registerHook(name, package, hookFactory, namespace);
})

# marking functions in R packages as unsupported
.rs.addFunction("registerUnsupported", function(name, package, alternative = "")
{
   unsupported <- function(...)
   {
      msg <- "function not supported in RStudio"
      if (nzchar(alternative))
        msg <- paste(msg, "(try", alternative, "instead)")
      msg <- paste(msg, "\n", sep = "")
      stop(msg)
   }

   .rs.registerReplaceHook(name, package, unsupported)
})

.rs.addFunction("parseCRANReposList", function(repos) {
  parts <- strsplit(repos, "\\|")[[1]]
  indexes <- seq_len(length(parts) / 2)

  r <- list()
  for (i in indexes)
    r[[parts[[2 * i - 1]]]] <- parts[[2 * i]]

  r
})

.rs.addFunction("setCRANRepos", function(cran, secondary)
{
  local({

      r <- c(
        list(CRAN = cran),
        .rs.parseCRANReposList(secondary)
      )

      # ensure repos is character (many packages assume the
      # repos option will be a character vector)
      n <- names(r)
      r <- as.character(r)
      names(r) <- n

      # attribute indicating the repos was set from rstudio prefs
      attr(r, "RStudio") <- TRUE

      options(repos=r)
    })
})

.rs.addFunction("setCRANReposAtStartup", function(cran, secondary)
{
   # check whether the user has already set a CRAN repository
   # in their .Rprofile
   repos <- getOption("repos")
   cranMirrorConfigured <- !"@CRAN@" %in% repos
   if (!cranMirrorConfigured)
      .rs.setCRANRepos(cran, secondary)
})

.rs.addFunction("isCRANReposFromSettings", function()
{
   !is.null(attr(getOption("repos"), "RStudio"))
})


.rs.addFunction("setCRANReposFromSettings", function(cran, secondary)
{
   # only set the repository if the repository was set by us
   # in the first place (it wouldn't be if the user defined a
   # repository in .Rprofile or called setRepositories directly)
   if (.rs.isCRANReposFromSettings())
      .rs.setCRANRepos(cran, secondary)
})


.rs.addFunction("libPathsAppend", function(path)
{
   # remove it if it already exists
   .libPaths(.libPaths()[.libPaths() != path])

   # append it
   .libPaths(append(.libPaths(), path))
})


.rs.addFunction("isLibraryWriteable", function(lib)
{
   # file.access() can be unreliable here, as it's
   # possible for a directory to be un-writable despite
   # having writable permissions set. the best way to
   # be sure is to try to create and remove a file in
   # that directory
   file <- tempfile(pattern = ".rstudio-", tmpdir = lib)
   status <- tryCatch(file.create(file), condition = identity)

   # treat any conditions as errors, since R will emit a
   # warning (rather than error) if file creation fails
   if (inherits(status, "condition"))
      return(FALSE)

   # now, try to remove the temporary file (it would stink
   # if we could create files but not remove them ...)
   status <- tryCatch(file.remove(file), condition = identity)
   if (inherits(status, "condition"))
      return(FALSE)

   # we successfully created and removed a file in the library
   # directory; treat it as writable
   TRUE

})

.rs.addFunction("defaultLibPathIsWriteable", function()
{
   .rs.isLibraryWriteable(.libPaths()[1L])
})

# Support for implementing json-rpc methods directly in R:
#
# - json-rpc method endpoints can be installed by calling the
#   .rs.addJsonRpcHandler function (all R files within the handlers directory
#   are sourced so that they can install handlers)
#
# - these endpoints are installed within the tools:rstudio environment,
#   therefore if common helper methods are required they should be added to
#   the same environment using .rs.addFunction
#
# - Json <-> R marshalling is implemented within RJsonRpc.cpp
#   details on how this works can be found in the comments therin
#

# add an rpc handler to the tools:rstudio environment
.rs.addFunction("addJsonRpcHandler", function(name, FN)
{
   fullName <- paste("rpc.", name, sep = "")
   .rs.addFunction(fullName, FN, TRUE)
})

# list all rpc handlers in the tools:rstudio environment
.rs.addFunction("listJsonRpcHandlers", function()
{
   objects(
      name      = "tools:rstudio",
      pattern   = utils:::glob2rx(".rs.rpc.*"),
      all.names = TRUE
   )
})


.rs.addFunction("showDiagnostics", function()
{
  diagPath <- shQuote(.rs.normalizePath("~/rstudio-diagnostics"))
  sysName <- Sys.info()[['sysname']]
  if (identical(sysName, "Windows"))
    shell.exec(diagPath)
  else if (identical(sysName, "Darwin"))
    system(paste("open", diagPath))
  else if (nzchar(Sys.which("nautilus")))
    system(paste("nautilus", diagPath))
})

.rs.addFunction("history", function(max.show = 25, reverse = FALSE, pattern, ...)
{
   invisible(.Call("rs_activatePane", "history", PACKAGE = "(embedding)"))
})

.rs.addFunction("savehistory", function(file = ".Rhistory")
{
   invisible(.Call("rs_saveHistory", file, PACKAGE = "(embedding)"))
})

.rs.addFunction("loadhistory", function(file = ".Rhistory")
{
   invisible(.Call("rs_loadHistory", file, PACKAGE = "(embedding)"))
})

.rs.addFunction("timestamp", function(stamp = date(),
                                      prefix = "##------ ",
                                      suffix = " ------##",
                                      quiet = FALSE)
{
   stamps <- paste(prefix, stamp, suffix, sep = "")

   lapply(stamps, function(stamp) {
      invisible(.Call("rs_timestamp", stamp, PACKAGE = "(embedding)"))
   })

   if (!quiet)
      cat(stamps, sep = "\n")

   invisible(stamps)
})

.rs.replacePackageBinding("history", "utils", .rs.history)

.rs.addFunction("registerHistoryFunctions", function()
{
   .rs.replacePackageBinding("savehistory", "utils", .rs.savehistory, namespace = TRUE)
   .rs.replacePackageBinding("loadhistory", "utils", .rs.loadhistory, namespace = TRUE)
   .rs.replacePackageBinding("timestamp", "utils", .rs.timestamp, namespace = TRUE)
})


.rs.addFunction("parseQuitArguments", function(command) {

   # parse the command
   expr <- parse(text = command)
   if (length(expr) == 0)
      stop("Not a fully formed command: ", command)

   # match args
   call <- as.call(expr[[1]])
   call <- match.call(quit, call)

   # return as list without the function name
   as.list(call)[-1]
})

.rs.addFunction("isTraced", function(fun) {
   isS4(fun) && class(fun) == "functionWithTrace"
})

# when a function is traced, some data about the function (such as its original
# body and source references) exist only on the untraced copy
.rs.addFunction("untraced", function(fun) {
   if (.rs.isTraced(fun))
      fun@original
   else
      fun
})

.rs.addFunction("getSrcref", function(fun) {
   attr(.rs.untraced(fun), "srcref")
})

# returns a list containing line data from the given source reference,
# formatted for output to the client
.rs.addFunction("lineDataList", function(srcref) {
   list(
      line_number = .rs.scalar(srcref[1]),
      end_line_number = .rs.scalar(srcref[3]),
      character_number = .rs.scalar(srcref[5]),
      end_character_number = .rs.scalar(srcref[6]))
})

.rs.addFunction("haveRequiredRSvnRev", function(requiredSvnRev) {
   svnRev <- R.version$`svn rev`
   if (!is.null(svnRev)) {
      svnRevNumeric <- suppressWarnings(as.numeric(svnRev))
      if (!is.na(svnRevNumeric) && length(svnRevNumeric) == 1)
         svnRevNumeric >= requiredSvnRev
      else
         FALSE
   } else {
      FALSE
   }
})

.rs.addFunction("rVersionString", function() {
   as.character(getRversion())
})

.rs.addFunction("listFilesFuzzy", function(directory, token)
{
   pattern <- if (nzchar(token))
      paste("^", .rs.asCaseInsensitiveRegex(.rs.escapeForRegex(token)), sep = "")

   # Manually construct a call to `list.files` which should work across
   # versions of R >= 2.11.
   formals <- as.list(formals(base::list.files))

   formals$path <- directory
   formals$pattern <- pattern
   formals$all.files <- TRUE
   formals$full.names <- TRUE

   # NOTE: not available in older versions of R, but defaults to FALSE
   # with newer versions.
   if ("include.dirs" %in% names(formals))
      formals[["include.dirs"]] <- TRUE

   # NOTE: not available with older versions of R, but defaults to FALSE
   if ("no.." %in% names(formals))
      formals[["no.."]] <- TRUE

   # Generate the call, and evaluate it.
   result <- do.call(base::list.files, formals)

   # Clean up duplicated '/'.
   absolutePaths <- gsub("/+", "/", result)

   # Remove un-needed `.` paths. These paths will look like
   #
   #     <path>/.
   #     <path>/..
   #
   # This is only unnecessary if we couldn't use 'no..'.
   if (!("no.." %in% names(formals)))
   {
      absolutePaths <- grep("/\\.+$",
                            absolutePaths,
                            invert = TRUE,
                            value = TRUE)
   }

   absolutePaths
})

.rs.addFunction("callWithRDS", function(functionName, inputLocation, outputLocation)
{
   params <- readRDS(inputLocation)
   result <- do.call(functionName, params)

   saveRDS(file = outputLocation, object = result)
})

.rs.addFunction("readFile", function(file,
                                     binary = FALSE,
                                     encoding = NULL)
{
   size <- file.info(file)$size
   if (binary)
      return(readBin(file, "raw", size))
   contents <- readChar(file, size, TRUE)
   if (is.character(encoding))
      Encoding(contents) <- encoding
   contents
})

.rs.addFunction("fromJSON", function(string)
{
   .Call("rs_fromJSON", string)
})

.rs.addFunction("stringBuilder", function()
{
   (function() {
      indent_ <- "  "
      indentSize_ <- 0
      data_ <- character()

      indented_ <- function(data) {
         indent <- paste(character(indentSize_ + 1), collapse = indent_)
         for (i in seq_along(data))
            if (is.list(data[[i]]))
               data[[i]] <- indented_(data[[i]])
            else
               data[[i]] <- paste(indent, data[[i]], sep = "")
            data
      }

      list(

         append = function(...) {
            data_ <<- c(data_, indented_(list(...)))
         },

         appendf = function(...) {
            data_ <<- c(data_, indented_(sprintf(...)))
         },

         indent = function() {
            indentSize_ <<- indentSize_ + 1
         },

         unindent = function() {
            indentSize_ <<- max(0, indentSize_ - 1)
         },

         data = function() unlist(data_)

      )

   })()
})

.rs.addFunction("listBuilder", function()
{
   (function() {
      capacity_ <- 1024
      index_ <- 0
      data_ <- vector("list", capacity_)

      append <- function(data) {

         # increment index and check capacity
         index_ <<- index_ + 1
         if (index_ > capacity_) {
            capacity_ <<- capacity_ * 2
            data_[capacity_] <<- list(NULL)
         }

         # append data
         if (is.null(data))
            data_[index_] <<- list(NULL)
         else
            data_[[index_]] <<- data
      }

      data <- function() {
         data_[seq_len(index_)]
      }

      clear <- function() {
         capacity_ <<- 1024
         index_ <<- 0
         data_ <<- vector("list", capacity_)
      }

      empty <- function() {
         index_ == 0
      }

      list(append = append, clear = clear, empty = empty, data = data)

   })()
})

.rs.addFunction("regexMatches", function(pattern, x) {

   matches <- gregexpr(pattern, x, perl = TRUE)[[1]]
   starts <- attr(matches, "capture.start")
   ends <- starts + attr(matches, "capture.length") - 1
   strings <- substring(x, starts, ends)

   names(strings) <- attr(matches, "capture.names")
   strings

})

.rs.addFunction("withChangedExtension", function(path, ext)
{
   ext <- sub("^\\.+", "", ext)
   if (.rs.endsWith(path, ".nb.html"))
      paste(substring(path, 1, nchar(path) - 8), ext, sep = ".")
   else if (.rs.endsWith(path, ".tar.gz"))
      paste(substring(path, 1, nchar(path) - 7), ext, sep = ".")
   else
      paste(tools::file_path_sans_ext(path), ext, sep = ".")
})

.rs.addFunction("dirExists", function(path)
{
   utils::file_test('-d', path)
})

.rs.addFunction("ensureDirectory", function(path)
{
   if (file.exists(path)) {
      if (!utils::file_test("-d", path))
         stop("file at path '", path, "' exists but is not a directory")
      return(TRUE)
   }

   success <- dir.create(path, recursive = TRUE)
   if (!success)
      stop("failed to create directory at path '", path, "'")

   TRUE
})

# adapted from merge_lists in the rmarkdown package
.rs.addFunction("mergeLists", function(baseList, overlayList, recursive = TRUE) {
  if (length(baseList) == 0)
    overlayList
  else if (length(overlayList) == 0)
    baseList
  else {
    mergedList <- baseList
    for (name in names(overlayList)) {
      base <- baseList[[name]]
      overlay <- overlayList[[name]]
      if (is.list(base) && is.list(overlay) && recursive)
        mergedList[[name]] <- .rs.mergeLists(base, overlay)
      else {
        mergedList[[name]] <- NULL
        mergedList <- append(mergedList,
                              overlayList[which(names(overlayList) %in% name)])
      }
    }
    mergedList
  }
})

.rs.addFunction("nBytes", function(x) {
   nchar(x, type = "bytes")
})

.rs.addFunction("randomString", function(prefix = "",
                                         postfix = "",
                                         candidates = c(letters, LETTERS, 0:9),
                                         n = 16L)
{
   sampled <- sample(candidates, n, TRUE)
   paste(prefix, paste(sampled, collapse = ""), postfix, sep = "")
})

.rs.addFunction("rbindList", function(data)
{
   data <- Filter(length, data)
   result <- do.call(mapply, c(c, data, USE.NAMES = FALSE, SIMPLIFY = FALSE))
   names(result) <- names(data[[1]])
   as.data.frame(result, stringsAsFactors = FALSE)
})

# NOTE: Used by 'rstudioapi'; unofficially part of the API.
.rs.addFunction("isDesktop", function()
{
   identical(.Call("rs_rstudioProgramMode", PACKAGE = "(embedding)"), "desktop")
})

# complete url with path
.rs.addFunction("completeUrl", function(url, path)
{
  .Call("rs_completeUrl", url, path, PACKAGE = "(embedding)")
})

.rs.addFunction("defaultHttpUserAgent", function()
{
   fields <- c(
      format(getRversion()),
      format(R.version$platform),
      format(R.version$arch),
      format(R.version$os)
   )

   sprintf("R (%s)", paste(fields, collapse = " "))
})

.rs.addFunction("initHttpUserAgent", function()
{
   utils <- asNamespace("utils")

   defaultAgent <- if (is.function(utils$defaultUserAgent))
      utils$defaultUserAgent()
   else
      .rs.defaultHttpUserAgent()

   if (identical(defaultAgent, getOption("HTTPUserAgent")))
   {
      info <- .rs.api.versionInfo()
      fields <- c(
         "RStudio",
         if (info$mode == "desktop") "Desktop" else "Server",
         if (!is.null(info$edition)) "Pro",
         paste("(", format(info$version), ")", sep = "")
      )

      rstudioAgent <- paste(fields, collapse = " ")
      newAgent <- paste(rstudioAgent, defaultAgent, sep = "; ")
      options(HTTPUserAgent = newAgent)
   }
})

.rs.addFunction("hasCapability", function(what)
{
   cap <- capabilities(what)
   length(cap) && cap
})

# NOTE: registered hooks will be run immediately if the
# package has already been loaded.
.rs.addFunction("registerPackageLoadHook", function(package, hook)
{
   if (package %in% loadedNamespaces())
      return(hook())

   setHook(
      hookName = packageEvent(package, "onLoad"),
      value    = hook,
      action   = "append"
   )

})

.rs.addFunction("registerPackageAttachedHook", function(package, hook)
{
   searchPathName <- paste("package", package, sep = ":")
   if (searchPathName %in% search())
      return(hook())

   setHook(
      hookName = packageEvent(package, "attach"),
      value    = hook,
      action   = "append"
   )
})

.rs.addFunction("prependToPath", function(entry)
{
   # Make sure we use native separators, and expand tildes.
   entry <- normalizePath(entry, mustWork = FALSE)

   # Get the current PATH.
   oldPath <- strsplit(Sys.getenv("PATH"), .Platform$path.sep, fixed = TRUE)[[1L]]

   # Prepend the new entry, removing it from the old PATH if is already exists.
   newPath <- c(entry, setdiff(oldPath, entry))

   # Update the PATH.
   Sys.setenv(PATH = paste(newPath, collapse = .Platform$path.sep))
})

.rs.addFunction("callSafely", function(call, args)
{
   call <- match.fun(call)
   args <- args[intersect(names(formals(call)), names(args))]
   envir <- parent.frame()
   do.call(call, args, envir = envir)
})

.rs.addFunction("attachConflicts", function(envirs)
{
   if ("conflicted" %in% loadedNamespaces())
   {
      tryCatch(
         conflicted:::conflicts_register(),
         error = function(e) {
            .rs.logErrorMessage(conditionMessage(e))
         }
      )
   }
})

.rs.addFunction("detachConflicts", function()
{
   # check for .conflicts on the search path
   pos <- which(search() == ".conflicts")
   if (length(pos) == 0)
      return(NULL)

   # empty out the .conflicts environment
   envirs <- lapply(pos, as.environment)
   for (envir in envirs)
   {
      symbols <- ls(envir = envir, all.names = TRUE)
      rm(list = symbols, envir = envir)
   }

   # return the environments
   envirs
})

.rs.addFunction("heredoc", function(text, ...)
{
   # remove leading, trailing whitespace
   trimmed <- gsub("^[^\\S\\r\\n]*\\n|\\n[^\\S\\r\\n]*$", "", text, perl = TRUE)

   # split into lines
   lines <- strsplit(trimmed, "\n", fixed = TRUE)[[1L]]

   # compute common indent
   indent <- regexpr("[^[:space:]]", lines)
   common <- min(setdiff(indent, -1L))

   # put it together
   rendered <- paste(substring(lines, common), collapse = "\n")

   # if any dots were supplied, assume they're sprintf format arguments
   dots <- eval(substitute(alist(...)))
   if (length(dots))
      rendered <- sprintf(rendered, ...)

   # return rendered text
   rendered
})

#' Wait Until
#'
#' Wait until a predicate expression returns TRUE.
#'
#' The effective timeout is approximately `retryCount * waitTimeSecs` seconds.
#' With the defaults (`retryCount = 30`, `waitTimeSecs = 1`), the timeout is
#' approximately 30 seconds.
#'
#' @param reason A description of what we're waiting for (used in timeout error message).
#' @param predicate A function returning TRUE when the wait condition is satisfied.
#' @param swallowErrors If TRUE, errors thrown by predicate are treated as FALSE.
#' @param retryCount Number of times to check the predicate before timing out.
#' @param waitTimeSecs Seconds to wait between each predicate check.
.rs.addFunction("waitUntil", function(reason,
                                      predicate,
                                      swallowErrors = FALSE,
                                      retryCount = 30L,
                                      waitTimeSecs = 1)
{
   pollForEvents <- if (isNamespaceLoaded("later"))
      later::run_now
   else
      function() {}

   callback <- if (swallowErrors)
   {
      function()
         tryCatch(predicate(), error = function(cnd) FALSE)
   }
   else
   {
      predicate
   }

   for (i in seq_len(retryCount))
   {
      pollForEvents()

      result <- callback()
      if (!identical(result, FALSE))
         return(result)

      Sys.sleep(waitTimeSecs)
   }

   stop(sprintf("timed out waiting until '%s'", reason))
})

.rs.addFunction("bugReport", function(pro = NULL)
{
   # collect information about the running version of R / RStudio
   rstudioInfo <- .rs.api.versionInfo()
   rstudioVersion <- format(rstudioInfo$long_version)

   rstudioEdition <- sprintf(
      "%s [%s]",
      if (rstudioInfo$mode == "desktop") "Desktop" else "Server",
      if (is.null(rstudioInfo$edition)) "Open Source" else toupper(rstudioInfo$edition)
   )

   rInfo <- utils::sessionInfo()
   rVersion <- rInfo$R.version$version.string
   rVersion <- sub("^R version", "", rVersion, fixed = TRUE)
   osVersion <- rInfo$running

   rInfoText <- local({
      op <- options(width = 78)
      on.exit(options(op), add = TRUE)
      paste(capture.output(print(rInfo)), collapse = "\n")
   })

   # create issue template and fill in the pieces
   template <- .rs.heredoc("
      ### System details

          RStudio Edition : %s
          RStudio Version : %s
          OS Version      : %s
          R Version       : %s

      ### Steps to reproduce the problem

      ### Describe the problem in detail

      ### Describe the behavior you expected

      ---

      <details>

      <summary>Session Info</summary>

      ``` r
      %s
      ```

      </details>
   ")

   rendered <- sprintf(template, rstudioVersion, rstudioEdition, osVersion, rVersion, rInfoText)

   if (rstudioInfo$mode == "desktop") {

      # on desktop, we copy the text directly to the clipboard
      text <- paste(rendered, collapse = "\n")
      .Call("rs_clipboardSetText", text, PACKAGE = "(embedding)")

      writeLines(.rs.heredoc("
         * The bug report template has been written to the clipboard.
         * Please paste the clipboard contents into the issue comment section,
         * and then fill out the rest of the issue details.
         *
      "))

   } else {

      # on server, we ask the user to copy the text to the clipboard
      header <- .rs.heredoc("
         <!--
         Please copy the following text to your clipboard,
         and then click 'Cancel' to close the dialog.
         -->
      ")

      # write generated text to file, then open it in an editor
      text <- c(header, "", rendered)
      file <- tempfile("rstudio-bug-report-", fileext = ".html")
      on.exit(unlink(file), add = TRUE)
      writeLines(text, con = file)
      utils::file.edit(file)

   }

   # if 'pro' wasn't supplied, then try to guess based on the running edition
   if (is.null(pro))
      pro <- !is.null(rstudioInfo$edition)

   # tell the user we're about to navigate away
   url <- if (pro) {
      "https://github.com/rstudio/rstudio-pro/issues/new"
   } else {
      "https://github.com/rstudio/rstudio/issues/new"
   }

   # notify the user
   fmt <- " * Navigating to '%s' in 3 seconds ... "
   msg <- sprintf(fmt, url)
   writeLines(msg)
   Sys.sleep(3)

   # perform navigation
   utils::browseURL(url)
})

.rs.addFunction("mapChr", function(x, f, ...)
{
   f <- match.fun(f)
   vapply(x, f, ..., FUN.VALUE = character(1))
})

.rs.addFunction("mapDbl", function(x, f, ...)
{
   f <- match.fun(f)
   vapply(x, f, ..., FUN.VALUE = double(1))
})

.rs.addFunction("mapInt", function(x, f, ...)
{
   f <- match.fun(f)
   vapply(x, f, ..., FUN.VALUE = integer(1))
})

.rs.addFunction("mapLgl", function(x, f, ...)
{
   f <- match.fun(f)
   vapply(x, f, ..., FUN.VALUE = logical(1))
})

# An R data.frame may have so-called "compact row names", where
# row names are set with an integer placeholder that defines the
# number of rows in the data.frame, without actually having a
# fully materialized vector of that length.
.rs.addFunction("hasCompactRowNames", function(data)
{
   info <- .row_names_info(data, type = 0L)
   is.integer(info) && length(info) == 2L && is.na(info[[1L]])
})

.rs.addFunction("nullCoalesce", function(...)
{
   for (i in seq_len(...length()))
   {
      value <- ...elt(i)
      if (!is.null(value))
         return(value)
   }
})

.rs.addFunction("emptyCoalesce", function(...)
{
   for (i in seq_len(...length()))
   {
      value <- ...elt(i)
      if (length(value))
         return(value)
   }
})

.rs.addFunction("restoreSearchPath", function(searchPathsFile,
                                              packagePathsFile,
                                              environmentDataDir)
{
   tryCatch(

      .rs.restoreSearchPathImpl(
         searchPathsFile,
         packagePathsFile,
         environmentDataDir
      ),

      error = function(cnd) {
         header <- "Error restoring search paths:"
         message <- paste(c(header, conditionMessage(cnd)), collapse = "\n\t")
         writeLines(message, con = stderr())
      }

   )
})

.rs.addFunction("restoreSearchPathImpl", function(searchPathsFile,
                                                  packagePathsFile,
                                                  environmentDataDir)
{
   # First, read the package paths file, and try to load those packages.
   packagePaths <- .rs.readProperties(
      path = packagePathsFile,
      delimiter = "=",
      dequote = TRUE,
      trim = TRUE
   )

   # Exclude 'base' if it was serialized, since that's always loaded.
   packagePaths[["base"]] <- NULL

   # Restore the packages.
   .rs.restorePackages(packagePaths)

   # Read the search paths file.
   searchPaths <- readLines(searchPathsFile, warn = FALSE)

   # Detach anything that's on the search path right now, but not in the search path list.
   currentSearchPaths <- setdiff(
      search(),
      c(".GlobalEnv", "tools:rstudio", "package:base", "package:tools", "package:utils")
   )

   for (entry in currentSearchPaths)
   {
      if (!entry %in% searchPaths)
      {
         detach(entry, character.only = TRUE)
      }
   }

   # Build our iteration indices.
   # - Iterate in reverse order, since 'library()' always attaches entries to the front of the search path.
   # - Iterate by index, since we use those to map certain search path elements to data files.
   indices <- seq.int(
      from = length(searchPaths) - 1L,
      to = 2L,
      by = -1L
   )

   # Using our index from above, iterate and attach packages.
   for (index in indices)
   {
      searchPathEl <- searchPaths[[index]]
      if (grepl("^package:", searchPathEl))
      {
         packageName <- substring(searchPathEl, 9L)
         library(packageName, character.only = TRUE)
      }
      else
      {
         # NOTE: Subtract by 1 to accommodate 0-based versus 1-based indexing.
         dataFilePath <- file.path(environmentDataDir, index - 1L)
         if (file.exists(dataFilePath))
            .rs.attachDataFile(dataFilePath, searchPathEl)
      }
   }

})

.rs.addFunction("restorePackages", function(packagePaths)
{
   # Use environment to track visited packages.
   visitedPackages <- new.env(parent = emptyenv())

   # Treat 'R' as already visited, so that we skip it.
   # (Some packages include R version requirements in their DESCRIPTION files.)
   visitedPackages[["R"]] <- TRUE

   # Start loading.
   for (package in names(packagePaths))
      .rs.restorePackagesImpl(package, packagePaths, visitedPackages)
})

.rs.addFunction("restorePackagesImpl", function(package,
                                                packagePaths,
                                                visitedPackages)
{
   # Check if we've already visited this package.
   if (exists(package, envir = visitedPackages))
      return()

   # Mark this package as visited.
   visitedPackages[[package]] <- TRUE

   # If we don't have a known namespace path for this package, skip it.
   # This can occur for packages which import a package in their DESCRIPTION file,
   # but don't explicitly import it in their NAMESPACE.
   #
   # In other words, such packages are loaded only on first use, which is somewhat
   # awkward -- especially since whether or not that package can be loaded does
   # depend on the library paths set at load time!
   packagePath <- packagePaths[[package]]
   if (is.null(packagePath))
      return()

   libLoc <- dirname(packagePath)
   packageDesc <- as.list(utils::packageDescription(package, lib.loc = libLoc))

   # Load any of its dependencies first.
   fields <- c("Depends", "Imports", "LinkingTo")
   for (field in fields)
   {
      splitDeps <- tools:::.split_dependencies(packageDesc[[field]])
      for (packageDep in names(splitDeps))
      {
         .rs.restorePackagesImpl(packageDep, packagePaths, visitedPackages)
      }
   }

   # Load the package.
   loadNamespace(package, lib.loc = libLoc)
})

.rs.addFunction("dequote", function(value)
{
   for (i in seq_along(value))
   {
      if (grepl("^[\x22\x27]", value[[i]], perl = TRUE))
      {
         value[[i]] <- tryCatch(
            parse(text = value[[i]])[[1L]],
            error = function(cnd) value[[i]]
         )
      }
   }

   value
})

.rs.addFunction("readProperties", function(path = NULL,
                                           text = NULL,
                                           delimiter = ":",
                                           dequote = TRUE,
                                           trim = TRUE)
{
   # disable warnings in this scope
   op <- options(warn = -1L)
   on.exit(options(op), add = TRUE)

   # read file
   text <- .rs.nullCoalesce(text, readLines(path, warn = FALSE))
   contents <- paste(text, collapse = "\n")

   # split on newlines; allow spaces to continue a value
   parts <- strsplit(contents, "\\n(?=\\S)", perl = TRUE)[[1L]]

   # remove comments and blank lines
   parts <- grep("^\\s*(?:#|$)", parts, perl = TRUE, value = TRUE, invert = TRUE)

   # split into key / value pairs
   index <- regexpr(delimiter, parts, fixed = TRUE)
   keys <- substring(parts, 1L, index - 1L)
   vals <- substring(parts, index + 1L)

   # trim whitespace when requested
   if (trim)
   {
      keys <- .rs.trimWhitespace(keys)
      vals <- gsub("\n\\s*", " ", .rs.trimWhitespace(vals), perl = TRUE)
   }

   # strip quotes if requested
   if (dequote)
   {
      keys <- .rs.dequote(keys)
      vals <- .rs.dequote(vals)
   }

   # return as named list
   storage.mode(vals) <- "list"
   names(vals) <- keys

   vals
})

.rs.addFunction("packagePaths", function()
{
   packages <- setdiff(loadedNamespaces(), "base")
   names(packages) <- packages
   lapply(packages, getNamespaceInfo, "path")
})

.rs.addFunction("startsWith", function(strings, string)
{
   if (!length(string))
      string <- ""

   n <- nchar(string)
   (nchar(strings) >= n) & (substring(strings, 1, n) == string)
})

.rs.addFunction("selectStartsWith", function(strings, string)
{
   strings[.rs.startsWith(strings, string)]
})

.rs.addFunction("endsWith", function(strings, string)
{
   if (!length(string))
      string <- ""

   nstrings <- nchar(strings)
   nstring <- nchar(string)
   (nstrings >= nstring) &
      (substring(strings, nstrings - nstring + 1, nstrings) == string)
})

.rs.addFunction("selectEndsWith", function(strings, string)
{
   strings[.rs.endsWith(strings, string)]
})

.rs.addFunction("netrcPath", function(overridePath = NULL)
{
   .rs.nullCoalesce(
      overridePath,
      getOption("netrc", default = Sys.getenv("NETRC", unset = "~/.netrc"))
   )
})

.rs.addFunction("readNetrc", function(netrcPath = NULL)
{
   # Resolve the path to the .netrc file.
   netrcPath <- .rs.netrcPath(netrcPath)
   
   info <- file.info(netrcPath, extra_cols = FALSE)
   if (is.na(info$mode))
      return(NULL)

   # Read the contents of the file.
   contents <- readLines(netrcPath, warn = FALSE)

   # Remove any 'macdef' entries within the file.
   macdefLines <- rev(grep("^\\s*macdef\\b", contents, perl = TRUE))
   if (length(macdefLines))
   {
      blankLines <- c(which(contents == ""), length(contents) + 1L)
      for (macdefLine in macdefLines)
      {
         lhs <- macdefLine
         rhs <- blankLines[blankLines > macdefLine][[1L]]
         contents <- c(
            head(contents, n = +(lhs - 1L)),
            tail(contents, n = -(rhs - 1L))
         )
      }
   }

   # Read the tokens within the file.
   tokens <- scan(
      text         = paste(contents, collapse = "\n"),
      what         = character(),
      quote        = "\"",
      comment.char = "#",
      quiet        = TRUE
   )

   # Add some blank tokens at end, to support lookahead.
   tokens <- c(tokens, "", "")

   # Make a simple token walker.
   i <- 0L; n <- length(tokens)
   pop <- function() {
      i <<- i + 1L
      tokens[[i]]
   }

   # netrc can have machine, login, password, and account fields.
   machine <- ""

   # Start parsing the .netrc entries within the file.
   stack <- .rs.stack(mode = "list")

   token <- pop()
   while (nzchar(token))
   {
      entry <- list()

      # Get the machine definition. Handle 'default' here as well.
      entry[["machine"]] <- if (token == "default")
         ""
      else if (token == "machine")
         pop()
      else
         stop("unexpected token '", token, "'; expected 'machine' or 'default'")

      # Handle the optional fields.
      token <- pop()
      while (token %in% c("login", "password", "account"))
      {
         entry[[token]] <- pop()
         token <- pop()
      }

      # Add it to the result stack.
      stack$push(entry)
   }

   # Return result as named list.
   result <- stack$data()
   names(result) <- .rs.mapChr(result, `[[`, "machine")

   result
})

.rs.addFunction("rBase64EncodeMain", function(input, table)
{
   ni <- as.integer(length(input))
   if (ni < 3L)
      return(integer())

   no <- ni %/% 3L * 4L
   output <- integer(no)

   i0 <- seq.int(1L, ni - 2L, by = 3L)
   i1 <- seq.int(2L, ni - 1L, by = 3L)
   i2 <- seq.int(3L, ni - 0L, by = 3L)

   o0 <- seq.int(1L, no - 3L, by = 4L)
   o1 <- seq.int(2L, no - 2L, by = 4L)
   o2 <- seq.int(3L, no - 1L, by = 4L)
   o3 <- seq.int(4L, no - 0L, by = 4L)

   output[o0] <- table[1L + bitwShiftR(input[i0], 2L)]

   output[o1] <- table[1L + bitwOr(
      bitwShiftL(bitwAnd(input[i0], 0x03L), 4L),
      bitwShiftR(bitwAnd(input[i1], 0xF0L), 4L)
   )]

   output[o2] <- table[1L + bitwOr(
      bitwShiftL(bitwAnd(input[i1], 0x0FL), 2L),
      bitwShiftR(bitwAnd(input[i2], 0xC0L), 6L)
   )]

   output[o3] <- table[1L + bitwAnd(input[i2], 0x3FL)]

   output
})

.rs.addFunction("rBase64EncodeRest", function(input, table)
{
   ni <- as.integer(length(input))
   remaining <- ni %% 3L
   if (remaining == 0L)
      return(integer())

   output <- rep.int(61L, 4L)
   i <- ni - remaining + 1

   output[1L] <- table[1L + bitwShiftR(input[i + 0L], 2L)]

   if (remaining == 1L)
   {
      output[2L] <- table[1L + bitwShiftL(bitwAnd(input[i + 0L], 0x03L), 4L)]
   }
   else if (remaining == 2L)
   {
      output[2L] <- table[1L + bitwOr(
         bitwShiftL(bitwAnd(input[i + 0L], 0x03L), 4L),
         bitwShiftR(bitwAnd(input[i + 1L], 0xF0L), 4L)
      )]

      output[3L] <- table[1L + bitwShiftL(bitwAnd(input[i + 1L], 0x0FL), 2L)]
   }

   output
})

.rs.addFunction("rBase64Encode", function(text)
{
   # coerce input
   input <- if (is.character(text))
      as.integer(charToRaw(text))
   else if (is.raw(text))
      as.integer(text)
   else
      stop("unexpected input type ", typeof(text))

   # build base64 table
   chars <- "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/="  # pragma: allowlist secret
   table <- as.integer(charToRaw(chars))

   # encode the data
   encoded <- c(
      .rs.rBase64EncodeMain(input, table),
      .rs.rBase64EncodeRest(input, table)
   )

   # convert back to character
   rawToChar(as.raw(encoded))
})

.rs.addFunction("computeAuthorizationHeader", function(url)
{
   # Read user's .netrc file (if any)
   netrcEntries <- .rs.readNetrc()
   if (is.null(netrcEntries))
      return("")

   # Parse the URL into its component parts.
   pattern <- paste0(
      "^",
      "(?:(?<scheme>[a-z][a-z0-9+\\-.]*)://)?",            # Scheme
      "(?:(?<user>[^:@/?#]+)(?::(?<pass>[^@/?#]*))?@)?",   # User, Password
      "(?<host>\\[[^\\]]+\\]|[^:/?#]+)",                   # Host (IPv6 in [])
      "(?::(?<port>\\d+))?",                               # Port
      "(?<path>/[^?#]*)?",                                 # Path
      "(?:\\?(?<query>[^#]*))?",                           # Query
      "(?:#(?<fragment>.*))?",                             # Fragment
      "$"
   )

   matches <- .rs.regexMatches(pattern, url)
   parts <- as.list(matches)

   # Look for valid credentials for the provided URLs.
   for (netrcEntry in netrcEntries)
   {
      if (.rs.startsWith(parts$host, netrcEntry$machine))
      {
         userPass <- paste(netrcEntry$login, netrcEntry$password, sep = ":")
         result <- paste("Basic", .rs.rBase64Encode(userPass))
         return(result)
      }
   }

   ""
})

.rs.addFunction("recordPackageSource", function(pkgPaths, pkgSrc = NULL)
{
   # Request available packages.
   db <- if (is.null(pkgSrc)) as.data.frame(
      utils::available.packages(),
      stringsAsFactors = FALSE
   )

   # Record sources for each package.
   for (pkgPath in pkgPaths)
      .rs.recordPackageSourceImpl(pkgPath, pkgSrc, db)
})

.rs.addFunction("recordPackageSourceImpl", function(pkgPath, pkgSrc, db)
{
   # Infer package name from installed path.
   pkgName <- basename(pkgPath)

   # Read the package's DESCRIPTION file.
   pkgDesc <- packageDescription(pkgName, lib.loc = dirname(pkgPath))

   # If the package already has some remote fields recorded, then skip.
   # Currently, this is relevant for packages installed from R-universe.
   remotes <- grep("^Remote", names(pkgDesc), value = TRUE)
   if (length(remotes))
      return()

   remoteFields <- if (!is.null(pkgSrc))
   {
      c(
         RemoteType   = "local",
         RemotePkgRef = sprintf("local::%s", pkgSrc),
         RemoteUrl    = normalizePath(pkgSrc, winslash = "/", mustWork = TRUE)
      )
   }
   else
   {
      # Try to figure out post-hoc from where the package was retrieved.
      pkgEntry <- subset(db, Package == pkgName)
      if (nrow(pkgEntry) == 0L)
         return()

      # Grab the package version.
      pkgVersion <- pkgDesc[["Version"]]

      # Normalize the repository path, removing source / binary suffixes.
      pkgSource <- gsub("/(src|bin)/.*", "", pkgEntry$Repository, perl = TRUE)

      # Also remove a potential binary component.
      pkgSource <- sub("/__[^_]+__/[^/]+/", "/", pkgSource)

      c(
         RemoteType   = "standard",
         RemotePkgRef = pkgName,
         RemoteRef    = pkgName,
         RemoteRepos  = pkgSource,
         RemoteSha    = pkgVersion
      )
   }

   remoteText <- sprintf(
      "%s: %s",
      names(remoteFields),
      unlist(remoteFields)
   )

   # Update the DESCRIPTION file. We read and write the DESCRIPTION file
   # just to avoid issues with potential trailing lines.
   descPath <- file.path(pkgPath, "DESCRIPTION")
   descContents <- readLines(descPath, warn = FALSE)
   descContents <- descContents[nzchar(descContents)]
   descContents <- c(descContents, remoteText)
   writeLines(descContents, con = descPath, useBytes = TRUE)

   # Update `Meta/package.rds`.
   metaPackagePath <- file.path(pkgPath, "Meta/package.rds")
   if (file.exists(metaPackagePath))
   {
      metaPackageInfo <- readRDS(metaPackagePath)
      metaPackageInfo[["DESCRIPTION"]][names(remoteFields)] <- remoteFields
      saveRDS(metaPackageInfo, file = metaPackagePath)
   }
})

.rs.addFunction("isReadingUserInput", function()
{
   for (i in seq_len(sys.nframe()))
   {
      fn <- sys.function(i)
      if (identical(fn, base::readline))
         return(TRUE)
   }

   FALSE
})

.rs.addFunction("markScalars", function(object)
{
   if (is.recursive(object))
      for (i in seq_along(object))
         object[[i]] <- .rs.markScalars(object[[i]])

   if (is.atomic(object) && length(object) == 1L)
      .rs.scalar(object)
   else
      object
})

.rs.addFunction("installedPackagesFileInfo", function(lib = .libPaths())
{
   pkgPaths <- list.files(lib, full.names = TRUE)
   descPaths <- file.path(pkgPaths, "DESCRIPTION")
   pkgInfo <- file.info(pkgPaths, extra_cols = FALSE)
   pkgInfo$path <- row.names(pkgInfo)
   pkgInfo
})

.rs.addFunction("installedPackagesFileInfoDiff", function(before, after)
{
   result <- merge(before, after, by = "path", all = TRUE)

   diffs <-
      (is.na(result$ctime.x) & !is.na(result$ctime.y)) |
      (is.na(result$mtime.x) & !is.na(result$mtime.y)) |
      (result$ctime.x != result$ctime.y) |
      (result$mtime.x != result$mtime.y)

   result[which(diffs), ]
})

.rs.addFunction("stack", function(mode = "list")
{
   .data <- list()
   storage.mode(.data) <- mode

   list(

      push = function(...) {
         dots <- list(...)
         for (data in dots) {
            if (is.null(data))
               .data[length(.data) + 1] <<- list(NULL)
            else
               .data[[length(.data) + 1]] <<- data
         }
      },

      pop = function() {
         item <- .data[[length(.data)]]
         length(.data) <<- length(.data) - 1
         item
      },

      peek = function() {
         .data[[length(.data)]]
      },

      contains = function(data) {
         data %in% .data
      },

      empty = function() {
         length(.data) == 0
      },

      get = function(index) {
         if (index <= length(.data)) .data[[index]]
      },

      set = function(index, value) {
         .data[[index]] <<- value
      },

      clear = function() {
         .data <<- list()
      },

      data = function() {
         .data
      }

   )

})

.rs.addFunction("isAbsolutePath", function(path)
{
   grepl("^(?:/|~|[a-zA-Z]:[/\\])", path)
})

.rs.addFunction("readPackageDescription", function(packagePath)
{
   info <- file.info(packagePath, extra_cols = FALSE)
   if (identical(info$isdir, TRUE))
      .rs.readPackageDescriptionFromDirectory(packagePath)
   else
      .rs.readPackageDescriptionFromArchive(packagePath)
})

.rs.addFunction("readPackageDescriptionFromDirectory", function(packagePath)
{
   # if this is an installed package with a package metafile,
   # read from that location
   metapath <- file.path(packagePath, "Meta/package.rds")
   if (file.exists(metapath)) {
      metadata <- readRDS(metapath)
      return(as.list(metadata$DESCRIPTION))
   }
   
   # otherwise, attempt to read DESCRIPTION directly
   descPath <- file.path(packagePath, "DESCRIPTION")
   read.dcf(descPath, all = TRUE)
})

.rs.addFunction("readPackageDescriptionFromArchive", function(packagePath)
{
   # figure out what files are in the archive
   isZip <- .rs.endsWith(packagePath, ".zip")
   files <- if (isZip)
      unzip(packagePath, list = TRUE)[["Name"]]
   else
      untar(packagePath, list = TRUE)
   
   # infer the path to the DESCRIPTION file we want to extract
   descPaths <- grep("(?:^|/)DESCRIPTION$", files, perl = TRUE, value = TRUE)
   
   # a package might have multiple DESCRIPTION files;
   # we want the top-level DESCRIPTION file
   n <- nchar(descPaths)
   descPath <- descPaths[n == min(n)]
   
   # extract to temporary directory
   tmpdir <- tempfile("description-")
   dir.create(tmpdir, recursive = TRUE)
   on.exit(unlink(tmpdir, recursive = TRUE), add = TRUE)
   
   if (isZip)
      unzip(packagePath, files = descPath, exdir = tmpdir)
   else
      untar(packagePath, files = descPath, exdir = tmpdir)
   
   # read the extracted DESCRIPTION file
   tmpDescPath <- file.path(tmpdir, descPath)
   read.dcf(tmpDescPath, all = TRUE)
})

.rs.addFunction("askForRestart", function(reason)
{
   testing <- getOption("rstudio.tests.running", default = FALSE)
   if (testing)
      return(FALSE)

   payload <- list(
      reason  = .rs.scalar(reason)
   )
   
   request <- .rs.api.createRequest(
      type    = .rs.api.eventTypes$TYPE_ASK_FOR_RESTART,
      sync    = TRUE,
      target  = .rs.api.eventTargets$TYPE_ACTIVE_WINDOW,
      payload = payload
   )
   
   response <- .rs.api.sendRequest(request)
   response[["value"]]
})

.rs.addFunction("ifElse", function(condition, consequence, alternative)
{
   if (condition) consequence else alternative
})

.rs.addFunction("safeEval", function(expr, envir)
{
   # Allow evaluation of symbols
   if (is.symbol(expr))
      return(eval(expr, envir))
   
   # Allow evaluation of `::` calls
   ok <-
      is.call(expr) &&
      length(expr) == 3L &&
      is.symbol(expr[[1L]]) &&
      as.character(expr[[1L]]) %in% c("::", ":::") &&
      is.symbol(expr[[2L]]) &&
      is.symbol(expr[[3L]])
   
   if (ok)
      return(eval(expr, envir))
   
   # Disallow other evaluation
   NULL
})

# Hooks -------------------------------------------------------------------

assign(".rs.downloadFile", utils::download.file, envir = .rs.toolsEnv())

.rs.defineGlobalHook(
   package = "utils",
   binding = "download.file",
   when    = getRversion() < "4.6.0" && "headers" %in% names(formals(utils::download.file)),
   function(url, destfile, method)
   {
      ""
      "This is an RStudio hook."
      "Use `.rs.downloadFile` to bypass this hook if necessary."
      ""

      # Note that R also supports downloading multiple files in parallel,
      # so the 'url' parameter may be a vector of URLs.
      #
      # Unfortunately, it doesn't support the use of URL-specific headers,
      # so we try to handle this appropriately here.
      if (missing(method))
         method <- getOption("download.file.method", default = "auto")

      # Silence diagnostic warnings.
      headers <- get("headers", envir = environment(), inherits = FALSE)

      # Handle the simpler length-one URL case up front.
      if (length(url) == 1L)
      {
         # Build relevant headers for the call.
         callHeaders <- headers
         authHeader <- .rs.computeAuthorizationHeader(url)
         if (length(authHeader) && nzchar(authHeader))
            callHeaders <- c(callHeaders, Authorization = authHeader)

         # Build a call to invoke the base R downloader.
         call <- match.call(expand.dots = TRUE)
         call[[1L]] <- quote(.rs.downloadFile)
         if (length(callHeaders))
            call["headers"] <- list(callHeaders)
         status <- eval(call, envir = parent.frame())
         return(invisible(status))
      }

      # Otherwise, do some more work to map headers to URLs as appropriate.
      retvals <- vector("integer", length = length(url))
      authHeaders <- .rs.mapChr(url, .rs.computeAuthorizationHeader)
      for (authHeader in unique(authHeaders))
      {
         # Figure out which URLs are associated with the current header.
         idx <- which(authHeaders == authHeader)

         # Build relevant headers for the call.
         callHeaders <- headers
         if (length(authHeader) && nzchar(authHeader))
            callHeaders <- c(callHeaders, Authorization = authHeader)

         # Build a call to download these files all in one go.
         call <- match.call(expand.dots = TRUE)
         call[[1L]] <- quote(.rs.downloadFile)
         call["url"] <- list(url[idx])
         call["destfile"] <- list(destfile[idx])
         if (length(callHeaders))
            call["headers"] <- list(callHeaders)
         retvals[idx] <- eval(call, envir = parent.frame())
      }

      # Note that even if multiple files are downloaded, R only reports
      # a single status code, with 0 implying that all downloads succeeded.
      status <- if (all(retvals == 0L)) 0L else 1L
      if (getRversion() >= "4.5.0")
         attr(status, "retvals") <- retvals
      invisible(status)

   })

.rs.defineHook(
   package = "utils",
   binding = "install.packages",
   function(pkgs, lib, repos)
   {
      ""
      "This is an RStudio hook."
      "Use `utils::install.packages()` to bypass this hook if necessary."
      ""

      if (interactive())
      {
         # Check if package installation was disabled in this version of RStudio.
         canInstallPackages <- .Call("rs_canInstallPackages", PACKAGE = "(embedding)")
         if (!canInstallPackages)
         {
            msg <- "Package installation is disabled in this version of RStudio."
            stop(msg, call. = FALSE)
         }

         # Notify if we're about to update an already-loaded package.
         # Skip this within renv and packrat projects.
         if (.rs.installPackagesRequiresRestart(pkgs))
         {
            response <- .rs.askForRestart("install.packages")
            if (identical(response, TRUE))
            {
               call <- do.call(substitute, list(match.call(), parent.frame()))
               call[[1L]] <- quote(install.packages)
               names(call)[[2L]] <- ""
               command <- paste(.rs.deparseCall(call), collapse = " ")
               
               .rs.enqueLoadedPackageUpdates(command)
               invokeRestart("abort")
            }
            else if (identical(response, FALSE))
            {
               # fall-through
            }
            else
            {
               invokeRestart("abort")
            }
         }

         # Make sure Rtools is on the PATH for Windows.
         .rs.addRToolsToPath()
         on.exit(.rs.restorePreviousPath(), add = TRUE)
      }

      # Resolve library path.
      if (missing(lib) || is.null(lib))
         lib <- .libPaths()[1L]

      # Check if we're installing a package from the filesystem,
      # versus installing a package from CRAN.
      isLocal <- is.null(repos) || any(grepl("/", pkgs, fixed = TRUE))

      if (isLocal)
      {
         # Invoke the original function.
         call <- sys.call()
         call[[1L]] <- quote(utils::install.packages)
         result <- eval(call, envir = parent.frame())

         # Record the package source. Note that we need to resolve the path
         # to the newly-installed package post-hoc from the provided path.
         shouldRecord <- is.character(pkgs) && length(pkgs) == 1L
         if (shouldRecord)
         {
            pkgDesc <- tryCatch(
               .rs.readPackageDescription(pkgs),
               error = function(cnd) NULL
            )
            
            if (length(pkgDesc))
            {
               pkgPath <- file.path(lib, pkgDesc[["Package"]])
               if (file.exists(pkgPath))
               {
                  tryCatch(
                     .rs.recordPackageSource(pkgPath, pkgs),
                     error = .rs.logWarningMessage
                  )
               }
            }
         }
      }
      else
      {
         # Get paths to DESCRIPTION files, so we can see what packages
         # were updated before and after installation.
         before <- .rs.installedPackagesFileInfo(lib)

         # Invoke the original function.
         call <- sys.call()
         call[[1L]] <- quote(utils::install.packages)
         result <- eval(call, envir = parent.frame())

         # Check and see what packages were updated.
         after <- .rs.installedPackagesFileInfo(lib)

         # Figure out which packages were changed.
         rows <- .rs.installedPackagesFileInfoDiff(before, after)

         # For any packages which appear to have been updated,
         # tag their DESCRIPTION file with their installation source.
         .rs.recordPackageSource(rows$path)
      }

      # Notify the front-end that we've made some updates.
      if (interactive())
      {
         .rs.updatePackageEvents()
         .Call("rs_packageLibraryMutated", PACKAGE = "(embedding)")
      }

      # Return installation result, invisibly.
      invisible(result)
   })

.rs.defineHook(
   package = "utils",
   binding = "remove.packages",
   function(pkgs, lib)
   {
      ""
      "This is an RStudio hook."
      "Use `utils::remove.packages()` to bypass this hook if necessary."
      ""

      # Invoke original.
      result <- utils::remove.packages(pkgs, lib)

      # Notify front-end that the package library was mutated.
      if (interactive())
      {
         .Call("rs_packageLibraryMutated", PACKAGE = "(embedding)")
      }

      # Return original result.
      invisible(result)
   })

