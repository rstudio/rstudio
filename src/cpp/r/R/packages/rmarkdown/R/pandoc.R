

pandocOptions <- function(options) {
  UseMethod("pandocOptions", options)
}

pandocOptions.default <- function(options) {
  options
}

pandocTemplateOptions <- function(template) {
  template <- pandocTemplate(template)
  c("--template", template,
    "--data-dir", dirname(template))
}

pandocTemplate <- function(file) {
  system.file(file.path("templates", file), package = "rmarkdown")
}

pandocOutputFile <- function(input, pandocFormat) {
  if (pandocFormat %in% c("latex", "beamer"))
    ext <- ".pdf"
  else if (pandocFormat %in% c("html", "html5", "revealjs"))
    ext <- ".html"
  else
    ext <- paste0(".", pandocFormat)
  output <- paste0(tools::file_path_sans_ext(input), ext)
  tools::file_path_as_absolute(output)
}

pandocExec <- function(args, ...) {
  pandoc <- pandocPath()
  if (nzchar(pandoc)) {
    command <- paste(pandoc, paste(shQuote(args), collapse = " "))
    system(command, ...)
  } else {
    stop("pandoc was not found on the system path", call. = FALSE)
  }
}

pandocPath <- function() {
  Sys.which("pandoc")
}

verifyPandocVersion <- function() {
  hasPandoc <- hasRequiredPandocVersion()
  if (!hasPandoc) {
    msg <- paste("The pandoc package requires that pandoc version",
                 requiredPandocVersion(),
                 "or greater is installed and available on the path.")
    oldVersion <- attr(hasPandoc, "version")
    if (!is.null(oldVersion))
      msg <- paste(msg, "You currently have version", oldVersion, "installed,",
                   "please update to a newer version.")
    else
      msg <- paste(msg, "No version of pandoc was found on the path.")

    stop(msg, call.=FALSE)
  }
}

hasRequiredPandocVersion <- function() {
  if (nzchar(pandocPath())) {
    versionInfo <- pandocExec("--version", intern = TRUE)
    version <- strsplit(versionInfo, "\n")[[1]][1]
    version <- strsplit(version, " ")[[1]][2]
    hasRequired <- numeric_version(version) >= requiredPandocVersion()
    if (hasRequired) {
      TRUE
    } else {
      attr(hasRequired, "version") <- version
      hasRequired
    }
  } else {
    FALSE
  }
}

requiredPandocVersion <- function() {
  "1.12.3"
}





