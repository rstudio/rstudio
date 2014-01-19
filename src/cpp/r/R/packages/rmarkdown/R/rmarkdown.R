
#' Convert R Markdown to HTML
#'
#' Converts an R Markdown (Rmd) file to HTML
#'
#' @param input input Rmd document
#' @param output target output file (defaults to <input>.html if not specified)
#' @param mathjax include mathjax from the specified url (pass NULL to
#' not include mathjax)
#' @param quiet whether to suppress the progress bar and messages
#' @param encoding the encoding of the input file; see \code{\link{file}}
#' @return The compiled document is written into the output file, and the path
#'   of the output file is returned.
#'
#' @export
rmd2html <- function(input,
                     output = NULL,
                     mathjax = mathjaxURL(),
                     quiet = FALSE,
                     encoding = getOption("encoding")) {

  # format and output file
  pandocFormat <- "html"
  if (is.null(output))
    output <- pandocOutputFile(input, pandocFormat)

  # knitr options
  knitrRenderMarkdown(pandocFormat)
  knitr::opts_chunk$set(dev = 'png',
                        fig.width = 7,
                        fig.height = 7)

  # call knitr
  md <- knitr::knit(input, quiet = quiet, encoding = encoding)

  # pandoc options
  options <- c(pandocMarkdownOptions(),
               pandocHTMLOptions(systemFile("templates/html/default.html"),
                                 mathjax),
               recursive = TRUE)

  # call pandoc
  pandoc(md, output, pandocFormat, options, quiet)
}


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
  pandoc(md, output, pandocFormat, options, quiet)
}


#' Determine the output file for a pandoc conversion
#'
#' Give an input file and pandoc format (e.g. \code{html}, \code{html5},
#' \code{docx}, \code{latex}, \code{beamer}) determine the name of the
#' output file to write.
#'
#' @param input Input file
#' @param pandocFormat Pandoc format of the output
#'
#' @return Output file
#'
#' @export
pandocOutputFile <- function(input, pandocFormat) {
  if (pandocFormat %in% c("latex", "beamer"))
    ext <- ".pdf"
  else if (pandocFormat %in% c("html", "html5", "revealjs"))
    ext <- ".html"
  else
    ext <- paste0(".", pandocFormat)
  paste0(tools::file_path_sans_ext(input), ext)
}



#' Set knitr hooks and options for rendering markdown
#'
#' This function sets knitr hooks and options for markdown rendering. Hooks are
#' based on the default
#' \code{\link[knitr:render_markdown]{knitr::render_markdown}} function with
#' some additional tweaks.
#'
#' @param figureScope Directory name scope for figures (useful when rendering
#'   more than one format from a directory)
#'
#' @export
knitrRenderMarkdown <- function(figureScope = NULL) {

  # stock markdown options
  knitr::render_markdown()

  # chunk options
  knitr::opts_chunk$set(tidy = FALSE,    # don't reformat R code
                        comment = NA,    # don't preface output with ##
                        error = FALSE)   # stop immediately on errors

  # figure directory scope if requested
  if (!is.null(figureScope))
    knitr::opts_chunk$set(fig.path=paste("figure-", figureScope, "/", sep = ""))
}


#' Compose pandoc options
#'
#' Convenince functions for composing sets of pandoc options. These functions
#' are used by higher-level rmarkdown renderig functions like
#' \code{\link{rmd2html}} and can be used to define additional custom renderers.
#'
#' @param extraOptions Additional flags for customizing the flavor of
#' markdown input interpreted by pandoc.
#' @param template Full path to a custom pandoc template
#' @param mathjax URL to mathjax library used in HTML output
#'
#' @return Character vector of pandoc options
#'
#' @rdname pandocOptions
#' @export
pandocMarkdownOptions <- function(extraOptions = NULL) {
  c("--from",
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
           "+yaml_metadata_block",
           extraOptions),
    "--smart")
}

#' @rdname pandocOptions
#' @export
pandocHTMLOptions <- function(template, mathjax = NULL) {
  options <- c("--template", template,
               "--data-dir", dirname(template),
               "--self-contained",
               "--no-highlight")
  if (!is.null(mathjax)) {
    options <- c(options,
                 "--mathjax",
                 "--variable", paste0("mathjax-url:", mathjax),
                 recursive = TRUE)
  }
  options
}

#' @rdname pandocOptions
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

#' MathJax URL
#'
#' Get the URL to the MathJax library.
#'
#' @param version Version to use
#' @param config Configuration to use
#' @param https Use secure connection
#'
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
