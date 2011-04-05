\name{checkbox}
\alias{checkbox}
\title{Create a checkbox control}
\description{
  Create a checkbox control to enable manipulation of logical plot variables.
}
\usage{
checkbox(initial = FALSE, label = NULL)
}


\arguments{
  \item{initial}{
    Initial value for checkbox. Must be logical (defaults to \code{FALSE}).
}
  \item{label}{
    Display label for checkbox. Defaults to the variable name if not specified.
}
}

\value{
   An object of class "manipulator.checkbox" which can be passed to the \code{\link{manipulate}} function.
}


\seealso{
  \code{\link{manipulate}}, \code{\link{slider}}, \code{\link{picker}}
}

\examples{
\dontrun{

## Using checkboxes for boolean parameters
manipulate(
  plot(cars, axes = axes, ann = label), 
  axes = checkbox(TRUE, "Draw Axes"), 
  label = checkbox(FALSE, "Draw Labels"))

## Toggle boxplot outlier display using checkbox
manipulate(
  boxplot(Freq ~ Class, data = Titanic, outline = outline),
  outline = checkbox(FALSE, "Show outliers"))

}
}

