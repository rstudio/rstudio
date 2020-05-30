#
# SessionPackages.R
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

# cached URLs for package NEWS files
.rs.setVar("packageNewsURLsEnv", new.env(parent = emptyenv()))

# cached information on available R packages
.rs.setVar("availablePackagesEnv", new.env(parent = emptyenv()))

# for asynchronous requests on available package information,
# map the repository string to the directory containing produced output
.rs.setVar("availablePackagesPendingEnv", new.env(parent = emptyenv()))

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
      .Call("rs_packageLoaded", pkgname, PACKAGE = "(embedding)")

      # when a package is loaded, it can register S3 methods which replace overrides we've
      # attached manually; take this opportunity to reattach them.
      .rs.reattachS3Overrides()
   }

   notifyPackageUnloaded <- function(pkgname, ...)
   {
      .Call("rs_packageUnloaded", pkgname, PACKAGE = "(embedding)")
   }
   
   pkgNames <-
      base::list.dirs(.libPaths(), full.names = FALSE, recursive = FALSE)
   
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
      if (missing(pkgs))
         return(utils::install.packages())
      
      if (!.Call("rs_canInstallPackages", PACKAGE = "(embedding)"))
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
         .Call("rs_packageLibraryMutated", PACKAGE = "(embedding)")
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
      on.exit(.Call("rs_packageLibraryMutated", PACKAGE = "(embedding)"))
                         
      # call original
      original(pkgs, lib, ...) 
   })
})

.rs.addFunction( "addRToolsToPath", function()
{
    .Call("rs_addRToolsToPath", PACKAGE = "(embedding)")
})

.rs.addFunction( "restorePreviousPath", function()
{
    .Call("rs_restorePreviousPath", PACKAGE = "(embedding)")
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

.rs.addFunction("lastCharacterIs", function(value, ending) {
   identical(tail(strsplit(value, "")[[1]], n = 1), ending)
})

.rs.addFunction("listInstalledPackages", function()
{
   # get the CRAN repository URL, and remove a trailing slash if required
   repos <- getOption("repos")
   cran <- if ("CRAN" %in% names(repos))
      repos[["CRAN"]]
   else
      .Call("rs_rstudioCRANReposUrl", PACKAGE = "(embedding)")
   
   # trim trailing slashes if necessary
   cran <- gsub("/*", "", cran)
   
   # helper function for extracting information from a package's
   # DESCRIPTION file
   readPackageInfo <- function(pkgPath) {
      
      # attempt to read package metadata
      desc <- .rs.tryCatch({
         metapath <- file.path(pkgPath, "Meta", "package.rds")
         metadata <- readRDS(metapath)
         as.list(metadata$DESCRIPTION)
      })
      
      # if that failed, try reading the DESCRIPTION
      if (inherits(desc, "error"))
         desc <- read.dcf(file.path(pkgPath, "DESCRIPTION"), all = TRUE)
      
      # attempt to infer an appropriate URL for this package
      if (identical(as.character(desc$Priority), "base")) {
         source <- "Base"
         url <- ""
      } else if (!is.null(desc$URL)) {
         source <- "Custom"
         url <- strsplit(desc$URL, "\\s*,\\s*")[[1]][[1]]
      } else if ("biocViews" %in% names(desc)) {
         source <- "Bioconductor"
         url <- sprintf("https://www.bioconductor.org/packages/release/bioc/html/%s.html", desc$Package)
      } else if (identical(desc$Repository, "CRAN")) {
         source <- "CRAN"
         url <- sprintf("%s/package=%s", cran, desc$Package)
      } else if (!is.null(desc$GithubRepo)) {
         source <- "GitHub"
         url <- sprintf("https://github.com/%s/%s", desc$GithubUsername, desc$GithubRepo)
      } else {
         source <- "Unknown"
         url <- sprintf("%s/package=%s", cran, desc$Package)
      }
      
      list(
         Package     = .rs.nullCoalesce(desc$Package, "[Unknown]"),
         LibPath     = dirname(pkgPath),
         Version     = .rs.nullCoalesce(desc$Version, "[Unknown]"),
         Title       = .rs.nullCoalesce(desc$Title, "[No description available]"),
         Source      = source,
         BrowseUrl   = utils::URLencode(url)
      )
      
   }
   
   # to be called if our attempt to read the package DESCRIPTION file failed
   # for some reason
   emptyPackageInfo <- function(pkgPath) {
      
      package <- basename(pkgPath)
      libPath <- dirname(pkgPath)
      
      list(
         Package   = package,
         LibPath   = libPath,
         Version   = "[Unknown]",
         Title     = "[Failed to read package metadata]",
         Source    = "Unknown",
         BrowseUrl = ""
      )
      
   }
   
   # now, find packages. we'll only include packages that have
   # a Meta folder. note that the pseudo-package 'translations'
   # lives in the R system library, and has a DESCRIPTION file,
   # but cannot be loaded as a regular R package.
   packagePaths <- list.files(.rs.uniqueLibraryPaths(), full.names = TRUE)
   hasMeta <- file.exists(file.path(packagePaths, "Meta"))
   packagePaths <- packagePaths[hasMeta]
   
   # now, iterate over these to generate the requisite package
   # information and combine into a data.frame
   parts <- lapply(packagePaths, function(pkgPath) {
      
      tryCatch(
         readPackageInfo(pkgPath),
         error = function(e) emptyPackageInfo(pkgPath)
      )
      
   })
   
   # combine into a data.frame
   info <- .rs.rbindList(parts)
   
   # find which packages are currently attached (be careful to handle
   # cases where package is installed into multiple libraries)
   #
   # we suppress warnings here as 'find.packages(.packages())' can warn
   # if a package that is attached is no longer actually installed
   loaded <- suppressWarnings(
      normalizePath(file.path(info$LibPath, info$Package), winslash = "/", mustWork = FALSE) %in%
      normalizePath(find.package(.packages(), quiet = TRUE), winslash = "/", mustWork = FALSE)
   )
   
   # extract fields relevant to us
   packages <- data.frame(
      name             = info$Package,
      library          = .rs.createAliasedPath(info$LibPath),
      library_absolute = info$LibPath,
      library_index    = match(info$LibPath, .libPaths(), nomatch = 0L),
      version          = info$Version,
      desc             = info$Title,
      loaded           = loaded,
      source           = info$Source,
      browse_url       = info$BrowseUrl,
      check.rows       = TRUE,
      stringsAsFactors = FALSE
   )
   
   # sort and return
   packages[order(packages$name), ]
})

.rs.addJsonRpcHandler("get_package_install_context", function()
{
   # cran mirror configured
   repos = getOption("repos")
   cranMirrorConfigured <- !is.null(repos) && !any(repos == "@CRAN@")
   
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

.rs.addJsonRpcHandler("get_cran_actives", function()
{
   data.frame(name = names(getOption("repos")),
              host = "",
              url = as.character(getOption("repos")),
              country = "",
              ok = TRUE,
              stringsAsFactors = FALSE)
})

.rs.addJsonRpcHandler( "init_default_user_library", function()
{
  .rs.initDefaultUserLibrary()
})

.rs.addJsonRpcHandler("check_for_package_updates", function()
{
   # get updates writeable libraries and convert to a data frame
   updates <- as.data.frame(
      utils::old.packages(lib.loc = .rs.writeableLibraryPaths()),
      stringsAsFactors = FALSE
   )
   row.names(updates) <- NULL
   
   # see which ones are from CRAN and add a news column for them
   # NOTE: defend against length-one repos with no name set
   repos <- getOption("repos")
   cranRep <- if ("CRAN" %in% names(repos))
      repos["CRAN"]
   else
      c(CRAN = repos[[1]])
   
   data.frame(
      packageName = updates$Package,
      libPath     = updates$LibPath,
      installed   = updates$Installed,
      available   = updates$ReposVer,
      stringsAsFactors = FALSE
   )
   
})

.rs.addJsonRpcHandler("get_package_news_url", function(packageName, libraryPath)
{
   # first, check if we've already discovered a NEWS link
   cache <- .rs.packageNewsURLsEnv
   entry <- file.path(libraryPath, packageName)
   if (exists(entry, envir = cache))
      return(get(entry, envir = cache))
   
   # determine an appropriate CRAN URL
   repos <- getOption("repos")
   cran <- if ("CRAN" %in% names(repos))
      repos[["CRAN"]]
   else if (length(repos))
      repos[[1]]
   else
      .Call("rs_rstudioCRANReposUrl", PACKAGE = "(embedding)")
   cran <- gsub("/*$", "", cran)
   
   # check to see if this package was from Bioconductor. if so, we'll need
   # to construct a more appropriate url
   desc <- .rs.tryCatch(.rs.readPackageDescription(file.path(libraryPath, packageName)))
   prefix <- if (inherits(desc, "error") || !"biocViews" %in% names(desc))
      file.path(cran, "web/packages")
   else
      "https://bioconductor.org/packages/release/bioc/news"
   
   # the set of candidate URLs -- we use the presence of a NEWS or NEWS.md
   # to help us prioritize the order of checking.
   #
   # in theory, the current-installed package might not have NEWS at all, but
   # the latest released version might have it after all, so checking the
   # current installed package is just a heuristic and won't be accurate
   # 100% of the time
   pkgPath <- file.path(libraryPath, packageName)
   candidates <- if (file.exists(file.path(pkgPath, "NEWS.md"))) {
      c("news/news.html", "news.html", "NEWS", "ChangeLog")
   } else if (file.exists(file.path(pkgPath, "NEWS"))) {
      c("NEWS", "news/news.html", "news.html", "ChangeLog")
   } else {
      c("news/news.html", "news.html", "NEWS", "ChangeLog")
   }
   
   
   # we do some special handling for 'curl'
   isCurl <- identical(getOption("download.file.method"), "curl")
   if (isCurl) {
      
      download.file.extra <- getOption("download.file.extra")
      on.exit(options(download.file.extra = download.file.extra), add = TRUE)
      
      # guard against NULL, empty extra
      extra <- if (length(download.file.extra))
         download.file.extra
      else
         ""
      
      # add in some extra flags for nicer download output
      addons <- c()
      
      # follow redirects if necessary
      hasLocation <-
         grepl("\b-L\b", extra) ||
         grepl("\b--location\b", extra)
      
      if (!hasLocation)
         addons <- c(addons, "-L")
      
      # fail on 404
      hasFail <-
         grepl("\b-f\b", extra) ||
         grepl("\b--fail\b", extra)
      
      if (!hasFail)
         addons <- c(addons, "-f")
      
      # don't print error output to the console
      hasSilent <-
         grepl("\b-s\b", extra) ||
         grepl("\b--silent\b", extra)
      
      if (!hasSilent)
         addons <- c(addons, "-s")
      
      if (nzchar(extra))
         extra <- paste(extra, paste(addons, collapse = " "))
      else
         extra <- paste(addons, collapse = " ")
      
      options(download.file.extra = extra)
   }
   
   # timeout a bit more quickly when forming web requests
   timeout <- getOption("timeout")
   on.exit(options(timeout = timeout), add = TRUE)
   options(timeout = 4L)
   
   for (candidate in candidates) {
      
      url <- file.path(prefix, packageName, candidate)
      
      # attempt to download the file (note that R preserves curl's printing of errors
      # to the console with 'quiet = TRUE' so we disable it there)
      destfile <- tempfile()
      on.exit(unlink(destfile), add = TRUE)
      status <- .rs.tryCatch(download.file(url, destfile = destfile, quiet = !isCurl, mode = "wb"))
      
      # handle explicit errors
      if (is.null(status) || inherits(status, "error"))
         next
      
      # check for success status
      if (identical(status, 0L)) {
         cache[[entry]] <- .rs.scalar(url)
         return(.rs.scalar(url))
      }
   }
   
   # we failed to figure out the NEWS url; provide our first candidate
   # as the best guess
   fmt <- "Failed to infer appropriate NEWS URL: using '%s' as best-guess candidate"
   warning(sprintf(fmt, candidates[[1]]))
   
   # return that URL
   .rs.scalar(candidates[[1]])
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
   .Call("rs_enqueLoadedPackageUpdates", installCmd, PACKAGE = "(embedding)")
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
   .Call("rs_getCachedAvailablePackages", contribUrl, PACKAGE = "(embedding)")
})

.rs.addFunction("downloadAvailablePackages", function(contribUrl)
{
   .Call("rs_downloadAvailablePackages", contribUrl, PACKAGE = "(embedding)")
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
      
      # Add .registration = TRUE for Rcpp >= 0.12.11
      if (.rs.isPackageVersionInstalled("Rcpp", "0.12.11"))
         registration <- ", .registration = TRUE"
      else
         registration <- ""
      
      # Add an import from Rcpp, and also useDynLib
      NAMESPACE <- c(
         NAMESPACE,
         "importFrom(Rcpp, evalCpp)",
         sprintf("useDynLib(%s%s)", packageName, registration)
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
         #   Install Package:           \'%s\'
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
   cat(text, file = file.path(packageDirectory, "DESCRIPTION"), sep = "\n")
   
   cat(NAMESPACE, file = file.path(packageDirectory, "NAMESPACE"), sep = "\n")
   
   RprojPath <- file.path(
      packageDirectory,
      paste(packageName, ".Rproj", sep = "")
   )
   
   if (!.Call("rs_writeProjectFile", RprojPath, PACKAGE = "(embedding)"))
      return(.rs.error("Failed to create package .Rproj file"))
   
   # Ensure new packages get AutoAppendNewLine + StripTrailingWhitespace
   Rproj <- readLines(RprojPath)
   
   appendNewLineIndex <- grep("AutoAppendNewline:", Rproj, fixed = TRUE)
   if (length(appendNewLineIndex))
      Rproj[appendNewLineIndex] <- "AutoAppendNewline: Yes"
   else
      Rproj <- c(Rproj, "AutoAppendNewline: Yes")
   
   stripTrailingWhitespace <- grep("StripTrailingWhitespace:", Rproj, fixed = TRUE)
   if (length(stripTrailingWhitespace))
      Rproj[stripTrailingWhitespace] <- "StripTrailingWhitespace: Yes"
   else
      Rproj <- c(Rproj, "StripTrailingWhitespace: Yes")
   
   cat(Rproj, file = RprojPath, sep = "\n")
   
   # NOTE: this file is not always generated (e.g. people who have implicitly opted
   # into using devtools won't need the template file)
   if (file.exists(file.path(packageDirectory, "R", "hello.R")))
      .Call("rs_addFirstRunDoc", RprojPath, "R/hello.R", PACKAGE = "(embedding)")

   ## NOTE: This must come last to ensure the other package
   ## infrastructure bits have been generated; otherwise
   ## compileAttributes can fail
   if (usingRcpp &&
       .rs.isPackageVersionInstalled("Rcpp", "0.10.1") &&
       require(Rcpp, quietly = TRUE))
   {
      Rcpp::compileAttributes(packageDirectory)
      if (file.exists(file.path(packageDirectory, "src/rcpp_hello.cpp")))
         .Call("rs_addFirstRunDoc", RprojPath, "src/rcpp_hello.cpp", PACKAGE = "(embedding)")
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

.rs.addFunction("availablePackages", function()
{
   # short-circuit for empty repositories case
   repos <- getOption("repos")
   if (length(repos) == 0) {
      value <- available.packages()
      attr(value, "time") <- Sys.time()
      return(list(state = "CACHED", value = value))
   }
   
   # figure out the current state. possibilities:
   #
   # - STALE:    we need to request available packages
   # - PENDING:  another process is requesting packages
   # - CACHED:   available packages ready in cache
   #
   reposString <- paste(deparse(repos), collapse = " ")
   state <- .rs.availablePackagesState(reposString)
   value <- switch(
      state,
      STALE   = .rs.onAvailablePackagesStale(reposString),
      PENDING = .rs.onAvailablePackagesPending(reposString),
      CACHED  = .rs.onAvailablePackagesCached(reposString),
      NULL
   )
   
   list(state = state, value = value)
})

.rs.addFunction("availablePackagesState", function(reposString)
{
   # do we have a cache entry?
   entry <- .rs.availablePackagesEnv[[reposString]]
   if (!is.null(entry)) {
      
      # verify the cache entry is not stale
      time <- attr(entry, "time", exact = TRUE)
      elapsed <- difftime(Sys.time(), time, units = "secs")
      limit <- as.numeric(Sys.getenv("R_AVAILABLE_PACKAGES_CACHE_CONTROL_MAX_AGE", 3600))
      
      if (elapsed > limit)
         return("STALE")
      else
         return("CACHED")
   }
   
   # do we have a pending dir? if none, we're stale
   dir <- .rs.availablePackagesPendingEnv[[reposString]]
   if (is.null(dir))
      return("STALE")
   
   # is the directory old? if so, assume the prior R process launched
   # to produce available.packages crashed or similar
   info <- file.info(dir)
   time <- info$mtime
   
   # in some cases, mtime may not be available -- in that case, fall
   # back to the time that was serialized when the process was launched
   #
   # https://github.com/rstudio/rstudio/issues/4312
   #
   # if all-else fails, just use the current time. this effectively means
   # that we will never mark the directory as 'stale', which means that
   # the dependency discovery feature may not work -- but at this point
   # there's not much else we can do
   if (is.na(time)) {
      time <- .rs.tryCatch(readRDS(file.path(dir, "time.rds")))
      if (inherits(time, "error"))
         time <- Sys.time()
   }
   
   # check to see if the directory is 'stale'
   diff <- difftime(Sys.time(), time, units = "secs")
   if (diff > 120)
      return("STALE")
   
   # we have a directory, and it's not too old -- we're waiting
   # for a new process to finish
   return("PENDING")
})

.rs.addFunction("onAvailablePackagesStale", function(reposString)
{
   # evict a stale cache entry (if any)
   .rs.availablePackagesEnv[[reposString]] <- NULL
   
   # defend against '@CRAN@' within the repositories, as this will
   # cause R to present the user with an interactive prompt when
   # invoking 'contrib.url()'
   repos <- getOption("repos")
   repos <- repos[repos != "@CRAN@"]
   
   # check and see if R has already queried available packages;
   # if so we can ask R for available packages as it will use
   # the cache
   paths <- vapply(repos, function(url) {
      sprintf("%s/repos_%s.rds", 
              tempdir(), 
              URLencode(contrib.url(url), TRUE)
   )
   }, FUN.VALUE = character(1))
   
   if (all(file.exists(paths))) {
      
      # request available packages
      packages <- if (getRversion() >= "3.5")
         available.packages(max_repo_cache_age = Inf)
      else
         available.packages()
      
      # note accessed time
      attr(packages, "time") <- Sys.time()
      
      # add it to the cache
      .rs.availablePackagesEnv[[reposString]] <- packages
      
      # we're done!
      packages
   }
   
   # prepare directory for discovery of available packages
   dir <- tempfile("rstudio-available-packages-")
   dir.create(dir, showWarnings = FALSE)
   .rs.availablePackagesPendingEnv[[reposString]] <- dir
   
   # mtime may be unreliable (or access could fail in some cases) so
   # instead serialize the current time to a file rather than relying on OS
   saveRDS(Sys.time(), file = file.path(dir, "time.rds"))
   
   # move there
   owd <- setwd(dir)
   on.exit(setwd(owd), add = TRUE)
   
   # define our helper script that will download + save available.packages
   template <- .rs.trimCommonIndent('
      options(repos = %s, pkgType = %s)
      packages <- available.packages()
      attr(packages, "time") <- Sys.time()
      saveRDS(packages, file = "packages.rds")
   ')
   
   script <- sprintf(
      template,
      .rs.deparse(getOption("repos")),
      .rs.deparse(getOption("pkgType"))
   )
   
   # fire off the process
   .rs.runAsyncRProcess(
      script,
      onCompleted = function(exitStatus) {
         
         # bail on error (don't log since this might occur
         # for reasons not actionable by the user; e.g. restarting
         # the R session while a lookup is happening)
         if (exitStatus)
            return()
         
         available <- .rs.onAvailablePackagesReady(reposString)
         data <- list(
            ready = .rs.scalar(TRUE),
            packages = rownames(available)
         )
         .rs.enqueClientEvent("available_packages_ready", data)
      }
   )
   
   # NULL indicates we don't have available packages yet
   return(NULL)
   
})

.rs.addFunction("onAvailablePackagesPending", function(reposString)
{
   # nothing to do here
   invisible(NULL)
})

.rs.addFunction("onAvailablePackagesReady", function(reposString)
{
   # get the directory and read packages.rds
   dir <- .rs.availablePackagesPendingEnv[[reposString]]
   rds <- file.path(dir, "packages.rds")
   
   # attempt to read the database and add it to the cache
   packages <- .rs.tryCatch(readRDS(rds))
   if (!inherits(packages, "error"))
      .rs.availablePackagesEnv[[reposString]] <- packages
   
   # remove state directory and mark as no longer pending
   unlink(dir, recursive = TRUE)
   .rs.availablePackagesPendingEnv[[reposString]] <- NULL
   
   # we're done!
   packages
})

.rs.addFunction("onAvailablePackagesCached", function(reposString)
{
   .rs.availablePackagesEnv[[reposString]]
})

.rs.addFunction("parseSecondaryReposIni", function(conf) {
   entries <- .rs.readIniFile(conf)
   repos <- list()

   for (entryName in names(entries)) {
     repo <- list(
        name  = .rs.scalar(trimws(entryName)),
        url = .rs.scalar(trimws(entries[[entryName]])),
        host = .rs.scalar("Custom"),
        country = .rs.scalar("")
     )

     if (identical(tolower(as.character(repo$name)), "cran")) {
        repo$name <- .rs.scalar("CRAN")
        repos <- append(list(repo), repos, 1)
     } else {
        repos[[length(repos) + 1]] <- repo
     }
   }

   repos
})

.rs.addFunction("parseSecondaryReposJson", function(conf) {
   lines <- readLines(conf)
   repos <- list()

   entries <- .rs.fromJSON(paste(lines, collpse = "\n"))

   for (entry in entries) {
      url <- if (is.null(entry$url)) "" else url

      repo <- list(
         name  = .rs.scalar(entry$name),
         url = .rs.scalar(url),
         host = .rs.scalar("Custom"),
         country = .rs.scalar("")
      )

      if (identical(tolower(as.character(repo$name)), "cran")) {
         repo$name <- .rs.scalar("CRAN")
         repos <- append(list(repo), repos, 1)
      } else {
         repos[[length(repos) + 1]] <- repo
      }
   }

   repos
})

.rs.addFunction("getSecondaryRepos", function(cran = getOption("repos")[[1]], custom = TRUE) {
   result <- list(
      repos = list()
   )
   
   rCranReposUrl <- .Call("rs_getCranReposUrl", PACKAGE = "(embedding)")
   isDefault <- identical(rCranReposUrl, NULL) || nchar(rCranReposUrl) == 0

   if (isDefault) {
      slash <- if (.rs.lastCharacterIs(cran, "/")) "" else "/"
      rCranReposUrl <- paste(slash, "../../__api__/repos", sep = "")
   }
   else {
      custom <- TRUE
   }

   if (.rs.startsWith(rCranReposUrl, "..") ||
       .rs.startsWith(rCranReposUrl, "/..")) {
      rCranReposUrl <- .rs.completeUrl(cran, rCranReposUrl)
   }

   if (custom) {
      conf <- tempfile(fileext = ".conf")
      
      result <- tryCatch({
         download.file(
            rCranReposUrl,
            conf,
            method = "curl",
            extra = "-H 'Accept: text/ini'",
            quiet = TRUE
         )
         
         result$repos <- .rs.parseSecondaryReposIni(conf)
         if (length(result$repos) == 0) {
            result$repos <- .rs.parseSecondaryReposJson(conf)
         }

         result
      }, error = function(e) {
         list(
            error = .rs.scalar(
               paste(
                  "Failed to process repos list from ",
                  rCranReposUrl, ". ", e$message, ".", sep = ""
               )
            )
         )
      })
   }

   result
})

.rs.addJsonRpcHandler("get_secondary_repos", function(cran, custom) {
   .rs.getSecondaryRepos(cran, custom)
})

.rs.addFunction("appendSlashIfNeeded", function(url) {
   slash <- if (.rs.lastCharacterIs(url, "/")) "" else "/"
   paste(url, slash, sep = "")
})

.rs.addJsonRpcHandler("validate_cran_repo", function(url) {
   packagesFile <- tempfile(fileext = ".gz")
   
   tryCatch({
      download.file(
         .rs.completeUrl(.rs.appendSlashIfNeeded(url), "src/contrib/PACKAGES.gz"),
         packagesFile,
         quiet = TRUE
      )

      .rs.scalar(TRUE)
   }, error = function(e) {
      .rs.scalar(FALSE)
   })
})

.rs.addJsonRpcHandler("is_package_installed", function(package, version)
{
   installed <- if (is.null(version))
      .rs.isPackageInstalled(package)
   else
      .rs.isPackageVersionInstalled(package, version)
   .rs.scalar(installed)
})
