\name{pdfOptions}
\alias{pdfGeometry}
\alias{pdfOptions}
\alias{pdfTemplate}
\title{Options for PDF conversion}
\usage{
  pdfOptions(toc = FALSE, toc.depth = 2,
    number.sections = FALSE, highlight = TRUE,
    template = pdfTemplate())

  pdfTemplate(geometry = pdfGeometry(),
    highlight.style = "pygments", include.header = NULL,
    include.before = NULL, include.after = NULL)

  pdfGeometry(margin = "1in", ...)
}
\arguments{
  \item{toc}{\code{TRUE} to include a table of contents in
  the output}

  \item{toc.depth}{Depth of headers to include in table of
  contents}

  \item{number.sections}{\code{TRUE} Number section
  headings}

  \item{highlight}{\code{TRUE} to syntax highlight code
  (see \code{highlight.style} for customizing the rendering
  of highlighted code).}

  \item{template}{LaTeX template to use for rendering
  document. This should either be the path to a pandoc
  template or an object that provides a
  \code{pandocOptions} S3 method.}

  \item{geometry}{List containing LaTeX geometry options
  for the document.}

  \item{margin}{Size of page margins}

  \item{highlight.style}{Style for syntax highlighting.
  Options are pygments, kate, monochrome, espresso,
  zenburn, haddock, and tango.}

  \item{include.header}{One or more files with LaTeX
  content to be included in the header of the document.}

  \item{include.before}{One or more files with LaTeX
  content to be included before the document body.}

  \item{include.after}{One or more files with LaTeX content
  to be included after the document body.}
}
\value{
  A list of PDF options that can be passed to
  \code{\link{rmd2pdf}}.
}
\description{
  Define the options for converting R Markdown to PDF.
}
\details{
  The \code{pdfTemplate} function provides the default
  pandoc LaTeX PDF template along with the abilty to add
  additional LaTeX to the beginning and end of the
  document.

  Paths for resources referenced from the
  \code{include.header}, \code{include.before}, and
  \code{include.after} parameters are resolved relative to
  the directory of the input document.
}

