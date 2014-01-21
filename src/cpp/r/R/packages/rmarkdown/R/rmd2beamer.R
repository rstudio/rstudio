#' Convert R Markdown to Beamer
#'
#' Converts an R Markdown (Rmd) file to a Beamer Presentation PDF
#'
#' @param input input Rmd document
#' @param output Target output file (defaults to <input>.pdf if not specified)
#' @param options List of Beamer rendering options created by calling
#'   \code{beamerOptions}
#' @param theme Beamer theme
#' @param envir The environment in which the code chunks are to be evaluated
#'   (can use \code{\link{new.env}()} to guarantee an empty new environment)
#' @param quiet Whether to suppress the progress bar and messages
#' @param encoding The encoding of the input file; see \code{\link{file}}
#'
#' @return The compiled document is written into the output file, and the path
#'   of the output file is returned.
#'
#' @export
rmd2beamer <- function(input,
                       output = NULL,
                       options = beamerOptions(),
                       envir = parent.frame(),
                       quiet = FALSE,
                       encoding = getOption("encoding")) {

  # knitr options
  knitrRenderPDF("beamer", 4.5, 3.5)

  # pandoc options
  options <- pandocBeamerOptions(options)

  # call pandoc
  rmd2pandoc(input,
             output,
             to = "beamer",
             options = options,
             envir = envir,
             quiet = quiet,
             encoding = encoding)
}


#' @rdname rmd2beamer
#' @export
beamerOptions <- function(theme = "default") {
  list(theme = theme)
}


