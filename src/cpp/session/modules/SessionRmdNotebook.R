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
  if (Encoding(input) == "unknown") Encoding(input) <- "UTF-8"
  if (Encoding(output) == "unknown") Encoding(output) <- "UTF-8"

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

.rs.addFunction("withChangedExtension", function(path, ext)
{
   paste(tools::file_path_sans_ext(path), ext, sep = ".")
})

.rs.addFunction("extractFromNotebook", function(tag, rnbPath)
{
   if (!file.exists(rnbPath))
      stop("no file at path '", rnbPath, "'")
   
   contents <- readLines(rnbPath, warn = FALSE)
   
   # find the line hosting the encoded content
   marker <- paste('<!--', tag)
   idx <- NULL
   for (i in seq_along(contents))
   {
      if (.rs.startsWith(contents[[i]], marker))
      {
         idx <- i
         break
      }
   }
   
   if (!length(idx))
      stop("no encoded content with tag '", tag, "' in '", rnbPath, "'")
      
   reDocument <- paste('<!--', tag, '(\\S+) -->')
   rmdEncoded <- sub(reDocument, "\\1", contents[idx])
   caTools::base64decode(rmdEncoded, character())
})

.rs.addFunction("executeSingleChunk", function(options,
                                               content,
                                               libDir,
                                               headerFile,
                                               outputFile) 
{
  # presume paths are UTF-8 encoded unless specified otherwise
  if (Encoding(libDir) == "unknown") Encoding(libDir) <- "UTF-8"
  if (Encoding(headerFile) == "unknown") Encoding(headerFile) <- "UTF-8"
  if (Encoding(outputFile) == "unknown") Encoding(outputFile) <- "UTF-8"

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
                 
  # begin capturing the error stream (and clean up when we're done)
  errorFile <- tempfile()
  on.exit(unlink(errorFile), add = TRUE)
  errorCon <- file(errorFile, open = "wt")
  sink(errorCon, type = "message")
  on.exit(sink(type = "message"), add = TRUE)
  on.exit(close(errorCon), add = TRUE)

  # render the stub to the given file
  errorMessage <- ""
  errorText <- ""
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
    # capture any error message returned
    errorMessage <<- paste("Error:", e$message)

    # flush the error stream and send it as well
    errorText <<- paste(readLines(errorFile), collapse = "\n")
  })

  list(message = errorMessage, 
       text    = errorText)
})

.rs.addFunction("injectHTMLComments", function(contents,
                                               location,
                                               inject)
{
   # find the injection location
   idx <- NULL
   for (i in seq_along(contents))
   {
      if (contents[[i]] == location)
      {
         idx <- i
         break
      }
   }
   
   if (is.null(idx))
      stop("failed to find injection location '", location, "'")
   
   # generate injection strings
   injection <- paste(vapply(seq_along(inject), FUN.VALUE = character(1), function(i) {
      sprintf('<!-- %s %s -->', names(inject)[i], inject[[i]])
   }), collapse = "\n")
   
   contents[[idx]] <- paste(contents[[idx]], injection, "", sep = "\n")
   contents
})

.rs.addFunction("createNotebook", function(inputFile,
                                           outputFile = NULL,
                                           envir = .GlobalEnv)
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
   
   # resolve input, output paths
   inputFile <- normalizePath(inputFile, winslash = "/", mustWork = TRUE)
   if (is.null(outputFile))
      outputFile <- .rs.withChangedExtension(inputFile, "Rnb")
   
   rmdContents <- readLines(inputFile, warn = FALSE)
   
   # inject placeholders so we can track chunk locations after render
   # ensure that the comments are surrounded by newlines, as otherwise
   # strange render errors can occur
   rmdModified <- rmdContents
   rmdChunks <- find_chunks(rmdModified)
   for (i in seq_along(rmdChunks)) {
      startIdx <- rmdChunks[[i]]$start
      rmdModified[startIdx] <- paste(
         chunk_annotation("start", i, startIdx),
         rmdModified[startIdx],
         sep = ""
      )
      
      endIdx <- rmdChunks[[i]]$end
      rmdModified[endIdx] <- paste(
         rmdModified[endIdx],
         chunk_annotation("end", i, endIdx),
         sep = ""
      )
   }
   
   # write out file and prepare for render
   renderInput  <- tempfile("rnb-render-input-", fileext = ".Rmd")
   renderOutput <- tempfile("rnb-render-output", fileext = ".Rnb")
   writeLines(rmdModified, renderInput)
   on.exit(unlink(renderInput), add = TRUE)
   on.exit(unlink(renderOutput), add = TRUE)
   
   # render the document
   outputFormat <- rmarkdown::html_document(self_contained = TRUE,
                                            keep_md = TRUE)
   
   if (is.null(renderOutput))
      renderOutput <- paste(tools::file_path_sans_ext(renderInput), "rnb", sep = ".")
   
   if (!file.exists(dirname(renderOutput)))
      dir.create(dirname(renderOutput), recursive = TRUE)
   
   rmarkdown::render(input = renderInput,
                     output_format = outputFormat,
                     output_file = renderOutput,
                     output_options = list(self_contained = TRUE),
                     encoding = "UTF-8",
                     envir = envir,
                     quiet = TRUE)
   
   # read the rendered file
   rnbContents <- readLines(renderOutput, warn = FALSE)
   
   # generate base64-encoded versions of .Rmd source, .md sidecar
   rmdContents <- readChar(inputFile, file.info(inputFile)$size, TRUE)
   rmdEncoded <- caTools::base64encode(paste(rmdContents, collapse = "\n"))
   
   mdPath <- .rs.withChangedExtension(renderOutput, "md")
   mdContents <- readChar(mdPath, file.info(mdPath)$size, TRUE)
   mdEncoded  <- caTools::base64encode(mdContents)
   
   # inject document contents into rendered file
   # (i heard you like documents, so i put a document in your document)
   rnbContents <- .rs.injectHTMLComments(
      rnbContents,
      "<head>",
      list("rnb-document-source" = rmdEncoded,
           "rnb-github-md"       = mdEncoded)
   )
   
   # write our .Rnb to file and we're done!
   cat(rnbContents, file = outputFile, sep = "\n")
   invisible(outputFile)
   
})
