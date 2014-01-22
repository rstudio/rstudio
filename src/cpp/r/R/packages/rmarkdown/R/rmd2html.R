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


#' Options for HTML Conversion
#'
#' Define the options for converting R Markdown to HTML.
#'
#' @param toc \code{TRUE} to include a table of contents in the output
#' @param toc.depth Depth of headers to include in table of contents
#' @param template HTML template to use for rendering document. This should
#'   either be the path to a pandoc template or an object that provides a
#'   \code{pandocOptions} S3 method. Pass \code{NULL} to create an HTML fragment
#'   rather than a full document.
#' @param bootstrap \code{TRUE} to style the document using
#'   \href{http://getbootstrap.com}{Bootstrap}. If you pass \code{FALSE} you can
#'   provide your own styles using the \code{css} and/or \code{include.header}
#'   parameters.
#' @param highlight \code{TRUE} to syntax highlight R code within the document.
#' @param mathjax Include mathjax from the specified URL. Pass \code{NULL} to
#'   not include mathjax.
#' @param css One or more css files to include
#' @param include.header One or more files with HTML content to be included
#'   within the HTML \code{head} tag.
#' @param include.before One or more files with HTML content to be included
#'   before the document body.
#' @param include.after One or more files with HTML content to be included after
#'   the document body.
#'
#' @details The \code{htmlTemplate} function provides a default template that
#'   includes Bootstrap CSS, syntax highlighting, and MathJax. Additional css
#'   and header and footer content can also be included, and the resulting HTML
#'   file is fully standalone.
#'
#'   Paths for resources referenced from the \code{css}, \code{include.header},
#'   \code{include.before}, and \code{include.after} parameters are resolved
#'   relative to the directory of the input document.
#'
#' @return A list of HTML options that can be passed to \code{\link{rmd2html}}.
#'
#' @export
htmlOptions <- function(toc = FALSE,
                        toc.depth = 3,
                        template = htmlTemplate()) {
  structure(list(toc = toc,
                 toc.depth = toc.depth,
                 template = htmlTemplate()),
            class = "htmlOptions")
}

#' @rdname htmlOptions
#' @export
htmlTemplate <- function(bootstrap = TRUE,
                         highlight = TRUE,
                         mathjax = mathjaxURL(),
                         css = NULL,
                         include.header = NULL,
                         include.before = NULL,
                         include.after = NULL) {
  structure(list(bootstrap = bootstrap,
                 highlight = highlight,
                 mathjax = mathjax,
                 css = css,
                 include.header = include.header,
                 include.before = include.before,
                 include.after = include.after),
            class = "htmlTemplate")
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
  options <- c(options, pandocTableOfContentsOptions(htmlOptions))

  if (!is.null(htmlOptions$template)) {

    # standalone
    options <- c(options, "--standalone")

    # check for a template path
    if (is.character(htmlOptions$template)) {

      # template and data dir
      template <- tools::file_path_as_absolute(htmlOptions$template)
      options <- c(options,
                   "--template", template,
                   "--data-dir", dirname(template))

      # process math (template still needs to include mathjax js)
      options <- c(options, "--mathjax")

      # assume highlighting is taken care of by the template
      options <- c(options, "--no-highlight")
    }
    # dispatch to S3
    else  {
      options <- c(options, pandocOptions(htmlOptions$template))
    }
  }

  # not standalone
  else {

    # use ascii since we weren't able to include a content-type in the head
    options <- c(options, "--ascii")

    # make sure math is escaped properly (since this is a fragment the
    # containing HTML will still need to include MathJax js)
    options <- c(options, "--mathjax")
  }

  options
}

#' @S3method pandocOptions htmlTemplate
pandocOptions.htmlTemplate <- function(htmlTemplate) {

  # self-contained html
  options <- c("--self-contained")

  # template path and assets
  options <- c(options, pandocTemplateOptions("html/default.html"))

  # bootstrap
  if (htmlTemplate$bootstrap)
    options <- c(options, "--variable", "bootstrap")

  # highlighting
  options <- c(options, "--no-highlight")
  if (htmlTemplate$highlight)
    options <- c(options, "--variable", "highlightjs")

  # mathjax
  if (!is.null(htmlTemplate$mathjax)) {
    options <- c(options, "--mathjax")
    options <- c(options,
                 "--variable", paste0("mathjax-url:", htmlTemplate$mathjax))
  }

  # additional css
  for (css in htmlTemplate$css)
    options <- c(options, "--css", css)

  # content includes
  options <- c(options, pandocIncludeOptions(htmlTemplate))

  options
}
