#
# SessionNotebookRender.R
#
# Copyright (C) 2025 by Posit Software, PBC
#
# Unless you have received this program directly from Posit Software pursuant
# to the terms of a commercial license agreement with Posit Software, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#
# Renders an R Notebook .nb.html file from its chunk output cache.
# Designed to run in a child R process via AsyncRProcess in augmented mode
# (Tools.R is sourced first, providing .rs.addFunction and other helpers).
#
# After this file is sourced, SessionRmdNotebook.R is also sourced to provide
# the .rs.rnb.* functions. This file defines overrides for functions that
# rely on .Call() native methods which are unavailable in the child process.
#
# Parameters are passed via environment variables:
#   RS_NB_RMD_PATH    -- absolute path to the .Rmd file
#   RS_NB_CACHE_PATH  -- absolute path to the chunk cache directory
#   RS_NB_OUTPUT_PATH -- absolute path for the .nb.html output
#   RS_NB_ENCODING    -- file encoding (e.g. "UTF-8")
#

# --- Override functions that use .Call() ----------------------------------

# .rs.readLines uses .Call("rs_readLines") -- replace with base R
.rs.addFunction("readLines", function(filePath)
{
   readLines(path.expand(filePath), warn = FALSE)
})

# .rs.fromJSON uses .Call("rs_fromJSON") -- replace with jsonlite
.rs.addFunction("fromJSON", function(string)
{
   jsonlite::fromJSON(string, simplifyVector = FALSE)
})

# .rs.base64encode uses .Call("rs_base64encode") -- replace with pure R impl
.rs.addFunction("base64encode", function(data, binary = FALSE)
{
   if (is.character(data))
      data <- charToRaw(data)
   .rs.rBase64Encode(data)
})

# .rs.scalar is defined in ModuleTools.R which isn't sourced in augmented mode
.rs.addFunction("scalar", function(obj)
{
   if (!is.null(obj))
      class(obj) <- "rs.scalar"
   obj
})

# .rs.rnb.cachePathFromRmdPath uses .Call("rs_chunkCacheFolder") --
# we receive the cache path from C++ via environment variable
.rs.addFunction("rnb.cachePathFromRmdPath", function(rmdPath)
{
   Sys.getenv("RS_NB_CACHE_PATH")
})

# .rs.getSourceDocumentProperties uses .Call("rs_getDocumentProperties") --
# we receive the encoding from C++ via environment variable
.rs.addFunction("getSourceDocumentProperties", function(path, includeContents = FALSE)
{
   list(encoding = Sys.getenv("RS_NB_ENCODING", unset = "UTF-8"))
})

# --- Define helpers from SessionCodeTools.R needed by SessionRmdNotebook.R ---

.rs.addFunction("enumerate", function(X, FUN, ...)
{
   keys <- if (is.environment(X))
      sort(ls(envir = X))
   else
      names(X)

   result <- lapply(keys, function(key) {
      FUN(key, X[[key]], ...)
   })
   names(result) <- keys
   result
})

.rs.addFunction("cutpoints", function(data)
{
   diffed <- diff(c(data[1], data))
   which(diffed != 0)
})

# --- Define .rs.readDataCapture from NotebookData.R -----------------------

.rs.addFunction("readDataCapture", function(path)
{
   envir <- new.env(parent = emptyenv())
   load(path, envir = envir)

   # modern caches store a pre-formatted 'result'
   result <- envir[["result"]]
   if (!is.null(result))
      return(result)

   # fallback for old caches: format on the fly
   data <- envir[["x"]]
   options <- envir[["options"]]
   .rs.formatDataCapture(data, options)
})

.rs.addFunction("formatDataCapture", function(data, options)
{
   type_sum <- function(x) {
      format_sum <- switch(class(x)[[1]],
         ordered = "ord",
         factor = "fctr",
         POSIXt = "dttm",
         difftime = "time",
         Date = "date",
         data.frame = class(x)[[1]],
         tbl_df = "tibble",
         NULL
      )
      if (!is.null(format_sum)) {
         format_sum
      } else if (!is.object(x)) {
         switch(typeof(x),
            logical = "lgl",
            integer = "int",
            double = "dbl",
            character = "chr",
            complex = "cplx",
            closure = "fun",
            environment = "env",
            typeof(x)
         )
      } else if (!isS4(x)) {
         paste0("S3: ", class(x)[[1]])
      } else {
         paste0("S4: ", methods::is(x)[[1]])
      }
   }

   size_sum <- function(x) {
      dim <- dim(x) %||% length(x)
      format_dim <- vapply(dim, function(d) {
         mark <- if (identical(getOption("OutDec"), ",")) "." else ","
         formatC(d, big.mark = mark)
      }, character(1))
      format_dim[is.na(dim)] <- "??"
      paste0(" [", paste0(format_dim, collapse = " \u00d7 "), "]")
   }

   `%||%` <- function(x, y) if (is.null(x)) y else x

   is_list_not_vctrs <- function(x) is.list(x) && !inherits(x, "vctrs_vctr")

   obj_sum <- function(x) {
      switch(class(x)[[1]],
         POSIXlt = rep("POSIXlt", length(x)),
         list = vapply(x, function(el) paste0(type_sum(el), size_sum(el)), character(1L)),
         paste0(type_sum(x), size_sum(x))
      )
   }

   columnNames <- names(data)
   columnSequence <- seq_len(ncol(data))

   columns <- lapply(columnSequence, function(columnIdx) {
      column <- data[[columnIdx]]
      baseType <- class(column)[[1]]
      tibbleType <- type_sum(column)
      list(
         label = if (!is.null(columnNames)) columnNames[[columnIdx]] else "",
         name = columnIdx,
         type = tibbleType,
         align = if (baseType == "character" || baseType == "factor") "left" else "right"
      )
   })

   addRowNames <- isTRUE(options[["rownames.print"]])
   if (addRowNames) {
      columns <- c(
         list(list(label = "", name = "_rn_", type = "", align = "left")),
         columns
      )
      data$`_rn_` <- rownames(data)
   }

   names(data) <- as.character(columnSequence)
   columns <- unname(columns)

   is_list <- vapply(data, is_list_not_vctrs, logical(1))
   data[is_list] <- lapply(data[is_list], function(x) {
      paste0("<", obj_sum(x), ">")
   })

   values <- lapply(data, function(y) {
      if (length(y) == 0) return(character())
      if (typeof(y) == "character") y[y == "NA"] <- "__NA__"
      y <- encodeString(format(y))
      gsub("^\\s+|\\s+$", "", y)
   })

   data <- as.data.frame(values, stringsAsFactors = FALSE, optional = TRUE)

   pagedTableOptions <- list(
      columns = list(
         min = options[["cols.min.print"]],
         max = if (is.null(options[["cols.print"]])) 10 else options[["cols.print"]],
         total = options[["cols.total"]]
      ),
      rows = list(
         min = if (is.null(options[["rows.print"]])) 10 else options[["rows.print"]],
         max = if (is.null(options[["rows.print"]])) 10 else options[["rows.print"]],
         total = options[["rows.total"]]
      ),
      pages = options[["pages.print"]]
   )

   list(
      columns = columns,
      data = if (length(data) == 0) list() else data,
      options = pagedTableOptions
   )
})

# --- Main render (after SessionRmdNotebook.R is sourced) ------------------

.rs.addFunction("renderNotebookAsync", function()
{
   rmdPath    <- Sys.getenv("RS_NB_RMD_PATH")
   outputPath <- Sys.getenv("RS_NB_OUTPUT_PATH")

   if (nchar(rmdPath) == 0 || nchar(outputPath) == 0)
      stop("RS_NB_RMD_PATH and RS_NB_OUTPUT_PATH must be set")

   tryCatch({
      cachePath <- .rs.rnb.cachePathFromRmdPath(rmdPath)
      rnbData <- .rs.readRnbCache(rmdPath, cachePath)
      .rs.createNotebookFromCacheData(rnbData, rmdPath, outputPath)
      cat("__RENDER_SUCCESS__\n")
   }, error = function(e) {
      cat(paste0("__RENDER_ERROR__:", jsonlite::toJSON(
         list(error = e$message),
         auto_unbox = TRUE
      ), "\n"))
      quit(save = "no", status = 1)
   })
})
