#
# SessionPackages.R
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

.rs.addFunction( "updatePackageEvents", function()
{
   reportPackageStatus <- function(status)
      function(pkgname, ...)
      {
         packageStatus = list(name=pkgname, loaded=status)
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
      # the following pacakges are known to not work in RStudio (and beyond
      # that are likely to be dangerous to load in RStudio, thus their
      # inclusion on this list rather than allowing the package install,
      # test, or load to fail in the normal manner)
      unsupported <- c("multicore")

      # check unsupported list
      unsupportedPkgs <- pkgs[pkgs %in% unsupported]
      if (length(unsupportedPkgs) > 0)
      {
         stop(paste("The following packages are unsupported in RStudio:",
                    paste(unsupportedPkgs, collapse=", ")))
      }


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

.rs.addJsonRpcHandler( "get_default_library", function()
{
   .rs.scalar(.libPaths()[1])
})

.rs.addJsonRpcHandler( "is_package_loaded", function(packageName)
{
   .rs.scalar(packageName %in% .packages())
})

.rs.addJsonRpcHandler( "list_packages", function()
{
   # calculate unique libpaths
   libPaths <- normalizePath(.libPaths())
   uniqueLibPaths <- subset(libPaths, !duplicated(libPaths))

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
   loaded.pkgs <- .packages()
   pkgs.loaded <- !is.na(match(pkgs.name, loaded.pkgs))
   
   # return data frame sorted by name
   packages = data.frame(name=pkgs.name,
                         library=pkgs.library,
                         desc=pkgs.desc,
                         url=pkgs.url,
                         loaded=pkgs.loaded,
                         check.rows = TRUE,
                         stringsAsFactors = FALSE)

   # sort and return
   packages[order(packages$name),]
})

.rs.addJsonRpcHandler( "is_cran_configured", function()
{
   repos = getOption("repos")
   return(.rs.scalar(!is.null(repos) && repos != "@CRAN@"))
})

.rs.addJsonRpcHandler( "get_cran_mirrors", function()
{
   cranMirrors <- utils::getCRANmirrors()
   data.frame(name = cranMirrors$Name,
              host = cranMirrors$Host,
              url = cranMirrors$URL,
              country = cranMirrors$CountryCode,
              stringsAsFactors = FALSE)
})
