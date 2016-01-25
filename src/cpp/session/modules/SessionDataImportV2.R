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

   if (is.null(dataName) || identical(dataName, ""))
      {
         if (!is.null(dataImportOptions$importLocation))
         {
            locationName <- basename(dataImportOptions$importLocation)
            if (length(locationName) > 0)
            {
               fileComponents <- unlist(strsplit(locationName, ".", fixed = TRUE))
               components <- length(fileComponents)
               if (components >= 1)
               {
                  dataName <- paste(fileComponents[1:(components-1)], collapse = "_")
               }
            }
         }
      }
   
   dataName
})

.rs.addFunction("assemble_data_import_parameters", function(options, optionTypes, importFunction, dataImportOptions)
{ 
   buildParameter <- function(optionType, optionValue)
   {
      if (identical(optionType, NULL)) {
         return (optionValue)
      }

      switch(optionType,
         "character" = {
            return (paste("\"", optionValue, "\"", sep = ""))
         },
         "locale" = {
            return (paste("readr::locale(date_names=\"", optionValue, "\")", sep = ""))
         },
         "columnDefinitions" = {
            colParams <- ""
            for(colIdx in seq_along(optionValue)) {
               col <- optionValue[[colIdx]]

               colType <- switch(col$assignedType,
                  date = "readr::col_date()",
                  skip = "readr::col_skip()",
                  time = "readr::col_time()",
                  double = "readr::col_double()",
                  factor = "readr::col_factor()",
                  numeric = "readr::col_numeric()",
                  integer = "readr::col_integer()",
                  logical = "readr::col_logical()",
                  numeric = "readr::col_numeric()",
                  datetime = "readr::col_datetime()",
                  character = "readr::col_character()",
                  euroDouble = "readr::col_euro_double()",
                  "readr::col_guess()"
               )

               colParams[[colIdx]] <- paste("\"", col$name, "\" = ", colType, sep="")
            }
            colParam <- (paste(colParams, collapse = ",\n      ", sep = ""))

            colsConstructor <- "cols";
            if (!identical(dataImportOptions$columnsOnly, NULL)) {
               colsConstructor <- "cols_only"
            }

            return (paste("readr::", colsConstructor, "(\n", colParam, ")", sep = ""))
         }, {
            return (optionValue)
         })
   }

   # load function definition
   parameterDefinitions <- formals(importFunction)
   parameterNames <- names(parameterDefinitions)

   # remove unused parameters
   options <- Filter(Negate(function(x) is.null(unlist(x))), options)

   # remove default and unused parameters
   optionsNoDefaults <- list()
   for (optionName in parameterNames) {
      if (!identical(NULL, options[[optionName]]) &&
            !identical(options[[optionName]], parameterDefinitions[[optionName]])) {
         optionsNoDefaults[[optionName]] <- options[[optionName]]
      }
   }

   # build parameter string
   parameters <- list()
   for (parameterName in parameterNames) {
      if (identical("symbol", typeof(parameterDefinitions[[parameterName]]))) {
         if (identical(NULL, optionsNoDefaults[[parameterName]])) {
            parameters[[parameterName]] <- "NULL"
         } else {
            parameters[[parameterName]] <- buildParameter(optionTypes[[parameterName]], optionsNoDefaults[[parameterName]])
         }
      } else if (!identical(NULL, optionsNoDefaults[[parameterName]])) {
         parameters[[parameterName]] <- paste(
            parameterName,
            buildParameter(optionTypes[[parameterName]], optionsNoDefaults[[parameterName]]),
            sep = " = ")
      }
   }

   paste(parameters, collapse = ",")
})

.rs.addFunction("assemble_data_import", function(dataImportOptions)
{
   importInfo <- list()

   dataName <- importInfo$dataName <- .rs.assemble_data_import_name(dataImportOptions)
   if (is.null(dataName) || identical(dataName, ""))
   {
      dataName <- "dataset"
   }

   dataName <- tolower(gsub("[\\._]+", "_", c(make.names(dataName)), perl=TRUE))

   functionName <- list()
   functionReference <- readr::read_delim
   if (is.null(dataImportOptions$delimiter) || identical(dataImportOptions$delimiter, ","))
   {
      functionName <- "read_csv"
      functionReference <- readr::read_csv
   }
   else
   {
      functionName <- "read_delim"
   }

   # load parameters
   options <- list()
   options[["file"]] <- dataImportOptions$importLocation
   options[["delim"]] <- dataImportOptions$delimiter
   options[["quote"]] <- dataImportOptions$quotes
   options[["escape_backslash"]] <- dataImportOptions$escapeBackslash
   options[["escape_double"]] <- dataImportOptions$escapeDouble
   options[["col_names"]] <- dataImportOptions$columnNames
   options[["trim_ws"]] <- dataImportOptions$trimSpaces
   options[["locale"]] <- dataImportOptions$locale
   options[["na"]] <- dataImportOptions$na
   options[["comment"]] <- dataImportOptions$comments
   options[["skip"]] <- dataImportOptions$skip
   options[["n_max"]] <- dataImportOptions$maxRows
   options[["col_types"]] <- dataImportOptions$columnDefinitions

   # set special parameter types
   optionTypes <- list()
   optionTypes[["file"]] <- "character"
   optionTypes[["delim"]] <- "character"
   optionTypes[["quote"]] <- "character"
   optionTypes[["locale"]] <- "locale"
   optionTypes[["na"]] <- "character"
   optionTypes[["comment"]] <- "character"
   optionTypes[["col_types"]] <- "columnDefinitions"

   functionParameters <- .rs.assemble_data_import_parameters(options, optionTypes, functionReference, dataImportOptions)

   importInfo$previewCode <- paste(
      "readr::",
      functionName,
      "(",
      functionParameters,
      ")",
      sep = "")

   importInfo$importCode <- paste(
      lapply(
         c(
            paste(dataName, " <- ", importInfo$previewCode, sep = ""),
            paste("View(", dataName, ")", sep = "")
         ),
         function(e) {
            paste(
               deparse(
                  eval(
                     parse(
                        text = paste("quote(", e, ")", sep = "")
                     )
                  ),
                  width.cutoff = 40
               ),
               collapse = "\n"
            )
         }
      ),
      collapse = "\n"
   )

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

      # while previewing data, always return a column even if it will be skipped
      if (!identical(dataImportOptions$columnDefinitions, NULL))
      {
         dataImportOptions$columnDefinitions <- Filter(function (e) {
               !(identical(e$assignedType, "skip") || identical(e$assignedType, "only"))
            },
            dataImportOptions$columnDefinitions
         )
      }

      importInfo <- .rs.assemble_data_import(dataImportOptions)
      data <- eval(parse(text=importInfo$previewCode))
      columns <- .rs.describeCols(data, maxCols, maxFactors)

      cnames <- names(data)
      size <- nrow(data)

      for(i in seq_along(data)) {
         data[[i]] <- .rs.formatDataColumn(data[[i]], 1, size)
      }

      return(list(data = unname(data),
                  columns = columns))
   }, error = function(e) {
      return(list(error = e))
   })
})
