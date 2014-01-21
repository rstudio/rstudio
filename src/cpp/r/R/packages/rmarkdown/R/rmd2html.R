#' Convert R Markdown to HTML
#'
#' Converts an R Markdown (Rmd) file to HTML
#'
#' @param input Input Rmd document
#' @param output Target output file (defaults to <input>.html if not specified)
#' @param options List of HTML rendering options created by calling
#'   \code{htmlOptions}
#' @param toc Whether to include a table of contents in the output
#' @param toc.depth Depth of headers to include in table of contents
#' @param theme HTML theme ("default", "cerulean", or "slate")
#' @param mathjax Include mathjax from the specified URL (pass NULL to
#' not include mathjax)
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


#' @rdname rmd2html
#' @export
htmlOptions <- function(toc = FALSE,
                        toc.depth = 3,
                        theme = "default",
                        highlight = "default",
                        mathjax = mathjaxURL()) {
  structure(list(toc = toc,
                 toc.depth = toc.depth,
                 theme = theme,
                 highlight = highlight,
                 mathjax = mathjax),
            class = "htmlOptions")
}

#' @rdname rmd2html
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

  options
}



