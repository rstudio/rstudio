
#' Set knitr hooks and options for rendering markdown
#'
#' This function sets knitr hooks and options for markdown rendering. Hooks are
#' based on the default
#' \code{\link[knitr:render_markdown]{knitr::render_markdown}} function with
#' some additional tweaks.
#'
#' @param figureScope Directory name scope for figures (useful when rendering
#'   more than one format from a directory)
#'
#' @export
knitrRenderMarkdown <- function(figureScope = NULL) {

  # stock markdown options
  knitr::render_markdown()

  # chunk options
  knitr::opts_chunk$set(tidy = FALSE,    # don't reformat R code
                        comment = NA,    # don't preface output with ##
                        error = FALSE)   # stop immediately on errors

  # figure directory scope if requested
  if (!is.null(figureScope))
    knitr::opts_chunk$set(fig.path=paste("figure-", figureScope, "/", sep = ""))
}

