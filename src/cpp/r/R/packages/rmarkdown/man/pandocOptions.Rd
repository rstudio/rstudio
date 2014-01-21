\name{pandocOptions}
\alias{pandocHTMLOptions}
\alias{pandocOptions}
\alias{pandocPDFOptions}
\title{Specify pandoc conversion options}
\usage{
  pandocOptions(template = NULL, toc = FALSE)

  pandocHTMLOptions(template = NULL, toc = FALSE,
    mathjax = NULL)

  pandocPDFOptions(template = NULL, toc = FALSE,
    geometry = NULL)
}
\arguments{
  \item{template}{Full path to a custom pandoc template.
  The \code{--data-dir} option is also set to the directory
  containing the template so that relative references to
  additional assets work correctly.}

  \item{toc}{Whether to include a table of contents in the
  output}

  \item{mathjax}{URL to mathjax library used in HTML
  output}

  \item{geometry}{List of \code{LaTeX} geometry settings
  for PDF output}
}
\value{
  Character vector of pandoc command line arguments
}
\description{
  Functions that build a character vector of command line
  arguments to pass to pandoc. These options are can then
  be specified as the \code{options} parameter of the
  \code{\link{rmd2pandoc}} function.
}
\details{
  This function is used for creating custom rmd rendering
  functions.  If you are calling a higher-level function
  like \code{\link{rmd2pdf}} then options are specified
  using explicit arguments to that function.

  You typically need to call only one pandoc options
  function, as the various format-specific functions (e.g.
  \code{pandocPDFOptions}) all call the
  \code{pandocOptions} function as part of their
  implementation.
}

