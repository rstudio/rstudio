\name{render_pandoc_markdown}
\alias{render_pandoc_markdown}
\title{Set knitr output hooks for pandoc markdown}
\usage{
  render_pandoc_markdown(format = NULL)
}
\arguments{
  \item{format}{one of the known output formats (
  \code{html}, \code{docx}, \code{latex}, and
  \code{beamer}) or \code{NULL}.}
}
\description{
  This function sets the built in output hooks for
  rendering to pandoc markdown. Output hooks are based on
  the default
  \code{\link[knitr:render_markdown]{knitr::render_markdown}}
  function with additional hooks provided for known
  formats.
}

