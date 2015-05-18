#
# ModuleTools.R
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

.rs.addFunction("enqueClientEvent", function(type, data = NULL)
{
   .Call(.rs.routines$rs_enqueClientEvent, type, data)
})

.rs.addFunction("showErrorMessage", function(title, message)
{
   .Call(.rs.routines$rs_showErrorMessage, title, message)
})

.rs.addFunction("logErrorMessage", function(message)
{
   .Call(.rs.routines$rs_logErrorMessage, message)
})

.rs.addFunction("logWarningMessage", function(message)
{
   .Call(.rs.routines$rs_logWarningMessage, message)
})

.rs.addFunction("getSignature", function(obj)
{
   sig = capture.output(print(args(obj)))
   sig = sig[1:length(sig)-1]
   sig = gsub('^\\s+', '', sig)
   paste(sig, collapse='')
})

# Wrap a return value in this to give a hint to the
# JSON serializer that one-element vectors should be
# marshalled as scalar types instead of arrays
.rs.addFunction("scalar", function(obj)
{
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
   return (nzchar(Sys.which("ls.exe")) && nzchar(Sys.which("gcc.exe")))
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
  name %in% .packages(all.available = TRUE, lib.loc = libLoc)
})

.rs.addFunction("isPackageVersionInstalled", function(name, version) {  
  .rs.isPackageInstalled(name) && (.rs.getPackageVersion(name) >= version)
})

.rs.addFunction("packageCRANVersionAvailable", function(name, version, source) {
  # get the specified CRAN repo
  repo <- NA
  repos <- getOption("repos")
  if (is.character(repos)) {
    if (is.null(names(repos))) {
      # no indication of which repo is which, presume the first entry to be
      # CRAN
      if (length(repos) > 0)
        repo <- repos[[1]]
    } else {
      # use repo named CRAN
      repo <- as.character(repos["CRAN"])
    }
  }

  # if no default repo and no repo marked CRAN, give up
  if (is.na(repo)) {
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
  .Call(.rs.routines$rs_installPackage,  archive, dirname(pkgDir))
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
         yesIsDefault)

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
   .Call(.rs.routines$rs_restartR, afterRestartCommand)
})




