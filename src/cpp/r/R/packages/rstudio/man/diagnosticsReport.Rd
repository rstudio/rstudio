\name{diagnosticsReport}
\alias{diagnosticsReport}
\title{
Write an RStudio Diagnostics Report
}
\description{
Write a diagnostics report to assist in troubleshooting problems with RStudio. The report is written to ~/rstudio-diagnostics and contains information on the R session and environment as well as copies of the RStudio log files.
}
\usage{
diagnosticsReport()
}

\examples{
\dontrun{
rstudio::diagnosticsReport()
}
}