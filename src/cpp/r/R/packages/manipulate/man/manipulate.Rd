\name{manipulate}
\alias{manipulate}
\title{Create an interactive plot}
\description{
The \code{\link{manipulate}} function accepts a plotting expression and a set of controls (e.g. \code{\link{slider}}, \code{\link{picker}},  \code{\link{checkbox}}, or \code{\link{button}}) which are used to dynamically change values within the expression. When a value is changed using its corresponding control the expression is automatically re-executed and the plot is redrawn.
}
\usage{
manipulate(`_expr`, ...)
}


\arguments{
  \item{_expr}{
    Expression to evalulate. The expression should result in the creation of a plot (e.g. \code{plot} or \code{qplot}). Note that the expression need not be a top-level plotting function, it could also be a custom function that creates a plot as part of its implementation. This expression will be re-evaluated with appropriate parameter substitution each time one of the manipulator control values is changed. 
}
  \item{\dots}{
    One or more named control arguments (i.e. \code{\link{slider}}, \code{\link{picker}}, \code{\link{checkbox}}, or \code{\link{button}}), or a list containing named controls. 
}
}

\details{
    Once a set of manipulator controls are attached to a plot they remain attached and can be recalled whenever viewing the plot (a gear button is added to the top-left of the plot to indicate that it has a manipulator).

    The \code{_expr} argument is evaluated using \code{\link{withVisible}}. If it's return value is visible then \code{\link{print}} is called. This enables manipulate expressions to behave simillarly to their being executed directly at the console. 
    
    The \code{_expr} argument uses a syntactially invalid (but backtick quoted) name to avoid clashes with named control arguments.

  The \code{\link{manipulatorSetState}} and \code{\link{manipulatorGetState}} functions can be used to associate custom state with a manipulator (for example, to track the values used for previous plot executions). These values are stored in a custom environment which is stored along with the rest of the manipulator context.
}

\author{
RStudio <info@rstudio.com>
}

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

