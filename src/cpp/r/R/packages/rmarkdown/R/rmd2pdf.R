#' Convert R Markdown to PDF
#'
#' Converts an R Markdown (Rmd) file to PDF
#'
#' @param input Input Rmd document
#' @param output Target output file (defaults to <input>.pdf if not specified)
#' @param options List of PDF rendering options created by calling
#'   \code{pdfOptions}
#' @param toc Whether to include a table of contents in the output
#' @param geometry List of LaTeX geometry options created by calling
#'   \code{pdfGeometry}
#' @param margin Size of page margins
#' @param envir The environment in which the code chunks are to be evaluated
#'   (can use \code{\link{new.env}()} to guarantee an empty new environment)
#' @param quiet Whether to suppress the progress bar and messages
#' @param encoding The encoding of the input file; see \code{\link{file}}
#'
#' @return The compiled document is written into the output file, and the path
#'   of the output file is returned.
#'
#' @export
rmd2pdf <- function(input,
                    output = NULL,
                    options = pdfOptions(),
                    envir = parent.frame(),
                    quiet = FALSE,
                    encoding = getOption("encoding")) {

  # knitr options
  knitrRenderPDF("latex", 6, 5)

  # pandoc options
  options <- pandocPDFOptions(options)

  # call pandoc
  rmd2pandoc(input,
             output,
             to = "latex",
             options = options,
             envir = envir,
             quiet = quiet,
             encoding = encoding)
}

#' @rdname rmd2pdf
#' @export
pdfOptions <- function(toc = FALSE, geometry = pdfGeometry()) {
  list(toc = toc,
       geometry = geometry)
}

#' @rdname rmd2pdf
#' @export
pdfGeometry <- function(margin = "1in", ...) {
  geometry <- as.list(...)
  geometry$margin <- margin
  geometry
}


