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

.rs.setVar("chat.hookedBindings", new.env(parent = emptyenv()))

# Well-known files that commonly contain secrets but are often
# world-readable. Checked by full path (resolved against the user's
# home directory) or by basename pattern.
.rs.setVar("chat.denyPaths", c(
   "~/.aws/credentials",
   "~/.aws/config",
   "~/.netrc",
   "~/.npmrc",
   "~/.ssh/config"
))

.rs.setVar("chat.denyBasenames", c(
   ".env",
   ".Renviron"
))


.rs.addFunction("chat.addPreflightHook", function(binding, hook)
{
   # grab the original binding from the base namespace
   envir <- .BaseNamespaceEnv
   original <- envir[[binding]]
   
   # keep around a reference to the old binding
   .rs.chat.hookedBindings[[binding]] <- original
   
   # force our hook to use the same environment, formals as original
   environment(hook) <- environment(original)
   formals(hook) <- formals(original)
   
   # inject the body of our hook as a prefix to the original code
   body(hook) <- call("{", body(hook), body(original))
   
   # inject our hook into the associated environment
   .rs.replaceBindingImpl(envir, binding, hook)
   
   # return old binding in case caller needs it
   invisible(original)
})

.rs.addFunction("chat.addPreflightHooks", function(hooks)
{
   .rs.enumerate(hooks, function(binding, hook)
   {
      .rs.chat.addPreflightHook(binding, hook)
   })
})

.rs.addFunction("chat.restoreBindings", function()
{
   envir <- .BaseNamespaceEnv
   bindings <- ls(envir = .rs.chat.hookedBindings, all.names = TRUE)
   
   for (binding in bindings)
   {
      value <- .rs.chat.hookedBindings[[binding]]
      .rs.replaceBindingImpl(envir, binding, value)
      rm(list = binding, envir = .rs.chat.hookedBindings)
   }
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
   .rs.startsWith(path, paste0(directory, "/"))
})

.rs.addFunction("chat.normalizePath", function(path)
{
   exists <- file.exists(path)
   
   path[exists] <- normalizePath(path[exists], winslash = "/", mustWork = TRUE)
   
   path[!exists] <- file.path(
      normalizePath(dirname(path[!exists]), winslash = "/", mustWork = FALSE),
      basename(path[!exists])
   )
   
   path
})

.rs.addFunction("chat.isFileReadAllowed", function(path)
{
   # normalize path for comparison
   path <- .rs.chat.normalizePath(path)

   # assume file reads are permitted by default
   ok <- rep.int(TRUE, length(path))

   # deny reads on files that lack read permission for 'others'
   info <- suppressWarnings(file.info(path))
   deny <- bitwAnd(info$mode, 4L) == 0L
   ok[which(deny)] <- FALSE

   # deny reads on well-known sensitive paths
   denyPaths <- normalizePath(
      path.expand(.rs.chat.denyPaths),
      winslash = "/",
      mustWork = FALSE
   )
   ok[path %in% denyPaths] <- FALSE

   # deny reads on files matching sensitive basename patterns
   bn <- basename(path)
   for (pattern in .rs.chat.denyBasenames)
      ok[.rs.startsWith(bn, pattern)] <- FALSE

   ok
})

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

# inject preflight validation hooks into potentially destructive base R
# functions. Each hook is prepended to the original function body via
# .rs.chat.addPreflightHook so that calls are validated before the real
# implementation executes. hooks are removed by .rs.chat.restoreBindings.
.rs.addFunction("chat.injectBindings", function()
{
   hooks <- list(

      unlink = function()
      {
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
         if (nzchar(description))
         {
            if (grepl("[wWaA+]", open))
               .rs.chat.validateFileEdit("file", description)
            else
               .rs.chat.validateFileRead("file", description)
         }
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
         .rs.chat.validateFileEdit("save", file)
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

      download.file = function()
      {
         .rs.chat.validateFileEdit("download.file", destfile)
      },

      sink = function()
      {
         if (is.character(file) && nzchar(file))
            .rs.chat.validateFileEdit("sink", file)
      }

   )

   .rs.chat.addPreflightHooks(hooks)
   
})

# Helper function for evaluating code for the 'runCode' tool.
.rs.addFunction("chat.safeEval", function(expr, envir = globalenv())
{
   tryCatch(error = identity, interrupt = identity, {

      # Register cleanup first so bindings are restored even if
      # injectBindings() itself errors partway through
      on.exit(.rs.chat.restoreBindings(), add = TRUE)
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
