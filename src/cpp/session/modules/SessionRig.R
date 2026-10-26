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
#' hardware is asked instead.
.rs.addFunction("rig.machine", function(machine = Sys.info()[["machine"]])
{
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

.rs.addFunction("rig.platform", function()
{
   if (.rs.platform.isWindows)
      "windows"
   else if (.rs.platform.isMacos)
      "macos"
   else
      "linux"
})

#' The files rig creates when it makes a user-mode R the default R: its
#' 'Current' link and the R and Rscript quick links (R.exe, RS.exe and
#' Rscript.exe shims on Windows), and on macOS a LaunchAgent that exports
#' RSTUDIO_WHICH_R to every GUI application at login. 'rig add' sets a
#' default whenever none is set yet, so an R installed from the project
#' R-version warning bar would become the default R outside RStudio too;
#' what the install adds or changes is put back once it finishes (#19229).
.rs.addFunction("rig.userDefaultPaths", function(dirs,
                                                 home = Sys.getenv("HOME"),
                                                 platform = .rs.rig.platform())
{
   if (identical(platform, "windows"))
   {
      c(
         R       = file.path(dirs$binary_dir, "R.exe"),
         RS      = file.path(dirs$binary_dir, "RS.exe"),
         Rscript = file.path(dirs$binary_dir, "Rscript.exe")
      )
   }
   else if (identical(platform, "macos"))
   {
      c(
         current     = file.path(dirs$r_root, "Current"),
         R           = file.path(dirs$binary_dir, "R"),
         Rscript     = file.path(dirs$binary_dir, "Rscript"),
         launchAgent = file.path(home, "Library/LaunchAgents/io.r-lib.rig.rstudio-which-r.plist")
      )
   }
   else
   {
      c(
         current = file.path(dirs$r_root, "current"),
         R       = file.path(dirs$binary_dir, "R"),
         Rscript = file.path(dirs$binary_dir, "Rscript")
      )
   }
})

#' The registry values rig writes on Windows: once a user-mode R becomes the
#' default, the current-user R-core default (the R and R64 keys, and their
#' WOW6432Node twins used for x86_64 R on arm64); and on every install the
#' user PATH, which gets rig's quick-link directory prepended. Paths are in
#' PowerShell's registry-provider form.
.rs.addFunction("rig.registryEntries", function()
{
   keys <- c(
      "HKCU:\\Software\\R-core\\R",
      "HKCU:\\Software\\R-core\\R64",
      "HKCU:\\Software\\WOW6432Node\\R-core\\R",
      "HKCU:\\Software\\WOW6432Node\\R-core\\R64"
   )

   entries <- expand.grid(
      name = c("Current Version", "InstallPath"),
      key = keys,
      stringsAsFactors = FALSE
   )

   entries <- rbind(entries, data.frame(name = "Path", key = "HKCU:\\Environment", stringsAsFactors = FALSE))
   entries[c("key", "name")]
})

.rs.addFunction("rig.registryId", function(key, name)
{
   paste(key, name, sep = "\\")
})

#' A PowerShell single-quoted string literal. Besides the ASCII apostrophe,
#' PowerShell takes the typographic single quotes (U+2018 to U+201B) as
#' delimiters too, so a path holding one would end the literal early unless
#' it is doubled as well.
.rs.addFunction("rig.psQuote", function(x)
{
   paste0("'", gsub("(['\u2018\u2019\u201a\u201b])", "\\1\\1", enc2utf8(x), perl = TRUE), "'")
})

#' A PowerShell script printing the registry values rig may write, as JSON:
#' key, name, type (the value kind) and data (unexpanded) of each that exists.
.rs.addFunction("rig.registryReadScript", function(entries = .rs.rig.registryEntries())
{
   items <- sprintf("@(%s, %s)", .rs.rig.psQuote(entries$key), .rs.rig.psQuote(entries$name))
   c(
      "$ErrorActionPreference = 'SilentlyContinue'",
      "[Console]::OutputEncoding = [System.Text.Encoding]::UTF8",
      "$values = @()",
      sprintf("foreach ($entry in @(%s)) {", paste(items, collapse = ", ")),
      "   $key = Get-Item -LiteralPath $entry[0]",
      "   if ($key -eq $null) { continue }",
      "   $name = $key.GetValueNames() | Where-Object { $_ -eq $entry[1] } | Select-Object -First 1",
      "   if ($name -eq $null) { continue }",
      "   $values += @{",
      "      key  = $entry[0];",
      "      name = $entry[1];",
      "      type = $key.GetValueKind($name).ToString();",
      "      data = [string] $key.GetValue($name, '', 'DoNotExpandEnvironmentNames')",
      "   }",
      "}",
      "ConvertTo-Json -InputObject @($values) -Compress"
   )
})

#' A PowerShell script applying registry operations: each is a list with
#' 'op' ("set" or "remove"), 'key' and 'name', plus 'type' and 'data' to set.
.rs.addFunction("rig.registryWriteScript", function(ops)
{
   vapply(ops, function(op) {
      if (identical(op$op, "remove"))
         sprintf(
            "Remove-ItemProperty -LiteralPath %s -Name %s -ErrorAction SilentlyContinue",
            .rs.rig.psQuote(op$key),
            .rs.rig.psQuote(op$name)
         )
      else
         sprintf(
            "Set-ItemProperty -LiteralPath %s -Name %s -Type %s -Value %s",
            .rs.rig.psQuote(op$key),
            .rs.rig.psQuote(op$name),
            op$type,
            .rs.rig.psQuote(op$data)
         )
   }, FUN.VALUE = character(1))
})

#' Write a PowerShell script as UTF-8 with a byte order mark: Windows
#' PowerShell reads a script without one in the system ANSI code page, which
#' would garble non-ASCII registry data (a user's PATH, say) on the way back
#' into the registry.
.rs.addFunction("rig.writePowershellScript", function(lines, path)
{
   con <- file(path, open = "wb")
   on.exit(close(con), add = TRUE)
   writeBin(as.raw(c(0xEF, 0xBB, 0xBF)), con)
   writeLines(enc2utf8(lines), con, useBytes = TRUE)
})

#' Run a PowerShell script, returning its standard output. The script is
#' expected to set the console output encoding to UTF-8 when it prints
#' anything that matters, as the registry reader does.
.rs.addFunction("rig.powershell", function(lines)
{
   script <- tempfile("rstudio-rig-", fileext = ".ps1")
   on.exit(unlink(script), add = TRUE)
   .rs.rig.writePowershellScript(lines, script)

   output <- suppressWarnings(system2(
      "powershell",
      c("-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-File", shQuote(script)),
      stdout = TRUE,
      stderr = FALSE
   ))

   status <- attr(output, "status", exact = TRUE)
   if (!is.null(status) && status != 0L)
      stop(sprintf("powershell exited with status %i", status))

   Encoding(output) <- "UTF-8"
   output
})

#' The registry values rig may write that exist now, as a list keyed by
#' .rs.rig.registryId(), each a list with 'type' and 'data'.
.rs.addFunction("rig.registryRead", function()
{
   output <- .rs.rig.powershell(.rs.rig.registryReadScript())
   items <- .rs.fromJSON(paste(output, collapse = "\n"))
   if (!is.null(items$key))
      items <- list(items)

   values <- list()
   for (item in items)
      values[[.rs.rig.registryId(item$key, item$name)]] <- list(type = item$type, data = item$data)

   values
})

.rs.addFunction("rig.registryWrite", function(ops)
{
   if (length(ops))
      .rs.rig.powershell(.rs.rig.registryWriteScript(ops))

   invisible(NULL)
})

.rs.addFunction("rig.pathHasDir", function(path, dir)
{
   if (is.null(path))
      return(FALSE)

   entries <- strsplit(path, ";", fixed = TRUE)[[1L]]
   tolower(dir) %in% tolower(trimws(entries))
})

#' The registry operations that take the values back to 'before' where rig
#' changed them: values rig added are removed, values it changed are set
#' back. The user PATH only loses rig's quick-link directory, so other
#' changes to it survive. Nothing is done about values that disappeared.
.rs.addFunction("rig.registryRestoreOps", function(before, after, binaryDir)
{
   ops <- list()
   pathId <- .rs.rig.registryId("HKCU:\\Environment", "Path")

   for (id in names(after))
   {
      old <- before[[id]]
      new <- after[[id]]
      op <- NULL

      if (identical(id, pathId))
      {
         if (!.rs.rig.pathHasDir(old$data, binaryDir) && .rs.rig.pathHasDir(new$data, binaryDir))
         {
            entries <- strsplit(new$data, ";", fixed = TRUE)[[1L]]
            kept <- entries[tolower(trimws(entries)) != tolower(binaryDir)]
            if (length(kept) == 0L && is.null(old))
               op <- list(op = "remove", key = "HKCU:\\Environment", name = "Path")
            else
               op <- list(op = "set", key = "HKCU:\\Environment", name = "Path", type = new$type, data = paste(kept, collapse = ";"))
         }
      }
      else if (is.null(old))
      {
         op <- list(op = "remove", key = sub("\\\\[^\\\\]*$", "", id), name = sub("^.*\\\\", "", id))
      }
      else if (!identical(old$data, new$data) || !identical(old$type, new$type))
      {
         op <- list(op = "set", key = sub("\\\\[^\\\\]*$", "", id), name = sub("^.*\\\\", "", id), type = old$type, data = old$data)
      }

      if (!is.null(op))
         ops[[length(ops) + 1L]] <- op
   }

   ops
})

.rs.addFunction("rig.registryOpLabel", function(op, binaryDir)
{
   if (identical(op$key, "HKCU:\\Environment") && identical(op$name, "Path"))
      sprintf("user PATH entry %s", binaryDir)
   else
      .rs.rig.registryId(op$key, op$name)
})

# the label of rig's LaunchAgent, as launchd knows it
.rs.setVar("rig.launchAgentLabel", "io.r-lib.rig.rstudio-which-r")

#' Whether each path exists, counting dangling symlinks (which file.exists()
#' follows, and so misses).
.rs.addFunction("rig.pathExists", function(paths)
{
   # Sys.readlink() gives NA for a missing path, and nzchar(NA) is TRUE
   targets <- Sys.readlink(paths)
   exists <- file.exists(paths) | (!is.na(targets) & nzchar(targets))
   names(exists) <- names(paths)
   exists
})

#' What each path links to: "" for a regular file or a missing path.
.rs.addFunction("rig.linkTargets", function(paths)
{
   targets <- Sys.readlink(paths)
   targets[is.na(targets)] <- ""
   names(targets) <- names(paths)
   targets
})

#' The bytes of a file, or NULL when it is missing or unreadable.
.rs.addFunction("rig.readBytes", function(path)
{
   if (!file.exists(path))
      return(NULL)

   bytes <- .rs.tryCatch(readBin(path, what = "raw", n = file.size(path)))
   if (inherits(bytes, "error"))
      return(NULL)

   bytes
})

#' Run launchctl, returning its output, or NULL when it fails.
.rs.addFunction("rig.launchctl", function(args)
{
   output <- .rs.tryCatch(suppressWarnings(
      system2("/bin/launchctl", shQuote(args), stdout = TRUE, stderr = FALSE)
   ))

   if (inherits(output, "error") || !is.null(attr(output, "status", exact = TRUE)))
      return(NULL)

   paste(output, collapse = "\n")
})

#' The state of rig's default-R setup before a user-mode install: which of
#' its files exist and what they link to, the LaunchAgent's contents, and
#' whether launchd has the agent loaded and which RSTUDIO_WHICH_R it gives
#' GUI applications; on Windows (through 'registry') the registry values rig
#' may write instead, with 'binaryDir' the quick-link directory it puts on
#' the user PATH. The snapshot records the session that took it, so that a
#' later session can tell a leftover from an install still under way.
.rs.addFunction("rig.userDefaultSnapshot", function(paths,
                                                    launchctl = .rs.rig.launchctl,
                                                    registry = if (.rs.platform.isWindows) .rs.rig.registryRead,
                                                    binaryDir = NULL)
{
   snapshot <- list(
      pid       = Sys.getpid(),
      paths     = paths,
      existed   = .rs.rig.pathExists(paths),
      targets   = .rs.rig.linkTargets(paths),
      agent     = NULL,
      loaded    = FALSE,
      whichR    = "",
      registry  = NULL,
      binaryDir = binaryDir
   )

   if ("launchAgent" %in% names(paths))
   {
      snapshot$agent <- .rs.rig.readBytes(paths[["launchAgent"]])
      snapshot$loaded <- !is.null(launchctl(c("list", .rs.rig.launchAgentLabel)))

      whichR <- launchctl(c("getenv", "RSTUDIO_WHICH_R"))
      if (!is.null(whichR))
         snapshot$whichR <- whichR
   }

   # a registry that cannot be read leaves its values out of the snapshot
   # (and so out of the restore); the reason is kept for the job's log
   if (!is.null(registry))
   {
      values <- .rs.tryCatch(registry())
      if (inherits(values, "error"))
         snapshot$registryError <- conditionMessage(values)
      else
         snapshot$registry <- values
   }

   snapshot
})

#' Undo what a user-mode install did to rig's default-R setup since the
#' snapshot was taken: the files it created are removed, the links and the
#' LaunchAgent it rewrote are put back (rig repoints a dangling 'Current'
#' and rewrites a stale agent), and launchd's agent and RSTUDIO_WHICH_R are
#' left as they were. A link whose directory has since been removed is left
#' alone. On Windows the registry values rig changed are taken back as well
#' (the current-user R-core default it set, and its entry on the user PATH).
#' Returns the paths and registry values removed and restored, and a message
#' for each step that failed.
.rs.addFunction("rig.userDefaultRestore", function(snapshot,
                                                   launchctl = .rs.rig.launchctl,
                                                   registry = if (.rs.platform.isWindows) .rs.rig.registryRead,
                                                   registryWrite = .rs.rig.registryWrite)
{
   paths <- snapshot$paths
   exists <- .rs.rig.pathExists(paths)
   targets <- .rs.rig.linkTargets(paths)

   created <- !snapshot$existed & exists
   relinked <- snapshot$existed & nzchar(snapshot$targets) & targets != snapshot$targets

   removed <- character()
   restored <- character()
   failed <- character()

   # the agent is unloaded before its file goes or changes: loading it ran
   # 'launchctl setenv', so the variable it exported is put back afterwards
   # as well (unloading alone leaves it set until logout)
   if ("launchAgent" %in% names(paths))
   {
      agent <- paths[["launchAgent"]]
      rewritten <- snapshot$existed[["launchAgent"]] && exists[["launchAgent"]] &&
         !identical(.rs.rig.readBytes(agent), snapshot$agent)
      loaded <- !is.null(launchctl(c("list", .rs.rig.launchAgentLabel)))

      if (loaded && (created[["launchAgent"]] || rewritten || !snapshot$loaded))
      {
         if (is.null(launchctl(c("unload", agent))))
            failed <- c(failed, sprintf("'launchctl unload %s' failed", agent))
         else
            loaded <- FALSE
      }

      # an agent that would not unload keeps its file: unloading needs it,
      # and a later try then still sees it as created or rewritten
      if (loaded)
         created[["launchAgent"]] <- FALSE

      if (rewritten && !loaded)
      {
         written <- .rs.tryCatch(writeBin(snapshot$agent, agent))
         if (inherits(written, "error"))
            failed <- c(failed, sprintf("could not restore %s: %s", agent, conditionMessage(written)))
         else
            restored <- c(restored, agent)
      }

      # an agent that was loaded is loaded again once its file is as it was
      # (also after an earlier try that put the file back but failed to load it)
      if (snapshot$loaded && !loaded && !created[["launchAgent"]] &&
          identical(.rs.rig.readBytes(agent), snapshot$agent))
      {
         if (is.null(launchctl(c("load", agent))))
            failed <- c(failed, sprintf("'launchctl load %s' failed", agent))
      }

      whichR <- launchctl(c("getenv", "RSTUDIO_WHICH_R"))
      if (!is.null(whichR) && !identical(whichR, snapshot$whichR))
      {
         args <- if (nzchar(snapshot$whichR))
            c("setenv", "RSTUDIO_WHICH_R", snapshot$whichR)
         else
            c("unsetenv", "RSTUDIO_WHICH_R")

         if (is.null(launchctl(args)))
            failed <- c(failed, sprintf("'launchctl %s' failed", paste(args, collapse = " ")))
      }
   }

   # the links go, not what they point at
   for (path in paths[created])
   {
      if (suppressWarnings(file.remove(path)))
         removed <- c(removed, path)
      else
         failed <- c(failed, sprintf("could not remove %s", path))
   }

   for (name in names(paths)[relinked])
   {
      path <- paths[[name]]

      # a link whose directory is gone since (rig's setup removed by hand
      # after a session exited mid-install) is left that way: recreating the
      # directory for a link back to its old target would not restore anything
      if (!dir.exists(dirname(path)))
         next

      if (exists[[name]] && !suppressWarnings(file.remove(path)))
      {
         failed <- c(failed, sprintf("could not remove %s", path))
         next
      }

      if (suppressWarnings(file.symlink(snapshot$targets[[name]], path)))
         restored <- c(restored, path)
      else
         failed <- c(failed, sprintf("could not restore the link %s", path))
   }

   # the registry values rig added go, the ones it changed are set back; a
   # restore that fails is tried again whole, as the operations only ever
   # take the values back toward the snapshot
   if (!is.null(snapshot$registry) && !is.null(registry))
   {
      after <- .rs.tryCatch(registry())
      if (inherits(after, "error"))
      {
         failed <- c(failed, sprintf("could not read rig's registry values: %s", conditionMessage(after)))
      }
      else
      {
         ops <- .rs.rig.registryRestoreOps(snapshot$registry, after, snapshot$binaryDir)
         labels <- vapply(ops, .rs.rig.registryOpLabel, character(1), binaryDir = snapshot$binaryDir)
         kinds <- vapply(ops, function(op) op$op, character(1))

         written <- NULL
         if (length(ops))
            written <- .rs.tryCatch(registryWrite(ops))

         if (inherits(written, "error"))
         {
            failed <- c(failed, sprintf(
               "could not restore the registry values %s: %s",
               paste(labels, collapse = ", "),
               conditionMessage(written)
            ))
         }
         else
         {
            removed <- c(removed, labels[kinds == "remove"])
            restored <- c(restored, labels[kinds != "remove"])
         }
      }
   }

   list(removed = unname(removed), restored = unname(restored), failed = failed)
})

#' Lines for the installation job's log describing what a restore did.
.rs.addFunction("rig.restoreMessages", function(result)
{
   c(
      character(),
      if (length(result$removed))
         paste("Removed the default R setup rig added outside RStudio:", paste(result$removed, collapse = ", ")),
      if (length(result$restored))
         paste("Put back the default R setup rig changed outside RStudio:", paste(result$restored, collapse = ", ")),
      if (length(result$failed))
         paste("Could not undo a change rig made outside RStudio:", result$failed)
   )
})

.rs.setVar("rig.installState", new.env(parent = emptyenv()))

#' Create rig's user-mode quick-link directory when it is missing. rig 0.11.0
#' fails outright ("No such file or directory") in a home without it, as on
#' a fresh macOS account (r-lib/rig#491). Returns TRUE when the directory
#' exists afterwards.
.rs.addFunction("rig.ensureBinaryDir", function(dirs)
{
   dir <- dirs$binary_dir
   if (is.null(dir) || !nzchar(dir))
      return(invisible(FALSE))

   if (!dir.exists(dir))
      dir.create(dir, recursive = TRUE, showWarnings = FALSE)

   invisible(dir.exists(dir))
})

#' Write the snapshot to 'file', replacing any earlier one whole (so that a
#' failed write leaves it as it was). Returns a message when that fails.
.rs.addFunction("rig.saveSnapshot", function(snapshot, file)
{
   staged <- tempfile("rig-install-snapshot-", tmpdir = dirname(file))
   saved <- .rs.tryCatch({
      saveRDS(snapshot, staged)
      if (!file.rename(staged, file))
         stop("cannot rename ", staged)
   })

   if (inherits(saved, "error"))
   {
      unlink(staged)
      return(sprintf("Could not save rig's default R setup to %s: %s", file, conditionMessage(saved)))
   }

   character()
})

#' Whether the install a snapshot was taken for may still be under way: the
#' session that took it, or the rig it started, is still running. (On macOS
#' rig outlives a session that crashes.)
.rs.addFunction("rig.snapshotInUse", function(snapshot, running = .rs.rig.processRunning)
{
   pids <- c(snapshot$pid, snapshot$rigPid)
   pids <- pids[!is.na(pids) & pids != Sys.getpid()]
   any(vapply(pids, running, logical(1)))
})

#' Called before 'rig add --user' runs, to remember rig's default-R setup.
#' The snapshot is also written to 'file', when one is given: rig sets the
#' default before its slow pak step, so a session that exits mid-install
#' may not get to undo it, and the next session then does (from the file).
#' A snapshot another session left there is undone first, so that its
#' leftovers do not pass for the setup to keep; while that session (or its
#' rig) still runs, or until its leftovers are all undone (or the snapshot
#' is deleted), the install is refused. Without a saved snapshot there is
#' no undoing what rig adds, so
#' the install is refused then as well. Returns messages for the job's log.
.rs.addFunction("rig.userInstallBegin", function(rig,
                                                 file = NULL,
                                                 launchctl = .rs.rig.launchctl,
                                                 running = .rs.rig.processRunning)
{
   state <- .rs.rig.installState
   state$snapshot <- NULL
   state$file <- NULL

   notes <- character()
   if (!is.null(file) && file.exists(file))
   {
      notes <- .rs.rig.userInstallRecover(file, running, launchctl)
      if (file.exists(file))
      {
         if (length(notes) == 0L)
            stop("another RStudio session is still installing R")

         # the file is named so that a leftover that can never be undone
         # (say, its directory lost its permissions for good) does not block
         # installs forever
         stop(paste(
            c(
               "an earlier installation's changes are not all undone yet:",
               notes,
               sprintf("(to leave them as they are, delete %s)", file)
            ),
            collapse = "\n"
         ))
      }
   }

   dirs <- .rs.rig.json(rig, c("system", "dirs", "--user"))
   .rs.rig.ensureBinaryDir(dirs)

   paths <- .rs.rig.userDefaultPaths(dirs)
   snapshot <- .rs.rig.userDefaultSnapshot(paths, launchctl, binaryDir = dirs$binary_dir)
   if (!is.null(snapshot$registryError))
      notes <- c(notes, sprintf(
         "Could not read rig's registry values (%s); the default R rig records in the registry will not be put back.",
         snapshot$registryError
      ))

   if (!is.null(file))
   {
      failure <- .rs.rig.saveSnapshot(snapshot, file)
      if (length(failure))
         stop(failure)
   }

   state$snapshot <- snapshot
   state$file <- file
   notes
})

#' Called once rig is running, to record its process in the snapshot: a
#' later session leaves the snapshot alone while that rig is still running.
#' Returns messages for the job's log.
.rs.addFunction("rig.userInstallStarted", function(pid)
{
   state <- .rs.rig.installState
   if (is.null(state$snapshot))
      return(character())

   state$snapshot$rigPid <- as.integer(pid)
   if (is.null(state$file))
      return(character())

   .rs.rig.saveSnapshot(state$snapshot, state$file)
})

#' Called once 'rig add' has exited, to undo the default-R setup it added or
#' changed. Returns messages for the job's log.
.rs.addFunction("rig.userInstallEnd", function(launchctl = .rs.rig.launchctl)
{
   state <- .rs.rig.installState
   snapshot <- state$snapshot
   file <- state$file
   state$snapshot <- NULL
   state$file <- NULL

   if (is.null(snapshot))
      return(character())

   # the snapshot stays on disk while something is left to undo, for the
   # next session to try again
   result <- .rs.rig.userDefaultRestore(snapshot, launchctl)
   if (!is.null(file) && length(result$failed) == 0L)
      unlink(file)

   .rs.rig.restoreMessages(result)
})

#' Undo an install a previous session did not get to undo, from the snapshot
#' it left in 'file'. The file is left alone while that install may still be
#' under way, and kept while something is left to undo. Returns messages for
#' the log.
.rs.addFunction("rig.userInstallRecover", function(file,
                                                   running = .rs.rig.processRunning,
                                                   launchctl = .rs.rig.launchctl)
{
   if (!file.exists(file))
      return(character())

   snapshot <- .rs.tryCatch(readRDS(file))
   if (inherits(snapshot, "error"))
   {
      unlink(file)
      return(sprintf("Could not read rig's default R setup from %s: %s", file, conditionMessage(snapshot)))
   }

   if (.rs.rig.snapshotInUse(snapshot, running))
      return(character())

   result <- .rs.rig.userDefaultRestore(snapshot, launchctl)
   if (length(result$failed) == 0L)
      unlink(file)

   .rs.rig.restoreMessages(result)
})

#' Whether a process of this user with the given id is running.
.rs.addFunction("rig.processRunning", function(pid)
{
   pid <- as.integer(pid)

   # on Windows pskill() terminates the process whatever the signal, so the
   # task list is consulted instead
   if (.rs.platform.isWindows)
   {
      output <- .rs.tryCatch(suppressWarnings(system2(
         "tasklist",
         c("/FI", shQuote(sprintf("PID eq %i", pid)), "/NH", "/FO", "CSV"),
         stdout = TRUE,
         stderr = FALSE
      )))

      if (inherits(output, "error"))
         return(FALSE)

      return(any(grepl(sprintf(",\"%i\",", pid), output, fixed = TRUE)))
   }

   isTRUE(tools::pskill(pid, 0L))
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
         file.path(Sys.getenv("LOCALAPPDATA", unset = ""), "Programs", "R"),
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

#' The architectures a macOS R installation's libR.dylib was built for
#' ("arm64", "x86_64"), read from its Mach-O header; empty when unreadable.
.rs.addFunction("rInstallations.architectures", function(home)
{
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
#' natively here, when that is a choice: on Apple silicon Macs, where x86_64
#' builds also run (emulated). NULL otherwise.
.rs.addFunction("rInstallations.preferredArchitecture", function(machine = .rs.rig.machine())
{
   if (.rs.platform.isMacos && identical(machine, "arm64")) "arm64"
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
