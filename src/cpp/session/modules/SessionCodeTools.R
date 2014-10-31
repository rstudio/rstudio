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

.rs.addFunction("attemptRoxygenTagCompletion", function(line, cursorPos)
{
   line <- substr(line, 0, cursorPos)
   match <- grepl("^\\s*#+'\\s*@[a-zA-Z0-9]*$", line, perl=T)
   if (!match)
      return(NULL)
   
   tag <- sub(".*(?=@)", '', line, perl=T)
   
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
      );
   
   matchingTags <- grep(paste("^", tag, sep=""), tags, value=T)
   
   list(token=tag,
        results=matchingTags,
        packages=vector(mode='character', length=length(matchingTags)),
        fguess=c())
})

.rs.addFunction("getPendingInput", function()
{
   .Call("rs_getPendingInput")
})

.rs.addFunction("getAnywhere", function(name)
{
   objects <- getAnywhere(name)
   if (length(objects$objs))
   {
      objects$objs[[1]]
   }
   else
   {
      NULL
   }
})

.rs.addFunction("getBracketCompletions", function(line,
                                                  cursorPos)
{
   ## Try getting completions manually for `x[`, `x[[`
   result <- list(
      token = "",
      results = character(),
      packages = character(),
      fguess = character()
   )
   
   variable <- sub("^\\s*([a-zA-Z0-9.])\\[+\\s*$", "\\1", line, perl = TRUE)
   object <- .rs.getAnywhere(variable)
   if (!is.null(object))
   {
      names <- names(object)
      if (length(names))
      {
         quotedNames <- paste('"', names, '"', sep = "")
         result$results <- quotedNames
         result$packages <- setNames(
            character(length(quotedNames)),
            quotedNames
         )
      }
   }
   result
})

.rs.addFunction("getInternalRCompletions", function(line, cursorPos)
{
   utils:::.assignLinebuffer(line)
   utils:::.assignEnd(cursorPos)
   token <- utils:::.guessTokenFromLine()
   utils:::.completeToken()
   results <- utils:::.retrieveCompletions()
   status <- utils:::rc.status()
   
   packages = sub('^package:', '', .rs.which(results))
   
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

.rs.addFunction("getCompletionsFromObject", function(object, name)
{
   if (missing(name))
      name <- ""
   
   if (is.function(object))
   {
      list(
         results = paste(names(formals(object)), "= "),
         packages = rep.int(name, length(formals(object)))
      )
   }
   else
   {
      list(
         results = names(object),
         packages = rep.int(name, length(names(object)))
      )
   }
})

.rs.addFunction("getQuotedCompletion", function(line)
{
   name <- gsub("^\\s*([`'\"])(.*?)\\1.*", "\\2", line, perl = TRUE)
   object <- .rs.getAnywhere(name)
   .rs.getCompletionsFromObject(object, name)
})

utils:::rc.settings(files = TRUE)
.rs.addJsonRpcHandler("get_completions", function(line,
                                                  cursorPos,
                                                  objectName,
                                                  additionalArgs,
                                                  excludeArgs)
{
   roxygen <- .rs.attemptRoxygenTagCompletion(line, cursorPos)
   if (!is.null(roxygen))
      return(roxygen)
   
   if (missing(objectName))
      objectName <- NULL
   
   if (missing(additionalArgs))
      additionalArgs <- NULL
   
   if (missing(excludeArgs))
      excludeArgs <- NULL
   
   additionalArgs <- unlist(additionalArgs)
   excludeArgs <- unlist(excludeArgs)
   
   ## If objName has been provided, try to get completions from that as well.
   objCompletions <- NULL
   if (!is.null(objectName) && objectName != "")
   {
      object <- .rs.getAnywhere(objectName)
      if (length(object))
      {
         object <- objects$objs[[1]]
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
   
   if (grepl("\\[\\s*$", line, perl = TRUE))
   {
      result <- .rs.getBracketCompletions(line, cursorPos)
   }
   else if (grepl("^\\s*([`'\"]).*?\\1.*$", line, perl = TRUE))
   {
      result <- .rs.getQuotedCompletion(line)
   }
   else
   {
      result <- .rs.getInternalRCompletions(line, cursorPos)
   }
   
   if (is.null(result$fguess))
   {
      result$fguess <- character()
   }
   
   token <- gsub(".*[\\s\\[\\(\\{]", "", line, perl = TRUE)
   n <- nchar(token)
   result$token <- token
   
   if (!is.null(objCompletions))
   {
      keep <- .rs.startsWith(objCompletions$results, token)
      result$results <- c(objCompletions$results[keep], result$results)
      result$packages <- c(objCompletions$packages[keep], result$packages)
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
   
   result
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

.rs.addFunction("startsWith", function(strings, string)
{
   n <- nchar(string)
   (nchar(strings) >= n) & (substring(strings, 1, n) == string)
})