\name{rmd2html}
\alias{rmd2html}
\title{Convert R Markdown to HTML}
\usage{
  rmd2html(input, output = NULL, options = htmlOptions(),
    envir = parent.frame(), quiet = FALSE,
    encoding = getOption("encoding"))
}
\arguments{
  \item{input}{Input Rmd document}

  \item{output}{Target output file (defaults to
  <input>.html if not specified)}

  \item{options}{List of HTML rendering options created by
  calling \code{\link{htmlOptions}}}

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
  Converts an R Markdown (Rmd) file to HTML
}

