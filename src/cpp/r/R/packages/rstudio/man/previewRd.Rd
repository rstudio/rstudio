\name{previewRd}
\alias{previewRd}

\title{
Preview an Rd topic in the Help pane
}
\description{
Preview an Rd topic in the Help pane
}
\note{
The \code{previewRd} function was added in version 0.98.191 of RStudio.
}

\usage{
previewRd(rdFile)
}
\arguments{
  \item{rdFile}{Single element character vector containing the name of the Rd file to be displayed}
}

\examples{
\dontrun{
rstudio::previewRd("~/MyPackage/man/foo.Rd")
}
}