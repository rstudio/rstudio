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
   match <- grepl("@[a-zA-Z0-9]*$", token, perl = TRUE)
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
   
   list(token=tag,
        results=matchingTags,
        packages=vector(mode='character', length=length(matchingTags)),
        fguess=c())
})

.rs.addFunction("getInternalRCompletions", function(token, isFileCompletion)
{
   if (isFileCompletion)
      token <- paste("\"", token, sep = "")
   
   utils:::.assignLinebuffer(token)
   utils:::.assignEnd(nchar(token))
   token <- utils:::.guessTokenFromLine()
   utils:::.completeToken()
   results <- utils:::.retrieveCompletions()
   status <- utils:::rc.status()
   
   if (isFileCompletion)
      packages <- rep.int("<file>", length(results))
   else
      packages <- sub('^package:', '', .rs.which(results))
   
   ## Fix up qualified 'packages', so that e.g. 'stats::rnorm'
   ## is parsed as 'rnorm'
   whichQualified <- grep(":{2,3}", results, perl = TRUE)
   packages[whichQualified] <- gsub(":.*", "", packages[whichQualified], perl = TRUE)
   
   # prefer completions for function arguments
   if (length(results) > 0) {
      n <- nchar(results)
      isFunctionArg <- substring(results, n, n) == "="
      idx <- c(which(isFunctionArg), which(!isFunctionArg))
      results <- results[idx]
      packages <- packages[idx]
   }
   
   # ensure spaces around =
   results <- sub("=$", " = ", results)
   
   choose = packages == '.GlobalEnv'
   results.sorted = c(results[choose], results[!choose])
   packages.sorted = c(packages[choose], packages[!choose])
   
   packages.sorted = sub('^\\.GlobalEnv$', '', packages.sorted)
   
   .rs.makeCompletions(
      token,
      results.sorted,
      packages.sorted,
      fguess = status$fguess
   )
   
})

.rs.addFunction("resolveFormals", function(token,
                                           object,
                                           functionCall,
                                           matchedCall,
                                           envir)
{
   tryCatch({
      
      parent <- functionCall
      node <- functionCall[[1]]
      while (!is.symbol(node))
      {
         parent <- node
         node <- parent[[1]]
      }
      
      if (as.character(node) %in% c("::", ":::"))
         functionName <- as.character(functionCall[[1]][[3]])
      else
         functionName <- as.character(functionCall[[1]])
      
      if (length(functionCall) > 1 && .rs.isS3Generic(functionName, envir))
      {
         s3methods <- .rs.getS3MethodsForFunction(functionName, envir)
         objectForDispatch <- .rs.getAnywhere(matchedCall[[2]], envir)
         classes <- class(objectForDispatch)
         
         for (class in c(classes, "default"))
         {
            methodName <- paste(functionName, class, sep = ".")
            method <- .rs.getAnywhere(methodName)
            if (!is.null(method))
            {
               formals <- .rs.getFunctionArgumentNames(method)
               methods <- rep.int(methodName, length(formals))
               if (length(parent) > 1)
               {
                  formals <- formals[-1]
                  methods <- methods[-1]
               }
               break
            }
         }
      }
      else
      {
         formals <- .rs.getFunctionArgumentNames(object)
         methods <- rep.int(functionName, length(formals))
      }
      
      keep <- .rs.startsWith(formals, token) & 
         !duplicated(formals) &
         !(formals %in% names(functionCall))
      
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
      formals <- .rs.resolveFormals(token, object, functionCall, matchedCall, envir)
      
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
         fguess = fguess
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
      completions <- .rs.selectStartsWith(objects, token)
      
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

.rs.addFunction("makeCompletions", function(token,
                                            results,
                                            packages = "",
                                            quote = FALSE,
                                            fguess = "",
                                            excludeOtherCompletions = FALSE,
                                            overrideInsertParens = FALSE)
{
   if (length(packages) <= 1 && length(results) > 1)
      packages <- rep.int(packages, length(results))
   
   if (length(quote) <= 1 && length(results) > 1)
      quote <- rep.int(quote, length(results))
   
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

.rs.addFunction("getCompletionsDollarR6", function(token, string, envir)
{
   tryCatch({
      object <- eval(parse(text = string), envir = envir)
      if (inherits(object, "R6") || inherits(object, "R6ClassGenerator"))
      {
         completions <- .rs.selectStartsWith(ls(object), token)
         .rs.makeCompletions(
            token,
            completions,
            string,
            FALSE
         )
      }
   },
   error = function(e) NULL
   )
})

.rs.addFunction("getCompletionsDollar", function(token, string, envir, S4)
{
   
   result <- .rs.emptyCompletions()
   
   ## Blacklist certain evaluations
   if (!is.null(result <- .rs.blackListEvaluation(token, string, envir)))
      return(result)
   
   ## Get completions for R6 objects
   if (!is.null(result <- .rs.getCompletionsDollarR6(token, string, envir)))
      return(result)
   
   parsed <- suppressWarnings(parse(text = string))
   evaled <- suppressWarnings(eval(parsed, envir = envir))
   if (!is.null(evaled))
   {
      names <- character()
      if (S4)
         names <- slotNames(evaled)
      else if (inherits(evaled, "tbl") && "dplyr" %in% loadedNamespaces())
         names <- dplyr::tbl_vars(evaled)
      else if (!is.null(names(evaled)))
         names <- names(evaled)
      
      completions <- .rs.selectStartsWith(names, token)
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
   
   object <- .rs.getAnywhere(string, envir)
   if (is.null(object))
      return(result)
   
   completions <- character()
   if (isS4(object))
   {
      completions <- slotNames(object)
   }
   else if (is.array(object) && !is.null(dn <- dimnames(object)))
   {
      if (numCommas + 1 <= length(dn))
         completions <- dimnames(object)[[numCommas + 1]]
   }
   else if (inherits(object, "data.table"))
   {
      completions <- names(object)
   }
   else if (inherits(object, "tbl") && "dplyr" %in% loadedNamespaces())
   {
      completions <- dplyr::tbl_vars(object)
   }
   else if (!is.null(names(object)))
   {
      completions <- names(object)
   }
   
   completions <- .rs.selectStartsWith(completions, token)
   
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
   object <- .rs.getAnywhere(string, envir)
   if (is.null(object))
      return(result)
   
   completions <- character()
   completions <- if (isS4(object))
   {
      completions <- slotNames(object)
   }
   else if (!is.null(names(object)))
   {
      completions <- names(object)
   }
   
   completions <- .rs.selectStartsWith(completions, token)
   
   if (length(completions))
   {
      result <- .rs.makeCompletions(
         token,
         completions,
         paste("[[", string, "]]", sep = ""),
         TRUE
      )
   }
   
   result
   
})

.rs.addFunction("getCompletionsPackages", function(token)
{
   allPackages <- Reduce(union, lapply(.libPaths(), list.files))
   completions <- .rs.selectStartsWith(allPackages, token)
   .rs.makeCompletions(token,
                       completions,
                       completions,
                       quote = TRUE,
                       excludeOtherCompletions = TRUE)
})

.rs.addFunction("getCompletionsGetOption", function(token)
{
   allOptions <- names(options())
   .rs.makeCompletions(token,
                       .rs.selectStartsWith(allOptions, token),
                       quote = TRUE,
                       excludeOtherCompletions = FALSE)   
})

.rs.addFunction("getCompletionsOptions", function(token)
{
   allOptions <- names(options())
   completions <- .rs.selectStartsWith(allOptions, token)
   if (length(completions))
      completions <- paste(completions, "= ")
   
   .rs.makeCompletions(token,
                       completions,
                       excludeOtherCompletions = TRUE)
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
      if (is.null(matched[["x"]]) || !is.null(matched[["which"]]))
         return(.rs.emptyCompletions())
      objectName <- as.character(matched[["x"]])
      object <- .rs.getAnywhere(objectName)
      completions <- .rs.selectStartsWith(
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
                                                  excludeArgs)
{
   ## NOTE: these are passed in as lists of strings; convert to character
   additionalArgs <- as.character(additionalArgs)
   excludeArgs <- as.character(excludeArgs)
   
   ## Different completion types (sync with RCompletionManager.java)
   TYPES <- list(
      UNKNOWN = 0L,
      FUNCTION = 1L,
      SINGLE_BRACKET = 2L,
      DOUBLE_BRACKET = 3L,
      NAMESPACE_EXPORTED = 4L,
      NAMESPACE_ALL = 5L,
      DOLLAR = 6L,
      AT = 7L,
      FILE = 8L,
      CHUNK = 9L
   )
   
   ## Try to parse the function call string
   functionCall <- tryCatch({
      parse(text = .rs.finishExpression(functionCallString))[[1]]
   }, error = function(e) 
      NULL
   )
   
   ## Handle some special cases early
   
   # Roxygen
   roxygen <- .rs.attemptRoxygenTagCompletion(token)
   if (!is.null(roxygen))
      return(roxygen)
   
   # library, require, requireNamespace, loadNamespace
   if (string[[1]] %in% c("library", "require", "requireNamespaces") &&
          numCommas[[1]] == 0)
   {
      return(.rs.getCompletionsPackages(token))
   }
   
   ## attr
   if (string[[1]] == "attr")
   {
      result <- .rs.getCompletionsAttr(token, functionCall)
      if (!.rs.isEmptyCompletion(result))
         return(result)
   }
   
   # getOption
   if (string[[1]] == "getOption" && numCommas[[1]] == 0)
   {
      return(.rs.getCompletionsGetOption(token))
   }
   
   # options
   if (string[[1]] == "options" && type == TYPES$FUNCTION)
   {
      return(.rs.getCompletionsOptions(token))
   }
   
   completions <- .rs.emptyCompletions()
   
   for (i in seq_along(string))
   {
      discardFirst <- type[[i]] == TYPES$FUNCTION && chainObjectName != ""
      completions <- .rs.appendCompletions(
         completions,
         .rs.getRCompletions(token,
                             string[[i]],
                             type[[i]],
                             numCommas[[i]],
                             functionCall,
                             discardFirst,
                             parent.frame(),
                             TYPES)
      )
   }
   
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
                               discardFirst,
                               parent.frame())
   )
   
   ## Override param insertion if the function was 'debug' or 'trace'
   if (type[[1]] %in% c(TYPES$FUNCTION, TYPES$UNKNOWN))
   {
      ## Try getting the function
      object <- .rs.getAnywhere(string[[1]], parent.frame())
      if (is.function(object))
      {
         argNames <- .rs.getFunctionArgumentNames(object)
         if (any(c("f", "fun", "func") %in% tolower(gsub("[^a-zA-Z]", "", argNames))))
            completions$overrideInsertParens <- .rs.scalar(TRUE)
      }
   }
   
   completions$token <- token
   if (is.null(completions$fguess))
      completions$fguess <- ""
   
   completions$excludeOtherCompletions <- .rs.scalar(type[[1]] %in% c(
      TYPES$DOLLAR,
      TYPES$NAMESPACE_EXPORTED,
      TYPES$NAMESPACE_ALL
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
      completions <- .rs.selectStartsWith(
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
      completions <- .rs.selectStartsWith(
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
                                                 discardFirst,
                                                 envir)
{
   ## chainObjectName will be provided if the client detected
   ## that we were performing completions within an e.g. 
   ## `%>%` chain -- use completions from the associated data object.
   result <- .rs.emptyCompletions()
   
   if (!is.null(chainObjectName) && chainObjectName != "")
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
      argsToAdd <- .rs.selectStartsWith(additionalArgs, token)
      result <- .rs.appendCompletions(
         result, 
         .rs.makeCompletions(
            token,
            argsToAdd
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
                                            TYPES)
{   
   
   if (type == TYPES$DOLLAR || type == TYPES$AT)
      return(.rs.getCompletionsDollar(token, string, envir, type == TYPES$AT))
   else if (type %in% c(TYPES$NAMESPACE_EXPORTED, TYPES$NAMESPACE_ALL))
      return(.rs.getCompletionsNamespace(token, string, type == TYPES$NAMESPACE_EXPORTED, envir))
   
   ourCompletions <-
      if (type == TYPES$FUNCTION)
         .rs.getCompletionsFunction(token, string, functionCall, discardFirst, envir)
   else if (type == TYPES$SINGLE_BRACKET)
      .rs.getCompletionsSingleBracket(token, string, numCommas, envir)
   else if (type == TYPES$DOUBLE_BRACKET)
      .rs.getCompletionsDoubleBracket(token, string, envir)
   else
      .rs.emptyCompletions()
   
   internalRCompletions <- .rs.getInternalRCompletions(token, type == TYPES$FILE)
   
   .rs.appendCompletions(ourCompletions,
                         internalRCompletions)
   
})
