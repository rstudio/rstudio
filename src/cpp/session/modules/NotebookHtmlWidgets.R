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

.rs.addFunction("recordHtmlWidget", function(htmlfile, depfile)
{
   .Call("rs_recordHtmlWidget", htmlfile, depfile)
})

.rs.addFunction("rnb.setHtmlCaptureContext", function(...)
{
   .rs.setVar("rnb.htmlCaptureContext", list(...))
})

.rs.addFunction("rnb.getHtmlCaptureContext", function()
{
   .rs.getVar("rnb.htmlCaptureContext")
})

# Hooks ----

.rs.addFunction("rnb.saveHtmlToCache", function(x, ...)
{
   ctx <- .rs.rnb.getHtmlCaptureContext()
   
   # tempfile paths for html resources
   htmlfile <- tempfile("_rs_html_", tmpdir = ctx$outputFolder, fileext = ".html")
   depfile <- tempfile("_rs_html_deps_", tmpdir = ctx$outputFolder, fileext = ".json")
   
   # render html tags (to discover recursive dependencies + get html)
   rendered <- htmltools::renderTags(x)
   htmldeps <- rendered$dependencies
   html <- rendered$html
   
   if (length(htmldeps)) {
      # if we have html dependencies, write those to file and use 'save_html'
      cat(.rs.toJSON(htmldeps, unbox = TRUE), file = depfile, sep = "\n")
      htmltools::save_html(x, file = htmlfile, libdir = ctx$libraryFolder)
   } else {
      # otherwise, just write html to file as-is
      cat(as.character(html), file = htmlfile, sep = "\n")
   }
   
   # record the generated artefacts
   .rs.recordHtmlWidget(htmlfile, depfile)
})

.rs.addFunction("rnbHooks.print.html",           .rs.rnb.saveHtmlToCache)
.rs.addFunction("rnbHooks.print.shiny.tag",      .rs.rnb.saveHtmlToCache)
.rs.addFunction("rnbHooks.print.shiny.tag.list", .rs.rnb.saveHtmlToCache)

.rs.addFunction("rnbHooks.print.htmlwidget", function(x, ...) {
   ctx <- .rs.rnb.getHtmlCaptureContext()
   
   outputFolder  <- ctx$outputFolder
   chunkOptions  <- ctx$chunkOptions
   libraryFolder <- ctx$libraryFolder
   
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
   
   # force a responsive viewer sizing policy
   x$sizingPolicy$viewer.padding <- 0
   x$sizingPolicy$viewer.fill <- TRUE
   
   # collect knitr options
   knitrOptions <- knitr::opts_chunk$get()
   
   # infer knitr sizing
   first_of <- function(...) {
      for (item in list(...))
         if (length(item)) return(item)
      return(NULL)
   }
   
   fig.height <- first_of(
      chunkOptions$fig.height,
      knitrOptions$fig.height,
      7
   )
   
   fig.width <- first_of(
      chunkOptions$fig.width,
      knitrOptions$fig.width,
      fig.height * 5 / 7
   )
   
   dpi <- first_of(
      chunkOptions$dpi,
      knitrOptions$dpi,
      72
   )
   
   knitrOptions$out.width.px <- fig.width * dpi
   knitrOptions$out.height.px <- fig.height * dpi
   
   # save as HTML -- save a modified version of the 'standalone' representation
   # that works effectively the same way as the 'embedded' representation;
   # we use a filter on the server side to disentangle things as needed
   html <- htmlwidgets:::toHTML(x, standalone = TRUE, knitrOptions = knitrOptions)
   
   # validate some expectations about the data structure
   if (length(html) != 3)
      stop("unexpected htmlwidget structure: expected taglist of length 3")
   
   if (html[[1]]$attribs$id != "htmlwidget_container")
      stop("expected a container div with id 'htmlwidget_container'")
   
   if (length(html[[1]]$children) == 0)
      stop("expected one or more children for htmlwidget container div")
   
   # force knitr styling on 'standalone' widget (will be overridden by sizing policy
   # in dynamic environments; this ensures that the 'preview' will be displayed as
   # though the widget were generated through 'rmarkdown::render()')
   embedded <- htmlwidgets:::toHTML(x, standalone = FALSE, knitrOptions = knitrOptions)
   html[[1]]$children[[1]][[2]]$attribs$style <- embedded[[1]][[2]]$attribs$style
   
   # split up into parts
   div <- html[[1]]$children[[1]]
   json <- html[[2]]
   policy <- html[[3]]
   
   # encode the htmlwidget sizing policy as base64 (disentangled on the server
   # as appropriate, e.g. when showing viewer element in IDE)
   fmt <- "<!-- htmlwidget-sizing-policy-base64 %s -->"
   encodedPolicy <- .rs.base64encode(paste(as.character(policy), collapse = "\n"))
   htmlPolicy <- htmltools::HTML(sprintf(fmt, encodedPolicy))
   
   # generate annotated version of standalone html that we can disentangle
   # with a server-side filter as needed
   # NOTE: if you touch this make sure you update the parsing code in NotebookOutput.cpp
   htmlProduct <- htmltools::tagList(
      htmltools::HTML("<!-- htmlwidget-container-begin -->"),
      div,
      htmltools::HTML("<!-- htmlwidget-container-end -->"),
      json,
      htmlPolicy
   )
   attributes(htmlProduct) <- attributes(html)
   
   # write html
   htmltools::save_html(htmlProduct, file = htmlfile, libdir = libraryFolder)
   
   # record the saved artefacts
   .rs.recordHtmlWidget(htmlfile, depfile)
})


# HTML Capture ----

.rs.addFunction("rnb.htmlCaptureHooks", function()
{
   list(
      "print.htmlwidget"     = .rs.rnbHooks.print.htmlwidget,
      "print.html"           = .rs.rnbHooks.print.html,
      "print.shiny.tag"      = .rs.rnbHooks.print.shiny.tag,
      "print.shiny.tag.list" = .rs.rnbHooks.print.shiny.tag.list
   )
})

.rs.addFunction("initHtmlCapture", function(outputFolder,
                                            libraryFolder,
                                            chunkOptions)
{
   # cache context
   .rs.rnb.setHtmlCaptureContext(
      outputFolder = outputFolder,
      libraryFolder = libraryFolder,
      chunkOptions = chunkOptions
   )
   
   # get and load hooks
   envir <- .rs.toolsEnv()
   hooks <- .rs.rnb.htmlCaptureHooks()
   .rs.enumerate(hooks, function(key, value) {
      assign(key, value, envir = envir)
   })
})

.rs.addFunction("releaseHtmlCapture", function()
{
   # remove hooks
   hooks <- .rs.rnb.htmlCaptureHooks()
   
   # construct call to 'rm'
   args <- c(as.list(names(hooks)), envir = .rs.toolsEnv())
   do.call(rm, args)
})
