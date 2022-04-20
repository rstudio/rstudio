#!/usr/bin/env Rscript

# Find translations defined for one string, but not another, in our
# translation property files.

readTranslationFile <- function(path) {
   
   # read the file
   contents <- readLines(path, warn = FALSE)
   eqIndex <- regexpr("=", contents, fixed = TRUE)
   
   # drop lines without an '='
   contents <- contents[eqIndex != -1]
   eqIndex <- eqIndex[eqIndex != -1]
   
   keys <- trimws(substring(contents, 1L, eqIndex - 1L))
   vals <- trimws(substring(contents, eqIndex + 1L))
   data.frame(Key = keys, Value = vals)
   
}

# look for all of the English property files
root <- normalizePath("../../..", winslash = "/")
enFiles <- list.files(
   path = file.path(root, "src/gwt/src"),
   pattern = "_en\\.properties$",
   all.files = TRUE,
   full.names = TRUE,
   recursive = TRUE
)

status <- lapply(enFiles, function(enFile) {
   
   # Flag marking if any issues were found.
   ok <- TRUE
   
   # TODO: refactor this for more (non_French) extensions.
   # TODO: optionally report files without a corresponding translation file
   frFile <- gsub("_en", "_fr", enFile)
   if (!file.exists(frFile))
      return(ok)
   
   # Find terms defined in one translation file, but not the other.
   enData <- readTranslationFile(enFile)
   frData <- readTranslationFile(frFile)
   
   missingEnTerms <- setdiff(enData$Key, frData$Key)
   if (length(missingEnTerms)) {
      message(paste("#", enFile))
      message(paste(missingEnTerms, collapse = " "))
      ok <- FALSE
   }
   
   missingFrTerms <- setdiff(frData$Key, enData$Key)
   if (length(missingFrTerms)) {
      message(paste("#", frFile))
      message(paste(missingFrTerms, collapse = " "))
      ok <- FALSE
   }
   
   if (!ok)
      writeLines("")
   
   ok
   
})

# For non-interactive usages, return with an exit code depending on whether
# there were any missing translations.
if (!interactive()) {
   allOk <- all(unlist(status))
   quit(save = "no", status = if (allOk) 0L else 1L)
}

