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
   ensure_dir(dir)
   setwd(dir)
}

ensure_dir <- function(...) {
   invisible(lapply(list(...), function(dir) {
      if (!file.exists(dir))
         if (!dir.create(dir, recursive = TRUE))
            fatal("Failed to create directory '%s'\n", dir)
   }))
}

replace_one_line = function(filepath, orig_line, new_line) {
   contents <- readLines(filepath)
   replaced <- gsub(orig_line, new_line, contents, fixed = TRUE)
   if (!identical(contents, replaced))
      writeLines(replaced, filepath)
}
