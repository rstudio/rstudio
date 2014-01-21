\name{rmd2html}
\alias{htmlOptions}
\alias{mathjaxURL}
\alias{rmd2html}
\title{Convert R Markdown to HTML}
\usage{
  rmd2html(input, output = NULL, options = htmlOptions(),
    envir = parent.frame(), quiet = FALSE,
    encoding = getOption("encoding"))

  htmlOptions(toc = FALSE, toc.depth = 3,
    theme = "default", highlight = "default",
    mathjax = mathjaxURL())

  mathjaxURL()
}
\arguments{
  \item{input}{Input Rmd document}

  \item{output}{Target output file (defaults to
  <input>.html if not specified)}

  \item{options}{List of HTML rendering options created by
  calling \code{htmlOptions}}

  \item{toc}{Whether to include a table of contents in the
  output}

  \item{toc.depth}{Depth of headers to include in table of
  contents}

  \item{theme}{HTML theme ("default", "cerulean", or
  "slate")}

  \item{mathjax}{Include mathjax from the specified URL
  (pass NULL to not include mathjax)}

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

