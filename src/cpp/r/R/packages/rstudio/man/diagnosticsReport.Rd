\name{diagnosticsReport}
\alias{diagnosticsReport}
\title{
Write an RStudio Diagnostics Report
}
\description{
Write a diagnostics report to assist in troubleshooting problems with RStudio. The report is written to ~/rstudio-diagnostics and contains information on the R session and environment as well as copies of the RStudio log files.
}
\note{
The \code{diagnosticsReport} function was added in version 0.97.139 of RStudio.
}
\usage{
diagnosticsReport()
}

\examples{
\dontrun{
rstudio::diagnosticsReport()
}
}