#' Convert R Markdown to PDF
#'
#' Converts an R Markdown (Rmd) file to PDF
#'
#' @param input Input Rmd document
#' @param output Target output file (defaults to <input>.pdf if not specified)
#' @param options List of PDF rendering options created by calling
#'   \code{pdfOptions}
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

  # call pandoc
  rmd2pandoc(input, "latex", output, options, envir, quiet, encoding)
}

#' Options for PDF conversion
#'
#' Define the options for converting R Markdown to PDF.
#'
#' @param toc \code{TRUE} to include a table of contents in the output
#' @param toc.depth Depth of headers to include in table of contents
#' @param number.sections \code{TRUE} Number section headings
#' @param highlight \code{TRUE} to syntax highlight code (see
#'   \code{highlight.style} for customizing the rendering of highlighted code).
#' @param template LaTeX template to use for rendering document. This should
#'   either be the path to a pandoc template or an object that provides a
#'   \code{pandocOptions} S3 method.
#' @param geometry List containing LaTeX geometry options for the document.
#' @param margin Size of page margins
#' @param highlight.style Style for syntax highlighting. Options are pygments,
#'   kate, monochrome, espresso, zenburn, haddock, and tango.
#' @param include.header One or more files with LaTeX content to be included in
#'   the header of the document.
#' @param include.before One or more files with LaTeX content to be included
#'   before the document body.
#' @param include.after One or more files with LaTeX content to be included
#'   after the document body.
#'
#' @details The \code{pdfTemplate} function provides the default pandoc LaTeX
#'   PDF template along with the abilty to add additional LaTeX to the beginning
#'   and end of the document.
#'
#'   Paths for resources referenced from the \code{include.header},
#'   \code{include.before}, and \code{include.after} parameters are resolved
#'   relative to the directory of the input document.
#'
#' @return A list of PDF options that can be passed to \code{\link{rmd2pdf}}.
#'
#' @export
pdfOptions <- function(toc = FALSE,
                       toc.depth = 2,
                       number.sections = FALSE,
                       highlight = TRUE,
                       template = pdfTemplate()) {
  structure(list(toc = toc,
                 toc.depth = toc.depth,
                 number.sections = number.sections,
                 highlight = highlight,
                 template = template),
            class = "pdfOptions")
}

#' @rdname pdfOptions
#' @export
pdfTemplate <- function(geometry = pdfGeometry(),
                        highlight.style = "pygments",
                        include.header = NULL,
                        include.before = NULL,
                        include.after = NULL) {
  structure(list(geometry = geometry,
                 highlight.style = highlight.style,
                 include.header = include.header,
                 include.before = include.before,
                 include.after = include.after),
            class = "pdfTemplate")
}

#' @rdname pdfOptions
#' @export
pdfGeometry <- function(margin = "1in", ...) {
  geometry <- list(...)
  geometry$margin <- margin
  structure(geometry,
            class = "pdfGeometry")
}


#' @S3method pandocOptions pdfOptions
pandocOptions.pdfOptions <- function(pdfOptions) {

  # base options for all PDF output
  options <- c()

  # table of contents
  options <- c(options, pandocTableOfContentsOptions(pdfOptions))

  # numbered sections
  if (pdfOptions$number.sections)
    options <- c(options, "--number-sections")

  # highlighting
  options <- c(options, pandocPdfHighlightOptions(pdfOptions))

  # check for a template path
  if (is.character(pdfOptions$template)) {

    # template and data dir
    template <- tools::file_path_as_absolute(pdfOptions$template)
    options <- c(options,
                 "--template", template,
                 "--data-dir", dirname(template))
  }
  # dispatch to S3
  else  {
    options <- c(options, pandocOptions(pdfOptions$template))
  }

  options
}

#' @S3method pandocOptions pdfTemplate
pandocOptions.pdfTemplate <- function(pdfTemplate) {

  options <- c()

  # geometry
  options <- c(options, pandocOptions(pdfTemplate$geometry))

  # content includes
  options <- c(options, pandocIncludeOptions(pdfTemplate))

  options
}

#' @S3method pandocOptions pdfGeometry
pandocOptions.pdfGeometry <- function(pdfGeometry) {

  options <- c()

  for (name in names(pdfGeometry)) {
    value <- pdfGeometry[[name]]
    options <- c(options,
                 "--variable",
                 paste0("geometry:", name, "=", value))
  }

  options
}


