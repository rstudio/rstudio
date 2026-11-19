#
# Diagnostics.R
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

# Init ----

library(utils)
diagnosticsDefault <- path.expand("~/rstudio-diagnostics/diagnostics-report.txt")
diagnosticsFile <- Sys.getenv("RSTUDIO_DIAGNOSTICS_REPORT", unset = diagnosticsDefault)
dir.create(dirname(diagnosticsFile), recursive = TRUE, showWarnings = FALSE)

# Utility functions ----

redact_regex <- function() {
  
  # matched as whole words in names with '_' read as a space, so that e.g.
  # GITHUB_PAT matches PAT but PATH does not
  words <- c(
    "API", "AUTH", "AUTHORIZATION", "CONNECTION STRING", "COOKIE", "DSN",
    "GITHUB", "HOST", "KEY", "LOGNAME", "PASS", "PAT", "PRIVATE", "PWD",
    "R TRAVIS (COM|ORG)", "UID", "USERNAME", "WEBHOOK"
  )
  
  # matched anywhere, so that e.g. PGPASSWORD and APIKEY match too
  fragments <- c(
    "ACCESSKEY", "APIKEY", "CREDENTIAL", "PASSPHRASE", "PASSWD", "PASSWORD",
    "SECRET", "TOKEN"
  )
  
  patterns <- c(sprintf("\\b%s\\b", words), fragments)
  sprintf("(?:%s)", paste(patterns, collapse = "|"))
  
}

# redacts the user name and password in URLs, e.g. in a proxy setting. they
# run through the last '@' before the path, since user names and passwords
# can hold an unescaped '@'; proxy settings can leave out the scheme, as in
# user:pass@proxy:3128
redact_url_credentials <- function(x) {

  x <- gsub(
    "://[^/?#[:space:]]*@",
    "://*** redacted ***@",
    x,
    perl = TRUE
  )

  gsub(
    "(?<![^[:space:]=\"'])[^[:space:]/?#:@=\"']+:[^[:space:]/?#\"']*@",
    "*** redacted ***@",
    x,
    perl = TRUE
  )

}

header <- function(fmt, ...) {
  label <- sprintf(fmt, ...)
  separator <- paste(rep.int("-", 50L), collapse = "")
  writeLines(c(label, separator))
}

newlines <- function(n = 2L) {
  writeLines(character(n))
}

dump <- function(object) {
  formatted <- if (!is.null(names(object))) {
    paste(format(names(object)), format(object), sep = " : ")
  } else {
    format(object)
  }
  writeLines(formatted)
}

dumpfile <- function(path) {
  
  if (!file.exists(path)) {
    writeLines("(File does not exist)")
    return(invisible())
  }
  
  contents <- readLines(path, warn = FALSE)
  if (length(contents) == 0L) {
    writeLines("(File exists but is empty)")
    return(invisible())
  }
  
  if (all(grepl("^\\s*$", contents, perl = TRUE))) {
    writeLines("(File exists but is empty / blank)")
    return(invisible())
  }
  
  # redact what is assigned to a sensitive name, in forms such as TOKEN=...,
  # GITHUB_PAT_READONLY=..., token <- ..., or Sys.setenv("TOKEN" = ...). a
  # line can assign several values, as in Sys.setenv(A = ..., TOKEN = ...),
  # so redact from its first assignment. this works line by line: a value
  # that starts or continues on a later line is not redacted
  regex <- sprintf("%s[[:alnum:]. ]*[\"'`]?\\s*(?:=|<-)", redact_regex())
  keys <- gsub("_", " ", contents, fixed = TRUE)
  matches <- grepl(regex, keys, ignore.case = TRUE, perl = TRUE)
  for (i in which(matches)) {
    first <- regexpr("=|<-", contents[[i]], perl = TRUE)
    end <- first + attr(first, "match.length") - 1L
    contents[[i]] <- paste(substring(contents[[i]], 1L, end), "*** redacted ***")
  }
  
  writeLines(redact_url_credentials(contents))
  
}

envvars <- function() {
  
  # log environment variables, redacting the values of those whose names
  # suggest they hold secrets (e.g. API tokens)
  vars <- Sys.getenv()
  keys <- gsub("_", " ", names(vars), fixed = TRUE)
  matches <- grepl(redact_regex(), keys, ignore.case = TRUE, perl = TRUE)
  vars[matches] <- "*** redacted ***"
  as.list(redact_url_credentials(vars))
}

# Main script ----

capture.output(file = diagnosticsFile, {

  report <- "
RStudio Diagnostics Report
==========================

WARNING: This report may contain sensitive security information and / or
personally identifiable information. Please audit the below and redact any
sensitive information before submitting your diagnostics report.
"

  writeLines(report)
  writeLines(paste("Generated:", date()))
  writeLines("")
  
  # version
  versionFile <- "../VERSION"
  if (file.exists(versionFile)) {
    header("RStudio Version")
    dumpfile(versionFile)
    newlines()
  }
  
  header("Session Information")
  print(sessionInfo())
  newlines()
  
  header("System Information")
  dump(Sys.info())
  newlines()
  
  header("Platform Information")
  dump(.Platform)
  newlines()
  
  header("Environment Variables")
  dump(envvars())
  newlines()
  
  header("R Version")
  dump(R.Version())
  newlines()
  
  header("R Home")
  dump(R.home())
  newlines()
  
  header("R Search Path")
  dump(search())
  newlines()
  
  header("R Library Paths")
  dump(.libPaths())
  newlines()
  
  header("Loaded Packages")
  packages <- loadedNamespaces()
  paths <- find.package(packages)
  names(paths) <- packages
  dump(sort(paths))
  newlines()
  
  local({
    
    op <- options()
    on.exit(options(op), add = TRUE)
    options(width = 1000, max.print = 50000)
    
    header("Installed Packages")
    ip <- as.data.frame(installed.packages(noCache = TRUE), stringsAsFactors = FALSE)
    rownames(ip) <- NULL
    print(ip[c("Package", "LibPath", "Version")])
    newlines()
  
  })
  
  # dump .Rprofiles
  r_base_profile <- file.path(R.home(), "library/base/R/Rprofile")
  header("R System Profile: %s", r_base_profile)
  dumpfile(r_base_profile)
  newlines()
  
  r_site_profile <- Sys.getenv("R_PROFILE", unset = file.path(R.home("etc"), "Rprofile.site"))
  header("R Site Profile: %s", r_site_profile)
  dumpfile(r_site_profile)
  newlines()
  
  r_user_profile <- Sys.getenv("R_PROFILE_USER", unset = path.expand("~/.Rprofile"))
  header("R User Profile: %s", r_user_profile)
  dumpfile(r_user_profile)
  newlines()
  
  # dump .Renvirons
  r_site_environ <- Sys.getenv("R_ENVIRON", unset = file.path(R.home("etc"), "Renviron.site"))
  header("R Site Environ: %s", r_site_environ)
  dumpfile(r_site_environ)
  newlines()
  
  r_user_environ <- Sys.getenv("R_ENVIRON_USER", unset = path.expand("~/.Renviron"))
  header("R User Environ: %s", r_user_environ)
  dumpfile(r_user_environ)
  newlines()
  
  header("R Temporary Directory")
  dump(tempdir())
  newlines()
  
  header("Files in R Temporary Directory")
  dump(list.files(tempdir()))
  newlines()
  
  # locate diagnostics binary and run it
  binaryPath <- local({
    
    # detect dev configurations
    postback <- Sys.getenv("RS_RPOSTBACK_PATH", unset = NA)
    if (!is.na(postback)) {
      devpath <- file.path(dirname(postback), "../../diagnostics/diagnostics")
      if (file.exists(devpath))
        return(devpath)
    }
    
    # detect release configurations
    sysname <- Sys.info()[["sysname"]]
    if (identical(sysname, "Darwin"))
      "../bin/diagnostics"
    else if (identical(sysname, "Windows"))
      "../bin/diagnostics.exe"
    else
      "../bin/diagnostics"
    
  })
    
  if (file.exists(binaryPath)) {
    binaryPath <- normalizePath(binaryPath)
    # the output includes rstudio-prefs.json, where e.g. a CRAN mirror URL
    # can hold credentials
    diagnostics <- system2(binaryPath, stdout = TRUE, stderr = TRUE)
    writeLines(redact_url_credentials(diagnostics))
  }
  
})

footer <- c(
  paste("Diagnostics report written to:", diagnosticsFile),
  "Please audit the report and remove any sensitive information before sharing this report with RStudio."
)

writeLines(footer)
