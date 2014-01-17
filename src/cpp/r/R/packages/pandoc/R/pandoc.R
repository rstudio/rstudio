
#' Convert a document using pandoc
#' 
#' Convert a document from one type to another using pandoc.
#' 
#' @param input input document
#' @param output output document (if not specified then defaults to 
#'   <input>.html)
#' @param format format to convert to (if not specified then is deduced from the
#'   output file extension).
#' @param options character vector of command line options to pass to pandoc.
#' @param quiet whether to suppress messages during conversion
#' @return the exit code of the call to pandoc  
#' 
#' @export
convert <- function(input,
                    output = NULL,
                    format = NULL,
                    options = c(),
                    quiet = FALSE) {
  
  verify_version()
  
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

pandoc_execute <- function(args, ...) {
  pandoc <- pandoc_path()
  if (nzchar(pandoc)) {
    command <- paste(pandoc, paste(shQuote(args), collapse = " "))
    system(command, ...)
  } else {
    stop("pandoc was not found on the system path", call. = FALSE)
  }
}

pandoc_path <- function() {
  Sys.which("pandoc")
}

verify_version <- function() {
  has_pandoc <- has_required_version()
  if (!has_pandoc) {
    msg <- paste("The pandoc package requires that pandoc version",
                 required_version(),
                 "or greater is installed and available on the path.")
    old_version <- attr(has_pandoc, "version")
    if (!is.null(old_version))
      msg <- paste(msg, "You currently have version", old_version, "installed,",
                   "please update to a newer version.")
    else
      msg <- paste(msg, "No version of pandoc was found on the path.")
    
    stop(msg, call.=FALSE)
  }
}

has_required_version <- function() {
  if (nzchar(pandoc_path())) {
    version_info <- pandoc_execute("--version", intern = TRUE)
    version <- strsplit(version_info, "\n")[[1]][1]
    version <- strsplit(version, " ")[[1]][2]
    has_required <- numeric_version(version) >= required_version()
    if (has_required) {
      TRUE
    } else {
      attr(has_required, "version") <- version
      has_required
    }
  } else {
    FALSE
  }
}

required_version <- function() {
  "1.12.3"
}

