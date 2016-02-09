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

.rs.addFunction("assemble_data_import_parameters", function(options, optionTypes, importFunction, dataImportOptions, package)
{
   ns <- ""
   if (!identical(package, NULL))
   {
      ns <- paste(package, "::", sep = "")
   }

   buildParameter <- function(optionType, optionValue)
   {
      if (identical(optionType, NULL)) {
         return (optionValue)
      }

      switch(optionType,
         "character" = {
            optionValue <- gsub("\\", "\\\\", optionValue, fixed = TRUE)
            optionValue <- gsub("\"", "\\\"", optionValue, fixed = TRUE)
            return (paste("\"", optionValue, "\"", sep = ""))
         },
         "locale" = {
            return (paste(ns, "locale(encoding=\"", optionValue, "\")", sep = ""))
         },
         "columnDefinitionsReadR" = {
            colParams <- c()
            for(colIdx in seq_along(optionValue)) {
               col <- optionValue[[colIdx]]

               if ((!identical(dataImportOptions$columnsOnly, TRUE) && !identical(col$assignedType, NULL)) || 
                  identical(col$only, TRUE))
               {
                  colType <- paste(ns, "col_guess()", sep = "")

                  parseString <- "";
                  if (!identical(col$parseString, NULL))
                  {
                     if (identical(col$assignedType, "factor"))
                     {
                        parseString <- paste("levels = ", col$parseString, sep = "")
                     }
                     else
                     {
                        parseString <- paste("format = \"", col$parseString, "\"", sep = "")
                     }
                  }

                  if (!identical(col$assignedType, NULL))
                  {
                     colType <- switch(col$assignedType,
                        date = paste(ns, "col_date(", parseString, ")", sep = ""),
                        skip = paste(ns, "col_skip()", sep = ""),
                        time = paste(ns, "col_time(", parseString, ")", sep = ""),
                        double = paste(ns, "col_double()", sep = ""),
                        factor = paste(ns, "col_factor(", parseString, ")", sep = ""),
                        numeric = paste(ns, "col_numeric()", sep = ""),
                        integer = paste(ns, "col_integer()", sep = ""),
                        logical = paste(ns, "col_logical()", sep = ""),
                        dateTime = paste(ns, "col_datetime(", parseString, ")", sep = ""),
                        character = paste(ns, "col_character()", sep = ""),
                        euroDouble = paste(ns, "col_euro_double()", sep = "")
                     )
                  }

                  colParams[[col$name]] <- paste("\"", col$name, "\" = ", colType, sep="")
               }
            }

            if (length(colParams) == 0)
               return (NULL)

            colParam <- (paste(colParams, collapse = ",", sep = ""))

            colsConstructor <- "cols";
            if (identical(dataImportOptions$columnsOnly, TRUE)) {
               colsConstructor <- "cols_only"
            }

            return (paste(ns, colsConstructor, "(\n", colParam, ")", sep = ""))
         },
         "columnDefinitionsReadXl" = {
            colParams <- c()

            hasAssignedType <- function(x) {
               !identical(x$assignedType, NULL)
            }

            if (!any(unlist(lapply(optionValue, hasAssignedType), use.names = FALSE)))
               return (NULL)

            colsByIndex <- list()
            for(col in optionValue) {
               colsByIndex[[col$index + 1]] <- col
            }

            for(colIdx in seq(from=1, to=length(optionValue) - 1)) {
               col <- colsByIndex[[colIdx + 1]]

               colParams[[colIdx]] <- "\"text\""
               if (!identical(col$assignedType, NULL))
               {
                  colParams[[colIdx]] <- switch(col$assignedType,
                     numeric = "\"numeric\"",
                     date = "\"date\"",
                     character = "\"text\"",
                     skip = "\"blank\""
                  )
               }
            }

            if (length(colParams) == 0)
               return (NULL)

            colParam <- (paste(colParams, collapse = ",", sep = ""))

            return (paste(ns, "c", "(\n", colParam, ")", sep = ""))
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
         assembledParameter <- buildParameter(optionTypes[[parameterName]], optionsNoDefaults[[parameterName]])
         if (!identical(assembledParameter, NULL)) {
            parameters[[parameterName]] <- paste(
               parameterName,
               assembledParameter,
               sep = " = ")
         }
      }
   }

   # remove empty parameters
   parameters <- Filter(Negate(function(x) is.null(unlist(x))), parameters)

   paste(parameters, collapse = ",")
})

.rs.addFunction("assemble_data_import", function(dataImportOptions)
{
   importInfo <- list()

   functionInfoFromOptions <- list(
      "text" = function() {
         functionName <- ""
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
         optionTypes[["col_types"]] <- "columnDefinitionsReadR"

         return(list(
            name = functionName,
            reference = functionReference,
            package = "readr",
            paramsPackage = "readr",
            options = options,
            optionTypes = optionTypes
         ))
      },
      "statistics" = function() {
         # load parameters
         options <- list()
         options[["path"]] <- dataImportOptions$importLocation
         options[["b7dat"]] <- dataImportOptions$importLocation
         options[["b7cat"]] <- dataImportOptions$modelLocation

         havenFunction <- switch(dataImportOptions$format,
            "sav" = list(name = "read_sav", ref = haven::read_sav),
            "dta" = list(name = "read_dta", ref = haven::read_dta),
            "por" = list(name = "read_por", ref = haven::read_por),
            "sas" = list(name = "read_sas", ref = haven::read_sas),
            "stata" = list(name = "read_stata", ref = haven::read_stata)
         )

         # set special parameter types
         optionTypes <- list()
         optionTypes[["path"]] <- "character"
         optionTypes[["b7dat"]] <- "character"
         optionTypes[["b7cat"]] <- "character"

         return(list(
            name = havenFunction$name,
            reference = havenFunction$ref,
            package = "haven",
            options = options,
            optionTypes = optionTypes
         ))
      },
      "xls" = function() {
         # load parameters
         options <- list()
         options[["path"]] <- dataImportOptions$importLocation
         options[["sheet"]] <- dataImportOptions$sheet
         options[["na"]] <- dataImportOptions$na
         options[["col_names"]] <- dataImportOptions$columnNames
         options[["skip"]] <- dataImportOptions$skip
         options[["col_types"]] <- dataImportOptions$columnDefinitions

         # set special parameter types
         optionTypes <- list()
         optionTypes[["path"]] <- "character"
         optionTypes[["sheet"]] <- "character"
         optionTypes[["na"]] <- "character"
         optionTypes[["col_types"]] <- "columnDefinitionsReadXl"

         return(list(
            name = "read_excel",
            reference = readxl::read_excel,
            package = "readxl",
            paramsPackage = NULL,
            options = options,
            optionTypes = optionTypes
         ))
      }
   )

   dataName <- importInfo$dataName <- .rs.assemble_data_import_name(dataImportOptions)
   if (is.null(dataName) || identical(dataName, ""))
   {
      dataName <- "dataset"
   }

   dataName <- tolower(gsub("[\\._]+", "_", c(make.names(dataName)), perl=TRUE))

   functionInfo <- functionInfoFromOptions[[dataImportOptions$mode]]()
   options <- functionInfo$options
   optionTypes <- functionInfo$optionTypes

   functionParameters <- .rs.assemble_data_import_parameters(options, optionTypes, functionInfo$reference, dataImportOptions, functionInfo$paramsPackage)
   functionParametersNoNs <- .rs.assemble_data_import_parameters(options, optionTypes, functionInfo$reference, dataImportOptions, NULL)

   previewCode <- paste(
      functionInfo$package,
      "::",
      functionInfo$name,
      "(",
      functionParameters,
      ")",
      sep = "")
   previewCodeNoNs <- paste(
      functionInfo$name,
      "(",
      functionParametersNoNs,
      ")",
      sep = "")

   importInfo$previewCode <- paste(
      previewCode,
      sep = "")

   importInfo$importCode <- paste(
      lapply(
         c(
            paste(
               "library(",
               functionInfo$package,
               ")",
               sep = ""
            ),
            paste(dataName, " <- ", previewCodeNoNs, sep = ""),
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
      beforeImportFromOptions <- list(
         "text" = function() {
            # while previewing data, always return a column even if it will be skipped
            dataImportOptions$columnsOnly <<- FALSE;
            if (!identical(dataImportOptions$columnDefinitions, NULL))
            {
               dataImportOptions$columnDefinitions <<- Filter(function (e) {
                     !(identical(e$assignedType, "skip") || identical(e$assignedType, "only"))
                  },
                  dataImportOptions$columnDefinitions
               )
            }
         },
         "xls" = function() {
            # while previewing data, always return a column even if it will be skipped
            if (!identical(dataImportOptions$columnDefinitions, NULL))
            {
               dataImportOptions$columnDefinitions <<- lapply(
                  dataImportOptions$columnDefinitions,
                  function (e) {
                     if (identical(e$assignedType, "skip")) {
                        e$assignedType <- "character"
                     }
                     e
                  }
               )
            }
         }
      )

      optionsInfoFromOptions <- list(
         "text" = function() {
            return(list(
               columnTypes = c("guess",
                               "date",
                               "time",
                               "double",
                               "factor",
                               "numeric",
                               "integer",
                               "logical",
                               "dateTime",
                               "character",
                               "include",
                               "only",
                               "skip")
            ))
         },
         "statistics" = function() {
            return(list(
               columnTypes = list()
            ))
         },
         "xls" = function() {
            sheets <- list()
            if (!identical(sheets, NULL)) {
               sheets <- tryCatch({
                  readxl::excel_sheets(path = dataImportOptions$importLocation)
               }, error = function(e) {
                  list()
               })
            }

            return(list(
               sheets = sheets,
               columnTypes = c("numeric",
                               "date",
                               "character",
                               "include",
                               "skip")
            ))
         }
      )

      if (dataImportOptions$mode %in% names(beforeImportFromOptions))
      {
         beforeImportFromOptions[[dataImportOptions$mode]]()
      }

      importInfo <- .rs.assemble_data_import(dataImportOptions)
      data <- eval(parse(text=importInfo$previewCode))

      columns <- list()
      if (ncol(data)) {
         columns <- .rs.describeCols(data, maxCols, maxFactors)
      }
      
      parsingErrors <-length(readr::problems(data)$row)

      cnames <- names(data)
      size <- nrow(data)

      for(i in seq_along(data)) {
         data[[i]] <- .rs.formatDataColumn(data[[i]], 1, size)
      }

      options <- optionsInfoFromOptions[[dataImportOptions$mode]]()

      return(list(data = unname(data),
                  columns = columns,
                  options = options,
                  parsingErrors = parsingErrors))
   }, error = function(e) {
      return(list(error = e))
   })
})
