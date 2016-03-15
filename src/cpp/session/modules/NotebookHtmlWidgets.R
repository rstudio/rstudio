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
    if (require(htmlwidgets, quietly = TRUE)) {
      htmlfile <- tempfile(pattern = "_rs_html_", tmpdir = outputFolder, 
                         fileext = ".html")

      # extract dependencies (for our own accounting)
      dependencies <- c(
        htmlwidgets:::widget_dependencies(class(x)[1], attr(x, "package")), 
        x$dependencies)

      # write them to JSON 
      depfile <- tempfile(pattern = "_rs_html_deps_", tmpdir = outputFolder,
                          fileext = ".json")
      writeLines(jsonlite::toJSON(lapply(dependencies, unclass), 
                                  auto_unbox = TRUE),
                 depfile)
                  
      # save the widget to HTML 
      htmlwidgets::saveWidget(
        widget = x, 
        file = htmlfile,
        selfcontained = FALSE, 
        libdir = libraryFolder)

      .Call("rs_recordHtmlWidget", htmlfile, depfile);
    }
  }, envir = as.environment("tools:rstudio"))
})

.rs.addFunction("releaseHtmlCapture", function()
{
  rm("print.htmlwidget", envir = as.environment("tools:rstudio"))
})
