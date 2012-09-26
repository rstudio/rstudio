#
# Diagnostics.R
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

# capture output into a file
require(utils)
dir.create("~/rstudio-diagnostics", showWarnings=FALSE)
diagnosticsFile <- normalizePath(paste("~/rstudio-diagnostics/diagnostics-report.txt"),
                                 mustWork = FALSE)

capture.output({

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
  print(as.list(Sys.getenv()))
  print(search())
  
  cat("\nCapabilities:\n\n")
  print(as.list(capabilities(c("png"))))
  cat("\n")

  # locate diagonstics binary and run it
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
  
  if (file.exists(cppDiag))
    diag <- system(cppDiag, intern=TRUE)
  cat(diag, sep="\n")
  
  
}, file=diagnosticsFile)

cat("Diagnostics written to:", diagnosticsFile, "\n")


