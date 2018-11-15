#
# SessionDataPreview.R
#
# Copyright (C) 2009-18 by RStudio, Inc.
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
   eval(parse(text = code), envir = globalenv())
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
   conn <- force(conn)

   # fetch at most 100 records as a preview
   rs <- DBI::dbSendQuery(conn, statement = statement, ...)
   data <- DBI::dbFetch(rs, n = 1000)
   DBI::dbClearResult(rs)

   .rs.previewDataFrame(data, script)
})

