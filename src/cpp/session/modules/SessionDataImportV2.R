#
# SessionDataImportV2.R
#
# Copyright (C) 2009-17 by RStudio, Inc.
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

.rs.addFunction("pathRelativeToWorkingDir", function(target)
{
   if (is.null(target))
   {
      target
   }
   else
   {
      currentPath <- paste(getwd(), .Platform$file.sep, sep = "")
      fullPath <- path.expand(target)
      if (identical(substr(fullPath, 0, nchar(currentPath)), currentPath))
      {
         substr(fullPath, nchar(currentPath) + 1, nchar(fullPath))
      }
      else
      {
         target
      }
   }
})

.rs.addFunction("assembleDataImportName", function(dataImportOptions)
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

.rs.addFunction("assembleDataImportParameters", function(params)
{
   options <- params$options
   optionTypes <- params$optionTypes
   importFunction <- params$importFunction
   dataImportOptions <- params$dataImportOptions
   package <- params$package

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
            localeDefaults <- formals(readr::locale)

            localeOrNull <- function(paramName, jsonName) {
               if (!identical(localeDefaults[[paramName]], optionValue[[jsonName]])) {
                  if (typeof(localeDefaults[[paramName]]) == "character") {
                     paste(paramName, " = \"", optionValue[[jsonName]], "\"", sep = "")
                  }
                  else {
                     paste(paramName, " = ", optionValue[[jsonName]])
                  }
               }
               else NULL
            }

            return (paste(
               ns, 
               "locale(",
               paste(c(
                  localeOrNull("date_names", "dateName"),
                  localeOrNull("date_format", "dateFormat"),
                  localeOrNull("time_format", "timeFormat"),
                  localeOrNull("decimal_mark", "decimalMark"),
                  localeOrNull("grouping_mark", "groupingMark"),
                  localeOrNull("tz", "tz"),
                  localeOrNull("encoding", "encoding"),
                  localeOrNull("asciify", "asciify")
               ), collapse = ", "),
               ")", sep = ""))
         },
         "columnDefinitionsReadR" = {
            colParams <- c()
            for(colIdx in seq_along(optionValue)) {
               col <- optionValue[[colIdx]]

               col_only <- col$only[[1]]
               col_parseString <- col$parseString[[1]]
               col_assignedType <- col$assignedType[[1]]
               col_name <- col$name[[1]]

               if ((!identical(dataImportOptions$columnsOnly, TRUE) && !identical(col_assignedType, NULL)) || 
                  identical(col_only, TRUE))
               {
                  colType <- paste(ns, "col_guess()", sep = "")

                  parseString <- "";
                  if (!identical(col_parseString, NULL))
                  {
                     if (identical(col_assignedType, "factor"))
                     {
                        parseString <- paste("levels = ", col_parseString, sep = "")
                     }
                     else
                     {
                        parseString <- paste("format = \"", col_parseString, "\"", sep = "")
                     }
                  }

                  if (!identical(col_assignedType, NULL))
                  {
                     colType <- switch(col_assignedType,
                        date = paste(ns, "col_date(", parseString, ")", sep = ""),
                        skip = paste(ns, "col_skip()", sep = ""),
                        time = paste(ns, "col_time(", parseString, ")", sep = ""),
                        double = paste(ns, "col_double()", sep = ""),
                        factor = paste(ns, "col_factor(", parseString, ")", sep = ""),
                        numeric = paste(ns, "col_number()", sep = ""),
                        integer = paste(ns, "col_integer()", sep = ""),
                        logical = paste(ns, "col_logical()", sep = ""),
                        dateTime = paste(ns, "col_datetime(", parseString, ")", sep = ""),
                        character = paste(ns, "col_character()", sep = ""),
                        euroDouble = paste(ns, "col_euro_double()", sep = "")
                     )
                  }

                  colParams[[col_name]] <- paste("\"", col_name, "\" = ", colType, sep="")
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
            for (colIdx in seq_along(optionValue)) {
               col <- optionValue[[colIdx]]
               colsByIndex[[col$index + 1]] <- col
            }

            for(colIdx in seq(from=1, to=length(optionValue) - 1)) {
               col <- colsByIndex[[colIdx + 1]]

               if (!identical(col$rType, NULL)) {
                  colParams[[colIdx]] <- switch(col$rType,
                     "date" = "date",
                     "time" = "date",
                     "double" = "numeric",
                     "factor" = "character",
                     "numeric" = "numeric",
                     "integer" = "numeric",
                     "logical" = "numeric",
                     "dateTime" = "date",
                     "character" = "text",
                     "\"text\""
                  )
                  colParams[[colIdx]] <- paste("\"", colParams[[colIdx]], "\"", sep = "")
               }

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
         if (is.numeric(options[[optionName]]) && is.numeric(parameterDefinitions[[optionName]])) {
            if (!isTRUE(all.equal(options[[optionName]], parameterDefinitions[[optionName]]))) {
               optionsNoDefaults[[optionName]] <- options[[optionName]]
            }
         }
         else {
            optionsNoDefaults[[optionName]] <- options[[optionName]]
         }
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

.rs.addFunction("assembleDataImport", function(dataImportOptions)
{
   dataImportOptions$importLocation <- .rs.pathRelativeToWorkingDir(dataImportOptions$importLocation)
   dataImportOptions$modelLocation <- .rs.pathRelativeToWorkingDir(dataImportOptions$modelLocation)

   importInfo <- list()

   pathIsUrl <- function(path) {
      if (identical(path, NULL)) FALSE else grepl("://", path)
   }

   cacheOrFileFromOptions <- function(options, resource = "importLocation") {
      canCacheData <- identical(options$canCacheData, TRUE)

      if (!canCacheData || !pathIsUrl(options[[resource]]) ||
          identical(options$cacheVariableNames[[resource]], NULL))
         dataImportOptions[[resource]]
      else
         options$cacheVariableNames[[resource]]
   }

   cacheTypeOrFileTypeFromOptions <- function(options, resource = "importLocation") {
      canCacheData <- identical(options$canCacheData, TRUE)

      if (!canCacheData || !pathIsUrl(options[[resource]]) ||
          identical(options$cacheVariableNames[[resource]], NULL))
         "character"
      else
         "symbol"
   }

   cacheCodeFromOptions <- function(options, functionInfo, resource) {
      importFromUrl <- pathIsUrl(options[[resource]])
      cacheDataCode <- list()

      localFile <- unlist(options$localFiles[[resource]])
      canCacheData <- identical(options$canCacheData, TRUE)
      cacheDataWorkingDir <- !identical(options$cacheDataWorkingDir, NULL) && options$cacheDataWorkingDir == TRUE

      if (importFromUrl && canCacheData)
      {
         cacheVariableName <- options$cacheVariableNames[[resource]]
         cacheUrlName <- options$cacheUrlNames[[resource]]
         downloadResource <- options[[resource]]
         resourceExtension <- sub("[^\\?]*([.][^.\\?]*)(\\?.*)?$", "\\1", basename(downloadResource), perl = TRUE)
         if (!(resourceExtension %in% functionInfo$cacheFileExtension))
            resourceExtension <- functionInfo$cacheFileExtension[[1]]

         cacheDataCode <- append(
            cacheDataCode,
            paste(
               cacheUrlName,
               " <- \"",
               downloadResource,
               "\"",
               sep = "")
         )

         if (cacheDataWorkingDir)
         {
            cacheDataCode <- append(
               cacheDataCode,
               paste(
                  cacheVariableName,
                  " <- \"",
                  options$dataName,
                  resourceExtension,
                  "\"",
                  sep = "")
            )
         }
         else
         {
            if (identical(localFile, NULL))
            {
               localFile <- normalizePath(tempfile(
                  tmpdir = dirname(tempdir()),
                  fileext = resourceExtension
               ), mustWork = FALSE, winslash = "/")
            }

            cacheDataCode <- append(
               cacheDataCode,
               paste(cacheVariableName, " <- \"", localFile, "\"", sep = "")
            )
         }


         downloadCondition <- ''
         if (canCacheData && !cacheDataWorkingDir)
         {
            downloadCondition <- paste("if (!file.exists(", cacheVariableName, ")) ", sep = "")
         }

         cacheDataCode <- append(cacheDataCode, list(
            paste(
               downloadCondition,
               if (.rs.isPackageInstalled("curl")) "curl::curl_download(" else "download.file(",
               cacheUrlName,
               ", ",
               cacheVariableName,
               ")",
               sep = ""
            )
         ))
      }

      list(
         code = cacheDataCode,
         localFile = localFile
      )
   }

   functionInfoFromOptions <- list(
      "text" = function() {
         functionName <- ""
         functionReference <- readr::read_delim
         if (is.null(dataImportOptions$delimiter)
            || identical(dataImportOptions$delimiter, ","))
         {
            functionName <- "read_csv"
            functionReference <- readr::read_csv
         }
         else if (identical(dataImportOptions$delimiter, " "))
         {
            functionName <- "read_table"
            functionReference <- readr::read_table
         }
         else
         {
            functionName <- "read_delim"
         }

         # load parameters
         options <- list()
         options[["file"]] <- cacheOrFileFromOptions(dataImportOptions)
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
         optionTypes[["file"]] <- cacheTypeOrFileTypeFromOptions(dataImportOptions)
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
            optionTypes = optionTypes,
            cacheFileExtension = ".txt"
         ))
      },
      "statistics" = function() {
         # load parameters
         options <- list()

         # current version of haven uses "path", next version uses "file" or "data_file"
         options[["path"]] <- options[["file"]] <- cacheOrFileFromOptions(dataImportOptions)
         options[["b7dat"]] <- options[["data_file"]] <- cacheOrFileFromOptions(dataImportOptions)
         options[["b7cat"]] <- cacheOrFileFromOptions(dataImportOptions, "modelLocation")

         havenFunction <- switch(dataImportOptions$format,
            "sav" = list(name = "read_sav", ref = haven::read_sav),
            "dta" = list(name = "read_dta", ref = haven::read_dta),
            "por" = list(name = "read_por", ref = haven::read_por),
            "sas" = list(name = "read_sas", ref = haven::read_sas),
            "stata" = list(name = "read_stata", ref = haven::read_stata)
         )

         # set special parameter types
         optionTypes <- list()
         optionTypes[["path"]] <- optionTypes[["file"]] <- cacheTypeOrFileTypeFromOptions(dataImportOptions)
         optionTypes[["b7dat"]] <- optionTypes[["data_file"]] <- cacheTypeOrFileTypeFromOptions(dataImportOptions)
         optionTypes[["b7cat"]] <- cacheTypeOrFileTypeFromOptions(dataImportOptions, "modelLocation")

         return(list(
            name = havenFunction$name,
            reference = havenFunction$ref,
            package = "haven",
            options = options,
            optionTypes = optionTypes,
            cacheFileExtension = ".dat"
         ))
      },
      "xls" = function() {
         # load parameters
         options <- list()
         options[["path"]] <- cacheOrFileFromOptions(dataImportOptions)
         options[["sheet"]] <- dataImportOptions$sheet
         options[["na"]] <- dataImportOptions$na
         options[["col_names"]] <- dataImportOptions$columnNames
         options[["skip"]] <- dataImportOptions$skip
         options[["col_types"]] <- dataImportOptions$columnDefinitions

         # set special parameter types
         optionTypes <- list()
         optionTypes[["path"]] <- cacheTypeOrFileTypeFromOptions(dataImportOptions)
         optionTypes[["sheet"]] <- "character"
         optionTypes[["na"]] <- "character"
         optionTypes[["col_types"]] <- "columnDefinitionsReadXl"

         return(list(
            name = "read_excel",
            reference = readxl::read_excel,
            package = "readxl",
            paramsPackage = NULL,
            options = options,
            optionTypes = optionTypes,
            cacheFileExtension = c(".xls", ".xlsx")
         ))
      }
   )

   dataName <- dataImportOptions$dataName
   if (identical(dataName, NULL) || identical(dataName, ""))
   {
      dataName <- .rs.assembleDataImportName(dataImportOptions)
      if (is.null(dataName) || identical(dataName, ""))
      {
         dataName <- "dataset"
      }
   }

   dataName <- gsub("[\\._]+", "_", c(make.names(dataName)), perl=TRUE)

   importInfo$dataName <- dataImportOptions$dataName <- dataName

   dataImportOptions$cacheUrlNames <- list()
   dataImportOptions$cacheUrlNames$importLocation <- paste("url", sep = "")
   dataImportOptions$cacheUrlNames$modelLocation <- paste("modelurl", sep = "")

   dataImportOptions$cacheVariableNames <- list()
   dataImportOptions$cacheVariableNames$importLocation <- paste("destfile", sep = "")
   dataImportOptions$cacheVariableNames$modelLocation <- paste("modelfile", sep = "")

   if (identical(dataImportOptions$localFiles, NULL)) {
      dataImportOptions$localFiles <- list()
   }

   functionInfo <- functionInfoFromOptions[[dataImportOptions$mode]]()
   options <- functionInfo$options
   optionTypes <- functionInfo$optionTypes

   importLocationCache <- cacheCodeFromOptions(dataImportOptions, functionInfo, "importLocation")
   modelLocationCache <- cacheCodeFromOptions(dataImportOptions, functionInfo, "modelLocation")

   if (identical(dataImportOptions$canCacheData, TRUE))
   {
      importInfo$localFiles <- list(
         importLocation = importLocationCache$localFile,
         modelLocation = modelLocationCache$localFile
      )
   }

   paramOptions <- list(
      options = options,
      optionTypes = optionTypes,
      importFunction = functionInfo$reference,
      dataImportOptions = dataImportOptions,
      package = functionInfo$paramsPackage
   )

   functionParameters <- .rs.assembleDataImportParameters(paramOptions)
   paramOptions$package <- NULL
   functionParametersNoNs <- .rs.assembleDataImportParameters(paramOptions)

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

   previewCodeExpressions <- list()
   previewCodeExpressions <- append(previewCodeExpressions, importLocationCache$code)
   previewCodeExpressions <- append(previewCodeExpressions, modelLocationCache$code)
   previewCodeExpressions <- append(previewCodeExpressions, previewCode)

   importInfo$previewCode <- paste(
      previewCodeExpressions,
      collapse = "\n")

   importCodeExpressions <- c(paste(
      "library(",
      functionInfo$package,
      ")",
      sep = ""
   ))

   importCodeExpressions <- append(importCodeExpressions, importLocationCache$code)
   importCodeExpressions <- append(importCodeExpressions, modelLocationCache$code)
   importCodeExpressions <- append(importCodeExpressions, paste(dataName, " <- ", previewCodeNoNs, sep = ""))
   
   if (dataImportOptions$openDataViewer) {
      importCodeExpressions <- append(importCodeExpressions, paste("View(", dataName, ")", sep = ""))
   }

   importInfo$importCode <- paste(
      lapply(
         importCodeExpressions,
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
      dataImportOptions$canCacheData <- dataImportOptions$mode == "xls"
      dataImportOptions$cacheDataWorkingDir <- dataImportOptions$mode == "xls"

      result <- .rs.assembleDataImport(dataImportOptions)
      Encoding(result$importCode) <- "UTF-8"
      Encoding(result$previewCode) <- "UTF-8"
      
      return (result)
   }, error = function(e) {
      return(list(error = e))
   })
})

.rs.addJsonRpcHandler("preview_data_import", function(dataImportOptions, maxCols = 100, maxFactors = 64)
{
   dataImportOptions$importLocation <- .rs.pathRelativeToWorkingDir(dataImportOptions$importLocation)
   dataImportOptions$modelLocation <- .rs.pathRelativeToWorkingDir(dataImportOptions$modelLocation)

   tryCatch({
      Encoding(dataImportOptions$importLocation) <- "UTF-8"
     
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

      parsingErrorsFromMode <- function(mode, data) {
         modeFunc <- list(
            "text" = function(data) {
               length(readr::problems(data)$row)
            }
         )

         if (identical(modeFunc[[mode]], NULL)) 0 else modeFunc[[mode]](data)
      }

      if (dataImportOptions$mode %in% names(beforeImportFromOptions))
      {
         beforeImportFromOptions[[dataImportOptions$mode]]()
      }

      dataImportOptions$canCacheData <- TRUE
      importInfo <- .rs.assembleDataImport(dataImportOptions)

      data <- suppressWarnings(
         eval(parse(text=importInfo$previewCode))
      )

      columns <- list()
      if (ncol(data)) {
         columns <- .rs.describeCols(data, maxFactors)
         if (ncol(data) > maxCols) {
            columns <- head(columns, maxCols)
            data <- data[, maxCols]
         }
      }
      
      parsingErrors <- parsingErrorsFromMode(dataImportOptions$mode, data)

      cnames <- names(data)
      size <- nrow(data)

      if (!identical(dataImportOptions$maxRows, NULL) && size > dataImportOptions$maxRows) {
         data <- head(data, dataImportOptions$maxRows)
         size <- nrow(data)
      }

      for(i in seq_along(data)) {
         data[[i]] <- .rs.formatDataColumn(data[[i]], 1, size)
      }

      options <- optionsInfoFromOptions[[dataImportOptions$mode]]()

      return(list(data = unname(data),
                  columns = columns,
                  options = options,
                  parsingErrors = parsingErrors,
                  localFiles = importInfo$localFiles))
   }, error = function(e) {
      return(list(error = e))
   })
})

.rs.addJsonRpcHandler("preview_data_import_clean", function(dataImportOptions)
{
   tryCatch({
      if (!identical(dataImportOptions$localFiles, NULL))
      {
         lapply(dataImportOptions$localFiles, function (e) {
            file.remove(paste(e));
         })
         dataImportOptions
      }
   }, error = function(e) {
      return(list(error = e))
   })
})