downloadUpdateInfo <- function(version, os, manual, secure, method) {
  updateUrl <- paste(ifelse(secure, "https", "http"),
                     "://www.rstudio.org/links/check_for_update",
                     "?version=", version, 
                     "&os=", os, 
                     "&format=kvp", sep = "")
  if (isTRUE(manual)) 
  {
    updateUrl <- paste(updateUrl, "&manual=true", sep = "")
  }
  
  # download the URL and read the result
  tmp <- tempfile()
  download.file(updateUrl, tmp, method = method, quiet = TRUE)
  result <- readLines(tmp, warn = FALSE)

  # Print one key-value pair per line:
  # key1=value1
  # key2=value2
  # .. 
  cat(sapply(unlist(strsplit(result, "&")), URLdecode), sep = "\n")
}

