
.rs.addApiFunction("versionInfo", function() {
  info <- list()
  info$citation <- .Call(getNativeSymbolInfo("rs_rstudioCitation", 
                                             PACKAGE=""))
  
  info$mode <- .Call(getNativeSymbolInfo("rs_rstudioProgramMode", 
                                         PACKAGE=""))

  info$edition <- .Call(getNativeSymbolInfo("rs_rstudioEdition",
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


.rs.addApiFunction("savePlotAsImage", function(
                   file, 
                   format = c("png", "jpeg", "bmp", "tiff", "emf", "svg", "eps"),
                   width,
                   height) {
   
   file <- path.expand(file)
   format <- match.arg(format)
   if (!is.numeric(width))
      stop("width argument mut be numeric", call. = FALSE)
   if (!is.numeric(height))
      stop("height argument mut be numeric", call. = FALSE)
   invisible(.Call("rs_savePlotAsImage", file, format, width, height))
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
   
   # normalize basePath
   if (!is.null(basePath))
      basePath <- .rs.normalizePath(basePath,  mustWork = TRUE)
   
   if (is.data.frame(markers)) {
      
      cols <- colnames(markers)
      
      if (!"type" %in% cols || !is.character(markers$type))
         stop("markers type field is unspecified or invalid", call. = FALSE)
      if (!"file" %in% cols || !is.character(markers$file))
         stop("markers file field is unspecified or invalid", call. = FALSE)
      if (!"line" %in% cols || !is.numeric(markers$line))
         stop("markers line field is unspecified or invalid", call. = FALSE)
      if (!"column" %in% cols || !is.numeric(markers$column))
         stop("markers column field is unspecified or invalid", call. = FALSE)
      if (!"message" %in% cols || !is.character(markers$message))
         stop("markers message field is unspecified or invalid", call. = FALSE)
      
      # normalize paths
      markers$file <- .rs.normalizePath(markers$file, mustWork = TRUE)
      
      # check for html
      markers$messageHTML <- inherits(markers$message, "html")
      
   } else if (is.list(markers)) {
      markers <- lapply(markers, function(marker) {
         markerTypes <- c("error", "warning", "box", "info", "style", "usage")
         if (is.null(marker$type) || (!marker$type %in% markerTypes))
            stop("Invalid marker type (", marker$type, ")", call. = FALSE)
         if (!is.character(marker$file))
            stop("Marker file is unspecified or invalid: ", marker$file, call. = FALSE)
         if (!is.numeric(marker$line))
            stop("Marker line is unspecified or invalid", marker$line, call. = FALSE)
         if (!is.numeric(marker$column))
            stop("Marker column is unspecified or invalid", marker$line, call. = FALSE)
         if (!is.character(marker$message))
            stop("Marker message is unspecified or invalid: ", marker$message, call. = FALSE)
         
         marker$type <- .rs.scalar(marker$type)
         marker$file <- .rs.scalar(.rs.normalizePath(marker$file, mustWork = TRUE))
         marker$line <- .rs.scalar(as.numeric(marker$line))
         marker$column <- .rs.scalar(as.numeric(marker$column))
         marker$message <- .rs.scalar(marker$message)
         marker$messageHTML <- .rs.scalar(inherits(marker$message, "html"))
         
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
   
   invisible(.Call("rs_sourceMarkers", name, markers, basePath, autoSelect))
})

.rs.addApiFunction("navigateToFile", function(filePath, line = 1L, col = 1L) {
   # validate file argument
   if (!is.character(filePath)) {
      stop("filePath must be a character")  
   }
   if (!file.exists(filePath)) {
      stop(filePath, " does not exist.")
   }

   # validate line/col arguments
   if (!is.integer(line) || length(line) != 1 ||
       !is.integer(col)  || length(col) != 1) {
      stop("line and column must be integer values.")
   }

   # expand and alias for client
   filePath <- .rs.normalizePath(filePath, winslash="/", mustWork = TRUE) 
   homeDir <- path.expand("~")
   if (identical(substr(filePath, 1, nchar(homeDir)), homeDir)) {
      filePath <- file.path("~", substring(filePath, nchar(homeDir) + 2))
   }
   
   # send event to client
   .rs.enqueClientEvent("jump_to_function", list(
      file_name     = .rs.scalar(filePath),
      line_number   = .rs.scalar(line), 
      column_number = .rs.scalar(col)))

   invisible(NULL)
})

.rs.addFunction("validateAndTransformLocation", function(location)
{
   invalidRangeMsg <- "'ranges' should be a list of 4-element integer vectors"
   
   # allow a single range (then validate that it's a true range after)
   if (!is.list(location) || inherits(location, "document_range"))
      location <- list(location)
   
   ranges <- lapply(location, function(el) {
      
      # detect proxy Inf object
      if (identical(el, Inf))
         el <- c(Inf, 0, Inf, 0)
      
      # detect positions (2-element vectors) and transform them to ranges
      n <- length(el)
      if (n == 2 && is.numeric(el))
         el <- c(el, el)
      
      # detect document_ranges and transform
      if (is.list(el) && all(c("start", "end") %in% names(el)))
         el <- c(el$start, el$end)
      
      # validate we have a range-like object
      if (length(el) != 4 || !is.numeric(el) || any(is.na(el)))
         stop(invalidRangeMsg, call. = FALSE)
      
      # transform out-of-bounds values appropriately
      el[el < 1] <- 1
      el[is.infinite(el)] <- NA
      
      # transform from 1-based to 0-based indexing for server
      result <- as.integer(el) - 1L
      
      # treat NAs as end of row / column
      result[is.na(result)] <- as.integer(2 ^ 31 - 1)
      
      result
   })
   
   ranges
})

.rs.addFunction("enqueEditorClientEvent", function(type, data)
{
   eventData = list(type = .rs.scalar(type), data = data)
   .rs.enqueClientEvent("editor_command", eventData)
})

.rs.addApiFunction("insertText", function(location, text, id = "") {
   
   invalidTextMsg <- "'text' should be a character vector"
   invalidLengthMsg <- "'text' should either be length 1, or same length as 'ranges'"

   if (is.null(id))
      id <- ""
   
   if (!is.character(id))
      stop("'id' must be NULL or a character vector of length one")
   
   # allow calls of the form:
   #    
   #    insertText("foo")
   #    insertText(text = "foo")
   #    
   # in such cases, we replace the current selection. we pass an empty range
   # and let upstream interpret this as a request to replace the current
   # selection.
   
   if (missing(text) && is.character(location)) {
      text <- location
      location <- list()
   } else if (missing(location) && is.character(text)) {
      text <- text
      location <- list()
   } else if (length(location) == 0) {
      return()
   }
   
   ranges <- .rs.validateAndTransformLocation(location)
   if (!is.character(text))
      stop(invalidTextMsg, call. = FALSE)
   
   if (length(text) != 1 && length(ranges) != length(text))
      stop(invalidLengthMsg, call. = FALSE)
   
   # sort the ranges in decreasing order -- this way, we can
   # ensure the replacements occur correctly (except in the
   # case of overlaps)
   if (length(ranges)) {
      idx <- order(unlist(lapply(ranges, `[[`, 1)))
      
      ranges <- ranges[idx]
      if (length(text) != 1)
         text <- text[idx]
   }
   
   data <- list(ranges = ranges, text = text, id = .rs.scalar(id))
   .rs.enqueEditorClientEvent("replace_ranges", data)
   invisible(data)
})

.rs.addApiFunction("setSelectionRanges", function(ranges, id = "")
{
   ranges <- .rs.validateAndTransformLocation(ranges)
   data <- list(ranges = ranges, id = .rs.scalar(id))
   .rs.enqueEditorClientEvent("set_selection_ranges", data)
   invisible(data)
})

# NOTE: Kept for backwards compatibility with older versions
# of the 'rstudioapi' package -- it is superceded by
# '.rs.getLastActiveEditorContext()'.
.rs.addApiFunction("getActiveDocumentContext", function() {
   .Call("rs_getEditorContext", 0L)
})

.rs.addApiFunction("getLastActiveEditorContext", function() {
   .Call("rs_getEditorContext", 0L)
})

.rs.addApiFunction("getConsoleEditorContext", function() {
   .Call("rs_getEditorContext", 1L)
})

.rs.addApiFunction("getSourceEditorContext", function() {
   .Call("rs_getEditorContext", 2L)
})

.rs.addApiFunction("getActiveProject", function() {
   .rs.getProjectDirectory()
})

.rs.addApiFunction("sendToConsole", function(code,
                                             echo = TRUE,
                                             execute = TRUE,
                                             focus = TRUE)
{
   if (!is.character(code))
      stop("'code' should be a character vector", call. = FALSE)
   
   code <- paste(code, collapse = "\n")
   data <- list(
      code = .rs.scalar(code),
      echo = .rs.scalar(as.logical(echo)),
      execute = .rs.scalar(as.logical(execute)),
      focus = .rs.scalar(as.logical(focus))
   )
   
   .rs.enqueClientEvent("send_to_console", data)
   invisible(data)
})

.rs.addApiFunction("askForPassword", function(prompt) {
   .rs.askForPassword(prompt)
})

.rs.addFunction("dialogIcon", function(name) {
  list(
    info = 1,
    warning = 2,
    error = 3,
    question = 4
  )
})

.rs.addApiFunction("showDialog", function(title, message, url = "") {
   .Call("rs_showDialog",
      title = title,
      message = message,
      dialogIcon = .rs.dialogIcon()$info,
      prompt = FALSE,
      promptDefault = NULL,
      ok = NULL,
      cancel = NULL,
      url = url)
})

.rs.addApiFunction("updateDialog", function(...)
{
   scalarValues <- lapply(list(...), .rs.scalar)
   .rs.enqueClientEvent("update_new_connection_dialog", scalarValues)

   invisible(NULL)
})

.rs.addApiFunction("showPrompt", function(title, message, default = "") {
   .Call("rs_showDialog",
      title = title,
      message = message,
      dialogIcon = .rs.dialogIcon()$info,
      prompt = TRUE,
      promptDefault = default,
      ok = NULL,
      cancel = NULL,
      url = NULL)
})

.rs.addApiFunction("showQuestion", function(title, message, ok = "", cancel = "") {
   .Call("rs_showDialog",
      title = title,
      message = message,
      dialogIcon = .rs.dialogIcon()$question,
      prompt = FALSE,
      promptDefault = NULL,
      ok = ok,
      cancel = cancel,
      url = NULL)
})

.rs.addApiFunction("writePreference", function(name, value) {
  .rs.writeUiPref(paste("rstudioapi", name, sep = "_"), value)
})

.rs.addApiFunction("readPreference", function(name, default = NULL) {
  value <- .rs.readUiPref(paste("rstudioapi", name, sep = "_"))
  if (is.null(value)) default else value
})

.rs.addApiFunction("setPersistentValue", function(name, value) {
   invisible(.Call("rs_setPersistentValue", name, value))
})

.rs.addApiFunction("getPersistentValue", function(name) {
   .Call("rs_getPersistentValue", name)
})

.rs.addApiFunction("getConsoleHasColor", function(name) {
   value <- .rs.readUiPref("ansi_console_mode")
   if (is.null(value) || value != 1) FALSE else TRUE
})

