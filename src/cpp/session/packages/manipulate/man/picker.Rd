\name{picker}
\alias{picker}
\title{Create a picker control}
\description{
  Create a picker control to enable manipulation of plot variables based on a set of fixed choices.
}

\usage{
picker(..., value = NULL, label = NULL)
}


\arguments{
  \item{\dots}{
    List of choices for the picker. Can be provided as a series of arguments, a character vector, or both (all arguments are combined together using \code{\link{unlist}}).
}
  \item{value}{
    Default value for picker. Value must be present in the list of choices specified. If not specified defaults to the first choice.
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
\dontrun{

## use with ggplot w/ picker for log param
manipulate(qplot(mpg$displ, mpg$cty, log = log),
           log = picker("x", "y", "xy"))
           
           
## histogram w/ picker to select type
require(stats)
manipulate(histogram( ~ height | voice.part, 
                     data = singer, type = type),
           type = picker("percent", "count", "density"))


}
}