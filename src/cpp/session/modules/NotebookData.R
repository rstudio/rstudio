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
  defaultOverride <- function(x, options) list(x = x, options = options, className = class(x), nRow = .rs.scalar(nrow(x)), nCol = .rs.scalar(ncol(x)))
  c(
    "print.data.frame" = defaultOverride,
    "print.tbl_df"     = defaultOverride,
    "print.paged_df"   = defaultOverride,
    "print.grouped_df" = defaultOverride,
    "print.rowwise_df" = defaultOverride,
    "print.tbl_sql"    = defaultOverride,
    "print.data.table" = function(x, options) {
      shouldPrintTable <- TRUE

      if ("data.table" %in% loadedNamespaces() &&
          exists("shouldPrint", envir = asNamespace("data.table")))
      {
        shouldPrint <- get("shouldPrint", envir = asNamespace("data.table"))
        shouldPrintTable <- tryCatch(
          shouldPrint(x) || !inherits(x, "data.table"),
          error = function(e) TRUE
        )
      }

      if (!shouldPrintTable) {
        return(NULL)
      }

      list(x = x, options = options, className = class(x), nRow = .rs.scalar(nrow(x)), nCol = .rs.scalar(ncol(x)))
    },
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
  pagedOption <- if (!is.null(options[["paged.print"]])) options[["paged.print"]] else getOption("paged.print")
  if (identical(pagedOption, FALSE)) {
    return()
  }

  overridePrint <- function(x, options, className, nRow, nCol) {
    original <- x
    options <- if (is.null(options)) list() else options
    
    optionRowNames <- options[["rownames.print"]]
    options[["rownames.print"]] <- if (is.null(optionRowNames)) (.row_names_info(x, type = 1) > 0) else optionRowNames
    options[["rows.total"]] <- nrow(x)
    options[["cols.total"]] <- ncol(x)

    output <- tempfile(pattern = "_rs_rdf_", tmpdir = outputFolder, 
                       fileext = ".rdf")

    max.print <- if (is.null(options$max.print)) getOption("max.print", 1000) else options$max.print

    cols.max.print <- if (is.null(options$cols.max.print)) getOption("cols.max.print", 1000) else options$cols.max.print
    if (NCOL(x) > cols.max.print) {
      x <- x[,c(1:cols.max.print)]
    }

    x <- as.data.frame(head(x, max.print))

    save(
      x,
      options,
      file = output)

    .Call("rs_recordData", output, list(classes = className,
                                        nrow = nRow, 
                                        ncol = nCol))
    invisible(original)
  }

  overrides <- .rs.dataCaptureOverrides()
  lapply(names(overrides), function(overrideName) {
    overrideMap <- overrides[[overrideName]]
    assign(
      overrideName,
      function(x, ...) {
        o <- overrideMap(x, options)
        if (!is.null(o)) {
          overridePrint(o$x, o$options, o$className, o$nRow, o$nCol)
        }
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
  if (!is.null(getOption("dplyr_tibble_print_original"))) {
    options(
      "dplyr.tibble.print" = get(
        "dplyr_tibble_print_original",
        envir = as.environment("tools:rstudio")
      )
    )
  }

  overrides <- .rs.dataCaptureOverrides()
  lapply(names(overrides), function(override) {
    if (exists(override, envir = as.environment("tools:rstudio"), inherits = FALSE)) {
      rm(
        list = override,
        envir = as.environment("tools:rstudio"),
        inherits = FALSE
      )
    }
  })
})

.rs.addFunction("readDataCapture", function(path)
{
  type_sum <- function(x) {
    format_sum <- switch (class(x)[[1]],
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

  "%||%" <- function(x, y) {
    if(is.null(x)) y else x
  }

  big_mark <- function(x, ...) {
    mark <- if (identical(getOption("OutDec"), ",")) "." else ","
    formatC(x, big.mark = mark, ...)
  }

  dim_desc <- function(x) {
    dim <- dim(x) %||% length(x)
    format_dim <- vapply(dim, big_mark, character(1))
    format_dim[is.na(dim)] <- "??"
    paste0(format_dim, collapse = " \u00d7 ")
  }

  is_atomic <- function(x) {
    is.atomic(x) && !is.null(x)
  }

  is_vector <- function(x) {
    is_atomic(x) || is.list(x)
  }

  paged_table_is_vector_s3 <- function(x) {
    switch(class(x)[[1]],
      ordered = TRUE,
      factor = TRUE,
      Date = TRUE,
      POSIXct = TRUE,
      difftime = TRUE,
      data.frame = TRUE,
      !is.object(x) && is_vector(x))
  }

  size_sum <- function(x) {
    if (!paged_table_is_vector_s3(x)) return("")

    paste0(" [", dim_desc(x), "]" )
  }

  obj_sum <- function(x) {
    switch(class(x)[[1]],
      POSIXlt = rep("POSIXlt", length(x)),
      list = vapply(x, obj_sum, character(1L)),
      paste0(type_sum(x), size_sum(x))
    )
  }

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
      tibbleType <- type_sum(column)

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
        summary <- obj_sum(x)
        paste0("<", summary, ">")
      })

  data <- as.data.frame(
    lapply(
      data,
      function (y) {
        # escape NAs from character columns
        if (typeof(y) == "character") {
          y[y == "NA"] <- "__NA__"
        }

        y <- encodeString(format(y))

        # trim spaces
        gsub("^\\s+|\\s+$", "", y)
      }
    ),
    stringsAsFactors = FALSE,
    optional = TRUE)

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

  conn <- options$connection
  
  if (is.numeric(options$connection)) {
    chunkReferences <- get(".rs.knitr.chunkReferences", envir = .rs.toolsEnv())
    conn <- chunkReferences[[chunkOptions$connection]]
  }

  if (is.null(conn)) {
    stop("The 'connection' option (DBI connection) is required for sql chunks.")
  }

  if (is.character(options$connection)) {
    conn <- get(options$connection, envir = globalenv())
    if (is.null(conn)) stop(
      "The 'connection' option must be a valid DBI connection."
    )
  }

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

  if (length(dbiObjectNames) > 0) .rs.scalar(dbiObjectNames[[1]]) else NULL
})
