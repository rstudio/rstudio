


# In RStudio the default rmarkdown engine for a document is v2
#
# To specify v1 use:
#
# <!-- rmarkdown 1.0 -->
#
# To specify an alternate engine use a function name:
#
# <!-- rmarkdown pubtools::rmd2foo -->
#
# Functions must accept an input and encoding argument, and must
# return the name of the output file generated
#
# To pass arguments to the function you do this:
#
# <!-- rmarkdown pubtools::rmd2foo format="pdf", toc=TRUE -->
#

#' Convert an Rmd file to another type using pandoc
#'
#' Converts R Markdown (Rmd) files to a variety of formats using the pandoc
#' rendering engine.
#'
#' @param input input Rmd document
#' @param output target output file (defaults to <input>.html if not specified)
#' @param format format to convert to. If not specified then is deduced from the
#'   output file extension.
#' @param markdown.options options that control the dialect of markdown used by
#'   pandoc in creating the output file. Defaults to
#'   \code{rmd_markdown_options()}.
#' @param pandoc.options additional options to pass pandoc on the command line.
#'   Defaults to \code{rmd_pandoc_options()}.
#' @param quiet whether to suppress the progress bar and messages
#' @param envir the environment in which the code chunks are to be evaluated
#'   (can use \code{\link{new.env}()} to guarantee an empty new environment)
#' @param encoding the encoding of the input file; see \code{\link{file}}
#' @return The compiled document is written into the output file, and the path
#'   of the output file is returned.
#'
#' @export
rmd2pandoc <- function(input,
                       output = NULL,
                       format = NULL,
                       markdown.options = rmd_markdown_options(),
                       pandoc.options = rmd_pandoc_options(),
                       envir = parent.frame(),
                       quiet = FALSE,
                       encoding = getOption("encoding")) {

  # default output to html if not provided
  if (is.null(output))
    output <- paste0(tools::file_path_sans_ext(input), ".html")

  # knit document
  render_pandoc_markdown(output, format)
  md <- paste0(tools::file_path_sans_ext(input), ".md")
  knitr::knit(input, md, envir = envir, quiet = quiet, encoding = encoding)

  # build options
  options <-c("--from", paste0(markdown.options, collapse=""))
  options <- append(options, pandoc.options)

  # call pandoc
  convert(md, output, format, options, quiet)

  # return output filename
  invisible(output)
}

#' @export
#' @rdname rmd2pandoc
rmd_markdown_options <- function() {
  c("markdown_github",
    "-hard_line_breaks",
    "+superscript",
    "+tex_math_dollars",
    "+raw_html",
    "+auto_identifiers",
    "+raw_tex",
    "+latex_macros",
    "+implicit_figures",
    "+footnotes",
    "+inline_notes",
    "+citations",
    "+pandoc_title_block",
    "+yaml_metadata_block")
}

#' @export
#' @rdname rmd2pandoc
rmd_pandoc_options <- function() {
  c("--smart")
}


