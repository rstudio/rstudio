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

  # call pandoc
  rmd2pandoc(input, "beamer", output, options, envir, quiet, encoding)
}


#' @rdname rmd2beamer
#' @export
beamerOptions <- function(theme = "default") {
  structure(list(theme = theme),
            class = "beamerOptions")
}


#' @S3method pandocOptions beamerOptions
pandocOptions.beamerOptions <- function(beamerOptions) {

  # base options for all beamer output
  options <- c()

  # theme
  options <- c(options,
               "--variable", paste0("theme:", beamerOptions$theme))

  options
}




