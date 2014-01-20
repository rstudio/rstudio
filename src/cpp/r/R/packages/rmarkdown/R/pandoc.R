
#' Convert an R Markdown document using pandoc
#'
#' Convert the \code{input} document to the specified \code{format} and write it
#' to \code{output}.
#'
#' @param input Input file
#' @param output Output file (if not specified then a default based on the
#' specified format is chosen)
#' @param from Options determining the flavor of markdown
#' supported by the conversion
#' @param to Pandoc format to convert to (defaults to HTML if not specified)
#' @param options Command line options to pass to pandoc
#' @param quiet \code{TRUE} to supress printing of the pandoc command line
#' @param encoding the encoding of the input file; see \code{\link{file}}
#'
#' @return The compiled document is written into the output file, and the path
#'   of the output file is returned.
#'
#' @export
rmd2pandoc <- function(input,
                       output = NULL,
                       from = rmdFormat(),
                       to = "html",
                       options = NULL,
                       quiet = FALSE,
                       encoding = getOption("encoding")) {

  # verify we have the minimum version
  verifyPandocVersion()

  # resolve the name of the output file
  if (is.null(output))
    output <- pandocOutputFile(input, to)

  # knit
  md <- knitr::knit(input, quiet = quiet, encoding = encoding)

  # build pandoc args
  args <- c("--from", from,
            "--to", to,
            options,
            "--output", output,
            md,
            recursive = TRUE)

  # show pandoc command line if requested
  if (!quiet) {
    cat("pandoc ")
    cat(paste(args, collapse=" "))
    cat("\n")
  }

  # run the conversion
  execPandoc(args)

  # return the name of the output file
  invisible(output)
}


#' Pandoc options for R Markdown
#'
#' Get the pandoc command line options required to render R Markdown.
#'
#' @param options Additional flags for enabling and disabling markdown
#' features supported during a conversion.
#'
#' @return Pandoc options
#'
#' @export
rmdFormat <- function(options = NULL) {
  paste0("markdown_github",
        "-hard_line_breaks",
        "+superscript",
        "+tex_math_dollars",
        "+raw_html",
        "+auto_identifiers",
        "+raw_tex",
        "+latex_macros",
        "+footnotes",
        "+inline_notes",
        "+citations",
        "+yaml_metadata_block",
        options,
        collapse = "")
}



#' Pandoc options for HTML rendering
#'
#' Get pandoc command-line options required for converting R Markdown to HTML.
#'
#' @param template Full path to a custom pandoc HTML template
#' @param mathjax URL to mathjax library used in HTML output
#'
#' @return Character vector of pandoc options
#'
#' @export
pandocHTMLOptions <- function(template, mathjax = NULL) {
  options <- c("--smart",
               "--template", template,
               "--data-dir", dirname(template),
               "--self-contained",
               "--no-highlight")
  if (!is.null(mathjax)) {
    options <- c(options,
                 "--mathjax",
                 "--variable", paste0("mathjax-url:", mathjax),
                 recursive = TRUE)
  }
  options
}


#' Pandoc options for PDF rendering
#'
#' Get pandoc command-line options required for converting R Markdown PDF.
#'
#' @param geometry List of \code{LaTeX} geometry settings (optional)
#'
#' @return Character vector of pandoc options
#'
#' @export
pandocPDFOptions <- function(geometry = NULL) {

  options <- c()

  if (!is.null(geometry)) {
    for (name in names(geometry)) {
      value <- geometry[[name]]
      options <- c(options,
                   "--variable",
                   paste0("geometry:", name, "=", value),
                   recursive = TRUE)
    }
  }

  options
}


pandocOutputFile <- function(input, pandocFormat) {
  if (pandocFormat %in% c("latex", "beamer"))
    ext <- ".pdf"
  else if (pandocFormat %in% c("html", "html5", "revealjs"))
    ext <- ".html"
  else
    ext <- paste0(".", pandocFormat)
  paste0(tools::file_path_sans_ext(input), ext)
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

