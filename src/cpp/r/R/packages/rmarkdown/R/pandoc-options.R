
#' Specify pandoc conversion options
#' 
#' Functions that build a character vector of command line arguments to pass to 
#' pandoc. These options are can then be specified as the \code{options} 
#' parameter of the \code{\link{rmd2pandoc}} function.
#' 
#' @param template Full path to a custom pandoc template. The \code{--data-dir} 
#'   option is also set to the directory containing the template so that 
#'   relative references to additional assets work correctly.
#' @param toc Whether to include a table of contents in the output
#' @param mathjax URL to mathjax library used in HTML output
#' @param geometry List of \code{LaTeX} geometry settings for PDF output
#'   
#' @return Character vector of pandoc command line arguments
#'   
#' @details This function is used for creating custom rmd rendering functions. 
#'   If you are calling a higher-level function like \code{\link{rmd2pdf}} then 
#'   options are specified using explicit arguments to that function.
#'   
#'   You typically need to call only one pandoc options function, as the various
#'   format-specific functions (e.g. \code{pandocPDFOptions}) all call the 
#'   \code{pandocOptions} function as part of their implementation.
#'   
#' @export
pandocOptions <- function(template = NULL, toc = FALSE) {
  
  options <- c()
  
  if (!is.null(template)) {
    options <- c(options, 
                 "--template", template,
                 "--data-dir", dirname(template))
  }
  
  if (toc)
    options <- c(options, "--table-of-contents ")
  
  options
}


#' @rdname pandocOptions
#' @export
pandocHTMLOptions <- function(template = NULL, toc = FALSE, mathjax = NULL) {
  
  options <- pandocOptions(template, toc)
  
  options <- c(options,
               "--smart",
               "--self-contained",
               "--no-highlight")
  if (!is.null(mathjax)) {
    options <- c(options,
                 "--mathjax",
                 "--variable", paste0("mathjax-url:", mathjax))
  }
  
  options
}


#' @rdname pandocOptions
#' @export
pandocPDFOptions <- function(template = NULL, toc = FALSE, geometry = NULL) {
  
  options <- pandocOptions(template, toc)
  
  if (!is.null(geometry)) {
    for (name in names(geometry)) {
      value <- geometry[[name]]
      options <- c(options,
                   "--variable",
                   paste0("geometry:", name, "=", value))
    }
  }
  
  options
}


