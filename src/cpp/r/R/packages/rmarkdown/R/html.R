
#' Convert R Markdown to HTML
#'
#' Converts an R Markdown (Rmd) file to HTML
#'
#' @param input input Rmd document
#' @param output target output file (defaults to <input>.html if not specified)
#' @param mathjax include mathjax from the specified url (pass NULL to
#' not include mathjax)
#' @param quiet whether to suppress the progress bar and messages
#' @param encoding the encoding of the input file; see \code{\link{file}}
#' @return The compiled document is written into the output file, and the path
#'   of the output file is returned.
#'
#' @export
rmd2html <- function(input,
                     output = NULL,
                     mathjax = mathjaxURL(),
                     quiet = FALSE,
                     encoding = getOption("encoding")) {

  # knitr options
  knitrRenderHTML("html", 7, 7)

  # call knitr
  md <- knitr::knit(input, quiet = quiet, encoding = encoding)

  # pandoc options
  options <- c(pandocMarkdownOptions(),
               pandocHTMLOptions(systemFile("templates/html/default.html"),
                                 mathjax),
               recursive = TRUE)

  # call pandoc
  pandocConvert(md, output, "html", options, quiet)
}


#' @rdname knitrRender
#' @export
knitrRenderHTML <- function(format, fig.width, fig.height) {

  # inherit defaults
  knitrRender(format)

  # graphics device
  knitr::opts_chunk$set(dev = 'png',
                        fig.width = fig.width,
                        fig.height = fig.height)
}



#' Pandoc options for HTML rendering
#'
#' Get pandoc command-line options required for converting R Markdown to HTML.
#'
#' @param template Full path to a custom pandoc HTML template
#' @param mathjax URL to mathjax library used in HTML output
#'
#' @return Character vector of pandoc options
#'
#' @export
pandocHTMLOptions <- function(template, mathjax = NULL) {
  options <- c("--template", template,
               "--data-dir", dirname(template),
               "--self-contained",
               "--no-highlight")
  if (!is.null(mathjax)) {
    options <- c(options,
                 "--mathjax",
                 "--variable", paste0("mathjax-url:", mathjax),
                 recursive = TRUE)
  }
  options
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




