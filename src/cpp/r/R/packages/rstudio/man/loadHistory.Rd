\name{loadHistory}
\alias{loadHistory}
\alias{saveHistory}

\title{Load or Save the Command History}

\description{Load or save the command history.}
\usage{
loadHistory(file = ".Rhistory")
saveHistory(file = ".Rhistory")
}

\arguments{
  \item{file}{The name of the file in which to save the history, or from which to load it. The path is relative to the current working directory.
}
}
\details{
The maximum number of entries saved in history files is controlled by the \code{R_HISTSIZE} envrionment variable (the default is 512).
}
