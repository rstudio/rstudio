
#' Set knitr hooks and options for rendering markdown
#'
#' These functions set knitr hooks and options for markdown rendering. Hooks are
#' based on the default
#' \code{\link[knitr:render_markdown]{knitr::render_markdown}} function with
#' additional customization for various output formats.
#'
#' @param format Pandoc format being rendered (used to create distinct figure
#'   directories for multiple formats)
#' @param fig.width Default width for figures
#' @param fig.height Default height for figures
#'
#' @details You typically need to call only one knitr render function, as the
#' various format-specific functions (e.g. \code{knitrRenderPDF}) all call
#' the \code{knitrRender} function as part of their implementation.
#'
#' @export
knitrRender <- function(format) {

  # stock markdown options
  knitr::render_markdown()

  # chunk options
  knitr::opts_chunk$set(tidy = FALSE,    # don't reformat R code
                        comment = NA,    # don't preface output with ##
                        error = FALSE)   # stop immediately on errors

  # figure directory scope
  knitr::opts_chunk$set(fig.path=paste("figure-", format, "/", sep = ""))
}

