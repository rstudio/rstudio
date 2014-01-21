\name{rmd2html}
\alias{rmd2html}
\title{Convert R Markdown to HTML}
\usage{
  rmd2html(input, output = NULL, mathjax = mathjaxURL(),
    quiet = FALSE, encoding = getOption("encoding"))
}
\arguments{
  \item{input}{input Rmd document}

  \item{output}{target output file (defaults to
  <input>.html if not specified)}

  \item{mathjax}{include mathjax from the specified url
  (pass NULL to not include mathjax)}

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
  Converts an R Markdown (Rmd) file to HTML
}

