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
   
   # extract rmd contents and populate file
   contents <- .rs.extractFromNotebook("rnb-document-source", input)
   cat(contents, file = output, sep = "\n")
   
   # extract and populate cache
   .rs.hydrateCacheFromNotebook(input)
   
   # return TRUE to indicate success
   .rs.scalar(TRUE)
})

.rs.addFunction("extractFromNotebook", function(tag, rnbPath)
{
   if (!file.exists(rnbPath))
      stop("no file at path '", rnbPath, "'")
   
   contents <- .rs.readLines(rnbPath)
   
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
   .rs.base64decode(rmdEncoded)
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
   
   contents <- c(
      contents[1:idx],
      injection,
      contents[(idx + 1):length(contents)]
   )
   
   contents
})

.rs.addFunction("rnb.cachePathFromRmdPath", function(rmdPath)
{
   .Call("rs_chunkCacheFolder", rmdPath)
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
   rmdContents <- .rs.readLines(rmdPath)
   
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
   names(chunkInfo$chunk_definitions) <-
      unlist(lapply(chunkInfo$chunk_definitions, `[[`, "chunk_id"))
   
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

.rs.addFunction("rnb.getActiveChunkId", function(rnbData, row)
{
   chunkDefns <- rnbData$chunk_info$chunk_definitions
   chunkRows <- vapply(chunkDefns, `[[`, "row", FUN.VALUE = numeric(1))
   idx <- match(row, chunkRows)
   if (is.na(idx)) NULL else names(chunkRows)[[idx]]
})


.rs.addFunction("rnb.htmlAnnotatedOutput", function(output, label, meta = NULL)
{
   before <- if (is.null(meta)) {
      sprintf("\n<!-- rnb-%s-begin -->\n", label)
   } else {
      meta <- .rs.rnb.encode(meta)
      sprintf("\n<!-- rnb-%s-begin %s -->\n", label, meta)
   }
   after  <- sprintf("\n<!-- rnb-%s-end -->\n", label)
   paste(before, output, after, sep = "\n")
})

.rs.addFunction("rnb.annotatedKnitrHook", function(label, hook, meta = NULL) {
   force(list(label, hook, meta))
   function(x, ...) {
      output <- hook(x, ...)
      meta <- if (is.function(meta)) meta(x, output, ...)
      .rs.rnb.htmlAnnotatedOutput(output, label, meta)
   }
})

.rs.addFunction("rnb.renderHtmlWidget", function(output)
{
   unpreserved <- substring(
      output,
      .rs.nBytes("<!--html_preserve-->") + 1,
      .rs.nBytes(output) - .rs.nBytes("<!--/html_preserve-->")
   )
   
   meta <- .rs.rnb.encode(attr(output, "knit_meta"))
   
   before <- sprintf("\n<!-- rnb-htmlwidget-begin %s-->", meta)
   after  <- "<!-- rnb-htmlwidget-end -->\n"
   annotated <- htmltools::htmlPreserve(paste(before, unpreserved, after, sep = "\n"))
   attributes(annotated) <- attributes(output)
   return(annotated)
})

.rs.addFunction("rnb.htmlNotebook", function(...)
{
   if ("rmarkdown" %in% loadedNamespaces() &&
       "html_notebook" %in% getNamespaceExports("rmarkdown"))
   {
      return(rmarkdown::html_notebook(...))
   }
   
   # make sure 'html_notebook' available on search path
   toolsEnv <- .rs.toolsEnv()
   if (!exists("html_notebook", envir = toolsEnv))
      assign("html_notebook", .rs.rnb.htmlNotebook, envir = toolsEnv)
   
   # generate actual format
   rmarkdown::html_document(code_folding = "show",
                            theme = "cerulean",
                            highlight = "textmate",
                            ...)
})

.rs.addFunction("rnb.render", function(inputFile,
                                       outputFile,
                                       outputFormat = .rs.rnb.htmlNotebook(),
                                       rmdContents = .rs.readFile(inputFile),
                                       envir = .GlobalEnv)
{
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
   rnbContents <- .rs.readLines(renderOutput)
   
   # generate base64-encoded versions of .Rmd source, .md sidecar
   rmdEncoded <- .rs.base64encode(paste(rmdContents, collapse = "\n"))
   
   # inject document contents into rendered file
   # (i heard you like documents, so i put a document in your document)
   rnbContents <- .rs.injectHTMLComments(
      rnbContents,
      "</body>",
      list("rnb-document-source" = rmdEncoded)
   )
   
   # write our .Rnb to file and we're done!
   cat(rnbContents, file = outputFile, sep = "\n")
   invisible(outputFile)
   
})

.rs.addFunction("rnb.augmentKnitrHooks", function(format)
{
   # save original hooks
   savedKnitHooks <- knitr::knit_hooks$get()
   on.exit(knitr::knit_hooks$set(savedKnitHooks), add = TRUE)
   
   savedOptsChunk <- knitr::opts_chunk$get()
   on.exit(knitr::opts_chunk$set(savedOptsChunk), add = TRUE)
   
   # use 'render_markdown()' to get default hooks
   knitr::render_markdown()
   
   # store original hooks and annotate in format
   knitHooks <- knitr::knit_hooks$get()
   
   # generic hooks for knitr output
   hookNames <- c("source", "chunk", "plot", "text", "output",
                  "warning", "error", "message", "error")
   
   # metadata for hooks
   textMetaHook <- function(input, output, ...) {
      list(data = input)
   }
   
   metaFns <- list(
      source = textMetaHook,
      output = textMetaHook,
      warning = textMetaHook,
      message = textMetaHook,
      error = textMetaHook
   )
   
   newKnitHooks <- lapply(hookNames, function(hookName) {
      .rs.rnb.annotatedKnitrHook(hookName,
                                 knitHooks[[hookName]],
                                 metaFns[[hookName]])
   })
   names(newKnitHooks) <- hookNames
   
   format$knitr$knit_hooks <- newKnitHooks
   
   # hook into 'render' for htmlwidgets
   renderHook <- function(x, ...) {
      output <- knitr::knit_print(x, ...)
      if (inherits(x, "htmlwidget"))
         return(.rs.rnb.renderHtmlWidget(output))
      output
   }
   
   # set hooks on format
   format$knitr$knit_hooks <- newKnitHooks
   format$knitr$opts_chunk$render <- renderHook
   
   format
})

.rs.addFunction("createNotebook", function(inputFile,
                                           outputFile = NULL,
                                           envir = .GlobalEnv)
{
   if (is.null(outputFile))
      outputFile <- .rs.withChangedExtension(inputFile, ext = ".nb.html")
   
   # generate format
   format <- .rs.rnb.htmlNotebook()
   
   # augment hooks for createNotebook()
   format <- .rs.rnb.augmentKnitrHooks(format)
   
   # call render with our special format hooks
   .rs.rnb.render(inputFile, outputFile, outputFormat = format, envir = envir)
})

.rs.addFunction("replaceBinding", function(binding, package, override)
{
   # override in namespace
   if (!requireNamespace(package, quietly = TRUE))
      stop(sprintf("Failed to load namespace for package '%s'", package))
   
   namespace <- asNamespace(package)
   
   # get reference to original binding
   original <- get(binding, envir = namespace)
   
   # replace the binding
   if (is.function(override))
      environment(override) <- namespace
   
   do.call("unlockBinding", list(binding, namespace))
   assign(binding, override, envir = namespace)
   do.call("lockBinding", list(binding, namespace))
   
   # if package is attached, override there as well
   searchPathName <- paste("package", package, sep = ":")
   if (searchPathName %in% search()) {
      env <- as.environment(searchPathName)
      do.call("unlockBinding", list(binding, env))
      assign(binding, override, envir = env)
      do.call("lockBinding", list(binding, env))
   }
   
   # return original
   original
})

.rs.addFunction("rnb.cacheAugmentKnitrHooks", function(rnbData, format)
{
   # save original hooks
   savedKnitHooks <- knitr::knit_hooks$get()
   on.exit(knitr::knit_hooks$set(savedKnitHooks), add = TRUE)
   
   savedOptsChunk <- knitr::opts_chunk$get()
   on.exit(knitr::opts_chunk$set(savedOptsChunk), add = TRUE)
   
   # use 'render_markdown()' to get default hooks
   knitr::render_markdown()
   
   # store original hooks and annotate in format
   knitHooks <- knitr::knit_hooks$get()
   
   # track number of lines processed (so we can correctly map
   # chunks in document back to chunks in cache)
   linesProcessed <- 1
   
   original <- evaluate::evaluate
   
   # override 'evaluate' for duration of knit
   # TODO: this code can be removed once knitr hits CRAN
   evaluateOverride <- function(...) {
      knitr::knit_hooks$get("evaluate")(...)
   }
   
   # set and unset with pre/post hooks
   format$pre_knit <- function(...) {
      .rs.replaceBinding("evaluate", "evaluate", evaluateOverride)
      NULL
   }
   
   format$post_knit <- function(...) {
      .rs.replaceBinding("evaluate", "evaluate", original)
      NULL
   }
   
   # generate our custom hooks
   newKnitHooks <- list(
      
      text = function(x, ...) {
         newLineMatches <- gregexpr('\n', x, fixed = TRUE)[[1]]
         linesProcessed <<- linesProcessed + length(newLineMatches) + 1
         output <- knitHooks$text(x, ...)
         annotated <- .rs.rnb.htmlAnnotatedOutput(output, "text")
         annotated
      },
      
      chunk = function(...) {
         output <- knitHooks$chunk(...)
         annotated <- .rs.rnb.htmlAnnotatedOutput(output, "chunk")
         annotated
      },
      
      evaluate = function(code, ...) {
         
         linesProcessed <<- linesProcessed + length(code)
         activeChunkId <- .rs.rnb.getActiveChunkId(rnbData, linesProcessed)
         linesProcessed <<- linesProcessed + 2
         
         # TODO: this implies there was no chunk output associated
         # with this chunk, but it would be pertinent to validate
         if (is.null(activeChunkId))
            return(knitr::asis_output(""))
         
         activeChunkData <- rnbData$chunk_data[[activeChunkId]]
         
         # collect output for knitr
         # TODO: respect output options?
         htmlOutput <- .rs.enumerate(activeChunkData, function(key, val) {
            
            # png output: create base64 encoded image
            if (.rs.endsWith(key, ".png")) {
               rendered <- .rs.rnb.renderBase64Png(bytes = val)
               annotated <- .rs.rnb.htmlAnnotatedOutput(rendered, "plot")
               return(knitr::asis_output(annotated))
            }
            
            # console output
            if (.rs.endsWith(key, ".csv")) {
               htmlList <- .rs.rnb.consoleDataToHtmlList(val)
               transformed <- lapply(htmlList, function(el) {
                  output <- el$output
                  label <- if (el$type == "input") "source" else "output"
                  meta <- list(data = el$input)
                  .rs.rnb.htmlAnnotatedOutput(output, label, meta)
               })
               output <- paste(transformed, collapse = "\n")
               return(knitr::asis_output(output))
            }
            
            # html widgets
            if (.rs.endsWith(key, ".html")) {
               jsonName <- .rs.withChangedExtension(key, ".json")
               jsonPath <- file.path(rnbData$cache_path, activeChunkId, jsonName)
               jsonContents <- .rs.fromJSON(.rs.readFile(jsonPath))
               for (i in seq_along(jsonContents))
                  class(jsonContents[[i]]) <- "html_dependency"
               
               bodyEl <- .rs.extractHTMLBodyElement(val)
               
               rendered <- htmltools::htmlPreserve(bodyEl)
               annotated <- .rs.rnb.htmlAnnotatedOutput(rendered, "htmlwidget", meta = jsonContents)
               output <- knitr::asis_output(annotated, meta = jsonContents)
               return(output)
            }
            
            # TODO: shouldn't get here?
            NULL
         })
         
         list(
            structure(list(src = NULL), class = "source"),
            htmlOutput
         )
      }
   )
   
   # save to format
   format$knitr$knit_hooks <- newKnitHooks
   
   format
})
   
.rs.addFunction("createNotebookFromCacheData", function(rnbData,
                                                        inputFile,
                                                        outputFile = NULL,
                                                        envir = .GlobalEnv)
{
   if (is.null(outputFile))
      outputFile <- .rs.withChangedExtension(inputFile, ext = ".nb.html")
   
   # generate format
   format <- .rs.rnb.htmlNotebook()
   
   # augment hooks
   format <- .rs.rnb.cacheAugmentKnitrHooks(rnbData, format)
   
   # call render with special format hooks
   .rs.rnb.render(inputFile, outputFile, outputFormat = format, envir = envir)
})

.rs.addFunction("createNotebookFromCache", function(rmdPath, outputPath = NULL)
{
   if (is.null(outputPath))
      outputPath <- .rs.withChangedExtension(rmdPath, "Rnb")
   
   cachePath <- .rs.rnb.cachePathFromRmdPath(rmdPath)
   if (!file.exists(cachePath)) {
      
      # render our notebook, but don't evaluate any R code
      eval <- knitr::opts_chunk$get("eval")
      knitr::opts_chunk$set(eval = FALSE)
      on.exit(knitr::opts_chunk$set(eval = eval), add = TRUE)
      
      # create the notebook
      .rs.createNotebook(inputFile = rmdPath,
                         outputFile = outputPath)
      
      return(TRUE)
   }
   
   rnbData <- .rs.readRnbCache(rmdPath, cachePath)
   .rs.createNotebookFromCacheData(rnbData, rmdPath, outputPath)
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

.rs.addFunction("rnb.renderBase64Html", function(path = NULL, bytes = NULL, format)
{
   if (is.null(bytes))
      bytes <- .rs.readFile(path, binary = TRUE)
   
   encoded <- .rs.base64encode(bytes)
   sprintf(format, encoded)
})

.rs.addFunction("rnb.renderBase64Png", function(path = NULL, bytes = NULL)
{
   format <- '<img src="data:image/png;base64,%s" />'
   .rs.rnb.renderBase64Html(path, bytes, format)
})

.rs.addFunction("rnb.renderBase64JavaScript", function(path = NULL, bytes = NULL)
{
   format <- '<script src="data:application/x-javascript;base64,%s"></script>'
   .rs.rnb.renderBase64Html(path, bytes, format)
})

.rs.addFunction("rnb.renderBase64StyleSheet", function(path = NULL, bytes = NULL)
{
   format <- '<link href="data:text/css;charset=utf8;base64,%s" />'
   .rs.rnb.renderBase64Html(path, bytes, format)
})

.rs.addFunction("rnb.consoleDataToHtmlList", function(data, prefix = knitr::opts_chunk$get("comment"))
{
   csvData <- .rs.rnb.parseConsoleData(data)
   cutpoints <- .rs.cutpoints(csvData$type)
   
   ranges <- Map(
      function(start, end) list(start = start, end = end),
      c(1, cutpoints),
      c(cutpoints - 1, nrow(csvData))
   )
   
   splat <- lapply(ranges, function(range) {
      
      type <- csvData$type[[range$start]]
      collapse <- if (type == 0) "\n" else ""
      
      pasted <- paste(csvData$text[range$start:range$end], collapse = collapse)
      result <- .rs.trimWhitespace(pasted)
      if (!nzchar(result))
         return(NULL)
      
      if (type == 1 || type == 2)
         result <- paste(prefix, gsub("\n", paste0("\n", prefix, " "), result, fixed = TRUE))
      
      attr(result, ".class") <- if (type == 0) "r"
      result
   })
   
   filtered <- Filter(Negate(is.null), splat)
   htmlList <- lapply(filtered, function(el) {
      class <- attr(el, ".class")
      result <- if (is.null(class)) {
         sprintf(
            "<pre><code>%s</code></pre>",
            el
         )
      } else {
         sprintf(
            "<pre class=\"%s\"><code>%s</code></pre>",
            class,
            el
         )
      }
      
      list(input = el,
           output = result,
           type = if (is.null(class)) "output" else "input")
   })
   
   htmlList
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

.rs.addFunction("listToHtmlAttributes", function(data)
{
   paste(
      names(data),
      .rs.surround(unlist(data), with = "\""),
      sep = "="
   )
})

.rs.addFunction("rnb.encode", function(data)
{
   .rs.base64encode(.rs.toJSON(data, unbox = TRUE))
})

.rs.addFunction("rnb.decode", function(encoded)
{
   .rs.fromJSON(.rs.base64decode(encoded))
})

.rs.addFunction("evaluateChunkOptions", function(options)
{
  opts <- list()
  tryCatch({
    # if this is the setup chunk, it's not included by default
    setupIndicator <- "r setup"
    if (identical(substring(options, 1, nchar(setupIndicator)), 
                  setupIndicator)) {
      opts$include <- FALSE
    }

    # remove leading text from the options
    options <- sub("^[^,]*,\\s*", "", options)

    # parse them, then merge with the defaults
    opts <- .rs.mergeLists(opts,
                           eval(parse(text = paste("list(", options, ")"))))
                           
  },
  error = function(e) {})

  .rs.scalarListFromList(opts)
})

# TODO: Consider re-implementing in C++ if processing
# in R is too slow.
.rs.addFunction("parseNotebook", function(nbPath)
{
   contents <- .rs.readLines(nbPath)
   
   reComment  <- "^\\s*<!--\\s*rnb-([^-]+)-(begin|end)\\s*([^\\s-]+)?\\s*-->\\s*$"
   reDocument <- "^\\s*<!--\\s*rnb-document-source\\s*([^\\s-]+)\\s*-->\\s*$"
   
   rmdContents <- NULL
   builder <- .rs.listBuilder()
   
   for (row in seq_along(contents)) {
      line <- contents[[row]]
      
      # extract document contents
      matches <- gregexpr(reDocument, line, perl = TRUE)[[1]]
      if (!identical(c(matches), -1L)) {
         start <- c(attr(matches, "capture.start"))
         end   <- start + c(attr(matches, "capture.length")) - 1
         decoded <- .rs.base64decode(substring(line, start, end))
         rmdContents <- strsplit(decoded, "\\r?\\n", perl = TRUE)[[1]]
         next
      }
      
      # extract information from comment
      matches <- gregexpr(reComment, line, perl = TRUE)[[1]]
      if (identical(c(matches), -1L))
         next
      
      starts <- c(attr(matches, "capture.start"))
      ends   <- starts + c(attr(matches, "capture.length")) - 1
      strings <- substring(line, starts, ends)
      
      n <- length(strings)
      if (n < 2)
         stop("invalid rnb comment")
      
      # decode comment information and update stack
      data <- list(row = row,
                   label = strings[[1]],
                   state = strings[[2]])
      
      # add metadata if available
      if (n >= 3 && nzchar(strings[[3]]))
         data[["meta"]] <- .rs.rnb.decode(strings[[3]])
      else
         data["meta"] <- list(NULL)
      
      # append
      builder$append(data)
   }
   
   annotations <- builder$data()
   
   # extract header content
   headStart <- grep("^\\s*<head>\\s*$", contents, perl = TRUE)[[1]]
   headEnd   <- grep("^\\s*</head>\\s*$", contents, perl = TRUE)[[1]]
   
   list(source = contents,
        rmd = rmdContents,
        header = contents[headStart:headEnd],
        annotations = annotations)
   
})

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
   nbData <- .rs.parseNotebook(nbPath)
   
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
   writePlot <- function(source, range) {
      
      # get html plot output
      html <- paste(source[`:`(
         range$start + 1,
         range$end - 1
      )], collapse = " ")
      
      # extract base64 encoded content
      scraped <- .rs.scrapeHtmlAttributes(html)
      pngDataEncoded <- substring(scraped$src, nchar("data:image/png;base64,") + 1)
      pngData <- .rs.base64decode(pngDataEncoded, binary = TRUE)
      
      # write to file
      path <- outputPath(cachePath, activeChunkId, activeIndex, "png")
      .rs.ensureDirectory(dirname(path))
      writeBin(pngData, path, useBytes = TRUE)
      
      # update state
      activeIndex <<- activeIndex + 1
   }
   onPlot <- function(annotation) {
      if (annotation$state == "begin") {
         writeConsoleData(consoleDataBuilder)
         plotRange$start <<- annotation$row
      } else {
         plotRange$end <<- annotation$row
         writePlot(nbData$source, plotRange)
         plotRange <<- list(start = NULL, end = NULL)
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