#
# RStudioAPI.R
#
# Copyright (C) 2020 by RStudio, PBC
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

.rs.addJsonRpcHandler("get_document_chunk_context", function(docId)
{
   data <- .rs.getDocumentChunkContext(docId)
   .rs.scalarListFromList(data)
})

.rs.addFunction("getDocumentChunks", function(properties)
{
   # map internal RStudio source types to knitr pattern types
   patternMap <- list(
      r_markdown = "md"
   )
   
   patternName <- patternMap[[properties$type]]
   if (is.null(patternName))
      return(getDocumentChunksDefault(properties))
   
   # grab reference to knit env (need to clean things up when we're done)
   envir <- knitr:::.knitEnv
   
   # read and restore knitr labels when we're done
   labels <- get("labels", envir = envir)
   on.exit(assign("labels", labels, envir = envir), add = TRUE)
   
   # now use knitr to split the file into chunks
   contents <- strsplit(properties$contents, split = "\n", fixed = TRUE)[[1L]]
   knitr:::split_file(contents, patterns = knitr::all_patterns[[patternName]])
})

.rs.addFunction("getDocumentChunkContext", function(docId)
{
   # read document properties
   properties <- .rs.getSourceDocumentProperties(
      id = docId,
      includeContents = TRUE
   )
   
   # split document into chunks
   chunks <- .rs.getDocumentChunks(properties)
   
   # return list of data
   list(id = properties$id, type = properties$type, chunks = chunks)
})

.rs.addFunction("rstudioapi.processRequest", function(requests,
                                                      response,
                                                      secret)
{
   result <- .rs.tryCatch(
      .rs.rstudioapi.processRequestImpl(requests, response, secret)
   )
   
   unlink(requests)
   if (inherits(result, "error")) {
      saveRDS(result, response)
      return(FALSE)
   }
   
   TRUE
})

.rs.addFunction("rstudioapi.processRequestImpl", function(requests,
                                                          response,
                                                          secret)
{
   data <- readRDS(requests)
   if (!identical(data$secret, secret))
      stop("invalid secret in rstudioapi IPC")
   
   call <- data$call
   output <- eval(call, envir = baseenv())
   saveRDS(output, file = response)
})
