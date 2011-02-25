\name{slider}
\alias{slider}
\title{Create a slider control}
\description{
  Create a slider control to allow manipulation of a plot variable along a numeric range.
}
\usage{
slider(min, max, initial = min, 
       label = NULL, step = NULL, ticks = TRUE)
}

\arguments{
  \item{min}{
    Minimum value for slider.
}
  \item{max}{
    Maximum value for slider.
}
  \item{value}{
    Initial value for slider. Defaults to \code{min} if not specified.
}
  \item{label}{
    Display label for slider. Defaults to the variable name if not specified.
}
  \item{step}{
    Step value for slider. If not specified then defaults to 1 for integer ranges and single pixel granularity for floating point ranges (\code{max} - \code{min} divided by the number of pixels in the slider).
}
  \item{ticks}{
    Show tick marks on the slider. Note that if the granularity of the step value is very low (more than 25 ticks would be shown) then ticks are automatically turned off.
}

}

\value{
  An object of class "manipulator.slider" which can be passed to the \code{\link{manipulate}} function.
}

\seealso{
\code{\link{manipulate}}, \code{\link{picker}}, \code{\link{checkbox}}
}

\examples{
\donttest{
## create a plot with a slider
manipulate(plot(1:x), x = slider(1, 10))
}
\dontrun{
## specify a custom initial value for a slider
manipulate(plot(1:x), x = slider(1, 10, initial = 5))

## specify a custom label for a slider
manipulate(plot(1:x), x = slider(1, 10, label = "Scale"))

## specify a step value for a slider
manipulate(plot(1:x), x = slider(1, 10, step = 2))

## use more than one slider
manipulate(plot(x:y), 
           x = slider(1, 10),
           y = slider(1, 100))

}
}

