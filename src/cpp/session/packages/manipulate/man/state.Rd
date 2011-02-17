\name{Manipulator Custom State}
\alias{manipulatorSetState}
\alias{manipulatorGetState}
%- Also NEED an '\alias' for EACH other topic documented here.
\title{Custom State Functions}
\description{
  Set and get custom manipulator state.
}
\usage{
manipulatorSetState(name, value)
}
%- maybe also 'usage' for other objects documented here.
\arguments{
  \item{name}{
%%     ~~Describe \code{name} here~~
}
  \item{value}{
%%     ~~Describe \code{value} here~~
}
}
\details{
%%  ~~ If necessary, more details than the description above ~~
}
\value{
%%  ~Describe the value returned
%%  If it is a LIST, use
%%  \item{comp1 }{Description of 'comp1'}
%%  \item{comp2 }{Description of 'comp2'}
%% ...
}
\references{
%% ~put references to the literature/web site here ~
}
\author{
%%  ~~who you are~~
}
\note{
%%  ~~further notes~~
}

%% ~Make other sections like Warning with \section{Warning }{....} ~

\seealso{
%% ~~objects to See Also as \code{\link{help}}, ~~~
}
\examples{
##---- Should be DIRECTLY executable !! ----
##-- ==>  Define data, use random,
##--	or do  help(data=index)  for the standard data sets.

## The function is currently defined as
function (name, value) 
{
    if (hasActiveManipulator()) {
        assign(name, value, envir = get(".state", envir = activeManipulator()))
        ensureManipulatorSaved()
        invisible(NULL)
    }
    else {
        stop("no plot manipulator currently active")
    }
  }
}
% Add one or more standard keywords, see file 'KEYWORDS' in the
% R documentation directory.
\keyword{ ~kwd1 }
\keyword{ ~kwd2 }% __ONLY ONE__ keyword per line
