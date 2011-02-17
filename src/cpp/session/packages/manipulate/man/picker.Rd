\name{picker}
\alias{picker}
%- Also NEED an '\alias' for EACH other topic documented here.
\title{Picker Control}
\description{
  Create a picker control.
}
\usage{
picker(..., value = NULL, label = NULL)
}
%- maybe also 'usage' for other objects documented here.
\arguments{
  \item{\dots}{
%%     ~~Describe \code{\dots} here~~
}
  \item{value}{
%%     ~~Describe \code{value} here~~
}
  \item{label}{
%%     ~~Describe \code{label} here~~
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
function (..., value = NULL, label = NULL) 
{
    choices <- as.character(unlist(list(...), recursive = TRUE, 
        use.names = FALSE))
    if (!is.character(choices)) 
        stop("choices is not a character vector")
    else if (length(choices) < 1) 
        stop("choices must contain at least one value")
    else if (!is.null(value)) {
        if (!is.character(value) || (length(value) != 1)) 
            stop("value is not of type character")
        else if (!(value \%in\% choices)) 
            stop("value doesn't match one of the supplied choices")
    }
    else if (!is.null(label) && !is.character(label)) 
        stop("label is not a character value")
    if (is.null(value)) 
        value <- choices[1]
    picker <- list(type = 1, choices = choices, initialValue = value, 
        label = label)
    class(picker) <- "manipulator.picker"
    return(picker)
  }
}
% Add one or more standard keywords, see file 'KEYWORDS' in the
% R documentation directory.
\keyword{ ~kwd1 }
\keyword{ ~kwd2 }% __ONLY ONE__ keyword per line
