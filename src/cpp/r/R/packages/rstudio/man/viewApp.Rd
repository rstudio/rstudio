\name{viewApp}
\alias{viewApp}

\title{
View an R web application within RStudio
}
\description{
View an R web application within the RStudio IDE. This is useful for running \link[shiny:shiny-package]{Shiny}, \link[Rook:Rook-package]{Rook}, \link[opencpu:opencpu]{OpenCPU}, or any other type of local web application. Applications are displayed within the Viewer pane.
}
\usage{
viewApp(url, maximize = TRUE)
}
\arguments{
  \item{url}{Application URL}
  \item{maximize}{Request that the application occupy the full height of the IDE}
}

\details{
RStudio also sets the global \code{browser.internal.app} option to the \code{rstudio::viewApp} function so that it can be invoked in front-end independent manner.

Applications displayed within the Viewer pane must be served from the local system (i.e. they cannot come from an arbitrary URL on the internet).
}

\examples{
\dontrun{

# run an application in the Viewer pane
rstudio::viewApp("http://localhost:8100")

# attempt to run an application inside the IDE and fall back
# if it isn't available
appBrowser <- getOption("browser.internal.app", utils::browseURL)
appBrowser("http://localhost:8100")
}
}
