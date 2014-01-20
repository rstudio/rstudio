\name{rmd2pandoc}
\alias{rmd2pandoc}
\title{Convert an R Markdown document using pandoc}
\usage{
  rmd2pandoc(input, output = NULL, from = rmdFormat(),
    to = "html", options = NULL, quiet = FALSE,
    encoding = getOption("encoding"))
}
\arguments{
  \item{input}{Input file}

  \item{output}{Output file (if not specified then a
  default based on the specified format is chosen)}

  \item{from}{Options determining the flavor of markdown
  supported by the conversion}

  \item{to}{Pandoc format to convert to (defaults to HTML
  if not specified)}

  \item{options}{Command line options to pass to pandoc}

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
  Convert the \code{input} document to the specified
  \code{format} and write it to \code{output}.
}

