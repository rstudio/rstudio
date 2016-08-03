#
# NotebookData.R
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

.rs.addFunction("initDataCapture", function(outputFolder, libraryFolder)
{
  assign("print.data.frame", function(x, ...) {
    output <- tempfile(pattern = "_rs_rdf_", tmpdir = outputFolder, 
                       fileext = "rdf")

    x <- head(x, 1000)

    save(
      x, 
      file = output)
    .Call("rs_recordData", output);
  }, envir = as.environment("tools:rstudio"))
})

.rs.addFunction("releaseDataCapture", function()
{
  rm("print.data.frame", envir = as.environment("tools:rstudio"))
})

.rs.addFunction("readDataCapture", function(path)
{
  e <- new.env(parent = emptyenv())
  load(file = path, envir = e)

  columns <- unname(lapply(
    names(e$x),
    function(columnName) {
      type <- class(e$x[[columnName]])[[1]]
      list(
        name = columnName,
        type = type,
        align = if (type == "character" || type == "factor") "left" else "right"
      )
    }
  ))

  data <- head(e$x, 1000)

  if (length(columns) > 0) {
    first_column = data[[1]]
    if (is.numeric(first_column) && all(diff(first_column) == 1))
      columns[[1]]$align <- "left"
  }

  data <- as.data.frame(
    lapply(
      data,
      function (y) as.character(y)),
    stringsAsFactors = FALSE)

  list(
    columns = columns,
    data = if (length(data) == 0) list() else data
  )
})

.rs.addFunction("runSqlForDataCapture", function(query, connectionName, outputFile)
{
  # precreate directories if needed
  dir.create(dirname(outputFile), recursive = TRUE, showWarnings = FALSE)

  conn <- get(connectionName, envir = globalenv())

  res <- DBI::dbSendQuery(conn, query)
  x <- if (!DBI::dbHasCompleted(res) || (DBI::dbGetRowCount(res) > 0))
            DBI::dbFetch(res, n = 1000)
  DBI::dbClearResult(res)

  save(
    x, 
    file = outputFile
  )
})