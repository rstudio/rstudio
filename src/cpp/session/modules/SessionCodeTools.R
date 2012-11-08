#
# SessionCodeTools.R
#
# Copyright (C) 2009-12 by RStudio, Inc.
#
# This program is licensed to you under the terms of version 3 of the
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
      "@aliases",
      "@author",
      "@concepts",
      "@description",
      "@details",
      "@docType",
      "@example",
      "@examples",
      "@export",
      "@exportClass",
      "@exportMethod",
      "@family",
      "@format",
      "@import",
      "@importClassesFrom",
      "@importFrom",
      "@importMethodsFrom",
      "@include",
      "@inheritParams",
      "@keywords",
      "@method",
      "@name",
      "@note",
      "@param",
      "@rdname",
      "@references",
      "@return",
      "@S3method",
      "@section",
      "@seealso",
      "@source",
      "@template",
      "@templateVar",
      "@title",
      "@usage",
      "@useDynLib"
      );
   
   matchingTags <- grep(paste("^", tag, sep=""), tags, value=T)
   
   list(token=tag,
        results=matchingTags,
        packages=vector(mode='character', length=length(matchingTags)),
        fguess=c())
})

utils:::rc.settings(files=T)
.rs.addJsonRpcHandler("get_completions", function(line, cursorPos)
{
   roxygen <- .rs.attemptRoxygenTagCompletion(line, cursorPos)
   if (!is.null(roxygen))
      return(roxygen);
   
   utils:::.assignLinebuffer(line)
   utils:::.assignEnd(cursorPos)
   token = utils:::.guessTokenFromLine()
   utils:::.completeToken()
   results = utils:::.retrieveCompletions()
   status = utils:::rc.status()
   
   packages = sub('^package:', '', .rs.which(results))

   choose = packages == '.GlobalEnv'
   results.sorted = c(results[choose], results[!choose])
   packages.sorted = c(packages[choose], packages[!choose])
   
   packages.sorted = sub('^\\.GlobalEnv$', '', packages.sorted)
   
   list(token=token, 
        results=results.sorted, 
        packages=packages.sorted, 
        fguess=status$fguess)
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
