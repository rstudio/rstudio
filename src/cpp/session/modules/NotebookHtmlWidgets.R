#
# NotebookHtmlWidgets.R
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

.rs.addFunction("recordHtmlWidget", function(x, htmlfile, depfile)
{
   .Call("rs_recordHtmlWidget", htmlfile, depfile, list(
      classes = class(x),
      sizingPolicy = if (is.list(x)) x$sizingPolicy else list()
   ))
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
   .rs.recordHtmlWidget(x, htmlfile, depfile)
})

.rs.addFunction("rnbHooks.print.html",           .rs.rnb.saveHtmlToCache)
.rs.addFunction("rnbHooks.print.shiny.tag",      .rs.rnb.saveHtmlToCache)
.rs.addFunction("rnbHooks.print.shiny.tag.list", .rs.rnb.saveHtmlToCache)

.rs.addFunction("rnbHooks.print.knit_asis", function(x, ...) 
{
   ctx <- .rs.rnb.getHtmlCaptureContext()

   # create temporary file paths
   mdfile   <- tempfile("_rs_md_", fileext = ".md")
   htmlfile <- tempfile("_rs_html_", tmpdir = ctx$outputFolder, 
                        fileext = ".html")

   # save contents to temporary file
   writeLines(enc2utf8(x), con = mdfile, useBytes = TRUE)
   
   # render to HTML with pandoc
   rmarkdown::pandoc_convert(input = rmarkdown::pandoc_path_arg(mdfile), 
                             to = "html",
                             output = rmarkdown::pandoc_path_arg(htmlfile))

   # record in cache
   .rs.recordHtmlWidget(x, htmlfile, NULL)
})

.rs.addFunction("rnbHooks.print.knit_image_paths", function(x, ...) 
{
   .Call("rs_recordExternalPlot", vapply(x, function(path) {
      dest <- tempfile(fileext = paste(".", tools::file_ext(path), sep = ""))
      if (identical(substr(path, 1, 7), "http://") ||
          identical(substr(path, 1, 8), "https://")) {
         # if the path appears to be a URL, download it locally 
         tryCatch({
            suppressMessages(download.file(path, dest, quiet = TRUE))
         },
         error = function(e) {})
      } else {
         # not a URL, presume it to be an ordinary path on disk
         file.copy(path, dest, copy.mode = FALSE)
      }
      dest
   }, ""))
})

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

   # make width dinamic to size correctly non-figured
   if (!is.null(x$sizingPolicy$knitr) && identical(x$sizingPolicy$knitr$figure, FALSE)) {
      x$sizingPolicy$defaultWidth <- "auto"
      x$sizingPolicy$browser$fill <- FALSE
   }
   
   # collect knitr options
   knitrOptions <- knitr::opts_current$get()
   `%||%` <- function(x, y) if (length(x)) x else y
   
   defaultDpi <- 96
   defaultFigWidth  <- 7
   defaultFigHeight <- 5
   
   # detect if the default knitr chunk fig.width, fig.height have been set
   # TODO: could this be made more robust? are we setting these values,
   # or are they coming from rmarkdown / knitr?
   
   # infer the DPI (be careful to handle fig.retina)
   if (!is.null(chunkOptions$dpi)) {
      dpi <- chunkOptions$dpi
   } else if (!is.null(knitrOptions$dpi)) {
      dpi <- knitrOptions$dpi
      if (is.numeric(knitrOptions$fig.retina))
         dpi <- dpi / knitrOptions$fig.retina
   } else {
      dpi <- defaultDpi
   }
   
   isDefaultKnitrSizing <-
      identical(knitrOptions$fig.width, defaultFigWidth) &&
      identical(knitrOptions$fig.height, defaultFigHeight) &&
      identical(dpi, defaultDpi) &&
      is.null(knitrOptions$fig.asp)
   
   # detect if the user has explicitly set fig.width, fig.height for
   # the currently executing chunk
   isDefaultChunkSizing <-
      is.null(chunkOptions$fig.width) &&
      is.null(chunkOptions$fig.height) &&
      is.null(chunkOptions$fig.asp)
   
   if (isDefaultKnitrSizing && isDefaultChunkSizing) {
      
      # if the user has not set any options related to chunk sizing, then
      # rely on the pre-computed values in 'knitrOptions', which should
      # have been set based on the htmlwidget sizing policy. (recompute
      # those values if not set)
      knitrOptions$out.width.px <- .rs.firstOf(
         knitrOptions$out.width.px,
         defaultFigWidth * defaultDpi
      )
      
      knitrOptions$out.height.px <- .rs.firstOf(
         knitrOptions$out.height.px,
         defaultFigHeight * defaultDpi
      )
      
   } else {
      
      # otherwise, set the figure width + height according to those chunk options
      
      # compute fig.width, fig.height
      figWidth  <- .rs.firstOf(chunkOptions$fig.width,  knitrOptions$fig.width) * dpi
      figHeight <- .rs.firstOf(chunkOptions$fig.height, knitrOptions$fig.height) * dpi
      figAsp    <- .rs.firstOf(chunkOptions$fig.asp,    knitrOptions$fig.asp, 1)
      
      # if fig.width or fig.height is null, compute from the companion
      knitrOptions$out.width.px  <- figWidth
      knitrOptions$out.height.px <- figHeight %||% figWidth * figAsp
   }
   
   # if, for some reason, we don't have an out.width.px or out.height.px
   # at this point, then just set some sane defaults
   knitrOptions$out.width.px  <- floor(knitrOptions$out.width.px %||% 500)
   knitrOptions$out.height.px <- floor(knitrOptions$out.height.px %||% 500)
   
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
   #
   # note that this make assumptions about the widget structure that might not be
   # true in all cases so we wrap this in tryCatch
   embedded <- htmlwidgets:::toHTML(x, standalone = FALSE, knitrOptions = knitrOptions)
   .rs.tryCatch(html[[1]]$children[[1]][[2]]$attribs$style <- embedded[[1]][[2]]$attribs$style)
   
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
   .rs.recordHtmlWidget(x, htmlfile, depfile)
})


# HTML Capture ----

.rs.addFunction("rnb.htmlCaptureHooks", function()
{
   list(
      "print.htmlwidget"       = .rs.rnbHooks.print.htmlwidget,
      "print.html"             = .rs.rnbHooks.print.html,
      "print.shiny.tag"        = .rs.rnbHooks.print.shiny.tag,
      "print.shiny.tag.list"   = .rs.rnbHooks.print.shiny.tag.list,
      "print.knit_asis"        = .rs.rnbHooks.print.knit_asis,
      "print.knit_image_paths" = .rs.rnbHooks.print.knit_image_paths
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
   hooks <- .rs.rnb.htmlCaptureHooks()
   .rs.enumerate(hooks, function(key, value) {
      .rs.addS3Override(key, value)
   })
})

.rs.addFunction("releaseHtmlCapture", function()
{
   # remove hooks
   hooks <- .rs.rnb.htmlCaptureHooks()
   for (name in names(hooks)) {
      .rs.removeS3Override(name)
   }
})

.rs.addFunction("firstOf", function(...)
{
   for (item in list(...))
      if (length(item)) return(item)
   return(NULL)
})
