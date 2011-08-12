\name{manipulatorMouseClick}
\alias{manipulatorMouseClick}
\title{Receive notification of mouse clicks on a manipulator plot}
\description{
   This function can be called to determine if a mouse click on the plot was what caused the current invocation of the manipulate expression, and to determine the coordinates which were clicked.
}
\usage{
manipulatorMouseClick()
}

\details{
   If a mouse click did occur, then the function returns a list with the coordinates which the user clicked on.
   
   If a mouse click did not cause the current invocation of the manipulate expression (e.g. if it was caused by the user changing the value of a control) then the function returns NULL.
   
   The mouse click coordinates are provided in device, user, and ndc coordinates. To convert these coordinates into other coordinate systems (e.g. cm or npc) you can use the   \code{\link{grconvertX}} and \code{\link{grconvertY}} functions.  
   
   Note that the userX and userY coordinates are only applicable for base graphics plots (they are not applicable for grid, lattice, ggplot, etc). Therefore, for non-base graphics the userX and userY values will not contain valid coordinates.
}

\value{
Returns a list containing the coordinates that user clicked (or NULL if a mouse click didn't occur):
\tabular{ll}{
   \code{deviceX} \tab Device X coordinate (expressed in pixels)\cr
   \code{deviceY} \tab Device Y coordinate (expressed in pixels)\cr
   \code{userX} \tab User X coordinate (expressed in plot x units). Note that this value is only valid for base graphics.\cr
   \code{userY} \tab User Y coordinate (expressed in plot y units). Note that this value is only valid for base graphics.\cr
   \code{ndcX} \tab NDC X coordinate (0 to 1 from left to right)\cr
   \code{ndcY} \tab NDC Y coordinate (0 to 1 from bottom to top)\cr
}
}

\seealso{
  \code{\link{manipulate}}, \code{\link{grconvertX}}, \code{\link{grconvertY}}
}

\examples{
\dontrun{



}
}


