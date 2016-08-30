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
    "print.data.frame" = function(x, options) list(x = x, options = options, className = class(x), nRow = .rs.scalar(nrow(x)), nCol = .rs.scalar(ncol(x))),
    "print.tbl_df" = function(x, options)     list(x = x, options = options, className = class(x), nRow = .rs.scalar(nrow(x)), nCol = .rs.scalar(ncol(x))),
    "print.grouped_df" = function(x, options) list(x = x, options = options, className = class(x), nRow = .rs.scalar(nrow(x)), nCol = .rs.scalar(ncol(x))),
    "print.data.table" = function(x, options) list(x = x, options = options, className = class(x), nRow = .rs.scalar(nrow(x)), nCol = .rs.scalar(ncol(x))),
    "print.tbl_lazy" = function(x, options) {
      tblLazyData <- lapply(dplyr::tbl_vars(x), function(e) character(0))
      names(tblLazyData) <- dplyr::tbl_vars(x)
      lazyFrame <- do.call("data.frame", tblLazyData)

      list(
        x = lazyFrame,
        options = options,
        className = class(x),
        nRow = "?",
        nCol = "?"
      )
    }

  )
})

.rs.addFunction("initDataCapture", function(outputFolder, options)
{
  overridePrint <- function(x, options, className, nRow, nCol) {
    options <- if (is.null(options)) list() else options
    
    optionRowNames <- options[["rownames.print"]]
    options[["rownames.print"]] <- if (is.null(optionRowNames)) (.row_names_info(x, type = 1) > 0) else optionRowNames

    output <- tempfile(pattern = "_rs_rdf_", tmpdir = outputFolder, 
                       fileext = ".rdf")

    max.print <- if (is.null(options$max.print)) getOption("max.print", 1000) else options$max.print

    x <- head(x, max.print)

    save(
      x,
      options,
      file = output)

    .Call("rs_recordData", output, list(classes = className,
                                        nrow = nRow, 
                                        ncol = nCol))
    invisible(x)
  }

  overrides <- .rs.dataCaptureOverrides()
  lapply(names(overrides), function(overrideName) {
    overrideMap <- overrides[[overrideName]]
    assign(
      overrideName,
      function(x, ...) {
        o <- overrideMap(x, options)
        overridePrint(o$x, o$options, o$className, o$nRow, o$nCol)
      },
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

  assign(
    "print.knitr_kable",
    function(x, ...) {
      print(
        knitr::asis_output(x)
      )

      invisible(x)
    },
    envir = as.environment("tools:rstudio")
  )
})

.rs.addFunction("releaseDataCapture", function()
{
  options(
    "dplyr.tibble.print" = get(
      "dplyr_tibble_print_original",
      envir = as.environment("tools:rstudio")
    )
  )

  overrides <- names(.rs.dataCaptureOverrides())
  rm(
    list = overrides,
    envir = as.environment("tools:rstudio")
  )
})

.rs.addFunction("readDataCapture", function(path)
{
  e <- new.env(parent = emptyenv())
  load(file = path, envir = e)

  data <- head(e$x, getOption("max.print", 1000))
  data <- if (is.null(data)) as.data.frame(list()) else data
  options <- e$options

  columnNames <- names(data)
  columnSequence <- seq_len(ncol(data))
  
  columns <- lapply(
    columnSequence,
    function(columnIdx) {
      column <- data[[columnIdx]]
      baseType <- class(column)[[1]]
      tibbleType <- tibble::type_sum(column)

      list(
        label = if (!is.null(columnNames)) columnNames[[columnIdx]] else "",
        name = columnIdx,
        type = tibbleType,
        align = if (baseType == "character" || baseType == "factor") "left" else "right"
      )
    }
  )

  names(data) <- as.character(columnSequence)

  addRowNames = isTRUE(options[["rownames.print"]])
  if (addRowNames) {
    columns <- c(
      list(
        list(
          label = "",
          name = "_rn_",
          type = "",
          align = "left"
        )
      ),
      columns
    )

    data$`_rn_` <- rownames(data)
  }

  columns <- unname(columns)

  is_list <- vapply(data, is.list, logical(1))
  data[is_list] <- lapply(data[is_list], function(x) {
    summary <- tibble::obj_sum(x)
    paste0("<", summary, ">")
  })

  if (length(columns) > 0) {
    first_column = data[[1]]
    if (is.numeric(first_column) && isTRUE(all(diff(first_column) == 1)))
      columns[[1]]$align <- "left"
  }

  data <- as.data.frame(
    lapply(
      data,
      function (y) {
        # escape NAs from character columns
        if (typeof(y) == "character") {
          y[y == "NA"] <- "__NA__"
        }

        y <- encodeString(format(y, digits = getOption("digits")))

        # trim spaces
        gsub("^\\s+|\\s+$", "", y)
      }
    ),
    stringsAsFactors = FALSE,
    optional = TRUE)

  pagedTableOptions <- list(
    columns = list(
      min = options[["cols.min.print"]],
      max = if (is.null(options[["cols.print"]])) 10 else options[["cols.print"]]
    ),
    rows = list(
      min = if (is.null(options[["rows.print"]])) 10 else options[["rows.print"]],
      max = if (is.null(options[["rows.print"]])) 10 else options[["rows.print"]]
    ),
    pages = options[["pages.print"]]
  )

  list(
    columns = columns,
    data = if (length(data) == 0) list() else data,
    options = pagedTableOptions
  )
})

.rs.addFunction("runSqlForDataCapture", function(sql, outputFile, options)
{
  is_sql_update_query <- function(query) {
    # remove line comments
    query <- gsub("^\\s*--.*\n", "", query)

    # remove multi-line comments
    if (grepl("^\\s*\\/\\*.*", query)) {
      query <- gsub(".*\\*\\/", "", query)
    }

    grepl("^\\s*(INSERT|UPDATE|DELETE|CREATE).*", query, ignore.case = TRUE)
  }

  # precreate directories if needed
  dir.create(dirname(outputFile), recursive = TRUE, showWarnings = FALSE)

  max.print <- if (is.null(options$max.print)) getOption("max.print", 1000) else as.numeric(options$max.print)
  max.print <- if (is.null(options$sql.max.print)) max.print else as.numeric(options$sql.max.print)

  if (is.null(options$connection)) stop(
    "The 'connection' option (DBI connection) is required for sql chunks."
  )

  conn <- get(options$connection, envir = globalenv())
  if (is.null(conn)) stop(
    "The 'connection' option must be a valid DBI connection."
  )

  # Return char vector of sql interpolation param names
  varnames_from_sql <- function(conn, sql) {
    varPos <- DBI::sqlParseVariables(conn, sql)
    if (length(varPos$start) > 0) {
      varNames <- substring(sql, varPos$start, varPos$end)
      sub("^\\?", "", varNames)
    }
  }

  # Vectorized version of exists
  mexists <- function(x, env = globalenv(), inherits = TRUE) {
    vapply(x, exists, logical(1), where = env, inherits = inherits)
  }

  # Interpolate a sql query based on the variables in an environment
  interpolate_from_env <- function(conn, sql, env = globalenv(), inherits = TRUE) {
    names <- unique(varnames_from_sql(conn, sql))
    names_missing <- names[!mexists(names, env, inherits)]
    if (length(names_missing) > 0) {
      stop("Object(s) not found: ", paste('"', names_missing, '"', collapse = ", "))
    }

    args <- if (length(names) > 0) setNames(
      mget(names, envir = env, inherits = inherits), names
    )

    do.call(DBI::sqlInterpolate, c(list(conn, sql), args))
  }

  # extract options
  varname <- options$output.var

  # execute query -- when we are printing with an enforced max.print we
  # use dbFetch so as to only pull down the required number of records
  query <- interpolate_from_env(conn, sql)
  if (is.null(varname) && max.print > 0 && !is_sql_update_query(query)) {
    res <- DBI::dbSendQuery(conn, query)
    data <- DBI::dbFetch(res, n = max.print)
    DBI::dbClearResult(res)
  } else {
    data <- DBI::dbGetQuery(conn, query)
  }

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

.rs.addJsonRpcHandler("default_sql_connection_name", function()
{
  dbiClassNames <- lapply(methods::.S4methods("dbGetQuery"), function(methodInfo) {
    methodInfoSplit <- strsplit(methodInfo, split = ",")[[1]]
    methodInfoSplit[[2]]
  })

  dbiObjectNames <- Filter(function(objName) {
    any(class(get(objName, envir = globalenv())) %in% dbiClassNames)
  }, ls(envir = globalenv()))

  if (length(dbiObjectNames) > 0) .rs.scalar(dbiObjectNames[[1]]) else null
})
