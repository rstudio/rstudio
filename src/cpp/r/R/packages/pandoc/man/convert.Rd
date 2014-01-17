\name{convert}
\alias{convert}
\title{Convert a document using pandoc}
\usage{
  convert(input, output = NULL, format = NULL,
    options = c(), quiet = FALSE)
}
\arguments{
  \item{input}{input document}

  \item{output}{output document (if not specified then
  defaults to <input>.html)}

  \item{format}{format to convert to (if not specified then
  is deduced from the output file extension).}

  \item{options}{character vector of command line options
  to pass to pandoc.}

  \item{quiet}{whether to suppress messages during
  conversion}
}
\value{
  the exit code of the call to pandoc
}
\description{
  Convert a document from one type to another using pandoc.
}

