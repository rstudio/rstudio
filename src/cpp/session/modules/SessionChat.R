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

# Main function to execute code and capture output
.rs.addFunction("chat.executeCode", function(code,
                                              capturePlot = FALSE,
                                              timeout = 30000)
{
   # Check for required package
   if (!requireNamespace("evaluate", quietly = TRUE)) {
      return(list(
         items = list(list(
            type = "error",
            content = "The 'evaluate' package is required for code execution. Please install it with: install.packages('evaluate')"
         )),
         plots = list(),
         executionTime = 0
      ))
   }

   # Initialize result structure
   result <- list(
      items = list(),      # List of output items (source, output, message, warning, error)
      plots = list(),      # Captured plots
      executionTime = 0
   )

   # Record start time
   startTime <- Sys.time()

   # Plot index for capturing multiple plots
   plotIndex <- 1

   tryCatch({
      # Use evaluate package for proper output capture
      evalResult <- evaluate::evaluate(
         code,
         envir = globalenv(),
         new_device = capturePlot,
         stop_on_error = 0,  # Continue through errors to capture all output
         output_handler = evaluate::new_output_handler(
            # Use default handlers - we'll process the result list
         )
      )

      # Process each item from evaluate's result
      for (item in evalResult) {
         if (inherits(item, "source")) {
            # Source code - format with prompts
            srcLines <- strsplit(item$src, "\n", fixed = TRUE)[[1]]
            # Remove trailing empty line if present (from trailing newline)
            if (length(srcLines) > 0 && srcLines[length(srcLines)] == "") {
               srcLines <- srcLines[-length(srcLines)]
            }
            formattedLines <- character()
            for (i in seq_along(srcLines)) {
               # Use "> " for first line, "+ " for continuation lines
               prompt <- if (i == 1) "> " else "+ "
               formattedLines <- c(formattedLines, paste0(prompt, srcLines[i]))
            }
            result$items[[length(result$items) + 1]] <- list(
               type = "source",
               content = paste(formattedLines, collapse = "\n")
            )
         } else if (is.character(item)) {
            # Standard output
            result$items[[length(result$items) + 1]] <- list(
               type = "output",
               content = item
            )
         } else if (evaluate::is.recordedplot(item) && capturePlot) {
            # Capture plot to PNG and base64 encode
            plotData <- .rs.chat.capturePlotFromRecorded(item)
            if (!is.null(plotData)) {
               result$plots[[plotIndex]] <- plotData
               plotIndex <- plotIndex + 1
            }

            # Also replay the plot to the current RStudio device so it appears in the plots pane
            tryCatch({
               replayPlot(item)
            }, error = function(e) {
               # Ignore errors in replay - the plot was already captured successfully
            })
         } else if (inherits(item, "message")) {
            # Message condition
            result$items[[length(result$items) + 1]] <- list(
               type = "message",
               content = conditionMessage(item)
            )
         } else if (inherits(item, "warning")) {
            # Warning condition
            result$items[[length(result$items) + 1]] <- list(
               type = "warning",
               content = paste0("Warning message:\n", conditionMessage(item))
            )
         } else if (inherits(item, "error")) {
            # Error condition
            result$items[[length(result$items) + 1]] <- list(
               type = "error",
               content = conditionMessage(item)
            )
         }
      }

   }, error = function(e) {
      # Catch any errors in the evaluation infrastructure itself
      result$items[[length(result$items) + 1]] <<- list(
         type = "error",
         content = conditionMessage(e)
      )
   })

   # Calculate execution time in milliseconds
   endTime <- Sys.time()
   result$executionTime <- as.integer(
      difftime(endTime, startTime, units = "secs") * 1000
   )

   result
})
