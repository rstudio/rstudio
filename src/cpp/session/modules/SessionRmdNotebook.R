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

.rs.addFunction("readFile", function(path) {
   readChar(path, file.info(path)$size, TRUE)
})

# Given the path to a .Rmd file, and its accompanying cache folder, create
# an R object that represents the collection of these dependencies on disk.
.rs.addFunction("createNotebook", function(rmdPath, cachePath = NULL)
{
   if (!file.exists(rmdPath))
      stop("No file at path '", rmdPath, "'")
   
   contents <- suppressWarnings(readLines(rmdPath))
   
   # Resolve the cache directory
   if (is.null(cachePath))
   {
      cachePath <- paste(
         tools::file_path_sans_ext(rmdPath),
         ".Rnb.cached",
         sep = ""
      )
   }
   
   if (!file.exists(cachePath))
      stop("No cache directory at path '", cachePath, "'")
   
   # Begin collecting the units that form the Rnb data structure
   rnbData <- list()
   
   # Keep the original source data
   rnbData[["contents"]] <- contents
   
   # Read the chunk information
   chunkInfoPath <- file.path(cachePath, "chunks.csv")
   chunkInfo <- read.table(chunkInfoPath, header = TRUE, sep = ",")
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
   if (file.exists(libDir))
   {
      owd <- setwd(libDir)
      libFiles <- list.files(libDir, recursive = TRUE)
      libData <- lapply(libFiles, .rs.readFile)
      names(libData) <- libFiles
      rnbData[["lib"]] <- libData
      setwd(owd)
   }
   
   rnbData
})

.rs.addFunction("writeNotebook", function(rnbData)
{
   html <- .rs.stringBuilder()
   
   html$append(
      '<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">',
      '<html xmlns="http://www.w3.org/1999/xhtml">'
   )
   
   # write document contents as comment block
   html$append(
      "<!-- document-source",
      rnbData$contents,
      "-->"
   )
   
   # header
   html$append("<head>")
   html$append("</head>")
   
   # body
   html$append("<body>")
   
   # begin writing chunk output, etc
   
   
   html$append("</body>")
 
   # close the HTML
   html$append("</html>")
   
})

.rs.addFunction("stringBuilder", function()
{
   (function() {
      # private
      data_ <- c()
      
      # public
      append  <- function(...) data_ <<- c(data_, ...)
      prepend <- function(...) data_ <<- c(..., data_)
      data    <- function()    data_
      
      # exported
      list(append = append, prepend = prepend, data = data)
   })()
})












