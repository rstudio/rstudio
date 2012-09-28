#
# SessionPackages.R
#
# Copyright (C) 2009-12 by RStudio, Inc.
#
# This program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

.rs.addFunction( "updatePackageEvents", function()
{
   reportPackageStatus <- function(status)
      function(pkgname, ...)
      {
         packageStatus = list(name=pkgname,
                              path=.path.package(pkgname, quiet=TRUE),
                              loaded=status)
         .rs.enqueClientEvent("package_status_changed", packageStatus)
      }
   
   sapply(.packages(TRUE), function(packageName) 
   {
      if ( !(packageName %in% .rs.hookedPackages) )
      {
         attachEventName = packageEvent(packageName, "attach")
         setHook(attachEventName, reportPackageStatus(TRUE), action="append")
             
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
    
   # ensure we are subscribed to package attach/detach events
   .rs.updatePackageEvents()
   
   # whenever a package is installed notify the client and make sure
   # we are subscribed to its attach/detach events
   .rs.registerReplaceHook("install.packages", "utils", function(original,
                                                                pkgs,
                                                                lib,
                                                                ...) 
   {
      # do housekeeping after we execute the original
      on.exit({
         .rs.updatePackageEvents()
         .rs.enqueClientEvent("installed_packages_changed")
      })
                          
      # call original
      original(pkgs, lib, ...)
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
      on.exit(.rs.enqueClientEvent("installed_packages_changed"))
                         
      # call original
      original(pkgs, lib, ...) 
   })
})

.rs.addFunction( "uniqueLibraryPaths", function()
{
   # get library paths (normalize on unix to get rid of duplicate symlinks)
   libPaths <- .libPaths()
   if (!identical(.Platform$OS.type, "windows"))
      libPaths <- normalizePath(libPaths)

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

.rs.addJsonRpcHandler( "is_package_loaded", function(packageName, libName)
{
   .rs.scalar( (packageName %in% .packages()) &&
               identical(.path.package(packageName, quiet=TRUE),
                         paste(libName, packageName, sep="/"))
             )
})

.rs.addFunction("isPackageInstalled", function(name)
{
   name %in% .packages(all.available = TRUE)
})

.rs.addFunction("forceUnloadPackage", function(name)
{
   if (name %in% .packages())
   {
      fullName <- paste("package:", name, sep="")
      detach(fullName, character.only=TRUE, force=TRUE)
      pkgDLL <- getLoadedDLLs()[[name]]
      if (!is.null(pkgDLL))
         dyn.unload(pkgDLL[["path"]])
   }
})

.rs.addFunction("libPathsString", function()
{
   paste(.libPaths(), collapse = .Platform$path.sep)
})

.rs.addFunction("packageVersion", function(name, libPath, pkgs)
{
   pkgs <- subset(pkgs, Package == name & LibPath == libPath)
   if (nrow(pkgs) == 1)
      pkgs$Version
   else
      ""
})

.rs.addJsonRpcHandler( "list_packages", function()
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
   loaded.pkgs <- .path.package()
   pkgs.loaded <- !is.na(match(paste(pkgs.library,pkgs.name, sep="/"),
                               loaded.pkgs))
   

   # build up vector of package versions
   instPkgs <- as.data.frame(installed.packages(), stringsAsFactors=F)
   pkgs.version <- character(length=length(pkgs.name))
   for (i in 1:length(pkgs.name)) {
      pkgs.version[[i]] <- .rs.packageVersion(pkgs.name[[i]],
                                              pkgs.library[[i]],
                                              instPkgs)
   }

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
   
   # selected repository names
   selectedRepositoryNames <- names(repos)

   # can we build c code
   canBuildCpp <- .Call("rs_canBuildCpp")

   # are we in dev mode
   devModeOn <- .rs.devModeOn()

   # package archive extension
   if (identical(.Platform$OS.type, "windows"))
      packageArchiveExtension <- ifelse(canBuildCpp,".zip; .tar.gz", ".zip")
   else if (identical(substr(.Platform$pkgType, 1L, 10L), "mac.binary"))
      packageArchiveExtension <- ".tgz; .tar.gz"
   else
      packageArchiveExtension <- ".tar.gz"

   # default library path (normalize on unix)
   defaultLibraryPath = .libPaths()[1L]
   if (!identical(.Platform$OS.type, "windows"))
      defaultLibraryPath <- normalizePath(defaultLibraryPath)
   
   # is default library writeable (based on install.packages)
   defaultLibraryWriteable <- .rs.defaultLibPathIsWriteable()
   
   # writeable library paths
   writeableLibraryPaths <- .rs.writeableLibraryPaths()

   # default user library path (based on install.packages)
   defaultUserLibraryPath <- .rs.defaultUserLibraryPath()

   # return context
   list(cranMirrorConfigured = cranMirrorConfigured,
        selectedRepositoryNames = selectedRepositoryNames,
        packageArchiveExtension = packageArchiveExtension,
        defaultLibraryPath = defaultLibraryPath,
        defaultLibraryWriteable = defaultLibraryWriteable,
        writeableLibraryPaths = writeableLibraryPaths,
        defaultUserLibraryPath = defaultUserLibraryPath,
        canBuildCpp = canBuildCpp,
        devModeOn = devModeOn)
})

.rs.addJsonRpcHandler( "get_cran_mirrors", function()
{
   # RStudio mirror
   rstudioDF <- data.frame(name = "Global (CDN)",
                           host = "RStudio",
                           url = "http://cran.rstudio.org",
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
   userdir <- .rs.defaultUserLibraryPath()
   dir.create(userdir, recursive = TRUE)
   .libPaths(c(userdir, .libPaths()))
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
