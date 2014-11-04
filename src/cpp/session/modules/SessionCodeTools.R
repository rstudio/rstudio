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

.rs.addFunction("endsWith", function(strings, string)
{
   nstrings <- nchar(strings)
   nstring <- nchar(string)
   (nstrings >= nstring) & 
      (substring(strings, nstrings - nstring + 1, nstrings) == string)
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
   objects <- getAnywhere(name, envir)
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
   
   list(token=token, 
        results=results.sorted, 
        packages=packages.sorted,
        fguess=status$fguess)
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
   
   result <- list(
      results = character(),
      packages = character(),
      fguess = ""
   )
   
   splat <- strsplit(string, ":{2,3}", perl = TRUE)[[1]]
   if (length(splat) == 1)
   {
      object <- .rs.getAnywhere(string, envir = envir)
      if (!is.null(object) && is.function(object))
      {
         formals <- names(formals(object))
         keep <- .rs.startsWith(formals, token)
         formals <- formals[keep]
         
         if (length(formals))
            result$results <- paste(formals, "= ")
         
         result$packages <- rep.int(string, length(formals))
         result$fguess <- string
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
            formals <- names(formals(object))
            keep <- .rs.startsWith(formals, token)
            formals <- formals[keep]
            
            if (length(formals))
               result$results <- paste(formals, "= ")
            
            result$packages <- rep.int(string, length(formals))
            result$fguess <- functionString
         }
      }
   }
   
   if (discardFirst)
   {
      result$results <- tail(result$results, length(result$results) - 1)
      result$packages <- tail(result$packages, length(result$packages) - 1)
   }
   result
   
})

.rs.addFunction("getCompletionsNamespace", function(token, string, exportsOnly)
{
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
      keep <- .rs.startsWith(objects, token)
      completions <- objects[keep]
      
      list(
         token = token,
         results = completions,
         packages = rep.int(string, length(completions)),
         fguess = "",
         excludeContext = .rs.scalar(TRUE)
      )
   }
   else
   {
      list(
         token = token,
         results = character(),
         packages = character(),
         fguess = "",
         excludeContext = .rs.scalar(TRUE)
      )
   }
})

.rs.addFunction("subsetCompletions", function(completions, indices)
{
   completions$results <- completions$results[indices]
   completions$packages <- completions$packages[indices]
   completions
})

.rs.addFunction("appendCompletions", function(old, new)
{
   old$results <- c(old$results, new$results)
   old$packages <- c(old$packages, new$packages)
   if (!is.null(new$fguess))
      old$fguess <- new$fguess
   if (!is.null(new$excludeContext))
      old$excludeContext <- new$excludeContext
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
               list(
                  token = token,
                  results = names(object),
                  packages = rep.int(
                     paste("[", string, "]", sep = ""), length(object)
                  ),
                  fguess = "",
                  excludeContext = .rs.scalar(TRUE)
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
         candidates <- ls(object)
         completions <- candidates[.rs.startsWith(candidates, token)]
         list(token = token,
              results = completions,
              packages = rep.int(
                 string,
                 length(completions)
              ),
              fguess = "",
              excludeContext = .rs.scalar(TRUE))
      }
   },
   error = function(e) NULL
   )
})

.rs.addFunction("getCompletionsDollar", function(token, string, envir)
{
   
   default <- list(token = token,
                   results = character(),
                   packages = character(),
                   fguess = "",
                   excludeContext = .rs.scalar(TRUE))
   
   .rs.withTimeLimit(0.15, fail = default, {
      
      ## Blacklist certain evaluations
      if (!is.null(result <- .rs.blackListEvaluation(token, string, envir)))
         return(result)
      
      ## Get completions for R6 objects
      if (!is.null(result <- .rs.getCompletionsDollarR6(token, string, envir)))
         return(result)
      
      parsed <- suppressWarnings(parse(text = string))
      evaled <- suppressWarnings(eval(parse(text = string), envir = envir))
      if (!is.null(evaled) & !is.null(names(evaled)))
      {
         names <- names(evaled)
         completions <- names[.rs.startsWith(names, token)]
         list(
            token = token,
            results = completions,
            packages = rep.int(
               paste("[", string, "]", sep = ""),
               length(completions)
            ),
            fguess = "",
            excludeContext = .rs.scalar(TRUE)
         )
      }
      else
      {
         default
      }
   })
})

.rs.addFunction("getCompletionsDoubleBracket", function(token,
                                                        string,
                                                        envir = parent.frame())
{
   result <- list(
      token = token,
      results = character(),
      packages = character(),
      fguess = ""
   )
   
   object <- .rs.getAnywhere(string, envir)
   if (!is.null(object) && !is.null(names(object)))
   {
      completions <- names(object)
      completions <- completions[.rs.startsWith(completions, token)]
      result$results <- paste('"', completions, '"', sep = "")
      result$packages <- character(length(completions))
   }
   
   result
   
})

utils:::rc.settings(files = TRUE)
utils:::rc.settings(ipck = TRUE)
.rs.addJsonRpcHandler("get_completions", function(content,
                                                  token,
                                                  string,
                                                  type,
                                                  numCommas,
                                                  chainObjectName,
                                                  additionalArgs,
                                                  excludeArgs)
{
   print(token)
   print(string)
   print(type)
   
   roxygen <- .rs.attemptRoxygenTagCompletion(token)
   if (!is.null(roxygen))
      return(roxygen)
   
   ## Different completion types (sync with RCompletionManager.java)
   TYPE_UNKNOWN <- 0L
   TYPE_FUNCTION <- 1L
   TYPE_SINGLE_BRACKET <- 2L
   TYPE_DOUBLE_BRACKET <- 3L
   TYPE_NAMESPACE_EXPORTED <- 4L
   TYPE_NAMESPACE_ALL <- 5L
   TYPE_DOLLAR <- 6L
   TYPE_FILE <- 7L
   TYPE_CHUNK <- 8L
   
   # Discard the first argument for function completions if we're
   # in a chain
   
   ## TODO: The caller should really pass in whether we want to discard
   ## the first argument.
   discardFirst <-
      string %in% c(
         "mutate", "summarise", "summarize", "rename", "transmute",
         "select", "rename_vars"
      ) &&
      (
         chainObjectName != "" || 
         length(additionalArgs) ||
         length(excludeArgs)
      )
   
   ## If we're completing after a '$' or an '@', then
   ## we don't need any other completions
   if (type == TYPE_DOLLAR)
      return(.rs.getCompletionsDollar(token, string, parent.frame()))
   else if (type %in% c(TYPE_NAMESPACE_EXPORTED, TYPE_NAMESPACE_ALL))
      return(.rs.getCompletionsNamespace(token, string, type == TYPE_NAMESPACE_EXPORTED))
   
   additionalArgs <- unlist(additionalArgs)
   excludeArgs <- unlist(excludeArgs)
   
   ## chainObjectName will be provided if the client detected
   ## that we were performing completions within an e.g. 
   ## `%>%` chain -- use completions from the associated data object.
   objCompletions <- NULL
   if (!is.null(chainObjectName) && chainObjectName != "")
   {
      object <- .rs.getAnywhere(chainObjectName, parent.frame())
      if (length(object))
      {
         nm <- names(object)
         if (length(nm))
         {
            objCompletions <- list(
               results = nm,
               packages = character(length(nm))
            )
         }
      }
   }
   
   result <- .rs.appendCompletions(
      .rs.getInternalRCompletions(token, type == TYPE_FILE),
      if (type == TYPE_FUNCTION)
         .rs.getCompletionsFunction(token, string, discardFirst, parent.frame())
      else if (type == TYPE_SINGLE_BRACKET)
         .rs.getCompletionsSingleBracket(token, string, parent.frame())
      else if (type == TYPE_DOUBLE_BRACKET)
         .rs.getCompletionsDoubleBracket(token, string, parent.frame())
   )
   
   if (is.null(result$fguess))
   {
      result$fguess <- character()
   }
   
   n <- nchar(token)
   result$token <- token
   
   if (!is.null(objCompletions))
   {
      result <- .rs.appendCompletions(
         result,
         .rs.subsetCompletions(
            objCompletions,
            .rs.startsWith(objCompletions$results, token)
         )
      )
   }
   
   if (length(additionalArgs))
   {
      keep <- .rs.startsWith(additionalArgs, token)
      result$results <- c(additionalArgs[keep], result$results)
      result$packages <- c(character(sum(keep)), result$packages)
   }
   
   if (length(excludeArgs))
   {
      keep <- which(!(result$results %in% excludeArgs))
      result$results <- result$results[keep]
      result$packages <- result$packages[keep]
   }
   
   if (is.null(result$excludeContext))
      result$excludeContext <- .rs.scalar(FALSE)
   
   if (is.null(result$dontInsertParens))
      result$dontInsertParens <- .rs.scalar(FALSE)
   
   ## Override param insertion if the function was 'debug' or 'trace'
   ## NOTE: This logic should be synced in 'RCompletionManager.java'.
   functionBlacklist <- c(
      "debug", "debugonce", "undebug", "isdebugged", "library", "require"
   )
   
   if (string %in% functionBlacklist ||
       .rs.endsWith(string, "ply"))
      result$dontInsertParens <- .rs.scalar(TRUE)
   
   result[
      c(
         "token",
         "results", 
         "packages", 
         "fguess", 
         "excludeContext", 
         "dontInsertParens"
      )
   ]
   
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
         object <- tryCatch(
            eval(call("$", container, nameString)),
            error = function(e) NULL
         )
      }
   }
   .rs.scalar(!is.null(object) && is.function(object))
})

