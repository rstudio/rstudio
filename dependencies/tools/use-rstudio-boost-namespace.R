
# figure out which files to explore
exts <- c("cpp", "h", "hpp", "inc", "inl", "ipp")
pattern <- sprintf("\\.(%s)$", paste(exts, collapse = "|"))

# find all the files we might need to modify
files <- list.files(
   path       = c("boost", "libs"),
   pattern    = pattern,
   full.names = TRUE,
   recursive  = TRUE
)

# skip examples and tests
files <- grep("/(examples?)/", files, value = TRUE, invert = TRUE)

for (file in files) {
   
   # read the file as a string
   original <- paste(readLines(file, warn = FALSE), collapse = "\n")
   replacement <- original
   
   # make replacements
   replacement <- gsub(
      pattern     = "namespace\\s+boost\\s*{",
      replacement = "namespace rstudio_boost {} namespace boost = rstudio_boost; namespace rstudio_boost {",
      x           = replacement,
      perl        = TRUE
   )
   
   # also needed for some macro Boost stuff
   replacement <- gsub(
      pattern     = "(boost)",
      replacement = "(rstudio_boost)",
      x           = replacement,
      fixed       = TRUE
   )
   
   if (!identical(original, replacement)) {
      writeLines(replacement, con = file, useBytes = TRUE)
      writeLines(sprintf("-- Updated \"%s\"", file))
   }

}
