#
# SessionPackages.R
#
# Copyright (C) 2009-12 by RStudio, Inc.
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

# a vectorized function that takes any number of paths and aliases the home
# directory in those paths (i.e. "/Users/bob/foo" => "~/foo"), leaving any 
# paths outside the home directory untouched
.rs.addFunction("createAliasedPath", function(path)
{
   homeDir <- path.expand("~/")
   homePathIdx <- substr(path, 1, nchar(homeDir)) == homeDir
   homePaths <- path[homePathIdx]
   path[homePathIdx] <-
          paste("~", substr(homePaths, nchar(homeDir), nchar(homePaths)), sep="")
   path
})

# Some R commands called during packaging-related operations (such as untar)
# delegate to the system tar binary specified in TAR. On OS X, R may set TAR to
# /usr/bin/gnutar, which exists prior to Mavericks (10.9) but not in later
# rleases of the OS. In the special case wherein the TAR environment variable
# on OS X is set to a non-existant gnutar and there exists a tar at
# /usr/bin/tar, tell R to use that binary instead.
if (identical(as.character(Sys.info()["sysname"]), "Darwin") &&
    identical(Sys.getenv("TAR"), "/usr/bin/gnutar") && 
    !file.exists("/usr/bin/gnutar") &&
    file.exists("/usr/bin/tar"))
{
   Sys.setenv(TAR = "/usr/bin/tar")
}

.rs.addFunction( "updatePackageEvents", function()
{
   reportPackageStatus <- function(status)
      function(pkgname, ...)
      {
         packageStatus = list(name=pkgname,
                              path=.rs.createAliasedPath(
                                     .rs.pathPackage(pkgname, quiet=TRUE)),
                              loaded=status)
         .rs.enqueClientEvent("package_status_changed", packageStatus)
      }
   
   notifyPackageLoaded <- function(pkgname, ...)
   {
      .Call("rs_packageLoaded", pkgname)
   }

   notifyPackageUnloaded <- function(pkgname, ...)
   {
      .Call("rs_packageUnloaded", pkgname)
   }
   
   # NOTE: `list.dirs()` was introduced with R 2.13 but was buggy until 3.0
   # (the 'full.names' argument was not properly respected)
   pkgNames <- if (getRversion() >= "3.0.0")
      base::list.dirs(.libPaths(), full.names = FALSE, recursive = FALSE)
   else
      .packages(TRUE)
   
   sapply(pkgNames, function(packageName)
   {
      if ( !(packageName %in% .rs.hookedPackages) )
      {
         attachEventName = packageEvent(packageName, "attach")
         setHook(attachEventName, reportPackageStatus(TRUE), action="append")
         
         loadEventName = packageEvent(packageName, "onLoad")
         setHook(loadEventName, notifyPackageLoaded, action="append")

         unloadEventName = packageEvent(packageName, "onUnload")
         setHook(unloadEventName, notifyPackageUnloaded, action="append")
             
         detachEventName = packageEvent(packageName, "detach")
         setHook(detachEventName, reportPackageStatus(FALSE), action="append")
          
         .rs.setVar("hookedPackages", append(.rs.hookedPackages, packageName))
      }
   })
})

.rs.addFunction( "packages.initialize", function()
{  
   # list of packages we have hooked attach/detach for
   .rs.setVar( "hookedPackages", character() )

   # set flag indicating we should not ignore loadedPackageUpdates checks
   .rs.setVar("ignoreNextLoadedPackageCheck", FALSE)
    
   # ensure we are subscribed to package attach/detach events
   .rs.updatePackageEvents()
   
   # whenever a package is installed notify the client and make sure
   # we are subscribed to its attach/detach events
   .rs.registerReplaceHook("install.packages", "utils", function(original,
                                                                pkgs,
                                                                lib,
                                                                repos = getOption("repos"),
                                                                ...) 
   {
      if (!.Call("rs_canInstallPackages"))
      {
        stop("Package installation is disabled in this version of RStudio",
             call. = FALSE)
      }
      
      packratMode <- !is.na(Sys.getenv("R_PACKRAT_MODE", unset = NA))
      
      if (!is.null(repos) && !packratMode && .rs.loadedPackageUpdates(pkgs)) {

         # attempt to determine the install command
         installCmd <- NULL
         for (i in seq_along(sys.calls()))
         {
           if (identical(deparse(sys.call(i)[[1]]), "install.packages"))
           {
             installCmd <- gsub("\\s+"," ", 
                                paste(deparse(sys.call(i)), collapse = " "))
             break
           }
         }

         # call back into rsession to send an event to the client
         .rs.enqueLoadedPackageUpdates(installCmd)

         # throw error
         stop("Updating loaded packages")
      }

      # fixup path as necessary
      .rs.addRToolsToPath()

      # do housekeeping after we execute the original
      on.exit({
         .rs.updatePackageEvents()
         .Call("rs_packageLibraryMutated")
         .rs.restorePreviousPath()
      })

      # call original
      original(pkgs, lib, repos, ...)
   })
   
   # whenever a package is removed notify the client (leave attach/detach
   # alone because the dangling event is harmless and removing it would
   # requrie somewhat involved code
   .rs.registerReplaceHook("remove.packages", "utils", function(original,
                                                               pkgs,
                                                               lib,
                                                               ...) 
   {
      # do housekeeping after we execute the original
      on.exit(.Call("rs_packageLibraryMutated"))
                         
      # call original
      original(pkgs, lib, ...) 
   })
})

.rs.addFunction( "addRToolsToPath", function()
{
    .Call("rs_addRToolsToPath")
})

.rs.addFunction( "restorePreviousPath", function()
{
    .Call("rs_restorePreviousPath")
})

.rs.addFunction( "uniqueLibraryPaths", function()
{
   # get library paths (normalize on unix to get rid of duplicate symlinks)
   libPaths <- .libPaths()
   if (!identical(.Platform$OS.type, "windows"))
      libPaths <- .rs.normalizePath(libPaths)

   uniqueLibPaths <- subset(libPaths, !duplicated(libPaths))
   return (uniqueLibPaths)
})

.rs.addFunction( "writeableLibraryPaths", function()
{
   uniqueLibraryPaths <- .rs.uniqueLibraryPaths()
   writeableLibraryPaths <- character()
   for (libPath in uniqueLibraryPaths)
      if (.rs.isLibraryWriteable(libPath))
         writeableLibraryPaths <- append(writeableLibraryPaths, libPath)
   return (writeableLibraryPaths)
})

.rs.addFunction("defaultUserLibraryPath", function()
{
   unlist(strsplit(Sys.getenv("R_LIBS_USER"),
                              .Platform$path.sep))[1L]
})

.rs.addFunction("defaultLibraryPath", function()
{
  .libPaths()[1]
})

.rs.addFunction("isPackageLoaded", function(packageName, libName)
{
   if (packageName %in% .packages())
   {
      # get the raw path to the package 
      packagePath <- .rs.pathPackage(packageName, quiet=TRUE)

      # alias (for comparison against libName, which comes from the client and
      # is alised)
      packagePath <- .rs.createAliasedPath(packagePath)

      # compare with the library given by the client
      .rs.scalar(identical(packagePath, paste(libName, packageName, sep="/")))
   }
   else 
      .rs.scalar(FALSE)
})

.rs.addJsonRpcHandler( "is_package_loaded", function(packageName, libName)
{
   .rs.isPackageLoaded(packageName, libName)
})

.rs.addFunction("forceUnloadPackage", function(name)
{
  if (name %in% .packages())
  {
    fullName <- paste("package:", name, sep="")
    suppressWarnings(detach(fullName, 
                            character.only=TRUE, 
                            unload=TRUE, 
                            force=TRUE))
    
    pkgDLL <- getLoadedDLLs()[[name]]
    if (!is.null(pkgDLL)) {
      suppressWarnings(library.dynam.unload(name, 
                                            system.file(package=name)))
    }
  }
})

.rs.addFunction("packageVersion", function(name, libPath, pkgs)
{
   pkgs <- subset(pkgs, Package == name & LibPath == libPath)
   if (nrow(pkgs) == 1)
      pkgs$Version
   else
      ""
})

.rs.addFunction( "initDefaultUserLibrary", function()
{
  userdir <- .rs.defaultUserLibraryPath()
  dir.create(userdir, showWarnings = FALSE, recursive = TRUE)
  .libPaths(c(userdir, .libPaths()))
})

.rs.addFunction("ensureWriteableUserLibrary", function()
{
   if (!.rs.defaultLibPathIsWriteable())
      .rs.initDefaultUserLibrary()
})

.rs.addFunction("listInstalledPackages", function()
{
   # calculate unique libpaths
   uniqueLibPaths <- .rs.uniqueLibraryPaths()

   # get packages
   x <- suppressWarnings(library(lib.loc=uniqueLibPaths))
   x <- x$results[x$results[, 1] != "base", , drop=FALSE]
   
   # extract/compute required fields 
   pkgs.name <- x[, 1]
   pkgs.library <- x[, 2]
   pkgs.desc <- x[, 3]
   pkgs.url <- file.path("help/library",
                         pkgs.name, 
                         "html", 
                         "00Index.html")
   loaded.pkgs <- .rs.pathPackage()
   pkgs.loaded <- !is.na(match(normalizePath(
                                  paste(pkgs.library,pkgs.name, sep="/")),
                               loaded.pkgs))
   

   # build up vector of package versions
   instPkgs <- as.data.frame(installed.packages(), stringsAsFactors=F)
   pkgs.version <- sapply(seq_along(pkgs.name), function(i){
     .rs.packageVersion(pkgs.name[[i]], pkgs.library[[i]], instPkgs)
   })
   
   # alias library paths for the client
   pkgs.library <- .rs.createAliasedPath(pkgs.library)

   # return data frame sorted by name
   packages = data.frame(name=pkgs.name,
                         library=pkgs.library,
                         version=pkgs.version,
                         desc=pkgs.desc,
                         url=pkgs.url,
                         loaded=pkgs.loaded,
                         check.rows = TRUE,
                         stringsAsFactors = FALSE)

   # sort and return
   packages[order(packages$name),]
})

.rs.addJsonRpcHandler( "get_package_install_context", function()
{
   # cran mirror configured
   repos = getOption("repos")
   cranMirrorConfigured <- !is.null(repos) && repos != "@CRAN@"
   
   # selected repository names (assume an unnamed repo == CRAN)
   selectedRepositoryNames <- names(repos)
   if (is.null(selectedRepositoryNames))
     selectedRepositoryNames <- "CRAN"

   # package archive extension
   if (identical(.Platform$OS.type, "windows"))
      packageArchiveExtension <- ".zip; .tar.gz"
   else if (identical(substr(.Platform$pkgType, 1L, 10L), "mac.binary"))
      packageArchiveExtension <- ".tgz; .tar.gz"
   else
      packageArchiveExtension <- ".tar.gz"

   # default library path (normalize on unix)
   defaultLibraryPath = .libPaths()[1L]
   if (!identical(.Platform$OS.type, "windows"))
      defaultLibraryPath <- .rs.normalizePath(defaultLibraryPath)
   
   # return context
   list(cranMirrorConfigured = cranMirrorConfigured,
        selectedRepositoryNames = selectedRepositoryNames,
        packageArchiveExtension = packageArchiveExtension,
        defaultLibraryPath = defaultLibraryPath,
        defaultLibraryWriteable = .rs.defaultLibPathIsWriteable(),
        writeableLibraryPaths = .rs.writeableLibraryPaths(),
        defaultUserLibraryPath = .rs.defaultUserLibraryPath(),
        devModeOn = .rs.devModeOn())
})

.rs.addJsonRpcHandler( "get_cran_mirrors", function()
{
   # get CRAN mirrors (securely if we are configured to do so)
   haveSecureMethod <- .rs.haveSecureDownloadFileMethod()
   protocol <- ifelse(haveSecureMethod, "https", "http")
   cranMirrors <- try(silent = TRUE, {
      mirrors_csv_url <- paste(protocol, "://cran.r-project.org/CRAN_mirrors.csv",
                               sep = "")
      mirrors_csv <- tempfile("mirrors", fileext = ".csv")
      download.file(mirrors_csv_url, destfile = mirrors_csv, quiet = TRUE)
      
      # read them
      read.csv(mirrors_csv, as.is = TRUE, encoding = "UTF-8")
   })
   
   # if we got an error then use local only
   if (is.null(cranMirrors) || inherits(cranMirrors, "try-error"))
      cranMirrors <- utils::getCRANmirrors(local.only = TRUE)
   
   # create data frame
   cranDF <- data.frame(name = cranMirrors$Name,
                        host = cranMirrors$Host,
                        url = cranMirrors$URL,
                        country = cranMirrors$CountryCode,
                        ok = cranMirrors$OK,
                        stringsAsFactors = FALSE)

   # filter by OK status
   cranDF <- cranDF[as.logical(cranDF$ok), ]

   # filter by mirror type supported by the current download.file.method
   # (also verify that https urls are provided inline -- if we didn't do
   # this and CRAN changed the format we could end up with no mirrors)
   secureMirror <- grepl("^https", cranDF[, "url"])
   useHttpsURL <- haveSecureMethod && any(secureMirror)
   if (useHttpsURL)
      cranDF <- cranDF[secureMirror,]
   else
      cranDF <- cranDF[!secureMirror,]

   # prepend RStudio mirror and return
   rstudioDF <- data.frame(name = "Global (CDN)",
                           host = "RStudio",
                           url = paste(ifelse(useHttpsURL, "https", "http"), 
                                       "://cran.rstudio.com/", sep=""),
                           country = "us",
                           ok = TRUE,
                           stringsAsFactors = FALSE)
   rbind(rstudioDF, cranDF)
})

.rs.addJsonRpcHandler( "init_default_user_library", function()
{
  .rs.initDefaultUserLibrary()
})


.rs.addJsonRpcHandler( "check_for_package_updates", function()
{
   # get updates writeable libraries and convert to a data frame
   updates <- as.data.frame(utils::old.packages(lib.loc =
                                          .rs.writeableLibraryPaths()),
                            stringsAsFactors = FALSE)
   row.names(updates) <- NULL
   
   # see which ones are from CRAN and add a news column for them
   # NOTE: defend against length-one repos with no name set
   repos <- getOption("repos")
   if ("CRAN" %in% names(repos))
      cranRep <- repos["CRAN"]
   else
      cranRep <- c(CRAN = repos[[1]])
   cranRepLen <- nchar(cranRep)
   isFromCRAN <- cranRep == substr(updates$Repository, 1, cranRepLen)
   newsURL <- character(nrow(updates))
   if (substr(cranRep, cranRepLen, cranRepLen) != "/")
      cranRep <- paste(cranRep, "/", sep="")

   newsURL[isFromCRAN] <- paste(cranRep,
                                "web/packages/",
                                updates$Package,
                                "/NEWS", sep = "")[isFromCRAN]
   
   updates <- data.frame(packageName = updates$Package,
                         libPath = updates$LibPath,
                         installed = updates$Installed,
                         available = updates$ReposVer,
                         newsUrl = newsURL,
                         stringsAsFactors = FALSE)
                       
                       
   return (updates)
})

.rs.addFunction("packagesLoaded", function(pkgs) {
   # first check loaded namespaces
   if (any(pkgs %in% loadedNamespaces()))
      return(TRUE)

   # now check if there are libraries still loaded in spite of the
   # namespace being unloaded 
   libs <- .dynLibs()
   libnames <- vapply(libs, "[[", character(1), "name")
   return(any(pkgs %in% libnames))
})

.rs.addFunction("loadedPackageUpdates", function(pkgs)
{
   # are we ignoring?
   ignore <- .rs.ignoreNextLoadedPackageCheck
   .rs.setVar("ignoreNextLoadedPackageCheck", FALSE)
   if (ignore)
      return(FALSE)

   # if the default set of namespaces in rstudio are loaded
   # then skip the check
   defaultNamespaces <- c("base", "datasets", "graphics", "grDevices",
                          "methods", "stats", "tools", "utils")
   if (identical(defaultNamespaces, loadedNamespaces()) &&
       length(.dynLibs()) == 4)
      return(FALSE)

   if (.rs.packagesLoaded(pkgs)) {
      return(TRUE)
   }
   else {
      avail <- available.packages()
      deps <- suppressMessages(suppressWarnings(
         utils:::getDependencies(pkgs, available=avail)))
      return(.rs.packagesLoaded(deps))
   }
})

.rs.addFunction("loadedPackagesAndDependencies", function(pkgs) {
  
  # if the default set of namespaces in rstudio are loaded
  # then skip the check
  defaultNamespaces <- c("base", "datasets", "graphics", "grDevices",
                         "methods", "stats", "tools", "utils")
  if (identical(defaultNamespaces, loadedNamespaces()) && length(.dynLibs()) == 4)
    return(character())
  
  packagesLoaded <- function(pkgList) {
    
    # first check loaded namespaces
    loaded <- pkgList[pkgList %in% loadedNamespaces()]
    
    # now check if there are libraries still loaded in spite of the
    # namespace being unloaded 
    libs <- .dynLibs()
    libnames <- vapply(libs, "[[", character(1), "name")
    loaded <- c(loaded, pkgList[pkgList %in% libnames])
    loaded
  }
  
  # package loaded
  loaded <- packagesLoaded(pkgs)
  
  # dependencies loaded
  avail <- available.packages()
  deps <- suppressMessages(suppressWarnings(
    utils:::getDependencies(pkgs, available=avail)))
  loaded <- c(loaded, packagesLoaded(deps))
  
  # return unique list
  unique(loaded)  
})

.rs.addFunction("forceUnloadForPackageInstall", function(pkgs) {
  
  # figure out which packages are loaded and/or have dependencies loaded
  pkgs <- .rs.loadedPackagesAndDependencies(pkgs)
  
  # force unload them
  sapply(pkgs, .rs.forceUnloadPackage)
  
  # return packages unloaded
  pkgs
})


.rs.addFunction("enqueLoadedPackageUpdates", function(installCmd)
{
   .Call("rs_enqueLoadedPackageUpdates", installCmd)
})

.rs.addJsonRpcHandler("loaded_package_updates_required", function(pkgs)
{
   .rs.scalar(.rs.loadedPackageUpdates(as.character(pkgs)))
})

.rs.addJsonRpcHandler("ignore_next_loaded_package_check", function() {
   .rs.setVar("ignoreNextLoadedPackageCheck", TRUE)
   return(NULL)
})

.rs.addFunction("getCachedAvailablePackages", function(contribUrl)
{
   .Call("rs_getCachedAvailablePackages", contribUrl)
})

.rs.addFunction("downloadAvailablePackages", function(contribUrl)
{
   .Call("rs_downloadAvailablePackages", contribUrl)
})

.rs.addJsonRpcHandler("package_skeleton", function(packageName,
                                                   packageDirectory,
                                                   sourceFiles,
                                                   usingRcpp)
{
   # sourceFiles is passed in as a list -- convert back to
   # character vector
   sourceFiles <- as.character(sourceFiles)
   
   # Make sure we expand the aliased path if necessary
   # (note this is a no-op if there is no leading '~')
   packageDirectory <- path.expand(packageDirectory)
   
   ## Validate the package name -- note that we validate this upstream
   ## but it is sensible to validate it once more here
   if (!grepl("^[[:alpha:]][[:alnum:].]*", packageName))
      return(.rs.error(
         "Invalid package name: the package name must start ",
         "with a letter and follow with only alphanumeric characters"))
   
   ## Validate the package directory -- if it exists, make sure it's empty,
   ## otherwise, try to create it
   if (file.exists(packageDirectory))
   {
      containedFiles <- list.files(packageDirectory) ## what about hidden files?
      if (length(containedFiles))
      {
         return(.rs.error(
            "Folder '", packageDirectory, "' ",
            "already exists and is not empty"))
      }
   }
   
   # Otherwise, create it
   else
   {
      if (!dir.create(packageDirectory, recursive = TRUE))
         return(.rs.error(
            "Failed to create directory '", packageDirectory, "'"))
   }
   
   ## Create a DESCRIPTION file
   
   # Fill some bits based on devtools options if they're available.
   # Protect against vectors with length > 1
   getDevtoolsOption <- function(optionName, default, collapse = " ")
   {
      devtoolsDesc <- getOption("devtools.desc")
      if (!length(devtoolsDesc))
         return(default)
      
      option <- devtoolsDesc[[optionName]]
      if (is.null(option))
         return(default)
      
      paste(option, collapse = collapse)
   }
   
   
   Author <- getDevtoolsOption("Author", "Who wrote it")
   
   Maintainer <- getDevtoolsOption(
      "Maintainer",
      "The package maintainer <yourself@somewhere.net>"
   )
   
   License <- getDevtoolsOption(
      "License",
      "What license is it under?",
      ", "
   )
   
   DESCRIPTION <- list(
      Package = packageName,
      Type = "Package",
      Title = "What the Package Does (Title Case)",
      Version = "0.1.0",
      Author = Author,
      Maintainer = Maintainer,
      Description = c(
         "More about what it does (maybe more than one line)",
         "Use four spaces when indenting paragraphs within the Description."
      ),
      License = License,
      Encoding = "UTF-8",
      LazyData = "true"
   )
   
   # Create a NAMESPACE file
   NAMESPACE <- c(
      'exportPattern("^[[:alpha:]]+")'
   )
   
   # If we are using Rcpp, update DESCRIPTION and NAMESPACE
   if (usingRcpp)
   {
      dir.create(file.path(packageDirectory, "src"), showWarnings = FALSE)
      
      rcppImportsStatement <- "Rcpp"
      
      # We'll enforce Rcpp > (installed version)
      ip <- installed.packages()
      if ("Rcpp" %in% rownames(ip))
         rcppImportsStatement <- sprintf("Rcpp (>= %s)", ip["Rcpp", "Version"])
      
      DESCRIPTION$Imports <- c(DESCRIPTION$Imports, rcppImportsStatement)
      DESCRIPTION$LinkingTo <- c(DESCRIPTION$LinkingTo, "Rcpp")
      
      # Add an import from Rcpp, and also useDynLib
      NAMESPACE <- c(
         NAMESPACE,
         "importFrom(Rcpp, evalCpp)",
         sprintf("useDynLib(%s)", packageName)
      )
   }
   
   # Get other fields from devtools options
   if (length(getOption("devtools.desc.suggests")))
      DESCRIPTION$Suggests <- getOption("devtools.desc.suggests")
   
   if (length(getOption("devtools.desc")))
   {
      devtools.desc <- getOption("devtools.desc")
      for (i in seq_along(devtools.desc))
      {
         name <- names(devtools.desc)[[i]]
         value <- devtools.desc[[i]]
         DESCRIPTION[[name]] <- value
      }
   }
   
   # If we are using 'testthat' and 'devtools' is available, use it to
   # add test infrastructure
   if ("testthat" %in% DESCRIPTION$Suggests)
   {
      dir.create(file.path(packageDirectory, "tests"))
      dir.create(file.path(packageDirectory, "tests", "testthat"))
      
      if ("devtools" %in% rownames(installed.packages()))
      {
         # NOTE: Okay to load devtools as we will restart the R session
         # soon anyhow
         ns <- asNamespace("devtools")
         if (exists("render_template", envir = ns))
         {
            tryCatch(
               writeLines(
                  devtools:::render_template(
                     "testthat.R",
                     list(name = packageName)
                  ),
                  file.path(packageDirectory, "tests", "testthat.R")
               ), error = function(e) NULL
            )
         }
      }
   }
   
   # If we are using the MIT license, add the template
   if (grepl("MIT\\s+\\+\\s+file\\s+LICEN[SC]E", DESCRIPTION$License, perl = TRUE))
   {
      # Guess the copyright holder
      holder <- if (!is.null(getOption("devtools.name")))
         Author
      else
         "<Copyright holder>"
      
      msg <- c(
         paste("YEAR:", format(Sys.time(), "%Y")),
         paste("COPYRIGHT HOLDER:", holder)
      )
      
      cat(msg,
          file = file.path(packageDirectory, "LICENSE"),
          sep = "\n")
   }
   
   # Always create 'R/', 'man/' directories
   dir.create(file.path(packageDirectory, "R"), showWarnings = FALSE)
   dir.create(file.path(packageDirectory, "man"))
   
   # If there were no source files specified, create a simple 'hello world'
   # function -- but only if the user hasn't implicitly opted into the 'devtools'
   # ecosystem
   if ((!length(getOption("devtools.desc"))) &&
       (!length(sourceFiles)))
   {
      
      # Some simple shortcuts that authors should know
      sysname <- Sys.info()[["sysname"]]
      
      buildShortcut <- if (sysname == "Darwin")
         "Cmd + Shift + B"
      else
         "Ctrl + Shift + B"
      
      checkShortcut <- if (sysname == "Darwin")
         "Cmd + Shift + E"
      else
         "Ctrl + Shift + E"
      
      testShortcut <- if (sysname == "Darwin")
         "Cmd + Shift + T"
      else
         "Ctrl + Shift + T"
      
      helloWorld <- .rs.trimCommonIndent('
         # Hello, world!
         #
         # This is an example function named \'hello\' 
         # which prints \'Hello, world!\'.
         #
         # You can learn more about package authoring with RStudio at:
         #
         #   http://r-pkgs.had.co.nz/
         #
         # Some useful keyboard shortcuts for package authoring:
         #
         #   Build and Reload Package:  \'%s\'
         #   Check Package:             \'%s\'
         #   Test Package:              \'%s\'
         
         hello <- function() {
           print(\"Hello, world!\")
         }
      ', buildShortcut, checkShortcut, testShortcut)
      
      cat(helloWorld,
          file = file.path(packageDirectory, "R", "hello.R"),
          sep = "\n")
      
      # Similarly, create a simple example .Rd for this 'hello world' function
      helloWorldRd <- .rs.trimCommonIndent('
         \\name{hello}
         \\alias{hello}
         \\title{Hello, World!}
         \\usage{
         hello()
         }
         \\description{
         Prints \'Hello, world!\'.
         }
         \\examples{
         hello()
         }
      ')
      
      cat(helloWorldRd,
          file = file.path(packageDirectory, "man", "hello.Rd"),
          sep = "\n")
      
      if (usingRcpp)
      {
         ## Ensure 'src/' directory exists
         if (!file.exists(file.path(packageDirectory, "src")))
            dir.create(file.path(packageDirectory, "src"))
         
         ## Write a 'hello world' for C++
         helloWorldCpp <- .rs.trimCommonIndent('
            #include <Rcpp.h>
            using namespace Rcpp;
            
            // This is a simple function using Rcpp that creates an R list
            // containing a character vector and a numeric vector.
            //
            // Learn more about how to use Rcpp at:
            //
            //   http://www.rcpp.org/
            //   http://adv-r.had.co.nz/Rcpp.html
            //
            // and browse examples of code using Rcpp at:
            // 
            //   http://gallery.rcpp.org/
            //

            // [[Rcpp::export]]
            List rcpp_hello() {
              CharacterVector x = CharacterVector::create("foo", "bar");
              NumericVector y   = NumericVector::create(0.0, 1.0);
              List z            = List::create(x, y);
              return z;
            }

         ')

         helloWorldDoc <- .rs.trimCommonIndent('
            \\name{rcpp_hello}
            \\alias{rcpp_hello}
            \\title{Hello, Rcpp!}
            \\usage{
            rcpp_hello()
            }
            \\description{
            Returns an \\R \\code{list} containing the character vector
            \\code{c("foo", "bar")} and the numeric vector \\code{c(0, 1)}.
            }
            \\examples{
            rcpp_hello()
            }
         ')
         
         cat(helloWorldCpp,
             file = file.path(packageDirectory, "src", "rcpp_hello.cpp"),
             sep = "\n")

         cat(helloWorldDoc,
             file = file.path(packageDirectory, "man", "rcpp_hello.Rd"),
             sep = "\n")
         
      }
   }
   else if (length(sourceFiles))
   {
      # Copy the source files to the appropriate sub-directory
      sourceFileExtensions <- tolower(gsub(".*\\.", "", sourceFiles, perl = TRUE))
      sourceDirs <- .rs.swap(
         sourceFileExtensions,
         "R" = c("r", "q", "s"),
         "src" = c("c", "cc", "cpp", "h", "hpp"),
         "vignettes" = c("rmd", "rnw"),
         "man" = "rd",
         "data" = c("rda", "rdata"),
         default = ""
      )
      
      copyPaths <- gsub("/+", "/", file.path(
         packageDirectory,
         sourceDirs,
         basename(sourceFiles)
      ))
      
      dirPaths <- dirname(copyPaths)
      
      success <- unlist(lapply(dirPaths, function(path) {
         
         if (isTRUE(file.info(path)$isdir))
            return(TRUE)
         
         dir.create(path, recursive = TRUE, showWarnings = FALSE)
         
      }))
      
      if (!all(success))
         return(.rs.error("Failed to create package directory structure"))
      
      success <- file.copy(sourceFiles, copyPaths)
      
      if (!all(success))
         return(.rs.error("Failed to copy one or more source files"))
   }
   
   # Write various files out
   
   # NOTE: write.dcf mangles whitespace so we manually construct
   # the text we wish to write out
   DESCRIPTION <- lapply(DESCRIPTION, function(field) {
      paste(field, collapse = "\n    ")
   })
   
   names <- names(DESCRIPTION)
   values <- unlist(DESCRIPTION)
   text <- paste(names, ": ", values, sep = "", collapse = "\n")
   cat(text, file = file.path(packageDirectory, "DESCRIPTION"))
   
   cat(NAMESPACE, file = file.path(packageDirectory, "NAMESPACE"), sep = "\n")
   
   RprojPath <- file.path(
      packageDirectory,
      paste(packageName, ".Rproj", sep = "")
   )
   
   if (!.Call("rs_writeProjectFile", RprojPath))
      return(.rs.error("Failed to create package .Rproj file"))
   
   # Ensure new packages get AutoAppendNewLine + StripTrailingWhitespace
   Rproj <- readLines(RprojPath)
   
   appendNewLineIndex <- grep("AutoAppendNewline:", Rproj, fixed = TRUE)
   if (length(appendNewLineIndex))
      Rproj[appendNewLineIndex] <- "AutoAppendNewline: Yes"
   else
      Rproj <- c(Rproj, "AutoAppendNewline: Yes")
   
   stripTrailingWhitespace <- grep("StripTrailingWhitespace:", Rproj, fixed = TRUE)
   if (length(appendNewLineIndex))
      Rproj[appendNewLineIndex] <- "StripTrailingWhitespace: Yes"
   else
      Rproj <- c(Rproj, "StripTrailingWhitespace: Yes")
   
   cat(Rproj, file = RprojPath, sep = "\n")
   
   # NOTE: this file is not always generated (e.g. people who have implicitly opted
   # into using devtools won't need the template file)
   if (file.exists(file.path(packageDirectory, "R", "hello.R")))
      .Call("rs_addFirstRunDoc", RprojPath, "R/hello.R")

   ## NOTE: This must come last to ensure the other package
   ## infrastructure bits have been generated; otherwise
   ## compileAttributes can fail
   if (usingRcpp &&
       .rs.isPackageVersionInstalled("Rcpp", "0.10.1") &&
       require(Rcpp, quietly = TRUE))
   {
      Rcpp::compileAttributes(packageDirectory)
      if (file.exists(file.path(packageDirectory, "src/rcpp_hello.cpp")))
         .Call("rs_addFirstRunDoc", RprojPath, "src/rcpp_hello.cpp")
   }
   
   .rs.success()
   
})


.rs.addFunction("secureDownloadMethod", function()
{
   # Function to determine whether R checks for 404 in libcurl calls
   libcurlHandles404 <- function() {
      getRversion() >= "3.3" && .rs.haveRequiredRSvnRev(69197)
   }

   # Check whether we are running R 3.2 and whether we have libcurl
   isR32 <- getRversion() >= "3.2"
   haveLibcurl <- isR32 && capabilities("libcurl") && libcurlHandles404()
   
   # Utility function to bind to libcurl or a fallback utility (e.g. wget)
   posixMethod <- function(utility) {
      if (haveLibcurl)
         "libcurl"
      else if (nzchar(Sys.which(utility)))
         utility
      else
         ""
   }
   
   # Determine the right secure download method per-system
   sysName <- Sys.info()[['sysname']]
   
   # For windows we prefer binding directly to wininet if we can (since
   # that doesn't rely on the value of setInternet2). If it's R <= 3.1
   # then we can use "internal" for https so long as internet2 is enabled 
   # (we don't use libcurl on Windows because it doesn't check certs).
   if (identical(sysName, "Windows")) {
      if (isR32)
         "wininet"
      else if (isTRUE(.rs.setInternet2(NA)))
         "internal"
      else
         ""
   }
   
   # For Darwin and Linux we use libcurl if we can and then fall back
   # to curl or wget as appropriate. We prefer libcurl because it honors
   # the same proxy configuration that "internal" does so it less likely
   # to break downloads for users behind proxy servers. 
   
   else if (identical(sysName, "Darwin")) {
      posixMethod("curl")
   }
   
   else if (identical(sysName, "Linux")) {
      method <- posixMethod("wget")
      if (!nzchar(method))
         method <- posixMethod("curl")
      method
   } 
   
   # Another OS, don't even attempt detection since RStudio currently
   # only runs on Windows, Linux, and Mac
   else {
      ""
   }
})

.rs.addFunction("autoDownloadMethod", function() {
   if (capabilities("http/ftp"))
      "internal"
   else if (nzchar(Sys.which("wget")))
      "wget"
   else if (nzchar(Sys.which("curl")))
      "curl"
   else
      ""
})

.rs.addFunction("isDownloadMethodSecure", function(method) {
   
   # resolve auto if needed
   if (identical(method, "auto"))
      method <- .rs.autoDownloadMethod()
   
   # check for methods known to work securely
   if (method %in% c("wininet", "libcurl", "wget", "curl")) {
      TRUE
   }
   
   # if internal then see if were using windows internal with inet2
   else if (identical(method, "internal")) {
      identical(Sys.info()[['sysname']], "Windows") && isTRUE(.rs.setInternet2(NA))
   }
   
   # method with unknown properties (e.g. "lynx") or unresolved auto
   else {
      FALSE
   }
})

.rs.addFunction("haveSecureDownloadFileMethod", function() {
   .rs.isDownloadMethodSecure(getOption("download.file.method", "auto"))
})

.rs.addFunction("showSecureDownloadWarning", function() {
   is.na(Sys.getenv("RSTUDIO_DISABLE_SECURE_DOWNLOAD_WARNING", unset = NA))
})

.rs.addFunction("insecureReposWarning", function(msg) {
   if (.rs.showSecureDownloadWarning()) {
      message("WARNING: ", msg, " You should either switch to a repository ",
              "that supports HTTPS or change your RStudio options to not require HTTPS ",
              "downloads.\n\nTo learn more and/or disable this warning ",
              "message see the \"Use secure download method for HTTP\" option ",
              "in Tools -> Global Options -> Packages.")
   }
})

.rs.addFunction("insecureDownloadWarning", function(msg) {
   if (.rs.showSecureDownloadWarning()) {
      message("WARNING: ", msg,
              "\n\nTo learn more and/or disable this warning ",
              "message see the \"Use secure download method for HTTP\" option ",
              "in Tools -> Global Options -> Packages.")
   }
})

.rs.addFunction("initSecureDownload", function() {
      
   # check if the user has already established a download.file.method and
   # if so verify that it is secure
   method <- getOption("download.file.method")
   if (!is.null(method)) {
      if (!.rs.isDownloadMethodSecure(method)) {
         .rs.insecureDownloadWarning(
             paste("The download.file.method option is \"", method, "\" ",
                   "however that method cannot provide secure (HTTPS) downloads ",
                   "on this platform. ", 
                   "This option was likely specified in .Rprofile or ",
                   "Rprofile.site so if you wish to change it you may need ",
                   "to edit one of those files.",
                   sep = "")
         )
      }
   } 
   
   # no user specified method, automatically set a secure one if we can
   else {
      secureMethod <- .rs.secureDownloadMethod()
      if (nzchar(secureMethod)) {
         options(download.file.method = secureMethod) 
         if (secureMethod == "curl")
            options(download.file.extra = .rs.downloadFileExtraWithCurlArgs())
      }
      else {
         .rs.insecureDownloadWarning(
            paste("Unable to set a secure (HTTPS) download.file.method (no",
                  "compatible method available in this installation of R).")
         )
      }
   }
})
   

.rs.addFunction("downloadFileExtraWithCurlArgs", function() {
   newArgs <- "-L -f -g"
   curArgs <- getOption("download.file.extra")
   if (!is.null(curArgs) && !grepl(newArgs, curArgs, fixed = TRUE))
      curArgs <- paste(newArgs, curArgs)
   curArgs
})

.rs.addFunction("setInternet2", function(value = NA) {
   
   # from R 3.3.x, 'setInternet2' is defunct and does nothing
   if (getRversion() >= "3.3.0")
      return(TRUE)
   
   # should only be called on Windows, but sanity check
   if (Sys.info()[["sysname"]] != "Windows")
      return(TRUE)
   
   # delegate to 'setInternet2'
   utils::setInternet2(value)
})
