

#' Convert R Markdown to PDF
#'
#' Converts an R Markdown (Rmd) file to PDF
#'
#' @param input input Rmd document
#' @param output target output file (defaults to <input>.pdf if not specified)
#' @param quiet whether to suppress the progress bar and messages
#' @param encoding the encoding of the input file; see \code{\link{file}}
#' @return The compiled document is written into the output file, and the path
#'   of the output file is returned.
#'
#' @export
rmd2pdf <- function(input,
                    output = NULL,
                    quiet = FALSE,
                    encoding = getOption("encoding")) {

  # format and output file
  pandocFormat <- "latex"
  if (is.null(output))
    output <- pandocOutputFile(input, pandocFormat)

  # knitr options
  knitrRenderMarkdown(pandocFormat)
  knitr::knit_hooks$set(crop = knitr::hook_pdfcrop)
  knitr::opts_chunk$set(dev = 'cairo_pdf',
                        fig.width = 6,
                        fig.height = 5)

  # call knitr
  md <- knitr::knit(input, quiet = quiet, encoding = encoding)

  # pandoc options
  geometry <- list()
  geometry$margin = "1in"
  options <- c(pandocMarkdownOptions(),
               pandocPDFOptions(geometry = geometry),
               recursive = TRUE)

  # call pandoc
  pandocConvert(md, output, pandocFormat, options, quiet)
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
                   paste0("geometry:", name, "=", value),
                   recursive = TRUE)
    }
  }
  options
}

