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
assign(".rs.notebookVersion", envir = .rs.toolsEnv(), "1.0")

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

   # populate the cache folder with the chunk output
   .Call("rs_populateNotebookCache", input)
   
   .rs.scalar(TRUE)
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
      input = normalizePath(chunkFile, winslash = "/"), 
      output_format = rmarkdown::html_document(
        theme = NULL,
        highlight = NULL,
        template = NULL, 
        self_contained = FALSE,
        includes = list(
          in_header = headerFile),
        lib_dir = libDir),
      output_file = normalizePath(outputFile, winslash = "/", mustWork = FALSE),
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

.rs.addFunction("readRnbCache", function(rmdPath, cachePath)
{
   if (Encoding(rmdPath) == "unknown")   Encoding(rmdPath) <- "UTF-8"
   if (Encoding(cachePath) == "unknown") Encoding(cachePath) <- "UTF-8"
   
   if (!file.exists(rmdPath))
      stop("No file at path '", rmdPath, "'")
   
   if (!file.exists(cachePath))
      stop("No cache directory at path '", cachePath, "'")
   
   rmdPath <- .rs.normalizePath(rmdPath, winslash = "/", mustWork = TRUE)
   cachePath <- normalizePath(cachePath, winslash = "/", mustWork = TRUE)
   rmdContents <- suppressWarnings(readLines(rmdPath))
   
   # Begin collecting the units that form the Rnb data structure
   rnbData <- list()
   
   # store reference to source path
   rnbData[["source_path"]] <- rmdPath
   rnbData[["cache_path"]]  <- cachePath
   
   # Keep the original source data
   rnbData[["contents"]] <- rmdContents
   
   # Read the chunk information
   chunkInfoPath <- file.path(cachePath, "chunks.json")
   chunkInfo <- .rs.fromJSON(.rs.readFile(chunkInfoPath))
   rnbData[["chunk_info"]] <- chunkInfo
   
   # Collect all of the HTML files, alongside their dependencies
   htmlFiles <- list.files(cachePath, pattern = "html$", full.names = TRUE)
   chunkData <- lapply(htmlFiles, function(file) {
      dependenciesDir <- paste(tools::file_path_sans_ext(file), "files", sep = "_")
      dependenciesFiles <- list.files(dependenciesDir, full.names = TRUE, recursive = TRUE)
      list(
         html = .rs.readFile(file),
         deps = lapply(dependenciesFiles, .rs.readFile)
      )
   })
   names(chunkData) <- tools::file_path_sans_ext(basename(htmlFiles))
   rnbData[["chunk_data"]] <- chunkData
   
   # Read in the 'libs' directory.
   rnbData[["lib"]] <- list()
   
   libDir <- file.path(cachePath, "lib")
   if (file.exists(libDir)) {
      owd <- setwd(libDir)
      libFiles <- list.files(libDir, recursive = TRUE)
      libData <- lapply(libFiles, .rs.readFile)
      names(libData) <- libFiles
      rnbData[["lib"]] <- libData
      setwd(owd)
   }
   
   rnbData
})

.rs.addFunction("maskChunks", function(contents, chunkInfo)
{
   # TODO: Mask the chunks in the document, so that we can render with pandoc
   # without actually rendering chunks -- after render, we inject the HTML
   # from the .Rnb cache back into the document.
   
   # masked <- contents
   # chunkDefns <- chunkInfo$chunk_definitions
   # for (i in seq_along(chunkDefns)) {
   #    
   #    start <- chunkDefns[[]]
   #    start <- chunkInfo[["row"]][[i]] - 1
   #    end   <- start + chunkInfo[["row_count"]][[i]] + 1
   #    id    <- chunkInfo[["chunk_id"]][[i]]
   #    
   #    masked <- c(
   #       masked[1:(start - 1)],
   #       paste('<!-- rnb-chunk-id', id, '-->'),
   #       masked[(end + 1):length(masked)]
   #    )
   # }
   # 
   # masked
})

.rs.addFunction("injectBase64Data", function(html, chunkId, rnbData)
{
   # we'll be calling pandoc in the cache dir, so make sure
   # we save our original dir
   owd <- getwd()
   on.exit(setwd(owd), add = TRUE)
   setwd(rnbData$cache_path)
   
   # use pandoc to render the HTML fragment, thereby injecting
   # base64-encoded dependencies
   input  <- "rnb-base64-inject-input.html"
   output <- "rnb-base64-inject-output.html"
   
   cat(html, file = input, sep = "\n")
   opts <- c("--self-contained")
   rmarkdown:::pandoc_convert(input, output = output, options = opts)
   
   result <- .rs.readFile(output)
   .rs.extractHTMLBodyElement(result)
})

.rs.addFunction("extractHTMLBodyElement", function(html)
{
   begin <- regexpr('<body[^>]*>', html, perl = TRUE)
   end   <- regexpr('</body>', html, perl = TRUE)
   
   contents <- substring(html, begin + attr(begin, "match.length"), end - 1)
   .rs.trimWhitespace(contents)
})

.rs.addFunction("rnbFillChunk", function(rnbData, chunkId)
{
   builder <- .rs.stringBuilder()
   chunkInfo <- rnbData$chunk_info[rnbData$chunk_info$chunk_id == chunkId, ]
   chunkData <- rnbData$chunk_data[[chunkId]]
   
   # parse the chunk header as we'll need to interleave
   # that information within the output
   pattern <- paste(
      "^\\s*```+{",
      "(\\w+)",
      "\\s*",
      "([^}]*)",
      "}\\s*$",
      sep = ""
   )
   
   chunkHeader <- rnbData$contents[chunkInfo$row - 1]
   matches <- .rs.regexMatches(pattern, chunkHeader)
   
   chunkClass   <- matches[[1]]
   chunkOptions <- matches[[2]]
   
   startIdx <- chunkInfo$row
   endIdx   <- chunkInfo$row + chunkInfo$row_count - 1
   visible  <- chunkInfo$visible
   
   builder$appendf('<div class="%s_chunk" data-chunk-id="%s" data-chunk-options="%s">',
                   chunkClass,
                   chunkId,
                   chunkOptions)
   builder$indent()
   
   # Input code
   builder$appendf('<code class="%s_input">', chunkClass)
   builder$indent()
   builder$append(rnbData$contents[startIdx:endIdx])
   builder$unindent()
   builder$append('</code>')
   
   # Output code
   bodyContent <- .rs.extractHTMLBodyElement(chunkData$html)
   bodyContent <- .rs.injectBase64Data(bodyContent, chunkId, rnbData)
   builder$appendf('<div class="%s_output" visible="%s">', chunkClass, visible)
   builder$indent()
   builder$append(bodyContent)
   builder$unindent()
   builder$append('</div>')
   
   builder$unindent()
   builder$append('</div>')
   
   builder$data()
})

.rs.addFunction("rnbFillChunks", function(html, rnbData)
{
   filled <- html
   for (i in seq_along(filled)) {
      line <- filled[[i]]
      if (.rs.startsWith(line, '<!-- rnb-chunk-id') && .rs.endsWith(line, '-->')) {
         chunkId <- sub('<!-- rnb-chunk-id\\s*(\\S+)\\s*-->', '\\1', line)
         chunkData <- rnbData$chunk_data[[chunkId]]
         if (is.null(chunkData))
            stop("no chunk with id '", chunkId, "'")
         
         filled[[i]] <- paste(.rs.rnbFillChunk(rnbData, chunkId), collapse = "\n")
      }
   }
   filled
})

.rs.addFunction("createNotebookFromCache", function(rnbData)
{
   # first, render our .Rmd to transform markdown to html
   contents <- rnbData$contents
   chunkInfo <- rnbData$chunk_info
   
   # mask out chunks (replace with placeholders w/id)
   masked <- .rs.maskChunks(contents, chunkInfo)
   
   # use pandoc to convert md to html
   input  <- tempfile("rnb-tempfile-input", fileext = ".md")
   output <- tempfile("rnb-tempfile-output", fileext = ".html")
   cat(masked, file = input, sep = "\n")
   rmarkdown:::pandoc_convert(input = input, output = output)
   
   # read the HTML
   html <- readLines(output)
   
   # replace chunk placeholders with their actual data
   html <- .rs.rnbFillChunks(html, rnbData)
   
   # extract yaml header
   frontMatter <- rmarkdown:::partition_yaml_front_matter(contents)$front_matter
   yaml <- rmarkdown:::parse_yaml_front_matter(frontMatter)
   
   # begin building our output file
   builder <- .rs.stringBuilder()
   
   builder$append('<html>')
   
   # write header output
   builder$append('<head>')
   builder$indent()
   builder$append(sprintf('<meta name="r-notebook-version" content="%s" />', .rs.notebookVersion))
   builder$append(sprintf('<title>%s</title>', yaml$title))
   builder$append('<script type="text/yaml">')
   builder$indent()
   builder$append(frontMatter[2:(length(frontMatter) - 1)])
   builder$unindent()
   builder$append('</script>')
   builder$unindent()
   builder$append('</head>')
   
   # write body output
   builder$append('<body>')
   builder$append(html)
   builder$append('</body>')
   
   # close html
   builder$append('</html>')
   
   unlist(builder$data())
   
})
