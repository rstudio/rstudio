
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
#'   \code{defaultMarkdownOptions()}.
#' @param pandoc.options additional options to pass pandoc on the command line.
#'   Defaults to \code{defaultPandocOptions()}.
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
                       markdown.options = defaultMarkdownOptions(),
                       pandoc.options = defaultPandocOptions(),
                       mathjax = mathjaxURL(),
                       envir = parent.frame(),
                       quiet = FALSE,
                       encoding = getOption("encoding")) {

  # default output to html if not provided
  if (is.null(output))
    output <- paste0(tools::file_path_sans_ext(input), ".html")

  # see if we can identify a known format (this can be used to
  # customize the knitting and pandoc conversion of the document )
  knownFormat <- knownOutputFormat(output, format)

  # knit document
  renderPandocMarkdown(knownFormat)
  md <- paste0(tools::file_path_sans_ext(input), ".md")
  knitr::knit(input, md, envir = envir, quiet = quiet, encoding = encoding)

  # build options
  options <-c("--from", paste0(markdown.options, collapse=""))
  options <- append(options, pandoc.options)

  # options for known format
  if (!is.null(knownFormat))
    options <- append(options, pandocOptionsForFormat(knownFormat, mathjax))

  # call pandoc
  pandoc(md, output, format, options, quiet)

  # return output filename
  invisible(output)
}


#' @export
#' @rdname rmd2pandoc
defaultMarkdownOptions <- function() {
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
defaultPandocOptions <- function() {
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
mathjaxURL <- function(version = "latest",
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
renderPandocMarkdown <- function(format = NULL) {

  if (!is.null(format) && ! (format %in% knownOutputFormats()))
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

pandocOptionsForFormat <- function(format, mathjax) {
  if (identical(format, "html")) {
    templateDir <- rmarkdownSystemFile("templates/html", package = "rmarkdown")
    options <- c("--template", file.path(templateDir, "default.html"),
                 "--data-dir", templateDir,
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

knownOutputFormat <- function(output, format = NULL) {

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
  } else if (format %in% knownOutputFormats()) {
    format
  } else {
    NULL
  }
}


knownOutputFormats <- function() {
  c("html", "docx", "latex", "beamer")
}


