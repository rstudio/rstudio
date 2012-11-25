\name{viewData}
\alias{viewData}
\title{Invoke the RStudio Data Viewer}
\description{
Invoke the RStudio data viewer on a matrix-like R object
}
\usage{
viewData(x, title)
}

\arguments{
  \item{x}{An R object which can be coerced to a data frame with non-zero numbers of rows and columns.
}
  \item{title}{Title for viewer window. Default to name of x.
}
}

\details{
Object x is coerced (if possible) to a data frame, and all non-numeric columns are then coerced to character. The object is then viewed in a spreadsheet-like data viewer.

If there are row names on the data frame that are not 1:nrow, they are displayed in a separate first column called row.names.

Objects with zero columns or zero rows are not accepted.
}
\value{
Invisible NULL. The function shows the viewer and returns immediately.
}
