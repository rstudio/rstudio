\name{pandocHTMLOptions}
\alias{pandocHTMLOptions}
\title{Pandoc options for HTML rendering}
\usage{
  pandocHTMLOptions(template, mathjax = NULL)
}
\arguments{
  \item{template}{Full path to a custom pandoc HTML
  template}

  \item{mathjax}{URL to mathjax library used in HTML
  output}
}
\value{
  Character vector of pandoc options
}
\description{
  Get pandoc command-line options required for converting R
  Markdown to HTML.
}

