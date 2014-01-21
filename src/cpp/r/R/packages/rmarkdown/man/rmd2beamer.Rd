\name{rmd2beamer}
\alias{rmd2beamer}
\title{Convert R Markdown to Beamer}
\usage{
  rmd2beamer(input, output = NULL, theme = "default",
    envir = parent.frame(), quiet = FALSE,
    encoding = getOption("encoding"))
}
\arguments{
  \item{input}{input Rmd document}

  \item{output}{Target output file (defaults to <input>.pdf
  if not specified)}

  \item{theme}{Beamer theme}

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
  Converts an R Markdown (Rmd) file to a Beamer
  Presentation PDF
}

