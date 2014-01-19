\name{pandocOutputFile}
\alias{pandocOutputFile}
\title{Determine the output file for a pandoc conversion}
\usage{
  pandocOutputFile(input, pandocFormat)
}
\arguments{
  \item{input}{Input file}

  \item{pandocFormat}{Pandoc format of the output}
}
\value{
  Output file
}
\description{
  Give an input file and pandoc format (e.g. \code{html},
  \code{html5}, \code{docx}, \code{latex}, \code{beamer})
  determine the name of the output file to write.
}

