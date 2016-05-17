#
# NotebookHtmlWidgets.R
#
# Copyright (C) 2009-16 by RStudio, Inc.
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

.rs.addFunction("initHtmlCapture", function(outputFolder, libraryFolder)
{
   assign("print.htmlwidget", function(x, ...) {
      
      if (!requireNamespace("htmlwidgets", quietly = TRUE))
         stop("print.htmlwidget called without 'htmlwidgets' available")
      
      htmlfile <- tempfile(pattern = "_rs_html_",
                           tmpdir = outputFolder, 
                           fileext = ".html")
      
      # extract dependencies (for our own accounting)
      dependencies <- c(
         htmlwidgets:::widget_dependencies(class(x)[1], attr(x, "package")), 
         x$dependencies
      )
      
      # write them to JSON 
      depfile <- tempfile(pattern = "_rs_html_deps_",
                          tmpdir = outputFolder,
                          fileext = ".json")
      
      cat(.rs.toJSON(dependencies, unbox = TRUE), file = depfile, sep = "\n")
      
      # collect knitr options
      # TODO: parse chunk options and use here?
      knitrOptions <- knitr::opts_chunk$get()
      
      # save as HTML -- save a modified version of the 'standalone' representation
      # that works effectively the same way as the 'embedded' representation;
      # we use a filter on the server side to disentangle things as needed
      html <- htmlwidgets:::toHTML(x, standalone = TRUE, knitrOptions = knitrOptions)
      
      n <- length(html)
      sizingPolicy <- html[[n]]
      encodedPolicy <- .rs.base64encode(as.character(sizingPolicy))
      
      # NOTE: if you touch this make sure you update the parsing code in NotebookOutput.cpp
      fmt <- "<!-- htmlwidget-sizing-policy-base64 %s -->"
      html[[n]] <- htmltools::HTML(sprintf(fmt, encodedPolicy))
      
      # write html
      htmltools::save_html(html, file = htmlfile, libdir = libraryFolder)
      
      # record the saved artefacts
      .Call("rs_recordHtmlWidget", htmlfile, depfile)
      
   }, envir = as.environment("tools:rstudio"))
})

.rs.addFunction("releaseHtmlCapture", function()
{
   rm("print.htmlwidget", envir = as.environment("tools:rstudio"))
})
