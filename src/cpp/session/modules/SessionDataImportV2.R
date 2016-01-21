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

.rs.addFunction("assemble_data_import_parameters", function(options, optionTypes, importFunction)
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

               colType <- switch(col$type,
                  logical = "readr::col_logical()",
                  integer = "readr::col_integer()",
                  double = "readr::col_double()",
                  character = "readr::col_character()",
                  date = "readr::col_date()",
                  datetime = "readr::col_datetime()",
                  numeric = "readr::col_number()"
               )

               colParams[[colIdx]] <- paste(col$name, " = ", colType, sep="")
            }
            colParam <- (paste(colParams, collapse = ",\n      ", sep = ""))
            return (paste("readr::cols(\n      ", colParam, ")", sep = ""))
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

   spacing <- ",\n   "
   paste(parameters, collapse = spacing)
})

.rs.addFunction("assemble_data_import", function(dataImportOptions)
{
   importInfo <- list()

   dataName <- importInfo$dataName <- .rs.assemble_data_import_name(dataImportOptions)
   if (is.null(dataName) || identical(dataName, ""))
   {
      dataName <- "dataset"
   }

   dataName <- gsub("[\\._]+", "_", c(make.names(dataName)), perl=TRUE)

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

   functionParameters <- .rs.assemble_data_import_parameters(options, optionTypes, functionReference)

   importInfo$previewCode <- paste(
      "readr::",
      functionName,
      "(",
      functionParameters,
      ")",
      sep = "")

   importInfo$importCode <- paste(dataName, " <- ", importInfo$previewCode, "\n", "View(", dataName, ")", sep = "")

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

      for(i in seq_along(data)) {
         data[[i]] <- .rs.formatDataColumn(data[[i]], 1, size)
      }

      return(list(data = data,
                        columns = columns))
   }, error = function(e) {
      return(list(error = e))
   })
})
