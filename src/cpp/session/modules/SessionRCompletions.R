#
# SessionRCompletions.R
#
# Copyright (C) 2014 by RStudio, Inc.
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

# Put the autocompletion types in the .rs.Env environment so they're accessible
# everywhere (sync with RCompletionManager.java)
assign(x = ".rs.acContextTypes",
       pos = which(search() == "tools:rstudio"),
       value = list(
          UNKNOWN = 0,
          FUNCTION = 1,
          SINGLE_BRACKET = 2,
          DOUBLE_BRACKET = 3,
          NAMESPACE_EXPORTED = 4,
          NAMESPACE_ALL = 5,
          DOLLAR = 6,
          AT = 7,
          FILE = 8,
          CHUNK = 9,
          ROXYGEN = 10,
          HELP = 11
       )
)

# Sync with RCompletionTypes.java
assign(x = ".rs.acCompletionTypes",
       pos = which(search() == "tools:rstudio"),
       value = list(
          UNKNOWN = 0,
          VECTOR = 1,
          ARRAY = 2,
          DATAFRAME = 3,
          LIST = 4,
          ENVIRONMENT = 5,
          FUNCTION = 6,
          ARGUMENT = 7,
          S4_CLASS = 8,
          S4_OBJECT = 9,
          S4_GENERIC = 10,
          S4_METHOD = 11,
          R5_CLASS = 12,
          R5_OBJECT = 13,
          FILE = 14,
          CHUNK = 15,
          ROXYGEN = 16,
          HELP = 17,
          STRING = 18,
          PACKAGE = 19,
          KEYWORD = 20,
          OPTION = 21,
          DATASET = 22
       )
)

.rs.addFunction("getCompletionType", function(object)
{
   # Reference classes
   if (inherits(object, "refObjectGenerator"))
      .rs.acCompletionTypes$R5_CLASS
   else if (inherits(object, "refClass"))
      .rs.acCompletionTypes$R5_OBJECT
   
   # S4
   else if (isS4(object))
   {
      if (inherits(object, "standardGeneric") || 
          inherits(object, "nonstandardGenericFunction"))
         .rs.acCompletionTypes$S4_GENERIC
      else if (inherits(object, "MethodDefinition"))
         .rs.acCompletionTypes$S4_METHOD
      else
         .rs.acCompletionTypes$S4_OBJECT
   }
   
   # Base
   else if (is.function(object))
      .rs.acCompletionTypes$FUNCTION
   else if (is.array(object))
      .rs.acCompletionTypes$ARRAY
   else if (inherits(object, "data.frame"))
      .rs.acCompletionTypes$DATAFRAME
   else if (is.list(object))
      .rs.acCompletionTypes$LIST
   else if (is.environment(object))
      .rs.acCompletionTypes$ENVIRONMENT
   else if (is.vector(object))
      .rs.acCompletionTypes$VECTOR
   else
      .rs.acCompletionTypes$UNKNOWN
})

.rs.addFunction("attemptRoxygenTagCompletion", function(token)
{
   # Allow for roxygen completions when no token is available
   match <- token == "" || grepl("^@[a-zA-Z0-9]*$", token, perl = TRUE)
   if (!match)
      return(.rs.emptyCompletions(excludeOtherCompletions = TRUE))
   
   tag <- sub(".*(?=@)", '', token, perl = TRUE)
   
   # All known Roxygen2 tags, in alphabetical order
   tags <- c(
      "@aliases ",
      "@author ",
      "@concepts ",
      "@describeIn ",
      "@description ",
      "@details ",
      "@docType ",
      "@example ",
      "@examples ",
      "@export",
      "@exportClass ",
      "@exportMethod ",
      "@family ",
      "@field ",
      "@format ",
      "@import ",
      "@importClassesFrom ",
      "@importFrom ",
      "@importMethodsFrom ",
      "@include ",
      "@inheritParams ",
      "@keywords ",
      "@method ",
      "@name ",
      "@note ",
      "@noRd",
      "@param ",
      "@rdname ",
      "@references ",
      "@return ",
      "@S3method ",
      "@section ",
      "@seealso ",
      "@slot ",
      "@source ",
      "@template ",
      "@templateVar ",
      "@title ",
      "@usage ",
      "@useDynLib "
   )
   
   matchingTags <- grep(paste("^", tag, sep=""), tags, value = TRUE)
   
   .rs.makeCompletions(tag,
                       matchingTags,
                       type = .rs.acCompletionTypes$ROXYGEN,
                       excludeOtherCompletions = TRUE)
})

.rs.addFunction("getCompletionsFile", function(token)
{
   slashIndices <- gregexpr("/", token, fixed = TRUE)[[1]]
   lastSlashIndex <- slashIndices[length(slashIndices)]
   
   if (lastSlashIndex == -1)
   {
      pattern <- paste("^", .rs.asCaseInsensitiveRegex(.rs.escapeForRegex(token)), sep = "")
      files <- list.files(all.files = TRUE,
                          pattern = pattern,
                          no.. =  TRUE)
   }
   else
   {
      directory <- substring(token, 1, lastSlashIndex - 1)
      file <- substring(token, lastSlashIndex + 1, nchar(token))
      pattern <- paste("^", .rs.asCaseInsensitiveRegex(.rs.escapeForRegex(file)), sep = "")
      listed <- list.files(directory,
                           all.files = TRUE,
                           pattern = pattern,
                           no.. = TRUE)
      
      startsWithLetter <- grepl("^[a-zA-Z0-9]", listed, perl = TRUE)
      first <- which(startsWithLetter)
      last <- which(!startsWithLetter)
      order <- c(first, last)
      listed <- listed[order]
      
      files <- file.path(
         directory,
         listed
      )
   }
   
   isDir <- file.info(files)[, "isdir"] %in% TRUE ## protect against NA
   files[isDir] <- paste(files[isDir], "/", sep = "")
   .rs.makeCompletions(token = token,
                       results = files,
                       packages = ifelse(isDir, "<directory>", "<file>"),
                       type = .rs.acCompletionTypes$FILE,
                       quote = FALSE,
                       excludeOtherCompletions = TRUE)
})

.rs.addFunction("resolveFormals", function(token,
                                           object,
                                           functionName,
                                           functionCall,
                                           matchedCall,
                                           envir)
{
   tryCatch({
      
      if (length(functionCall) > 1 && .rs.isS3Generic(object))
      {
         s3methods <- .rs.getS3MethodsForFunction(functionName, envir)
         objectForDispatch <- .rs.getAnywhere(matchedCall[[2]], envir)
         classes <- class(objectForDispatch)
         
         for (class in c(classes, "default"))
         {
            methodName <- paste(functionName, class, sep = ".")
            method <- .rs.getAnywhere(methodName, envir)
            if (!is.null(method))
            {
               formals <- .rs.getFunctionArgumentNames(method)
               methods <- rep.int(methodName, length(formals))
               break
            }
         }
      }
      else
      {
         formals <- .rs.getFunctionArgumentNames(object)
         methods <- rep.int(functionName, length(formals))
      }
      
      keep <- .rs.fuzzyMatches(formals, token) & 
         !(formals %in% names(functionCall)) ## leave out formals already in call
      
      return(list(
         formals = formals[keep],
         methods = methods[keep]
      ))
      
   }, error = function(e) NULL
   )
})

.rs.addFunction("matchCall", function(func,
                                      call)
{
   ## NOTE: the ugliness here is necessary to handle missingness in calls
   ## e.g. `x <- call[[i]]` fails to assign to `x` if `call[[i]]` is missing
   i <- 1
   while (TRUE)
   {
      if (i > length(call))
         break
      
      if (length(call[[i]]) == 1 && is.symbol(call[[i]]) && as.character(call[[i]]) == "...")
         call <- call[-i]
      else
         i <- i + 1
   }
   
   tryCatch(match.call(func, call), error = function(e) call)
})

.rs.addFunction("getCompletionsFunction", function(token,
                                                   string,
                                                   functionCall,
                                                   discardFirst,
                                                   envir = parent.frame())
{
   result <- .rs.emptyCompletions()
   
   splat <- strsplit(string, ":{2,3}", perl = TRUE)[[1]]
   object <- NULL
   if (length(splat) == 1)
   {
      object <- .rs.getAnywhere(string, envir = envir)
   }
   else if (length(splat) == 2)
   {
      namespaceString <- splat[[1]]
      functionString <- string <- splat[[2]]
      
      if (namespaceString %in% loadedNamespaces())
      {
         object <- tryCatch(
            expr = {
               get(
                  functionString,
                  envir = asNamespace(namespaceString)
               )
            },
            error = function(e) {
               NULL
            }
         )
      }
   }
   
   if (!is.null(object) && is.function(object))
   {
      matchedCall <- .rs.matchCall(object, functionCall)
      formals <- .rs.resolveFormals(token,
                                    object,
                                    string,
                                    functionCall,
                                    matchedCall,
                                    envir)
      
      if (length(formals$formals))
         formals$formals <- paste(formals$formals, "= ")
      
      fguess <- if (length(formals$methods))
         formals$methods[[1]]
      else
         ""
      
      result <- .rs.makeCompletions(
         token = token,
         results = formals$formals,
         packages = formals$methods,
         type = .rs.acCompletionTypes$ARGUMENT,
         fguess = fguess,
         orderStartsWithAlnumFirst = FALSE
      )
   }
   
   if (discardFirst)
   {
      result$results <- tail(result$results, length(result$results) - 1)
      result$packages <- tail(result$packages, length(result$packages) - 1)
      result$quote <- tail(result$quote, length(result$quote) - 1)
      result$type <- tail(result$type, length(result$type) - 1)
   }
   
   result
   
})

.rs.addFunction("getSourceIndexCompletions", function(token)
{
   .Call("rs_getSourceIndexCompletions", token)
})

.rs.addFunction("getCompletionsNamespace", function(token, string, exportsOnly, envir)
{
   result <- .rs.emptyCompletions(excludeOtherCompletions = TRUE)
   
   if (!(string %in% loadedNamespaces()))
   {
      tryCatch(
         suppressPackageStartupMessages(requireNamespace(string, quietly = TRUE)),
         error = function(e) NULL
      )
   }
   
   if (string %in% loadedNamespaces())
   {
      namespace <- asNamespace(string)
      objectNames <- if (exportsOnly)
         getNamespaceExports(namespace)
      else
         objects(namespace, all.names = TRUE)
      
      completions <- .rs.selectFuzzyMatches(objectNames, token)
      objects <- mget(completions, envir = namespace, inherits = TRUE)
      type <- vapply(objects, FUN.VALUE = numeric(1), USE.NAMES = FALSE, .rs.getCompletionType)
      
      result <- .rs.makeCompletions(
         token = token,
         results = completions,
         packages = string,
         quote = FALSE,
         type = type,
         excludeOtherCompletions = TRUE
      )
   }
   
   result
   
})


.rs.addFunction("emptyCompletions", function(excludeOtherCompletions = FALSE,
                                             overrideInsertParens = FALSE,
                                             orderStartsWithAlnumFirst = TRUE)
{
   .rs.makeCompletions(
      token = "",
      results = character(),
      packages = character(),
      quote = logical(),
      type = numeric(),
      fguess = "",
      excludeOtherCompletions = .rs.scalar(excludeOtherCompletions),
      overrideInsertParens = .rs.scalar(overrideInsertParens),
      orderStartsWithAlnumFirst = .rs.scalar(orderStartsWithAlnumFirst)
   )
})

.rs.addFunction("isEmptyCompletion", function(completions)
{
   length(completions$results) == 0
})

.rs.addFunction("fuzzyMatches", function(completions, token)
{
   .rs.startsWith(tolower(completions), tolower(token))
})

.rs.addFunction("selectFuzzyMatches", function(completions, token)
{
   completions[.rs.fuzzyMatches(completions, token)]
})

.rs.addFunction("formCompletionVector", function(object, default, n)
{
   if (!length(object))
      rep_len(default, n)
   else
      rep_len(object, n)
})

.rs.addFunction("makeCompletions", function(token,
                                            results,
                                            packages = character(),
                                            quote = logical(),
                                            type = numeric(),
                                            fguess = "",
                                            excludeOtherCompletions = FALSE,
                                            overrideInsertParens = FALSE,
                                            orderStartsWithAlnumFirst = TRUE)
{
   # Ensure other 'vector' completions are of the same length as 'results'
   n <- length(results)
   packages <- .rs.formCompletionVector(packages, "", n)
   quote    <- .rs.formCompletionVector(quote, FALSE, n)
   type     <- .rs.formCompletionVector(type, .rs.acCompletionTypes$UNKNOWN, n)
   
   # Favor completions starting with a letter
   if (orderStartsWithAlnumFirst)
   {
      startsWithLetter <- grepl("^[a-zA-Z0-9]", results, perl = TRUE)
      first <- which(startsWithLetter)
      last <- which(!startsWithLetter)
      order <- c(first, last)
      
      results <- results[order]
      packages <- packages[order]
      quote <- quote[order]
      type <- type[order]
   }
   
   list(token = token,
        results = results,
        packages = packages,
        quote = quote,
        type = type,
        fguess = fguess,
        excludeOtherCompletions = .rs.scalar(excludeOtherCompletions),
        overrideInsertParens = .rs.scalar(overrideInsertParens))
})

.rs.addFunction("subsetCompletions", function(completions, indices)
{
   for (name in c("results", "packages", "quote", "type"))
      completions[[name]] <- completions[[name]][indices]
   
   completions
})

.rs.addFunction("appendCompletions", function(old, new)
{
   for (name in c("results", "packages", "quote", "type"))
      old[[name]] <- c(old[[name]], new[[name]])
   
   # resolve duplicates -- a completion is duplicated if its result
   # and package are identical (if 'type' or 'quote' differs, it's probably a bug?)
   drop <- intersect(
      which(duplicated(old$results)),
      which(duplicated(old$packages))
   )
   
   if (length(drop))
   {
      for (name in c("results", "packages", "quote", "type"))
         old[[name]] <- old[[name]][-c(drop)]
   }
   
   if (length(new$fguess) && new$fguess != "")
      old$fguess <- new$fguess
   
   if (length(new$excludeOtherCompletions) && new$excludeOtherCompletions)
      old$excludeOtherCompletions <- new$excludeOtherCompletions
   
   if (length(new$overrideInsertParens) && new$overrideInsertParens)
      old$overrideInsertParens <- new$overrideInsertParens
   
   old
})

.rs.addFunction("blackListEvaluationDataTable", function(token, string, envir)
{
   tryCatch({
      parsed <- suppressWarnings(parse(text = string))
      if (is.expression(parsed))
      {
         call <- parsed[[1]][[1]]
         if (as.character(call[[1]]) == "[")
         {
            objectName <- parsed[[1]][[2]]
            object <- .rs.getAnywhere(objectName, envir = envir)
            if (inherits(object, "data.table"))
            {
               .rs.makeCompletions(
                  token = token,
                  results = names(object),
                  packages = string,
                  quote = FALSE,
                  type = vapply(object, FUN.VALUE = numeric(1), USE.NAMES = FALSE, .rs.getCompletionType)
               )
            }
         }
      }
      else
      {
         NULL
      }
   }, error = function(e) NULL
   )
   
})

.rs.addFunction("blackListEvaluation", function(token, string, envir)
{
   if (!is.null(result <- .rs.blackListEvaluationDataTable(token, string, envir)))
      return(result)
   
   NULL
   
})

## NOTE: for '@' as well (set with S4 bit)
.rs.addFunction("getCompletionsDollar", function(token, string, envir, isAt)
{
   result <- .rs.emptyCompletions(excludeOtherCompletions = TRUE)
   
   ## Blacklist certain evaluations
   if (!is.null(blacklist <- .rs.blackListEvaluation(token, string, envir)))
      return(blacklist)
   
   object <- .rs.getAnywhere(string, envir)
   if (!is.null(object))
   {
      names <- character()
      type <- numeric()
      
      if (isAt)
      {
         if (isS4(object) && !inherits(object, "classRepresentation"))
         {
            tryCatch({
               names <- slotNames(object)
               type <- numeric(length(names))
               for (i in seq_along(names))
                  type[[i]] <- tryCatch(
                     .rs.getCompletionType(eval(call("@", object, names[[i]]), envir = envir)),
                     error = function(e) .rs.acCompletionTypes$UNKNOWN
                  )
            }, error = function(e) NULL
            )
         }
      }
      else
      {
         names <- character()
         
         # Check to see if an overloadd .DollarNames method has been provided,
         # and use that to resolve names if possible.
         dollarNamesMethod <- .rs.getDollarNamesMethod(object)
         if (!is.null(dollarNamesMethod))
         {
            names <-  dollarNamesMethod(object)
         }
         else
         {
            # Don't allow S4 objects for dollar name resolution
            if (!isS4(object))
            {
               names <- .rs.getNames(object)
            }
         }
         
         type <- numeric(length(names))
         for (i in seq_along(names))
            type[[i]] <- tryCatch(
               .rs.getCompletionType(eval(call("$", object, names[[i]]), envir = envir)),
               error = function(e) .rs.acCompletionTypes$UNKNOWN
            )
      }
      
      keep <- .rs.fuzzyMatches(names, token)
      completions <- names[keep]
      type <- type[keep]
      
      result <- .rs.makeCompletions(
         token = token,
         results = completions,
         packages = string,
         quote = FALSE,
         type = type,
         excludeOtherCompletions = TRUE
      )
   }
   
   result
   
})

.rs.addFunction("getCompletionsSingleBracket", function(token,
                                                        string,
                                                        numCommas,
                                                        envir)
{
   result <- .rs.emptyCompletions()
   
   ## Blacklist certain evaluations
   if (!is.null(result <- .rs.blackListEvaluation(token, string, envir)))
      return(result)
   
   object <- .rs.getAnywhere(string, envir)
   if (is.null(object))
      return(result)
   
   completions <- character()
   if (is.array(object) && !is.null(dn <- dimnames(object)))
   {
      if (numCommas + 1 <= length(dn))
         completions <- dimnames(object)[[numCommas + 1]]
      
      string <- if (numCommas == 0)
         paste("rownames(", string, ")", sep = "")
      else if (numCommas == 1)
         paste("colnames(", string, ")", sep = "")
      else
         paste("dimnames(", string, ")[", numCommas + 1, "]", sep = "")
   }
   else
   {
      completions <- .rs.getNames(object)
   }
   
   completions <- .rs.selectFuzzyMatches(completions, token)
   
   if (length(completions))
   {
      result <- .rs.makeCompletions(
         token = token,
         results = completions,
         packages = string,
         quote = !inherits(object, "data.table"),
         type = .rs.acCompletionTypes$STRING
      )
   }
   
   result
   
})

.rs.addFunction("getCompletionsDoubleBracket", function(token,
                                                        string,
                                                        envir = parent.frame())
{
   result <- .rs.emptyCompletions()
   
   ## Blacklist certain evaluations
   if (!is.null(result <- .rs.blackListEvaluation(token, string, envir)))
      return(result)
   
   object <- .rs.getAnywhere(string, envir)
   if (is.null(object))
      return(result)
   
   completions <- .rs.getNames(object)
   completions <- .rs.selectFuzzyMatches(completions, token)
   
   if (length(completions))
   {
      result <- .rs.makeCompletions(
         token = token,
         results = completions,
         packages = string,
         quote = TRUE,
         type = .rs.acCompletionTypes$STRING,
         overrideInsertParens = TRUE
      )
   }
   
   result
   
})

.rs.addFunction("getCompletionsPackages", function(token, appendColons = FALSE)
{
   allPackages <- Reduce(union, lapply(.libPaths(), list.files))
   
   # Not sure why 'DESCRIPTION' might show up here, but let's take it out
   allPackages <- setdiff(allPackages, "DESCRIPTION")
   
   completions <- .rs.selectFuzzyMatches(allPackages, token)
   .rs.makeCompletions(token = token,
                       results = if (appendColons && length(completions))
                          paste(completions, "::", sep = "")
                       else
                          completions,
                       packages = completions,
                       quote = !appendColons,
                       type = .rs.acCompletionTypes$PACKAGE)
})

.rs.addFunction("getCompletionsData", function(token)
{
   # Suppress warnings e.g.
   #
   #    Warning messages:
   #       1: In data(package = .packages(all.available = TRUE)) :
   #       datasets have been moved from package 'base' to package 'datasets'
   #    2: In data(package = .packages(all.available = TRUE)) :
   #       datasets have been moved from package 'stats' to package 'datasets'
   #
   availableData <- suppressWarnings(data()$results)
   
   # Don't include aliases
   indices <- intersect(
      which(.rs.fuzzyMatches(availableData[, "Item"], token)),
      grep(" ", availableData[, "Item"], fixed = TRUE, invert = TRUE)
   )
   
   results <- availableData[indices, "Item"]
   packages <- availableData[indices, "Package"]
   
   .rs.makeCompletions(token,
                       results,
                       packages,
                       quote = TRUE,
                       type = .rs.acCompletionTypes$DATASET)
})

.rs.addFunction("getCompletionsGetOption", function(token)
{
   allOptions <- names(options())
   .rs.makeCompletions(token = token,
                       results = .rs.selectFuzzyMatches(allOptions, token),
                       package = "options",
                       quote = TRUE,
                       type = .rs.acCompletionTypes$OPTION)
})

.rs.addFunction("getCompletionsOptions", function(token)
{
   allOptions <- names(options())
   completions <- .rs.selectFuzzyMatches(allOptions, token)
   if (length(completions))
      completions <- paste(completions, "= ")
   
   .rs.makeCompletions(token,
                       completions,
                       type = .rs.acCompletionTypes$OPTION)
})

.rs.addFunction("getCompletionsSearchPath", function(token, overrideInsertParens = FALSE)
{
   objects <- .rs.objectsOnSearchPath(token, TRUE)
   objects[["keywords"]] <- c(
      "NULL", "NA", "TRUE", "FALSE", "T", "F", "Inf", "NaN",
      "NA_integer_", "NA_real_", "NA_character_", "NA_complex_"
   )
   
   names <- names(objects)
   results <- unlist(objects, use.names = FALSE)
   packages <- unlist(lapply(1:length(objects), function(i) {
      rep.int(names[i], length(objects[[i]]))
   }))
   
   # Keywords are really from the base package
   packages[packages == "keywords"] <- "base"
   
   keep <- .rs.fuzzyMatches(results, token)
   results <- results[keep]
   packages <- packages[keep]
   
   order <- order(results)
   
   # If the token is 'T' or 'F', prefer 'TRUE' and 'FALSE' completions
   if (token == "T")
   {
      TRUEpos <- which(results == "TRUE")
      order <- c(TRUEpos, order[-c(TRUEpos)])
   }
   
   if (token == "F")
   {
      FALSEpos <- which(results == "FALSE")
      order <- c(FALSEpos, order[-c(FALSEpos)])
   }
   
   results <- results[order]
   packages <- packages[order]
   
   type <- vapply(seq_along(results), function(i) {
      if (packages[[i]] == "keywords")
         .rs.acCompletionTypes$KEYWORD
      else
         tryCatch(
            .rs.getCompletionType(get(results[[i]], pos = which(search() == packages[[i]]))),
            error = function(e) .rs.acCompletionTypes$UNKNOWN
         )
   }, FUN.VALUE = numeric(1), USE.NAMES = FALSE)
   
   .rs.makeCompletions(token = token,
                       results = results,
                       packages = packages,
                       quote = FALSE,
                       type = type,
                       overrideInsertParens = overrideInsertParens)
   
})

.rs.addFunction("finishExpression", function(string)
{
   .Call("rs_finishExpression", as.character(string))
})

.rs.addFunction("getCompletionsAttr", function(token,
                                               functionCall,
                                               envir)
{
   result <- tryCatch({
      wrapper <- function(x, which, exact = FALSE) {}
      matched <- .rs.matchCall(wrapper, functionCall)
      if (is.null(matched[["x"]]))
         return(.rs.emptyCompletions())
      objectExpr <- matched[["x"]]
      objectName <- capture.output(print(objectExpr))
      object <- eval(objectExpr, envir = envir)
      
      completions <- .rs.selectFuzzyMatches(
         names(attributes(object)),
         token
      )
      
      result <- .rs.makeCompletions(
         token,
         completions,
         paste("attributes(", objectName, ")", sep = ""),
         quote = TRUE,
         type = .rs.acCompletionTypes$STRING
      )
      
   }, error = function(e) .rs.emptyCompletions())
   result
})

.rs.addJsonRpcHandler("get_completions", function(token,
                                                  string,
                                                  type,
                                                  numCommas,
                                                  functionCallString,
                                                  chainObjectName,
                                                  additionalArgs,
                                                  excludeArgs,
                                                  excludeArgsFromObject,
                                                  filePath)
{
   filePath <- suppressWarnings(.rs.normalizePath(filePath))
   
   ## NOTE: these are passed in as lists of strings; convert to character
   additionalArgs <- as.character(additionalArgs)
   excludeArgs <- as.character(excludeArgs)
   
   ## Try to parse the function call string
   functionCall <- tryCatch({
      parse(text = .rs.finishExpression(functionCallString))[[1]]
   }, error = function(e) 
      NULL
   )
   
   ## Handle some special cases early
   
   # help
   if (.rs.acContextTypes$HELP %in% type)
      return(.rs.getCompletionsHelp(token))
   
   # Roxygen
   if (.rs.acContextTypes$ROXYGEN %in% type)
      return(.rs.attemptRoxygenTagCompletion(token))
   
   if (.rs.acContextTypes$FUNCTION %in% type &&
       string[[1]] == "data" &&
       numCommas[[1]] == 0)
      return(.rs.getCompletionsData(token))
   
   # No information on completions other than token
   if (!length(string))
   {
      # If there was no token, give up
      if (token == "")
         return(.rs.emptyCompletions())
      
      # Otherwise, complete from the seach path + available packages
      completions <- .rs.appendCompletions(
         .rs.getCompletionsSearchPath(token),
         .rs.getCompletionsPackages(token, TRUE)
      )
      
      # try completing from the source index if this is an
      # R file within a package
      if (.rs.isRScriptInPackageBuildTarget(filePath))
      {
         # get the active package
         pkgName <- .rs.packageNameForSourceFile(filePath)
         completions <- .rs.appendCompletions(
            completions,
            .rs.getCompletionsActivePackage(token, pkgName)
         )
      }
      return(completions)
   }
   
   # library, require, requireNamespace, loadNamespace
   if (string[[1]] %in% c("library", "require", "requireNamespace") &&
          numCommas[[1]] == 0)
   {
      return(.rs.getCompletionsPackages(token))
   }
   
   ## Other special cases (but we may still want completions from
   ## other contexts)
   
   # attr
   completions <- if (string[[1]] == "attr")
   {
      .rs.getCompletionsAttr(token, functionCall, parent.frame())
   }
   
   # getOption
   else if (string[[1]] == "getOption" && numCommas[[1]] == 0)
   {
      .rs.getCompletionsGetOption(token)
   }
   
   # options
   else if (string[[1]] == "options" && type == .rs.acContextTypes$FUNCTION)
   {
      .rs.getCompletionsOptions(token)
   }
   
   # no special case (start with empty completions)
   else
   {
      .rs.emptyCompletions()
   }
   
   ## If we are getting completions from a '$', '@', '::' or ':::' then we do not
   ## want to look for completions in other contexts
   # dollar context
   dontLookBack <- length(type) && type[[1]] %in% c(
      .rs.acContextTypes$DOLLAR, .rs.acContextTypes$AT,
      .rs.acContextTypes$NAMESPACE_EXPORTED,
      .rs.acContextTypes$NAMESPACE_ALL
   )
   
   if (dontLookBack)
   {
      # dollar context
      if (type[[1]] %in% c(.rs.acContextTypes$DOLLAR, .rs.acContextTypes$AT))
      {
         completions <- .rs.getCompletionsDollar(
            token,
            string[[1]],
            parent.frame(),
            type[[1]] == .rs.acContextTypes$AT
         )
      }
      
      # namespace context
      else if (type[[1]] %in% c(.rs.acContextTypes$NAMESPACE_EXPORTED,
                                .rs.acContextTypes$NAMESPACE_ALL))
      {
         completions <- .rs.getCompletionsNamespace(
            token,
            string[[1]],
            type[[1]] == .rs.acContextTypes$NAMESPACE_EXPORTED,
            parent.frame()
         )
      }
   }
   
   # otherwise, look through the contexts and pick up completions
   else
   {
      for (i in seq_along(string))
      {
         discardFirst <- type[[i]] == .rs.acContextTypes$FUNCTION && chainObjectName != ""
         completions <- .rs.appendCompletions(
            completions,
            .rs.getRCompletions(token,
                                string[[i]],
                                type[[i]],
                                numCommas[[i]],
                                functionCall,
                                discardFirst,
                                parent.frame())
         )
      }
   }
   
   # get completions from the search path for the 'generic' contexts
   if (token != "" && 
          type[[1]] %in% c(.rs.acContextTypes$UNKNOWN, .rs.acContextTypes$FUNCTION,
                           .rs.acContextTypes$SINGLE_BRACKET, .rs.acContextTypes$DOUBLE_BRACKET))
   {
      completions <- .rs.appendCompletions(
         completions,
         .rs.getCompletionsSearchPath(token)
      )
      
      if (.rs.isRScriptInPackageBuildTarget(filePath))
      {
         pkgName <- .rs.packageNameForSourceFile(filePath)
         completions <- .rs.appendCompletions(
            completions,
            .rs.getCompletionsActivePackage(token, pkgName)
         )
      }
   }
   
   ## File-based completions
   if (.rs.acContextTypes$FILE %in% type)
      completions <- .rs.appendCompletions(
         completions,
         .rs.getCompletionsFile(token)
      )
   
   ## Package completions (e.g. `stats::`)
   if (token != "" && .rs.acContextTypes$UNKNOWN %in% type)
      completions <- .rs.appendCompletions(
         completions,
         .rs.getCompletionsPackages(token, TRUE)
      )
   
   ## If the caller has supplied information about chain completions (e.g.
   ## for completions from
   ##
   ##    x %>% foo() %>% bar()
   ##
   ## then retrieve those completions.
   completions <- .rs.appendCompletions(
      completions,
      .rs.getRChainCompletions(token,
                               chainObjectName,
                               additionalArgs,
                               excludeArgs,
                               excludeArgsFromObject,
                               discardFirst,
                               parent.frame())
   )
   
   ## Override param insertion if the function was 'debug' or 'trace'
   for (i in seq_along(type))
   {
      if (type[[i]] %in% c(.rs.acContextTypes$FUNCTION, .rs.acContextTypes$UNKNOWN))
      {
         ## Blacklist certain functions
         if (string[[i]] %in% c("help", "str"))
         {
            completions$overrideInsertParens <- .rs.scalar(TRUE)
         }
         else
         {
            ## Blacklist based on formals of the function
            object <- .rs.getAnywhere(string[[i]], parent.frame())
            if (is.function(object))
            {
               argNames <- .rs.getFunctionArgumentNames(object)
               if (any(c("f", "fun", "func") %in% tolower(gsub("[^a-zA-Z]", "", argNames))))
                  completions$overrideInsertParens <- .rs.scalar(TRUE)
            }
         }
      }
   }
   
   completions$token <- token
   completions
   
})

.rs.addJsonRpcHandler("get_dplyr_join_completions", function(token,
                                                             leftDataName,
                                                             rightDataName,
                                                             verb,
                                                             cursorPos)
{
   .rs.getDplyrJoinCompletions(
      token,
      leftDataName,
      rightDataName,
      verb,
      cursorPos,
      parent.frame()
   )
   
})

.rs.addJsonRpcHandler("get_dplyr_join_completions_string", function(token,
                                                                    string,
                                                                    cursorPos)
{
   result <- tryCatch(
      
      expr = {
         parsed <- parse(text = string)[[1]]
         verb <- as.character(parsed[[1]])
         func <- get(verb, envir = asNamespace("dplyr"))
         matched <- .rs.matchCall(func, parsed)
         leftName <- as.character(matched[["x"]])
         rightName <- as.character(matched[["y"]])
         .rs.getDplyrJoinCompletions(
            token,
            leftName,
            rightName,
            verb,
            cursorPos,
            parent.frame()
         )
      }, 
      
      error = function(e) {
         .rs.emptyCompletions()
      }
   )
   
   result
})

.rs.addFunction("getDplyrJoinCompletions", function(token,
                                                    leftDataName,
                                                    rightDataName,
                                                    verb,
                                                    cursorPos,
                                                    envir)
{
   result <- .rs.emptyCompletions()
   
   leftData <- .rs.getAnywhere(leftDataName, envir)
   rightData <- .rs.getAnywhere(rightDataName, envir)
   
   if (cursorPos == "left" && !is.null(leftData))
   {
      completions <- .rs.selectFuzzyMatches(
         .rs.getNames(leftData),
         token
      )
      result <- .rs.makeCompletions(
         token = token,
         results = completions,
         packages = leftDataName,
         quote = TRUE,
         type = vapply(leftData[completions], FUN.VALUE = numeric(1), USE.NAMES = FALSE, .rs.getCompletionType),
         excludeOtherCompletions = TRUE
      )
   }
   else if (cursorPos == "right" && !is.null(rightData))
   {
      completions <- .rs.selectFuzzyMatches(
         .rs.getNames(rightData),
         token
      )
      result <- .rs.makeCompletions(
         token = token,
         results = completions,
         packages = rightDataName,
         quote = TRUE,
         type = vapply(rightData[completions], FUN.VALUE = numeric(1), USE.NAMES = FALSE, .rs.getCompletionType),
         excludeOtherCompletions = TRUE
      )
   }
   
   result
   
})

.rs.addFunction("getRChainCompletions", function(token,
                                                 chainObjectName,
                                                 additionalArgs,
                                                 excludeArgs,
                                                 excludeArgsFromObject,
                                                 discardFirst,
                                                 envir)
{
   ## chainObjectName will be provided if the client detected
   ## that we were performing completions within an e.g. 
   ## `%>%` chain -- use completions from the associated data object.
   result <- .rs.emptyCompletions()
   
   if (!is.null(chainObjectName) && !excludeArgsFromObject)
   {
      object <- .rs.getAnywhere(chainObjectName, envir = envir)
      if (length(object))
      {
         objectNames <- .rs.getNames(object)
         if (length(objectNames))
         {
            completions <- .rs.selectFuzzyMatches(objectNames, token)
            result <- .rs.makeCompletions(
               token = token,
               results = completions,
               packages = paste("[", chainObjectName, "]", sep = ""),
               quote = FALSE,
               type = vapply(object[completions], FUN.VALUE = numeric(1), USE.NAMES = FALSE, .rs.getCompletionType)
            )
         }
      }
   }
   
   if (length(additionalArgs))
   {
      argsToAdd <- .rs.selectFuzzyMatches(additionalArgs, token)
      result <- .rs.appendCompletions(
         result, 
         .rs.makeCompletions(
            token = token,
            results = argsToAdd,
            packages = paste("*", chainObjectName, "*", sep = ""),
            type = .rs.acCompletionTypes$UNKNOWN
         )
      )
   }
   
   if (length(excludeArgs))
   {
      indices <- which(!(result$results %in% excludeArgs))
      result <- .rs.subsetCompletions(result, indices)
   }
   
   result
   
})

.rs.addFunction("getRCompletions", function(token,
                                            string,
                                            type,
                                            numCommas,
                                            functionCall,
                                            discardFirst,
                                            envir)
{
   if (type == .rs.acContextTypes$FUNCTION)
      .rs.getCompletionsFunction(token, string, functionCall, discardFirst, envir)
   else if (type == .rs.acContextTypes$SINGLE_BRACKET)
      .rs.getCompletionsSingleBracket(token, string, numCommas, envir)
   else if (type == .rs.acContextTypes$DOUBLE_BRACKET)
      .rs.getCompletionsDoubleBracket(token, string, envir)
   else
      .rs.emptyCompletions()
   
})

## NOTE: This is a modified version of 'matchAvailableTopics'
## in 'completions.R' of the R sources.
.rs.addFunction("getCompletionsHelp", function(token)
{
   pkgCacheName <- ".completions.attachedPackagesCache"
   helpTopicsName <- ".completions.helpTopics"
   rsEnvPos <- which(search() == "tools:rstudio")
   
   attachedPackagesCache <- tryCatch(
      get(pkgCacheName, pos = rsEnvPos),
      error = function(e) character()
   )
   
   paths <- searchpaths()[substring(search(), 1, 8) == "package:"]
   if (!identical(basename(paths), attachedPackagesCache))
   {
      assign(pkgCacheName,
             basename(paths),
             pos = rsEnvPos)
      
      assign(helpTopicsName,
             unique(unlist(lapply(paths, .rs.readAliases))),
             pos = rsEnvPos)
   }
   
   aliases <- get(helpTopicsName, pos = rsEnvPos)
   completions <- .rs.selectFuzzyMatches(aliases, token)
   
   .rs.makeCompletions(
      token = token,
      results = completions,
      quote = grepl("[^a-zA-Z0-9._]", completions, perl = TRUE),
      type = .rs.acCompletionTypes$HELP,
      overrideInsertParens = TRUE
   )
   
})

.rs.addFunction("readAliases", function(path)
{
   if (file.exists(f <- file.path(path, "help", "aliases.rds")))
      names(readRDS(f))
   else
      character()
})

.rs.addFunction("getCompletionsActivePackage", function(token, pkgName)
{
   # Get completions from the source index
   sourceIndexCompletions <- .rs.getSourceIndexCompletions(token)
   
   # format completions for return to R
   if (!length(sourceIndexCompletions$completions))
   {
      completions <- .rs.emptyCompletions()
   }
   else
   {
      results <- sourceIndexCompletions$completions
      
      # TODO: more granular lookup on the object type for source index completions
      type <- ifelse(sourceIndexCompletions$isFunction,
                     .rs.acCompletionTypes$FUNCTION,
                     .rs.acCompletionTypes$UNKNOWN)
      
      completions <- .rs.makeCompletions(token = token,
                                         results = results,
                                         packages = pkgName,
                                         quote = FALSE,
                                         type = type)
   }
   
   # get completions from the imports of the package
   importCompletions <- tryCatch(
      getNamespaceImports(asNamespace(pkgName)),
      error = function(e) NULL
   )
   
   # remove 'base' element if it's just TRUE
   if (length(importCompletions))
   {
      if (isTRUE(importCompletions$base))
         importCompletions$base <- NULL
   }
   
   # if we have import completions, use them
   if (length(importCompletions))
   {
      importCompletionsList <- .rs.namedVectorAsList(importCompletions)
      
      # filter completions
      indices <- .rs.fuzzyMatches(importCompletionsList$values, token)
      importCompletionsList <- lapply(importCompletionsList, function(x) {
         x[indices]
      })
      
      objects <- lapply(seq_along(importCompletionsList$values), function(i) {
         
         tryCatch(
            expr = get(importCompletionsList$values[[i]],
                       envir = asNamespace(importCompletionsList$names[[i]])),
            error = function(e) NULL
         )
      })
      
      type <- vapply(objects, FUN.VALUE = numeric(1), USE.NAMES = FALSE, .rs.getCompletionType)
      
      completions <- .rs.appendCompletions(
         completions,
         .rs.makeCompletions(token = token,
                             results = importCompletionsList$values,
                             packages = paste("package:", importCompletionsList$names, sep = ""),
                             type = type)
      )
   }
   
   completions
   
})
