#
# SessionCodeTools.R
#
# Copyright (C) 2009-12 by RStudio, Inc.
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

.rs.addFunction("withTimeLimit", function(time,
                                          expr,
                                          envir = parent.frame(),
                                          fail = NULL)
{
   setTimeLimit(elapsed = time, transient = TRUE)
   on.exit(setTimeLimit(), add = TRUE)
   tryCatch(
      eval(expr, envir = envir),
      error = function(e) {
         fail
      }
   )
})

.rs.addFunction("startsWith", function(strings, string)
{
   n <- nchar(string)
   (nchar(strings) >= n) & (substring(strings, 1, n) == string)
})

.rs.addFunction("selectStartsWith", function(strings, string)
{
   strings[.rs.startsWith(strings, string)]
})

.rs.addFunction("endsWith", function(strings, string)
{
   nstrings <- nchar(strings)
   nstring <- nchar(string)
   (nstrings >= nstring) & 
      (substring(strings, nstrings - nstring + 1, nstrings) == string)
})

.rs.addFunction("selectEndsWith", function(strings, string)
{
   strings[.rs.endsWith(strings, string)]
})

# Return the scope names in which the given names exist
.rs.addFunction("which", function(names) {
   scopes = search()
   sapply(names, function(name) {
      for (scope in scopes) {
         if (exists(name, where=scope, inherits=F))
            return(scope)
      }
      return("")
   })
})

.rs.addFunction("guessToken", function(line, cursorPos)
{
   utils:::.assignLinebuffer(line)
   utils:::.assignEnd(cursorPos)
   utils:::.guessTokenFromLine()
})

.rs.addFunction("findFunctionNamespace", function(name, fromWhere)
{
   if (!identical(fromWhere, ""))
   {
      if ( ! (fromWhere %in% search()) )
         return ("")

      where = as.environment(fromWhere)
   }
   else
   {
      where = globalenv()
   }

   envList <- methods:::findFunction(name, where = where)
   if (length(envList) > 0)
   {
      env <- envList[[1]]
      if (identical(env, baseenv()))
      {
         return ("package:base")
      }
      else if (identical(env, globalenv()))
      {
         return(".GlobalEnv")
      }
      else
      {
         envName = attr(envList[[1]], "name")
         if (!is.null(envName))
            return (envName)
         else
            return ("")
      }
   }
   else
   {
      return ("")
   }
})

.rs.addFunction("getFunction", function(name, namespaceName)
{
   tryCatch(eval(parse(text = name),
                 envir = as.environment(namespaceName),
                 enclos = NULL),
            error = function(e) NULL)
})


.rs.addFunction("functionHasSrcRef", function(func)
{
   return (!is.null(attr(func, "srcref")))
})

.rs.addFunction("deparseFunction", function(func, useSource)
{
   control <- c("keepInteger", "keepNA")
   if (useSource)
     control <- append(control, "useSource")

   deparse(func, width.cutoff = 59, control = control)
})

.rs.addFunction("getS3MethodsForFunction", function(func)
{
  tryCatch(as.character(suppressWarnings(methods(func))),
           error = function(e) character())
})


# Return a list of S4 methods formatted as  functionName {className, className}
# NOTE: should call isGeneric prior to calling this (it will yield an error
# for functions that aren't generic)
.rs.addFunction("getS4MethodsForFunction", function(func)
{
  sigs <- findMethodSignatures(methods = findMethods(func))
  apply(sigs, 
        1, 
        function(sig)
        {
           paste(func, 
                 " {", 
                 paste(sig, collapse=", "),
                 "}",
                 sep="",
                 collapse = "")
        })
})

.rs.addFunction("getS4MethodNamespaceName", function(method)
{
  env <- environment(method)
  if (identical(env, baseenv()))
    return ("package:base")
  else if (identical(env, globalenv()))
    return (".GlobalEnv")
  else
  {
    envName <- environmentName(env)
    if (envName %in% search())
      return (envName)
    else
      paste("package:", envName, sep="")
  }
})

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

.rs.addFunction("getPendingInput", function()
{
   .Call("rs_getPendingInput")
})

.rs.addFunction("doStripSurrounding", function(string, complements)
{
   result <- gsub("^\\s*([`'\"])(.*?)\\1.*", "\\2", string, perl = TRUE)
   for (item in complements)
   {
      result <- sub(
         paste("^\\", item[[1]], "(.*)\\", item[[2]], "$", sep = ""),
         "\\1",
         result,
         perl = TRUE
      )
   }
   result
   
})

.rs.addFunction("stripSurrounding", function(string)
{
   complements <- list(
      c("(", ")"),
      c("{", "}"),
      c("[", "]"),
      c("<", ">")
   )
   
   result <- .rs.doStripSurrounding(string, complements)
   while (result != string)
   {
      string <- result
      result <- .rs.doStripSurrounding(string, complements)
   }
   result
})

.rs.addFunction("getAnywhere", function(name, envir = parent.frame())
{
   result <- NULL
 
   ## First, attempt to evaluate 'name' in 'envir'
   if (is.character(name)) {
      name <- .rs.stripSurrounding(name)
      result <- tryCatch({
         suppressWarnings(eval(parse(text = name), envir = envir))
      }, error = function(e) NULL
      )
   }
   
   if (is.language(name))
   {
      result <- tryCatch({
         suppressWarnings(eval(name, envir = envir))
      }, error = function(e) NULL
      )
   }
   
   ## Return on success
   if (!is.null(result))
   {
      return(result)
   }
   
   ## Otherwise, rely on 'getAnywhere'
   objects <- getAnywhere(name)
   if (length(objects$objs))
   {
      ## TODO: What if we have multiple completions?
      objects$objs[[1]]
   }
   else
   {
      NULL
   }
})

.rs.addFunction("getInternalRCompletions", function(token, isFileCompletion)
{
   if (isFileCompletion)
      token <- paste("\"", token, sep = "")
   
   if (token == "library" || token == "require")
      token <- paste(token, "(", sep = "")
   
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

.rs.addFunction("getFunctionArgumentNames", function(object)
{
   
   if (is.primitive(object))
   {
      ## Only closures have formals, not primitive functions.
      result <- tryCatch({
         parsed <- suppressWarnings(parse(text = capture.output(print(object)))[[1L]])
         names(parsed[[2]])
      }, error = function(e) {
         character()
      })
   }
   else
   {
      result <- names(formals(object))
   }
   result
})

.rs.addFunction("getCompletionsFunction", function(token,
                                                   string,
                                                   discardFirst,
                                                   envir = parent.frame())
{
   if (string == "library" || string == "require")
      return(
         .rs.getInternalRCompletions(
            paste(string, "(", token, sep = ""), FALSE
         )
      )
   
   result <- .rs.emptyCompletions()
   
   splat <- strsplit(string, ":{2,3}", perl = TRUE)[[1]]
   if (length(splat) == 1)
   {
      object <- .rs.getAnywhere(string, envir = envir)
      if (!is.null(object) && is.function(object))
      {
         formals <- .rs.selectStartsWith(
            .rs.getFunctionArgumentNames(object),
            token
         )
         
         if (length(formals))
            formals <- paste(formals, "= ")
         
         result <- .rs.makeCompletions(
            token,
            formals,
            string,
            fguess = string
         )
      }
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
         
         if (!is.null(object) && is.function(object))
         {
            formals <- .rs.selectStartsWith(
               .rs.getFunctionArgumentNames(object),
               token
            )
            
            if (length(formals))
               formals <- paste(formals, "= ")
            
            result <- .rs.makeCompletions(
               token,
               formals,
               string,
               fguess = string
            )
         }
      }
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
      includeContext = .rs.scalar(TRUE),
      overrideInsertParens = .rs.scalar(FALSE)
   )
})

.rs.addFunction("makeCompletions", function(token,
                                            results,
                                            packages = "",
                                            quote = FALSE,
                                            fguess = "",
                                            includeContext = TRUE,
                                            overrideInsertParens = FALSE)
{
   if (length(packages) <= 1)
      packages <- rep.int(packages, length(results))
   
   if (length(quote) <= 1)
      quote <- rep.int(quote, length(results))
   
   list(token = token,
        results = results,
        packages = packages,
        quote = quote,
        fguess = fguess,
        includeContext = .rs.scalar(includeContext),
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
   
   if (length(new$includeContext) && new$includeContext)
      old$includeContext <- new$includeContext
   
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

utils:::rc.settings(files = TRUE)
utils:::rc.settings(ipck = TRUE)
.rs.addJsonRpcHandler("get_completions", function(token,
                                                  string,
                                                  type,
                                                  numCommas,
                                                  chainObjectName,
                                                  additionalArgs,
                                                  excludeArgs)
{
   
   cat("Token: '", token, "'\n", sep = "")
   cat("String:\n")
   print(string)
   cat("Type:\n")
   print(type)
   cat("Number of commas:\n")
   print(numCommas)
   
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
   
   roxygen <- .rs.attemptRoxygenTagCompletion(token)
   if (!is.null(roxygen))
      return(roxygen)
   
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
                             discardFirst,
                             parent.frame(),
                             TYPES)
      )
   }
   
   cat("After getting R completions:\n")
   print(completions)
   
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
   
   cat("After getting chain completions:\n")
   print(completions)
   
   ## Override param insertion if the function was 'debug' or 'trace'
   if (type[[1]] %in% c(TYPES$FUNCTION, TYPES$UNKNOWN))
   {
      functionBlacklist <- c(
         "debug", "debugonce", "undebug", "isdebugged", "library", "require"
      )
      
      if (string[[1]] %in% functionBlacklist ||
             .rs.endsWith(string[[1]], "ply"))
      {
         completions$overrideInsertParens <- .rs.scalar(TRUE)
      }
   }
   
   completions$token <- token
   if (is.null(completions$fguess))
      completions$fguess <- ""
      
   completions$includeContext <- .rs.scalar(!(type[[1]] %in% c(
      TYPES$DOLLAR,
      TYPES$NAMESPACE_EXPORTED,
      TYPES$NAMESPACE_ALL
   )))
   
   if (is.null(completions$quote))
      completions$quote <- logical(length(completions$results))
   
   cat("Completions:\n")
   print(completions)
   
   completions
   
})

.rs.addFunction("getNames", function(object)
{
   if (inherits(object, "tbl") && "dplyr" %in% loadedNamespaces())
      dplyr::tbl_vars(object)
   else
      names(object)
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
         matched <- match.call(func, parsed)
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
         includeContext = FALSE
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
         includeContext = FALSE
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
         .rs.getCompletionsFunction(token, string, discardFirst, envir)
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

.rs.addJsonRpcHandler("get_help_at_cursor", function(line, cursorPos)
{
   token <- .rs.guessToken(line, cursorPos)
   if (token == '')
      return()

   pieces <- strsplit(token, ':{2,3}')[[1]]

   if (length(pieces) > 1)
      print(help(pieces[2], package=pieces[1], help_type='html'))
   else
      print(help(pieces[1], help_type='html', try.all.packages=T))
})

.rs.addJsonRpcHandler("is_function", function(nameString, envString)
{
   object <- NULL
   
   if (envString == "")
   {
      object <- .rs.getAnywhere(nameString, parent.frame())
   }
   else
   {
      envString <- .rs.stripSurrounding(envString)
      if (envString %in% search())
      {
         object <- tryCatch(
            get(nameString, pos = which(envString == search())),
            error = function(e) NULL
         )
      }
      else if (envString %in% loadedNamespaces())
      {
         object <- tryCatch(
            get(nameString, envir = asNamespace(envString)),
            error = function(e) NULL
         )
      }
      else if (!is.null(container <- .rs.getAnywhere(envString, parent.frame())))
      {
         if (isS4(container))
         {
            object <- tryCatch(
               eval(call("@", container, nameString)),
               error = function(e) NULL
            )
         }
         else
         {
            object <- tryCatch(
               eval(call("$", container, nameString)),
               error = function(e) NULL
            )
         }
      }
   }
   .rs.scalar(!is.null(object) && is.function(object))
})
