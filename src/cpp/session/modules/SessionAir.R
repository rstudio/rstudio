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

.rs.addFunction("air.downloadFile", function(url, destfile)
{
   # Prefer the curl binary when available -- unlike download.file(), it can
   # time out on inactivity rather than on total download time, so a slow but
   # active download is not killed prematurely.
   # https://github.com/rstudio/rstudio/issues/17746
   curl <- Sys.which("curl")
   if (nzchar(curl))
      return(.rs.air.downloadFileWithCurl(curl, url, destfile))

   # Otherwise, fall back to download.file(). The 'timeout' option caps the
   # total download time for a single file, and the 60-second default has
   # proven too short for slow connections, so bump it for this download.
   timeout <- getOption("timeout", default = 60L)
   options(timeout = max(300L, timeout))
   on.exit(options(timeout = timeout), add = TRUE)

   download.file(url, destfile = destfile, quiet = TRUE, mode = "wb")

   invisible(destfile)
})

.rs.addFunction("air.downloadFileWithCurl", function(curl, url, destfile)
{
   args <- c(
      "--fail", "--location", "--silent", "--show-error",
      "--connect-timeout", "60",
      "--speed-limit", "1",
      "--speed-time", "60",
      "--output", shQuote(destfile),
      shQuote(url)
   )

   output <- suppressWarnings(system2(curl, args, stdout = TRUE, stderr = TRUE))

   status <- attr(output, "status")
   if (!is.null(status) && status != 0L)
   {
      fmt <- "Error downloading '%s' [exit code %d]:\n%s"
      stop(sprintf(fmt, url, status, paste(output, collapse = "\n")))
   }

   invisible(destfile)
})

.rs.addFunction("air.defaultVersion", function()
{
   version <- getOption("rstudio.air.version")
   if (!is.null(version))
      return(version)

   url <- "https://api.github.com/repos/posit-dev/air/releases/latest"
   destfile <- tempfile(fileext = ".json")
   .rs.air.downloadFile(url, destfile)

   # .rs.fromJSON() takes a single string; readLines() on the pretty-printed
   # response yields one element per line, so collapse before parsing.
   contents <- paste(readLines(destfile, warn = FALSE), collapse = "\n")
   response <- .rs.fromJSON(contents)

   version <- response[["tag_name"]]
   if (is.null(version))
      stop(sprintf("Failed to determine latest Air version from '%s'.", url))

   version
})

.rs.addFunction("air.installedVersions", function()
{
   # Search every known root (the current data directory plus any legacy
   # locations) so that copies of air installed by older versions of
   # RStudio are still discovered. https://github.com/rstudio/rstudio/issues/18068
   versions <- character()
   for (root in .rs.air.rootDirs())
   {
      candidates <- list.files(root)
      candidates <- candidates[file.exists(.rs.air.exePath(candidates, root))]
      versions <- c(versions, candidates)
   }

   versions <- unique(versions)
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

   # Use an existing copy if we can find one (searching legacy roots too);
   # otherwise install into the current data directory.
   exe <- .rs.air.findExe(version)
   if (is.null(exe))
   {
      autoinstall <- getOption("rstudio.air.autoinstall", default = TRUE)
      if (autoinstall)
         .rs.air.installVersion(version)

      exe <- .rs.air.findExe(version)
   }

   if (is.null(exe))
      stop(sprintf("Air binary not found at '%s'.", .rs.air.exePath(version)))

   normalizePath(exe)
})

.rs.addFunction("air.rootDir", function()
{
   # Install air underneath RStudio's per-user data directory, e.g.
   # '~/.local/share/rstudio/air' on Linux, '~/Library/.../rstudio/air' on
   # macOS, or '%LOCALAPPDATA%\rstudio\air' on Windows. The C++ helper handles
   # the platform-specific resolution (and any environment overrides).
   # https://github.com/rstudio/rstudio/issues/18068
   dataDir <- .Call("rs_userDataDir", PACKAGE = "(embedding)")
   chartr("\\", "/", file.path(dataDir, "air"))
})

.rs.addFunction("air.oldRootDir", function()
{
   # The location used by older versions of RStudio. We still look here when
   # discovering already-installed copies of air, but never install here.
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

.rs.addFunction("air.rootDirs", function()
{
   # The current root is searched first, so installations there take
   # precedence over copies left behind in a legacy location.
   unique(c(.rs.air.rootDir(), .rs.air.oldRootDir()))
})

.rs.addFunction("air.binDir", function(version, root = .rs.air.rootDir())
{
   file.path(root, version, "bin")
})

.rs.addFunction("air.exePath", function(version, root = .rs.air.rootDir())
{
   exe <- if (.rs.platform.isWindows) "air.exe" else "air"
   file.path(.rs.air.binDir(version, root), exe)
})

.rs.addFunction("air.findExe", function(version)
{
   # Return the path to an installed copy of air for the requested version,
   # preferring the current root but falling back to legacy locations.
   # Returns NULL when no installed copy exists.
   for (root in .rs.air.rootDirs())
   {
      exe <- .rs.air.exePath(version, root)
      if (file.exists(exe))
         return(exe)
   }

   NULL
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
      .rs.air.downloadFile(url, destfile)
      unzip(destfile)
   }
   else if (.rs.platform.isMacos)
   {
      fmt <- "https://github.com/posit-dev/air/releases/download/%s/air-%s-apple-darwin.tar.gz"
      url <- sprintf(fmt, version, R.version$arch)
      destfile <- basename(url)
      .rs.air.downloadFile(url, destfile)
      untar(destfile)
   }
   else
   {
      fmt <- "https://github.com/posit-dev/air/releases/download/%s/air-%s-unknown-linux-gnu.tar.gz"
      url <- sprintf(fmt, version, R.version$arch)
      destfile <- basename(url)
      .rs.air.downloadFile(url, destfile)
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
