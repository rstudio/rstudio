\name{rmd2pdf}
\alias{pdfGeometry}
\alias{pdfOptions}
\alias{rmd2pdf}
\title{Convert R Markdown to PDF}
\usage{
  rmd2pdf(input, output = NULL, options = pdfOptions(),
    envir = parent.frame(), quiet = FALSE,
    encoding = getOption("encoding"))

  pdfOptions(toc = FALSE, geometry = pdfGeometry())

  pdfGeometry(margin = "1in", ...)
}
\arguments{
  \item{input}{Input Rmd document}

  \item{output}{Target output file (defaults to <input>.pdf
  if not specified)}

  \item{options}{List of PDF rendering options created by
  calling \code{pdfOptions}}

  \item{toc}{Whether to include a table of contents in the
  output}

  \item{geometry}{List of LaTeX geometry options created by
  calling \code{pdfGeometry}}

  \item{margin}{Size of page margins}

  \item{envir}{The environment in which the code chunks are
  to be evaluated (can use \code{\link{new.env}()} to
  guarantee an empty new environment)}

  \item{quiet}{Whether to suppress the progress bar and
  messages}

  \item{encoding}{The encoding of the input file; see
  \code{\link{file}}}
}
\value{
  The compiled document is written into the output file,
  and the path of the output file is returned.
}
\description{
  Converts an R Markdown (Rmd) file to PDF
}

