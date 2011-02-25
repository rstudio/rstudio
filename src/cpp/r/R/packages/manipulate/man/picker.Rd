\name{picker}
\alias{picker}
\title{Create a picker control}
\description{
  Create a picker control to enable manipulation of plot variables based on a set of fixed choices.
}

\usage{
picker(..., choices = list(), initial = NULL, label = NULL)
}


\arguments{
  \item{\dots}{
    Arguments containing objects to be presented as choices for the picker. If an argument is named then the name is used to display it within the picker. If an argument is not named then it is displayed within the picker using \code{\link{as.character}}). 
}
  \item{choices}{
    A list of objects to be used as choices for the picker. Display names for objects within the picker are derived using list element names or \code{\link{as.character}} for unnamed items. If both \code{choices} and variable arguments are given then the variable arguments are appended to \code{choices}.
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
manipulate(histogram( ~ height | voice.part, 
                     data = singer, type = type),
           type = picker("percent", "count", "density"))
}
\dontrun{
## use with ggplot w/ picker for log param
require(ggplot2)
manipulate(qplot(mpg$displ, mpg$cty, log = log),
           log = picker("x", "y", "xy"))  
}
}