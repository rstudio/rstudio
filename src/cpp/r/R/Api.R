.rs.addApiFunction("restartSession", function(command = NULL) {
   command <- as.character(command)
   invisible(.rs.restartR(command))
})

.rs.addApiFunction("initializeProject", function(path = getwd())
{
   path <- .rs.ensureScalarCharacter(path)
   
   # if this is an existing file ...
   if (file.exists(path))
   {
      # ... and it's an .Rproj file, then just return that (don't 
      # re-initialize the project)
      if (grepl("[.]Rproj$", path))
         return(.rs.normalizePath(path, winslash = "/"))
      
      # ... and it's not a directory, bail
      if (!utils::file_test("-d", path))
      {
         fmt <- "file '%s' exists and is not a directory"
         stop(sprintf(fmt, .rs.createAliasedPath(path)))
      }
   }
   
   # otherwise, assume we've received the path to a directory, and
   # attempt to initialize a project within that directory
   .rs.ensureDirectory(path)
   
   # check to see if a .Rproj file already exists in that directory;
   # if so, then we don't need to re-initialize
   rProjFiles <- list.files(path, 
                            pattern = "[.]Rproj$",
                            full.names = TRUE)
   
   # if we already have a .Rproj file, just return that
   if (length(rProjFiles))
      return(.rs.normalizePath(rProjFiles[[1]], winslash = "/"))
   
   # otherwise, attempt to create a new .Rproj file, and return
   # the path to the generated file
   rProjFile <- file.path(
      normalizePath(path, mustWork = TRUE, winslash = "/"),
      paste(basename(path), "Rproj", sep = ".")
   )
   
   success <- .Call(
      "rs_writeProjectFile",
      rProjFile,
      PACKAGE = "(embedding)"
   )
   
   if (!success)
   {
      fmt <- "failed to initialize RStudio project in directory '%s'"
      stop(sprintf(fmt, .rs.createAliasedPath(path)))
   }
   
   return(rProjFile)
   
})

.rs.addApiFunction("openProject", function(path = NULL,
                                           newSession = FALSE)
{
   # default to current project directory (note that this can be
   # NULL if the user is not within an RStudio project)
   if (is.null(path))
     path <- .rs.getProjectDirectory()
   
   path <- .rs.ensureScalarCharacter(path)
   
   # attempt to initialize project if necessary
   rProjFile <- .rs.api.initializeProject(path)
   
   # request that we open this project
   invisible(
      .Call("rs_requestOpenProject",
            rProjFile,
            newSession,
            PACKAGE = "(embedding)")
   )
})

.rs.addApiFunction("versionInfo", function() {
  info <- list()
  info$citation <- .Call("rs_rstudioCitation", PACKAGE = "(embedding)")
  info$mode <- .Call("rs_rstudioProgramMode", PACKAGE = "(embedding)")
  info$edition <- .Call("rs_rstudioEdition", PACKAGE = "(embedding)")
  info$version <- .Call("rs_rstudioVersion", PACKAGE = "(embedding)")
  info$version <- package_version(info$version)
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
   
   # transform numeric line, column values to integer
   if (is.numeric(line))
      line <- as.integer(line)
   
   if (is.numeric(col))
      col <- as.integer(col)

   # validate line/col arguments
   if (!is.integer(line) || length(line) != 1 ||
       !is.integer(col)  || length(col) != 1) {
      stop("line and column must be numeric values.")
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
      focus = .rs.scalar(as.logical(focus)),
      language = "R"
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
   
   # ensure URL is a string
   if (is.null(url) || is.na(url))
      url <- ""
   
   .Call("rs_showDialog",
      title = title,
      message = message,
      dialogIcon = .rs.dialogIcon()$info,
      prompt = FALSE,
      promptDefault = "",
      ok = "OK",
      cancel = "Cancel",
      url = url)
})

.rs.addApiFunction("updateDialog", function(...)
{
   scalarValues <- lapply(list(...), .rs.scalar)
   .rs.enqueClientEvent("update_new_connection_dialog", scalarValues)

   invisible(NULL)
})

.rs.addApiFunction("showPrompt", function(title, message, default = "") {
   
   # ensure default is a string
   if (is.null(default) || is.na(default))
      default <- ""
   
   .Call("rs_showDialog",
      title = title,
      message = message,
      dialogIcon = .rs.dialogIcon()$info,
      prompt = TRUE,
      promptDefault = default,
      ok = "OK",
      cancel = "Cancel",
      url = "")
})

.rs.addApiFunction("showQuestion", function(title, message, ok = "OK", cancel = "Cancel") {
   
   # fix up ok, cancel
   if (is.null(ok) || is.na(ok))
      ok <- "OK"
   
   if (is.null(cancel) || is.na(cancel))
      cancel <- "Cancel"
   
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

.rs.addApiFunction("documentSave", function(id = NULL) {
   # If no ID is specified, try to save the active editor.
   if (is.null(id)) {
      context <- .rs.api.getSourceEditorContext()
      if (!is.null(context)) {
         id <- context$id
      }
   }
   if (is.null(id)) {
      # No ID specified and no document open; succeed without meaning
      return(TRUE)
   }
   .Call("rs_requestDocumentSave", id, PACKAGE = "(embedding)")
})

.rs.addApiFunction("documentSaveAll", function() {
   .Call("rs_requestDocumentSave", NULL, PACKAGE = "(embedding)")
})

.rs.addApiFunction("documentNew", function(type, code, row = 0, column= 0, execute = FALSE) {
   type <- switch(
      type,
      rmarkdown = "r_markdown",
      sql = "sql",
      "r_script"
   )

   .rs.enqueClientEvent("new_document_with_code", list(
      type = .rs.scalar(type),
      code = .rs.scalar(code),
      row = .rs.scalar(row),
      column = .rs.scalar(column),
      execute = .rs.scalar(execute)
   ))

   invisible(NULL)
})

.rs.addApiFunction("documentClose", function(id = NULL, save = TRUE) {
   # If no ID is specified, try to close the active editor.
   if (is.null(id)) {
      context <- .rs.api.getSourceEditorContext()
      if (!is.null(context)) {
         id <- context$id
      }
   }
   if (is.null(id)) {
      # No ID specified and no document open; succeed without meaning
      return(TRUE)
   }
   .Call("rs_requestDocumentClose", id, save, PACKAGE = "(embedding)")
})

.rs.addApiFunction("getConsoleHasColor", function(name) {
   value <- .rs.readUiPref("ansi_console_mode")
   if (is.null(value) || value != 1) FALSE else TRUE
})

.rs.addApiFunction("terminalSend", function(id, text) {
   if (!is.character(text))
      stop("'text' should be a character vector", call. = FALSE)

   if (is.null(id) || !is.character(id) || length(id) != 1)
      stop("'id' must be a character vector of length one")

  .Call("rs_terminalSend", id, text)
   invisible(NULL)
})

.rs.addApiFunction("terminalClear", function(id) {
   if (is.null(id) || !is.character(id) || length(id) != 1)
      stop("'id' must be a character vector of length one")

  .Call("rs_terminalClear", id)
  invisible(NULL)
})

.rs.addApiFunction("terminalCreate", function(caption = NULL, show = TRUE, shellType = NULL) {
   if (!is.null(caption) && (!is.character(caption) || (length(caption) != 1)))
      stop("'caption' must be NULL or a character vector of length one")

   if (is.null(show) || !is.logical(show))
      stop("'show' must be a logical vector")

   if (!is.null(shellType) && (!is.character(shellType) || (length(shellType) != 1)))
      stop("'shellType' must be NULL or a character vector of length one")

   validShellType = TRUE
   if (!is.null(shellType)) {
      validShellType <- tolower(shellType) %in% c("default", "win-cmd", 
            "win-ps", "win-git-bash", "win-wsl-bash", "custom")
   }      
   if (!validShellType)
      stop("'shellType' must be NULL, or one of 'default', 'win-cmd', 'win-ps', 'win-git-bash', 'win-wsl-bash', or 'custom'.") 

   .Call("rs_terminalCreate", caption, show, shellType)
})

.rs.addApiFunction("terminalBusy", function(id) {
   if (is.null(id) || !is.character(id))
      stop("'id' must be a character vector")

   .Call("rs_terminalBusy", id)
})

.rs.addApiFunction("terminalRunning", function(id) {
   if (is.null(id) || !is.character(id))
      stop("'id' must be a character vector")

   .Call("rs_terminalRunning", id)
})

.rs.addApiFunction("terminalList", function() {
   .Call("rs_terminalList")
})

.rs.addApiFunction("terminalContext", function(id) {
   if (is.null(id) || !is.character(id) || (length(id) != 1))
      stop("'id' must be a single element character vector")

   .Call("rs_terminalContext", id)
})

.rs.addApiFunction("terminalActivate", function(id = NULL, show = TRUE) {
   if (!is.null(id) && (!is.character(id) || (length(id) != 1)))
      stop("'id' must be NULL or a character vector of length one")

   if (!is.logical(show))
     stop("'show' must be TRUE or FALSE")

   .Call("rs_terminalActivate", id, show)
   invisible(NULL)
})

.rs.addApiFunction("terminalBuffer", function(id, stripAnsi = TRUE) {
   if (is.null(id) || !is.character(id) || (length(id) != 1))
      stop("'id' must be a single element character vector")

   if (is.null(stripAnsi) || !is.logical(stripAnsi))
      stop("'stripAnsi' must be a logical vector")

   .Call("rs_terminalBuffer", id, stripAnsi)
})

.rs.addApiFunction("terminalKill", function(id) {
   if (is.null(id) || !is.character(id))
      stop("'id' must be a character vector")

   .Call("rs_terminalKill", id)
   invisible(NULL)
})

.rs.addApiFunction("terminalVisible", function() {
   .Call("rs_terminalVisible")
})

.rs.addApiFunction("terminalExecute", function(command,
                                               workingDir = NULL,
                                               env = character(),
                                               show = TRUE) {
   if (is.null(command) || !is.character(command) || (length(command) != 1))
      stop("'command' must be a single element character vector")
   if (!is.null(workingDir) && (!is.character(workingDir) || (length(workingDir) != 1)))
      stop("'workingDir' must be a single element character vector")
   if (!is.null(env) && !is.character(env))
      stop("'env' must be a character vector")
   if (is.null(show) || !is.logical(show))
      stop("'show' must be a logical vector")

   .Call("rs_terminalExecute", command, workingDir, env, show)
})

.rs.addApiFunction("terminalExitCode", function(id) {
   if (is.null(id) || !is.character(id) || (length(id) != 1))
      stop("'id' must be a single element character vector")

   .Call("rs_terminalExitCode", id)
})

options(terminal.manager = list(terminalActivate = .rs.api.terminalActivate,
                                terminalCreate = .rs.api.terminalCreate,
                                terminalClear = .rs.api.terminalClear,
                                terminalList = .rs.api.terminalList,
                                terminalContext = .rs.api.terminalContext,
                                terminalBuffer = .rs.api.terminalBuffer,
                                terminalVisible = .rs.api.terminalVisible,
                                terminalBusy = .rs.api.terminalBusy,
                                terminalRunning = .rs.api.terminalRunning,
                                terminalKill = .rs.api.terminalKill,
                                terminalSend = .rs.api.terminalSend,
                                terminalExecute = .rs.api.terminalExecute,
                                terminalExitCode = .rs.api.terminalExitCode))

.rs.addApiFunction("selectFile", function(
   caption = "Select File",
   label = "Select",
   path = .rs.getProjectDirectory(),
   filter = NULL,
   existing = TRUE)
{
   .Call("rs_openFileDialog",
         1L,
         caption,
         label,
         path,
         filter,
         existing,
         PACKAGE = "(embedding)")
})

.rs.addApiFunction("selectDirectory", function(
   caption = "Select Directory",
   label = "Select",
   path = .rs.getProjectDirectory())
{
   .Call("rs_openFileDialog",
         2L,
         caption,
         label,
         path,
         NULL,
         TRUE,
         PACKAGE = "(embedding)")
})

.rs.addApiFunction("getThemeInfo", function() {
   
   # read theme preferences
   global <- .rs.readUiPref("flat_theme")

   theme <- .rs.readUiPref("rstheme")
   if (is.null(theme))
      theme <- list("name" = "Textmate (default)", "isDark" = FALSE)

   global <- switch(
      if (is.null(global)) "" else global,
      alternate = "Sky",
      default = "Modern",
      "Classic"
   )

   # default/fallback theme colors 
   foreground <- "#000000";
   background <- "#FFFFFF";

   # attempt to read colors from browser
   colors <- .Call("rs_getThemeColors", PACKAGE = "(embedding)")
   if (!is.null(colors)) {
      foreground <- colors$foreground
      background <- colors$background
   }

   list(
      editor = theme$name,
      global = global,
      dark = theme$isDark,
      foreground = foreground,
      background = background
   )
})

.rs.addApiFunction("askForSecret", function(name, title, prompt) {
   .rs.askForSecret(name, title, prompt)
})

.rs.addApiFunction("previewSql", function(conn, statement, ...) {
   .rs.previewSql(conn, statement, ...)
})

.rs.addApiFunction("buildToolsCheck", function() {
   .Call("rs_canBuildCpp", PACKAGE = "(embedding)")
})

.rs.addApiFunction("buildToolsInstall", function(action) {
   
   # skip prompt if requested explicitly
   if (is.null(action) || !nzchar(action))
      return(.Call("rs_installBuildTools", PACKAGE = "(embedding)"))
   
   # otherwise, call prompting version
   .rs.installBuildTools(action)
})

.rs.addApiFunction("buildToolsExec", function(expr) {
   .rs.withBuildTools(expr)
})

.rs.addApiFunction("dictionariesPath", function() {
   .Call("rs_dictionariesPath", PACKAGE = "(embedding)")
})

.rs.addApiFunction("userDictionariesPath", function() {
   .Call("rs_userDictionariesPath", PACKAGE = "(embedding)")
})

# translate a local URL into an externally accessible URL on RStudio Server
.rs.addApiFunction("translateLocalUrl", function(url, absolute = FALSE) {
  .Call("rs_translateLocalUrl", url, absolute, PACKAGE = "(embedding)")
})

# execute an arbitrary RStudio application command (AppCommand)
.rs.addApiFunction("executeCommand", function(commandId, quiet = FALSE) {
  .Call("rs_executeAppCommand", commandId, quiet, PACKAGE = "(embedding)")
})

