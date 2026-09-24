#
# SessionRig.R
#
# Copyright (C) 2026 by Posit Software, PBC
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

# Support for rig (https://github.com/r-lib/rig), the R installation manager
# RStudio uses to discover and install versions of R.

# The rig release RStudio downloads when no rig installation can be found.
.rs.setVar("rig.pinnedVersion", "0.10.0")

.rs.addFunction("rig.exeName", function()
{
   if (.rs.platform.isWindows) "rig.exe" else "rig"
})

#' The directory RStudio installs its own copy of rig into.
.rs.addFunction("rig.userInstallRoot", function()
{
   file.path(.Call("rs_userDataDir", PACKAGE = "(embedding)"), "rig")
})

#' The locations searched for a rig binary, in order of preference: an
#' explicit override, the PATH, the platform's standard install locations,
#' and finally the copy RStudio downloaded itself.
.rs.addFunction("rig.candidatePaths", function()
{
   exe <- .rs.rig.exeName()

   paths <- c(
      Sys.getenv("RSTUDIO_RIG", unset = ""),
      unname(Sys.which("rig"))
   )

   if (.rs.platform.isWindows)
   {
      paths <- c(
         paths,
         file.path(Sys.getenv("ProgramFiles", unset = "C:/Program Files"), "rig", "bin", exe),
         file.path(Sys.getenv("LOCALAPPDATA", unset = ""), "Programs", "rig", "bin", exe)
      )
   }
   else
   {
      paths <- c(
         paths,
         "/usr/local/bin/rig",
         "/opt/homebrew/bin/rig",
         "/usr/bin/rig",
         path.expand("~/.local/bin/rig")
      )
   }

   paths <- c(paths, file.path(.rs.rig.userInstallRoot(), "bin", exe))
   paths[nzchar(paths)]
})

#' Locate a rig binary, or return "" if none is available.
.rs.addFunction("rig.find", function()
{
   for (path in .rs.rig.candidatePaths())
      if (file.exists(path))
         return(normalizePath(path, winslash = "/"))

   ""
})

#' The URL of the rig release archive for this platform.
.rs.addFunction("rig.downloadUrl", function(version = .rs.rig.pinnedVersion,
                                            sysname = Sys.info()[["sysname"]],
                                            machine = Sys.info()[["machine"]],
                                            processorArch = Sys.getenv("PROCESSOR_ARCHITECTURE"))
{
   isArm <- tolower(machine) %in% c("arm64", "aarch64")

   file <- if (identical(sysname, "Darwin"))
   {
      sprintf("rig-macos-%s-%s.tar.gz", if (isArm) "arm64" else "x86_64", version)
   }
   else if (identical(sysname, "Windows"))
   {
      # on Windows, Sys.info() reports the architecture of R rather than
      # of the machine, which matters when running x86_64 R on ARM
      isArm <- identical(toupper(processorArch), "ARM64")
      sprintf("rig-windows-%s-%s.zip", if (isArm) "arm64" else "x86_64", version)
   }
   else
   {
      sprintf("rig-linux-%s-%s.tar.gz", if (isArm) "aarch64" else "x86_64", version)
   }

   sprintf("https://github.com/r-lib/rig/releases/download/v%s/%s", version, file)
})

#' Download the rig release archive to a temporary file. Prefers the curl
#' binary as it honors the system's proxy configuration and certificate
#' store; falls back to download.file() otherwise.
.rs.addFunction("rig.downloadArchive", function(url = .rs.rig.downloadUrl())
{
   destfile <- tempfile("rig-", fileext = if (grepl("[.]zip$", url)) ".zip" else ".tar.gz")

   curl <- Sys.which("curl")
   if (nzchar(curl))
   {
      args <- c("-fsSL", "--retry", "3", "-o", shQuote(destfile), shQuote(url))
      status <- system2(curl, args, stdout = FALSE, stderr = FALSE)
      if (status == 0L && file.exists(destfile))
         return(destfile)
   }

   # large downloads can exceed R's default 60 second timeout
   timeout <- getOption("timeout")
   options(timeout = max(600L, timeout))
   on.exit(options(timeout = timeout), add = TRUE)

   download.file(url, destfile = destfile, quiet = TRUE, mode = "wb")
   destfile
})

#' Extract a rig release archive into the RStudio-managed install root,
#' returning the path to the extracted rig binary.
.rs.addFunction("rig.installArchive", function(archive, root = .rs.rig.userInstallRoot())
{
   # extract into a staging directory so a failed extraction never leaves a
   # half-written install behind
   staging <- tempfile("rig-extract-")
   dir.create(staging, recursive = TRUE, showWarnings = FALSE)
   on.exit(unlink(staging, recursive = TRUE), add = TRUE)

   if (grepl("[.]zip$", archive))
      unzip(archive, exdir = staging)
   else
      untar(archive, exdir = staging)

   rig <- file.path(staging, "bin", .rs.rig.exeName())
   if (!file.exists(rig))
      stop("rig archive does not contain a rig binary")

   unlink(root, recursive = TRUE)
   dir.create(dirname(root), recursive = TRUE, showWarnings = FALSE)
   if (!file.rename(staging, root))
   {
      # a rename can fail across filesystems; fall back to a copy
      dir.create(root, recursive = TRUE, showWarnings = FALSE)
      file.copy(list.files(staging, full.names = TRUE), root, recursive = TRUE)
   }

   rig <- file.path(root, "bin", .rs.rig.exeName())
   Sys.chmod(rig, mode = "0755")
   normalizePath(rig, winslash = "/")
})

#' Download and install RStudio's own copy of rig, returning its path.
.rs.addFunction("rig.install", function()
{
   archive <- .rs.rig.downloadArchive()
   on.exit(unlink(archive), add = TRUE)
   .rs.rig.installArchive(archive)
})

#' Run rig with the given arguments, returning its parsed JSON output.
.rs.addFunction("rig.json", function(rig, args)
{
   output <- suppressWarnings(
      system2(rig, c(args, "--json"), stdout = TRUE, stderr = FALSE)
   )

   status <- attr(output, "status", exact = TRUE)
   if (!is.null(status) && status != 0L)
      stop(sprintf("rig %s failed with status %i", paste(args, collapse = " "), status))

   .rs.fromJSON(paste(output, collapse = "\n"))
})

#' The R versions rig knows about, as a data frame with columns 'version',
#' 'path' and 'binary'. rig only reports the installations belonging to the
#' mode it runs in, so both admin-mode and user-mode installations are
#' collected.
.rs.addFunction("rig.list", function(rig = .rs.rig.find())
{
   empty <- data.frame(
      version = character(),
      path    = character(),
      binary  = character(),
      stringsAsFactors = FALSE
   )

   if (!nzchar(rig))
      return(empty)

   entries <- list()
   for (mode in c("--admin", "--user"))
   {
      result <- .rs.tryCatch(.rs.rig.json(rig, c("list", mode)))
      if (!inherits(result, "error"))
         entries <- c(entries, result)
   }

   rows <- lapply(entries, function(entry) {
      if (is.null(entry$version) || is.null(entry$binary))
         return(NULL)

      data.frame(
         version = as.character(entry$version),
         path    = as.character(entry$path),
         binary  = as.character(entry$binary),
         stringsAsFactors = FALSE
      )
   })

   rows <- Filter(Negate(is.null), rows)
   if (length(rows) == 0L)
      return(empty)

   installed <- do.call(rbind, rows)
   installed[!duplicated(installed$binary), , drop = FALSE]
})

#' Find an installed R matching the requested version. Versions sharing the
#' requested major.minor are considered a match; an exact match is preferred,
#' then the newest patch release. Returns NULL when nothing matches.
.rs.addFunction("rig.findRVersion", function(version, installed = .rs.rig.list())
{
   if (nrow(installed) == 0L)
      return(NULL)

   requested <- .rs.tryCatch(package_version(version))
   if (inherits(requested, "error"))
      return(NULL)

   candidates <- installed[.rs.rVersionMatches(version, installed$version), , drop = FALSE]
   if (nrow(candidates) == 0L)
      return(NULL)

   exact <- candidates[candidates$version == as.character(requested), , drop = FALSE]
   match <- if (nrow(exact) > 0L)
      exact[1L, ]
   else
      candidates[which.max(package_version(candidates$version)), ]

   match <- as.list(match)
   match$home <- .rs.rig.rHome(match$binary)
   lapply(match, .rs.scalar)
})

#' The home directory of the R installation with the given binary.
.rs.addFunction("rig.rHome", function(binary)
{
   # the launcher script of a macOS framework install reports the
   # framework's current default version rather than its own, so derive the
   # home from the path instead of asking
   framework <- regmatches(binary, regexpr(".*/R[.]framework/Versions/[^/]+/Resources", binary))
   if (length(framework) == 1L)
      return(framework)

   if (!file.exists(binary))
      return("")

   home <- .rs.tryCatch(system2(binary, "RHOME", stdout = TRUE, stderr = FALSE))
   if (inherits(home, "error"))
      return("")

   status <- attr(home, "status", exact = TRUE)
   if (!is.null(status) || length(home) == 0L)
      return("")

   home[[length(home)]]
})

#' Whether each of 'versions' shares the major.minor of 'version'.
.rs.addFunction("rVersionMatches", function(version, versions)
{
   requested <- .rs.tryCatch(package_version(version))
   if (inherits(requested, "error"))
      return(rep.int(FALSE, length(versions)))

   vapply(versions, function(candidate) {
      candidate <- .rs.tryCatch(package_version(candidate))
      if (inherits(candidate, "error"))
         return(FALSE)

      requested[[c(1L, 1L)]] == candidate[[c(1L, 1L)]] &&
         requested[[c(1L, 2L)]] == candidate[[c(1L, 2L)]]
   }, FUN.VALUE = logical(1), USE.NAMES = FALSE)
})

#' The mode flag to install R with. User mode installs into the home
#' directory without needing administrator rights, so it is the default; a
#' mode the user configured for rig themselves is respected instead.
.rs.addFunction("rig.modeArgs", function(rig)
{
   if (nzchar(Sys.getenv("RIG_MODE")))
      return(character())

   configured <- suppressWarnings(
      system2(rig, c("config", "get", "mode"), stdout = TRUE, stderr = FALSE)
   )

   status <- attr(configured, "status", exact = TRUE)
   if (is.null(status) && any(nzchar(configured)))
      return(character())

   "--user"
})

#' Whether curl is available for downloading rig in a background job.
.rs.addFunction("rig.curlPath", function()
{
   unname(Sys.which("curl"))
})

#' The installed R versions as a list of records, for the client.
.rs.addFunction("rig.listAsJson", function()
{
   installed <- .rs.rig.list()
   lapply(seq_len(nrow(installed)), function(i) {
      lapply(as.list(installed[i, ]), .rs.scalar)
   })
})
