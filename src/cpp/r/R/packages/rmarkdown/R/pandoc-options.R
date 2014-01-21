
#' Specify pandoc conversion options
#'
#' Functions that build a character vector of command line arguments to pass to
#' pandoc. These options are can then be specified as the \code{options}
#' parameter of the \code{\link{rmd2pandoc}} function.
#'
#' @param template Full path to a custom pandoc template. The \code{--data-dir}
#'   option is also set to the directory containing the template so that
#'   relative references to additional assets work correctly.
#' @param options List of options to use for genering pandoc command line
#'   options
#'
#' @return Character vector of pandoc command line arguments
#'
#' @details These functions are used for creating custom rmd rendering
#'   functions. If you are calling higher-level rendering functions like
#'   \code{\link{rmd2pdf}} then options are specified using explicit arguments
#'   to those functions.
#'
#' @rdname pandocOptions
#' @export
pandocTemplateOptions <- function(template) {
  c("--template", template,
    "--data-dir", dirname(template))
}


#' @rdname pandocOptions
#' @export
pandocHTMLOptions <- function(options = htmlOptions()) {

  # base options for all HTML output
  options <- c("--smart",
               "--self-contained",
               "--no-highlight")

  # table of contents
  if (options$toc)
    options <- c(options, "--table-of-contents")

  # mathjax
  if (!is.null(options$mathjax)) {
    options <- c(options,
                 "--mathjax",
                 "--variable", paste0("mathjax-url:", options$mathjax))
  }

  options
}


#' @rdname pandocOptions
#' @export
pandocPDFOptions <- function(options = pdfOptions()) {

  # base options for all PDF output
  options <- c()

  # table of contents
  if (options$toc)
    options <- c(options, "--table-of-contents")

  # geometry
  if (!is.null(options$geometry)) {
    for (name in names(geometry)) {
      value <- geometry[[name]]
      options <- c(options,
                   "--variable",
                   paste0("geometry:", name, "=", value))
    }
  }

  options
}

#' @rdname pandocOptions
#' @export
pandocBeamerOptions <- function(options = beamerOptions) {

  # base options for all beamer output
  options <- c()

  # theme
  options <- c(options,
               "--variable", paste0("theme:", options$theme))

  options
}

