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

.rs.addFunction("error", function(...)
{
   list(
      result  = NULL,
      message = .rs.scalar(paste(..., sep = ""))
   )
})

.rs.addFunction("success", function(result = NULL)
{
   list(
      result  = result,
      message = NULL
   )
})

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
   if (!length(string))
      string <- ""
   
   n <- nchar(string)
   (nchar(strings) >= n) & (substring(strings, 1, n) == string)
})

.rs.addFunction("selectStartsWith", function(strings, string)
{
   strings[.rs.startsWith(strings, string)]
})

.rs.addFunction("endsWith", function(strings, string)
{
   if (!length(string))
      string <- ""
   
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

.rs.addFunction("isS3Generic", function(object)
{
   if (!is.function(object))
      return(FALSE)
   
   if (inherits(object, "groupGenericFunction"))
      return(TRUE)
   
   .rs.callsUseMethod(body(object))
   
})

.rs.addFunction("callsUseMethod", function(x)
{
   if (missing(x))
      return(FALSE)
   
   if (!is.call(x))
      return(FALSE)
   
   if (identical(x[[1]], quote(UseMethod)))
      return(TRUE)
   
   if (length(x) == 1)
      return(FALSE)
   
   for (arg in as.list(x[-1]))
      if (.rs.callsUseMethod(arg))
         return(TRUE)
   
   FALSE
})

.rs.addFunction("getS3MethodsForFunction", function(func, envir = parent.frame())
{
  tryCatch({
     call <- call("methods", func)
     as.character(suppressWarnings(eval(call, envir = envir)))
  }, error = function(e) character())
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

.rs.addFunction("getPendingInput", function()
{
   .Call(.rs.routines$rs_getPendingInput)
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

.rs.addFunction("resolveObjectSource", function(object, envir)
{
   # Try to find the associated namespace of the object
   namespace <- NULL
   if (is.primitive(object))
      namespace <- "base"
   else if (is.function(object))
   {
      envString <- capture.output(environment(object))[1]
      match <- regexpr("<environment: namespace:(.*)>", envString, perl = TRUE)
      if (match == -1L)
         return()
      
      start <- attr(match, "capture.start")[1]
      end <- start + attr(match, "capture.length")[1]
      namespace <- substring(envString, start, end - 1)
   }
   else if (isS4(object))
      namespace <- attr(class(object), "package")
   
   if (is.null(namespace))
      return()
   
   # Get objects from that namespace
   ns <- asNamespace(namespace)
   objectNames <- objects(ns, all.names = TRUE)
   objects <- tryCatch(
      mget(objectNames, envir = ns),
      error = function(e) NULL
   )
   
   if (is.null(objects))
      return()
   
   # Find which object is actually identical to the one we have
   success <- FALSE
   for (i in seq_along(objects))
   {
      if (identical(object, objects[[i]], ignore.environment = TRUE))
      {
         success <- TRUE
         break
      }
   }
   
   # Use that name for the help lookup
   if (success)
      return(list(
         name = objectNames[[i]],
         package = namespace
      ))
   
})

.rs.addFunction("resolveAliasedSymbol", function(object)
{
   if (!is.function(object))
      return(object)
   
   if (is.primitive(object))
      return(object)
   
   body <- body(object)
   env <- environment(object)
   
   if (length(body) && .rs.isSymbolCalled(body[[1]], ".rs.callAs"))
      return(env$original)
   
   return(object)
})

.rs.addFunction("getAnywhere", function(name, envir = parent.frame())
{
   result <- NULL
   
   if (!length(name))
      return(NULL)
   
   if (is.character(name) && (length(name) != 1 || name == ""))
      return(NULL)
   
   # Don't evaluate any functions -- blacklist any 'name' that contains a paren
   if (is.character(name) && regexpr("(", name, fixed = TRUE) > 0)
      return(FALSE)
   
   if (is.character(name) && is.character(envir))
   {
      # If envir is the name of something on the search path, get it from there
      pos <- match(envir, search(), nomatch = -1L)
      if (pos >= 0)
      {
         object <- tryCatch(
            get(name, pos = pos),
            error = function(e) NULL
         )
         
         if (!is.null(object))
            return(.rs.resolveAliasedSymbol(object))
      }
      
      # Otherwise, maybe envir is the name of a package -- search there
      if (envir %in% loadedNamespaces())
      {
         object <- tryCatch(
            get(name, envir = asNamespace(envir)),
            error = function(e) NULL
         )
         
         if (!is.null(object))
            return(.rs.resolveAliasedSymbol(object))
      }
   }
   
   if (is.character(name))
   {
      name <- .rs.stripSurrounding(name)
      name <- tryCatch(
         suppressWarnings(parse(text = name)),
         error = function(e) NULL
      )
      
      if (is.null(name))
         return(NULL)
   }
   
   if (is.language(name))
   {
      result <- tryCatch(
         eval(name, envir = envir),
         error = function(e) NULL
      )
   }
   
   .rs.resolveAliasedSymbol(result)
   
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

.rs.addFunction("getNames", function(object)
{
   tryCatch({
      if (is.environment(object))
         ls(object, all.names = TRUE)
      else if (inherits(object, "tbl") && "dplyr" %in% loadedNamespaces())
         dplyr::tbl_vars(object)
      else
         names(object)
   }, error = function(e) NULL)
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

.rs.addJsonRpcHandler("execute_r_code", function(code)
{
   .envir <- parent.frame(2)
   result <- .rs.withTimeLimit(2, fail = "", envir = .envir, {
   
      output <- capture.output(
         evaled <- suppressWarnings(
            eval(parse(text = code), envir = .envir)
         )
      )
      
      object <- if (length(output))
         output
      else
         evaled
      
      paste(as.character(object), collapse = "\n")
   })
   .rs.scalar(result)
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

.rs.addFunction("asCaseInsensitiveRegex", function(string)
{
   if (string == "")
      return(string)
   
   splat <- strsplit(string, "", fixed = TRUE)[[1]]
   lowerSplat <- tolower(splat)
   upperSplat <- toupper(splat)
   result <- vapply(1:length(splat), FUN.VALUE = character(1), USE.NAMES = FALSE, function(i) {
      if (lowerSplat[i] == upperSplat[i])
         splat[i]
      else
         paste("[", lowerSplat[i], upperSplat[i], "]", sep = "")
   })
   paste(result, collapse = "")
})

.rs.addFunction("asCaseInsensitiveSubsequenceRegex", function(string)
{
   if (string == "")
      return(string)
   
   splat <- strsplit(string, "", fixed = TRUE)[[1]]
   lowerSplat <- tolower(splat)
   upperSplat <- toupper(splat)
   
   result <- vapply(1:length(splat), FUN.VALUE = character(1), USE.NAMES = FALSE, function(i) {
      if (lowerSplat[i] == upperSplat[i])
         splat[i]
      else
         paste("[", lowerSplat[i], upperSplat[i], "]", sep = "")
   })
   
   negated <- vapply(1:length(splat), FUN.VALUE = character(1), USE.NAMES = FALSE, function(i) {
      if (lowerSplat[i] == upperSplat[i])
         paste("[^", splat[i], "]*", sep = "")
      else
         paste("[^", lowerSplat[i], upperSplat[i], "]*", sep = "")
   })
   
   negated <- c(negated[-1L], "")
   paste(result, negated, sep = "", collapse = "")
})

.rs.addFunction("isSubsequence", function(strings, string)
{
   .Call(.rs.routines$rs_isSubsequence, strings, string)
})

.rs.addFunction("whichIsSubsequence", function(strings, string)
{
   which(.rs.isSubsequence(strings, string))
})

.rs.addFunction("selectIsSubsequence", function(strings, string)
{
   .subset(strings, .rs.isSubsequence(strings, string))
})

.rs.addFunction("escapeForRegex", function(regex)
{
   gsub("([\\-\\[\\]\\{\\}\\(\\)\\*\\+\\?\\.\\,\\\\\\^\\$\\|\\#\\s])", "\\\\\\1", regex, perl = TRUE)
})

.rs.addFunction("objectsOnSearchPath", function(token = "",
                                                caseInsensitive = FALSE,
                                                excludeGlobalEnv = FALSE)
{
   search <- search()
   startIdx <- 1
   range <- 1:length(search)
   
   if (excludeGlobalEnv)
   {
      startIdx <- 2
      search <- search[-1]
      range <- range[-1]
   }
   
   if (nzchar(token))
   {
      token <- .rs.escapeForRegex(token)
      if (caseInsensitive)
         token <- .rs.asCaseInsensitiveRegex(token)
      pattern <- paste("^", token, sep = "")
      
      objects <- lapply(range, function(i) {
         ls(pos = i, all.names = TRUE, pattern = pattern)
      })
   }
   else
   {
      objects <- lapply(range, function(i) {
         ls(pos = i, all.names = TRUE)
      })
   }
   
   names(objects) <- search
   objects
})

.rs.addFunction("assign", function(x, value)
{
   pos <- which(search() == "tools:rstudio")
   if (length(pos))
      assign(paste(".rs.cache.", x, sep = ""), value, pos = pos)
})

.rs.addFunction("get", function(x)
{
   pos <- which(search() == "tools:rstudio")
   if (length(pos))
      tryCatch(
         get(paste(".rs.cache.", x, sep = ""), pos = pos),
         error = function(e) NULL
      )
})

.rs.addFunction("mget", function(x = NULL)
{
   pos <- which(search() == "tools:rstudio")
   if (length(pos))
      tryCatch({
         
         objects <- if (is.null(x))
            .rs.selectStartsWith(objects(pos = pos, all.names = TRUE), ".rs.cache")
         else
            paste(".rs.cache.", x, sep = "")
         
         mget(objects, envir = as.environment(pos))
      },
         error = function(e) NULL
      )
})

.rs.addFunction("packageNameForSourceFile", function(filePath)
{
   .Call(.rs.routines$rs_packageNameForSourceFile, filePath)
})

.rs.addFunction("isRScriptInPackageBuildTarget", function(filePath)
{
   .Call(.rs.routines$rs_isRScriptInPackageBuildTarget, filePath)
})

.rs.addFunction("namedVectorAsList", function(vector)
{
   # Early escape for zero-length vectors
   if (!length(vector))
   {
      return(list(
         values = NULL,
         names = NULL
      ))
   }
   
   values <- unlist(vector, use.names = FALSE)
   vectorNames <- names(vector)
   names <- unlist(lapply(1:length(vector), function(i) {
      rep.int(vectorNames[i], length(vector[[i]]))
   }))
   
   list(values = values,
        names = names)
})

.rs.addFunction("getDollarNamesMethod", function(object)
{
   classes <- class(object)
   for (class in classes)
   {
      method <- .rs.getAnywhere(paste(".DollarNames", class, sep = "."))
      if (!is.null(method))
         return(method)
   }
   NULL
})

.rs.addJsonRpcHandler("get_args", function(name, src)
{
   if (identical(src, ""))
      src <- NULL
   
   result <- .rs.getSignature(.rs.getAnywhere(name, src))
   result <- sub("function ", "", result)
   .rs.scalar(result)
})

.rs.addFunction("getActiveArgument", function(object,
                                              matchedCall)
{
   allArgs <- .rs.getFunctionArgumentNames(object)
   matchedArgs <- names(matchedCall)[-1L]
   qualifiedArgsInCall <- setdiff(matchedArgs, "")
   setdiff(allArgs, qualifiedArgsInCall)[1]
})

.rs.addFunction("swap", function(vector, ..., default)
{
   dotArgs <- list(...)
   
   nm <- names(dotArgs)
   
   to <- unlist(lapply(seq_along(dotArgs), function(i)
      rep(nm[i], each = length(dotArgs[[i]]))
   ))
   
   from <- unlist(dotArgs)
   
   tmp <- to[match(vector, from)]
   tmp[is.na(tmp)] <- default
   tmp
})

.rs.addFunction("scoreMatches", function(strings, string)
{
   .Call(.rs.routines$rs_scoreMatches, strings, string)
})

.rs.addFunction("getProjectDirectory", function()
{
   .Call(.rs.routines$rs_getProjectDirectory)
})

.rs.addFunction("hasFileMonitor", function()
{
   .Call(.rs.routines$rs_hasFileMonitor)
})

.rs.addFunction("listIndexedFiles", function(term = "",
                                             inDirectory = .rs.getProjectDirectory(),
                                             maxCount = 200L)
{
   if (is.null(.rs.getProjectDirectory()))
      return(NULL)
   
   .Call(.rs.routines$rs_listIndexedFiles,
         term,
         suppressWarnings(.rs.normalizePath(inDirectory)),
         as.integer(maxCount))
})

.rs.addFunction("listIndexedFolders", function(term = "",
                                               inDirectory = .rs.getProjectDirectory(),
                                               maxCount = 200L)
{
   if (is.null(inDirectory))
      return(character())
   
   .Call(.rs.routines$rs_listIndexedFolders, term, inDirectory, maxCount)
})

.rs.addFunction("listIndexedFilesAndFolders", function(term = "",
                                                       inDirectory = .rs.getProjectDirectory(),
                                                       maxCount = 200L)
{
   if (is.null(inDirectory))
      return(character())
   
   .Call(.rs.routines$rs_listIndexedFilesAndFolders, term, inDirectory, maxCount)
})

.rs.addFunction("doGetIndex", function(term = "",
                                       inDirectory = .rs.getProjectDirectory(),
                                       maxCount = 200L,
                                       getter)
{
   if (is.null(inDirectory))
      return(character())
   
   inDirectory <- suppressWarnings(
      .rs.normalizePath(inDirectory)
   )
   
   index <- getter(term, inDirectory, maxCount)
   
   if (is.null(index))
   {
      return(list(
         paths = character(),
         more_available = FALSE
      ))
   }
   
   paths <- suppressWarnings(.rs.normalizePath(index$paths))
   scores <- .rs.scoreMatches(basename(paths), term)
   index$paths <- paths[order(scores)]
   index
   
})

.rs.addFunction("getIndexedFiles", function(term = "",
                                            inDirectory = .rs.getProjectDirectory(),
                                            maxCount = 200L)
{
   .rs.doGetIndex(term, inDirectory, maxCount, .rs.listIndexedFiles)
})

.rs.addFunction("getIndexedFolders", function(term = "",
                                              inDirectory = .rs.getProjectDirectory(),
                                              maxCount = 200L)
{
   .rs.doGetIndex(term, inDirectory, maxCount, .rs.listIndexedFolders)
})

.rs.addFunction("getIndexedFilesAndFolders", function(term = "",
                                                      inDirectory = .rs.getProjectDirectory(),
                                                      maxCount = 200L)
{
   .rs.doGetIndex(term, inDirectory, maxCount, .rs.listIndexedFilesAndFolders)
})

## NOTE: Function may be used by async R process; must not call back into
## 'rs_' compiled code!
.rs.addFunction("jsonEscapeString", function(value)
{
   if (is.na(value)) return("null")
   chars <- strsplit(value, "", fixed = TRUE)[[1]]
   chars <- vapply(chars, function(x) {
      if (x %in% c('"', '\\', '/'))
         paste('\\', x, sep = '')
      else if (charToRaw(x) < 20)
         paste('\\u', toupper(format(as.hexmode(as.integer(charToRaw(x))), 
                                     width = 4)), 
               sep = '')
      else
         x
   }, character(1))
   paste(chars, sep = "", collapse = "")
})

## NOTE: Function may be used by async R process; must not call back into
## 'rs_' compiled code!
.rs.addFunction("jsonProperty", function(name, value)
{
   paste(sep = "",
         "\"", 
         .rs.jsonEscapeString(enc2utf8(name)), 
         "\":\"",
         .rs.jsonEscapeString(enc2utf8(value)), 
         "\""
   )
})

## NOTE: Specify that a JSON value should be returned as a scalar by
## giving it the 'AsIs' class; ie, by writing 'foo = I(1)', or otherwise
## by using the '.rs.scalar' function.
## 
## NOTE: Function may be used by async R process; must not call back into
## 'rs_' compiled code!
##
## NOTE: lists are considered as objects if they are named, and as arrays if
## they are not. If you have an empty list that you want to treat as an object,
## you must give it a names attribute.
.rs.addFunction("toJSON", function(object)
{
   AsIs <- inherits(object, "AsIs") || inherits(object, ".rs.scalar")
   if (is.list(object))
   {
      if (is.null(names(object)))
      {
         return(paste('[', paste(lapply(seq_along(object), function(i) {
            .rs.toJSON(object[[i]])
         }), collapse = ','), ']', sep = '', collapse=','))
      }
      else
      {
         return(paste('{', paste(lapply(seq_along(object), function(i) {
            paste(sep = "",
                  '"',
                  .rs.jsonEscapeString(enc2utf8(names(object)[[i]])),
                  '":',
                  .rs.toJSON(object[[i]])
            )
         }), collapse = ','), '}', sep = '', collapse = ','))
      }
   }
   else
   {
      # NOTE: For type safety we cannot unmarshal NULL as '{}' as e.g. jsonlite does.
      if (!length(object))
      {
         return('[]')
      }
      else if (is.character(object) || is.factor(object))
      {
         object <- paste(collapse = ",", vapply(as.character(object), FUN.VALUE = character(1), USE.NAMES = FALSE, function(x) {
            if (is.na(x)) "null"
            else paste("\"", .rs.jsonEscapeString(enc2utf8(x)), "\"", sep = "")
         }))
      }
      else if (is.numeric(object))
      {
         object[is.na(object)] <- '\"NA\"'
      }
      else if (is.logical(object))
      {

         object[is.na(object)] <- 'null'
      }
      
      if (AsIs)
         return(object)
      else
         return(paste('[', paste(object, collapse = ','), ']', sep = '', collapse = ','))
   }
})

.rs.addFunction("recordFunctionInformation", function(node,
                                                      missingEnv,
                                                      symbolsUsedEnv)
{
   if (is.call(node) && length(node) == 2)
   {
      head <- node[[1]]
      second <- node[[2]]
      if (.rs.isSymbolCalled(head, "missing") &&
          is.symbol(second))
      {
         missingEnv[[as.character(second)]] <- TRUE
      }
   }
   
   ## TODO: Obviously not perfect because of NSE.
   if (is.symbol(node) && !identical(node, quote(expr = )))
      symbolsUsedEnv[[as.character(node)]] <- TRUE
})

.rs.addFunction("emptyNamedList", function()
{
   `names<-`(list(), character())
})

.rs.addFunction("emptyFunctionInfo", function()
{
   list(
      formal_names = character(),
      formal_info  = list(),
      performs_nse = I(0L)
   )
})

# NOTE: This function is used in asynchronous R processes and so
# cannot call back into any RStudio compiled code! Even further,
# `SessionAsyncPackageInformation.cpp` decodes fields based on
# their names, so if you re-name a field here you must modify the
# extraction implementation there as well.
.rs.addFunction("getPackageInformation", function(...)
{
   invisible(lapply(list(...), function(x) {
      tryCatch({
         
         # Explicitly load the library, and do everything we can to hide any
         # package startup messages (because we don't want to put non-JSON
         # on stdout)
         invisible(capture.output(suppressPackageStartupMessages(suppressWarnings(
            success <- library(x, character.only = TRUE, quietly = TRUE, logical.return = TRUE)
         ))))
         
         if (!success)
            return(.rs.emptyFunctionInfo())
         
         # Get the exported items in the NAMESPACE (for search path + `::`
         # completions).
         ns <- asNamespace(x)
         exports <- getNamespaceExports(ns)
         objects <- mget(exports, ns, inherits = TRUE)
         isFunction <- vapply(objects, FUN.VALUE = logical(1), USE.NAMES = FALSE, is.function)
         functions <- objects[isFunction]
         
         # Figure out the completion types for these objects
         types <- vapply(objects, FUN.VALUE = numeric(1), USE.NAMES = FALSE, .rs.getCompletionType)
         
         # Find the functions, and generate information on each formal
         # (does it have a default argument; is missingness handled; etc)
         functionInfo <- lapply(functions, function(f) {
            
            formals <- formals(f)
            if (!length(formals))
               return(.rs.emptyFunctionInfo())
            
            formalNames <- names(formals)
            hasDefault <- vapply(formals, FUN.VALUE = integer(1), USE.NAMES = FALSE, function(x) {
               !identical(x, quote(expr =))
            })
            
            # Record which symbols in the function body handle missingness,
            # to check if missingness of default arguments is handled
            missingEnv <- new.env(parent = emptyenv())
            usedSymbolsEnv <- new.env(parent = emptyenv())
            .rs.recursiveWalk(body(f), function(node) {
               .rs.recordFunctionInformation(node, missingEnv, usedSymbolsEnv)
            })
            
            # Figure out which functions perform NSE. TODO: Figure out which
            # arguments are actually involved in NSE.
            performsNse <- as.integer(
               .rs.recursiveSearch(body(f), .rs.performsNonstandardEvaluation)
            )
            
            formalInfo <- lapply(seq_along(formalNames), function(i) {
               as.integer(c(
                  hasDefault[[i]],
                  formalNames[[i]] == "..." || exists(formalNames[[i]], envir = missingEnv),
                  exists(formalNames[[i]], envir = usedSymbolsEnv)
               ))
            })
            
            list(
               formal_names = formalNames,
               formal_info  = formalInfo,
               performs_nse = I(performsNse)
            )
         })
         
         # Generate the output
         output <- list(
            package = I(x),
            exports = exports,
            types = types,
            function_info = functionInfo
         )
         
         # Write the JSON to stdout; parent processes
         cat(.rs.toJSON(output), sep = "\n")
         
         # Return output for debug purposes
         output
         
      }, error = function(e) NULL)
   }))
})

.rs.setVar("nse.primitives", c(
   "quote", "substitute", "match.call", "eval.parent",
   "enquote", "bquote", "evalq", "lazy_dots"
))

.rs.addFunction("performsNonstandardEvaluation", function(functionBody)
{
   # Allow callers to just pass in functions
   if (is.function(functionBody))
      functionBody <- body(functionBody)
   
   nsePrimitives <- .rs.getVar("nse.primitives")
   .rs.recursiveSearch(functionBody,
                       .rs.performsNonstandardEvaluationImpl,
                       nsePrimitives = nsePrimitives)
})

.rs.addFunction("performsNonstandardEvaluationImpl", function(node, nsePrimitives)
{
   # Check if this is a call to an NSE primitive.
   if (is.call(node))
   {
      head <- node[[1]]
      if (!is.symbol(head))
         return(FALSE)
      
      headString <- as.character(head)
      if (headString %in% nsePrimitives)
         return(TRUE)
      
      # Check if this is a call to an NSE primitive, qualified through
      # `::` or `:::`.
      if (headString %in% c("::", ":::") && length(node) == 3)
      {
         export <- node[[3]]
         if (as.character(export) %in% nsePrimitives)
            return(TRUE)
      }
   }
   
   return(FALSE)
})

.rs.addFunction("recursiveSearch", function(`_node`, fn, ...)
{
   if (fn(`_node`, ...)) return(TRUE)
   
   if (is.call(`_node`))
      for (i in seq_along(`_node`))
         if (.rs.recursiveSearch(`_node`[[i]], fn, ...))
            return(TRUE)
   
   return(FALSE)
})

.rs.addFunction("recursiveWalk", function(`_node`, fn, ...)
{
   fn(`_node`, ...)
   
   if (is.call(`_node`))
      for (i in seq_along(`_node`))
         .rs.recursiveWalk(`_node`[[i]], fn, ...)
})

.rs.addFunction("trimWhitespace", function(x)
{
   gsub("^[\\s\\n]+|[\\s\\n]+$", "", x, perl = TRUE)
})

.rs.addFunction("trimCommonIndent", function(string, ...)
{
   splat <- strsplit(string, "\n", fixed = TRUE)[[1]]
   indents <- regexpr("\\S", splat, perl = TRUE)
   indents[indents == -1] <- Inf
   common <- min(indents)
   result <- paste(substring(splat, common, nchar(string)), collapse = "\n")
   .rs.trimWhitespace(sprintf(result, ...))
})

.rs.addFunction("parseNamespaceImports", function(path)
{
   output <- list(
      import = character(),
      importFrom = list()
   )
   
   if (!file.exists(path))
      return(output)
   
   parsed = tryCatch(
      suppressWarnings(parse(path)),
      error = function(e) NULL
   )
   
   if (is.null(parsed))
      return(output)
   
   # Loop over parsed entries and fill 'output'
   for (i in seq_along(parsed))
   {
      directive <- parsed[[i]]
      if (length(directive) < 2) next
      
      directiveName <- as.character(directive[[1]])
      pkgName <- as.character(directive[[2]])
      
      if (directiveName == "import")
      {
         output$import <- sort(unique(c(output$import, pkgName)))
         next
      }
      
      if (directiveName == "importFrom")
      {
         exports <- character(length(directive) - 2)
         for (i in 3:length(directive))
            exports[[i - 2]] <- as.character(directive[[i]])
         output$importFrom[[pkgName]] <- sort(unique(c(output$importFrom[[pkgName]], exports)))
         next
      }
   }
   
   output
   
})

.rs.addFunction("isSymbolCalled", function(maybeSymbol, name)
{
   is.symbol(maybeSymbol) && as.character(maybeSymbol) == name
})

.rs.addJsonRpcHandler("get_set_class_slots", function(setClassCallString)
{
   onFail <- list()
   parsed <- tryCatch(
      suppressWarnings(parse(text = setClassCallString))[[1]],
      error = function(e) NULL
   )
   
   if (is.null(parsed))
      return(onFail)
   
   matched <- tryCatch(
      match.call(methods::setClass, parsed),
      error = function(e) NULL
   )
   
   if (is.null(parsed))
      return(onFail)
   
   # NOTE: Previously, R has used 'representation' to 
   # store the information about slots; it is now 
   # deprecated (from 3.0.0) and the use of 'slots' is
   # encouraged. We'll check for 'slots' first, then
   # fall back to representation if necessary.
   # 
   # NOTE: Okay to define a class with no slots / fields.
   field <- if ("slots" %in% names(matched))
      matched[["slots"]]
   else if ("representation" %in% names(matched))
      matched[["representation"]]
   else
      list()
   
   if (!("Class" %in% names(matched)))
      return(onFail)
   
   Class <- matched[["Class"]]
   
   slots <- if (length(field))
      names(field)[-1]
   else
      character()
   
   types <- if (length(field))
      unlist(lapply(2:length(field), function(i) {
         tryCatch(as.character(field[[i]]), error = function(e) "")
      }))
   else
      character()
   
   result <- list(
      Class = Class,
      slots = slots,
      types = types
   )
   return(result)
})

.rs.addFunction("tryParseCall", function(text)
{
   tryCatch(
      suppressWarnings(parse(text = text))[[1]],
      error = function(e) NULL
   )
})

.rs.addFunction("tryMatchCall", function(method, call)
{
   tryCatch(
      suppressWarnings(match.call(method, call)),
      error = function(e) NULL
   )
})

.rs.addFunction("extractElement", function(object,
                                           name,
                                           default = NULL)
{
   if (name %in% names(object))
      object[[name]]
   else
      default
})

.rs.addJsonRpcHandler("get_set_generic_call", function(call)
{
   parsed <- .rs.tryParseCall(call)
   if (is.null(parsed))
      return(list())
   
   matched <- .rs.tryMatchCall(methods::setGeneric, parsed)
   if (is.null(matched))
      return(list())
   
   generic <- .rs.extractElement(matched, "name", "")
   parameters <- character()
   if ("def" %in% names(matched))
   {
      def <- matched[["def"]]
      if (as.character(def[[1]]) == "function" &&
          length(def) > 1)
      {
         parameters <- names(def[[2]])
      }
   }
   
   list(
      generic = generic,
      parameters = parameters
   )
})

.rs.addJsonRpcHandler("get_set_method_call", function(call)
{
   parsed <- .rs.tryParseCall(call)
   if (is.null(parsed))
      return(list())
   
   matched <- .rs.tryMatchCall(methods::setMethod, parsed)
   if (is.null(matched))
      return(list())
   
   generic <- .rs.extractElement(matched, "f", "")
   parameter.names <- character()
   parameter.types <- character()
   
   signature <- .rs.extractElement(matched, "signature")
   if (!is.null(signature))
   {
      if (is.call(signature) &&
          length(signature) > 1)
      {
         parameter.names <- names(signature)[-1]
         parameter.types <- unlist(lapply(2:length(signature), function(i) {
            if (is.character(signature[[i]]))
               signature[[i]]
            else
               ""
         }))
      }
      else if (is.character(signature))
      {
         parameter.names <- signature
      }
   }
   
   list(
      generic = generic,
      parameter.names = parameter.names,
      parameter.types = parameter.types
   )
   
})

.rs.addJsonRpcHandler("get_set_ref_class_call", function(call)
{
   parsed <- .rs.tryParseCall(call)
   if (is.null(parsed))
      return(list())
   
   matched <- .rs.tryMatchCall(methods::setRefClass, parsed)
   if (is.null(matched))
      return(list())
   
   Class <- .rs.extractElement(matched, "Class", "")
   
   field.names <- character()
   field.types <- character()
   fields <- .rs.extractElement(matched, "fields")
   
   if (length(fields) > 1)
   {
      field.names <- names(fields)[-1]
      field.types <- unlist(lapply(2:length(fields), function(i) {
         if (is.character(fields[[i]]))
            fields[[i]]
         else
            ""
      }))
   }
   
   methods <- .rs.extractElement(matched, "methods")
   method.names <- if (length(methods))
      names(methods)[-1]
   else
      character()

   list(
      Class = Class,
      field.names = field.names,
      field.types = field.types,
      method.names = method.names
   )
   
})

.rs.addFunction("getSetRefClassSymbols", function(callString)
{
   parsed <- .rs.rpc.get_set_ref_class_call(callString)
   as.character(c(
      parsed$field.names,
      parsed$method.name
   ))
})

.rs.addFunction("getR6ClassSymbols", function(callString)
{
   parsed <- .rs.tryParseCall(callString)
   if (is.null(parsed)) return(character())
   
   symbols <- c("self", "public", "private", "super")
   public <- .rs.extractElement(parsed, "public")
   if (!is.null(public))
      symbols <- c(symbols, names(public)[-1])
   
   symbols
})

.rs.addFunction("registerNativeRoutines", function()
{
   pos <- match("tools:rstudio", search())
   if (is.na(pos))
      return()
   
   if (exists(".rs.routines", pos))
      return()
   
   routineEnv <- new.env(parent = emptyenv())
   routines <- tryCatch(
      getDLLRegisteredRoutines("(embedding)"),
      error = function(e) NULL
   )
   
   if (is.null(routines))
      return(NULL)
   
   .CallRoutines <- routines[[".Call"]]
   lapply(.CallRoutines, function(routine) {
      routineEnv[[routine$name]] <- routine
   })
   assign(".rs.routines", routineEnv, pos = which(search() == "tools:rstudio"))
   routineEnv
})

.rs.addFunction("setEncodingUnknownToUTF8", function(object)
{
   if (is.character(object) && Encoding(object) == "unknown")
      Encoding(object) <- "UTF-8"
   else if (is.list(object))
      return(lapply(object, .rs.setEncodingUnknownToUTF8))
   
   object
})

.rs.addFunction("makePrimitiveWrapper", function(x) {
   eval(parse(text = capture.output(x)), envir = parent.frame(1))
})

.rs.addFunction("extractNativeSymbols", function(DLL, collapse = TRUE)
{
   info <- getDLLRegisteredRoutines(DLL)
   result <- lapply(info, function(routine) {
      as.character(names(routine))
   })
   
   if (collapse)
      result <- as.character(unlist(result))
   
   result
})

.rs.addFunction("getNativeSymbols", function(package)
{
   loadedDLLs <- getLoadedDLLs()
   if (package %in% names(loadedDLLs))
      return(.rs.extractNativeSymbols(loadedDLLs[[package]]))
   
   reExtension <- paste("\\", .Platform$dynlib.ext, "$", sep = "")
   
   # Try loading the DLL temporarily so we can extract the symbols.
   # Note that the shared object name does not necessarily match that
   # of the package; e.g. `data.table` munges the object name.
   libPath <- system.file("libs", package = package)
   dllNames <- sub(
      reExtension,
      "",
      list.files(libPath, pattern = reExtension)
   )
   
   as.character(unlist(lapply(dllNames, function(name) {
      
      # TODO: Are there side-effects of this call that we want to avoid? If so
      # we might want to execute this in a separate R process.
      DLL <- try(
         library.dynam(name, package = package, lib.loc = .libPaths()),
         silent = TRUE
      )
      
      if (inherits(DLL, "try-error"))
         return(character())
      
      dllPath <- DLL[["path"]]
      on.exit({
         library.dynam.unload(name, libpath = system.file(package = package))
      }, add = TRUE)
      
      return(.rs.extractNativeSymbols(DLL))
   })))
   
})
