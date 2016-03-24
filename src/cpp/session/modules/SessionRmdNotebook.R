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
  if (Encoding(input) == "unknown")  Encoding(input) <- "UTF-8"
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

.rs.addFunction("reRmdChunkBegin", function()
{
   "^[\t >]*```+\\s*\\{[.]?([a-zA-Z]+.*)\\}\\s*$"
})

.rs.addFunction("reRmdChunkEnd", function()
{
   "^[\t >]*```+\\s*$"
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
      chunkStarts <- grep(.rs.reRmdChunkBegin(), contents, perl = TRUE)
      chunkEnds <- grep(.rs.reRmdChunkEnd(), contents, perl = TRUE)
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
   writeLines(rmdModified, renderInput)
   on.exit(unlink(renderInput), add = TRUE)
   
   # perform render
   .rs.rnb.render(inputFile = renderInput,
                  outputFile = outputFile,
                  rmdContents = rmdContents)
   
   invisible(outputFile)
   
})

.rs.addFunction("rnb.withChunkLocations", function(rmdContents, chunkInfo)
{
   chunkLocs <- grep(.rs.reRmdChunkBegin(), rmdContents, perl = TRUE)
   for (i in seq_along(chunkInfo$chunk_definitions)) {
      info <- chunkInfo$chunk_definitions[[i]]
      
      info$chunk_start <- tail(chunkLocs[chunkLocs < info$row + 1], 1)
      info$chunk_end   <- info$row + 1
      
      chunkInfo$chunk_definitions[[i]] <- info
      
   }
   names(chunkInfo$chunk_definitions) <-
      unlist(lapply(chunkInfo$chunk_definitions, "[[", "chunk_id"))
   chunkInfo
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
   cachePath <- .rs.normalizePath(cachePath, winslash = "/", mustWork = TRUE)
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
   
   # Augment with start, end locations of chunks
   chunkInfo <- .rs.rnb.withChunkLocations(rmdContents, chunkInfo)
   rnbData[["chunk_info"]] <- chunkInfo
   
   # Read the chunk data
   chunkDirs <- file.path(cachePath, names(chunkInfo$chunk_definitions))
   chunkData <- lapply(chunkDirs, function(dir) {
      files <- list.files(dir, full.names = TRUE)
      contents <- lapply(files, function(file) {
         .rs.readFile(file, binary = .rs.endsWith(file, "png"))
      })
      names(contents) <- basename(files)
      contents
   })
   names(chunkData) <- basename(chunkDirs)
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

.rs.addFunction("extractHTMLBodyElement", function(html)
{
   begin <- regexpr('<body[^>]*>', html, perl = TRUE)
   end   <- regexpr('</body>', html, perl = TRUE)
   
   contents <- substring(html, begin + attr(begin, "match.length"), end - 1)
   .rs.trimWhitespace(contents)
})

.rs.addFunction("rnb.injectBase64Data", function(html, chunkId, rnbData)
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

.rs.addFunction("rnb.maskChunks", function(contents, chunkInfo)
{
   masked <- contents
   
   # Extract chunk locations based on the document + chunk info
   chunkRanges <- lapply(chunkInfo$chunk_definitions, function(info) {
      list(start = info$chunk_start,
           end   = info$chunk_end,
           id    = info$chunk_id)
   })
   
   for (range in rev(chunkRanges)) {
      
      beforeText <- if (range$start > 1)
         masked[1:(range$start - 1)]
      
      afterText  <- if (range$end < length(masked))
         masked[(range$end + 1):length(masked)]
      
      masked <- c(
         beforeText,
         paste("<!-- rnb-chunk-id", range$id, "-->"),
         afterText
      )
   }
   
   # mask any remaining chunks (these are chunks which
   # have no associated output in the cache; ie, they
   # were not executed)
   #
   # TODO: respect chunk options here (e.g. 'include = TRUE')
   chunkStarts <- grep(.rs.reRmdChunkBegin(), masked, perl = TRUE)
   chunkEnds   <- grep(.rs.reRmdChunkEnd(), masked, perl = TRUE)
   ranges <- mapply(function(x, y) list(start = x, end = y),
                    chunkStarts, chunkEnds, SIMPLIFY = FALSE)
   
   for (range in rev(ranges)) {
      masked <- c(
         masked[1:(range$start - 1)],
         masked[(range$end + 1):length(masked)]
      )
   }
   
   masked
   
})

.rs.addFunction("rnb.fillChunks", function(html, rnbData)
{
   indices <- which(
      .rs.startsWith(html, "<!-- rnb-chunk-id") &
      .rs.endsWith(html, "-->"))
   
   # Record htmlwidget dependencies as we fill chunks
   jsDependencies <- list()
   
   for (chunkIdx in seq_along(indices))
   {
      i <- indices[[chunkIdx]]
      line <- html[i]
      
      chunkId <- sub('<!-- rnb-chunk-id\\s*(\\S+)\\s*-->', '\\1', line)
      chunkData <- rnbData$chunk_data[[chunkId]]
      chunkDefn <- rnbData$chunk_info$chunk_definitions[[chunkId]]
      if (is.null(chunkData) || is.null(chunkDefn))
         stop("no chunk with id '", chunkId, "'")
      
      # convert to HTML
      htmlList <- .rs.enumerate(chunkData, function(name, value) {
         if (.rs.endsWith(name, "csv")) {
            parsed <- .rs.rnb.parseConsoleData(value)
            .rs.rnb.consoleDataToHtml(parsed, chunkId, name)
         } else if (.rs.endsWith(name, "png")) {
            encoded <- caTools::base64encode(value)
            fmt <- "<img data-chunk-id=\"%s\" data-chunk-filename=\"%s\" src=data:image/png;base64,%s />"
            sprintf(fmt, chunkId, name, encoded)
         } else if (.rs.endsWith(name, "html")) {
            # parse and record JSON dependencies
            jsonPath <- .rs.withChangedExtension(name, "json")
            jsonString <- chunkData[[jsonPath]]
            jsDependencies <<- c(jsDependencies, .rs.fromJSON(jsonString))
            
            # emit body of HTML content
            .rs.extractHTMLBodyElement(value)
         }
      })
      
      # insert into document
      injection <- c(
         sprintf("<!-- rnb-chunk-start-%s %s -->", chunkIdx, chunkDefn$chunk_start),
         paste(unlist(htmlList), collapse = "\n"),
         sprintf("<!-- rnb-chunk-end-%s %s -->", chunkIdx, chunkDefn$chunk_end)
      )
      
      html[[i]] <- paste(injection, sep = "\n", collapse = "\n")
      chunkIdx <- chunkIdx + 1
   }
   
   # Inject JSON dependency information into document
   # TODO: Resolve duplicates
   htmlDeps <- lapply(jsDependencies, function(dep) {
      injection <- character()
      
      jsPath <- file.path(dep$src$file, dep$script)
      if (file.exists(jsPath))
      {
         contents <- .rs.readFile(jsPath, binary = TRUE)
         encoded <- caTools::base64encode(contents)
         scriptHtml <- sprintf("<script src=\"data:application/x-javascript;base64,%s\"></script>", encoded)
         injection <- c(injection, scriptHtml)
      }
      
      paste(injection, collapse = "\n")
   })
   
   bodyIdx <- tail(grep("^\\s*</body>\\s*$", html, perl = TRUE), n = 1)
   html[bodyIdx] <- paste(
      paste(htmlDeps, collapse = "\n"),
      "</body>",
      sep = "\n"
   )
   
   html
})

.rs.addFunction("rnb.render", function(inputFile,
                                       outputFile,
                                       rmdContents = .rs.readFile(inputFile),
                                       envir = .GlobalEnv)
{
   # TODO: Move to 'rmarkdown' package when appropriate
   html_notebook <- function(toc = TRUE,
                             toc_float = TRUE,
                             code_folding = "show",
                             theme = "cerulean",
                             highlight = "textmate",
                             ...)
   {
      rmarkdown::html_document(toc = toc,
                               toc_float = toc_float,
                               code_folding = code_folding,
                               theme = theme,
                               highlight = highlight,
                               ...)
   }
   
   outputFormat <- html_notebook()
   renderOutput <- tempfile("rnb-render-output-", fileext = ".html")
   outputOptions <- list(self_contained = TRUE, keep_md = TRUE)
   
   rmarkdown::render(input = inputFile,
                     output_format = outputFormat,
                     output_file = renderOutput,
                     output_options = outputOptions,
                     encoding = "UTF-8",
                     envir = envir,
                     quiet = TRUE)
   
   
   # read the rendered file
   rnbContents <- readLines(renderOutput, warn = FALSE)
   
   # generate base64-encoded versions of .Rmd source, .md sidecar
   rmdEncoded <- caTools::base64encode(paste(rmdContents, collapse = "\n"))
   
   # inject document contents into rendered file
   # (i heard you like documents, so i put a document in your document)
   rnbContents <- .rs.injectHTMLComments(
      rnbContents,
      "<head>",
      list("rnb-document-source" = rmdEncoded)
   )
   
   # write our .Rnb to file and we're done!
   cat(rnbContents, file = outputFile, sep = "\n")
   invisible(outputFile)
   
})

.rs.addFunction("createNotebookFromCacheData", function(rnbData,
                                                        outputFile,
                                                        envir = .GlobalEnv)
{
   # first, render our .Rmd to transform markdown to html
   contents <- rnbData$contents
   chunkInfo <- rnbData$chunk_info
   
   # mask out chunks (replace with placeholders w/id)
   masked <- .rs.rnb.maskChunks(contents, chunkInfo)
   
   # use pandoc to convert md to html
   inputTemp  <- tempfile("rnb-tempfile-input-", fileext = ".md")
   outputTemp <- tempfile("rnb-tempfile-output-", fileext = ".html")
   cat(masked, file = inputTemp, sep = "\n")
   
   # render our notebook
   .rs.rnb.render(inputFile = inputTemp,
                  outputFile = outputTemp,
                  rmdContents = contents,
                  envir = envir)
   
   # read the HTML
   html <- readLines(outputTemp)
   
   # replace chunk placeholders with their actual data
   html <- .rs.rnb.fillChunks(html, rnbData)
   
   # write to file
   cat(html, file = outputFile, sep = "\n")
   outputFile
})

.rs.addFunction("createNotebookFromCache", function(rmdPath, outputPath = NULL)
{
   if (is.null(outputPath))
      outputPath <- .rs.withChangedExtension(rmdPath, "Rnb")
   
   cachePath <- .rs.rnb.cachePathFromNotebookPath(rmdPath)
   if (!file.exists(cachePath))
      stop("no cache data associated with '", rmdPath, "'")
   
   rnbData <- .rs.readRnbCache(rmdPath, cachePath)
   .rs.createNotebookFromCacheData(rnbData, outputPath)
})

.rs.addFunction("rnb.cachePathFromNotebookPath", function(rmdPath)
{
  .Call(.rs.routines$rs_chunkCacheFolder, rmdPath)
})

.rs.addFunction("rnb.parseConsoleData", function(data)
{
   csvData <- read.csv(
      text = data,
      encoding = "UTF-8",
      header = FALSE,
      stringsAsFactors = FALSE
   )
   
   names(csvData) <- c("type", "text")
   csvData
})

.rs.addFunction("rnb.consoleDataToHtml", function(csvData, chunkId, fileName)
{
   cutpoints <- .rs.cutpoints(csvData$type)
   
   ranges <- Map(
      function(start, end) list(start = start, end = end),
      c(1, cutpoints + 1),
      c(cutpoints, nrow(csvData))
   )
   
   splat <- lapply(ranges, function(range) {
      
      type <- csvData$type[[range$start]]
      collapse <- if (type == 0) "\n" else ""
      
      pasted <- paste(csvData$text[range$start:range$end], collapse = collapse)
      result <- .rs.trimWhitespace(pasted)
      if (!nzchar(result))
         return(NULL)
      
      if (type == 1 || type == 2)
         result <- paste("##", gsub("\n", "\n## ", result, fixed = TRUE))
      
      attr(result, "type") <- type
      attr(result, ".class") <- if (type == 0) "r"
      
      result
   })
   
   filtered <- Filter(Negate(is.null), splat)
   html <- lapply(filtered, function(el) {
      type  <- attr(el, "type")
      class <- attr(el, ".class")
      result <- if (is.null(class)) {
         fmt <- "<pre data-chunk-id=\"%s\" data-chunk-filename=\"%s\" data-chunk-type=\"%s\"><code>%s</code></pre>"
         sprintf(fmt, chunkId, fileName, type, el)
      } else {
         fmt <- "<pre data-chunk-id=\"%s\" data-chunk-filename=\"%s\" data-chunk-type=\"%s\" class=\"%s\"><code>%s</code></pre>"
         sprintf(fmt, chunkId, fileName, type, class, el)
      }
      result
   })
   
   paste(unlist(html), collapse = "\n")
   
})
