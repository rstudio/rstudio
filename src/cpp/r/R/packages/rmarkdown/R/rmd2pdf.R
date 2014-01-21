#' Convert R Markdown to PDF
#'
#' Converts an R Markdown (Rmd) file to PDF
#'
#' @param input Input Rmd document
#' @param output Target output file (defaults to <input>.pdf if not specified)
#' @param envir The environment in which the code chunks are to be evaluated (can use
#'   \code{\link{new.env}()} to guarantee an empty new environment)
#' @param quiet Whether to suppress the progress bar and messages
#' @param encoding The encoding of the input file; see \code{\link{file}}
#'
#' @return The compiled document is written into the output file, and the path
#'   of the output file is returned.
#'
#' @export
rmd2pdf <- function(input,
                    output = NULL,
                    envir = parent.frame(),
                    quiet = FALSE,
                    encoding = getOption("encoding")) {

  # knitr options
  knitrRenderPDF("latex", 6, 5)

  # pandoc options
  geometry <- list()
  geometry$margin = "1in"
  options <- pandocPDFOptions(geometry = geometry)

  # call pandoc
  rmd2pandoc(input,
             output,
             to = "latex",
             options = options,
             envir = envir,
             quiet = quiet,
             encoding = encoding)
}


#' Pandoc options for PDF rendering
#'
#' Get pandoc command-line options required for converting R Markdown PDF.
#'
#' @param geometry List of \code{LaTeX} geometry settings (optional)
#'
#' @return Character vector of pandoc options
#'
#' @export
pandocPDFOptions <- function(geometry = NULL) {

  options <- c()

  if (!is.null(geometry)) {
    for (name in names(geometry)) {
      value <- geometry[[name]]
      options <- c(options,
                   "--variable",
                   paste0("geometry:", name, "=", value))
    }
  }

  options
}


