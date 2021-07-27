args <- commandArgs(TRUE)

# Put the path containing the C compiler on the PATH.
Sys.setenv(PATH = paste(dirname(args[1]), Sys.getenv("PATH"), sep = ";"))

# Find R DLLs.
dlls <- list.files(R.home("bin"), pattern = "dll$", full.names = TRUE)

message("Generating .lib files for DLLs in ", R.home("bin"))

# Generate corresponding 'lib' file for each DLL.
for (dll in dlls) {
   
   # check to see if we've already generated our exports
   def <- sub("dll$", "def", dll)
   if (file.exists(def))
      next
   
   # Call it on R.dll to generate exports.
   command <- sprintf("dumpbin.exe /EXPORTS /NOLOGO %s", dll)
   message("> ", command)
   output <- system(paste(command), intern = TRUE)
   
   # Remove synonyms.
   output <- sub("=.*$", "", output)
   
   # Find start, end markers
   start <- grep("ordinal\\s+hint\\s+RVA\\s+name", output)
   end <- grep("^\\s*Summary\\s*$", output)
   contents <- output[start:(end - 1)]
   contents <- contents[nzchar(contents)]
   
   # Remove forwarded fields
   contents <- grep("forwarded to", contents, invert = TRUE, value = TRUE, fixed = TRUE)
   
   # parse into a table
   tbl <- read.table(text = contents, header = TRUE, stringsAsFactors = FALSE)
   exports <- tbl$name
   
   # sort and re-format exports
   exports <- sort(exports)
   exports <- c("EXPORTS", paste("\t", tbl$name, sep = ""))
   
   # Write the exports to a def file
   def <- sub("dll$", "def", dll)
   cat(exports, file = def, sep = "\n")
   
   # Call 'lib.exe' to generate the library file.
   outfile <- sub("dll$", "lib", dll)
   fmt <- "lib.exe /def:%s /out:%s /machine:%s"
   cmd <- sprintf(fmt, def, outfile, .Platform$r_arch)
   system(cmd)
   
}
