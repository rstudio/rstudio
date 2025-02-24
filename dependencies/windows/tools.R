
print_progress <- function(fmt, ..., prefix) {
   tryCatch({
     cat(sprintf(paste(prefix, fmt, "\n", sep = ""), ...))
   }, error = function(e) {
     cat(paste(prefix, fmt, ..., "\n"))
   })
}

section  <- function(fmt, ...) print_progress(fmt, ..., prefix = "--> ")
progress <- function(fmt, ...) print_progress(fmt, ..., prefix = "-> ")

fatal <- function(fmt, ...) {
   if (interactive()) {
      stop(sprintf(fmt, ...), "\n", call. = FALSE)
   } else {
      err <- paste(fmt, ...) 
      err <- try({
        sprintf(fmt, ...)
      }, silent = TRUE)
      message("FATAL: ", err)
      quit(save = "no", status = 1, runLast = TRUE)
   }  
}

path_program <- function(name) {
   prog <- Sys.which(name)
   if (!nzchar(prog))
      fatal("Failed to find '%s' on PATH", name)
   prog
}

download <- function(url, destfile, ...) {
   progress("Downloading file:\n- '%s' => '%s'", url, destfile)
   exec("wget.exe", "--no-check-certificate", "-c", shQuote(url), "-O", shQuote(destfile))
}


printf <- function(fmt, ...) {
   tryCatch({
     cat(sprintf(fmt, ...))
   }, error = function(e) {
     cat(fmt, ...)
   })
}

PATH <- (function() {
   
   read <- function() {
      unlist(strsplit(Sys.getenv("PATH"), .Platform$path.sep, fixed = TRUE))
   }
   
   write <- function(path) {
      pasted <- paste(path, collapse = .Platform$path.sep)
      Sys.setenv(PATH = pasted)
      invisible(pasted)
   }
   
   prepend <- function(dir) {
      dir <- normalizePath(dir, mustWork = TRUE)
      path <- unique(c(dir, read()))
      write(path)
   }
   
   append <- function(dir) {
      dir <- normalizePath(dir, mustWork = TRUE)
      path <- unique(c(read(), dir))
      write(path)
   }
   
   remove <- function(dir) {
      dir <- normalizePath(dir, mustWork = TRUE)
      path <- setdiff(read(), dir)
      write(path)
   }
   
   list(read = read, prepend = prepend, append = append, remove = remove)
   
})()

execBatch <- function(batchfile) {
  if (!file.exists(batchfile)) {
    fatal("Batch file does not exist: %s", batchfile)
  }
  
  # Execute the batch file and wait for completion
  status <- system2("cmd", args = c("/c", batchfile), 
                    stdout = TRUE, stderr = TRUE)
  
  # Check return status
  if (!is.null(attr(status, "status")) && attr(status, "status") != 0) {
    # Combine output into single message
    output <- paste(status, collapse = "\n")
    fatal("Batch file failed with status %d:\n%s", 
          attr(status, "status"), output)
  }
  
  invisible(TRUE)
}

exec <- function(command,
                 ...,
                 output = NULL,
                 dir = getOption("log.dir", default = getwd()))
{
   # construct path to logfile
   if (is.null(output)) {
      prefix <- paste0(basename(command), "-output-")
      output <- paste(tempfile(prefix, dir), "txt", sep = ".")
   }
   
   # construct command line arguments
   dots <- list(...)
   splat <- unlist(rapply(dots, function(dot) {
      unlist(strsplit(dot, "[[:space:]]+"))
   }))
   
   # print command to console
   print_progress(
      paste(command, paste(splat, collapse = " ")),
      prefix = "> "
   )
   
   # run command
   status <- suppressWarnings(
      system2(command, splat, stdout = output, stderr = output)
   )
   
   # report status
   if (status) {
      msg <- paste0("Command exited with status ", as.integer(status), ".")
      if (is.character(output) && file.exists(output)) {
         logmsg <- paste0("Logs written to ", output, ":\n")
         logmsg <- paste0(logmsg, paste(readLines(output), collapse = "\n"), "\n")
         msg <- paste(msg, logmsg, sep = "\n")
      }
      fatal("%s\n", msg)
   }
   
   invisible(TRUE)
}

enter <- function(dir) {
   progress("Entering directory '%s'", dir)
   dir.create(dir, recursive = TRUE, showWarnings = FALSE)
   setwd(dir)
}

replace_one_line = function(filepath, orig_line, new_line) {
   contents <- readLines(filepath)
   replaced <- gsub(orig_line, new_line, contents, fixed = TRUE)
   if (!identical(contents, replaced))
      writeLines(replaced, filepath)
}

win32_setup <- function() {
   
   # Make sure MSVC tools are available
   msvcCandidates <- c(
      "C:/Program Files (x86)/Microsoft Visual Studio/2019/Community/VC/Auxiliary/Build",
      "C:/Program Files (x86)/Microsoft Visual Studio/2019/BuildTools/VC/Auxiliary/Build"
   )
   
   msvc <- Filter(file.exists, msvcCandidates)
   if (length(msvc) == 0L) {
      message <- paste(
         "No MSVC 2019 installation detected.",
         "Install build tools using 'Install-RStudio-Prereqs.ps1'.
   ")
      fatal(message)
   }
   PATH$prepend(msvc[[1L]])
   
   # Make sure perl is available
   # try to find a perl installation directory
   perlCandidates <- c(
      "C:/Perl64/bin",
      "C:/Perl/bin",
      "C:/Strawberry/perl/bin"
   )
   
   perl <- Filter(file.exists, perlCandidates)
   if (length(perl) == 0L) {
      message <- paste(
         "No perl installation detected.",
         "Please install ActiveState Perl via 'choco install activeperl'."
      )
      fatal(message)
   }
   PATH$prepend(perl[[1L]])
   
}

if (.Platform$OS.type == "windows") {
   win32_setup()
}
