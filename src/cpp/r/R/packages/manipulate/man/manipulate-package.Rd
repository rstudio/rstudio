\name{manipulate-package}
\alias{manipulate-package}
\docType{package}
\title{Interactive Plots for RStudio}
\description{
Interactive plotting functions for use within RStudio.
}
\details{

The \code{\link{manipulate}} function accepts a plotting expression and a set of controls (e.g. \code{\link{slider}}, \code{\link{picker}}, \code{\link{checkbox}}, or \code{\link{button}}) which are used to dynamically change values within the expression. When a value is changed using its corresponding control the expression is automatically re-executed and the plot is redrawn.

For example, to create a plot that enables manipulation of a parameter using a slider control you could use syntax like this: 

   \code{manipulate(plot(1:x), x = slider(1, 10))}

After this code is executed the plot is drawn using an initial value of 1 for \code{x}. A manipulator panel is also opened adjacent to the plot which contains a slider control used to change the value of \code{x} from 1 to 10.

}

\author{
RStudio

Maintainer: RStudio <info@rstudio.com>
}


\keyword{ package }
\keyword{ dynamic }
\keyword{ iplot }

\examples{
\dontrun{

## Create a plot with a manipulator
manipulate(plot(1:x), x = slider(5, 10))

## Using more than one slider
manipulate(
  plot(cars, xlim=c(x.min,x.max)), 
  x.min=slider(0,15), 
  x.max=slider(15,30))

## Filtering data with a picker
manipulate(
  barplot(as.matrix(longley[,factor]), 
          beside = TRUE, main = factor),
  factor = picker("GNP", "Unemployed", "Employed"))

## Create a picker with labels
manipulate(
  plot(pressure, type = type), 
  type = picker("points" = "p", "line" = "l", "step" = "s"))

## Toggle boxplot outlier display using checkbox
manipulate(
  boxplot(Freq ~ Class, data = Titanic, outline = outline),
  outline = checkbox(FALSE, "Show outliers"))

## Combining controls
manipulate(
  plot(cars, xlim = c(x.min, x.max), type = type, 
       axes = axes, ann = label),
  x.min = slider(0,15),
  x.max = slider(15,30, initial = 25),
  type = picker("p", "l", "b", "c", "o", "h", "s", "S", "n"),
  axes = checkbox(TRUE, "Draw Axes"),
  label = checkbox(FALSE, "Draw Labels"))
}
}


