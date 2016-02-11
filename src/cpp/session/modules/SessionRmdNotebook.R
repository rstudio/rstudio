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

.rs.addJsonRpcHandler("extract_rmd_from_notebook", function(input, output)
{
   # if 'output' already exists, compare file write times to determine
   # whether we really want to overwrite a pre-existing .Rmd
   if (file.exists(output)) {
      inputInfo  <- file.info(input)
      outputInfo <- file.info(output)
      
      if (outputInfo$mtime > inputInfo$mtime)
         stop("'", output, "' exists and is newer than '", input, "'")
   }
   
   contents <- .rs.extractFromNotebook("rnb-document-source", input)
   cat(contents, file = output, sep = "\n")
   
   .rs.scalar(TRUE)
})

.rs.addFunction("extractFromNotebook", function(tag, rnbPath)
{
   if (!file.exists(rnbPath))
      stop("no file at path '", rnbPath, "'")
   
   contents <- readLines(rnbPath, warn = FALSE)
   reDocument <- paste('<!--', tag, '(\\S+) -->')
   idx <- grep(reDocument, contents, perl = TRUE)
   if (!length(idx))
      stop("no encoded content with tag '", tag, "' in '", rnbPath, "'")
   
   rmdEncoded <- sub(reDocument, "\\1", contents[idx])
   caTools::base64decode(rmdEncoded, character())
})

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
  errorMessage <- ""
  tryCatch({
    capture.output(rmarkdown::render(
      input = chunkFile, 
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
  }, error = function(e) {
    errorMessage <<- paste("Error:", e$message)
  })

  invisible(errorMessage)
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
   
   # generate base64 encoded version of original document
   docEncoded <- caTools::base64encode(paste(c(contents, ""), collapse = "\n"))
   
   # render a GitHub-flavored version of document, for injection
   ghOutput <- tempfile("rnb-gh-file-", fileext = ".md")
   ghFormat <- rmarkdown::github_document(html_preview = FALSE)
   rmarkdown::render(input = input,
                     output_format = ghFormat,
                     output_file = ghOutput,
                     output_options = list(self_contained = TRUE),
                     envir = envir,
                     quiet = TRUE)
   ghContents <- readLines(ghOutput, warn = FALSE)
   ghEncoded <- caTools::base64encode(paste(c(ghContents, ""), collapse = "\n"))
   
   
   # inject base64-encoded document into document
   # (i heard you like documents, so i put a document in your document)
   partitioned <- rmarkdown:::partition_yaml_front_matter(contents)
   injected <- c(
      partitioned$front_matter,
      "",
      sprintf('<!-- rnb-document-source %s -->', docEncoded),
      sprintf('<!-- rnb-github-md %s -->', ghEncoded),
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
                     output_options = list(self_contained = TRUE),
                     encoding = "UTF-8",
                     envir = envir,
                     quiet = TRUE)
   
   invisible(output)
   
})
