
.rs.addApiFunction("versionInfo", function() {
  info <- list()
  info$version <- package_version(
    .Call(getNativeSymbolInfo("rs_rstudioVersion", PACKAGE=""))
  )

  info$mode <- .Call(getNativeSymbolInfo("rs_rstudioProgramMode", 
                                         PACKAGE=""))
  
  info$citation <- .Call(getNativeSymbolInfo("rs_rstudioCitation", 
                                         PACKAGE=""))
  info
})

.rs.addApiFunction("diagnosticsReport", function() {
  invisible(.Call(getNativeSymbolInfo("rs_sourceDiagnostics", PACKAGE="")))
})


.rs.addApiFunction("previewRd", function(rdFile) {
  
  if (!is.character(rdFile) || (length(rdFile) != 1))
    stop("rdFile must be a single element character vector.")
  if (!file.exists(rdFile))
    stop("The specified rdFile ' ", rdFile, "' does not exist.")
      
  invisible(.Call(getNativeSymbolInfo("rs_previewRd", PACKAGE=""), rdFile))
})

.rs.addApiFunction("viewer", function(url, height = NULL) {
  
  if (!is.character(url) || (length(url) != 1))
    stop("url must be a single element character vector.")
  
  if (identical(height, "maximize"))
     height <- -1

  if (!is.null(height) && (!is.numeric(height) || (length(height) != 1)))
     stop("height must be a single element numeric vector or 'maximize'.")
  
  invisible(.Call(getNativeSymbolInfo("rs_viewer", PACKAGE=""), url, height))     
})

