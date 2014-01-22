#' Convert R Markdown to Beamer
#'
#' Converts an R Markdown (Rmd) file to a Beamer Presentation PDF
#'
#' @param input input Rmd document
#' @param output Target output file (defaults to <input>.pdf if not specified)
#' @param options List of Beamer rendering options created by calling
#'   \code{beamerOptions}
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


#' Options for Beamer conversion
#'
#' Define the options for converting R Markdown to Beamer
#'
#' @param toc \code{TRUE} to include a table of contents in the output (only
#'   level 1 headers will be included in the table of contents).
#' @param incremental \code{TRUE} to render slide bullets incrementally. Note
#'   that if you want to reverse the default incremental behavior for an
#'   individual bullet you can preceded it with \code{>}. For example:
#'   \emph{\code{> - Bullet Text}}
#' @param slide.level The heading level which defines indvidual slides. By
#'   default this is level 2, which allows level 1 headers to be used to define
#'   sections of the presentation.
#' @param include.header One or more files with LaTeX content to be included in
#'   the header of the document.
#' @param include.before One or more files with LaTeX content to be included
#'   before the document body.
#' @param include.after One or more files with LaTeX content to be included
#'   after the document body.
#'
#' @return A list of options that can be passed to \code{\link{rmd2beamer}}.
#'
#' @export
beamerOptions <- function(toc = FALSE,
                          incremental = FALSE,
                          slide.level = 2,
                          include.header = NULL,
                          include.before = NULL,
                          include.after = NULL) {
  structure(list(toc = toc,
                 incremental = incremental,
                 slide.level = slide.level,
                 include.header = include.header,
                 include.before = include.before,
                 include.after = include.after),
            class = "beamerOptions")
}


#' @S3method pandocOptions beamerOptions
pandocOptions.beamerOptions <- function(beamerOptions) {

  # base options for all beamer output
  options <- c()

  # table of contents
  if (beamerOptions$toc)
    options <- c(options, "--table-of-contents")

  # incremental
  if (beamerOptions$incremental)
    options <- c(options, "--incremental")

  # slide level
  options <- c(options,
               "--slide-level",
               as.character(beamerOptions$slide.level))

  # content includes
  options <- c(options, pandocIncludeOptions(beamerOptions))

  options
}




