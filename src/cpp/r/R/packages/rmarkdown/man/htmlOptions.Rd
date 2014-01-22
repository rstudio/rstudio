\name{htmlOptions}
\alias{htmlOptions}
\alias{mathjaxURL}
\title{Options for HTML conversion}
\usage{
  htmlOptions(toc = FALSE, toc.depth = 3, bootstrap = TRUE,
    highlight = TRUE, mathjax = mathjaxURL(), css = NULL,
    include.header = NULL, include.before = NULL,
    include.after = NULL, standalone = TRUE,
    self.contained = TRUE)

  mathjaxURL()
}
\arguments{
  \item{toc}{\code{TRUE} to include a table of contents in
  the output}

  \item{toc.depth}{Depth of headers to include in table of
  contents}

  \item{bootstrap}{\code{TRUE} to style the document using
  \href{http://getbootstrap.com}{Bootstrap}. If you pass
  \code{FALSE} you can provide your own styles using the
  \code{css} and/or \code{include.header} parameters.}

  \item{highlight}{\code{TRUE} to syntax highlight R code
  within the document.}

  \item{mathjax}{Include mathjax from the specified URL.
  Pass \code{NULL} to not include mathjax.}

  \item{css}{One or more css files to include}

  \item{include.header}{One or more files with HTML content
  to be included within the HTML \code{head} tag.}

  \item{include.before}{One or more files with HTML content
  to be included before the document body.}

  \item{include.after}{One or more files with HTML content
  to be included after the document body.}

  \item{standalone}{\code{TRUE} to produce a fully valid
  HTML document (rather than a fragment). If this is
  \code{FALSE} then the \code{bootstrap}, \code{highlight},
  \code{css}, and \code{include} options are not applied.}

  \item{self.contained}{\code{TRUE} to produce a standalone
  HTML file with no external dependencies, using data: URIs
  to incorporate the contents of linked scripts,
  stylesheets, and images (note that MathJax is still
  referenced externally). If this is \code{FALSE} then the
  \code{bootstrap} and \code{highlight} options are not
  applied.}
}
\value{
  A list of HTML options that can be passed to
  \code{\link{rmd2html}}.
}
\description{
  Define the options for converting R Markdown to HTML.
}
\details{
  Paths for resources referenced from the \code{css},
  \code{include.header}, \code{include.before}, and
  \code{include.after} parameters are resolved relative to
  the directory of the input document.
}

