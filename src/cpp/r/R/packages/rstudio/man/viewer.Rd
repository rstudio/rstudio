\name{viewer}
\alias{viewer}

\title{
View local web content within RStudio
}
\description{
View local web content within RStudio. Content can be served from static files in the R session temporary directory or can be a \link[shiny:shiny-package]{Shiny}, \link[Rook:Rook-package]{Rook}, \link[opencpu:opencpu]{OpenCPU}, or any other type of localhost web application.
}
\usage{
rstudio::viewer(url, height = NULL)
}
\arguments{
  \item{url}{Application URL. This can be either a localhost URL or a path to a file within the R session temporary directory (i.e. a path returned by \code{\link[base:tempfile]{tempfile}}).
  }
  \item{height}{Desired height. Specifies a desired height for the Viewer pane (the default is \code{NULL} which makes no change to the height of the pane). See details below for a discussion of constraints imposed on the height.
  }
}

\details{
RStudio also sets the global \code{viewer} option to the \code{rstudio::viewer} function so that it can be invoked in a front-end independent manner.

Applications are displayed within the Viewer pane. The application URL must either be served from localhost or be a path to a file within the R session temporary directory. If the URL doesn't conform to these requirements it is displayed within a standard browser window.

The \code{height} paramater specifies a desired height, however it's possible the Viewer pane will end up smaller if the request can't be fulfuilled (RStudio ensures that the pane paired with the Viewer maintains a minimum height). A height of 400 pixels or lower is likely to succeed in a large proportion of configurations. A very large height (e.g. 2000 pixels) will allocate the maximum allowable space for the Viewer.

}

\examples{
\dontrun{

# run an application inside the IDE
rstudio::viewer("http://localhost:8100")

# run an application and request a height of 500 pixels
rstudio::viewer("http://localhost:8100", height = 500)

# probe for viewer option then fall back to browseURL
viewer <- getOption("viewer")
if (!is.null(viewer))
   viewer("http://localhost:8100")
else
   utils::browseURL("http://localhost:8100")

# generate a temporary html file and display it
htmlFile <- tempfile(fileext=".html")
# (code to write some content to the file)
rstudio::viewer(htmlFile)
}

}
