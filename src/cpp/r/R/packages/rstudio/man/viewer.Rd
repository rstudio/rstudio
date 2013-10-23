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

The \code{height} parameter specifies a desired height, however it's possible the Viewer pane will end up smaller if the request can't be fulfilled (RStudio ensures that the pane paired with the Viewer maintains a minimum height). A height of 400 pixels or lower is likely to succeed in a large proportion of configurations. A very large height (e.g. 2000 pixels) will allocate the maximum allowable space for the Viewer.

}

\section{Viewer Detection}{

When a page is displayed within the Viewer it's possible that the user will choose to pop it out into a standalone browser window. When rendering inside a standard browser you may want to make different choices about how content is laid out or scaled. Web pages can detect that they are running inside the Viewer pane by looking for the \code{viewer_pane} query parameter, which is automatically injected into URLs when they are shown in the Viewer. For example, the following URL:

\preformatted{
http://localhost:8100
}

When rendered in the Viewer pane is transformed to:

\preformatted{
http://localhost:8100?viewer_pane=1
}

To provide a good user experience it's strongly recommended that callers take advantage of this to automatically scale their content to the current size of the Viewer pane. For example, re-rendering a JavaScript plot with new dimensions when the size of the pane changes.

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
