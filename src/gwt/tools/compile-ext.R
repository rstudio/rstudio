#!/usr/bin/env Rscript
library(methods, quietly = TRUE)

if (!require("rvest", quietly = TRUE))
   stop("This script requires 'rvest' to run.")

# Ensure we're in the `src/gwt/` directory
workingDirectory <- getwd()
if (regexpr("src/gwt$", workingDirectory) == -1)
   stop("This script must be run from the `src/gwt` subdirectory")

# Scrape out the 'js' build files
doc <- xml("build.xml")
jscomp <- xml_node(doc, "jscomp")

extractFiles <- function(node) {
   if (identical(xml_tag(node), "externs"))
      return(NULL)
   
   dir <- xml_attr(node, "dir")
   children <- xml_children(node)
   paths <- unlist(lapply(children, function(child) {
      xml_attr(child, "name")
   }))
   file.path(dir, paths)
}

sources <- unname(unlist(lapply(xml_children(jscomp), extractFiles)))

# Collect all of the sources into a single file, and write them out
outputPath <- "src/org/rstudio/studio/client/workbench/views/source/editors/text/ace/acesupport.js"

contents <- unlist(lapply(sources, readLines))
cat(contents, file = outputPath, sep = "\n")
cat("- Successfully compiled 'acesupport.js'")
