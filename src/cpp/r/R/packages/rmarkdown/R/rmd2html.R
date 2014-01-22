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
#' @param toc \code{TRUE} to include a table of contents in the output
#' @param toc.depth Depth of headers to include in table of contents
#' @param theme HTML theme ("default", "cerulean", or "slate"). Pass \code{NULL}
#'   to not use any theme (add your own css using the \code{css} parameter).
#' @param highlight Syntax highlighting style ("default", "pygments", "kate",
#'   "monochrome", "espresso", "zenburn", "haddock", or "tango"). Pass
#'   \code{NULL} to not syntax highlight code.
#' @param mathjax Include mathjax from the specified URL. Pass \code{NULL} to
#'   not include mathjax.
#' @param css One or more css files to include
#' @param include.header One or more files with HTML content to be included
#'   within the HTML \code{head} tag.
#' @param include.before One or more files with HTML content to be included
#'   before the document body.
#' @param include.after One or more files with HTML content to be included after
#'   the document body.
#' @param standalone \code{TRUE} to produce a fully valid HTML document (rather
#'   than a fragment). If this is \code{FALSE} then the \code{theme},
#'   \code{highlight}, \code{mathjax}, \code{css}, and content inclusion options
#'   are not applied.
#' @param self.contained \code{TRUE} to produce a standalone HTML file with no
#'   external dependencies, using data: URIs to incorporate the contents of
#'   linked scripts, stylesheets, and images (note that MathJax is still
#'   referenced externally). If this is \code{FALSE} then the \code{theme} and
#'   \code{highlight} options are not applied.
#'
#' @details Paths for resources referenced from the \code{css},
#'   \code{include.header}, \code{include.before}, and \code{include.after}
#'   parameters are resolved relative to the directory of the input document.
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
                        include.after = NULL,
                        standalone = TRUE,
                        self.contained = TRUE) {
  structure(list(toc = toc,
                 toc.depth = toc.depth,
                 theme = theme,
                 highlight = highlight,
                 mathjax = mathjax,
                 css = css,
                 include.header = include.header,
                 include.before = include.before,
                 include.after = include.after,
                 standalone = standalone,
                 self.contained = self.contained),
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
  options <- c("--smart")

  # table of contents
  if (htmlOptions$toc) {
    options <- c(options, "--table-of-contents")
    options <- c(options, "--toc-depth", htmlOptions$toc.depth)
  }

  # mathjax
  if (!is.null(htmlOptions$mathjax)) {
    options <- c(options, "--mathjax")
  }

  if (htmlOptions$standalone) {

    # standalone
    options <- c(options, "--standalone")

    # self contained
    if (htmlOptions$self.contained) {

      options <- c(options, "--self-contained")

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
    } else {
      options <- c(options, "--no-highlight")
    }

    # mathjax url
    if (!is.null(htmlOptions$mathjax)) {
      options <- c(options,
                   "--variable", paste0("mathjax-url:", htmlOptions$mathjax))
    }

    # template
    options <- c(options, pandocTemplateOptions("html/default.html"))

    # additional css
    for (css in htmlOptions$css)
      options <- c(options, "--css", css)

    # content includes
    options <- c(options, pandocIncludeOptions(htmlOptions))
  }

  # not standalone
  else {

    # no highlighting since we can't include the highlighting js/css
    options <- c(options, "--no-highlight")

    # use ascii since we weren't able to include a content-type in the head
    options <- c(options, "--ascii")

  }

  options
}



