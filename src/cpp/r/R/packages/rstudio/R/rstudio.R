

versionInfo <- function() {
  info <- list()
  info$version <- package_version(utils:::packageDescription("rstudio", 
                                                             fields="Version"))
  info$mode <- .Call(getNativeSymbolInfo("rs_rstudioProgramMode", 
                                         PACKAGE=""))
  info
}

diagnosticsReport <- function() {
  invisible(.Call(getNativeSymbolInfo("rs_sourceDiagnostics", PACKAGE="")))
}

previewRd <- function(rdFile) {
  
  if (!is.character(rdFile) || (length(rdFile) != 1))
    stop("rdFile must be a single element character vector.")
  if (!file.exists(rdFile))
    stop("The specified rdFile ' ", rdFile, "' does not exist.")
      
  invisible(.Call(getNativeSymbolInfo("rs_previewRd", PACKAGE=""), rdFile))
}

viewer <- function(url, height = NULL) {
  
  if (!is.character(url) || (length(url) != 1))
    stop("url must be a single element character vector.")
  
  if (!is.null(height) && (!is.numeric(height) || (length(height) != 1)))
     stop("height must be a single element numeric vector.")
  
  invisible(.Call(getNativeSymbolInfo("rs_viewer", PACKAGE=""), url, height))     
}
