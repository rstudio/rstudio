\name{rmd2pandoc}
\alias{defaultMarkdownOptions}
\alias{defaultPandocOptions}
\alias{rmd2pandoc}
\title{Convert an Rmd file to another type using pandoc}
\usage{
  rmd2pandoc(input, output = NULL, format = NULL,
    markdown.options = defaultMarkdownOptions(),
    pandoc.options = defaultPandocOptions(),
    mathjax = mathjaxURL(), envir = parent.frame(),
    quiet = FALSE, encoding = getOption("encoding"))

  defaultMarkdownOptions()

  defaultPandocOptions()
}
\arguments{
  \item{input}{input Rmd document}

  \item{output}{target output file (defaults to
  <input>.html if not specified)}

  \item{format}{format to convert to. If not specified then
  is deduced from the output file extension.}

  \item{markdown.options}{options that control the dialect
  of markdown used by pandoc in creating the output file.
  Defaults to \code{defaultMarkdownOptions()}.}

  \item{pandoc.options}{additional options to pass pandoc
  on the command line.  Defaults to
  \code{defaultPandocOptions()}.}

  \item{mathjax}{include mathjax from the specified url
  (pass NULL to not include mathjax)}

  \item{quiet}{whether to suppress the progress bar and
  messages}

  \item{envir}{the environment in which the code chunks are
  to be evaluated (can use \code{\link{new.env}()} to
  guarantee an empty new environment)}

  \item{encoding}{the encoding of the input file; see
  \code{\link{file}}}
}
\value{
  The compiled document is written into the output file,
  and the path of the output file is returned.
}
\description{
  Converts R Markdown (Rmd) files to a variety of formats
  using the pandoc rendering engine.
}

