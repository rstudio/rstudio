#
# Diagnostics.R
#
# Copyright (C) 2009-19 by RStudio, Inc.
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

# capture output into a file (note this path is in module_context::sourceDiagnostics
# so changes to the path should be synchronized there)
library(utils)
dir.create("~/rstudio-diagnostics", showWarnings=FALSE)
diagnosticsFile <- suppressWarnings(normalizePath("~/rstudio-diagnostics/diagnostics-report.txt"))

capture.output({

  cat("RStudio Diagnostics Report\n",
      "-----------------------------------------------------------------------------\n",
      "Generated ", date(), "\n\n",
      "WARNING: This report may contain sensitive security information and/or\n",
      "personally identifiable information. Please audit the below and redact any\n",
      "sensitive information before submitting your diagnostics report, then remove\n",
      "this notice.\n\n", sep = "")

  # version
  versionFile <- "../VERSION"
  if (file.exists(versionFile)) {
    print(readLines(versionFile))
    cat("\n")
  }
  
  # basic info
  print(as.list(Sys.which(c("R", 
                            "pdflatex",
                            "bibtex",
                            "gcc",
                            "git", 
                            "svn"))))
  print(sessionInfo())
  cat("\nSysInfo:\n")
  print(Sys.info())
  cat("\nR Version:\n")
  print(version)

  envVars <- Sys.getenv()
  
  # create a list of words that are likely to appear in environment variables
  # that we shouldn't capture in a diagnostics report
  redactWords <- c(
     "API",
     "AUTH",
     "GITHUB",
     "HOST",
     "HOST",
     "KEY",
     "LOGNAME",
     "PASSWORD",
     "PAT",
     "SECRET",
     "TOKEN",
     "USERNAME"
  )
  
  # form each into a regex that matches the word exactly
  redactRegexes <- vapply(redactWords, function(word) { 
     paste0("\\b(?:", word, ")\\b")
  }, "")
  
  # collapse all of the regexes into a mega-regex that matches any banned word,
  # then match it on the list of environment variable names with _ converted to
  # a space (so that e.g. GITHUB_PAT becomes GITHUB PAT and matches the banned
  # word PAT)
  matches <- grepl(paste0(redactRegexes, collapse = "|"), 
                   gsub("_", " ", names(envVars), fixed = TRUE), 
                   ignore.case = TRUE)
  envVars[matches] <- "*** redacted ***"

  print(as.list(envVars))
  print(search())
  
  # locate diagnostics binary and run it
  sysName <- Sys.info()[['sysname']]
  ext <- ifelse(identical(sysName, "Windows"), ".exe", "")
  
  # first look for debug version
  cppDiag <- paste("../../../qtcreator-build/diagnostics/diagnostics",
                ext, sep="")
  if (!file.exists(cppDiag)) {
    if (identical(sysName, "Darwin"))
      cppDiag <- "../../MacOS/diagnostics"
    else
      cppDiag <- paste("../bin/diagnostics", ext, sep="")
  }
  
  if (file.exists(cppDiag)) {
    diag <- system(cppDiag, intern=TRUE)
    cat(diag, sep="\n")
  }
  
  
}, file=diagnosticsFile)

cat("Diagnostics report written to:", diagnosticsFile, "\n\n",
    "Please audit the report and remove any sensitive information before submitting.\n", sep = "")


