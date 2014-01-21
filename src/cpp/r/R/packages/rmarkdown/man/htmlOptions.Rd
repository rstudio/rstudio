\name{htmlOptions}
\alias{htmlOptions}
\alias{mathjaxURL}
\title{Options for HTML conversion}
\usage{
  htmlOptions(toc = FALSE, toc.depth = 3,
    theme = "default", highlight = "default",
    mathjax = mathjaxURL(), css = NULL,
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

  \item{theme}{HTML theme ("default", "cerulean", or
  "slate"). Pass \code{NULL} to not use any theme (add your
  own css using the \code{css} parameter).}

  \item{highlight}{Syntax highlighting style ("default",
  "pygments", "kate", "monochrome", "espresso", "zenburn",
  "haddock", or "tango"). Pass \code{NULL} to not syntax
  highlight code.}

  \item{mathjax}{Include mathjax from the specified URL.
  Pass \code{NULL} to not include mathjax.}

  \item{css}{One or more css files to include}

  \item{include.header}{One or more files that include
  content to be inclued within the HTML \code{head}.}

  \item{include.before}{One or more files with content to
  be included at the beginning of the HTML \code{body}.}

  \item{include.after}{One or more files with content to be
  inserted at the end of the HTML \code{body}.}

  \item{standalone}{\code{TRUE} to produce a fully valid
  HTML document (rather than a fragment). If this is
  \code{FALSE} then the \code{theme}, \code{highlight},
  \code{mathjax}, \code{css}, and content inclusion options
  are not applied.}

  \item{self.contained}{\code{TRUE} to produce a standalone
  HTML file with no external dependencies, using data: URIs
  to incorporate the contents of linked scripts,
  stylesheets, and images (note that MathJax is still
  referenced externally). If this is \code{FALSE} then the
  \code{theme} and \code{highlight} options are not
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

