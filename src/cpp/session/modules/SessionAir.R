#
# SessionAir.R
#
# Copyright (C) 2025 by Posit Software, PBC
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

.rs.addFunction("air.defaultVersion", function()
{
   version <- getOption("rstudio.air.version")
   if (!is.null(version))
      return(version)
   
   url <- "https://api.github.com/repos/posit-dev/air/releases/latest"
   destfile <- tempfile(fileext = ".html")
   download.file(url, destfile = destfile, quiet = TRUE)
   
   contents <- readLines(destfile, warn = FALSE)
   response <- .rs.fromJSON(contents)
   response[["tag_name"]]
})

.rs.addFunction("air.ensureAvailable", function()
{
   # If air is on the PATH, use it
   exe <- Sys.which("air")
   if (nzchar(exe))
      return(normalizePath(exe))
   
   # Otherwise, install and use our own copy of air
   version <- .rs.air.defaultVersion()
   exe <- .rs.air.exePath(version)
   if (!file.exists(exe))
   {
      autoinstall <- getOption("rstudio.air.autoinstall", default = TRUE)
      if (autoinstall)
         .rs.air.installVersion(version)
   }
   
   normalizePath(exe)
})

.rs.addFunction("air.binDir", function(version)
{
   homeDir <- if (.rs.platform.isWindows)
   {
      Sys.getenv("USERPROFILE", unset = path.expand("~"))
   }
   else
   {
      Sys.getenv("HOME", unset = path.expand("~"))
   }

   binDir <- sprintf("%s/.local/lib/air/%s/bin", homeDir, version)
   chartr("\\", "/", binDir)
})

.rs.addFunction("air.exePath", function(version)
{
   exe <- if (.rs.platform.isWindows) "air.exe" else "air"
   file.path(.rs.air.binDir(version), exe)
})

.rs.addFunction("air.installVersion", function(version)
{
   # Work in temporary directory
   owd <- setwd(tempdir())
   on.exit(setwd(owd))
   
   # Set up installation directory
   binDir <- .rs.air.binDir(version)
   dir.create(binDir, recursive = TRUE, showWarnings = FALSE)
   
   # Download air binaries
   if (.rs.platform.isWindows)
   {
      fmt <- "https://github.com/posit-dev/air/releases/download/%s/air-%s-pc-windows-msvc.zip"
      url <- sprintf(fmt, version, R.version$arch)
      destfile <- basename(url)
      download.file(url, destfile = destfile)
      unzip(destfile, exdir = binDir)
   }
   else if (.rs.platform.isMacos)
   {
      fmt <- "https://github.com/posit-dev/air/releases/download/%s/air-%s-apple-darwin.tar.gz"
      url <- sprintf(fmt, version, R.version$arch)
      destfile <- basename(url)
      download.file(url, destfile = destfile)
      untar(destfile, exdir = binDir)
   }
   else
   {
      fmt <- "https://github.com/posit-dev/air/releases/download/%s/air-%s-unknown-linux-gnu.tar.gz"
      url <- sprintf(fmt, version, R.version$arch)
      destfile <- basename(url)
      download.file(url, destfile = destfile)
      untar(destfile, exdir = binDir)
   }
   
   fmt <- "Air %s has been installed to %s."
   msg <- sprintf(fmt, version, binDir)
   message(msg)
})
