\name{htmlOptions}
\alias{htmlOptions}
\alias{mathjaxURL}
\title{Options for HTML conversion}
\usage{
  htmlOptions(toc = FALSE, toc.depth = 3,
    theme = "default", highlight = "default",
    mathjax = mathjaxURL(), css = NULL,
    include.header = NULL, include.before = NULL,
    include.after = NULL)

  mathjaxURL()
}
\arguments{
  \item{toc}{Whether to include a table of contents in the
  output}

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

  \item{css}{One or more css files to include (paths
  relative to the location of the input document)}

  \item{include.header}{One more files that include content
  to be inserted into the HTML \code{head}.}

  \item{include.before}{One more files that include content
  to be inserted at the beginning of the HTML \code{body}.}

  \item{include.after}{One more files that include content
  to be inserted at the end of the HTML \code{body}.}
}
\value{
  A list of HTML options that can be passed to
  \code{\link{rmd2html}}.
}
\description{
  Define the options for converting R Markdown to HTML.
}

