\name{beamerOptions}
\alias{beamerOptions}
\title{Options for Beamer conversion}
\usage{
  beamerOptions(toc = FALSE, incremental = FALSE,
    slide.level = 2)
}
\arguments{
  \item{toc}{\code{TRUE} to include a table of contents in
  the output (only level 1 headers will be included in the
  table of contents).}

  \item{incremental}{\code{TRUE} to render slide bullets
  incrementally. Note that if you want to reverse the
  default incremental behavior for an individual bullet you
  can preceded it with \code{>}. For example: \code{> -
  Bullet Text}.}

  \item{slide.level}{The heading level which defines
  indvidual slides. By default this is level 2, which
  allows level 1 headers to be used to define sections of
  the presentation.}
}
\value{
  A list of options that can be passed to
  \code{\link{rmd2beamer}}.
}
\description{
  Define the options for converting R Markdown to Beamer
}

