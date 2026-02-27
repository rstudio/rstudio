#
# SessionChat.R
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

# NOTE: The guardrails implemented here are intended to prevent accidental
# misuse and unintentionally destructive AI-generated code. They should
# not be relied upon to defend against deliberately malicious code.

# Each hooked namespace gets its own environment within this container,
# keyed by the namespace name. The per-namespace environment maps
# binding names to their original values.
.rs.setVar("chat.hookedBindings", new.env(parent = emptyenv()))
.rs.setVar("chat.bindingsInjected", FALSE)

# PCRE patterns matched against normalized paths to deny reads.
# Paths are always absolute and normalized before matching.
.rs.setVar("chat.denyReadPatterns", c(
   
   # Deny files that are likely to contain credentials
   "/\\.aws/credentials$",
   "/\\.aws/config$",
   "/\\.netrc$",
   "/\\.npmrc$",
   "/\\.ssh/config$",
   
   # Deny files like .env, .env.local, and so on.
   "/\\.env(\\.|$)",
   "/\\.Renviron(\\.|$)",
   "/\\.Rprofile(\\.|$)",
   
   # Deny access to non-public files within the .ssh directory.
   "/\\.ssh/id.*(?<!\\.pub)$"
   
))

# PCRE patterns matched against normalized paths to deny edits.
# Note that file edits are disallowed by default, except for files within
#
# - The R temporary directory
# - The project directory (when a project is open)
# - The current working directory
#
# This list serves to deny edits for certain files even if they're within
# one of the above 'allowed' directories.
.rs.setVar("chat.denyEditPatterns", c(
   
   # Deny edits on or within the .ssh directory.
   "/\\.ssh(/|$)"
   
))


#' Add a preflight validation hook to an existing function.
#'
#' Prepends the body of `hook` to the body of the target function,
#' so that the hook runs before the original implementation. The
#' original binding is saved for later restoration by
#' `.rs.chat.restoreBindings`.
#'
#' @param package The name of the package containing the binding.
#' @param binding The name of the function to hook.
#' @param hook A function whose body will be prepended to the original.
.rs.addFunction("chat.addPreflightHook", function(package, binding, hook)
{
   # resolve the namespace environment
   envir <- asNamespace(package)

   # skip bindings that don't exist (e.g. removed in a future R version)
   if (!exists(binding, envir = envir, inherits = FALSE))
   {
      warning(sprintf("binding '%s' not found in '%s'; skipping hook", binding, package))
      return(invisible(NULL))
   }

   # grab the original binding
   original <- envir[[binding]]

   # get or create the per-namespace storage for original bindings
   if (!exists(package, envir = .rs.chat.hookedBindings, inherits = FALSE))
      .rs.chat.hookedBindings[[package]] <- new.env(parent = emptyenv())
   .rs.chat.hookedBindings[[package]][[binding]] <- original

   # set hook environment and formals to match original (replaceBinding
   # will also set the environment, but we need it here so the merged
   # body can resolve symbols from the original namespace)
   environment(hook) <- environment(original)
   formals(hook) <- formals(original)

   # inject the body of our hook as a prefix to the original code
   body(hook) <- call("{", body(hook), body(original))

   # replace in both namespace and search path
   .rs.replaceBinding(binding, package, hook)

   # return old binding in case caller needs it
   invisible(original)
})

.rs.addFunction("chat.addPreflightHooks", function(package, hooks)
{
   if (!isNamespaceLoaded(package))
      return(invisible())

   .rs.enumerate(hooks, function(binding, hook)
   {
      .rs.chat.addPreflightHook(package, binding, hook)
   })
})

.rs.addFunction("chat.restoreBindings", function()
{
   packages <- ls(envir = .rs.chat.hookedBindings, all.names = TRUE)
   allRestored <- TRUE

   for (package in packages)
   {
      originals <- .rs.chat.hookedBindings[[package]]
      bindings <- ls(envir = originals, all.names = TRUE)

      for (binding in bindings)
      {
         status <- .rs.tryCatch(.rs.replaceBinding(binding, package, originals[[binding]]))
         if (inherits(status, "error"))
         {
            warning(sprintf(
               "failed to restore binding '%s' in '%s': %s",
               binding, package, conditionMessage(status)
            ))
            allRestored <- FALSE
         }
         else
         {
            rm(list = binding, envir = originals)
         }
      }

      # only remove the package entry if all bindings were restored
      if (length(ls(envir = originals, all.names = TRUE)) == 0L)
         rm(list = package, envir = .rs.chat.hookedBindings)
   }

   if (allRestored)
      .rs.setVar("chat.bindingsInjected", FALSE)
})

#' Match paths against a pattern.
#'
#' Wrapper around grepl that uses case-insensitive matching on
#' platforms with case-insensitive filesystems (macOS, Windows).
#'
#' @param pattern A PCRE regular expression.
#' @param x A character vector of file paths.
#' @param ... Additional arguments passed to `grepl()`.
.rs.addFunction("chat.pathMatches", function(pattern, x, ...)
{
   grepl(pattern, x, ignore.case = !.rs.platform.isLinux, perl = TRUE, ...)
})

#' Check whether a path lies within a directory.
#'
#' Both `path` and `directory` should be normalized before calling
#' this function.
#'
#' @param path A character vector of file paths.
#' @param directory A single directory path.
.rs.addFunction("chat.isPathWithin", function(path, directory)
{
   pattern <- paste0("^\\Q", directory, "\\E(/|$)")
   .rs.chat.pathMatches(pattern, path)
})

#' Normalize file paths for use in guardrail path comparisons.
#'
#' For existing paths, delegates to `normalizePath()`. For non-existing
#' paths, normalizes the dirname and appends the basename. Rejects paths
#' containing unresolved '..' components to prevent path traversal.
#'
#' @param path A character vector of file paths.
.rs.addFunction("chat.normalizePath", function(path)
{
   exists <- file.exists(path)

   path[exists] <- normalizePath(path[exists], winslash = "/", mustWork = TRUE)

   path[!exists] <- file.path(
      normalizePath(dirname(path[!exists]), winslash = "/", mustWork = FALSE),
      basename(path[!exists])
   )

   # Reject paths that still contain '..' after normalization, since
   # normalizePath(mustWork = FALSE) won't resolve '..' when the parent
   # directory doesn't exist, which could allow path traversal.
   unresolved <- grepl("(?:^|/)\\.\\.(?:/|$)", path)
   if (any(unresolved))
      stop(sprintf(
         "path contains unresolved '..' components: %s",
         paste(path[unresolved], collapse = ", ")
      ))

   path
})

#' Check whether reading the given paths is allowed.
#'
#' Reads are allowed by default, but denied for files that lack
#' world-readable permissions, match well-known sensitive path
#' patterns (e.g. `~/.aws/credentials`, `.env`, `.Renviron`).
#'
#' @param path A character vector of file paths.
#' @return A logical vector the same length as `path`.
.rs.addFunction("chat.isFileReadAllowed", function(path)
{
   # normalize path for comparison
   path <- .rs.chat.normalizePath(path)

   # assume file reads are permitted by default
   ok <- rep.int(TRUE, length(path))

   # deny reads on files that lack read permission for 'others'
   # (use which() to drop NA modes from non-existent files)
   info <- suppressWarnings(file.info(path))
   deny <- bitwAnd(info$mode, 4L) == 0L
   ok[which(deny)] <- FALSE

   # deny reads matching sensitive path patterns
   pattern <- paste(.rs.chat.denyReadPatterns, collapse = "|")
   ok[.rs.chat.pathMatches(pattern, path)] <- FALSE

   ok
})

#' Check whether editing the given paths is allowed.
#'
#' Edits are denied by default, but allowed within the R temporary
#' directory, the active project directory, and the current working
#' directory. Edits within sensitive directories (e.g. `~/.ssh`)
#' are always denied.
#'
#' @param path A character vector of file paths.
#' @return A logical vector the same length as `path`.
.rs.addFunction("chat.isFileEditAllowed", function(path)
{
   # normalize path for comparison
   path <- .rs.chat.normalizePath(path)

   # assume file edits are disallowed by default
   ok <- rep.int(FALSE, length(path))

   # allow edits within the R temporary directory
   tempDir <- normalizePath(tempdir(), winslash = "/", mustWork = TRUE)
   ok[.rs.chat.isPathWithin(path, tempDir)] <- TRUE

   # allow edits within the project directory
   projectDir <- .rs.getProjectDirectory()
   if (!is.null(projectDir))
   {
      projectDir <- normalizePath(projectDir, winslash = "/", mustWork = TRUE)
      ok[.rs.chat.isPathWithin(path, projectDir)] <- TRUE
   }

   # allow edits within the current working directory
   workingDir <- normalizePath(getwd(), winslash = "/", mustWork = TRUE)
   ok[.rs.chat.isPathWithin(path, workingDir)] <- TRUE

   # deny edits matching sensitive path patterns (both read and edit
   # deny lists apply, since edits should be at least as restrictive)
   pattern <- paste(c(.rs.chat.denyReadPatterns, .rs.chat.denyEditPatterns), collapse = "|")
   ok[.rs.chat.pathMatches(pattern, path)] <- FALSE

   ok
})

.rs.addFunction("chat.validateFileEdit", function(action, path)
{
   ok <- .rs.chat.isFileEditAllowed(path)
   if (all(ok))
      return(TRUE)

   fmt <- "denied %s() on file %s"
   msg <- sprintf(fmt, action, paste(shQuote(path[!ok]), collapse = ", "))
   stop(msg, call. = FALSE)
})

.rs.addFunction("chat.validateFileRead", function(action, path)
{
   ok <- .rs.chat.isFileReadAllowed(path)
   if (all(ok))
      return(TRUE)

   fmt <- "denied %s() on file %s"
   msg <- sprintf(fmt, action, paste(shQuote(path[!ok]), collapse = ", "))
   stop(msg, call. = FALSE)
})

#' Validate a connection open based on the open mode.
#'
#' When `open` is "" (deferred), the connection could later be used for
#' reading or writing, so it is validated as an edit (which applies both
#' the read and edit deny lists).
#'
#' @param name The name of the connection function (e.g. "file", "gzfile").
#' @param description The file path passed to the connection constructor.
#' @param open The open mode string.
.rs.addFunction("chat.validateConnection", function(name, description, open)
{
   if (!nzchar(description))
      return()

   if (!nzchar(open) || grepl("[wWaA+]", open))
   {
      .rs.chat.validateFileEdit(name, description)
   }
   else
   {
      .rs.chat.validateFileRead(name, description)
   }
})

# inject preflight validation hooks into security-sensitive R
# functions. Each hook is prepended to the original function body via
# .rs.chat.addPreflightHook so that calls are validated before the real
# implementation executes. hooks are removed by .rs.chat.restoreBindings.
.rs.addFunction("chat.injectBindings", function()
{
   # guard against reentrant calls -- if hooks are already injected,
   # skip injection to avoid overwriting saved originals
   if (.rs.chat.bindingsInjected)
      return(invisible())

   baseHooks <- list(

      unlink = function()
      {
         # Block recursive deletes in the user's home directory
         if ("*" %in% x)
         {
            workDir <- normalizePath(getwd(), winslash = "/", mustWork = TRUE)
            homeDir <- normalizePath("~", winslash = "/", mustWork = TRUE)
            if (identical(workDir, homeDir))
            {
               msg <- "denied unlink(\"*\") on user home directory"
               stop(msg, call. = FALSE)
            }
         }
         
         # The 'expand' formal was added in R 4.0.0; on older versions,
         # unlink always performed glob expansion.
         doExpand <- getRversion() < "4.0.0" || expand
         paths <- if (doExpand) Sys.glob(x) else x
         .rs.chat.validateFileEdit("unlink", paths)
      },

      file.create = function()
      {
         .rs.chat.validateFileEdit("file.create", c(...))
      },

      file.remove = function()
      {
         .rs.chat.validateFileEdit("file.remove", c(...))
      },

      file.rename = function()
      {
         .rs.chat.validateFileEdit("file.rename", c(from, to))
      },

      file.append = function()
      {
         .rs.chat.validateFileEdit("file.append", file1)
         .rs.chat.validateFileRead("file.append", file2)
      },

      file.copy = function()
      {
         .rs.chat.validateFileRead("file.copy", from)
         .rs.chat.validateFileEdit("file.copy", to)
      },

      file.symlink = function()
      {
         .rs.chat.validateFileRead("file.symlink", from)
         .rs.chat.validateFileEdit("file.symlink", to)
      },

      file.link = function()
      {
         .rs.chat.validateFileRead("file.link", from)
         .rs.chat.validateFileEdit("file.link", to)
      },

      file = function()
      {
         .rs.chat.validateConnection("file", description, open)
      },

      gzfile = function()
      {
         .rs.chat.validateConnection("gzfile", description, open)
      },

      bzfile = function()
      {
         .rs.chat.validateConnection("bzfile", description, open)
      },

      xzfile = function()
      {
         .rs.chat.validateConnection("xzfile", description, open)
      },

      readLines = function()
      {
         if (is.character(con) && nzchar(con))
            .rs.chat.validateFileRead("readLines", con)
      },
      
      writeLines = function()
      {
         if (is.character(con) && nzchar(con))
            .rs.chat.validateFileEdit("writeLines", con)
      },

      cat = function()
      {
         if (is.character(file) && nzchar(file))
            .rs.chat.validateFileEdit("cat", file)
      },

      readChar = function()
      {
         if (is.character(con) && nzchar(con))
            .rs.chat.validateFileRead("readChar", con)
      },

      writeChar = function()
      {
         if (is.character(con) && nzchar(con))
            .rs.chat.validateFileEdit("writeChar", con)
      },

      readBin = function()
      {
         if (is.character(con) && nzchar(con))
            .rs.chat.validateFileRead("readBin", con)
      },

      writeBin = function()
      {
         if (is.character(con) && nzchar(con))
            .rs.chat.validateFileEdit("writeBin", con)
      },

      save = function()
      {
         if (is.character(file) && nzchar(file))
            .rs.chat.validateFileEdit("save", file)
      },

      load = function()
      {
         if (is.character(file) && nzchar(file))
            .rs.chat.validateFileRead("load", file)
      },

      source = function()
      {
         if (is.character(file) && nzchar(file))
            .rs.chat.validateFileRead("source", file)
      },

      sys.source = function()
      {
         if (is.character(file) && nzchar(file))
            .rs.chat.validateFileRead("sys.source", file)
      },

      readRDS = function()
      {
         if (is.character(file) && nzchar(file))
            .rs.chat.validateFileRead("readRDS", file)
      },

      saveRDS = function()
      {
         if (is.character(file) && nzchar(file))
            .rs.chat.validateFileEdit("saveRDS", file)
      },

      dput = function()
      {
         if (is.character(file) && nzchar(file))
            .rs.chat.validateFileEdit("dput", file)
      },

      dump = function()
      {
         if (is.character(file) && nzchar(file))
            .rs.chat.validateFileEdit("dump", file)
      },

      dir.create = function()
      {
         .rs.chat.validateFileEdit("dir.create", path)
      },

      Sys.chmod = function()
      {
         .rs.chat.validateFileEdit("Sys.chmod", paths)
      },

      Sys.setFileTime = function()
      {
         .rs.chat.validateFileEdit("Sys.setFileTime", path)
      },

      sink = function()
      {
         if (is.character(file) && nzchar(file))
            .rs.chat.validateFileEdit("sink", file)
      }

   )

   .rs.chat.addPreflightHooks("base", baseHooks)

   utilsHooks <- list(

      download.file = function()
      {
         .rs.chat.validateFileEdit("download.file", destfile)
      },

      write.table = function()
      {
         if (is.character(file) && nzchar(file))
            .rs.chat.validateFileEdit("write.table", file)
      }

   )

   .rs.chat.addPreflightHooks("utils", utilsHooks)

   fsHooks <- list(

      file_create = function()
      {
         .rs.chat.validateFileEdit("file_create", path)
      },

      file_delete = function()
      {
         .rs.chat.validateFileEdit("file_delete", path)
      },

      file_copy = function()
      {
         .rs.chat.validateFileRead("file_copy", path)
         .rs.chat.validateFileEdit("file_copy", new_path)
      },

      file_move = function()
      {
         .rs.chat.validateFileEdit("file_move", c(path, new_path))
      },

      file_chmod = function()
      {
         .rs.chat.validateFileEdit("file_chmod", path)
      },

      file_chown = function()
      {
         .rs.chat.validateFileEdit("file_chown", path)
      },

      file_touch = function()
      {
         .rs.chat.validateFileEdit("file_touch", path)
      },

      file_show = function()
      {
         .rs.chat.validateFileRead("file_show", path)
      }

   )

   .rs.chat.addPreflightHooks("fs", fsHooks)

   .rs.setVar("chat.bindingsInjected", TRUE)
   invisible(TRUE)
})

# Helper function for evaluating code for the 'runCode' tool.
.rs.addFunction("chat.safeEval", function(expr, envir = globalenv())
{
   tryCatch(error = identity, interrupt = identity, {

      # Register cleanup first so bindings are restored even if
      # injectBindings() itself errors partway through
      on.exit({
         tryCatch(
            .rs.chat.restoreBindings(),
            error = function(e) {
               warning("failed to restore bindings: ", conditionMessage(e))
            }
         )
      }, add = TRUE)
      .rs.chat.injectBindings()
      
      # Evaluate the provided code
      withVisible(eval(expr, envir = envir))

   })
})

# Helper function to capture a recorded plot as base64-encoded PNG
.rs.addFunction("chat.capturePlotFromRecorded", function(recordedPlot)
{
   # Get plot dimensions from options or use defaults
   # Default: 7x7 inches at 96 DPI (R's standard default)
   width <- getOption("repr.plot.width", 7)
   height <- getOption("repr.plot.height", 7)
   dpi <- 96

   # Calculate pixel dimensions
   widthPx <- as.integer(width * dpi)
   heightPx <- as.integer(height * dpi)

   # Create temporary file for PNG output
   tmpFile <- tempfile(fileext = ".png")
   on.exit(unlink(tmpFile), add = TRUE)

   # Open PNG device and replay plot
   tryCatch({
      png(tmpFile, width = widthPx, height = heightPx, res = dpi)
      replayPlot(recordedPlot)
      dev.off()

      # Base64 encode the PNG file using built-in C++ implementation
      encoded <- .rs.base64encodeFile(tmpFile)

      list(
         data = encoded,
         mimeType = "image/png",
         width = widthPx,
         height = heightPx
      )
   }, error = function(e) {
      # Ensure device is closed on error
      if (dev.cur() > 1) dev.off()
      warning(paste("Failed to capture plot:", conditionMessage(e)))
      NULL
   })
})

# Get the current recorded plot (used to detect if plotting occurred)
.rs.addFunction("chat.getRecordedPlot", function()
{
   if (dev.cur() <= 1)
      return(NULL)

   tryCatch({
      recordPlot()
   }, error = function(e) {
      NULL
   })
})

# Capture the current plot, but only if plotting occurred since the given
# recorded plot snapshot (to avoid returning stale plots from previous executions).
#
# NOTE: This only captures the final plot state. If code creates multiple plots
# (e.g., plot(1); plot(2)), only the last one is captured. This is a known
# limitation compared to the previous evaluate-based approach.
#
# Returns NULL if no NEW plot is available, otherwise returns a list with:
#   - data: base64-encoded PNG
#   - mimeType: "image/png"
#   - width: pixel width
#   - height: pixel height
.rs.addFunction("chat.captureCurrentPlot", function(plotBefore = NULL)
{
   # No graphics device open
   if (dev.cur() <= 1)
      return(NULL)

   # Try to record the current plot
   recordedPlot <- tryCatch({
      recorded <- recordPlot()
      # Check if the display list is non-empty
      if (is.null(recorded) || length(recorded[[1]]) == 0)
         return(NULL)
      recorded
   }, error = function(e) {
      NULL
   })

   if (is.null(recordedPlot))
      return(NULL)

   # Check if the plot changed since before execution
   # If the plots are identical, this is a stale plot from a previous execution
   if (!is.null(plotBefore)) {
      # Compare the plot objects to see if plotting actually occurred
      if (identical(recordedPlot, plotBefore))
         return(NULL)
   }

   # Use the helper to capture and encode the plot
   plotData <- .rs.chat.capturePlotFromRecorded(recordedPlot)

   if (!is.null(plotData)) {
      # Also replay the plot to the current RStudio device so it appears in the plots pane
      tryCatch({
         replayPlot(recordedPlot)
      }, error = function(e) {
         # Ignore errors in replay - the plot was already captured successfully
      })
   }

   plotData
})
