
pandoc <- function(input,
                   output = NULL,
                   format = NULL,
                   options = c(),
                   quiet = FALSE) {

  verifyPandocVersion()

  args <- c()
  if (!is.null(output))
    args <- append(args, c("--output", output))

  if (!is.null(format))
    args <- append(args, c("--to", format))

  args <- append(args, options)
  args <- append(args, input)

  if (!quiet) {
    cat("pandoc ")
    cat(paste(args, collapse=" "))
    cat("\n")
  }

  execPandoc(args)
}

execPandoc <- function(args, ...) {
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
    versionInfo <- execPandoc("--version", intern = TRUE)
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

