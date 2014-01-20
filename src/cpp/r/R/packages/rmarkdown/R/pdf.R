

#' Convert R Markdown to PDF
#'
#' Converts an R Markdown (Rmd) file to PDF
#'
#' @param input input Rmd document
#' @param output target output file (defaults to <input>.pdf if not specified)
#' @param quiet whether to suppress the progress bar and messages
#' @param encoding the encoding of the input file; see \code{\link{file}}
#'
#' @return The compiled document is written into the output file, and the path
#'   of the output file is returned.
#'
#' @export
rmd2pdf <- function(input,
                    output = NULL,
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
             quiet = quiet,
             encoding = encoding)
}


#' Convert R Markdown to Beamer
#'
#' Converts an R Markdown (Rmd) file to a Beamer Presentation PDF
#'
#' @param input input Rmd document
#' @param output Target output file (defaults to <input>.pdf if not specified)
#' @param theme Beamer theme
#' @param quiet Whether to suppress the progress bar and messages
#' @param encoding The encoding of the input file; see \code{\link{file}}
#'
#' @return The compiled document is written into the output file, and the path
#'   of the output file is returned.
#'
#' @export
rmd2beamer <- function(input,
                       output = NULL,
                       theme = "default",
                       quiet = FALSE,
                       encoding = getOption("encoding")) {

  # knitr options
  knitrRenderPDF("beamer", 4.5, 3.5)

  # pandoc options
  options <- c(pandocPDFOptions(),
               "--variable", paste0("theme:", theme),
               recursive = TRUE)

  # call pandoc
  rmd2pandoc(input,
             output,
             to = "beamer",
             options = options,
             quiet = quiet,
             encoding = encoding)
}

