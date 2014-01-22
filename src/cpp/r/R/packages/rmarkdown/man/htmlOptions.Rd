\name{htmlOptions}
\alias{htmlOptions}
\alias{htmlTemplate}
\alias{mathjaxURL}
\title{Options for HTML Conversion}
\usage{
  htmlOptions(toc = FALSE, toc.depth = 3,
    template = htmlTemplate())

  htmlTemplate(bootstrap = TRUE, highlight = TRUE,
    mathjax = mathjaxURL(), css = NULL,
    include.header = NULL, include.before = NULL,
    include.after = NULL)

  mathjaxURL()
}
\arguments{
  \item{toc}{\code{TRUE} to include a table of contents in
  the output}

  \item{toc.depth}{Depth of headers to include in table of
  contents}

  \item{template}{HTML template to use for rendering
  document. This should either be the path to a pandoc
  template or an object that provides a
  \code{pandocOptions} S3 method. Pass \code{NULL} to
  create an HTML fragment rather than a full document.}

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
}
\value{
  A list of HTML options that can be passed to
  \code{\link{rmd2html}}.
}
\description{
  Define the options for converting R Markdown to HTML.
}
\details{
  The \code{htmlTemplate} function provides a default
  template that includes Bootstrap CSS, syntax
  highlighting, and MathJax. Additional css and header and
  footer content can also be included, and the resulting
  HTML file is fully standalone.

  Paths for resources referenced from the \code{css},
  \code{include.header}, \code{include.before}, and
  \code{include.after} parameters are resolved relative to
  the directory of the input document.
}

