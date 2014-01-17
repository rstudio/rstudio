


# In RStudio the default rmarkdown engine for a document is v2
#
# To specify v1 use:
#
# <!-- rmarkdown v1 -->
#
# To specify an alternate engine use a function name:
#
# <!-- rmarkdown pubtools::rmd2foo -->
#
# Functions must accept an input and encoding argument, and must
# return the name of the output file generated
#
# To pass arguments to the function you do this:
#
# <!-- rmarkdown pubtools::rmd2foo toc=TRUE -->
#


rmd2pandoc <- function(input,
                       output = NULL,
                       format = NULL,
                       markdown.options = default_markdown_options(),
                       pandoc.options = default_pandoc_options(),
                       envir = parent.frame(),
                       quiet = FALSE,
                       encoding = getOption("encoding")) {

  # default output to html if not provided
  if (is.null(output))
    output <- paste0(tools::file_path_sans_ext(input), ".html")

  # knit document
  render_pandoc_markdown(output, format)
  md <- paste0(tools::file_path_sans_ext(input), ".md")
  knit(input, md, envir = envir, quiet = quiet, encoding = encoding)

  # build options
  options <-c("--from", paste0(markdown.options, collapse=""))
  options <- append(options, pandoc.options)

  # call pandoc
  pandoc_convert(md, output, format, options, quiet)

  # return output filename
  invisible(output)
}


render_pandoc_markdown <- function(output, format = NULL) {

  # guess the format if it's not provided. it's not critical that we
  # have a format, but when we do then it's possible to customize
  # markdown rendering to optimize for the intended output
  if (is.null(format)) {
    ext <- tools::file_ext(output)
    if (ext %in% c("html", "docx", "odt", "rtf"))
      format <- ext
    else if (identical(ext, "pdf"))
      format <- "latex"
    else
      format <- "unknown"
  }

  # stock markdown options
  knitr::render_markdown()

  # chunk options (do a figure directory per-format to gracefully handle
  # switching between formats)
  knitr::opts_chunk$set(tidy = FALSE,
                        error = FALSE,
                        fig.path=paste("figure-", format, "/", sep = ""))

  # some pdf specific options
  if (format %in% c("latex", "beamer")) {
    knitr::opts_chunk$set(dev = 'cairo_pdf')
    knitr::knit_hooks$set(crop = knitr::hook_pdfcrop)
  }
}

pandoc_convert <- function(input,
                           output = NULL,
                           format = NULL,
                           options = NULL,
                           quiet = FALSE) {

  pandoc_check_version()

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

  pandoc_execute(args)
}

default_markdown_options <- function() {
  c("markdown_github",
    "-hard_line_breaks",
    "+superscript",
    "+tex_math_dollars",
    "+raw_html",
    "+auto_identifiers",
    "+raw_tex",
    "+latex_macros",
    "+implicit_figures",
    "+footnotes",
    "+inline_notes",
    "+citations",
    "+pandoc_title_block",
    "+yaml_metadata_block")
}

default_pandoc_options <- function() {
  c("--smart")
}

pandoc_execute <- function(args, ...) {
  pandoc <- pandoc_path()
  if (nzchar(pandoc)) {
    command <- paste(pandoc, paste(shQuote(args), collapse = " "))
    system(command, ...)
  } else {
    stop("pandoc was not found on the system path", call. = FALSE)
  }
}

pandoc_check_version <- function() {
  haspandoc <- pandoc_has_required_version()
  if (!haspandoc) {
    msg <- paste("The pandoc package requires that pandoc version",
                 pandoc_required_version(),
                 "or greater is installed and available on the path.")
    oldVersion <- attr(haspandoc, "version")
    if (!is.null(oldVersion))
      msg <- paste(msg, "You currently have version", oldVersion, "installed,",
                   "please update to a newer version.")
    else
      msg <- paste(msg, "No version of pandoc was found on the path.")

    stop(msg, call.=FALSE)
  }
}

pandoc_has_required_version <- function() {
  if (nzchar(pandoc_path())) {
    versioninfo <- pandoc_execute("--version", intern = TRUE)
    version <- strsplit(versioninfo, "\n")[[1]][1]
    version <- strsplit(version, " ")[[1]][2]
    hasrequired <- numeric_version(version) >= pandoc_required_version()
    if (hasrequired) {
      TRUE
    } else {
      attr(hasrequired, "version") <- version
      hasrequired
    }
  } else {
    FALSE
  }
}

pandoc_path <- function() {
  Sys.which("pandoc")
}

pandoc_required_version <- function() {
  "1.12.3"
}

