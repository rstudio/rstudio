\name{manipulate-package}
\alias{manipulate-package}
\docType{package}
\title{Interactive Plots for RStudio}
\description{
Interactive plotting functions for use within RStudio.
}
\details{
\tabular{ll}{
Package: \tab manipulate\cr
Type: \tab Package\cr
License: \tab AGPL-3\cr
LazyLoad: \tab yes\cr
}

The \code{\link{manipulate}} function accepts a plotting expression and a set of controls (e.g. \code{\link{slider}}, \code{\link{picker}}, or \code{\link{checkbox}}) which are used to dynamically change values within the expression. When a value is changed using its corresponding control the expression is automatically re-executed and the plot is redrawn.

For example, to create a plot that enables manipulation of a parameter using a slider control you could use syntax like this: 

   \code{manipulate(plot(1:x), x = slider(1, 10))}

After this code is executed the plot is drawn using an initial value of 1 for \code{x}. A manipulator panel is also opened adjacent to the plot which contains a slider control used to change the value of \code{x} from 1 to 10.

}

\author{
JJ Allaire

Maintainer: JJ Allaire <jj@rstudio.org>
}


\keyword{ package }
\keyword{ dynamic }
\keyword{ iplot }

\examples{
\dontrun{

## create a plot with a manipulator
manipulate(plot(1:x), x = slider(1, 10))

## specify a custom initial value for a slider
manipulate(plot(1:x), x = slider(1, 10, value = 5))

## specify a custom label for a slider
manipulate(plot(1:x), x = slider(1, 10, label = "Scale"))

## specify a step value for a slider
manipulate(plot(1:x), x = slider(1, 10, step = 2))

## use more than one slider
manipulate(plot(x:y), 
           x = slider(1, 10),
           y = slider(1, 100))

## use with ggplot w/ picker for log param
manipulate(qplot(mpg$displ, mpg$cty, log = log),
           log = picker("x", "y", "xy"))

## specify controls as a dynamically built list
controls <- list()
controls$x <- slider(1, 10)
controls$y <- slider(1, 100)
manipulate(plot(x:y), controls)
}
}


