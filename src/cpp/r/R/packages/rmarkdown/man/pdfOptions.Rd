\name{pdfOptions}
\alias{pdfOptions}
\title{Options for PDF conversion}
\usage{
  pdfOptions(toc = FALSE, toc.depth = 2, margin = "1in")
}
\arguments{
  \item{toc}{\code{TRUE} to include a table of contents in
  the output}

  \item{toc.depth}{Depth of headers to include in table of
  contents}

  \item{margin}{Size of page margins}
}
\value{
  A list of PDF options that can be passed to
  \code{\link{rmd2pdf}}.
}
\description{
  Define the options for converting R Markdown to PDF.
}

