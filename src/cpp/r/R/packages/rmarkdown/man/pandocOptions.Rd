\name{pandocMarkdownOptions}
\alias{pandocHTMLOptions}
\alias{pandocMarkdownOptions}
\alias{pandocPDFOptions}
\title{Compose pandoc options}
\usage{
  pandocMarkdownOptions(extraOptions = NULL)

  pandocHTMLOptions(template, mathjax = NULL)

  pandocPDFOptions(geometry = NULL)
}
\arguments{
  \item{extraOptions}{Additional flags for customizing the
  flavor of markdown input interpreted by pandoc.}

  \item{template}{Full path to a custom pandoc template}

  \item{mathjax}{URL to mathjax library used in HTML
  output}
}
\value{
  Character vector of pandoc options
}
\description{
  Convenince functions for composing sets of pandoc
  options. These functions are used by higher-level
  rmarkdown renderig functions like \code{\link{rmd2html}}
  and can be used to define additional custom renderers.
}

