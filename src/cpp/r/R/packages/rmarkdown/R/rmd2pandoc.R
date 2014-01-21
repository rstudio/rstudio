#' Knit and convert an R Markdown document
#'
#' Knit the specified R Markdown input file and convert it to final output using
#' pandoc.
#'
#' @param input Input file
#' @param to Pandoc format to convert to
#' @param output Output file (if not specified then a default based on the
#'   specified \code{to} format is chosen)
#' @param options Command line options to pass to pandoc. This should either be
#'   a character vector of literal command line options or an object that
#'   provices a \code{pandocOptions} S3 method which yields the options.
#' @param envir The environment in which the code chunks are to be evaluated
#'   (can use \code{\link{new.env}()} to guarantee an empty new environment)
#' @param quiet \code{TRUE} to supress printing of the pandoc command line
#' @param encoding the encoding of the input file; see \code{\link{file}}
#'
#' @return The compiled document is written into the output file, and the path
#'   of the output file is returned.
#'
#' @details Typically one of the \code{\link{knitrRender}} functions is called
#'   prior to calling \code{knit2pandoc} to optimize knitr rendering for the
#'   intended output format.
#'
#' @export
rmd2pandoc <- function(input,
                       to,
                       output = NULL,
                       options = NULL,
                       envir = parent.frame(),
                       quiet = FALSE,
                       encoding = getOption("encoding")) {

  # verify we have the minimum version
  verifyPandocVersion()

  # execute within the input file's directory
  oldwd <- setwd(dirname(tools::file_path_as_absolute(input)))
  on.exit(setwd(oldwd), add = TRUE)

  # knit
  input <- knitr::knit(input, envir = envir, quiet = quiet, encoding = encoding)

  # pandoc: convert to format
  args <- c("--to", to)

  # pandoc: convert from format
  args <- c(args, "--from",
            paste0("markdown_github",
                   "-hard_line_breaks",
                   "+superscript",
                   "+tex_math_dollars",
                   "+raw_html",
                   "+auto_identifiers",
                   "+raw_tex",
                   "+latex_macros",
                   "+footnotes",
                   "+inline_notes",
                   "+citations",
                   "+yaml_metadata_block"))

  # pandoc: additional command line options
  args <- c(args, pandocOptions(options))

  # pandoc: output file
  if (is.null(output))
    output <- pandocOutputFile(input, to)
  args <- c(args, "--output", output)

  # pandoc: input file
  args <- c(args, input)

  # show pandoc command line if requested
  if (!quiet) {
    cat("pandoc ")
    cat(paste(args, collapse=" "))
    cat("\n")
  }

  # run the conversion
  pandocExec(args)

  # return the name of the output file
  invisible(output)
}


#' Set knitr hooks and options for rendering R Markdown
#'
#' These functions set knitr hooks and options for markdown rendering. Hooks are
#' based on the default
#' \code{\link[knitr:render_markdown]{knitr::render_markdown}} function with
#' additional customization for various output formats.
#'
#' @param format Pandoc format being rendered (used to create distinct figure
#'   directories for multiple formats)
#' @param fig.width Default width for figures
#' @param fig.height Default height for figures
#'
#' @details You typically need to call only one knitr render function, as the
#' various format-specific functions (e.g. \code{knitrRenderPDF}) all call
#' the \code{knitrRender} function as part of their implementation.
#'
#' @export
knitrRender <- function(format) {

  # stock markdown options
  knitr::render_markdown()

  # chunk options
  knitr::opts_chunk$set(tidy = FALSE,    # don't reformat R code
                        comment = NA,    # don't preface output with ##
                        error = FALSE)   # stop immediately on errors

  # figure directory scope
  knitr::opts_chunk$set(fig.path=paste("figure-", format, "/", sep = ""))
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


#' @rdname knitrRender
#' @export
knitrRenderPDF <- function(format, fig.width, fig.height) {

  # inherit defaults
  knitrRender(format)

  # crop
  knitr::knit_hooks$set(crop = knitr::hook_pdfcrop)

  # graphics device
  knitr::opts_chunk$set(dev = 'cairo_pdf',
                        fig.width = fig.width,
                        fig.height = fig.height)
}

