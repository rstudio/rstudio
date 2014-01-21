\name{rmd2pandoc}
\alias{rmd2pandoc}
\title{Knit and convert an R Markdown document}
\usage{
  rmd2pandoc(input, to, output = NULL, options = NULL,
    envir = parent.frame(), quiet = FALSE,
    encoding = getOption("encoding"))
}
\arguments{
  \item{input}{Input file}

  \item{to}{Pandoc format to convert to}

  \item{output}{Output file (if not specified then a
  default based on the specified \code{to} format is
  chosen)}

  \item{options}{Command line options to pass to pandoc.
  This should either be a character vector of literal
  command line options or an object that provices a
  \code{pandocOptions} S3 method which yields the options.}

  \item{envir}{The environment in which the code chunks are
  to be evaluated (can use \code{\link{new.env}()} to
  guarantee an empty new environment)}

  \item{quiet}{\code{TRUE} to supress printing of the
  pandoc command line}

  \item{encoding}{the encoding of the input file; see
  \code{\link{file}}}
}
\value{
  The compiled document is written into the output file,
  and the path of the output file is returned.
}
\description{
  Knit the specified R Markdown input file and convert it
  to final output using pandoc.
}
\details{
  Typically one of the \code{\link{knitrRender}} functions
  is called prior to calling \code{knit2pandoc} to optimize
  knitr rendering for the intended output format.
}

