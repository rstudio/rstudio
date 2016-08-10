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

.rs.addFunction("dataCaptureOverrides", function(outputFolder, libraryFolder)
{
  c(
    "print.data.frame",
    "print.tbl_df"
  )
})

.rs.addFunction("initDataCapture", function(outputFolder, libraryFolder)
{
  overridePrint <- function(x, ...) {
    output <- tempfile(pattern = "_rs_rdf_", tmpdir = outputFolder, 
                       fileext = ".rdf")

    x <- head(x, getOption("max.print", 1000))

    save(
      x, 
      file = output)

    invisible(.Call("rs_recordData", output));
  }

  lapply(.rs.dataCaptureOverrides(), function(override) {
    assign(
      override,
      overridePrint,
      envir = as.environment("tools:rstudio")
    )
  })

  assign(
    "dplyr_tibble_print_original",
    getOption("dplyr.tibble.print"),
    envir = as.environment("tools:rstudio")
  )

  options("dplyr.tibble.print" = function(x, n, width, ...) {
    isSQL <- "tbl_sql" %in% class(x)
    n <- if (isSQL) getOption("sql.max.print", 1000) else getOption("max.print", 1000)

    print(as.data.frame(head(x, n)))
  })
})

.rs.addFunction("releaseDataCapture", function()
{
  options(
    "dplyr.tibble.print" = get(
      "dplyr_tibble_print_original",
      envir = as.environment("tools:rstudio")
    )
  )

  overrides <- .rs.dataCaptureOverrides()
  rm(
    list = overrides,
    envir = as.environment("tools:rstudio")
  )
})

.rs.addFunction("readDataCapture", function(path)
{
  e <- new.env(parent = emptyenv())
  load(file = path, envir = e)

  columns <- unname(lapply(
    names(e$x),
    function(columnName) {
      type <- tibble::type_sum(e$x[[columnName]])
      list(
        name = columnName,
        type = type,
        align = if (type == "character" || type == "factor") "left" else "right"
      )
    }
  ))

  data <- head(e$x, getOption("max.print", 1000))

  is_list <- vapply(data, is.list, logical(1))
  data[is_list] <- lapply(data[is_list], function(x) {
    summary <- tibble::obj_sum(x)
    paste0("<", summary, ">")
  })

  if (length(columns) > 0) {
    first_column = data[[1]]
    if (is.numeric(first_column) && all(diff(first_column) == 1))
      columns[[1]]$align <- "left"
  }

  data <- as.data.frame(
    lapply(
      data,
      function (y) format(y)),
    stringsAsFactors = FALSE)

  list(
    columns = columns,
    data = if (length(data) == 0) list() else data
  )
})

.rs.addFunction("runSqlForDataCapture", function(sql, outputFile, options)
{
  # precreate directories if needed
  dir.create(dirname(outputFile), recursive = TRUE, showWarnings = FALSE)

  max.print <- if (is.null(options$max.print)) getOption("max.print", 1000) else as.numeric(options$max.print)
  max.print <- if (is.null(options$sql.max.print)) max.print else as.numeric(options$sql.max.print)

  conn <- get(options$connection, envir = globalenv())
  if (is.null(conn)) stop(
    "The 'connection' option (DBI connection) is required for sql chunks."
  )

  # Return char vector of sql interpolation param names
  varnames_from_sql = function(conn, sql) {
    varPos = DBI::sqlParseVariables(conn, sql)
    if (length(varPos$start) > 0) {
      varNames = substring(sql, varPos$start, varPos$end)
      sub("^\\?", "", varNames)
    }
  }

  # Vectorized version of exists
  mexists = function(x, env = globalenv(), inherits = TRUE) {
    vapply(x, exists, logical(1), where = env, inherits = inherits)
  }

  # Interpolate a sql query based on the variables in an environment
  interpolate_from_env = function(conn, sql, env = globalenv(), inherits = TRUE) {
    names = unique(varnames_from_sql(conn, sql))
    names_missing = names[!mexists(names, env, inherits)]
    if (length(names_missing) > 0) {
      stop("Object(s) not found: ", paste('"', names_missing, '"', collapse = ", "))
    }

    args = if (length(names) > 0) setNames(
      mget(names, envir = env, inherits = inherits), names
    )

    do.call(DBI::sqlInterpolate, c(list(conn, sql), args))
  }

  # extract options
  varname = options$output.var

  # execute query -- when we are printing with an enforced max.print we
  # use dbFetch so as to only pull down the required number of records
  query = interpolate_from_env(conn, sql)
  if (is.null(varname) && max.print > 0) {
    res = DBI::dbSendQuery(conn, query)
    data = if (!DBI::dbHasCompleted(res) || (DBI::dbGetRowCount(res) > 0))
              DBI::dbFetch(res, n = max.print)
    DBI::dbClearResult(res)
  } else {
    data = DBI::dbGetQuery(conn, query)
  }

  res <- DBI::dbSendQuery(conn, query)
  x <- if (!DBI::dbHasCompleted(res) || (DBI::dbGetRowCount(res) > 0))
            DBI::dbFetch(res, n = max.print)
  DBI::dbClearResult(res)

  # assign varname if requested, otherwise print
  if (!is.null(varname)) {
    assign(varname, data, envir = globalenv())
  }
  else {
    x <- data
    save(
      x, 
      file = outputFile
    )
  }
})
