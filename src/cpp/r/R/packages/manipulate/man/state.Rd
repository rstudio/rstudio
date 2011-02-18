\name{Manipulator Custom State}
\alias{manipulatorSetState}
\alias{manipulatorGetState}
\title{Modify manipulator state}
\description{
  These functions allow the storage of custom state variables across multiple evaluations of manipulator expressions. These functions are useful if the manipulate expression is a custom function (rather than a high level plotting function like \code{\link{plot}}) which requires reading and writing of persistent values.
}
\usage{
manipulatorSetState(name, value)
manipulatorGetState(name)
}

\arguments{
  \item{name}{
    A chraracter string holding a state variable name.
}
  \item{value}{
    An object holding a state value.
}
}

\value{
  \code{manipulatorGetState} returns a custom state value which was previously set by \code{manipulatorSetState} (or \code{NULL} if the specified name is not found).
}

\seealso{
  \code{\link{manipulate}}
}

\examples{
\dontrun{

## set custom state variable
manipulatorSetState("last", x)

## get custom state variable
last <- manipulatorGetState("last")
if ( !is.null(last) ) {
  # do something interesting
}

}
}


