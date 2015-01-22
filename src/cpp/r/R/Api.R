
.rs.addApiFunction("versionInfo", function() {
  info <- list()
  info$citation <- .Call(getNativeSymbolInfo("rs_rstudioCitation", 
                                             PACKAGE=""))
  
  info$mode <- .Call(getNativeSymbolInfo("rs_rstudioProgramMode", 
                                         PACKAGE=""))
  
  info$version <- package_version(
    .Call(getNativeSymbolInfo("rs_rstudioVersion", PACKAGE=""))
  )
  
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

.rs.addApiFunction("sourceMarkers", function(name, 
                                             markers, 
                                             basePath = NULL,
                                             autoSelect = c("none", "first", "error")) {
   
   # validate name
   if (!is.character(name))
      stop("name parameter is specified or invalid: ", name, call. = FALSE)
   
   # validate autoSelect
   autoSelect = match.arg(autoSelect)
   
   # validate markers
   isMarker <- function(marker) {
      markerTypes <- c("error", "warning")
      if (is.null(marker$type) || (!marker$type %in% markerTypes))
         stop("Invalid marker type (", marker, ")", call. = FALSE)
      if (!is.character(marker$file))
         stop("Marker file is unspecified or invalid: ", marker$file, call. = FALSE)
      if (!is.numeric(marker$line))
         stop("Marker line is unspecified or invalid", marker$line, call. = FALSE)
      if (!is.numeric(marker$column))
         stop("Marker column is unspecified or invalid", marker$line, call. = FALSE)
      if (!is.character(marker$message))
         stop("Marker message is unspecified or invalid: ", marker$message, call. = FALSE)
   }
   
   if (is.data.frame(markers)) {
      
      if (is.null(markers$type))
         stop("markers type field not specified", call. = FALSE)
      if (is.null(markers$file))
         stop("markers type field not specified", call. = FALSE)
      if (is.null(markers$line))
         stop("markers type field not specified", call. = FALSE)
      if (is.null(markers$column))
         stop("markers column field not specified", call. = FALSE)
      if (is.null(markers$message))
         stop("markers message field not specified", call. = FALSE)
      
   } else if (is.list(markers)) {
      markers <- lapply(markers, function(marker) {
         markerTypes <- c("error", "warning")
         if (is.null(marker$type) || (!marker$type %in% markerTypes))
            stop("Invalid marker type (", marker, ")", call. = FALSE)
         if (!is.character(marker$file))
            stop("Marker file is unspecified or invalid: ", marker$file, call. = FALSE)
         if (!is.numeric(marker$line))
            stop("Marker line is unspecified or invalid", marker$line, call. = FALSE)
         if (!is.numeric(marker$column))
            stop("Marker column is unspecified or invalid", marker$line, call. = FALSE)
         if (!is.character(marker$message))
            stop("Marker message is unspecified or invalid: ", marker$message, call. = FALSE)
         
         marker$type <- .rs.scalar(marker$type)
         marker$file <- .rs.scalar(marker$file)
         marker$line <- .rs.scalar(marker$line)
         marker$column <- .rs.scalar(marker$column)
         marker$message <- .rs.scalar(marker$message)
         
         marker
      })
   } else {
      stop("markers was not a data.frame or a list", call. = FALSE)
   }
   
   # validate basePath
   if (is.null(basePath))
      basePath <- ""
   else if (!is.character(basePath))
      stop("basePath parameter is not of type character", call. = FALSE)
   
   # pass everything on
   invisible(.Call("rs_sourceMarkers", name, markers, basePath, autoSelect))
})



