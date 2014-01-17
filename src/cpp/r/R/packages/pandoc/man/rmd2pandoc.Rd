\name{rmd2pandoc}
\alias{rmd2pandoc}
\alias{rmd_markdown_options}
\alias{rmd_pandoc_options}
\title{Convert an Rmd file to another type using pandoc}
\usage{
  rmd2pandoc(input, output = NULL, format = NULL,
    markdown.options = rmd_markdown_options(),
    pandoc.options = rmd_pandoc_options(),
    envir = parent.frame(), quiet = FALSE,
    encoding = getOption("encoding"))

  rmd_markdown_options()

  rmd_pandoc_options()
}
\arguments{
  \item{input}{input Rmd document}

  \item{output}{target output file (defaults to
  <input>.html if not specified)}

  \item{format}{format to convert to. If not specified then
  is deduced from the output file extension.}

  \item{markdown.options}{options that control the dialect
  of markdown used by pandoc in creating the output file.
  Defaults to \code{rmd_markdown_options()}.}

  \item{pandoc.options}{additional options to pass pandoc
  on the command line.  Defaults to
  \code{rmd_pandoc_options()}.}

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

