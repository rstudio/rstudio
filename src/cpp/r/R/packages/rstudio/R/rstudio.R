

#' @export
programMode <- function() {
  .Call(getNativeSymbolInfo("rs_rstudioProgramMode", 
                            PACKAGE="")) 
}

#' @export
versionInfo <- function() {
  packageVersion("rstudio")
}

