# Progress Reporting ----

print_progress <- function(fmt, ..., prefix) {
   cat(sprintf(paste(prefix, fmt, "\n", sep = ""), ...))
}

section  <- function(fmt, ...) print_progress(fmt, ..., prefix = "--> ")
progress <- function(fmt, ...) print_progress(fmt, ..., prefix = "-> ")

fatal <- function(fmt, ...) {
   if (interactive()) {
      stop("FATAL: ", sprintf(fmt, ...), "\n", call. = FALSE)
   } else {
      message("FATAL: ", sprintf(fmt, ...))
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
   cat(sprintf(fmt, ...))
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

exec <- function(..., args = NULL) {
   
   args <- if (is.null(args))
      list(...)
   else
      args
   
   cmd <- paste(lapply(args, paste, collapse = " "), collapse = " ")
   
   print_progress(cmd, prefix = "> ")
   system(cmd)
}

ensure_dir <- function(...) {
   invisible(lapply(list(...), function(dir) {
      if (!file.exists(dir))
         if (!dir.create(dir, recursive = TRUE))
            fatal("Failed to create directory '%s'\n", dir)
   }))
}

enter <- function(dir) {
   ensure_dir(dir)
   setwd(dir)
}
