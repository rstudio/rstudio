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

.rs.addFunction("extractRmdFromNotebook", function(notebook) {
   # parse the notebook to get the text of the contained R markdown doc
   parsed <- try(rmarkdown::parse_html_notebook(notebook), silent = TRUE)
   if (inherits(parsed, "try-error") || is.null(parsed$rmd)) {
    return("")
   }

   return(paste(parsed$rmd, collapse = "\n"))
})

.rs.addFunction("reRmdChunkBegin", function()
{
   "^[\t >]*```+\\s*\\{[.]?([a-zA-Z]+.*)\\}\\s*$"
})

.rs.addFunction("reRmdChunkEnd", function()
{
   "^[\t >]*```+\\s*$"
})

.rs.addFunction("rnb.cachePathFromRmdPath", function(rmdPath)
{
   .Call("rs_chunkCacheFolder", rmdPath)
})

.rs.addFunction("coalesceCsvOutput", function(chunkData) {
  # nothing to coalesce if < 2 outputs
  if (length(chunkData) < 2)
    return(chunkData)
    
  # keep all output by default
  keep <- rep(TRUE, length(chunkData))

  # coalesce contents for consecutive csv entries
  csvs <- .rs.endsWith(names(chunkData), ".csv")
  for (i in seq.int(1, length(csvs) - 1)) {
    if (keep[[i]] && csvs[[i]] && csvs[[i + 1]]) {
      chunkData[[i]] <- paste(chunkData[[i]], chunkData[[i + 1]], sep = "")
      keep[[i + 1]] <- FALSE
    }
  }
  chunkData[keep]
})

.rs.addFunction("readRnbCache", function(rmdPath, cachePath)
{
   if (!file.exists(rmdPath))
      stop("No file at path '", rmdPath, "'")
   
   # tolerate missing cache (implies there's no chunk outputs)
   rmdPath <- path.expand(rmdPath)
   rmdContents <- .rs.readLines(rmdPath)
   
   # Begin collecting the units that form the Rnb data structure
   rnbData <- list()
   
   # store reference to source path
   rnbData[["source_path"]] <- rmdPath
   rnbData[["cache_path"]]  <- cachePath
   
   # Keep the original source data
   rnbData[["contents"]] <- rmdContents
   
   # Set up rnbData structure (if we have a cache, these entries will be filled)
   rnbData[["chunk_info"]] <- list()
   rnbData[["chunk_data"]] <- list()
   rnbData[["lib"]] <- list()
   
   # early return if we have no cache
   if (!file.exists(cachePath))
      return(rnbData)
   
   # Read the chunk information
   chunkInfoPath <- file.path(cachePath, "chunks.json")
   chunkInfo <- .rs.fromJSON(.rs.readFile(chunkInfoPath))
   names(chunkInfo$chunk_definitions) <-
      unlist(lapply(chunkInfo$chunk_definitions, `[[`, "chunk_id"))
   rnbData[["chunk_info"]] <- chunkInfo
   
   # Read the chunk data
   chunkDirs <- file.path(cachePath, names(chunkInfo$chunk_definitions))
   chunkData <- lapply(chunkDirs, function(dir) {
      files <- list.files(dir, full.names = TRUE)
      contents <- lapply(files, function(file) {
         .rs.readFile(file, binary = .rs.endsWith(file, "png") || 
                                     .rs.endsWith(file, "jpg") ||
                                     .rs.endsWith(file, "jpeg") ||
                                     .rs.endsWith(file, "rdf"))
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

.rs.addFunction("rnb.resolveActiveChunkId", function(rnbData, label)
{
   chunkDefns <- rnbData$chunk_info$chunk_definitions
   for (defn in chunkDefns) {
      if (identical(defn$chunk_label, label))
         return(defn$chunk_id)
   }
   return(NULL)
})

.rs.addFunction("rnb.engineToCodeClass", function(engine)
{
   engine <- tolower(engine)
   switch(engine,
      rcpp = "cpp",
      sh = "bash",
      engine)
})

.rs.addFunction("rnb.outputSourcePng", function(fileName,
                                                fileContents,
                                                metadata,
                                                ...)
{
   rmarkdown:::html_notebook_output_png(bytes = fileContents,
                                        meta = metadata)
})

.rs.addFunction("rnb.outputSourceJpeg", function(fileName,
                                                 fileContents,
                                                 metadata,
                                                 ...)
{
   rmarkdown:::html_notebook_output_img(bytes = fileContents,
                                        meta = metadata,
                                        format = "jpeg")
})

.rs.addFunction("rnb.outputSourceConsole", function(fileName,
                                                    fileContents,
                                                    context,
                                                    includeSource,
                                                    ...)
{
   parsed <- .rs.rnb.readConsoleData(fileContents)
   if (!includeSource)
      parsed <- parsed[parsed$type != 0, ]
   if (identical(context$results, "hide"))
      parsed <- parsed[parsed$type != 1, ]
   attributes <- list(class = .rs.rnb.engineToCodeClass(context$engine))
   rendered <- .rs.rnb.renderConsoleData(parsed,
                                         attributes = attributes,
                                         context)
   return(rendered)
})

.rs.addFunction("rnb.outputSourceHtml", function(fileName,
                                                 fileContents,
                                                 rnbData,
                                                 chunkId,
                                                 metadata,
                                                 ...)
{
   # if we have a '.json' sidecar file, this is an HTML widget
   jsonName <- .rs.withChangedExtension(fileName, ".json")
   jsonPath <- file.path(rnbData$cache_path, chunkId, jsonName)
   if (file.exists(jsonPath)) {
      
      # read and restore 'html_dependency' class (this may not be
      # preserved in the serialized JSON file)
      jsonContents <- .rs.fromJSON(.rs.readFile(jsonPath))
      for (i in seq_along(jsonContents))
         class(jsonContents[[i]]) <- "html_dependency"
      
      # extract body element (this is effectively what the
      # widget emitted on print in addition to aforementioned
      # dependency information)
      bodyEl <- .rs.extractHTMLBodyElement(fileContents)
      
      # annotate widget manually and return asis output
      annotated <- rmarkdown:::html_notebook_annotated_output(
         bodyEl,
         "htmlwidget",
         list(dependencies = jsonContents,
              metadata     = metadata)
      )
      preserved <- htmltools::htmlPreserve(annotated)
      widget <- knitr::asis_output(preserved)
      attr(widget, "knit_meta") <- jsonContents
      return(widget)
   }
   
   # no .json file; just return HTML as-is
   annotated <- rmarkdown:::html_notebook_annotated_output(
      fileContents,
      "html",
      list(metadata = metadata)
   )
   preserved <- htmltools::htmlPreserve(annotated)
   knitr::asis_output(preserved)
})

.rs.addFunction("rnb.pagedTableHtml", function(rdfPath)
{
   data <- .rs.readDataCapture(rdfPath)
   
   paste(
      "<div data-pagedtable>",
      "  <script data-pagedtable-source type=\"application/json\">",
      jsonlite::toJSON(data),
      "  </script>",
      "</div>",
      sep = "\n"
   )
})

.rs.addFunction("rnb.outputSourceRdf", function(fileName,
                                                fileContents,
                                                metadata,
                                                rnbData,
                                                chunkId,
                                                ...)
{
   rdfName <- .rs.withChangedExtension(fileName, ".rdf")
   rdfPath <- file.path(rnbData$cache_path, chunkId, rdfName)
   
   annotated <- rmarkdown:::html_notebook_annotated_output(
      .rs.rnb.pagedTableHtml(rdfPath),
      "frame",
      list(metadata = metadata, rdf = .rs.base64encode(fileContents))
   )
   
   knitr::asis_output(annotated)
})

.rs.addFunction("rnb.outputSource", function(rnbData) {
   force(rnbData)
   function(code, context, ...) {
      
      # resolve chunk id (attempt to match labels)
      chunkId <- .rs.rnb.resolveActiveChunkId(rnbData, context$label)
      
      # determine whether we include source code (respect chunk options)
      includeSource <- isTRUE(context$echo) && isTRUE(context$include)
      
      # if we have no chunk outputs, just show source code (respecting
      # chunk options as appropriate)
      if (is.null(chunkId)) {
         if (includeSource) {
            attributes <- list(class = .rs.rnb.engineToCodeClass(context$engine))
            if (!is.null(context$indent)) {
               return(.rs.rnb.renderVerbatimConsoleInput(code, tolower(context$engine), ""))
            } else {
               return(rmarkdown::html_notebook_output_code(code, attributes = attributes))
            }
         }
         return(knitr::asis_output(""))
      }
      
      chunkData <- .rs.coalesceCsvOutput(rnbData$chunk_data[[chunkId]])
      
      # map file extensions to handlers
      outputHandlers <- list(
         "png"  = .rs.rnb.outputSourcePng,
         "jpg"  = .rs.rnb.outputSourceJpeg,
         "jpeg" = .rs.rnb.outputSourceJpeg,
         "csv"  = .rs.rnb.outputSourceConsole,
         "html" = .rs.rnb.outputSourceHtml,
         "rdf"  = .rs.rnb.outputSourceRdf
      )

      outputList <- .rs.enumerate(chunkData, function(fileName, fileContents) {
         
         # read metadata sidecar if present
         metadata <- NULL
         metadataName <- .rs.withChangedExtension(fileName, ".metadata")
         metadataPath <- file.path(rnbData$cache_path, chunkId, metadataName)
         if (file.exists(metadataPath))
            metadata <- .rs.fromJSON(.rs.readFile(metadataPath))
         
         # find and execute handler for extension (return NULL if no handler defined)
         ext <- tools::file_ext(fileName)
         handler <- outputHandlers[[ext]]
         if (!is.function(handler))
            return(NULL)
         
         handler(code = code,
                 context = context,
                 fileName = fileName,
                 fileContents = fileContents,
                 metadata = metadata,
                 rnbData = rnbData,
                 chunkId = chunkId,
                 includeSource = includeSource)
      })
      
      # remove nulls and return
      Filter(Negate(is.null), outputList)
      
   }
})
   
.rs.addFunction("createNotebookFromCacheData", function(rnbData,
                                                        inputFile,
                                                        outputFile = NULL,
                                                        envir = .GlobalEnv)
{
   if (is.null(outputFile))
      outputFile <- .rs.withChangedExtension(inputFile, ext = ".nb.html")

   # TODO: pass encoding from frontend
   encoding <- "UTF-8"
   
   # reset the knitr chunk counter (it can be modified as a side effect of
   # parse_params, which is called during notebook execution)
   knitr:::chunk_counter(reset = TRUE)

   # implement output_source
   outputOptions <- list(output_source = .rs.rnb.outputSource(rnbData))
   
   # call render with special format hooks
   rmarkdown::render(input = inputFile,
                     output_format = "html_notebook",
                     output_options = outputOptions,
                     output_file = outputFile,
                     quiet = TRUE,
                     envir = envir,
                     encoding = encoding)
})

.rs.addFunction("createNotebookFromCache", function(rmdPath, outputPath = NULL)
{
   # presume success unless we fail below
   result <- list(succeeded = .rs.scalar(TRUE))

   tryCatch({
      cachePath <- .rs.rnb.cachePathFromRmdPath(rmdPath)
      rnbData <- .rs.readRnbCache(rmdPath, cachePath)
      .rs.createNotebookFromCacheData(rnbData, rmdPath, outputPath)
   }, 
   
   error = function(e) {
      # convert exception to error message for client
      result <<- list(
         succeeded = .rs.scalar(FALSE),
         error_message = .rs.scalar(e$message)
      )
   })
   
   result
})

.rs.addFunction("rnb.readConsoleData", function(encodedData)
{
   # read from CSV
   csvData <- read.csv(
      text = encodedData,
      encoding = "UTF-8",
      header = FALSE,
      stringsAsFactors = FALSE
   )
   
   names(csvData) <- c("type", "text")
   
   csvData
})

.rs.addFunction("rnb.renderVerbatimConsoleInput", function(code, engine, indent)
{
   # remove indentation from code
   code <- substring(code, nchar(indent) + 1)
   
   # print as block code (as knitr might normally do)
   fmt <- "```%s\n%s\n```"
   out <- sprintf(fmt, tolower(engine), paste(code, collapse = "\n"))
   knitr::asis_output(out)
})

.rs.addFunction("rnb.renderConsoleData", function(csvData,
                                                  attributes = list(class = "r"),
                                                  context = list())
{
   # bail early for empty data
   if (length(csvData) == 0 || nrow(csvData) == 0)
      return(list())
   
   # split on type
   cutpoints <- .rs.cutpoints(csvData$type)
   
   ranges <- Map(
      function(start, end) list(start = start, end = end),
      c(1, cutpoints),
      c(cutpoints - 1, nrow(csvData))
   )
   
   splat <- lapply(ranges, function(range) {
      type <- csvData$type[[range$start]]
      text <- csvData$text[range$start:range$end]
      collapse <- if (type == 0) "\n" else ""
      pasted <- paste(text, collapse = collapse)
      if (type == 0) {
         if (is.null(context$indent)) {
            return(rmarkdown::html_notebook_output_code(pasted, attributes = attributes))
         } else {
            return(.rs.rnb.renderVerbatimConsoleInput(pasted, tolower(context$engine), context$indent))
         }
      } else {
         return(pasted)
      }
   })
   
   splat
})

.rs.addFunction("scrapeHtmlAttributes", function(line)
{
   reData <- '([[:alnum:]_-]+)[[:space:]]*=[[:space:]]*"(\\\\.|[^"])+"'
   reMatches <- gregexpr(reData, line)[[1]]
   starts <- c(reMatches)
   ends   <- starts + attr(reMatches, "match.length") - 1
   stripped <- substring(line, starts, ends)
   equalsIndex <- regexpr("=", stripped, fixed = TRUE)
   lhs <- substring(stripped, 1, equalsIndex - 1)
   rhs <- substring(stripped, equalsIndex + 2, nchar(stripped) - 1)
   names(rhs) <- lhs
   as.list(rhs)
})

.rs.addFunction("rnb.encode", function(data)
{
   .rs.base64encode(.rs.toJSON(data, unbox = TRUE))
})

.rs.addFunction("rnb.decode", function(encoded)
{
   .rs.fromJSON(.rs.base64decode(encoded))
})

.rs.addFunction("evaluateChunkOptions", function(code)
{
  opts <- list()

  # if several lines of code are passed, operate only on the first 
  code <- unlist(strsplit(code, "\n", fixed = TRUE))[[1]]

  # strip chunk indicators if present
  matches <- unlist(regmatches(code, regexec(.rs.reRmdChunkBegin(), code)))
  if (length(matches) > 1)
    code <- matches[[2]]

  tryCatch({
    # if this is the setup chunk, it's not included by default
    setupIndicator <- "r setup"
    if (identical(substring(code, 1, nchar(setupIndicator)), 
                  setupIndicator)) {
      opts$include <- FALSE
    }

    # extract and remove engine name (can be overridden below with engine=)
    opts$engine <- unlist(strsplit(code, split = "(\\s|,)+"))[[1]]
    code <- substring(code, nchar(opts$engine) + 1)

    # parse them, then merge with the defaults (evaluate in global environment)
    opts <- .rs.mergeLists(opts,
                           eval(substitute(knitr:::parse_params(code)),
                                envir = .GlobalEnv))
  },
  error = function(e) {})

  .rs.scalarListFromList(opts)
})

.rs.addFunction("extractChunkInnerCode", function(code)
{
  # split into lines
  code <- unlist(strsplit(code, "\n", fixed = TRUE))
  
  # find chunk indicators (safe fallbacks if absent)
  start <- max(1, grep(.rs.reRmdChunkBegin(), code, perl = TRUE))
  end   <- min(length(code), grep(.rs.reRmdChunkEnd(), code, perl = TRUE))

  paste(code[(start + 1):(end - 1)], collapse = "\n")
});

.rs.addFunction("extractRmdChunkInformation", function(rmd)
{
   starts <- grep(.rs.reRmdChunkBegin(), rmd, perl = TRUE)
   ends   <- grep(.rs.reRmdChunkEnd(), rmd, perl = TRUE)
   
   # TODO: how to handle invalid document?
   n <- min(length(starts), length(ends))
   if (length(starts) != n) starts <- starts[seq_len(n)]
   if (length(ends) != n)   ends <- ends[seq_len(n)]
   
   # collect chunk information
   lapply(seq_len(n), function(i) {
      begin    <- starts[[i]]
      end      <- ends[[i]]
      contents <- rmd[begin:end]
      list(
         begin    = starts[[i]],
         end      = ends[[i]],
         contents = contents
      )
   })
})

.rs.addFunction("rnb.generateRandomChunkId", function()
{
   candidates <- c(letters, 0:9)
   .rs.randomString("c", candidates = candidates, n = 12L)
})

.rs.addFunction("hydrateCacheFromNotebook", function(nbPath, cachePath = NULL)
{
   if (is.null(cachePath)) {
      rmdPath <- .rs.withChangedExtension(nbPath, ".Rmd")
      cachePath <- .rs.rnb.cachePathFromRmdPath(rmdPath)
   }

   # ensure cache directory
   if (!.rs.dirExists(cachePath))
      dir.create(cachePath, recursive = TRUE)
   
   # parse the notebook file
   nbData <- rmarkdown::parse_html_notebook(nbPath)
   
   # clear out old cache
   unlink(list.files(cachePath, full.names = TRUE), recursive = TRUE)
   
   # State, etc. ----
   lastActiveAnnotation   <- list()
   activeChunkId          <- "unknown"
   activeChunkIndex       <- 0
   activeIndex            <- 2
   
   headerContent <- nbData$source[`:`(
      grep("^\\s*<head>\\s*$", nbData$source, perl = TRUE)[[1]] + 1,
      grep("^\\s*</head>\\s*$", nbData$source, perl = TRUE)[[1]] - 1
   )]
   
   chunkInfo <- .rs.extractRmdChunkInformation(nbData$rmd)
   
   outputPath <- function(cachePath, chunkId, index, ext) {
      file.path(cachePath, chunkId, sprintf("%06s.%s", index, ext))
   }
   
   # Text ----
   onText <- function(annotation) {
      # HACK: the 'chunk' hook is not fired for chunks that
      # produce no output on execution, and so such chunks
      # are not annotated within the generated .nb.html file.
      # fortunately, we can detect two sequential 'text'
      # blocks as an indicator that there was an 'invisible'
      # chunk within.
      if (identical(annotation$state, "begin") &&
          identical(lastActiveAnnotation$label, "text") &&
          identical(lastActiveAnnotation$state, "end"))
      {
         activeChunkId    <<- .rs.rnb.generateRandomChunkId()
         activeChunkIndex <<- activeChunkIndex + 1
         activeIndex      <<- 2
      }
   }
   
   # Console I/O ----
   consoleDataBuilder <- .rs.listBuilder()
   writeConsoleData <- function(builder) {
      if (builder$empty()) return()
      
      # convert to .csv file
      df <- .rs.rbindList(builder$data())
      
      # reorder and rename type column
      df <- df[c("type", "data")]
      df$type[df$type == "input"]  <- "0"
      df$type[df$type == "output"] <- "1"
      
      path <- outputPath(cachePath, activeChunkId, activeIndex, "csv")
      .rs.ensureDirectory(dirname(path))
      write.table(df,
                  file = path,
                  quote = TRUE,
                  sep = ",",
                  row.names = FALSE,
                  col.names = FALSE,
                  fileEncoding = "UTF-8")
      
      # update state
      builder$clear()
      activeIndex <<- activeIndex + 1
   }
   
   onSource <- function(annotation) {
      if (annotation$state == "begin") {
         consoleDataBuilder$append(list(
            data = paste(annotation$meta$data, collapse = "\n"),
            type = "input"
         ))
      } else {
         # nothing to do?
      }
   }
   
   onOutput <- function(annotation) {
      if (annotation$state == "begin") {
         consoleDataBuilder$append(list(
            data = paste(annotation$meta$data, collapse = "\n"),
            type = "output"
         ))
      } else {
         # nothing to do?
      }
   }
   
   # Chunk Handling ----
   chunkDefnsBuilder <- .rs.listBuilder()
   onChunk <- function(annotation) {
      if (annotation$state == "begin") {
         
         # update state re: active chunk + number of processed outputs
         activeChunkIndex <<- activeChunkIndex + 1
         activeIndex      <<- 2
         
         # create chunk defn for this chunk
         info <- chunkInfo[[activeChunkIndex]]
         
         # get active chunk id (special handling for setup chunk)
         candidates <- c(letters, 0:9)
         if (.rs.isSetupChunk(info$contents))
            activeChunkId <<- "csetup_chunk"
         else
            activeChunkId <<- .rs.rnb.generateRandomChunkId()
         
         # add our chunk defn
         chunkDefnsBuilder$append(list(
            chunk_id = activeChunkId,
            expansion_state = 0,
            options = list(),         # TODO: parse from chunk header?
            row = info$end - 1,
            row_count = 1,            # TODO: ever not 1?
            visible = TRUE
         ))
         
      } else {
         # flush console data
         writeConsoleData(consoleDataBuilder)
         
         # leaving active chunk
         activeChunkId <<- "unknown"
      }
   }
   
   # Plot Handling ----
   plotRange <- list(start = NULL, end = NULL)
   plotMeta <- NULL
   writePlot <- function(source, range, meta) {
      
      # get html plot output
      html <- paste(source[`:`(
         range$start + 1,
         range$end - 1
      )], collapse = " ")
      
      # extract base64 encoded content
      scraped <- .rs.scrapeHtmlAttributes(html)
      ext <- if (.rs.startsWith(scraped$src, "data:image/jpeg;"))
                 "jpeg" else "png"
      imgDataEncoded <- substring(scraped$src, 
         nchar(paste("data:image/", ext, ";base64,", sep = "")) + 1)
      imgData <- .rs.base64decode(imgDataEncoded, binary = TRUE)
      
      # write to file
      path <- outputPath(cachePath, activeChunkId, activeIndex, ext)
      .rs.ensureDirectory(dirname(path))
      writeBin(imgData, path, useBytes = TRUE)
      
      # write metadata if present
      if (!is.null(meta) && !is.null(meta$metadata)) {
        metaPath <- outputPath(cachePath, activeChunkId, activeIndex, "metadata")
        cat(.rs.toJSON(meta, unbox = TRUE), file = metaPath, sep = "\n")
      }

      # update state
      activeIndex <<- activeIndex + 1
   }
   onPlot <- function(annotation) {
      if (annotation$state == "begin") {
         writeConsoleData(consoleDataBuilder)
         plotRange$start <<- annotation$row
         plotMeta        <<- annotation$meta
      } else {
         plotRange$end <<- annotation$row
         writePlot(nbData$source, plotRange, plotMeta)
         plotRange <<- list(start = NULL, end = NULL)
         plotMeta  <<- NULL
      }
   }

   # Data frame handling ----
   frameRange <- list(start = NULL, end = NULL)
   frameMeta <- NULL
   writeFrame <- function(source, range, meta) {
      if (is.null(meta))
        return(NULL);

      # write out frame metadata
      if (!is.null(meta$metadata)) {
        metaPath <- outputPath(cachePath, activeChunkId, activeIndex, "metadata")
        cat(.rs.toJSON(meta$metadata, unbox = TRUE), file = metaPath, sep = "\n")
      }

      # write out raw frame data
      if (!is.null(meta$rdf)) {
        rdfPath <- outputPath(cachePath, activeChunkId, activeIndex, "rdf")
        writeBin(object = .rs.base64decode(meta$rdf, binary = TRUE), 
                 con = rdfPath)
      }
   }
   onFrame <- function(annotation) {
      if (annotation$state == "begin") {
         writeConsoleData(consoleDataBuilder)
         frameRange$start <<- annotation$row
         frameMeta        <<- annotation$meta
      } else {
         frameRange$end <<- annotation$row
         writeFrame(nbData$source, frameRange, frameMeta)
         frameRange <<- list(start = NULL, end = NULL)
         frameMeta  <<- NULL
      }
   }
   
   # HTML Handling ----
   htmlRange <- list(start = NULL, end = NULL)
   htmlMeta <- NULL
   writeHtml <- function(source, range, meta) {
      
      # extract html from source document
      htmlOutput <- source[`:`(
         range$start + 1,
         range$end - 1
      )]
      
      htmlPath <- outputPath(cachePath, activeChunkId, activeIndex, "html")
      cat(htmlOutput, file = htmlPath, sep = "\n")

      # write metadata if present
      if (!is.null(meta) && !is.null(meta$metadata)) {
        metaPath <- outputPath(cachePath, activeChunkId, activeIndex, "metadata")
        cat(.rs.toJSON(meta, unbox = TRUE), file = metaPath, sep = "\n")
      }

      # update state
      activeIndex <<- activeIndex + 1
   }
   onHtml <- function(annotation) {
      if (annotation$state == "begin") {
         writeConsoleData(consoleDataBuilder)
         htmlRange$start <<- annotation$row
         htmlMeta        <<- annotation$meta
      } else {
         htmlRange$end   <<- annotation$row
         writeHtml(nbData$source, htmlRange, htmlMeta)
         htmlRange <<- list(start = NULL, end = NULL)
         htmlMeta  <<- NULL
      }
   }
   
   # HTML Widget Handling ----
   widgetRange <- list(start = NULL, end = NULL)
   widgetMeta  <- NULL
   writeHtmlWidget <- function(source, range, meta) {
      
      # get inner (body) HTML
      htmlBody <- source[`:`(
         range$start + 1,
         range$end - 1
      )]
      
      # TODO: background color should be passed as
      # metadata attribute and used here
      fmt <- paste(
         '<!DOCTYPE html>',
         '<html>',
         '<head>',
         '%s',
         '</head>',
         '<body style="background-color:white;">',
         '%s',
         '</body>',
         '</html>',
         sep = "\n"
      )
      
      htmlOutput <- sprintf(fmt,
                            paste(headerContent, collapse = "\n"),
                            paste(htmlBody, collapse = "\n"))
      
      htmlPath <- outputPath(cachePath, activeChunkId, activeIndex, "html")
      cat(htmlOutput, file = htmlPath, sep = "\n")
      
      jsonPath <- outputPath(cachePath, activeChunkId, activeIndex, "json")
      cat(.rs.toJSON(meta, unbox = TRUE), file = jsonPath, sep = "\n")
      
      # update state
      activeIndex <<- activeIndex + 1
   }
   onHtmlWidget <- function(annotation) {
      if (annotation$state == "begin") {
         writeConsoleData(consoleDataBuilder)
         widgetRange$start <<- annotation$row
         widgetMeta        <<- annotation$meta
      } else {
         widgetRange$end <- annotation$row
         writeHtmlWidget(nbData$source, widgetRange, widgetMeta)
         widgetRange <<- list(start = NULL, end = NULL)
         widgetMeta  <<- NULL
      }
   }
   
   # begin looping through annotations and building cache
   annotations <- nbData$annotations
   for (i in seq_along(annotations)) {
      
      annotation <- annotations[[i]]
      label <- annotation$label
      
      switch(label,
             "text"       = onText(annotation),
             "chunk"      = onChunk(annotation),
             "source"     = onSource(annotation),
             "output"     = onOutput(annotation),
             "plot"       = onPlot(annotation),
             "html"       = onHtml(annotation),
             "frame"      = onFrame(annotation),
             "htmlwidget" = onHtmlWidget(annotation))
      
      lastActiveAnnotation <- annotation
   }
   
   # write 'chunks.json' based on gathered chunk info
   mtime <- file.info(nbPath)$mtime
   chunks <- list(
      chunk_definitions = chunkDefnsBuilder$data(),
      doc_write_time    = as.numeric(mtime)
   )
   chunksJson <- .rs.toJSON(chunks, unbox = TRUE)
   chunksJsonPath <- file.path(cachePath, "chunks.json")
   cat(chunksJson, file = chunksJsonPath, sep = "\n")
   
   # NOTE: we don't bother writing a libs folder as we'll just dump
   # the base64 encoded headers into all HTML widget elements
   
})

.rs.addFunction("isSetupChunk", function(lines)
{
   if (!is.character(lines) || !length(lines))
      return(FALSE)
   
   grepl("^\\s*```{[Rr]\\s+setup[\\s,}]", lines[[1]], perl = TRUE)
})

.rs.addFunction("defaultChunkOptions", function()
{
   .rs.scalarListFromList(knitr::opts_chunk$get())
})

.rs.addFunction("setDefaultChunkOptions", function()
{
   knitr::opts_chunk$set(error = FALSE)
})

