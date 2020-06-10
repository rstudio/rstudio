#
# ModuleTools.R
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

.rs.addFunction("enqueClientEvent", function(type, data = NULL)
{
   .Call("rs_enqueClientEvent", type, data, PACKAGE = "(embedding)")
})

.rs.addFunction("invokeRpc", function(method, ...) 
{
   # callback to session to invoke RPC
   args <- list(...)
   .Call("rs_invokeRpc", method, .rs.scalarListFromList(args), PACKAGE = "(embedding)")
})

.rs.addFunction("showErrorMessage", function(title, message)
{
   .Call("rs_showErrorMessage", title, message, PACKAGE = "(embedding)")
})

.rs.addFunction("logErrorMessage", function(message)
{
   .Call("rs_logErrorMessage", message, PACKAGE = "(embedding)")
})

.rs.addFunction("logWarningMessage", function(message)
{
   .Call("rs_logWarningMessage", message, PACKAGE = "(embedding)")
})

.rs.addFunction("format", function(object, ...)
{
   if (is.symbol(object))
      as.character(object)
   else
      base::format.default(object, ...)
})

.rs.addFunction("getSignature", function(object)
{
   signature <- .rs.format(base::args(object))
   length(signature) <- length(signature) - 1
   trimmed <- gsub("^\\s+", "", signature)
   paste(trimmed, collapse = "")
})

# Wrap a return value in this to give a hint to the
# JSON serializer that one-element vectors should be
# marshalled as scalar types instead of arrays
.rs.addFunction("scalar", function(obj)
{
   if (!is.null(obj))
      class(obj) <- 'rs.scalar'
   return(obj)
})

.rs.addFunction("validateAndNormalizeEncoding", function(encoding)
{
   iconvList <- toupper(iconvlist())
   encodingUpper <- toupper(encoding)
   if (encodingUpper %in% iconvList)
   {
      return (encodingUpper)
   }
   else
   {
      encodingUpper <- gsub("[_]", "-", encodingUpper)
      if (encodingUpper %in% iconvList)
         return (encodingUpper)
      else
         return ("")
   }
})

.rs.addFunction("usingUtf8Charset", function()
{
   l10n_info()$`UTF-8` || identical(utils::localeToCharset(), "UTF-8")
})


.rs.addFunction("isRtoolsOnPath", function()
{
   # ensure that the Rtools utility 'bin' directory is on the PATH
   # (this is just a heuristic but works okay in general)
   if (!nzchar(Sys.which("ls.exe")))
      return(FALSE)
   
   # for newer versions of R, ensure that BINPREF is set
   # (since BINPREF may be going away in R 4.0.0 we don't require its
   # existence here)
   rv <- getRversion()
   if (rv >= "3.3.0" && rv < "4.0.0")
   {
      if (is.na(Sys.getenv("BINPREF", unset = NA)))
         return(FALSE)
   }
   
   # for older versions of R (with the old toolchain) check for 'gcc.exe'
   # on the PATH (assuming the old, multilib-variant of gcc)
   if (rv < "3.3.0")
   {
      if (!nzchar(Sys.which("gcc.exe")))
         return(FALSE)
   }
   
   # survived all checks; return TRUE
   TRUE
})

.rs.addFunction("getPackageFunction", function(name, packageName)
{
   tryCatch(eval(parse(text=paste(packageName, ":::", name, sep=""))),
            error = function(e) NULL)
})

.rs.addFunction("libPathsString", function()
{
   paste(.libPaths(), collapse = .Platform$path.sep)
})

.rs.addFunction("parseLinkingTo", function(linkingTo)
{
   if (is.null(linkingTo))
      return (character())

   linkingTo <- strsplit(linkingTo, "\\s*\\,")[[1]]
   result <- gsub("\\s", "", linkingTo)
   gsub("\\(.*", "", result)
})


.rs.addFunction("isPackageInstalled", function(name, libLoc = NULL)
{
   paths <- vapply(name, FUN.VALUE = character(1), USE.NAMES = FALSE, function(pkg) {
      system.file(package = pkg, lib.loc = libLoc)
   })
   nzchar(paths)
})

.rs.addFunction("isPackageVersionInstalled", function(name, version) {  
  .rs.isPackageInstalled(name) && (.rs.getPackageVersion(name) >= version)
})

.rs.addFunction("packageCRANVersionAvailable", function(name, version, source) {
  # get the specified CRAN repo
  repo <- NA
  repos <- getOption("repos")
  # the repos option is canonically a named character vector, but could also
  # be a named list
  if (is.character(repos) || is.list(repos)) {
    # check for a repo named "CRAN"
    repo <- as.character(repos["CRAN"])

    # if no repo named "CRAN", blindly guess that the first repo is a CRAN mirror 
    if (length(repo) < 1 || is.na(repo)) {
      repo <- as.character(repos[[1]])
    }
  }

  # if no default repo and no repo marked CRAN, give up
  if (length(repo) < 1 || is.na(repo)) {
    return(list(version = "", satisfied = FALSE))
  }

  # get the available packages and extract the version information
  type <- ifelse(source, "source", getOption("pkgType"))
  pkgs <- available.packages(
            contriburl = contrib.url(repo, type = type))
  if (!(name %in% row.names(pkgs))) {
    return(list(version = "", satisfied = FALSE))
  }
  pkgVersion <- pkgs[name, "Version"]
  return(list(
    version = pkgVersion, 
    satisfied = package_version(pkgVersion) >= package_version(version)))
})

.rs.addFunction("packageVersionString", function(pkg) {
   as.character(packageVersion(pkg))
})

.rs.addFunction("getPackageCompatStatus", 
  function(name, packageVersion, protocolVersion) 
  {  
     if (!.rs.isPackageInstalled(name))
       return(1L)  # COMPAT_MISSING
     else if (!.rs.getPackageVersion(name) >= packageVersion) 
       return(2L)  # COMPAT_TOO_OLD
     else if (!.rs.getPackageRStudioProtocol(name) >= protocolVersion) 
       return(3L)  # COMPAT_TOO_NEW
     return (0L)   # COMPAT_OK
  }
)

.rs.addFunction("getPackageRStudioProtocol", function(name) {

   ## First check to see if the package has a 'rstudio-protocol' file
   path <- system.file("rstudio/rstudio-protocol", package = name)
   if (path != "") {
      tryCatch(
         expr = {
            return(as.integer(read.dcf(path, all = TRUE)$Version))
         },
         warning = function(e) {},
         error = function(e) {}
      )
   }

   ## Otherwise, check the namespace
   needsUnloadAfter <- !(name %in% loadedNamespaces())
   rpv <- ".RStudio_protocol_version"
   env <- asNamespace(name)
   if (exists(rpv, envir = env, mode = "integer")) {
      version <- get(rpv, envir = env)
   } else {
      version <- 0L
   }
   if (needsUnloadAfter)
      unloadNamespace(name)
   version
})

.rs.addFunction("rstudioIDEPackageRequiresUpdate", function(name, sha1) {
   
  if (.rs.isPackageInstalled(name))
  {
     f <- utils::packageDescription(name, fields=c("Origin", "GithubSHA1"))
     identical(f$Origin, "RStudioIDE") && !identical(f$GithubSHA1, sha1)
  }
  else
  {
     TRUE
  }
})

.rs.addFunction("updateRStudioIDEPackage", function(name, archive)
{
  pkgDir <- find.package(name)
  .rs.forceUnloadPackage(name)
  .Call("rs_installPackage",  archive, dirname(pkgDir), PACKAGE = "(embedding)")
})


.rs.addFunction("userPrompt", function(type,
                                       caption,
                                       message,
                                       yesLabel = NULL,
                                       noLabel = NULL,
                                       includeCancel = FALSE,
                                       yesIsDefault = TRUE) {

   if (identical(type, "info"))
      type <- 1
   else if (identical(type, "warning"))
      type <- 2
   else if (identical(type, "error"))
      type <- 3
   else if (identical(type, "question"))
      type <- 4
   else
      stop("Invalid type specified")

   result <- .Call("rs_userPrompt",
         type,
         caption,
         message,
         yesLabel,
         noLabel,
         includeCancel,
         yesIsDefault, PACKAGE = "(embedding)")

   if (result == 0)
      "yes"
   else if (result == 1)
      "no"
   else if (result == 2)
      "cancel"
   else
      stop("Invalid result")
})

.rs.addFunction("restartR", function(afterRestartCommand = "") {
   afterRestartCommand <- paste(as.character(afterRestartCommand),
                                collapse = "\n")
   .Call("rs_restartR", afterRestartCommand, PACKAGE = "(embedding)")
})

.rs.addFunction("markdownToHTML", function(content) {
   .Call("rs_markdownToHTML", content, PACKAGE = "(embedding)")
})

.rs.addFunction("readPrefInternal", function(method, prefName) {
  if (missing(prefName) || is.null(prefName))
    stop("No preference name supplied")
  .Call(method, prefName, PACKAGE = "(embedding)")
})

.rs.addFunction("writePrefInternal", function(method, prefName, value) {
  if (missing(prefName) || is.null(prefName))
    stop("No preference name supplied")
  if (missing(value))
    stop("No value supplied")
  invisible(.Call(method, prefName, .rs.scalar(value), PACKAGE = "(embedding)"))
})

.rs.addFunction("readApiPref", function(prefName) {
  .rs.readPrefInternal("rs_readApiPref", prefName)
})

.rs.addFunction("writeApiPref", function(prefName, value) {
  .rs.writePrefInternal("rs_writeApiPref", prefName, value)
})

.rs.addFunction("readUiPref", function(prefName) {
  .rs.readPrefInternal("rs_readUserPref", prefName)
})
.rs.addFunction("readUserPref", .rs.readUiPref)

.rs.addFunction("writeUiPref", function(prefName, value) {
  .rs.writePrefInternal("rs_writeUserPref", prefName, value)
})
.rs.addFunction("writeUserPref", .rs.writeUiPref)

.rs.addFunction("readUserState", function(stateName) {
  if (missing(stateName) || is.null(stateName))
    stop("No state name supplied")
  .Call("rs_readUserState", stateName, PACKAGE = "(embedding)")
})

.rs.addFunction("allPrefs", function() {
  .Call("rs_allPrefs", PACKAGE = "(embedding)")
})

.rs.addFunction("writeUserState", function(stateName, value) {
  if (missing(stateName) || is.null(stateName))
    stop("No state name supplied")
  if (missing(value))
    stop("No value supplied")
  invisible(.Call("rs_writeUserState", stateName, .rs.scalar(value), PACKAGE = "(embedding)"))
})

.rs.addFunction("removePref", function(prefName) {
  if (missing(prefName) || is.null(prefName))
    stop("No preference name supplied")
  invisible(.Call("rs_removePref", prefName, PACKAGE = "(embedding)"))
})

.rs.addFunction("setUsingMingwGcc49", function(usingMingwGcc49) {
  invisible(.Call("rs_setUsingMingwGcc49", usingMingwGcc49, PACKAGE = "(embedding)"))
})


.rs.addGlobalFunction("rstudioDiagnosticsReport", function() {
  invisible(.Call(getNativeSymbolInfo("rs_sourceDiagnostics", PACKAGE="")))
})


.rs.addFunction("pandocSelfContainedHtml", function(input, template, output) {
   
   # make input file path absolute
   input <- normalizePath(input)

   # create a temporary copy of the file for conversion
   inputFile <- tempfile(tmpdir = dirname(input), fileext = ".html")
   inputLines <- readLines(con = input, warn = FALSE)

   # write all the lines from the input except the DOCTYPE declaration, which pandoc will not treat
   # as HTML (starting in pandoc 2)
   writeLines(text = inputLines[!grepl("<!DOCTYPE", inputLines, fixed = TRUE)],
              con  = inputFile)

   # ensure output file exists and make its path absolute
   if (!file.exists(output))
      file.create(output)
   output <- normalizePath(output)
   
   # convert from markdown to html to get base64 encoding. note there is no markdown in the source
   # document but we still need to do this "conversion" to get the base64 encoding; we also don't
   # want to convert from HTML since that will cause pandoc to convert only the <body>
   args <- c(inputFile)
   args <- c(args, "--from", "markdown_strict")
   args <- c(args, "--output", output)

   # define a title for the document. this value is not actually consumed by the template, but
   # pandoc requires it in metadata when converting to HTML, so supply a dummy value to keep the
   # output clean.
   args <- c(args, "--metadata", "title:RStudio")
   
   # set stack size
   stack_size <- getOption("pandoc.stack.size", default = "512m")
   args <- c(c("+RTS", paste0("-K", stack_size), "-RTS"), args)
   
   # additional options
   args <- c(args, "--self-contained")
   args <- c(args, "--template", template)
   
   # build the conversion command
   pandoc <- file.path(Sys.getenv("RSTUDIO_PANDOC"), "pandoc")
   command <- paste(shQuote(c(pandoc, args)), collapse = " ")
   
   # setwd temporarily
   wd <- getwd()
   on.exit(setwd(wd), add = TRUE)
   setwd(dirname(inputFile))
   
   # execute it
   result <- system(command)
   if (result != 0) {
      stop("pandoc document conversion failed with error ", result,
           call. = FALSE)
   }
   
   # return output file
   invisible(output)
})

# create an environment to hold original versions of S3 functions we have overridden
assign(".rs.S3Originals", new.env(parent = emptyenv()), envir = .rs.toolsEnv())

# create an environment to hold current S3 overrides
assign(".rs.S3Overrides", new.env(parent = emptyenv()), envir = .rs.toolsEnv())

if (getRversion() < "3.5") {
   # prior to R 3.5, we can override S3 methods just by installing the override function into the
   # tools:rstudio namespace, which is on the search path
   .rs.addFunction("addS3Override", function(name, method) {
      assign(name, method, envir = .rs.toolsEnv())
   })

   .rs.addFunction("removeS3Override", function(name) {
      if (exists(name, envir = .rs.toolsEnv(), inherits = FALSE)) {
         rm(list = name, envir = .rs.toolsEnv(), inherits = FALSE)
      }
   })

   .rs.addFunction("reattachS3Overrides", function() {
      # S3 methods on the search path maintain precedence
   })
} else {
   # after R 3.5, S3 methods are not discovered on the search path, so we resort to injecting our
   # overrides directly into the base namespace's S3 methods table
   .rs.addFunction("addS3Override", function(name, method) {
      # get a reference to the table of S3 methods stored in the base namespace
      table <- .BaseNamespaceEnv[[".__S3MethodsTable__."]]

      # cache old dispatch table entry if it exists
      if (exists(name, envir = table)) {
         assign(name, get(name, envir = table), envir = .rs.S3Originals)
      }

      # add a flag indicating that this method belongs to us
      attr(method, ".rs.S3Override") <- TRUE

      # ... and inject our own entry
      assign(name, method, envir = table)

      # make a copy in our override table so we can restore when overwritten by e.g. an attached
      # package
      assign(name, method, envir = .rs.S3Overrides)

      invisible(NULL)
   })

   .rs.addFunction("removeS3Override", function(name) {
      table <- .BaseNamespaceEnv[[".__S3MethodsTable__."]]

      # see if there's an override to remove; if not, no work to do
      if (!exists(name, envir = table))
         return(invisible(NULL))
      
      # see if the copy that exists in the methods table is one that we put there.
      if (!isTRUE(attr(get(name, envir = table), ".rs.S3Override", exact = TRUE)))
      {
         # it isn't, so don't touch it. we do this so that changes to the S3 dispatch table that
         # have occurred since the call to .rs.addS3Override are persisted
         return(invisible(NULL))
      }

      # see if we have a copy to restore
      if (exists(name, envir = .rs.S3Originals))
      {
         # we do, so overwrite with our copy
         assign(name, get(name, envir = .rs.S3Originals), envir = table)
      }
      else
      {
         # no copy to restore, so just remove from the dispatch table
         rm(list = name, envir = table)
      }

      # remove from our override table if present
      if (exists(name, envir = .rs.S3Overrides))
         rm(list = name, envir = .rs.S3Overrides)

      invisible(NULL)
   })

   # recovers from changes made to the S3 method dispatch table during e.g. package load
   .rs.addFunction("reattachS3Overrides", function() {
      # get a list of all of the methods that are currently overridden
      names <- ls(envir = .rs.S3Overrides)
      table <- .BaseNamespaceEnv[[".__S3MethodsTable__."]]
      for (name in names) {
         if (exists(name, envir = table)) {
            # retrieve reference to method
            method = get(name, envir = table)

            # if we didn't put the method there, we've been replaced; reattach our own method. 
            if (!isTRUE(attr(get(name, envir = table), ".rs.S3Override", exact = TRUE)))
               .rs.addS3Override(name, get(name, envir = .rs.S3Overrides))
         }
      }
   })
}

.rs.addFunction("sessionModulePath", function() {
   .Call("rs_sessionModulePath", PACKAGE = "(embedding)")
})

