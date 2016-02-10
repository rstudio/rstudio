#
# SessionRmdNotebook.R
#
# Copyright (C) 2009-16 by RStudio, Inc.
#
# Unless you have received this program directly from RStudio pursuant
# to the terms of a commercial license agreement with RStudio, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

.rs.addFunction("executeSingleChunk", function(options,
                                               content,
                                               libDir,
                                               headerFile,
                                               outputFile) 
{
  # create a temporary file stub to send to R Markdown
  chunkFile <- tempfile(fileext = ".Rmd")
  chunk <- paste(paste("```{r", options, "echo=FALSE}"),
                 content,
                 "```\n", 
                 sep = "\n");
  writeLines(chunk, con = chunkFile)
  on.exit(unlink(chunkFile), add = TRUE)
  
  # render chunks directly in .GlobalEnv
  # TODO: use .rs.getActiveFrame()? use sandbox env
  # that has .GlobalEnv as parent?
  envir <- .GlobalEnv
                 
  # render the stub to the given file
  capture.output(rmarkdown::render(input = chunkFile, 
                                   output_format = rmarkdown::html_document(
                                     theme = NULL,
                                     highlight = NULL,
                                     template = NULL, 
                                     self_contained = FALSE,
                                     includes = list(
                                       in_header = headerFile),
                                     lib_dir = libDir),
                                   output_file = outputFile,
                                   encoding = "UTF-8",
                                   envir = envir,
                                   quiet = TRUE))
  invisible(NULL)
})

.rs.addFunction("createNotebook", function(input, output = NULL, envir = .GlobalEnv)
{
   find_chunks <- function(contents) {
      chunkStarts <- grep("^\\s*```{", contents, perl = TRUE)
      chunkEnds <- grep("^\\s*```\\s*$", contents, perl = TRUE)
      chunkRanges <- Map(list, start = chunkStarts, end = chunkEnds)
      lapply(chunkRanges, function(range) {
         list(start = range$start,
              end = range$end,
              header = contents[range$start],
              contents = contents[(range$start + 1):(range$end - 1)])
      })
   }
   
   chunk_annotation <- function(name, index, row) {
      sprintf("\n\n<!-- rnb-chunk-%s-%s %s -->\n\n", name, index, row)
   }
   
   input <- normalizePath(input, winslash = "/", mustWork = TRUE)
   contents <- readLines(input, warn = FALSE)
   
   # inject placeholders so we can track chunk locations after render
   # ensure that the comments are surrounded by newlines, as otherwise
   # strange render errors can occur
   modified <- contents
   chunks <- find_chunks(modified)
   for (i in seq_along(chunks)) {
      startIdx <- chunks[[i]]$start
      modified[startIdx] <- paste(
         chunk_annotation("start", i, startIdx),
         modified[startIdx],
         sep = ""
      )
      
      endIdx <- chunks[[i]]$end
      modified[endIdx] <- paste(
         modified[endIdx],
         chunk_annotation("end", i, endIdx),
         sep = ""
      )
   }
   
   # inject base64-encoded document into document
   # (i heard you like documents, so i put a document in your document)
   partitioned <- rmarkdown:::partition_yaml_front_matter(modified)
   encoded <- caTools::base64encode(paste(c(contents, ""), collapse = "\n"))
   injected <- c(
      partitioned$front_matter,
      "",
      sprintf("<!-- rnb-document-source %s -->", encoded),
      "",
      partitioned$body
   )
   
   # write out file and prepare for render
   inputFile <- tempfile("rnb-input-file-", fileext = ".Rmd")
   writeLines(injected, inputFile)
   
   # render the document
   outputFormat <- rmarkdown::html_document()
   
   if (is.null(output))
      output <- paste(tools::file_path_sans_ext(input), "Rnb", sep = ".")
   
   if (!file.exists(dirname(output)))
      dir.create(dirname(output), recursive = TRUE)
   
   rmarkdown::render(input = inputFile,
                     output_format = outputFormat,
                     output_file = output,
                     encoding = "UTF-8",
                     envir = envir,
                     quiet = TRUE)
   
   invisible(output)
   
})