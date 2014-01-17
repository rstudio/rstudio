
#' Set knitr output hooks for pandoc markdown
#' 
#' This function sets the built in output hooks for rendering to pandoc
#' markdown. Output hooks are based on the target output format, which is
#' deduced from the file extension (or alternatively via an explicit format
#' argument).
#' 
#' @param output target output file
#' @param format target output format. If not specified then is deduced from the
#'   output file extension.
#'
#' @export
render_pandoc_markdown <- function(output, format = NULL) {
  
  # guess the format if it's not provided. it's not critical that we
  # have a format, but when we do then it's possible to customize
  # markdown rendering to optimize for the intended output
  if (is.null(format)) {
    ext <- tools::file_ext(output)
    if (ext %in% c("html", "docx", "odt", "rtf"))
      format <- ext
    else if (identical(ext, "pdf"))
      format <- "latex"
    else
      format <- "unknown"
  }
  
  # stock markdown options
  knitr::render_markdown()
  
  # chunk options (do a figure directory per-format to gracefully handle
  # switching between formats)
  knitr::opts_chunk$set(tidy = FALSE,
                        error = FALSE,
                        fig.path=paste("figure-", format, "/", sep = ""))
  
  # some pdf specific options
  if (format %in% c("latex", "beamer")) {
    knitr::opts_chunk$set(dev = 'cairo_pdf')
    knitr::knit_hooks$set(crop = knitr::hook_pdfcrop)
  }
  
  invisible(NULL)
}
