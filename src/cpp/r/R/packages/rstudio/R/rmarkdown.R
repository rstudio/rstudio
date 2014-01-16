

rmd2pandoc <- function(input,
                       format = c("html", "docx", "latex", "beamer"),
                       markdown.options = pandoc_rmarkdown_options(),
                       pandoc.options = NULL,
                       envir = parent.frame(),
                       quiet = FALSE,
                       encoding = getOption("encoding")) {

  # check that we have the right version of pandoc
  pandoc_check_version()

  # match format
  format <- match.arg(format)

  # choose output filename
  output <- paste0(tools::file_path_sans_ext(input),
                   switch(format,
                          html = ".html",
                          docx = ".docx",
                          latex = ".pdf",
                          beamer = ".pdf"))

  # knit document
  render_pandoc_markdown(format)
  md <- paste0(tools::file_path_sans_ext(input), ".md")
  knit(input, md, envir = envir, quiet = quiet, encoding = encoding)

  # call pandoc
  pandoc.options <- c(pandoc.options, "--smart")
  pandoc_convert(md, output, format, markdown.options, pandoc.options)

  # return output filename
  invisible(output)
}


render_pandoc_markdown <- function(format = c("html",
                                              "docx",
                                              "latex",
                                              "beamer")) {
  # verify format
  format <- match.arg(format)

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
                           output,
                           format,
                           markdown.options = pandoc_rmarkdown_options(),
                           pandoc.options = NULL) {
  args <- c("--output", output,
            "--from",
            paste(markdown.options, sep=""),
            "--to", format,
            pandoc.options,
            input, recursive = TRUE)
  cat("pandoc ")
  cat(paste(args, collapse=" "))
  cat("\n")
  pandoc_execute(args)
}

pandoc_rmarkdown_options <- function() {
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

