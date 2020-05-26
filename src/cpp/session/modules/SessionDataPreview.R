#
# SessionDataPreview.R
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

.rs.addJsonRpcHandler("preview_sql", function(code)
{
   # helper function for reporting errors
   onError <- function(reason) {
      .rs.scalar(.rs.truncate(reason))
   }
   
   # parse the user-provided code
   parsed <- .rs.tryCatch(parse(text = code)[[1]])
   if (inherits(parsed, "error"))
      return(onError("Failed to parse SQL preview comment."))
   
   # substitute missing arguments for NULL, so that match.call
   # does what we expect
   for (i in seq_along(parsed))
      if (identical(parsed[[i]], quote(expr = )))
         parsed[i] <- list(NULL)

   # attempt to match the call
   matched <- .rs.tryCatch(match.call(.rs.previewSql, parsed))
   if (inherits(matched, "error"))
      return(onError("Unexpected SQL preview comment."))
   
   # validate that the user provided a connection
   if (is.null(matched$conn))
      return(onError("No connection was specified in SQL preview comment."))
   
   # okay, try to evaluate it
   status <- .rs.tryCatch(eval(parsed, envir = globalenv()))
   if (inherits(status, "error"))
      return(onError(conditionMessage(status)))
   
   invisible(status)
})

.rs.addFunction("previewDataFrame", function(data, script)
{
   preparedData <- .rs.prepareViewerData(
      data,
      maxFactors = 100,
      maxCols = 100,
      maxRows = 1000
   )

   preview <- list(
      data = preparedData$data,
      columns = preparedData$columns,
      title = if (is.character(script)) .rs.scalar(script) else NULL
   )

   .rs.enqueClientEvent("data_output_completed", preview)
   
   invisible(NULL)

})

.rs.addFunction("previewSql", function(conn, statement, ...)
{
   script <- NULL
   if (file.exists(statement)) {
      script <- statement
      statement <- paste(readLines(script), collapse = "\n")
   }

   # remove comments since some drivers might not support them
   statement <- gsub("--[^\n]*\n+", "", statement)

   # force the connection to let DBI and others initialize S3
   conn <- .rs.tryCatch(force(conn))
   if (inherits(conn, "error")) {
      msg <- paste("Failed to retrieve connection:", conditionMessage(conn))
      return(.rs.scalar(msg))
   }

   # fetch at most 100 records as a preview
   status <- .rs.tryCatch({
      rs <- DBI::dbSendQuery(conn, statement = statement, ...)
      data <- DBI::dbFetch(rs, n = 1000)
      DBI::dbClearResult(rs)
   })
   
   if (inherits(status, "error")) {
      msg <- paste("Failed to query database:", conditionMessage(status))
      return(.rs.scalar(msg))
   }

   .rs.previewDataFrame(data, script)
})

