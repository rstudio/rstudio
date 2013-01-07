

versionInfo <- function() {
  info <- list()
  info$version <- utils::packageVersion("rstudio")
  info$mode <- .Call(getNativeSymbolInfo("rs_rstudioProgramMode", 
                                         PACKAGE=""))
  info
}

diagnosticsReport <- function() {
  invisible(.Call(getNativeSymbolInfo("rs_sourceDiagnostics", PACKAGE="")))
}

loadHistory <- function(file = ".Rhistory") {
  invisible(.Call(getNativeSymbolInfo("rs_loadHistory", PACKAGE=""), file))
}

saveHistory <- function(file = ".Rhistory") {
  invisible(.Call(getNativeSymbolInfo("rs_saveHistory", PACKAGE=""), file))
}

viewData <- function(data, caption) {
  invisible(.Call(getNativeSymbolInfo("rs_viewData", PACKAGE=""), 
                  as.data.frame(data), caption))
}
