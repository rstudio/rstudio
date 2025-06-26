#
# SessionPackages.R
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

# cached URLs for package NEWS files
.rs.setVar("packageNewsURLsEnv", new.env(parent = emptyenv()))

# cached information on available R packages
.rs.setVar("availablePackagesEnv", new.env(parent = emptyenv()))

# for asynchronous requests on available package information,
# map the repository string to the directory containing produced output
.rs.setVar("availablePackagesPendingEnv", new.env(parent = emptyenv()))

.rs.addJsonRpcHandler("is_package_installed", function(package, version)
{
   installed <- if (is.null(version))
      .rs.isPackageInstalled(package)
   else
      .rs.isPackageVersionInstalled(package, version)
   .rs.scalar(installed)
})

.rs.addJsonRpcHandler("is_package_attached", function(packageName, libraryPath)
{
   # quick check first if the package is loaded
   if (!isNamespaceLoaded(packageName))
      return(.rs.scalar(FALSE))
   
   # get the raw path to the package
   packagePath <- .rs.pathPackage(packageName, quiet = TRUE)
   if (length(packagePath) == 0L)
      return(.rs.scalar(FALSE))
   
   # get associated library path
   packageLibraryPath <- dirname(packagePath)
   
   # compare with the library given by the client
   samePath <- identical(
      normalizePath(libraryPath, winslash = "/", mustWork = FALSE),
      normalizePath(packageLibraryPath, winslash = "/", mustWork = FALSE)
   )
   
   .rs.scalar(samePath)
})

.rs.addJsonRpcHandler("is_package_hyperlink_safe", function(packageName)
{
   .rs.isPackageHyperlinkSafe(packageName)
})

.rs.addJsonRpcHandler("get_package_install_context", function()
{
   # check if the '@CRAN@' placeholder is present in repos
   repos <- getOption("repos")
   cranMirrorConfigured <- !"@CRAN@" %in% repos
   
   # selected repository names (assume an unnamed repo == CRAN)
   # TODO: What if no repositories are set?
   selectedRepositoryNames <- names(repos)
   if (is.null(selectedRepositoryNames))
      selectedRepositoryNames <- "CRAN"
   
   # package archive extension
   packageArchiveExtension <- switch(
      Sys.info()[["sysname"]],
      Windows = ".zip; .tar.gz",
      Darwin  = ".tgz; .tar.gz",
      ".tar.gz"
   )
   
   # default library path (normalize on unix)
   defaultLibraryPath = .libPaths()[1L]
   if (!identical(.Platform$OS.type, "windows"))
      defaultLibraryPath <- .rs.normalizePath(defaultLibraryPath)
   
   # return context
   list(
      cranMirrorConfigured = cranMirrorConfigured,
      selectedRepositoryNames = selectedRepositoryNames,
      packageArchiveExtension = packageArchiveExtension,
      defaultLibraryPath = defaultLibraryPath,
      defaultLibraryWriteable = .rs.defaultLibPathIsWriteable(),
      writeableLibraryPaths = .rs.writeableLibraryPaths(),
      defaultUserLibraryPath = .rs.defaultUserLibraryPath(),
      devModeOn = .rs.devModeOn()
   )
})

.rs.addJsonRpcHandler("get_cran_mirrors", function()
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
   # Exit early if we don't have any repositories set.
   repos <- getOption("repos")
   if (length(repos) == 0L)
      return(list())
   
   # Get all of the currently-installed packages. We list packages for each
   # library separately, so we can merge them appropriately after.
   installedPkgsList <- lapply(.libPaths(), function(libPath)
   {
      pkgPaths <- list.files(libPath, full.names = TRUE)
      
      pkgDescs <- lapply(pkgPaths, function(pkgPath)
      {
         pkgDesc <- tryCatch(
            .rs.readPackageDescription(pkgPath),
            condition = function(cnd) NULL
         )
         
         pkgDesc[c("Package", "Version")]
      })
      
      pkgDescs <- Filter(length, pkgDescs)
      if (length(pkgDescs) == 0L)
         return(NULL)
      
      pkgResult <- .rs.rbindList(pkgDescs)
      pkgResult[["LibPath"]] <- libPath
      pkgResult
   })
   
   installedPkgs <- .rs.rbindList(installedPkgsList)
   
   # Remove duplicates.
   installedPkgs <- installedPkgs[!duplicated(installedPkgs$Package), ]
   
   # Compare installed packages with what's available from package repositories.
   availablePkgs <- as.data.frame(
      utils::available.packages(),
      stringsAsFactors = FALSE
   )
   
   # Keep only the package name and version component.
   availablePkgs <- data.frame(
      Package  = availablePkgs[["Package"]],
      ReposVer = availablePkgs[["Version"]],
      stringsAsFactors = FALSE
   )
   
   # Merge in the repository version components.
   allPkgs <- merge(
      x  = installedPkgs,
      y  = availablePkgs,
      by = "Package"
   )
   
   # Figure out which packages are out-of-date.
   oldPkgs <- subset(allPkgs, numeric_version(Version) < numeric_version(ReposVer))
   
   # Add in version fields as appropriate.
   data.frame(
      packageName = oldPkgs[["Package"]],
      libPath     = oldPkgs[["LibPath"]],
      installed   = oldPkgs[["Version"]],
      available   = oldPkgs[["ReposVer"]],
      stringsAsFactors = FALSE
   )
   
})

.rs.addJsonRpcHandler("get_package_citations", function(packageName, libraryPath)
{
   toPerson <- function(author) {
      list(
         given = author[["given"]],
         family = .rs.scalar(author[["family"]]),
         email = .rs.scalar(author[["email"]]),
         role = .rs.scalar(author[["role"]])
      )
   }
   
   cites <- citation(packageName)
   lapply(unclass(cites), function(cite) {
      list(
         # bibtex type
         type = .rs.scalar(attr(cite, "bibtype")),
         
         title = .rs.scalar(cite[["title"]]),
         url = .rs.scalar(cite[["url"]]),
         note = .rs.scalar(cite[["note"]]),
         doi = .rs.scalar(cite[["doi"]]),
         
         publisher = .rs.scalar(cite[["publisher"]]),
         institution = .rs.scalar(cite[["institution"]]),
         address = .rs.scalar(cite[["address"]]),
         
         journal = .rs.scalar(cite[["journal"]]),
         year = .rs.scalar(cite[["year"]]),
         booktitle = .rs.scalar(cite[["booktitle"]]),
         chapter = .rs.scalar(cite[["chapter"]]),
         number = .rs.scalar(cite[["number"]]),
         volume = .rs.scalar(cite[["volume"]]),
         pages = .rs.scalar(cite[["pages"]]),
         series = .rs.scalar(cite[["series"]]),
         school = .rs.scalar(cite[["school"]]),
         
         
         # person
         author = lapply(unclass(cite[["author"]]), toPerson),
         editor = lapply(unclass(cite[["editor"]]), toPerson)
      )
   })
})

.rs.addJsonRpcHandler("get_package_news_url", function(packageName, libraryPath)
{
   # first, check if we've already discovered a NEWS link
   cache <- .rs.packageNewsURLsEnv
   entry <- file.path(libraryPath, packageName)
   if (exists(entry, envir = cache))
      return(get(entry, envir = cache))
   
   # determine an appropriate repository URL for this package
   # default to our public CRAN repository
   cran <- .Call("rs_rstudioCRANReposUrl", PACKAGE = "(embedding)")
   
   # check whether the requested package is from a separate repository URL
   # note that the 'Repository' entry below will include a suffix based on the
   # package type, so we need to trim that after
   db <- as.data.frame(available.packages(), stringsAsFactors = FALSE)
   if ("Repository" %in% names(db)) {
      index <- match(packageName, db$Package)
      if (!is.na(index)) {
         repo <- db$Repository[index]
         cran <- gsub("/(?:src|bin)/.*", "", repo)
      }
   }
   
   # re-route PPM URLs to CRAN for now
   # https://github.com/rstudio/rstudio/issues/12648
   isPpm <-
      grepl("^\\Qhttp://rspm/\\E", cran, perl = TRUE) ||
      grepl("^\\Qhttps://packagemanager.posit.co/\\E", cran, perl = TRUE) ||
      grepl("^\\Qhttps://packagemanager.rstudio.com/\\E", cran, perl = TRUE) ||
      grepl("^\\Qhttps://p3m.dev/\\E", cran, perl = TRUE)
   
   if (isPpm)
      cran <- "https://cloud.R-project.org"
   
   # check to see if this package was from Bioconductor. if so, we'll need
   # to construct a more appropriate url
   desc <- .rs.tryCatch(.rs.readPackageDescription(file.path(libraryPath, packageName)))
   prefix <- if ("biocViews" %in% names(desc))
      "https://bioconductor.org/packages/release/bioc/news"
   else
      file.path(cran, "web/packages")
   
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

.rs.addJsonRpcHandler("loaded_package_updates_required", function(pkgs)
{
   .rs.scalar(.rs.loadedPackageUpdates(as.character(pkgs)))
})

.rs.addJsonRpcHandler("ignore_next_loaded_package_check", function() {
   .rs.setVar("ignoreNextLoadedPackageCheck", TRUE)
   return(NULL)
})

.rs.addJsonRpcHandler("package_skeleton", function(packageName,
                                                   packageDirectory,
                                                   sourceFiles,
                                                   usingRcpp)
{
   # mark encoding
   Encoding(packageDirectory) <- "UTF-8"
   
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
   # Protect against vectors with length > 1.
   getDevtoolsOption <- function(optionName,
                                 default = NULL,
                                 collapse = " ")
   {
      for (descKey in c("usethis.description", "devtools.desc"))
      {
         descValue <- getOption(descKey)
         if (length(descValue) == 0L)
            next
         
         optionValue <- descValue[[optionName]]
         if (length(optionValue) == 0L)
            next
         
         # Check for 'person' objects, and expand those in a way
         # that will be formatted nicely in the DESCRIPTION file.
         if (inherits(optionValue, "person"))
            optionValue <- .rs.formatPerson(optionValue)
         
         return(paste(optionValue, collapse = collapse))
      }
      
      default
   }
   
   authorsDefault <- .rs.heredoc('
      c(
        person(
          "Jane", "Doe",
          email = "jane@example.com",
          role = c("aut", "cre")
        )
      )
   ')
   
   authors <- getDevtoolsOption(
      "Authors@R",
      gsub("\n", "\n  ", authorsDefault, fixed = TRUE),
      collapse = "\n"
   )
   
   license <- getDevtoolsOption(
      "License",
      "What license is it under?",
      ", "
   )
   
   DESCRIPTION <- list(
      Package = packageName,
      Type = "Package",
      Title = "What the Package Does (Title Case)",
      Version = "0.1.0",
      "Authors@R" = authors,
      Description = c(
         "More about what it does (maybe more than one line).",
         "Continuation lines should be indented."
      ),
      License = license,
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
   suggests <- .rs.nullCoalesce(
      getOption("devtools.desc.suggests"),
      getDevtoolsOption("Suggests")
   )
   
   if (length(suggests))
      DESCRIPTION$Suggests <- suggests
   
   # Add in any other options
   desc <- .rs.nullCoalesce(
      getOption("usethis.description"),
      getOption("devtools.desc")
   )
   
   .rs.enumerate(desc, function(key, value)
   {
      DESCRIPTION[[key]] <<- .rs.nullCoalesce(
         DESCRIPTION[[key]],
         value
      )
   })
   
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
      holder <- .rs.nullCoalesce(
         getOption("usethis.full_name"),
         getOption("devtools.name"),
         "<Copyright holder>"
      )
      
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
         #   https://r-pkgs.org
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
            //   https://www.rcpp.org/
            //   https://adv-r.hadley.nz/rcpp.html
            //
            // and browse examples of code using Rcpp at:
            // 
            //   https://gallery.rcpp.org/
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
         "vignettes" = c("rmd", "rnw", "qmd"),
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
   
   # compute scratch paths
   scratchPaths <- .Call("rs_computeScratchPaths", RprojPath, PACKAGE = "(embedding)")
   scratchPath <- scratchPaths$scratch_path
   
   # NOTE: this file is not always generated (e.g. people who have implicitly opted
   # into using devtools won't need the template file)
   if (!is.null(scratchPath) &&
       file.exists(file.path(packageDirectory, "R", "hello.R")))
   {
      .Call("rs_addFirstRunDoc", scratchPath, "R/hello.R", PACKAGE = "(embedding)")
   }
   
   ## NOTE: This must come last to ensure the other package
   ## infrastructure bits have been generated; otherwise
   ## compileAttributes can fail
   if (usingRcpp &&
       .rs.isPackageVersionInstalled("Rcpp", "0.10.1") &&
       require(Rcpp, quietly = TRUE))
   {
      Rcpp::compileAttributes(packageDirectory)
      if (!is.null(scratchPath) &&
          file.exists(file.path(packageDirectory, "src/rcpp_hello.cpp")))
      {
         .Call("rs_addFirstRunDoc", scratchPath, "src/rcpp_hello.cpp", PACKAGE = "(embedding)")
      }
   }
   
   .rs.success()
   
})

.rs.addJsonRpcHandler("validate_cran_repo", function(url) {
   
   url <- .rs.completeUrl(.rs.appendSlashIfNeeded(url), "src/contrib/PACKAGES.gz")
   packagesFile <- tempfile(fileext = ".gz")
   
   # ensure warnings are not suppressed in this scope
   op <- options(warn = 1L)
   on.exit(options(op), add = TRUE)
   
   # execute download.file(), but capture warnings and errors
   warnings <- list()
   error <- NULL
   
   withCallingHandlers(
      
      tryCatch(
         download.file(url, packagesFile, quiet = TRUE),
         error = function(cnd) error <<- cnd
      ),
      
      warning = function(cnd) {
         warnings[[length(warnings) + 1L]] <<- cnd
         restart <- findRestart("muffleWarning")
         if (isRestart(restart))
            invokeRestart(restart)
      }
      
   )
   
   
   # build a message for display to client if an error occurred
   message <- ""
   if (length(error))
      message <- paste("Error:", conditionMessage(error))
   
   if (length(warnings)) {
      msgs <- vapply(warnings, conditionMessage, FUN.VALUE = character(1))
      message <- paste(message, paste("Warning:", msgs, collapse = "\n"), sep = "\n")
   }
   
   result <- list(
      valid = file.exists(packagesFile) && is.null(error),
      error = message
   )
   
   .rs.scalarListFromList(result)
   
})

.rs.addJsonRpcHandler("get_secondary_repos", function(cran, custom)
{
   .rs.getSecondaryRepos(cran, custom)
})


# a vectorized function that takes any number of paths and aliases the home
# directory in those paths (i.e. "/Users/bob/foo" => "~/foo"), leaving any 
# paths outside the home directory untouched
.rs.addFunction("createAliasedPath", function(path)
{
   homeDir <- path.expand("~/")
   homePathIdx <- substr(path, 1L, nchar(homeDir)) == homeDir
   homePaths <- path[homePathIdx]
   homeSuffix <- substr(homePaths, nchar(homeDir), nchar(homePaths))
   path[homePathIdx] <- paste0("~", homeSuffix)
   path
})

# Some R commands called during packaging-related operations (such as untar)
# delegate to the system tar binary specified in TAR. On OS X, R may set TAR to
# /usr/bin/gnutar, which exists prior to Mavericks (10.9) but not in later
# rleases of the OS. In the special case wherein the TAR environment variable
# on OS X is set to a non-existent gnutar and there exists a tar at
# /usr/bin/tar, tell R to use that binary instead.
if (identical(as.character(Sys.info()["sysname"]), "Darwin") &&
    identical(Sys.getenv("TAR"), "/usr/bin/gnutar") && 
    !file.exists("/usr/bin/gnutar") &&
    file.exists("/usr/bin/tar"))
{
   Sys.setenv(TAR = "/usr/bin/tar")
}

.rs.addFunction("beforePackageUnloaded", function(package)
{
   # force any promises associated with package's registered S3 methods --
   # this is necessary as otherwise the lazy-load database may become corrupt
   # if a new version of this package is later installed as the old promises
   # will now have invalid pointers to the old lazy-load database.
   #
   # note that we iterate over all loaded packages here because loading a
   # package might entail registering S3 methods in the namespace of the
   # package owning the generic, which typically is a separate package
   #
   # https://bugs.r-project.org/bugzilla/show_bug.cgi?id=16644
   # https://github.com/rstudio/rstudio/issues/8265
   for (namespaceName in loadedNamespaces())
   {
      .rs.tryCatch({
         ns <- asNamespace(namespaceName)
         table <- ns[[".__S3MethodsTable__."]]
         as.list(table)
      })
   }
})

.rs.addFunction("updatePackageEvents", function()
{
   reportPackageStatus <- function(attached)
   {
      function(pkgname, ...)
      {
         packagePath <- .rs.pathPackage(pkgname, quiet = TRUE)
         packageStatus = list(
            name     = I(pkgname),
            library  = I(dirname(packagePath)),
            path     = I(.rs.createAliasedPath(packagePath)),
            attached = I(attached)
         )
         .rs.enqueClientEvent("package_status_changed", packageStatus)
      }
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
      .rs.beforePackageUnloaded(pkgname)
      .Call("rs_packageUnloaded", pkgname, PACKAGE = "(embedding)")
   }
   
   pkgNames <-
      base::list.dirs(.libPaths(), full.names = FALSE, recursive = FALSE)
   
   sapply(pkgNames, function(packageName)
   {
      if ( !(packageName %in% .rs.hookedPackages) )
      {
         attachEventName = packageEvent(packageName, "attach")
         setHook(attachEventName, reportPackageStatus(TRUE), action = "append")
         
         loadEventName = packageEvent(packageName, "onLoad")
         setHook(loadEventName, notifyPackageLoaded, action = "append")

         unloadEventName = packageEvent(packageName, "onUnload")
         setHook(unloadEventName, notifyPackageUnloaded, action = "append")
             
         detachEventName = packageEvent(packageName, "detach")
         setHook(detachEventName, reportPackageStatus(FALSE), action = "append")
          
         .rs.setVar("hookedPackages", append(.rs.hookedPackages, packageName))
      }
   })
})

.rs.addFunction("packages.initialize", function()
{  
   # list of packages we have hooked attach/detach for
   .rs.setVar("hookedPackages", character())

   # set flag indicating we should not ignore loadedPackageUpdates checks
   .rs.setVar("ignoreNextLoadedPackageCheck", FALSE)
    
   # ensure we are subscribed to package attach/detach events
   .rs.updatePackageEvents()
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

   subset(libPaths, !duplicated(libPaths))
})

.rs.addFunction( "writeableLibraryPaths", function()
{
   uniqueLibraryPaths <- .rs.uniqueLibraryPaths()
   writeableLibraryPaths <- character()
   for (libPath in uniqueLibraryPaths)
      if (.rs.isLibraryWriteable(libPath))
         writeableLibraryPaths <- append(writeableLibraryPaths, libPath)
   writeableLibraryPaths
})

.rs.addFunction("defaultUserLibraryPath", function()
{
   unlist(strsplit(Sys.getenv("R_LIBS_USER"), .Platform$path.sep))[1L]
})

.rs.addFunction("defaultLibraryPath", function()
{
  .libPaths()[1]
})

.rs.addFunction("isPackageHyperlinkSafe", function(packageName)
{
   allowed <- setdiff(
      c(.packages(), "testthat", "rlang", "devtools", "usethis", "pkgload", "pkgdown"), 
      c("base", "stats", "utils")
   )
   .rs.scalar(
      packageName %in% allowed
   )
})

.rs.addFunction("forceUnloadPackage", function(package)
{
   tryCatch(
      withCallingHandlers(
         .rs.forceUnloadPackageImpl(package),
         warning = function(w) invokeRestart("muffleWarning")
      ),
      error = warning
   )
})

.rs.addFunction("forceUnloadPackageImpl", function(package)
{
   .rs.beforePackageUnloaded(package)
   
   # Figure out where the package was loaded from before unloading it.
   pkgPath <- if (package %in% loadedNamespaces())
   {
      getNamespaceInfo(package, "path")
   }
   
   # Now, try to unload the package. If it's attached, detach it;
   # if the namespace is loaded, unload it.
   searchPathName <- paste("package", package, sep = ":")
   if (searchPathName %in% search())
   {
      detach(
         name = searchPathName,
         unload = TRUE,
         character.only = TRUE,
         force = TRUE
      )
   }
   else if (package %in% loadedNamespaces())
   {
      unloadNamespace(package)
   }
   
   # Now, unload a loaded DLL (if any) associated with the package.
   dllInfo <- getLoadedDLLs()[[package]]
   if (!is.null(dllInfo) && !is.null(pkgPath))
   {
      suppressWarnings(
         library.dynam.unload(package, pkgPath)
      )
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

.rs.addFunction("findPackage", function(pkgPath)
{
   if (grepl("/", pkgPath, fixed = TRUE))
      pkgPath
   else
      find.package(pkgPath, quiet = TRUE)
})

.rs.addFunction("inferPackageSource", function(pkgDesc)
{
   # Check for 'base' packages.
   if (identical(pkgDesc[["Priority"]], "base"))
      return("Base")
   
   # Compute the package remote type, assuming "stanadrd" if none specified.
   rtype <- .rs.nullCoalesce(pkgDesc[["RemoteType"]], "standard")
   
   # Handle 'local' remotes.
   if (identical(rtype, "local"))
   {
      path <- pkgDesc[["RemoteUrl"]]
      if (is.null(path))
      {
         path <- pkgDesc[["RemoteUrl"]]
         if (!is.null(path))
         {
            path <- sub("^local::", "", path, perl = TRUE)
         }
      }
      
      return(sprintf("Local [%s]", .rs.nullCoalesce(path, "?")))
   }
   
   # Handle 'standard' remotes.
   
   if (identical(rtype, "standard"))
   {
      source <- .rs.nullCoalesce(
         pkgDesc[["RemoteReposName"]],
         pkgDesc[["RemoteRepos"]],
         pkgDesc[["Repository"]]
      )
      
      # Shorten the name for Bioconductor remotes.
      source <- sub("Bioconductor", "BioC", source, fixed = TRUE)
      
      # Check for R-universe remotes, and format them specially.
      pattern <- "^https?://(.*)\\.r-universe\\.dev/?$"
      m <- regexec(pattern, source)
      matches <- regmatches(source, m)[[1L]]
      if (length(matches))
         source <- sprintf("R-universe [%s]", matches[[2L]])
      
      # Check for PPM remorts, and format them specially.
      if (identical(pkgDesc[["Repository"]], "RSPM"))
      {
         repos <- pkgDesc[["RemoteRepos"]]
         if (!is.null(repos))
         {
            ppm <- if (grepl("https://p3m.dev/", repos, fixed = TRUE))
               "p3m.dev"
            else if (grepl("https://packagemanager.posit.co/", repos, fixed = TRUE))
               "P3M"
            else
               "PPM"
            
            snapshot <- basename(repos)
            name <- basename(dirname(repos))
            source <- sprintf("%s [%s/%s]", ppm, name, snapshot)
         }
      }
      
      return(source)
   }
   
   # Handle other remotes. Start by mapping the RemoteType field
   # into a "pretty" version of the type.
   aliases <- list(
      bioc         = "Bioconductor",
      bioconductor = "Bioconductor",
      bitbucket    = "Bitbucket",
      cran         = "CRAN",
      git2r        = "Git",
      github       = "GitHub",
      gitlab       = "GitLab",
      local        = "Local",
      repository   = "Repository",
      standard     = "Repository",
      url          = "URL",
      xgit         = "Git"
   )
   
   type <- .rs.nullCoalesce(aliases[[rtype]], rtype)
   
   # Check for packages installed from e.g. hosted Git repositories.
   user <- pkgDesc[["RemoteUsername"]]
   repo <- pkgDesc[["RemoteRepo"]]
   if (!is.null(user) && !is.null(repo))
   {
      source <- sprintf("%s [%s/%s]", type, user, repo)
      return(source)
   }
   
   # If we couldn't infer any more details, just use the type.
   type
   
})

.rs.addFunction("inferPackageBrowseUrl", function(pkgDesc)
{
   repos <- as.list(getOption("repos"))
   
   # For packages that were installed from CRAN or RSPM, browse
   # to the package page associated with the repository.
   repository <- pkgDesc[["Repository"]]
   if (identical(repository, "RSPM") || identical(repository, "CRAN"))
   {
      repoUrl <- .rs.nullCoalesce(
         pkgDesc[["RemoteRepos"]],
         repos[[repository]],
         repos[[1L]]
      )
      
      if (length(repoUrl))
      {
         url <- sprintf("%s/package=%s", repoUrl[[1L]], pkgDesc[["Package"]])
         return(url)
      }
   }
   
   # Try to build a link for packages installed from other remotes.
   host <- pkgDesc[["RemoteHost"]]
   user <- pkgDesc[["RemoteUsername"]]
   repo <- pkgDesc[["RemoteRepo"]]
   
   if (!is.null(host) && !is.null(user) && !is.null(repo))
   {
      host <- sub("api.github.com", "github.com", host, fixed = TRUE)
      url <- sprintf("https://%s/%s/%s", host, user, repo)
      return(url)
   }
   
   # If 'Repository' appears to be a URL, use it directly.
   if (is.character(repository) && grepl("^https://", repository))
   {
      return(repository)
   }
   
   # If this appears to be a Bioconductor package, link there.
   if (!is.null(pkgDesc[["biocViews"]]))
   {
      mirror <- getOption("BioC_mirror", default = "https://bioconductor.org")
      fmt <- "%s/packages/release/bioc/html/%s.html"
      url <- sprintf(fmt, mirror, pkgDesc[["Package"]])
      return(url)
   }
   
   # Give up.
   ""
})

.rs.addFunction("inferPackageDocumentationUrl", function(pkgDesc)
{
   url <- .rs.nullCoalesce(pkgDesc[["URL"]], "")
   sub("[[:space:],].*", "", url)
})

.rs.addFunction("listInstalledPackages", function()
{
   # Look for packages in the library paths.
   pkgPaths <- list.files(.rs.uniqueLibraryPaths(), full.names = TRUE)
   
   # Include only packages which have a 'Meta' sub-directory.
   hasMeta <- file.exists(file.path(pkgPaths, "Meta"))
   pkgPaths <- pkgPaths[hasMeta]
   
   # Iterate over these packages, and read their DESCRIPTION files.
   pkgInfos <- lapply(pkgPaths, function(pkgPath) {
      
      pkgDesc <- tryCatch(
         .rs.readPackageDescription(pkgPath),
         condition = function(e) list(Package = basename(pkgPath))
      )
      
      # Pull out package name for later use.
      pkgName <- pkgDesc[["Package"]]
      
      # Keep only fields of interest.
      pkgInfo <- pkgDesc[c("Package", "Title", "Version")]
      
      # Re-map names. Note that these names are also used and expected by
      # panmirror / Quarto.
      names(pkgInfo) <- c("name", "desc", "version")
      
      # Also record metadata about the library where it was found.
      libraryPath <- dirname(pkgPath)
      pkgInfo[["library"]] <- .rs.createAliasedPath(libraryPath)
      pkgInfo[["library_absolute"]] <- libraryPath
      pkgInfo[["library_index"]] <- match(libraryPath, .libPaths(), nomatch = 0L)
      
      # Also note which packages appear to be loaded or attached.
      isLoaded <- FALSE
      if (pkgName %in% loadedNamespaces())
      {
         isLoaded <-
            identical(pkgDesc[["Priority"]], "base") ||
            identical(getNamespaceInfo(pkgName, "path"), pkgPath)
      }
      
      isAttached <-
         isLoaded &&
         paste("package", pkgName, sep = ":") %in% search()
      
      pkgInfo[["loaded"]] <- isLoaded
      pkgInfo[["attached"]] <- isAttached
      
      pkgInfo[["source"]] <- tryCatch(
         .rs.inferPackageSource(pkgDesc),
         error = function(cnd) "[Unknown]"
      )
      
      pkgInfo[["browse_url"]] <- tryCatch(
         .rs.inferPackageBrowseUrl(pkgDesc),
         error = function(cnd) ""
      )
      
      pkgInfo[["package_url"]] <- tryCatch(
         .rs.inferPackageDocumentationUrl(pkgDesc),
         error = function(cnd) ""
      )
      
      # Return the resulting object.
      .rs.scalarListFromList(pkgInfo)
      
   })
   
   # Sort based on the package name.
   pkgOrder <- order(.rs.mapChr(pkgInfos, `[[`, "name"))
   pkgInfos <- pkgInfos[pkgOrder]
   
   # And we're done.
   pkgInfos
})

.rs.addFunction("readPackageImports", function(pkg)
{
   pkgPath <- find.package(pkg, quiet = TRUE)
   if (length(pkgPath) == 0L)
      return(character())
   
   metaPath <- file.path(pkgPath, "Meta/package.rds")
   if (!file.exists(metaPath))
      return(character())
   
   metaInfo <- readRDS(metaPath)
   importInfo <- metaInfo$Imports
   sort(unique(names(importInfo)))
})

.rs.addFunction("recursivePackageDependenciesImpl", function(pkg, envir)
{
   if (exists(pkg, envir = envir))
      return()
   
   deps <- setdiff(.rs.readPackageImports(pkg), "base")
   assign(pkg, deps, envir = envir)
   
   for (dep in deps)
      .rs.recursivePackageDependenciesImpl(dep, envir)
})

.rs.addFunction("recursivePackageDependencies", function(pkgs)
{
   envir <- new.env(parent = emptyenv())
   for (pkg in pkgs)
      .rs.recursivePackageDependenciesImpl(pkg, envir)
   as.list(envir, all.names = TRUE)
})

.rs.addFunction("packagesLoaded", function(pkgs)
{
   # exclude base packages
   basePkgs <- rownames(installed.packages(lib.loc = .Library, priority = "base"))
   pkgs <- setdiff(pkgs, basePkgs)
   
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
   
   # first, check if the packages themselves are loaded
   if (.rs.packagesLoaded(pkgs))
      return(TRUE)
   
   # next, check if any of these package's dependencies are loaded
   recdeps <- .rs.recursivePackageDependencies(pkgs)
   if (.rs.packagesLoaded(recdeps))
      return(TRUE)
   
   FALSE
})

.rs.addFunction("loadedPackagesAndDependencies", function(pkgs)
{
   recdeps <- .rs.recursivePackageDependencies(pkgs)
   sort(unique(unlist(recdeps)))
})

.rs.addFunction("forceUnloadForPackageInstall", function(pkgs)
{
   sapply(pkgs, .rs.forceUnloadPackage)
   pkgs
})

.rs.addFunction("enqueLoadedPackageUpdates", function(installCmd)
{
   .Call("rs_enqueLoadedPackageUpdates", installCmd, PACKAGE = "(embedding)")
})

.rs.addFunction("getCachedAvailablePackages", function(contribUrl)
{
   .Call("rs_getCachedAvailablePackages", contribUrl, PACKAGE = "(embedding)")
})

.rs.addFunction("downloadAvailablePackages", function(contribUrl)
{
   .Call("rs_downloadAvailablePackages", contribUrl, PACKAGE = "(embedding)")
})

# Formats a person object in a way suitable for the Authors@R
# section of a DESCRIPTION file.
.rs.addFunction("formatPerson", function(person)
{
   personFields <- unclass(person)
   if (length(personFields) == 1L)
   {
      formattedPerson <- .rs.formatPersonImpl(personFields[[1L]])
      return(gsub("\n", "\n  ", formattedPerson))
   }
   
   formattedPersons <- paste(
      .rs.mapChr(personFields, .rs.formatPersonImpl, indent = "    "),
      collapse = ",\n"
   )
   
   paste(c("c(", formattedPersons, "  )"), collapse = "\n")
})

.rs.addFunction("formatPersonImpl", function(fields, indent = identity)
{
   if (is.character(indent))
   {
      indentWidth <- indent
      indent <- function(x) sprintf("%s%s", indentWidth, x)
   }
   
   # Build header from given + family name.
   fullName <- c(fields$given, fields$family, fields$middle)
   header <- paste(shQuote(fullName, type = "cmd"), collapse = ", ")
   
   # Build body from remaining fields
   rest <- fields[setdiff(names(fields), c("given", "family", "middle"))]
   other <- .rs.enumerate(rest, function(key, value)
   {
      sprintf("%s = %s", key, .rs.deparse(value))
   })
   
   body <- as.character(c(header, other))
   body <- sprintf("  %s", body)
   
   parts <- c(
      indent("person("),
      paste(indent(body), collapse = ",\n"),
      indent(")")
   )
   
   paste(parts, collapse = "\n")
})

.rs.addFunction("secureDownloadMethod", function()
{
   # Function to determine whether R checks for 404 in libcurl calls
   libcurlHandles404 <- function() {
      getRversion() >= "3.3" && .rs.haveRequiredRSvnRev(69197)
   }

   # Check whether we are running R 3.2+, R 4.2+, and whether we have libcurl
   isR32 <- getRversion() >= "3.2"
   isR42 <- getRversion() >= "4.2"
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
   
   # For windows, with R versions >= 3.2 to <= 4.1, we prefer binding directly to wininet
   # if we can (since that doesn't rely on the value of setInternet2).
   # In R 4.2+ wininet is deprecated, so we use libcurl instead when available.
   # If it's R <= 3.1 then we can use "internal" for https so long as internet2 is enabled
   if (identical(sysName, "Windows")) {
      if (isR42 && haveLibcurl)
         "libcurl"
      else if (isR32)
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

.rs.addFunction("appendSlashIfNeeded", function(url) {
   slash <- if (.rs.lastCharacterIs(url, "/")) "" else "/"
   paste(url, slash, sep = "")
})

.rs.addFunction("installPackagesRequiresRestart", function(pkgs)
{
   # if we're in an renv project, no need
   if ("renv" %in% loadedNamespaces())
   {
      project <- renv::project()
      if (!is.null(project))
         return(FALSE)
   }
   
   # if we're in a packrat project, no need
   mode <- Sys.getenv("R_PACKRAT_MODE", unset = NA)
   if (identical(mode, "1"))
      return(FALSE)
   
   # in other cases, restart if one of the requested packages is loaded
   .rs.loadedPackageUpdates(pkgs)
})
