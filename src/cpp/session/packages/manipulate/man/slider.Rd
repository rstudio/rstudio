\name{slider}
\alias{slider}
%- Also NEED an '\alias' for EACH other topic documented here.
\title{Slider Control}
\description{
%%  ~~ A concise (1-5 lines) description of what the function does. ~~
}
\usage{
slider(min, max, value = min, label = NULL, step = NULL, ticks = TRUE)
}
%- maybe also 'usage' for other objects documented here.
\arguments{
  \item{min}{
%%     ~~Describe \code{min} here~~
}
  \item{max}{
%%     ~~Describe \code{max} here~~
}
  \item{value}{
%%     ~~Describe \code{value} here~~
}
  \item{label}{
%%     ~~Describe \code{label} here~~
}
  \item{step}{
%%     ~~Describe \code{step} here~~
}
  \item{ticks}{
%%     ~~Describe \code{ticks} here~~
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
function (min, max, value = min, label = NULL, step = NULL, ticks = TRUE) 
{
    if (!is.numeric(value) || !is.numeric(min) || !is.numeric(max)) 
        stop("min, max, amd value must all be numeric values")
    else if (value < min) 
        stop(paste("slider value", value, "is less than the specified minimum"))
    else if (value > max) 
        stop(paste("slider value", value, "is greater than the specified maximum"))
    else if (min > max) 
        stop(paste("slider maximum is greater than minimum"))
    else if (!is.null(step)) {
        if (!is.numeric(step)) 
            stop("step is not a numeric value")
        if (step > (max - min)) 
            stop("step is greater than range")
    }
    else if (!is.logical(ticks)) 
        stop("ticks is not a logical value")
    else if (!is.null(label) && !is.character(label)) 
        stop("label is not a character value")
    if (is.null(step)) 
        step <- -1
    slider <- list(type = 0, min = min, max = max, initialValue = value, 
        label = label, step = step, ticks = ticks)
    class(slider) <- "manipulator.slider"
    return(slider)
  }
}
% Add one or more standard keywords, see file 'KEYWORDS' in the
% R documentation directory.
\keyword{ ~kwd1 }
\keyword{ ~kwd2 }% __ONLY ONE__ keyword per line
