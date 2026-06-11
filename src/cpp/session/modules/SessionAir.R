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
   destfile <- tempfile(fileext = ".json")
   download.file(url, destfile = destfile, quiet = TRUE)
   
   contents <- readLines(destfile, warn = FALSE)
   response <- .rs.fromJSON(contents)
   response[["tag_name"]]
})

.rs.addFunction("air.installedVersions", function()
{
   versions <- list.files(.rs.air.rootDir())
   versions <- versions[file.exists(.rs.air.exePath(versions))]
   if (length(versions) == 0L)
      return(character())

   # Tolerate a leading 'v' when parsing, in case Air switches to
   # v-prefixed release tags; the directory name is still returned as-is.
   parsed <- numeric_version(sub("^v", "", versions), strict = FALSE)
   versions <- versions[!is.na(parsed)]
   parsed <- parsed[!is.na(parsed)]

   versions[order(parsed, decreasing = TRUE)]
})

.rs.addFunction("air.ensureAvailable", function()
{
   # If air is on the PATH, use it
   exe <- Sys.which("air")
   if (nzchar(exe))
      return(normalizePath(exe))

   # Check for a user-requested version of air
   version <- getOption("rstudio.air.version")

   # Otherwise, prefer the newest already-installed copy of air, so that
   # we can avoid querying GitHub for the latest release version
   if (is.null(version))
   {
      installed <- .rs.air.installedVersions()
      if (length(installed))
         version <- installed[[1L]]
   }

   # Otherwise, install and use our own copy of air
   if (is.null(version))
      version <- .rs.air.defaultVersion()

   exe <- .rs.air.exePath(version)
   if (!file.exists(exe))
   {
      autoinstall <- getOption("rstudio.air.autoinstall", default = TRUE)
      if (autoinstall)
         .rs.air.installVersion(version)
   }

   if (!file.exists(exe))
      stop(sprintf("Air binary not found at '%s'.", exe))

   normalizePath(exe)
})

.rs.addFunction("air.rootDir", function()
{
   homeDir <- if (.rs.platform.isWindows)
   {
      Sys.getenv("USERPROFILE", unset = path.expand("~"))
   }
   else
   {
      Sys.getenv("HOME", unset = path.expand("~"))
   }

   chartr("\\", "/", file.path(homeDir, ".local", "lib", "air"))
})

.rs.addFunction("air.binDir", function(version)
{
   file.path(.rs.air.rootDir(), version, "bin")
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
      download.file(url, destfile = destfile, mode = "wb")
      unzip(destfile)
   }
   else if (.rs.platform.isMacos)
   {
      fmt <- "https://github.com/posit-dev/air/releases/download/%s/air-%s-apple-darwin.tar.gz"
      url <- sprintf(fmt, version, R.version$arch)
      destfile <- basename(url)
      download.file(url, destfile = destfile)
      untar(destfile)
   }
   else
   {
      fmt <- "https://github.com/posit-dev/air/releases/download/%s/air-%s-unknown-linux-gnu.tar.gz"
      url <- sprintf(fmt, version, R.version$arch)
      destfile <- basename(url)
      download.file(url, destfile = destfile)
      untar(destfile)
   }
   
   # Copy air to the resulting binary directory.
   exeName <- if (.rs.platform.isWindows) "air.exe" else "air"
   exePattern <- if (.rs.platform.isWindows) "^air\\.exe$" else "^air$"
   airPath <- list.files(pattern = exePattern, full.names = TRUE, recursive = TRUE)
   if (length(airPath) != 1L)
   {
      fmt <- "Air binary not found in archive (pattern '%s'); found %d matches."
      stop(sprintf(fmt, exePattern, length(airPath)))
   }

   destPath <- file.path(binDir, exeName)
   ok <- file.copy(airPath, destPath, overwrite = TRUE)
   if (!isTRUE(ok))
      stop(sprintf("Failed to copy Air binary to '%s'.", destPath))

   fmt <- "Air %s has been installed to %s."
   msg <- sprintf(fmt, version, binDir)
   message(msg)
})
