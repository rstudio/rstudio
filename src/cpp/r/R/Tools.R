#
# Tools.R
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
   .rs.addFunction(fullName, FN, envir = globalenv())
})

.rs.addFunction("setVar", function(name, var)
{ 
   envir <- .rs.toolsEnv()
   fullName <- paste(".rs.", name, sep = "")
   environment(var) <- envir
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
   status <- try(load(path, envir = .GlobalEnv), silent = TRUE)
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
.rs.addFunction( "saveOptions", function(filename)
{
   opt = options();
   suppressWarnings(save(opt, file=filename))
})

# restore options() from file
.rs.addFunction( "restoreOptions", function(filename)
{
   load(filename)
   options(opt)
})

# save current state of .libPaths() to file
.rs.addFunction( "saveLibPaths", function(filename)
{
  libPaths = .libPaths();
  save(libPaths, file=filename)
})

# restore .libPaths() from file
.rs.addFunction( "restoreLibPaths", function(filename)
{
  load(filename)
  .libPaths(libPaths)
})


# try to determine if devtools::dev_mode is on
.rs.addFunction( "devModeOn", function(){
   
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

# load a package by name
.rs.addFunction( "loadPackage", function(packageName, lib)
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
.rs.addFunction( "unloadPackage", function(packageName)
{
   pkg = paste("package:", packageName, sep="")
   detach(pos = match(pkg, search()))
})

.rs.addFunction("getPackageVersion", function(packageName)
{
   package_version(utils:::packageDescription(packageName, 
                                              fields="Version"))   
})

# save an environment to a file
.rs.addFunction( "saveEnvironment", function(env, filename)
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

.rs.addFunction( "disableSaveCompression", function()
{
  options(save.defaults=list(ascii=FALSE, compress=FALSE))
  options(save.image.defaults=list(ascii=FALSE, safe=TRUE, compress=FALSE))
})

.rs.addFunction( "attachDataFile", function(filename, name, pos = 2)
{
   if (!file.exists(filename)) 
      stop(gettextf("file '%s' not found", filename), domain = NA)
   
   .Internal(attach(NULL, pos, name))
   load(filename, envir = as.environment(pos)) 
   
   invisible (NULL)
})

.rs.addGlobalFunction( "RStudioGD", function()
{
   .Call("rs_createGD")
})

# set our graphics device as the default and cause it to be created/set
.rs.addFunction( "initGraphicsDevice", function()
{
   options(device="RStudioGD")
   grDevices::deviceIsInteractive("RStudioGD")
})

.rs.addFunction( "activateGraphicsDevice", function()
{
   invisible(.Call("rs_activateGD"))
})

.rs.addFunction( "newDesktopGraphicsDevice", function()
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
.rs.addFunction( "saveGraphicsSnapshot", function(snapshot, filename)
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
.rs.addFunction( "saveGraphics", function(filename)
{
   plot = grDevices::recordPlot()
   save(plot, file=filename)
})

# restore an object from a file
.rs.addFunction( "restoreGraphics", function(filename)
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
   
   # we suppressWarnings so that R doesnt print a warning if we restore
   # a plot saved from a previous version of R (which will occur if we 
   # do a resume after upgrading the version of R on the server)
   suppressWarnings(grDevices::replayPlot(plot))
   
})

# generate a uuid
.rs.addFunction( "createUUID", function()
{
  .Call("rs_createUUID")
})

# check the current R architecture
.rs.addFunction( "getRArch", function()
{
   .Platform$r_arch
})

# pager
.rs.addFunction( "pager", function(files, header, title, delete.file)
{
   for (i in 1:length(files)) {
      if ((i > length(header)) || !nzchar(header[[i]]))
         fileTitle <- title
      else
         fileTitle <- header[[i]]

      .Call("rs_showFile", fileTitle, files[[i]], delete.file)
   }
})

# alias for normalizePath function
.rs.addFunction("normalizePath", normalizePath)

# alias for path.package function
.rs.addFunction("pathPackage", path.package)

# handle viewing a pdf differently on each platform:
#  - windows: shell.exec
#  - mac: Preview
#  - linux: getOption("pdfviewer")
.rs.addFunction( "shellViewPdf", function(path)
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


# hook an internal R function
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
   
   # re-map function 
   packageEnv <- as.environment(packageName)
   unlockBinding(name, packageEnv)
   assign(name, new, packageName)
   lockBinding(name, packageEnv)
   
   # remap in function namespace if requested as well
   if (namespace) {
      ns <- asNamespace(package)
      if (exists(name, envir = ns, mode = "function")) {
         unlockBinding(name, ns)
         assign(name, new, envir = ns)
         lockBinding(name, ns)
      }
   }
   
})

.rs.addFunction( "callAs", function(name, f, ...)
{
   # TODO: figure out how to print the args (...) as part of the message
   
   # run the original function (f). setup condition handlers soley so that
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
.rs.addFunction( "registerReplaceHook", function(name, package, hook, namespace = FALSE)
{
   hookFactory <- function(original) function(...) .rs.callAs(name,
                                                             hook, 
                                                             original,
                                                             ...);
   .rs.registerHook(name, package, hookFactory, namespace);
})

# notification that an internal R function was called
.rs.addFunction( "registerNotifyHook", function(name, package, hook, namespace = FALSE)
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
.rs.addFunction( "registerUnsupported", function(name, package, alternative = "")
{
   unsupported <- function(...) 
   {  
      msg <- "function not supported in RStudio"
      if (nzchar(alternative))
        msg <- paste(msg, "(try", alternative, "instead)")
      msg <- paste(msg, "\n", sep="")
      stop(msg)
   }
                                              
   .rs.registerReplaceHook(name, package, unsupported)
})

.rs.addFunction( "parseCRANReposList", function(repos) {
  parts <- strsplit(repos, "\\|")[[1]]
  indexes <- seq_len(length(parts) / 2)

  r <- list()
  for (i in indexes)
    r[[parts[[2 * i - 1]]]] <- parts[[2 * i]]

  r
})

.rs.addFunction( "setCRANRepos", function(cran, secondary)
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

.rs.addFunction( "setCRANReposAtStartup", function(cran, secondary)
{
   # check whether the user has already set a CRAN repository
   # in their .Rprofile
   repos = getOption("repos")
   cranMirrorConfigured <- !is.null(repos) && !any(repos == "@CRAN@")

   if (!cranMirrorConfigured)
      .rs.setCRANRepos(cran, secondary)
})


.rs.addFunction( "isCRANReposFromSettings", function()
{
   !is.null(attr(getOption("repos"), "RStudio"))
})


.rs.addFunction( "setCRANReposFromSettings", function(cran, secondary)
{
   # only set the repository if the repository was set by us
   # in the first place (it wouldn't be if the user defined a
   # repository in .Rprofile or called setRepositories directly)
   if (.rs.isCRANReposFromSettings())
      .rs.setCRANRepos(cran, secondary)
})


.rs.addFunction( "libPathsAppend", function(path)
{
   # remove it if it already exists
   .libPaths(.libPaths()[.libPaths() != path])

   # append it
   .libPaths(append(.libPaths(), path))
})


.rs.addFunction( "isLibraryWriteable", function(lib)
{
   file.exists(lib) && (file.access(lib, 2) == 0)
})

.rs.addFunction( "defaultLibPathIsWriteable", function()
{
   .rs.isLibraryWriteable(.libPaths()[1L])
})

.rs.addFunction( "disableQuartz", function()
{
  .rs.registerReplaceHook("quartz", "grDevices", function(...) {
    stop(paste("RStudio does not support the quartz device in R <= 2.11.1.",
               "Please upgrade to a newer version of R to use quartz."))
  })
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
.rs.addFunction( "addJsonRpcHandler", function(name, FN)
{
   fullName <- paste("rpc.", name, sep = "")
   .rs.addFunction(fullName, FN, TRUE)
})

# list all rpc handlers in the tools:rstudio environment
.rs.addFunction( "listJsonRpcHandlers", function()
{
   rpcHandlers <- objects("tools:rstudio", 
                          all.names=TRUE, 
                          pattern=utils:::glob2rx(".rs.rpc.*"))
   return (rpcHandlers)
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


.rs.registerReplaceHook("history", "utils", function(original, ...) {
   invisible(.Call("rs_activatePane", "history"))
})

.rs.addFunction("registerHistoryFunctions", function() {
  
  # loadhistory
  .rs.registerReplaceHook("loadhistory", "utils", function(original, 
                                                           file = ".Rhistory")
  {
    invisible(.Call("rs_loadHistory", file))
  })
  
  # savehistory
  .rs.registerReplaceHook("savehistory", "utils", function(original, 
                                                           file = ".Rhistory")
  {
    invisible(.Call("rs_saveHistory", file))
  })

  # timestamp
  .rs.registerReplaceHook("timestamp", "utils", function(
    original,
    stamp = date(),
    prefix = "##------ ",
    suffix = " ------##",
    quiet = FALSE)
  {
    stamp <- paste(prefix, stamp, suffix, sep = "")

    lapply(stamp, function(s) {
      invisible(.Call("rs_timestamp", s))
    })

    if (!quiet)
        cat(stamp, sep = "\n")

    invisible(stamp)
  }, namespace = TRUE)
})


.rs.addFunction("parseQuitArguments", function(command) {
  
  # parse the command
  expr <- parse(text=command)
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
   params = readRDS(inputLocation)
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
   substring(x, starts, ends)
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
        mergedList[[name]] <- merge_lists(base, overlay)
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
   result <- do.call(mapply, c(c, data, USE.NAMES = FALSE, SIMPLIFY = FALSE))
   names(result) <- names(data[[1]])
   as.data.frame(result, stringsAsFactors = FALSE)
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
   
   do.call("unlockBinding", list(binding, namespace))
   assign(binding, override, envir = namespace)
   do.call("lockBinding", list(binding, namespace))
   
   # if package is attached, override there as well
   searchPathName <- paste("package", package, sep = ":")
   if (searchPathName %in% search()) {
      env <- as.environment(searchPathName)
      if (exists(binding, envir = env)) {
         do.call("unlockBinding", list(binding, env))
         assign(binding, override, envir = env)
         do.call("lockBinding", list(binding, env))
      }
   }
   
   # return original
   original
})

.rs.addFunction("isDesktop", function() {
   identical(.Call("rs_rstudioProgramMode"), "desktop")
})

# complete url with path
.rs.addFunction("completeUrl", function(url, path)
{
  .Call("rs_completeUrl", url, path)
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

.rs.addFunction("initTools", function()
{
   ostype <- .Platform$OS.type
   info <- Sys.info()
   envir <- .rs.toolsEnv()
   
   assign(".rs.platform.isUnix",    ostype == "unix",              envir = envir)
   assign(".rs.platform.isWindows", ostype == "windows",           envir = envir)
   assign(".rs.platform.isLinux",   info[["sysname"]] == "Linux",  envir = envir)
   assign(".rs.platform.isMacos",   info[["sysname"]] == "Darwin", envir = envir)
   
})

.rs.initTools()
