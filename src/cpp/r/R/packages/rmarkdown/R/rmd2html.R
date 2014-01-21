#' Convert R Markdown to HTML
#'
#' Converts an R Markdown (Rmd) file to HTML
#'
#' @param input Input Rmd document
#' @param output Target output file (defaults to <input>.html if not specified)
#' @param options List of HTML rendering options created by calling
#'   \code{\link{htmlOptions}}
#' @param envir The environment in which the code chunks are to be evaluated
#'   (can use \code{\link{new.env}()} to guarantee an empty new environment)
#' @param quiet Whether to suppress the progress bar and messages
#' @param encoding The encoding of the input file; see \code{\link{file}}
#'
#' @return The compiled document is written into the output file, and the path
#'   of the output file is returned.
#'
#' @export
rmd2html <- function(input,
                     output = NULL,
                     options = htmlOptions(),
                     envir = parent.frame(),
                     quiet = FALSE,
                     encoding = getOption("encoding")) {

  # knitr options
  knitrRenderHTML("html", 7, 7)

  # call pandoc
  rmd2pandoc(input, "html", output, options, envir, quiet, encoding)
}


#' Options for HTML conversion
#'
#' Define the options for converting R Markdown to HTML.
#'
#' @param toc Whether to include a table of contents in the output
#' @param toc.depth Depth of headers to include in table of contents
#' @param theme HTML theme ("default", "cerulean", or "slate"). Pass \code{NULL}
#'   to not use any theme (add your own css using the \code{css} parameter).
#' @param highlight Syntax highlighting style ("default", "pygments", "kate",
#'   "monochrome", "espresso", "zenburn", "haddock", or "tango"). Pass
#'   \code{NULL} to not syntax highlight code.
#' @param mathjax Include mathjax from the specified URL. Pass \code{NULL} to
#'   not include mathjax.
#' @param css One or more css files to include
#' @param include.header One more files that include content to be inserted into
#'   the HTML \code{head}.
#' @param include.before One more files that include content to be inserted at
#'   the beginning of the HTML \code{body}.
#' @param include.after One more files that include content to be inserted at
#'   the end of the HTML \code{body}.
#'
#' @return A list of HTML options that can be passed to \code{\link{rmd2html}}.
#'
#' @export
htmlOptions <- function(toc = FALSE,
                        toc.depth = 3,
                        theme = "default",
                        highlight = "default",
                        mathjax = mathjaxURL(),
                        css = NULL,
                        include.header = NULL,
                        include.before = NULL,
                        include.after = NULL) {
  structure(list(toc = toc,
                 toc.depth = toc.depth,
                 theme = theme,
                 highlight = highlight,
                 mathjax = mathjax,
                 css = css,
                 include.header = include.header,
                 include.before = include.before,
                 include.after = include.after),
            class = "htmlOptions")
}

#' @rdname htmlOptions
#' @export
mathjaxURL <- function() {
  paste0("https://c328740.ssl.cf1.rackcdn.com/mathjax/latest/MathJax.js",
         "?config=TeX-AMS-MML_HTMLorMML")
}


#' @S3method pandocOptions htmlOptions
pandocOptions.htmlOptions <- function(htmlOptions) {

  # base options for all HTML output
  options <- c(pandocTemplateOptions("html/default.html"),
               "--smart",
               "--self-contained")

  # table of contents
  if (htmlOptions$toc) {
    options <- c(options, "--table-of-contents")
    options <- c(options, "--toc-depth", htmlOptions$toc.depth)
  }

  # theme
  if (!is.null(htmlOptions$theme)) {

    theme <- htmlOptions$theme
    if (identical(theme, "default"))
      theme <- "bootstrap"

    options <- c(options,
                 "--variable", paste0("theme:", theme))
  }

  # highlighting
  if (is.null(htmlOptions$highlight)) {
    options <- c(options, "--no-highlight")
  }
  else if (identical(htmlOptions$highlight, "default")) {
    options <- c(options, "--no-highlight",
                          "--variable", "highlightjs")
  }
  else {
    options <- c(options, "--highlight-style", htmlOptions$highlight)
  }

  # mathjax
  if (!is.null(htmlOptions$mathjax)) {
    options <- c(options,
                 "--mathjax",
                 "--variable", paste0("mathjax-url:", htmlOptions$mathjax))
  }

  # additional css
  for (css in htmlOptions$css)
    options <- c(options, "--css", css)

  # header content
  for (header in htmlOptions$include.header)
    options <- c(options, "--include-in-header", header)

  # body prefix
  for (before in htmlOptions$include.before)
    options <- c(options, "--include-before-body", before)

  # body suffix
  for (after in htmlOptions$include.after)
    options <- c(options, "--include-after-body", after)

  options
}



