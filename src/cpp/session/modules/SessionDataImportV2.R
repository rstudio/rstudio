#
# SessionDataImportV2.R
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

.rs.addFunction("assemble_data_import_name", function(dataImportOptions)
{
  dataName <- dataImportOptions$dataName

  if (is.null(dataName) || dataName == "")
    {
      if (!is.null(dataImportOptions$importLocation) && dataImportOptions$importLocation != "")
      {
        basename <- basename(dataImportOptions$importLocation)
        if (length(basename) > 0)
        {
          fileComponents <- unlist(strsplit(basename, ".", fixed = TRUE))
          components <- length(fileComponents)
          if (components >= 1)
          {
            dataName <- paste(fileComponents[1:components-1])
          }
        }
      }
    }
  
  dataName
})

.rs.addFunction("assemble_data_import_parameters_csv", function(dataImportOptions)
{
  parameters <- ""
  spacing <- ",\n  "
  useCsv <- (!is.null(dataImportOptions$delimiter) && dataImportOptions$delimiter == ",")

  if (!is.null(dataImportOptions$delimiter) && dataImportOptions$delimiter != ",")
  {
    parameters <- paste0(parameters, spacing, "delim = \"", dataImportOptions$delimiter, "\"")
  }

  if (!is.null(dataImportOptions$quotes) && dataImportOptions$quotes != "" && !useCsv)
  {
    parameters <- paste0(parameters, spacing, "quote = \"", dataImportOptions$quotes, "\"")
  }

  if (!is.null(dataImportOptions$escapeBackslash) && !useCsv)
  {
    parameters <- paste0(parameters, spacing, "escape_backslash = ", dataImportOptions$escapeBackslash)
  }

  if (!is.null(dataImportOptions$escapeDouble) && !useCsv)
  {
    parameters <- paste0(parameters, spacing, "escape_double = ", dataImportOptions$escapeDouble)
  }

  if (!is.null(dataImportOptions$columnNames))
  {
    parameters <- paste0(parameters, spacing, "col_names = ", dataImportOptions$columnNames)
  }

  if (!is.null(dataImportOptions$trimSpaces) && useCsv)
  {
    parameters <- paste0(parameters, spacing, "trim_ws = ", dataImportOptions$trimSpaces)
  }

  if (!is.null(dataImportOptions$locale) && dataImportOptions$locale != "")
  {
    parameters <- paste0(parameters, spacing, "locale = readr::locale(date_names=\"", dataImportOptions$locale, "\")")
  }

  if (!is.null(dataImportOptions$na) && dataImportOptions$na != "")
  {
    parameters <- paste0(parameters, spacing, "na = \"", dataImportOptions$na, "\"")
  }

  if (!is.null(dataImportOptions$comments) && dataImportOptions$comments != "")
  {
    parameters <- paste0(parameters, spacing, "comments = \"", dataImportOptions$comments, "\"")
  }

  if (!is.null(dataImportOptions$skip))
  {
    parameters <- paste0(parameters, spacing, "skip = ", dataImportOptions$skip)
  }

  if (!is.null(dataImportOptions$maxRows))
  {
    parameters <- paste0(parameters, spacing, "n_max = ", dataImportOptions$maxRows)
  }

  parameters
})

.rs.addFunction("assemble_data_import", function(dataImportOptions)
{
  importInfo <- list()

  dataName <- importInfo$dataName <- .rs.assemble_data_import_name(dataImportOptions)
  if (is.null(dataName) || dataName == "")
  {
    dataName <- "dataset"
  }

  if (!grepl("^[a-zA-Z]+[a-zA-Z0-9_]*$", dataName) || any(dataName == ls('package:base')))
  {
    dataName <- paste0("`", dataName, "`")
  }

  functionName <- list()
  if (is.null(dataImportOptions$delimiter) || dataImportOptions$delimiter == ",")
  {
    functionName <- "read_csv"
  }
  else
  {
    functionName <- "read_delim"
  }

  functionParameters <- .rs.assemble_data_import_parameters_csv(dataImportOptions)

  importInfo$previewCode <- paste0(
    "readr::",
    functionName,
    "(\"",
    dataImportOptions$importLocation,
    "\"",
    functionParameters,
    ")")

  importInfo$importCode <- paste0(dataName, " <- ", importInfo$previewCode, "\n", "View(", dataName, ")")

  importInfo
})

.rs.addJsonRpcHandler("assemble_data_import", function(dataImportOptions)
{
  tryCatch({
    return (.rs.assemble_data_import(dataImportOptions))
  }, error = function(e) {
    return(list(error = e))
  })
})

.rs.addJsonRpcHandler("preview_data_import", function(dataImportOptions, maxCols = 100, maxFactors = 64)
{
  tryCatch({

    importInfo <- .rs.assemble_data_import(dataImportOptions)
    data <- eval(parse(text=importInfo$previewCode))
    columns <- .rs.describeCols(data, maxCols, maxFactors)

    cnames <- names(data)
    size <- nrow(data)

    for(cname in cnames) {
      data[[cname]] <- .rs.formatDataColumn(data[[cname]], 1, size)
    }

    return(list(data = data,
                columns = columns))
  }, error = function(e) {
    return(list(error = e))
  })
})
