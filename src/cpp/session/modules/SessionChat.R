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

# Helper function for evaluating code for the 'runCode' tool.
.rs.addFunction("chat.safeEval", function(expr, envir = globalenv())
{
   tryCatch(
      withVisible(eval(expr, envir = envir)),
      error = identity,
      interrupt = identity
   )
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

      # Read and encode the PNG file
      pngData <- readBin(tmpFile, "raw", file.info(tmpFile)$size)

      # Base64 encode using available package
      if (requireNamespace("base64enc", quietly = TRUE)) {
         encoded <- base64enc::base64encode(pngData)
      } else if (requireNamespace("jsonlite", quietly = TRUE)) {
         encoded <- jsonlite::base64_enc(pngData)
      } else {
         warning("Neither base64enc nor jsonlite available for plot encoding")
         return(NULL)
      }

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
