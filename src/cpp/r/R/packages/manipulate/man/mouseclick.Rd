\name{Manipulator Mouse Click}
\alias{manipulatorMouseClick}
\alias{manipulatorMouseClick}
\title{Receive notification of mouse clicks on a manipulator plot}
\description{
   This function can be called to determine if a mouse click on the plot was what caused the current call to the manipulate plotting expression. If a mouse click did occur, then the function returns a list with two elements (\code{x} and \code{y}) indicating the NDC coordinates clicked on. If a mouse click did not cause the current call to the plotting expression (e.g. if it was caused by the user changing the value of a control) then the function returns NULL.
}
\usage{
manipulatorMouseClick()
}

\value{
Returns a list containing the NDC coordinates that user clicked (or NULL if a mouse click didn't occur).
\tabular{ll}{
   \code{x} \tab x-coordinate\cr
   \code{y} \tab y-coordinate\cr
}
}

\seealso{
  \code{\link{manipulate}}
}

\examples{
\dontrun{



}
}


