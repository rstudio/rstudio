\name{viewApp}
\alias{viewApp}

\title{
View a local web application within RStudio
}
\description{
View a local web application within the RStudio IDE. This is useful for running \link[shiny:shiny-package]{Shiny}, \link[Rook:Rook-package]{Rook}, \link[opencpu:opencpu]{OpenCPU}, or any other type of local web application, including ones served from static files in the R session temporary directory.
}
\usage{
viewApp(url)
}
\arguments{
  \item{url}{Application URL. This can be either a localhost URL or a path to a file within the R session temporary directory (i.e. a path returned by \code{\link[base:tempfile]{tempfile}})}
}

\details{
RStudio also sets the global \code{viewapp} option to the \code{rstudio::viewApp} function so that it can be invoked in front-end independent manner.

Applications are displayed within the Viewer pane. The application URL must either be served from localhost or be a path to a file within the R session temporary directory.
}

\examples{
\dontrun{

# run an application inside the IDE
rstudio::viewApp("http://localhost:8100")

# attempt to run an application inside the IDE and fall back
# to utils::browseURL if it isn't available
viewer <- getOption("viewapp", utils::browseURL)
viewer("http://localhost:8100")
}
}
