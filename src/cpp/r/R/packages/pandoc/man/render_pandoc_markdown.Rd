\name{render_pandoc_markdown}
\alias{render_pandoc_markdown}
\title{Set knitr output hooks for pandoc markdown}
\usage{
  render_pandoc_markdown(output, format = NULL)
}
\arguments{
  \item{output}{target output file}

  \item{format}{target output format. If not specified then
  is deduced from the output file extension.}
}
\description{
  This function sets the built in output hooks for
  rendering to pandoc markdown. Output hooks are based on
  the target output format, which is deduced from the file
  extension (or alternatively via an explicit format
  argument).
}

