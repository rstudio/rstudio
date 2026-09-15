#
# NotebookPlots.R
#
# Copyright (C) 2022 by Posit Software, PBC
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

.rs.setVar("notebooks.defaultPlotDpi", 96)

# resolves the arguments used to create the notebook graphics device. 'extraArgs'
# carries the RStudio graphics preferences (backend type, antialiasing) as a
# string of R arguments; the knitr 'dev.args' chunk option ('devArgs'), when
# set, is layered on top so that inline chunk output matches a knit.
#
# https://github.com/rstudio/rstudio/issues/4067
.rs.addFunction("notebookGraphicsDeviceArgs", function(args,
                                                       extraArgs,
                                                       dev = "png",
                                                       devArgs = list())
{
   if (nchar(extraArgs) > 0)
   {
      # trim leading comma from extra args if present
      if (identical(substr(extraArgs, 1, 1), ","))
         extraArgs <- substring(extraArgs, 2)
      
      # parse extra args and merge with existing args
      extraList <- tryCatch(
         eval(parse(text = paste("list(", extraArgs, ")"))),
         error = function(e) NULL
      )
      
      if (is.list(extraList))
         args <- c(args, extraList)
   }

   # the option is unset in the plot replay process, where the backend type
   # arrives via extraArgs instead and must not be cleared
   gdBackend <- getOption("RStudioGD.backend")
   if (is.character(gdBackend) && !identical(gdBackend, "default"))
   {
     # pass along graphics device backend if set
     # we allow this option to be temporarily set by the knitr chunk option while the notebook
     # chunk is executing; this takes precedence over the device passed via extraArgs
     args$type <- gdBackend
   }

   # knitr allows several devices (dev = c("png", "pdf")); inline output
   # only renders the first
   dev <- as.character(unlist(dev))[1L]
   if (is.na(dev) || !nzchar(dev))
      dev <- "png"

   # knitr accepts 'dev.args' either as a flat list of arguments, or as a
   # list of per-device argument lists keyed by device name
   if (is.list(devArgs) && is.list(devArgs[[dev]]))
      devArgs <- devArgs[[dev]]

   # chunk-level arguments win over the RStudio defaults
   if (is.list(devArgs) && !is.null(names(devArgs)))
      args <- utils::modifyList(args, devArgs)

   args
})

# creates the notebook graphics device 
.rs.addFunction("createNotebookGraphicsDevice", function(filename,
                                                         width,
                                                         height,
                                                         dpi,
                                                         units,
                                                         pixelRatio,
                                                         extraArgs,
                                                         dev = "png",
                                                         devArgs = list())
{
   dpi <- if (dpi <= 0) .rs.notebooks.defaultPlotDpi else dpi
   
   if (units == "px") # px = automatic size behavior
   {
      height <- height * pixelRatio
      width <- width * pixelRatio
   }
   dpi <- dpi * pixelRatio

   # form the arguments to the graphics device creator
   args <- list(
      filename = filename,
      height   = height, 
      width    = width,
      units    = units,
      res      = dpi
   )

   args <- .rs.notebookGraphicsDeviceArgs(args, extraArgs, dev, devArgs)
   
   # if it looks like we're using AGG, delegate to that
   if (identical(args$type, "ragg"))
   {
      device <- ragg::agg_png(
         filename = filename,
         width    = width,
         height   = height,
         units    = units,
         res      = dpi
      )
      
      return(device)
   }
   
   # create the device
   require(grDevices, quietly = TRUE)
   do.call(what = png, args = args)
})

# this seems like it is the only thing manipulated from NotebookPlots.cpp side
# this is where pixelRatio is introduced to the machinery
# it likely originates in RClientMetrics.cpp where it is drawn from
# "r.session.client_metrics.device-pixel-ratio"
.rs.addFunction("setNotebookGraphicsOption", function(filename,
                                                      width,
                                                      height,
                                                      dpi,
                                                      units,
                                                      pixelRatio,
                                                      extraArgs,
                                                      dev = "png",
                                                      devArgs = list())
{
   options(device = function()
   {
      .rs.createNotebookGraphicsDevice(filename, width, height, dpi, units, pixelRatio, extraArgs, dev, devArgs)
      dev.control(displaylist = "enable")
      # this introduces margins that makes the figure different from the actual output
      # as seen in the rendered document, so disable it
      # .rs.setNotebookMargins()
   })
})

.rs.addFunction("saveNotebookGraphics", function(plot, filename)
{
   # ensure output directory exists; it may have been moved if the notebook
   # was saved during chunk execution
   # https://github.com/rstudio/rstudio/issues/6260
   dir.create(dirname(filename), recursive = TRUE, showWarnings = FALSE)
   save(plot, file = filename)
})

# this should not be used in my opinion (it is never called with
# the change of setNotebookGraphicsOption above)
.rs.addFunction("setNotebookMargins", function() {
   #           bot  left top  right
   par(mar = c(5.1, 4.1, 2.1, 2.1))
})

.rs.addFunction("replayNotebookPlotsPackages", function()
{
   grid <- asNamespace("grid")
   names <- vapply(grid$.__S3MethodsTable__., function(method) {
      environmentName(environment(method))
   }, FUN.VALUE = character(1))
   sort(unique(names))
})

.rs.addFunction("replayNotebookPlots", function(chunkDefsPath,
                                                width,
                                                height,
                                                pixelRatio,
                                                persistOutput,
                                                extraArgs)
{
   require(grDevices, quietly = TRUE)
   
   # Load any required packages
   requiredPackages <- Sys.getenv("RS_NOTEBOOK_PACKAGES", unset = "")
   requiredPackages <- strsplit(requiredPackages, ",", fixed = TRUE)[[1L]]
   
   suppressPackageStartupMessages({
      for (package in requiredPackages) {
         require(package, character.only = TRUE, quietly = TRUE)
      }
   })
   
   # Read the chunk definitions
   chunkDefs <- jsonlite::read_json(chunkDefsPath)
   
   # Read the chunk ids we'll use for this render
   stdin <- file("stdin")
   chunkIds <- readLines(stdin, warn = FALSE)
   
   for (chunkId in chunkIds) {
      
      chunkDef <- chunkDefs[[chunkId]]
      if (is.null(chunkDef))
         next
      
      snapshots <- chunkDef[["snapshot_files"]]
      if (length(snapshots) == 0)
         next
      
      height <- if (height <= 0) width / 1.618 else height
      dpi <- .rs.nullCoalesce(chunkDef$options$dpi, .rs.notebooks.defaultPlotDpi)

      # use [[ rather than $ so 'dev' can't partially match 'dev.args'
      .rs.replayNotebookSnapshots(
         snapshots = snapshots,
         width = width,
         height = height,
         dpi = dpi,
         pixelRatio = pixelRatio,
         persistOutput = persistOutput,
         extraArgs = extraArgs,
         dev = .rs.nullCoalesce(chunkDef$options[["dev"]], "png"),
         devArgs = .rs.nullCoalesce(chunkDef$options[["dev.args"]], list())
      )
      
   }
   
})

.rs.addFunction("replayNotebookSnapshots", function(snapshots,
                                                    width,
                                                    height,
                                                    dpi,
                                                    pixelRatio,
                                                    persistOutput,
                                                    extraArgs,
                                                    dev = "png",
                                                    devArgs = list())
{
   lapply(snapshots, function(snapshot) {
      
      # ignore empty lines
      if (nchar(tools::file_ext(snapshot)) < 1)
         return(invisible(NULL))
      
      # create the PNG device on which we'll regenerate the plot -- it's somewhat
      # wasteful to use a separate device per plot, but the alternative is
      # doing a lot of state management and post-processing of the files
      # output from the device
      output <- paste(
         tools::file_path_sans_ext(snapshot),
         "resized.png",
         sep = "."
      )
      
      .rs.createNotebookGraphicsDevice(
         filename = output,
         width = width,
         height = height,
         dpi = dpi,
         units = "px",
         pixelRatio = pixelRatio,
         extraArgs = extraArgs,
         dev = dev,
         devArgs = devArgs
      )
      
      # actually replay the plot onto the device
      tryCatch(
         .rs.restoreGraphics(snapshot),
         error = function(err) {
            writeLines(conditionMessage(err), con = stderr())
         }
      )
      
      # close the device; has the side effect of committing the plot to disk
      dev.off()
      
      # if the plot file was written out, emit to standard out for caller
      if (!file.exists(output))
         return(invisible(NULL))
      
      # build output path
      final <- paste(tools::file_path_sans_ext(snapshot), "png", sep = ".")
      if (!persistOutput) {
         
         chunksPath <- dirname(snapshot)
         chunksTempPath <- file.path(chunksPath, "temp")
         
         if (!dir.exists(chunksTempPath))
            dir.create(chunksTempPath)
         
         chunkBaseName <- basename(tools::file_path_sans_ext(snapshot))
         
         final <- file.path(
            chunksTempPath,
            paste(chunkBaseName, "png", sep = ".")
         )
      }
      
      # remove the old copy of the plot if it existed
      file.copy(output, final, overwrite = TRUE)
      unlink(output)
      
      if (file.exists(final))
         writeLines(final)
      
   })
})
