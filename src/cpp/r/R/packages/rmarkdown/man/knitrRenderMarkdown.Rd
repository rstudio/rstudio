\name{knitrRenderMarkdown}
\alias{knitrRenderMarkdown}
\title{Set knitr hooks and options for rendering markdown}
\usage{
  knitrRenderMarkdown(figureScope = NULL)
}
\arguments{
  \item{figureScope}{Directory name scope for figures
  (useful when rendering more than one format from a
  directory)}
}
\description{
  This function sets knitr hooks and options for markdown
  rendering. Hooks are based on the default
  \code{\link[knitr:render_markdown]{knitr::render_markdown}}
  function with some additional tweaks.
}

