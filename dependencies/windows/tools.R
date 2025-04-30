
`%||%` <- function(x, y) {
   if (is.null(x)) y else x
}

sprintf <- function(fmt, ...) {
   if (nargs() == 1L) fmt else base::sprintf(fmt, ...)
}

printf <- function(fmt, ...) {
   msg <- sprintf(fmt, ...)
   cat(msg, sep = "")
}

section <- function(fmt, ...) {
   msg <- sprintf(fmt, ...)
   printf("\n== %s\n", msg)
}

progress <- function(fmt, ...) {
   msg <- sprintf(fmt, ...)
   printf("-- %s\n", msg)
}

fatal <- function(fmt, ...) {

   msg <- sprintf(fmt, ...)
   if (interactive())
      stop(msg, call. = FALSE)

   printf("!!\n!! ERROR: %s\n!!\n", msg)
   quit(save = "no", status = 1, runLast = TRUE)

}

path_program <- function(name) {

   prog <- Sys.which(name)
   if (!nzchar(prog))
      fatal("failed to find %s on PATH", shQuote(name))

   prog

}

execBatch <- function(batchfile) {

   if (!file.exists(batchfile))
      fatal("batch file %s does not exist", shQuote(batchfile))

   # Execute the batch file and wait for completion
   output <- system2(
      command = "cmd",
      args    = c("/c", shQuote(batchfile)),
      stdout  = TRUE,
      stderr  = TRUE
   )

   # Check return status
   status <- attr(output, "status") %||% 0L
   if (status != 0L) {
      fmt <- "error executing %s [error code %i]\n%s\n"
      fatal(fmt, shQuote(batchfile), as.integer(status), output)
   }

   invisible(TRUE)
}

exec <- function(command, ..., output = NULL, dir = NULL) {

   # construct path to logfile
   if (is.null(output)) {
      dir <- dir %||% getOption("log.dir", default = getwd())
      prefix <- paste0(basename(command), "-output-")
      output <- paste(tempfile(prefix, dir), "txt", sep = ".")
   }

   # construct command line arguments
   dots <- list(...)
   splat <- unlist(rapply(dots, function(dot) {
      unlist(strsplit(dot, "[[:space:]]+"))
   }))

   # print command to console
   cmd <- paste(command, paste(splat, collapse = " "))
   printf("> %s\n", cmd)

   # run command
   output <- suppressWarnings(
      system2(
         command = command,
         args    = splat,
         stdout  = output,
         stderr  = output
      )
   )

   # retrieve status, accommodating fact that form of output depends on
   # what we passed to 'stdout' and 'stderr'
   status <- if (is.integer(output)) {
      output
   } else {
      attr(output, "status") %||% 0L
   }

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

run <- function(...) {

   command <- paste(c(...), "2>&1", collapse = " ")
   writeLines(paste(">", command))

   conn <- pipe(command, open = "rb")
   if (!isOpen(conn))
      fatal("execution of %s failed", command)

   while (TRUE) {
      output <- readLines(conn, n = 1L, skipNul = TRUE, warn = FALSE)
      writeLines(output)
      if (length(output) == 0)
         break
   }

   result <- close(conn)
   if (is.integer(result) && result != 0L)
      fatal("execution of %s failed [status %i]", command, result)

   invisible(result)

}

download <- function(url, destfile, ...) {
   fmt <- "Downloading file:\n- %s => %s"
   progress(fmt, shQuote(url), shQuote(destfile))
   exec("wget.exe", "--no-check-certificate", "-c", shQuote(url), "-O", shQuote(destfile))
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

   list(
      read = read,
      prepend = prepend,
      append = append,
      remove = remove
   )

})()

enter <- function(dir) {
   progress("Entering directory %s", shQuote(dir))
   dir.create(dir, recursive = TRUE, showWarnings = FALSE)
   setwd(dir)
}

replace_one_line <- function(filepath, orig_line, new_line) {
   contents <- readLines(filepath)
   replaced <- gsub(orig_line, new_line, contents, fixed = TRUE)
   if (!identical(contents, replaced))
      writeLines(replaced, filepath)
}

interpolate <- function(string) {

   result <- string

   # get variable names used within the string
   starts <- gregexpr("{", string, perl = TRUE)[[1L]]
   ends <- gregexpr("}", string, perl = TRUE)[[1L]]
   exprs <- substring(string, starts + 1L, ends - 1L)

   # replace with their formatted values
   for (expr in exprs) {
      value <- eval(parse(text = expr), envir = parent.frame())
      pattern <- sprintf("{%s}", expr)
      replace <- paste(as.character(value), collapse = " ")
      result <- gsub(pattern, replace, result, fixed = TRUE)
   }

   result

}

initialize <- function() {

   # Make sure MSVC tools are available
   msvcCandidates <- c(
      "C:/Program Files/Microsoft Visual Studio/2022/BuildTools/VC/Auxiliary/Build",
      "C:/Program Files/Microsoft Visual Studio/2022/Community/VC/Auxiliary/Build",
      "C:/Program Files (x86)/Microsoft Visual Studio/2022/BuildTools/VC/Auxiliary/Build",
      "C:/Program Files (x86)/Microsoft Visual Studio/2022/Community/VC/Auxiliary/Build"
   )

   msvc <- Filter(file.exists, msvcCandidates)
   if (length(msvc) == 0L) {
      message <- paste(
         "No MSVC 2022 installation detected.",
         "Install build tools using 'Install-RStudio-Prereqs.ps1'."
      )
      fatal(message)
   }
   PATH$prepend(msvc[[1L]])

   # Make sure perl is available
   # try to find a perl installation directory
   perlCandidates <- c(
      "C:/Strawberry/perl/bin",
      "C:/Perl64/bin",
      "C:/Perl/bin"
   )

   perl <- Filter(file.exists, perlCandidates)
   if (length(perl) == 0L) {
      message <- paste(
         "No perl installation detected.",
         "Please install Strawberry Perl via 'choco install strawberryperl'."
      )
      fatal(message)
   }
   PATH$prepend(perl[[1L]])

}

initialize()
