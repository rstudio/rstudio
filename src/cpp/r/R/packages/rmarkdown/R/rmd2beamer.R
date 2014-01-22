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
#' @param slide.level The heading level which defines indvidual slides. By
#'   default this is level 2, which allows level 1 headers to be used to define
#'   sections of the presentation.
#' @param incremental \code{TRUE} to render slide bullets incrementally. Note
#'   that if you want to reverse the default incremental behavior for an
#'   individual bullet you can preceded it with \code{>}. For example:
#'   \emph{\code{> - Bullet Text}}
#' @param highlight \code{TRUE} to syntax highlight code (see
#'   \code{highlight.style} for customizing the rendering of highlighted code).
#' @param template Beamer template to use for rendering document. This should
#'   either be the path to a pandoc template or an object that provides a
#'   \code{pandocOptions} S3 method.
#' @param highlight.style Style for syntax highlighting. Options are pygments,
#'   kate, monochrome, espresso, zenburn, haddock, and tango.
#' @param include.header One or more files with LaTeX content to be included in
#'   the header of the document.
#' @param include.before One or more files with LaTeX content to be included
#'   before the document body.
#' @param include.after One or more files with LaTeX content to be included
#'   after the document body.
#'
#' @details The \code{beamerTemplate} function provides the default pandoc
#'   beamer template along with the abilty to add additional LaTeX to the
#'   beginning and end of the document.
#'
#'   Paths for resources referenced from the \code{include.header},
#'   \code{include.before}, and \code{include.after} parameters are resolved
#'   relative to the directory of the input document.
#'
#' @return A list of options that can be passed to \code{\link{rmd2beamer}}.
#'
#' @export
beamerOptions <- function(toc = FALSE,
                          slide.level = 2,
                          incremental = FALSE,
                          highlight = TRUE,
                          template = beamerTemplate()) {
  structure(list(toc = toc,
                 slide.level = slide.level,
                 incremental = incremental,
                 highlight = highlight,
                 template = template),
            class = "beamerOptions")
}

#' @rdname beamerOptions
#' @export
beamerTemplate <- function(highlight.style = "pygments",
                           include.header = NULL,
                           include.before = NULL,
                           include.after = NULL) {
  structure(list(highlight.style = highlight.style,
                 include.header = include.header,
                 include.before = include.before,
                 include.after = include.after),
            class = "beamerTemplate")
}


#' @S3method pandocOptions beamerOptions
pandocOptions.beamerOptions <- function(beamerOptions) {

  # base options for all beamer output
  options <- c()

  # table of contents
  if (beamerOptions$toc)
    options <- c(options, "--table-of-contents")

  # slide level
  options <- c(options,
               "--slide-level",
               as.character(beamerOptions$slide.level))

  # incremental
  if (beamerOptions$incremental)
    options <- c(options, "--incremental")

  # highlighting
  options <- c(options, pandocPdfHighlightOptions(beamerOptions))

  # check for a template path
  if (is.character(beamerOptions$template)) {

    # template and data dir
    template <- tools::file_path_as_absolute(beamerOptions$template)
    options <- c(options,
                 "--template", template,
                 "--data-dir", dirname(template))
  }
  # dispatch to S3
  else  {
    options <- c(options, pandocOptions(beamerOptions$template))
  }

  options
}

#' @S3method pandocOptions beamerTemplate
pandocOptions.beamerTemplate <- function(beamerTemplate) {

  options <- c()

  # content includes
  options <- c(options, pandocIncludeOptions(beamerTemplate))

  options
}




