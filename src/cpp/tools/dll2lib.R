# Put MSVC tools on the PATH.
PATH <- Sys.getenv("PATH")
PATH <- paste("C:/Program Files (x86)/Microsoft Visual Studio/2017/BuildTools/VC/Tools/MSVC/14.14.26428/bin/HostX64/x64", PATH, sep = ";")
Sys.setenv(PATH = PATH)

# Find R DLLs.
dlls <- list.files(R.home("bin"), pattern = "dll$", full.names = TRUE)

# Generate corresponding 'lib' file for each DLL.
for (dll in dlls) {
   
   # check to see if we've already generated our exports
   def <- sub("dll$", "def", dll)
   if (file.exists(def))
      next
   
   # Call it on R.dll to generate exports.
   command <- sprintf("dumpbin.exe /EXPORTS /NOLOGO %s", dll)
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
