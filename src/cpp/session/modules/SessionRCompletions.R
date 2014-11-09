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

.rs.addFunction("attemptRoxygenTagCompletion", function(token)
{
   match <- grepl("^@[a-zA-Z0-9]*$", token, perl = TRUE)
   if (!match)
      return(NULL)
   
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
                       matchingTags)
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
   .rs.makeCompletions(token,
                       files,
                       ifelse(isDir, "<directory>", "<file>"),
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
      
      list(
         formals = formals[keep],
         methods = methods[keep]
      )
      
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
      functionString <- splat[[2]]
      
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
         token,
         formals$formals,
         formals$methods,
         fguess = fguess,
         orderStartsWithAlnumFirst = FALSE
      )
   }
   
   if (discardFirst)
   {
      result$results <- tail(result$results, length(result$results) - 1)
      result$packages <- tail(result$packages, length(result$packages) - 1)
      result$quote <- tail(result$quote, length(result$quote) - 1)
   }
   
   result
   
})

.rs.addFunction("getCompletionsNamespace", function(token, string, exportsOnly, envir)
{
   result <- .rs.emptyCompletions()
   
   if (!(string %in% loadedNamespaces()))
   {
      tryCatch(
         suppressPackageStartupMessages(requireNamespace(string, quietly = TRUE)),
         error = function(e) NULL
      )
   }
   
   if (string %in% loadedNamespaces())
   {
      objects <- if (exportsOnly)
         getNamespaceExports(asNamespace(string))
      else
         objects(asNamespace(string), all.names = TRUE)
      completions <- .rs.selectFuzzyMatches(objects, token)
      
      result <- .rs.makeCompletions(
         token,
         completions,
         string,
         FALSE
      )
   }
   
   result
   
})


.rs.addFunction("emptyCompletions", function()
{
   .rs.makeCompletions(
      token = "",
      results = character(),
      packages = character(),
      quote = logical(),
      fguess = "",
      excludeOtherCompletions = .rs.scalar(FALSE),
      overrideInsertParens = .rs.scalar(FALSE)
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

.rs.addFunction("makeCompletions", function(token,
                                            results,
                                            packages = character(),
                                            quote = logical(),
                                            fguess = "",
                                            excludeOtherCompletions = FALSE,
                                            overrideInsertParens = FALSE,
                                            orderStartsWithAlnumFirst = TRUE)
{
   if (length(results) > 0)
   {
      if (length(packages) == 0)
         packages <- rep.int("", length(results))
      else if (length(packages) == 1)
         packages <- rep.int(packages, length(results))
      
      if (length(quote) == 0)
         quote <- rep.int(FALSE, length(results))
      else if (length(quote) == 1)
         quote <- rep.int(quote, length(results))
   }
   
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
   }
   
   list(token = token,
        results = results,
        packages = packages,
        quote = quote,
        fguess = fguess,
        excludeOtherCompletions = .rs.scalar(excludeOtherCompletions),
        overrideInsertParens = .rs.scalar(overrideInsertParens))
})

.rs.addFunction("subsetCompletions", function(completions, indices)
{
   completions$results <- completions$results[indices]
   completions$packages <- completions$packages[indices]
   completions$quote <- completions$quote[indices]
   
   completions
})

.rs.addFunction("appendCompletions", function(old, new)
{
   old$results <- c(old$results, new$results)
   old$packages <- c(old$packages, new$packages)
   old$quote <- c(old$quote, new$quote)
   
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
         if (as.character(call) == "[")
         {
            objectName <- parsed[[1]][[2]]
            object <- .rs.getAnywhere(objectName, envir = envir)
            if (inherits(object, "data.table"))
            {
               .rs.makeCompletions(
                  token,
                  names(object),
                  paste("[", string, "]", sep = ""),
                  FALSE
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

.rs.addFunction("getCompletionsDollar", function(token, string, envir, S4)
{
   result <- .rs.emptyCompletions()
   
   ## Blacklist certain evaluations
   if (!is.null(result <- .rs.blackListEvaluation(token, string, envir)))
      return(result)
   
   object <- .rs.getAnywhere(string, envir)
   if (!is.null(object))
   {
      names <- if (S4 && !inherits(object, "classRepresentation"))
         slotNames(object)
      else
         .rs.getNames(object)
      
      completions <- .rs.selectFuzzyMatches(names, token)
      result <- .rs.makeCompletions(
         token,
         completions,
         paste("[", string, "]", sep = ""),
         FALSE
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
         token,
         completions,
         paste("[", string, "]", sep = ""),
         !inherits(object, "data.table")
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
         token,
         completions,
         paste("[[", string, "]]", sep = ""),
         quote = TRUE,
         overrideInsertParens = TRUE
      )
   }
   
   result
   
})

.rs.addFunction("getCompletionsPackages", function(token, appendColons = FALSE)
{
   allPackages <- Reduce(union, lapply(.libPaths(), list.files))
   completions <- .rs.selectFuzzyMatches(allPackages, token)
   .rs.makeCompletions(token,
                       if (appendColons && length(completions))
                          paste(completions, "::", sep = "")
                       else
                          completions,
                       completions,
                       quote = !appendColons,
                       excludeOtherCompletions = TRUE)
})

.rs.addFunction("getCompletionsGetOption", function(token)
{
   allOptions <- names(options())
   .rs.makeCompletions(token,
                       .rs.selectFuzzyMatches(allOptions, token),
                       quote = TRUE,
                       excludeOtherCompletions = FALSE)   
})

.rs.addFunction("getCompletionsOptions", function(token)
{
   allOptions <- names(options())
   completions <- .rs.selectFuzzyMatches(allOptions, token)
   if (length(completions))
      completions <- paste(completions, "= ")
   
   .rs.makeCompletions(token,
                       completions,
                       excludeOtherCompletions = TRUE)
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
   
   keep <- .rs.fuzzyMatches(results, token)
   results <- results[keep]
   packages <- packages[keep]
   
   order <- order(results)
   results <- results[order]
   packages <- packages[order]
   
   .rs.makeCompletions(token,
                       results,
                       packages,
                       overrideInsertParens = overrideInsertParens)
   
})

.rs.addFunction("finishExpression", function(string)
{
   .Call("rs_finishExpression", as.character(string))
})

.rs.addFunction("getCompletionsAttr", function(token,
                                               functionCall)
{
   result <- tryCatch({
      wrapper <- function(x, which, exact = FALSE) {}
      matched <- .rs.matchCall(wrapper, functionCall)
      if (is.null(matched[["x"]]))
         return(.rs.emptyCompletions())
      objectName <- as.character(matched[["x"]])
      object <- .rs.getAnywhere(objectName)
      completions <- .rs.selectFuzzyMatches(
         names(attributes(object)),
         token
      )
      .rs.makeCompletions(token,
                          completions,
                          paste("attributes(", objectName, ")", sep = ""),
                          quote = TRUE,
                          excludeOtherCompletions = TRUE)
   }, error = function(e) .rs.emptyCompletions())
   result
})


utils:::rc.settings(files = TRUE)
.rs.addJsonRpcHandler("get_completions", function(token,
                                                  string,
                                                  type,
                                                  numCommas,
                                                  functionCallString,
                                                  chainObjectName,
                                                  additionalArgs,
                                                  excludeArgs,
                                                  excludeArgsFromObject)
{
   ## NOTE: these are passed in as lists of strings; convert to character
   additionalArgs <- as.character(additionalArgs)
   excludeArgs <- as.character(excludeArgs)
   
   ## Different completion types (sync with RCompletionManager.java)
   TYPE <- list(
      UNKNOWN = 0L,
      FUNCTION = 1L,
      SINGLE_BRACKET = 2L,
      DOUBLE_BRACKET = 3L,
      NAMESPACE_EXPORTED = 4L,
      NAMESPACE_ALL = 5L,
      DOLLAR = 6L,
      AT = 7L,
      FILE = 8L,
      CHUNK = 9L,
      ROXYGEN = 10L,
      HELP = 11L
   )
   
   ## Try to parse the function call string
   functionCall <- tryCatch({
      parse(text = .rs.finishExpression(functionCallString))[[1]]
   }, error = function(e) 
      NULL
   )
   
   ## Handle some special cases early
   
   # help
   if (TYPE$HELP %in% type)
      return(.rs.getCompletionsHelp(token))
   
   # Roxygen
   if (TYPE$ROXYGEN %in% type)
      return(.rs.attemptRoxygenTagCompletion(token))
   
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
      .rs.getCompletionsAttr(token, functionCall)
   }
   
   # getOption
   else if (string[[1]] == "getOption" && numCommas[[1]] == 0)
   {
      .rs.getCompletionsGetOption(token)
   }
   
   # options
   else if (string[[1]] == "options" && type == TYPE$FUNCTION)
   {
      .rs.getCompletionsOptions(token)
   }
   
   # no special case (start with empty completions)
   else
   {
      completions <- .rs.emptyCompletions()
   }
   
   for (i in seq_along(string))
   {
      discardFirst <- type[[i]] == TYPE$FUNCTION && chainObjectName != ""
      completions <- .rs.appendCompletions(
         completions,
         .rs.getRCompletions(token,
                             string[[i]],
                             type[[i]],
                             numCommas[[i]],
                             functionCall,
                             discardFirst,
                             parent.frame(),
                             TYPE)
      )
   }
   
   if (token != "" && 
          type[[1]] %in% c(TYPE$UNKNOWN, TYPE$FUNCTION,
                           TYPE$SINGLE_BRACKET, TYPE$DOUBLE_BRACKET))
      completions <- .rs.appendCompletions(
         completions,
         .rs.getCompletionsSearchPath(token)
      )
   
   ## File-based completions
   if (TYPE$FILE %in% type)
      completions <- .rs.appendCompletions(
         completions,
         .rs.getCompletionsFile(token)
      )
   
   ## Package completions (e.g. `stats::`)
   if (token != "" && length(type) == 1 && type == TYPE$UNKNOWN)
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
   if (type[[1]] %in% c(TYPE$FUNCTION, TYPE$UNKNOWN))
   {
      ## Blacklist certain functions
      if (string[[1]] %in% c("help", "str"))
      {
         completions$overrideInsertParens <- .rs.scalar(TRUE)
      }
      else
      {
         ## Blacklist based on formals of the function
         object <- .rs.getAnywhere(string[[1]], parent.frame())
         if (is.function(object))
         {
            argNames <- .rs.getFunctionArgumentNames(object)
            if (any(c("f", "fun", "func") %in% tolower(gsub("[^a-zA-Z]", "", argNames))))
               completions$overrideInsertParens <- .rs.scalar(TRUE)
         }
      }
   }
   
   completions$token <- token
   if (is.null(completions$fguess))
      completions$fguess <- ""
   
   completions$excludeOtherCompletions <- .rs.scalar(type[[1]] %in% c(
      TYPE$DOLLAR,
      TYPE$AT,
      TYPE$NAMESPACE_EXPORTED,
      TYPE$NAMESPACE_ALL
   ))
   
   if (is.null(completions$quote))
      completions$quote <- logical(length(completions$results))
   
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
         token,
         completions,
         leftDataName,
         TRUE,
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
         token,
         completions,
         rightDataName,
         TRUE,
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
            result <- .rs.makeCompletions(
               token,
               objectNames,
               paste("[", chainObjectName, "]", sep = ""),
               FALSE
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
            token,
            argsToAdd,
            paste("*", chainObjectName, "*", sep = "")
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
                                            envir,
                                            TYPE)
{   
   
   if (type %in% c(TYPE$DOLLAR, TYPE$AT))
      .rs.getCompletionsDollar(token, string, envir, type == TYPE$AT)
   else if (type %in% c(TYPE$NAMESPACE_EXPORTED, TYPE$NAMESPACE_ALL))
      .rs.getCompletionsNamespace(token, string, type == TYPE$NAMESPACE_EXPORTED, envir)
   else if (type == TYPE$FUNCTION)
      .rs.getCompletionsFunction(token, string, functionCall, discardFirst, envir)
   else if (type == TYPE$SINGLE_BRACKET)
      .rs.getCompletionsSingleBracket(token, string, numCommas, envir)
   else if (type == TYPE$DOUBLE_BRACKET)
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
   
   .rs.makeCompletions(token,
                       completions,
                       quote = grepl("[^a-zA-Z0-9._]", completions, perl = TRUE),
                       overrideInsertParens = TRUE)
   
})

.rs.addFunction("readAliases", function(path)
{
   if (file.exists(f <- file.path(path, "help", "aliases.rds")))
      names(readRDS(f))
   else
      character()
})
