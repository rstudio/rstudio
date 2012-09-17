#
# Tools.R
#
# Copyright (C) 2009-11 by RStudio, Inc.
#
# This program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

# target environment for rstudio supplemental tools
.rs.Env <- attach(NULL,name="tools:rstudio")

# add a function to the tools:rstudio environment
assign( envir = .rs.Env, ".rs.addFunction", function(name, FN)
{ 
   fullName = paste(".rs.", name, sep="")
   assign(fullName, FN, .rs.Env)
   environment(.rs.Env[[fullName]]) <- .rs.Env
})

# add a global (non-scoped) function to the tools:rstudio environment
assign( envir = .rs.Env, ".rs.addGlobalFunction", function(name, FN)
{ 
   assign(name, FN, .rs.Env)
   environment(.rs.Env[[name]]) <- .rs.Env
})

assign( envir = .rs.Env, ".rs.setVar", function(name, var)
{ 
   fullName = paste(".rs.", name, sep="")
   assign(fullName, var, .rs.Env)
   environment(.rs.Env[[fullName]]) <- .rs.Env
})

.rs.addFunction( "evalInGlobalEnv", function(code)
{
   eval(parse(text=code), envir=globalenv())
})

# save current state of options() to file
.rs.addFunction( "saveOptions", function(filename)
{
   opt = options();
   save(opt, file=filename)
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
  require(utils)

  # determine devmode path (devtools <= 0.6 hard-coded it)
  devToolsPath <- getOption("devtools.path")
  if (is.null(devToolsPath))
    if ("devtools" %in% .packages())
      devToolsPath <- "~/R-dev"

  # no devtools path
  if (is.null(devToolsPath))
    return (FALSE)

  # is the devtools path active?
  devToolsPath <- normalizePath(devToolsPath, winslash = "/", mustWork = FALSE)
  devToolsPath %in% .libPaths()
})

# load a package by name
.rs.addFunction( "loadPackage", function(packageName)
{
   library(packageName, character.only = TRUE)
})

# unload a package by name
.rs.addFunction( "unloadPackage", function(packageName)
{
   pkg = paste("package:", packageName, sep="")
   detach(pos = match(pkg, search()))
})

# save an environment to a file
.rs.addFunction( "saveEnvironment", function(env, filename)
{
   save(list = ls(envir = env, all.names = TRUE),
        file = filename,
        envir = env)
   
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

# record an object to a file
.rs.addFunction( "saveGraphicsSnapshot", function(snapshot, filename)
{
   # make a copy of the snapshot into plot and set its metadata in a way
   # that is compatible with recordPlot
   plot = snapshot
   attr(plot, "version") <- grDevices:::rversion()
   class(plot) <- "recordedplot"
   
   save(plot, file=filename)
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
   load(filename)
   
   # restore native symbols for R >= 2.14
   if (getRversion() >= "2.14")
   {
     try({
       for(i in 1:length(plot[[1]])) 
       {
         if("NativeSymbolInfo" %in% class(plot[[1]][[i]][[2]][[1]]))
         {
           nativeSymbol <-getNativeSymbolInfo(plot[[1]][[i]][[2]][[1]]$name);
           plot[[1]][[i]][[2]][[1]] <- nativeSymbol;         
         }
       }
     },
     silent = TRUE);
   }
   
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

# view a pdf (based on implementation in RShowDoc and print.vignette)
.rs.addFunction( "shellViewPdf", function(path)
{
   if(.Platform$OS.type == "windows")
   {
      require(utils, quietly=TRUE)
      shell.exec(normalizePath(path))
   }
   else if (identical(substr(.Platform$pkgType, 1L, 10L), "mac.binary"))
	  system(paste("open", "-a", "Preview", shQuote(path)), wait = FALSE)
   else
      system(paste(shQuote(getOption("pdfviewer")), shQuote(path)), wait = FALSE)
})


# hook an internal R function
.rs.addFunction( "registerHook", function(name, package, hookFactory)
{
   # get the original function  
   packageName = paste("package:", package, sep="")
   original <- get(name, packageName, mode="function")
   
   # install the hook
   if (!is.null(original))
   {
      # new function definition
      new <- hookFactory(original)
      
      # re-map function 
      packageEnv = as.environment(packageName)
      unlockBinding(name, packageEnv)
      assign(name, new, packageName)
      lockBinding(name, packageEnv)
   }
   else
   {
      stop(cat("function", name, "not found\n"))
   }
})

.rs.addFunction( "callAs", function(name, f, ...)
{
   # TODO: figure out how to print the args (...) as part of the message
   
   # run the original function (f). setup condition handlers soley so that
   # we can correctly print the name of the function called in error
   # and warning messages -- otherwise R prints "original(...)"
   withCallingHandlers(tryCatch(f(...), 
                                error=function(e)
                                {
                                   cat("Error in ", name, " : ", e$message, 
                                       "\n", sep="")
                                }),
                       warning=function(w)
                       {
                          cat("Warning in ", name, " :\n  ",  w$message, 
                              "\n", sep="")
                          invokeRestart("muffleWarning")
                       })
})

# replacing an internal R function
.rs.addFunction( "registerReplaceHook", function(name, package, hook)
{
   hookFactory <- function(original) function(...) .rs.callAs(name,
                                                             hook, 
                                                             original,
                                                             ...);
   .rs.registerHook(name, package, hookFactory);
})

# notification that an internal R function was called
.rs.addFunction( "registerNotifyHook", function(name, package, hook)
{
   hookFactory <- function(original) function(...) 
   { 
      # call hook after original is executed
      on.exit(hook(...))
      
      # call original
      .rs.callAs(name, original, ...)
   }
   .rs.registerHook(name, package, hookFactory);
})

# marking functions in R packages as unsupported
.rs.addFunction( "registerUnsupported", function(name, package)
{
   unsupported <- function(...) 
   {  
      stop("function not supported in RStudio\n")
   }
                                              
   .rs.registerReplaceHook(name, package, unsupported)
})

.rs.addFunction( "setCRANRepos", function(reposUrl)
{
  local({
      r <- getOption("repos");
      r["CRAN"] <- reposUrl;
      options(repos=r)
    })
})

.rs.addFunction( "setMemoryLimit", function(limit)
{
   suppressWarnings(utils::memory.limit(limit))
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
   # must be single string
   if (!is.character(lib) || (length(lib) > 1L))
      stop("lib must be single element character vector")
      
   # implementation based on install.packages
   ok <- file.info(lib)$isdir & (file.access(lib, 2) == 0)
   if (.Platform$OS.type == "windows") 
   {
      ok <- file.info(lib)$isdir %in% TRUE
      if (ok) 
      {
         fn <- file.path(lib, paste("_test_dir", Sys.getpid(), sep = "_"))
         unlink(fn, recursive = TRUE)
         res <- try(dir.create(fn, showWarnings = FALSE))
         if (inherits(res, "try-error") || !res)
            ok <- FALSE
         else 
            unlink(fn, recursive = TRUE)
      }
   } 
   return (ok)
})

# based on code in install.packages
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
   fullName = paste("rpc.", name, sep="")
   .rs.addFunction(fullName, FN)
})

# list all rpc handlers in the tools:rstudio environment
.rs.addFunction( "listJsonRpcHandlers", function()
{
   rpcHandlers <- objects("tools:rstudio", 
                          all.names=TRUE, 
                          pattern=utils:::glob2rx(".rs.rpc.*"))
   return (rpcHandlers)
})










