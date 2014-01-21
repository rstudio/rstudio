\name{pandocTemplateOptions}
\alias{pandocBeamerOptions}
\alias{pandocHTMLOptions}
\alias{pandocPDFOptions}
\alias{pandocTemplateOptions}
\title{Specify pandoc conversion options}
\usage{
  pandocTemplateOptions(template)

  pandocHTMLOptions(options = htmlOptions())

  pandocPDFOptions(options = pdfOptions())

  pandocBeamerOptions(options = beamerOptions)
}
\arguments{
  \item{template}{Full path to a custom pandoc template.
  The \code{--data-dir} option is also set to the directory
  containing the template so that relative references to
  additional assets work correctly.}

  \item{options}{List of options to use for genering pandoc
  command line options}
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
  These functions are used for creating custom rmd
  rendering functions. If you are calling higher-level
  rendering functions like \code{\link{rmd2pdf}} then
  options are specified using explicit arguments to those
  functions.
}

