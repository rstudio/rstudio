

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
#' @param mathjax include mathjax from the specified url (pass NULL to
#' not include mathjax)
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
                       mathjax = mathjax_url(),
                       envir = parent.frame(),
                       quiet = FALSE,
                       encoding = getOption("encoding")) {

  # default output to html if not provided
  if (is.null(output))
    output <- paste0(tools::file_path_sans_ext(input), ".html")

  # see if we can identify a known format (this can be used to
  # customize the knitting and pandoc conversion of the document )
  known_format <- known_output_format(output, format)

  # knit document
  render_pandoc_markdown(known_format)
  md <- paste0(tools::file_path_sans_ext(input), ".md")
  knitr::knit(input, md, envir = envir, quiet = quiet, encoding = encoding)

  # build options
  options <-c("--from", paste0(markdown.options, collapse=""))
  options <- append(options, pandoc.options)

  # options for known format
  if (!is.null(known_format))
    options <- append(options, pandoc_options_for_format(known_format, mathjax))

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
    "+footnotes",
    "+inline_notes",
    "+citations",
    "+yaml_metadata_block")
}

#' @export
#' @rdname rmd2pandoc
rmd_pandoc_options <- function() {
  c("--smart")
}

#' MathJax URL
#'
#' Get the URL to the MathJax library.
#'
#' @param version version to use
#' @param config configuration to use
#' @param https use secure connection
#' @return URL to MathJax library
#'
#' @export
mathjax_url <- function(version = "latest",
                        config = "TeX-AMS-MML_HTMLorMML",
                        https = FALSE) {
  if (https)
    baseurl <- "https://c328740.ssl.cf1.rackcdn.com/mathjax"
  else
    baseurl <- "http://cdn.mathjax.org/mathjax"

  paste0(baseurl, "/", version, "/MathJax.js?config=", config)
}


#' Set knitr output hooks for pandoc markdown
#'
#' This function sets the built in output hooks for rendering to pandoc
#' markdown. Output hooks are based on the default
#' \code{\link[knitr:render_markdown]{knitr::render_markdown}} function with
#' additional hooks provided for known formats.
#'
#' @param format one of the known output formats ( \code{html}, \code{docx},
#'   \code{latex}, and \code{beamer}) or \code{NULL}.
#'
#' @export
render_pandoc_markdown <- function(format = NULL) {

  if (!is.null(format) && ! (format %in% known_output_formats()))
    stop("Unknown output format specified")

  # stock markdown options
  knitr::render_markdown()

  # chunk options (do a figure directory per-format to gracefully handle
  # switching between formats)
  if (is.null(format))
    format <- "unknown"
  knitr::opts_chunk$set(tidy = FALSE,
                        error = FALSE,
                        fig.path=paste("figure-", format, "/", sep = ""))

  # some pdf specific options
  if (format %in% c("latex", "beamer")) {
    knitr::opts_chunk$set(dev = 'cairo_pdf')
    knitr::knit_hooks$set(crop = knitr::hook_pdfcrop)
  }

  invisible(NULL)
}

pandoc_options_for_format <- function(format, mathjax) {
  if (identical(format, "html")) {
    template_dir <- system.file("templates/html", package = "pandoc")
    options <- c("--template", file.path(template_dir, "default.html"),
                 "--data-dir", template_dir,
                 "--self-contained",
                 "--no-highlight")
    if (!is.null(mathjax)) {
      options <- append(options,
        c("--mathjax",
          paste0("--variable=mathjax-url:", mathjax)
        )
      )
    }
    options
  } else {
    NULL
  }
}

known_output_format <- function(output, format = NULL) {

  if (is.null(format)) {
    ext <- tools::file_ext(output)
    if (ext %in% c("htm", "html"))
      "html"
    else if (identical(ext, "docx"))
      "docx"
    else if (identical(ext, "pdf"))
      "latex"
    else
      NULL
  } else if (format %in% known_output_formats()) {
    format
  } else {
    NULL
  }
}


known_output_formats <- function() {
  c("html", "docx", "latex", "beamer")
}


