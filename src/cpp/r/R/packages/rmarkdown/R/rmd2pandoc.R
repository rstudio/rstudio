#' Convert an R Markdown document using pandoc
#'
#' Convert the \code{input} document to the specified \code{format} and write it
#' to \code{output}.
#'
#' @param input Input file
#' @param output Output file (if not specified then a default based on the
#'   specified format is chosen)
#' @param from Options to control the flavor of markdown converted from
#' @param to Pandoc format to convert to (defaults to HTML if not specified)
#' @param options Command line options to pass to pandoc. This should either
#' be a character vector of literal command line options or an object that
#' provices a \code{pandocOptions} S3 method which yields the options.
#' @param envir The environment in which the code chunks are to be evaluated
#'   (can use \code{\link{new.env}()} to guarantee an empty new environment)
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
                       envir = parent.frame(),
                       quiet = FALSE,
                       encoding = getOption("encoding")) {

  # verify we have the minimum version
  verifyPandocVersion()

  # resolve the name of the output file
  if (is.null(output))
    output <- pandocOutputFile(input, to)

  # execute within the input file's directory
  oldwd <- setwd(dirname(tools::file_path_as_absolute(input)))
  on.exit(setwd(oldwd))

  # knit
  md <- knitr::knit(input, quiet = quiet, encoding = encoding)

  # build pandoc args
  args <- c("--from", from,
            "--to", to,
            pandocOptions(options),
            "--output", output,
            md)

  # show pandoc command line if requested
  if (!quiet) {
    cat("pandoc ")
    cat(paste(args, collapse=" "))
    cat("\n")
  }

  # run the conversion
  pandocExec(args)

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

