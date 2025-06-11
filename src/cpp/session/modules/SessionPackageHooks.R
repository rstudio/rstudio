#
# SessionPackageHooks.R
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

assign(".rs.downloadFile", utils::download.file, envir = .rs.toolsEnv())

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

.rs.defineGlobalHook(
   package = "utils",
   binding = "download.file",
   when    = "headers" %in% names(formals(utils::download.file)),
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
      authHeader <- .rs.computeAuthorizationHeader(url)
      call <- match.call(expand.dots = TRUE)
      call[[1L]] <- quote(.rs.downloadFile)
      call["headers"] <- list(c(headers, Authorization = authHeader))
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
      
      # Build a call to download these files all in one go.
      call <- match.call(expand.dots = TRUE)
      call[[1L]] <- quote(.rs.downloadFile)
      call["url"] <- list(url[idx])
      call["destfile"] <- list(destfile[idx])
      call["headers"] <- list(c(headers, Authorization = authHeader))
      retvals[idx] <- eval(call, envir = parent.frame())
   }
   
   # Note that even if multiple files are downloaded, R only reports
   # a single status code, with 0 implying that all downloads succeeded.
   status <- if (all(retvals == 0L)) 0L else 1L
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
      call <- sys.call()
      call[[1L]] <- quote(install.packages)
      command <- .rs.deparseCall(call)
      
      .rs.enqueLoadedPackageUpdates(command)
      invokeRestart("abort")
   }
   
   # Make sure Rtools is on the PATH for Windows.
   .rs.addRToolsToPath()
   on.exit(.rs.restorePreviousPath(), add = TRUE)
   
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
      
      # Record the package source.
      shouldRecord <- is.character(pkgs) && length(pkgs) == 1L
      if (shouldRecord)
         .rs.recordPackageSource(pkgs, local = TRUE)
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
      .rs.recordPackageSource(rows$path, local = FALSE)
   }
   
   # Notify the front-end that we've made some updates.
   .rs.updatePackageEvents()
   .Call("rs_packageLibraryMutated", PACKAGE = "(embedding)")
   
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
   .Call("rs_packageLibraryMutated", PACKAGE = "(embedding)")
   
   # Return original result.
   invisible(result)
})
