\name{picker}
\alias{picker}
\title{Create a picker control}
\description{
  Create a picker control to enable manipulation of plot variables based on a set of fixed choices.
}

\usage{
picker(..., initial = NULL, label = NULL)
}


\arguments{
  \item{\dots}{
    Arguments containing objects to be presented as choices for the picker (or a list containing the choices). If an element is named then the name is used to display it within the picker. If an element is not named then it is displayed within the picker using \code{\link{as.character}}. 
}
  \item{initial}{
    Initial value for picker. Value must be present in the list of choices specified. If not specified defaults to the first choice.
}
  \item{label}{
    Display label for picker. Defaults to the variable name if not specified.
}
}

\value{
  An object of class "manipulator.picker" which can be passed to the \code{\link{manipulate}} function.
}

\seealso{
\code{\link{manipulate}}, \code{\link{slider}}, \code{\link{checkbox}}
}


\examples{
\donttest{
## histogram w/ picker to select type
require(lattice)
require(stats)
manipulate(
  histogram(~ height | voice.part, data = singer, type = type),
  type = picker("percent", "count", "density"))
}
\dontrun{
## use with ggplot w/ picker for log param
require(ggplot2)
manipulate(
  qplot(mpg$displ, mpg$cty, log = log),
  log = picker("x", "y", "xy"))  
}
}