#
# NotebookAlternateEngines.R
#
# Copyright (C) 2020 by RStudio, PBC
#
# Unless you have received this program directly from RStudio pursuant
# to the terms of a commercial license agreement with RStudio, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

.rs.addFunction("isSystemInterpreter", function(engine)
{
   # check to see if a knitr engine is defined
   knitr <- asNamespace("knitr")
   engines <- knitr$knit_engines$get()
   if (engine %in% names(engines))
   {
      # knitr engine defined; check to see if the engine uses 'eng_interpeted'
      isInterpreted <-
         is.function(knitr$eng_interpreted) &&
         is.function(engines[[engine]]) &&
         identical(engines[[engine]], knitr$eng_interpreted)
      isInterpreted
   }
   else
   {
      # no knitr engine defined; assume that the user intended to use a system
      # interpreter
      TRUE
   }
})

.rs.addFunction("runUserDefinedEngine", function(engine, code, options)
{
   Encoding(code) <- "UTF-8" 
   
   # trim common indent (this ensures that indented chunks of code can be run)
   # https://github.com/rstudio/rstudio/issues/3731
   code <- .rs.trimCommonIndent(code)
   
   # if we're using the python engine, attempt to load reticulate (this
   # will load the reticulate knitr engine and set it as the default engine)
   useReticulate <-
      identical(engine, "python") &&
      !identical(getOption("python.reticulate"), FALSE)
   
   if (useReticulate) {
      # TODO: prompt user for installation of reticulate?
      requireNamespace("reticulate", quietly = TRUE)
   }
   
   # retrieve the engine
   knitrEngines <- knitr::knit_engines$get()
   if (!engine %in% names(knitrEngines))
   {
      fmt <- "engine '%s' has not yet been registered"
      stop(sprintf(fmt, options))
   }
   
   # double-check that this is indeed a function (shouldn't happen
   # but should guard against invalid user-defined engines)
   knitrEngine <- knitrEngines[[engine]]
   if (!is.function(knitrEngine))
   {
      fmt <- "engine '%s' is not a function"
      stop(sprintf(fmt, options))
   }
   
   # prepare the R environment for reticulate Python engine
   if (useReticulate)
   {
      # install our own matplotlib hook -- TODO here is to save the plot
      # object itself so we can redraw the plot if needed on resize
      show <- getOption("reticulate.engine.matplotlib.show")
      on.exit(options(reticulate.engine.matplotlib.show = show), add = TRUE)
      options(reticulate.engine.matplotlib.show = function(plt, options) {
         path <- tempfile("reticulate-matplotlib-plot-", fileext = ".png")
         plt$savefig(path, dpi = options$dpi)
         structure(list(path = path), class = "reticulate_matplotlib_plot")
      })
      
      # install our own wrap hook -- we want to avoid the post-processing
      # typically done by knitr; we implement our own wrap behavior for
      # notebooks
      wrap <- getOption("reticulate.engine.wrap")
      on.exit(options(reticulate.engine.wrap = wrap), add = TRUE)
      options(reticulate.engine.wrap = function(outputs, options) {
         
         # take this opportunity to clear matplotlib figure if appropriate
         sys <- reticulate::import("sys", convert = TRUE)
         if (!is.null(sys$modules$matplotlib$pyplot)) {
            plt <- reticulate::import("matplotlib.pyplot", convert = TRUE)
            tryCatch(plt$clf(), error = identity)
         }
         
         # return outputs
         outputs
         
      })
      
      # use the global environment for rendering
      environment <- getOption("reticulate.engine.environment")
      on.exit(options(reticulate.engine.environment = environment), add = TRUE)
      options(reticulate.engine.environment = globalenv())
   }
   
   # prepare chunk options
   mergedOptions <- knitr::opts_chunk$merge(options)
   code <- strsplit(code, "\n", fixed = TRUE)[[1]]
   mergedOptions$code <- code
   
   # when invoking engines, we don't want to echo user code
   mergedOptions$echo <- FALSE
   
   # invoke engine
   knitrEngine(mergedOptions)
   
})
