\name{rmd2pdf}
\alias{rmd2pdf}
\title{Convert R Markdown to PDF}
\usage{
  rmd2pdf(input, output = NULL, quiet = FALSE,
    encoding = getOption("encoding"))
}
\arguments{
  \item{input}{input Rmd document}

  \item{output}{target output file (defaults to <input>.pdf
  if not specified)}

  \item{quiet}{whether to suppress the progress bar and
  messages}

  \item{encoding}{the encoding of the input file; see
  \code{\link{file}}}
}
\value{
  The compiled document is written into the output file,
  and the path of the output file is returned.
}
\description{
  Converts an R Markdown (Rmd) file to PDF
}

