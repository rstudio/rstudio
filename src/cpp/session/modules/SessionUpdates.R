downloadUpdateInfo <- function(version, os, manual) {
  updateUrl <- paste("http://www.rstudio.org/links/check_for_update", 
                     "?version=", version, 
                     "&os=", os, 
                     "&format=kvp", sep = "")
  if (isTRUE(manual)) 
  {
    updateUrl <- paste(updateUrl, "&manual=true", sep = "")
  }
  
  # Open the URL and read the result
  conn <- url(updateUrl, open = "rt")
  on.exit(close(conn), add = TRUE)  
  result <- readLines(conn, warn = FALSE)

  # Print one key-value pair per line:
  # key1=value1
  # key2=value2
  # .. 
  cat(sapply(unlist(strsplit(result, "&")), URLdecode), sep = "\n")
}

