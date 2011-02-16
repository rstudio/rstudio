\name{manipulate}
\alias{manipulate}
\title{Create Interactive Plot}
\description{
Manipulate enables interactive manipulation of plots within RStudio. The \code{manipulate} function accepts a plotting expression and a set of controls (e.g. \code{slider} or \code{picker}) which are used to dynamically change values within the expression. When a value is changed using its corresponding control the expression is automatically re-executed and the plot is redrawn.

For example, to create a plot that enables manipulation of a parameter using a slider control you could use syntax like this: 

   \code{manipulate(plot(1:x), x = slider(1, 10))}

After this code is executed the plot is drawn using an initial value of 1 for \code{x}. A manipulator panel is also opened adjacent to the plot which contains a slider control used to change the value of \code{x} from 1 to 10.

}
\usage{
manipulate(expr, ..., controls = list())

slider(min, max, value = min, label = NULL, step = NULL, ticks = TRUE)

picker(..., value = NULL, label = NULL)

checkbox(value, label)

manipulatorSetState(name, value)
manipulatorGetState(name)
}


\arguments{
  \item{expr}{
    Expression to evalulate. The expression should result in the creation of a plot (e.g. \code{plot} or \code{qplot}). Note that the expression need not be a top-level plotting function, it could also be a custom function that creates a plot as part of its implementation. This expression will be re-evaluated with appropriate parameter substitution each time one of the manipulator control values is changed. 
}
  \item{...}{
    For \code{manipulate}, one or more controls (i.e. \code{slider}, \code{picker}, or \code{checkbox}). Note that controls may also be specified as a pre-set list using the \code{controls} argument.
    For \code{picker}, one or more values to use as choices. All values provided will be converted to character. If a list is provided as one of the arguments then it will be expanded using \code{unlist}.
}
  \item{min}{
     Minimum value for slider.
}
  \item{max}{
     Maximum value for slider.
}
  \item{value}{
     Initial value for manipulator control. 
}
  \item{label}{
     Display label for manipulator control. Required for \code{checkbox}. Optional for \code{slider} and \code{picker} (if no label is specified then the variable name will be used as the label).
}
  \item{step}{
     Step value for slider control.
}
  \item{ticks}{
     Show tick marks on the slider. Note that if the granularity of the step value is very low (more than 25 ticks would be shown) then ticks are automatically turned off.
}
}

\details{
    Once a set of manipulator controls are attached to a plot they remain attached and can be recalled whenever viewing the plot (a gear button is added to the top-left of the plot to indicate that it has a manipulator).

  The \code{manipulatorSetState} and \code{manipulatorGetState} functions can be used to associate custom state with a manipulator (for example, to track the values used for previous plot executions). These values are stored in a custom environment which is stored along with the rest of the manipulator context.
}

\value{
   \code{slider}, \code{picker}, and \code{checkbox} return an object of class control which can be passed to the \code{manipulate} function.
   
   \code{manipulatorGetState} returns a custom state value which was previously set by \code{manipulatorSetState} (or \code{NULL} if the specified name is not found).
}

\author{
JJ Allaire <jj@rstudio.org>
}

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

