#' Convert R Markdown to HTML
#'
#' Converts an R Markdown (Rmd) file to HTML
#'
#' @param input Input Rmd document
#' @param output Target output file (defaults to <input>.html if not specified)
#' @param options List of HTML rendering options created by calling
#'   \code{htmlOptions}
#' @param toc Whether to include a table of contents in the output
#' @param mathjax Include mathjax from the specified url (pass NULL to
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

  # pandoc options
  options <- c(pandocTemplateOptions(systemFile("templates/html/default.html")),
               pandocHTMLOptions(options))

  # call pandoc
  rmd2pandoc(input,
             output,
             to = "html",
             options = options,
             envir = envir,
             quiet = quiet,
             encoding = encoding)
}


#' @rdname rmd2html
#' @export
htmlOptions <- function(toc = FALSE, mathjax = mathjaxURL()) {
  list(toc = toc,
       mathjax = mathjax)
}


#' MathJax URL
#'
#' Get the URL to the MathJax library.
#'
#' @param version Version to use
#' @param config Configuration to use
#' @param https Use secure connection
#'
#' @return URL to MathJax library
#'
#' @export
mathjaxURL <- function(version = "latest",
                       config = "TeX-AMS-MML_HTMLorMML",
                       https = FALSE) {
  if (https)
    baseurl <- "https://c328740.ssl.cf1.rackcdn.com/mathjax"
  else
    baseurl <- "http://cdn.mathjax.org/mathjax"

  paste0(baseurl, "/", version, "/MathJax.js?config=", config)
}




