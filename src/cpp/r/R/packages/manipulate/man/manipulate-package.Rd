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
## Create a plot with a manipulator
manipulate(barplot(1:x), x=slider(5, 10))

## Using more than one slider
manipulate(
  plot(cars, xlim=c(x.min,x.max)), 
  x.min=slider(0,15), 
  x.max=slider(15,30))

## Displaying options with a picker
manipulate(
  barplot(1:10, col=color), 
  color=picker("red", "blue", "green"))

## Displaying parameter choices with a picker
manipulate(
  plot(pressure, type=type), 
  type=picker("p", "l", "b", "c", "o", "h", "s", "S", "n"))

## Using checkboxes for boolean parameters
manipulate(
  plot(cars, axes=x, ann=y), 
  x=checkbox(initial=TRUE, label="Draw Axes"), 
  y=checkbox(initial=TRUE, label="Draw Labels"))

## Combining controls
manipulate(
  plot(cars, xlim=c(x.min,x.max), type=type, axes=x, ann=y),
  x.min=slider(0,15),
  x.max=slider(15,30, initial=25),
  type=picker("p", "l", "b", "c", "o", "h", "s", "S", "n"),
  x=checkbox(initial=TRUE, label="Draw Axes"),
  y=checkbox(label="Draw Labels"))
}
}


