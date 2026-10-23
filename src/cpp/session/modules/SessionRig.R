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
# RStudio uses to discover and install versions of R, and for finding the
# versions of R installed on this machine.

# The rig release RStudio bundles. Keep dependencies/common/rig.cmake in sync.
.rs.setVar("rig.pinnedVersion", "0.11.0")

.rs.addFunction("rig.exeName", function()
{
   if (.rs.platform.isWindows) "rig.exe" else "rig"
})

#' The locations searched for a rig binary, in order of preference: an
#' explicit override, the bundled copy, the PATH, and standard install
#' locations.
.rs.addFunction("rig.candidatePaths", function(
   override = Sys.getenv("RSTUDIO_RIG", unset = ""),
   bundled = .Call("rs_rigPath", PACKAGE = "(embedding)"))
{
   exe <- .rs.rig.exeName()

   paths <- c(
      override,
      file.path(bundled, "bin", exe),
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

   paths[nzchar(paths)]
})

#' Locate a rig binary, or return "" if none is available. A rig older than
#' the pinned release may lack the commands and flags RStudio uses, so it is
#' passed over (in favor of the bundled copy).
.rs.addFunction("rig.find", function(paths = .rs.rig.candidatePaths())
{
   for (path in paths)
   {
      if (!file.exists(path))
         next

      version <- .rs.rig.version(path)
      if (!is.null(version) && version >= package_version(.rs.rig.pinnedVersion))
         return(normalizePath(path, winslash = "/"))
   }

   ""
})

#' The version of the rig binary at 'path', or NULL when it can't be told.
.rs.addFunction("rig.version", function(path)
{
   # e.g. "RIG -- The R Installation Manager 0.11.0"
   output <- .rs.tryCatch(suppressWarnings(
      system2(path, "--version", stdout = TRUE, stderr = FALSE)
   ))

   if (inherits(output, "error") || length(output) == 0L)
      return(NULL)

   match <- regmatches(output[[1L]], regexpr("[0-9]+[.][0-9]+[.][0-9]+", output[[1L]]))
   if (length(match) == 0L)
      return(NULL)

   package_version(match)
})

#' The architecture of this machine. On macOS, an x86_64 build of R running
#' on Apple silicon (under Rosetta) reports the emulated architecture, so the
#' hardware is asked instead. On Windows, an x64 build of R on an ARM64
#' machine does the same, so the system's own kernel32.dll is read instead.
.rs.addFunction("rig.machine", function(machine = Sys.info()[["machine"]],
                                        kernel32 = file.path(Sys.getenv("SystemRoot", unset = "C:/Windows"), "System32", "kernel32.dll"))
{
   if (.rs.platform.isWindows)
      return(if (identical(.rs.rInstallations.peMachine(kernel32), "arm64")) "arm64" else machine)

   if (!.rs.platform.isMacos || identical(machine, "arm64"))
      return(machine)

   arm64 <- .rs.tryCatch(suppressWarnings(
      system2("/usr/sbin/sysctl", c("-n", "hw.optional.arm64"), stdout = TRUE, stderr = FALSE)
   ))

   if (is.character(arm64) && identical(trimws(arm64[1L]), "1"))
      return("arm64")

   machine
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

#' An empty table of R installations.
.rs.addFunction("rInstallations.empty", function()
{
   data.frame(
      version = character(),
      path    = character(),
      binary  = character(),
      home    = character(),
      stringsAsFactors = FALSE
   )
})

#' The R versions rig knows about, as a data frame with columns 'version',
#' 'path', 'binary' and 'home'. rig only reports the installations belonging
#' to the mode it runs in, so both admin-mode and user-mode installations are
#' collected.
.rs.addFunction("rig.list", function(rig = .rs.rig.find())
{
   if (!nzchar(rig))
      return(.rs.rInstallations.empty())

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

      path <- if (is.null(entry$path)) "" else as.character(entry$path)
      data.frame(
         version = as.character(entry$version),
         path    = path,
         binary  = as.character(entry$binary),
         home    = .rs.rInstallations.homeFromPath(path),
         stringsAsFactors = FALSE
      )
   })

   .rs.rInstallations.bind(rows)
})

.rs.addFunction("rInstallations.bind", function(rows)
{
   rows <- Filter(Negate(is.null), rows)
   if (length(rows) == 0L)
      return(.rs.rInstallations.empty())

   installed <- do.call(rbind, rows)
   installed[!duplicated(installed$binary), , drop = FALSE]
})

#' Whether a directory is the home of an R installation.
.rs.addFunction("rInstallations.isHome", function(dir)
{
   nzchar(dir) && dir.exists(file.path(dir, "library", "base"))
})

#' The home of the R installation rig reports at 'path' (the installation's
#' root: a framework version on macOS, a prefix such as /opt/R/4.4.1 on Linux,
#' the install directory on Windows), or "" when it can't be told from the
#' layout.
.rs.addFunction("rInstallations.homeFromPath", function(path)
{
   if (!nzchar(path))
      return("")

   candidates <- file.path(path, c("Resources", "lib/R", "lib64/R", "."))
   for (candidate in candidates)
      if (.rs.rInstallations.isHome(candidate))
         return(normalizePath(candidate, winslash = "/", mustWork = FALSE))

   ""
})

#' The version of the R installation at 'home', read from its headers, or ""
#' when they are missing.
.rs.addFunction("rInstallations.versionFromHome", function(home)
{
   header <- file.path(home, "include", "Rversion.h")
   if (!file.exists(header))
      return("")

   contents <- .rs.tryCatch(readLines(header, warn = FALSE))
   if (inherits(contents, "error"))
      return("")

   version <- paste(
      .rs.rInstallations.headerField(contents, "R_MAJOR"),
      .rs.rInstallations.headerField(contents, "R_MINOR"),
      sep = "."
   )

   if (.rs.rVersionIsValid(version)) version else ""
})

#' The value of a string #define in the lines of a C header, or "".
.rs.addFunction("rInstallations.headerField", function(contents, name)
{
   line <- grep(sprintf("^#define %s\\s", name), contents, value = TRUE)
   if (length(line) == 0L)
      return("")

   sub(".*\"(.*)\".*", "\\1", line[[1L]])
})

#' The R installations found in the platform's standard locations, in the
#' same form as rig.list(). This finds R installed without rig (e.g. by the
#' CRAN installers, or under /opt/R).
.rs.addFunction("rInstallations.scan", function()
{
   # each entry: the home directory, and the executable launching it
   entries <- if (.rs.platform.isWindows)
   {
      roots <- c(
         file.path(Sys.getenv("ProgramFiles", unset = "C:/Program Files"), "R"),
         file.path(Sys.getenv("ProgramFiles", unset = "C:/Program Files"), "R-aarch64"),
         file.path(Sys.getenv("LOCALAPPDATA", unset = ""), "Programs", "R"),
         file.path(Sys.getenv("LOCALAPPDATA", unset = ""), "Programs", "R-aarch64"),
         "C:/R"
      )
      homes <- list.files(roots[nzchar(roots)], pattern = "^R-", full.names = TRUE)
      lapply(homes, function(home) c(home, file.path(home, "bin", "R.exe")))
   }
   else if (.rs.platform.isMacos)
   {
      versions <- list.files("/Library/Frameworks/R.framework/Versions", full.names = TRUE)
      versions <- versions[basename(versions) != "Current"]
      lapply(versions, function(version) {
         home <- file.path(version, "Resources")
         c(home, file.path(home, "bin", "R"))
      })
   }
   else
   {
      prefixes <- list.files("/opt/R", full.names = TRUE)
      c(
         lapply(prefixes, function(prefix) c(file.path(prefix, "lib", "R"), file.path(prefix, "bin", "R"))),
         list(
            c("/usr/lib/R", "/usr/bin/R"),
            c("/usr/lib64/R", "/usr/bin/R"),
            c("/usr/local/lib/R", "/usr/local/bin/R")
         )
      )
   }

   rows <- lapply(entries, function(entry) {
      home <- entry[[1L]]
      binary <- entry[[2L]]
      if (!.rs.rInstallations.isHome(home) || !file.exists(binary))
         return(NULL)

      version <- .rs.rInstallations.versionFromHome(home)
      if (!nzchar(version))
         return(NULL)

      data.frame(
         version = version,
         path    = home,
         binary  = binary,
         home    = normalizePath(home, winslash = "/", mustWork = FALSE),
         stringsAsFactors = FALSE
      )
   })

   .rs.rInstallations.bind(rows)
})

#' The R installations on this machine: those rig reports, plus those found
#' in the standard locations, so that R installed without rig is found even
#' when rig is unavailable.
.rs.addFunction("rInstallations.list", function(rig = .rs.rig.find())
{
   installed <- rbind(.rs.rig.list(rig), .rs.rInstallations.scan())
   key <- ifelse(nzchar(installed$home), installed$home, installed$binary)
   installed[!duplicated(key), , drop = FALSE]
})

.rs.setVar("rInstallationsCache", new.env(parent = emptyenv()))

#' The R installations on this machine, as last listed this session.
#' Listing them runs rig (and possibly R) several times, so the list is
#' reused until a refresh is asked for.
.rs.addFunction("rInstallations.cached", function(refresh = FALSE)
{
   cache <- .rs.rInstallationsCache
   if (refresh || is.null(cache$installed))
      cache$installed <- .rs.rInstallations.list()

   cache$installed
})

#' The architecture a Windows program or DLL was built for ("arm64",
#' "x86_64", "i386"), read from its PE header; "" when unreadable or other.
.rs.addFunction("rInstallations.peMachine", function(path)
{
   con <- .rs.tryCatch(file(path, open = "rb"))
   if (inherits(con, "error"))
      return("")
   on.exit(close(con), add = TRUE)

   # the DOS header ends with the offset of the PE signature
   dos <- readBin(con, "raw", n = 64L)
   if (length(dos) < 64L || !identical(dos[1:2], charToRaw("MZ")))
      return("")

   offset <- readBin(dos[61:64], "integer", size = 4L, endian = "little")
   if (offset < 64L)
      return("")

   seek(con, offset)
   header <- readBin(con, "raw", n = 6L)
   if (length(header) < 6L || !identical(header[1:4], as.raw(c(0x50, 0x45, 0x00, 0x00))))
      return("")

   machine <- readBin(header[5:6], "integer", size = 2L, signed = FALSE, endian = "little")
   known <- c("43620" = "arm64", "34404" = "x86_64", "332" = "i386")
   architecture <- known[as.character(machine)]
   if (is.na(architecture)) "" else unname(architecture)
})

#' The architectures an R installation was built for ("arm64", "x86_64",
#' "i386"): on macOS from its libR.dylib's Mach-O header, on Windows from the
#' PE header of each R.dll it has. Empty when unreadable.
.rs.addFunction("rInstallations.architectures", function(home)
{
   if (.rs.platform.isWindows)
   {
      # an ARM64 R keeps R.dll in bin itself, an x64 or 32-bit R in a subfolder
      dlls <- file.path(home, c("bin", "bin/x64", "bin/i386"), "R.dll")
      architectures <- vapply(dlls, .rs.rInstallations.peMachine, character(1), USE.NAMES = FALSE)
      return(unique(architectures[nzchar(architectures)]))
   }

   lib <- file.path(home, "lib", "libR.dylib")
   con <- .rs.tryCatch(file(lib, open = "rb"))
   if (inherits(con, "error"))
      return(character())
   on.exit(close(con), add = TRUE)

   header <- readBin(con, "raw", n = 8L)
   if (length(header) < 8L)
      return(character())

   magic <- paste(as.character(header[1:4]), collapse = "")

   # a thin 64-bit image: its CPU type follows the magic (little-endian)
   cpuTypes <- if (identical(magic, "cffaedfe"))
   {
      readBin(header[5:8], "integer", size = 4L, endian = "little")
   }
   else if (magic %in% c("cafebabe", "cafebabf"))
   {
      # a universal image: a big-endian table of the images it contains
      count <- readBin(header[5:8], "integer", size = 4L, endian = "big")
      entrySize <- if (identical(magic, "cafebabe")) 20L else 32L
      entries <- readBin(con, "raw", n = max(0L, min(count, 16L)) * entrySize)
      offsets <- (seq_len(length(entries) %/% entrySize) - 1L) * entrySize + 1L
      vapply(offsets, function(offset) {
         readBin(entries[offset:(offset + 3L)], "integer", size = 4L, endian = "big")
      }, integer(1))
   }

   known <- c("16777228" = "arm64", "16777223" = "x86_64")
   architectures <- known[as.character(cpuTypes)]
   unname(architectures[!is.na(architectures)])
})

#' The architecture an installation of R should be built for to run
#' natively here, when that is a choice: on Apple silicon Macs and ARM64
#' Windows, where x86_64 builds also run (emulated). NULL otherwise.
.rs.addFunction("rInstallations.preferredArchitecture", function(machine = .rs.rig.machine())
{
   if ((.rs.platform.isMacos || .rs.platform.isWindows) && identical(machine, "arm64")) "arm64"
})

#' Whether an installation of R runs as itself when it isn't the default.
#' Every version in the macOS R framework comes with a launcher script (and
#' other files) naming the framework's shared Resources directory, which
#' follows whichever version is the framework's current one: launching such
#' a version, or any R process it starts, runs the current version instead.
#' rig makes a version "orthogonal" by rewriting those paths to its own
#' directory (see 'rig system make-orthogonal'); the launcher script tells
#' whether that happened. The current version itself is what the shared
#' paths resolve to, so it runs as itself as it is. Other installations are
#' always orthogonal.
.rs.addFunction("rInstallations.isOrthogonal", function(home)
{
   if (!grepl("/R[.]framework/Versions/[^/]+/Resources$", home))
      return(TRUE)

   if (.rs.rInstallations.isCurrentFrameworkVersion(home))
      return(TRUE)

   launcher <- .rs.tryCatch(readLines(file.path(home, "bin", "R"), warn = FALSE))
   if (inherits(launcher, "error"))
      return(TRUE)

   !any(grepl("R.framework/Resources", launcher, fixed = TRUE))
})

#' Whether a macOS framework version, given by its home, is the one the
#' framework's 'Current' link points to.
.rs.addFunction("rInstallations.isCurrentFrameworkVersion", function(home)
{
   dir <- dirname(home)
   current <- file.path(dirname(dir), "Current")
   if (!file.exists(current))
      return(FALSE)

   identical(
      normalizePath(current, winslash = "/", mustWork = FALSE),
      normalizePath(dir, winslash = "/", mustWork = FALSE)
   )
})

#' Find an installed R matching the requested version. Versions sharing the
#' requested major.minor are considered a match; an exact match is preferred,
#' then the newest patch release. A build for the preferred architecture is
#' chosen over others. Returns NULL when nothing matches. The installations
#' are listed afresh unless 'refresh' is FALSE, when the list from an
#' earlier lookup this session is reused.
.rs.addFunction("findInstalledRVersion", function(version,
                                                  installed = .rs.rInstallations.cached(refresh),
                                                  architecture = .rs.rInstallations.preferredArchitecture(),
                                                  refresh = TRUE)
{
   if (nrow(installed) == 0L)
      return(NULL)

   requested <- .rs.tryCatch(package_version(version))
   if (inherits(requested, "error"))
      return(NULL)

   candidates <- installed[.rs.rVersionMatches(version, installed$version), , drop = FALSE]
   if (nrow(candidates) == 0L)
      return(NULL)

   # an emulated R would need an emulated session as well
   if (!is.null(architecture))
   {
      native <- vapply(candidates$home, function(home) {
         architecture %in% .rs.rInstallations.architectures(home)
      }, logical(1), USE.NAMES = FALSE)

      if (any(native))
         candidates <- candidates[native, , drop = FALSE]
   }

   exact <- candidates[candidates$version == as.character(requested), , drop = FALSE]
   match <- if (nrow(exact) > 0L)
      exact[1L, ]
   else
      candidates[which.max(package_version(candidates$version)), ]

   match <- as.list(match)
   if (!nzchar(match$home))
      match$home <- .rs.rig.rHome(match$binary)

   match$orthogonal <- .rs.rInstallations.isOrthogonal(match$home)

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

#' Whether a string is an R version number (major.minor, optionally with a
#' patch level). Versions come from project files, so anything else is
#' ignored rather than shown to the user.
.rs.addFunction("rVersionIsValid", function(version)
{
   is.character(version) &&
      length(version) == 1L &&
      !is.na(version) &&
      grepl("^[0-9]+[.][0-9]+([.][0-9]+)?$", version)
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

#' Whether this build of RStudio can run the given version of R. Only the
#' minimum is enforced: newer versions than RStudio was tested with still
#' run (the session merely logs a warning).
.rs.addFunction("rVersionSupported", function(version, minimum)
{
   parsed <- .rs.tryCatch(package_version(version))
   !inherits(parsed, "error") && parsed >= package_version(minimum)
})
