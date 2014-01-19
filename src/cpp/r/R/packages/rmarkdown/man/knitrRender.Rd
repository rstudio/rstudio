\name{knitrRenderHTML}
\alias{knitrRender}
\alias{knitrRenderHTML}
\alias{knitrRenderPDF}
\title{Set knitr hooks and options for rendering markdown}
\usage{
  knitrRenderHTML(format, fig.width, fig.height)

  knitrRender(format)

  knitrRenderPDF(format, fig.width, fig.height)
}
\arguments{
  \item{format}{Pandoc format being rendered (used to
  create distinct figure directories for multiple formats)}

  \item{fig.width}{Default width for figures}

  \item{fig.height}{Default height for figures}
}
\description{
  These functions set knitr hooks and options for markdown
  rendering. Hooks are based on the default
  \code{\link[knitr:render_markdown]{knitr::render_markdown}}
  function with additional customization for various output
  formats.
}
\details{
  You typically need to call only one knitr render
  function, as the various format-specific functions (e.g.
  \code{knitrRenderPDF}) all call the \code{knitrRender}
  function as part of their implementation.
}

