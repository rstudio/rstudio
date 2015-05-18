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
      .Call(.rs.routines$rs_packageLoaded, pkgname)
   }

   notifyPackageUnloaded <- function(pkgname, ...)
   {
      .Call(.rs.routines$rs_packageUnloaded, pkgname)
   }
   
   sapply(.packages(TRUE), function(packageName) 
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
         if (length(sys.calls()) > 7) {
            installCall <- sys.call(-7)
            installCmd <- format(installCall)
         } else {
            installCmd <- NULL
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
         .Call(.rs.routines$rs_packageLibraryMutated)
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
    .Call(.rs.routines$rs_addRToolsToPath)
})

.rs.addFunction( "restorePreviousPath", function()
{
    .Call(.rs.routines$rs_restorePreviousPath)
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
   x <- x$results[x$results[, 1] != "base", ]
   
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
   pkgs.version <- character(length=length(pkgs.name))
   for (i in 1:length(pkgs.name)) {
      pkgs.version[[i]] <- .rs.packageVersion(pkgs.name[[i]],
                                              pkgs.library[[i]],
                                              instPkgs)
   }
   
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
   # RStudio mirror
   rstudioDF <- data.frame(name = "Global (CDN)",
                           host = "RStudio",
                           url = "http://cran.rstudio.com",
                           country = "us",
                           stringsAsFactors = FALSE)

   # CRAN mirrors
   cranMirrors <- utils::getCRANmirrors()
   cranDF <- data.frame(name = cranMirrors$Name,
                        host = cranMirrors$Host,
                        url = cranMirrors$URL,
                        country = cranMirrors$CountryCode,
                        stringsAsFactors = FALSE)

   # return mirrors
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
   cranRep <- getOption("repos")["CRAN"]
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
   .Call(.rs.routines$rs_enqueLoadedPackageUpdates, installCmd)
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
   .Call(.rs.routines$rs_getCachedAvailablePackages, contribUrl)
})

.rs.addFunction("downloadAvailablePackages", function(contribUrl)
{
   .Call(.rs.routines$rs_downloadAvailablePackages, contribUrl)
})

.rs.addJsonRpcHandler("package_skeleton", function(packageName,
                                                   packageDirectory,
                                                   sourceFiles,
                                                   usingRcpp)
{
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
      option <- getOption(optionName)
      if (!is.null(option))
      {
         if (length(option) > 0)
            paste(option, collapse = collapse)
         else
            option
      }
      else default
   }
   
   `%||%` <- function(x, y)
      if (is.null(x)) y else x
   
   Author <- getDevtoolsOption("devtools.name", "Who wrote it")
   
   Maintainer <- getDevtoolsOption(
      "devtools.desc.author",
      "Who to complain to <yourfault@somewhere.net>"
   )
   
   License <- getDevtoolsOption(
      "devtools.desc.license",
      "What license is it under?",
      ", "
   )
   
   DESCRIPTION <- list(
      Package = packageName,
      Type = "Package",
      Title = "What the Package Does (Title Case)",
      Version = "0.1",
      Date = as.character(Sys.Date()),
      Author = Author,
      Maintainer = Maintainer,
      Description = "More about what it does (maybe more than one line)",
      License = License,
      LazyData = "TRUE"
   )
   
   # If we filled in an 'Authors@R' field for Authors (e.g. provided for new
   # 'devtools'), then name this field as such.
   if (isTRUE(grepl("^\\s*(?:as\\.person|person|c)\\(", DESCRIPTION[["Maintainer"]], perl = TRUE)))
   {
      maintainerIdx <- which(names(DESCRIPTION) == "Maintainer")
      names(DESCRIPTION)[[maintainerIdx]] <- "Authors@R"
      DESCRIPTION[["Author"]] <- NULL
   }
   
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
      sourceFileExtensions <- gsub(".*\\.", "", sourceFiles, perl = TRUE)
      sourceDirs <- .rs.swap(
         sourceFileExtensions,
         "R" = c("r", "q", "s"),
         "src" = c("c", "cc", "cpp", "h", "hpp"),
         "vignettes" = c("rmd", "rnw"),
         "man" = "rd",
         "data" = c("rda", "rdata"),
         default = ""
      )
      
      copyPaths <- gsub("/+", "", file.path(
         packageDirectory,
         sourceDirs,
         basename(sourceFiles)
      ))
      
      dirPaths <- dirname(copyPaths)
      dir.create(dirPaths, recursive = TRUE)
      
      success <- file.copy(sourceFiles,
                           copyPaths)
      
      if (!all(success))
         return(.rs.error("Failed to copy one or more source files"))
   }
   
   # Write various files out
   
   # NOTE: write.dcf mangles whitespace so we manually construct
   # our DCF
   for (name in c("Depends", "Imports", "Suggests", "LinkingTo"))
   {
      if (name %in% names(DESCRIPTION))
      {
         DESCRIPTION[[name]] <- paste(
            sep = "",
            "\n    ",
            paste(DESCRIPTION[[name]], collapse = ",\n    ")
         )
      }
   }
   
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
      .Call(.rs.routines$rs_addFirstRunDoc, RprojPath, "R/hello.R")

   ## NOTE: This must come last to ensure the other package
   ## infrastructure bits have been generated; otherwise
   ## compileAttributes can fail
   if (usingRcpp &&
       .rs.isPackageVersionInstalled("Rcpp", "0.10.1") &&
       require(Rcpp, quietly = TRUE))
   {
      Rcpp::compileAttributes(packageDirectory)
      if (file.exists(file.path(packageDirectory, "src/rcpp_hello.cpp")))
         .Call(.rs.routines$rs_addFirstRunDoc, RprojPath, "src/rcpp_hello.cpp")
   }
   
   .rs.success()
   
})
