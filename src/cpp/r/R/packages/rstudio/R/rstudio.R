

#' @export
versionInfo <- function() {
  info <- list()
  info$version <- utils::packageVersion("rstudio")
  info$mode <- .Call(getNativeSymbolInfo("rs_rstudioProgramMode", 
                                         PACKAGE=""))
  info
}

