#
# SessionCodeTools.R
#
# Copyright (C) 2020 by RStudio, PBC
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

.rs.addJsonRpcHandler("discover_package_dependencies", function(docId, fileType)
{
   available <- .rs.availablePackages()
   ready <- !is.null(available$value)
   packages <- .rs.discoverPackageDependencies(docId, fileType)
   list(ready = .rs.scalar(ready), packages = packages)
})

.rs.addFunction("stopf", function(fmt, ..., call. = FALSE)
{
   msg <- sprintf(fmt, ...)
   stop(msg, call. = call.)
})

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

.rs.addFunction("withTimeLimit", function(seconds,
                                          expr,
                                          fail = NULL)
{
   setTimeLimit(elapsed = seconds, transient = TRUE)
   on.exit(setTimeLimit(), add = TRUE)
   tryCatch(expr, error = function(e) fail)
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

.rs.addFunction("surround", function(string, with)
{
   paste(with, string, with, sep = "")
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

.rs.addFunction("getFunction", function(name, envir = globalenv())
{
   tryCatch(
      .rs.getFunctionImpl(name, envir),
      error = function(e) NULL
   )
})

.rs.addFunction("getFunctionImpl", function(name, envir = globalenv())
{
   envir <- .rs.resolveEnvironment(envir)
   parsed <- parse(text = name)[[1L]]
   
   # for plain symbols, we can attempt lookup of a function object
   # directly; for more complex calls, we try to evaluate it in
   # the requested environment and hope we got a function
   fn <- if (is.symbol(parsed))
      get(name, envir = envir, mode = "function")
   else
      eval(parsed, envir = envir)
   
   if (!is.function(fn))
      return(NULL)
   
   fn
})


.rs.addFunction("functionHasSrcRef", function(func)
{
   !is.null(attr(func, "srcref"))
})

# Returns a function's code as a string. Arguments:
# useSource -- Whether to use the function's source references (if available)
# asString -- Whether to return the result as a single string
#             (otherwise a character vector of lines is returned)
.rs.addFunction("deparseFunction", function(func, useSource, asString)
{
   # If we're being asked to return the source references as-is, read the source
   # references directly instead of going through deparse (which has a known
   # issue that corrupts output with backslashes in R 4.0.0 and perhaps other
   # versions; see R bug 17800).
   if (useSource)
   {
      srcref <- attr(func, "srcref", exact = TRUE)
      if (!is.null(srcref))
      {
         code <- as.character(srcref, useSource = TRUE)
         if (asString)
           return(paste(code, collapse = "\n"))
         else
           return(code)
      }
   }

   control <- c("keepInteger", "keepNA")
   if (useSource)
     control <- append(control, "useSource")

   code <- deparse(func, width.cutoff = 59, control = control)
   
   # if we were asked not to use source refs, or we were but there wasn't a
   # source ref to use, then format the code according to user pref
   if (!useSource || is.null(attr(func, "srcref", exact = TRUE)))
   {
     replaceText <- "\t"

     # determine the replacement text based on the user's current editing
     # preferences
     if (isTRUE(.rs.readUiPref("use_spaces_for_tab"))) 
     {
       replaceText <- paste(rep(" ", .rs.readUiPref("num_spaces_for_tab")),
                                collapse = "")
     }

     # split the code into individual lines -- R's immutable strings make it
     # quicker to process these small chunks than on the whole code text
     lines <- unlist(strsplit(code, "\n", fixed = TRUE))
     for (l in seq_along(lines))
     {
       line <- lines[[l]]
       pos <- 1
       # convert up to 20 indentation levels per line
       for (lvl in seq_len(20))
       {
         # NOTE: the 4 spaces comes from the implementation of printtab2buff in
         # deparse.c -- it is hard-coded to use 4 spaces for the first 4 levels
         # of indentation and then 2 spaces for subsequent levels.
         indent <- if (lvl <= 4) "    " else "  "
         if (substring(line, pos, pos + (nchar(indent) - 1)) == indent)
         {
           # convert this indent to the user's preferred indentation 
           line <- paste(substring(line, 0, pos - 1),
                         replaceText, 
                         substring(line, pos + nchar(indent)),
                         sep = "")
           pos <- pos + nchar(replaceText)
         }
         else
         {
           # no more indents we want to convert on this line
           break
         }
       }
       lines[[l]] <- line
     }

     # if we were asked to return individual lines, we're done now
     if (!asString)
       return(lines)
     else 
       code <- lines
   }

   # return (possibly formatted) code
   if (asString)
     paste(code, collapse = "\n")
   else
     code
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

.rs.addFunction("getUseMethodGeneric", function(x)
{
   if (is.function(x))
      x <- body(x)
   
   if (!is.call(x))
      return(NULL)
   
   UseMethod <- as.name("UseMethod")
   generic <- NULL
   .rs.recursiveSearch(x, function(node) {
      
      if (!is.call(node) || length(node) < 2 || length(node) > 3)
         return(FALSE)
      
      lhs <- node[[1]]
      if (!identical(lhs, UseMethod))
         return(FALSE)
      
      matched <- tryCatch(
         match.call(function(generic, object) {}, node),
         error = function(e) NULL
      )
      
      if (is.character(matched[["generic"]]))
      {
         generic <<- matched[["generic"]]
         return(TRUE)
      }
      
      FALSE
   })
   
   generic
})

.rs.addFunction("getS3MethodDefinitions", function(generic)
{
   if (is.function(generic))
      generic <- .rs.getUseMethodGeneric(generic)
   
   if (!is.character(generic))
      return(NULL)
   
   call <- substitute(
      methods(generic),
      list(generic = generic)
   )
   
   methods <- eval(call, envir = globalenv())
   info <- attr(methods, "info")
   if (!is.data.frame(info))
      return(NULL)
   
   defns <- lapply(seq_len(nrow(info)), function(i) {
      method <- rownames(info)[[i]]
      generic <- info$generic[[i]]
      class <- substring(method, nchar(generic) + 2)
      
      call <- substitute(
         utils::getS3method(generic, class, optional = TRUE),
         list(generic = generic, class = class)
      )
      
      eval(call, envir = globalenv())
   })
   
   names(defns) <- rownames(info)
   defns
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
   if (is.symbol(body))
     return(object)

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
   
   # Helper function for evaluating an expression with warnings + messages
   # suppressed, and errors coerced to NULL
   quietly <- function(expr) {
      withCallingHandlers(
         tryCatch(expr, error = function(e) NULL),
         warning = function(e) invokeRestart("muffleWarning"),
         message = function(e) invokeRestart("muffleMessage")
      )
   }
   
   if (is.character(name) && is.character(envir))
   {
      # If envir is the name of something on the search path, get it from there
      pos <- match(envir, search(), nomatch = -1L)
      if (pos >= 0)
      {
         object <- quietly(get(name, pos = pos))
         if (!is.null(object))
            return(.rs.resolveAliasedSymbol(object))
      }
      
      # Otherwise, maybe envir is the name of a package -- search there
      if (envir %in% loadedNamespaces())
      {
         object <- quietly(get(name, envir = asNamespace(envir)))
         if (!is.null(object))
            return(.rs.resolveAliasedSymbol(object))
      }
   }
   
   if (is.character(name))
   {
      name <- quietly(parse(text = .rs.stripSurrounding(name)))
      if (is.null(name))
         return(NULL)
   }
   
   if (is.language(name))
      result <- quietly(eval(name, envir = envir))
   
   .rs.resolveAliasedSymbol(result)
   
})

.rs.addFunction("getFunctionArgumentNames", function(object)
{
   # for primitive objects, 'args()' can be used to extract
   # a function object with compatible prototype -- although
   # primitive functions that power control flow (e.g. `if()`,
   # `return()` can return NULL)
   if (is.primitive(object))
      object <- args(object)
   
   result <- character()
   if (is.function(object))
      result <- names(formals(object))
   
   result
})

.rs.addFunction("getNames", function(object)
{
   tryCatch({
      if (is.environment(object))
         ls(object, all.names = TRUE)
      else if (inherits(object, "tbl") && "dplyr" %in% loadedNamespaces())
         dplyr::tbl_vars(object)
      # For some reason, `jobjRef` objects (from rJava) return names containing
      # parentheses after the associated function call, which confuses our completion
      # system.
      else if (inherits(object, "jobjRef"))
         gsub("[\\(\\)]", "", names(object))
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
   
   # use devtools shim for help if available
   if ("devtools_shims" %in% search() &&
       "devtools" %in% loadedNamespaces() &&
       exists("shim_help", envir = asNamespace("devtools")))
   {
      help <- devtools:::shim_help
   }
   else
   {
      help <- utils::help
   }

   capture.output(suppressMessages({
      if (length(pieces) > 1) {
         print(help(pieces[2], package = pieces[1], help_type = 'html'))
      } else {
         # try custom help handler, otherwise fall through to default handler
         helpUrl <- .rs.getCustomHelpUrl(token)
         if (!is.null(helpUrl)) {
            if (nzchar(helpUrl)) # handlers return "" to indicate no help available
               utils::browseURL(helpUrl)
         } else {
            print(help(pieces[1], help_type = 'html', try.all.packages = TRUE))
         }
      }
   }))
})

.rs.addFunction("findCustomHelpContext", function(token, handler) {
   
   # if the token has a '$' in it then it might have a custom
   # help handler that can field this request
   if (grepl("\\$", token)) {
      
      # split on $ (it has at least one so components will be > 1)
      components <- strsplit(token, "\\$")[[1]]
      topic <- components[[length(components)]]
      source <- paste(components[1:(length(components)-1)], collapse = "$")
      
      # evaluate the source
      source <- tryCatch(eval(parse(text = source), envir = globalenv()), 
                         error = function(e) NULL)
      
      # look for a help url handler
      if (!is.null(source)) {
         for (cls in class(source)) {
            res <- utils::getAnywhere(paste(handler, ".", cls, sep=""))
            if (length(res$objs) > 0) {
               return(list(
                  topic = topic,
                  source = source,
                  handler = res$objs[[1]]
               ))
            }
         }
      }
   }
   
   # default to none found
   NULL
   
})

# check to see whether there is a custom help handler for this token
.rs.addFunction("getCustomHelpUrl", function(token) {
   custom <- .rs.findCustomHelpContext(token, "help_url_handler")
   if (!is.null(custom))
      custom$handler(custom$topic, custom$source)
   else
      NULL
})

.rs.addJsonRpcHandler("execute_r_code", function(code)
{
   # allow 2 seconds to execute code
   setTimeLimit(elapsed = 2, transient = TRUE)
   on.exit(setTimeLimit(), add = TRUE)
   
   # evaluate requested code
   envir <- parent.frame(2)
   result <- .rs.tryCatch({
      
      output <- capture.output(
         evaled <- suppressWarnings(
            eval(parse(text = code), envir = envir)
         )
      )
      
      object <- if (length(output))
         output
      else
         deparse(evaled)
      
   })
   
   if (inherits(result, "error"))
      return(.rs.scalar(""))
   
   .rs.scalar(paste(as.character(object), collapse = "\n"))
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
   .Call("rs_isSubsequence", strings, string)
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
   assign(paste(".rs.cache.", x, sep = ""), value, envir = .rs.toolsEnv())
})

.rs.addFunction("get", function(x)
{
   tryCatch(
      get(paste(".rs.cache.", x, sep = ""), envir = .rs.toolsEnv()),
      error = function(e) NULL
   )
})

.rs.addFunction("mget", function(x = NULL)
{
   tryCatch(
      {
         objects <- if (is.null(x))
            .rs.selectStartsWith(objects(envir = .rs.toolsEnv(), all.names = TRUE), ".rs.cache")
         else
            paste(".rs.cache.", x, sep = "")
         
         mget(objects, envir = .rs.toolsEnv())
      },
      
      error = function(e) NULL
   )
})

.rs.addFunction("packageNameForSourceFile", function(filePath)
{
   .Call("rs_packageNameForSourceFile", filePath)
})

.rs.addFunction("isRScriptInPackageBuildTarget", function(filePath)
{
   .Call("rs_isRScriptInPackageBuildTarget", filePath)
})

.rs.addFunction("namedVectorAsList", function(vector)
{
   values <- unlist(vector, use.names = FALSE)
   if (!length(values))
      return(list(names = NULL, values = NULL))
   vectorNames <- names(vector)
   names <- unlist(lapply(seq_along(vector), function(i) {
      rep.int(vectorNames[i], length(vector[[i]]))
   }))
   
   list(names = names, values = values)
})

.rs.addFunction("getDollarNamesMethod", function(object,
                                                 excludeBaseClasses = FALSE,
                                                 envir = globalenv())
{
   classAndSuper <- function(cl)
   {
      # selectSuperClasses throws an error if the class is unknown
      super <- tryCatch(
         methods::selectSuperClasses(cl),
         error = function(e) NULL
      )
      
      c(cl, super)
   }
   
   # interleave super classes after the corresponding original classes
   classes <- unlist(lapply(class(object), classAndSuper), recursive = TRUE)
   # either remove or add an explicit (=non-mode) list/environment class
   classes <- if (excludeBaseClasses)
      setdiff(classes, c("list", "environment"))
   else
      c(classes, mode(object))
   
   for (class in classes)
   {
      method <- utils::getS3method(
         f = ".DollarNames",
         class = class,
         envir = envir,
         optional = TRUE
      )
      
      if (!is.null(method))
         return(method)
   }
   
   NULL
})

.rs.addJsonRpcHandler("get_args", function(name, src, helpHandler)
{
   # call custom help handler if provided
   if (nzchar(helpHandler)) {
      helpHandlerFunc <- tryCatch(eval(parse(text = helpHandler)),
                                  error = function(e) NULL)
      if (!is.function(helpHandlerFunc))
         return(NULL)
      help <- helpHandlerFunc("completion", name, src)
      if (is.null(help))
         return(NULL)
      signature <- help$signature
      if (!is.null(signature)) {
         paren <- regexpr("\\(", signature)[[1]]
         if (paren != -1)
            signature <- substring(signature, paren)
      }
      return(.rs.scalar(signature))
   }
   
   # if we're getting arguments for 'write.csv', infer arguments
   # based on 'write.table'
   isUtils <-
      identical(src, "package:utils") ||
      identical(src, "utils")
   
   if (isUtils)
   {
      if (identical(name, "write.csv") ||
          identical(name, "write.csv2"))
      {
         name <- "write.table"
      }
   }
   
   onError <- .rs.scalar("(...)")
   
   envir <- .rs.tryCatch(.rs.resolveEnvironment(src))
   if (!is.environment(envir))
      return(onError)
   
   method <- .rs.tryCatch(get(name, envir = envir, mode = "function"))
   if (!is.function(method))
      return(onError)
   
   signature <- .rs.getSignature(method)
   result <- sub("function ", "", signature)
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
   .Call("rs_scoreMatches", strings, string)
})

.rs.addFunction("getProjectDirectory", function()
{
   .Call("rs_getProjectDirectory")
})

.rs.addFunction("hasFileMonitor", function()
{
   .Call("rs_hasFileMonitor")
})

.rs.addFunction("doListIndex", function(routine, term, inDirectory, maxCount)
{
   if (!.rs.hasFileMonitor() || is.null(inDirectory))
      return(NULL)
   
   inDirectory <- suppressWarnings(.rs.normalizePath(inDirectory))
   inDirectory <- gsub("[/\\\\]+$", "", inDirectory)
   
   .Call(routine, term, inDirectory, as.integer(maxCount))
})

.rs.addFunction("listIndexedFiles", function(term = "",
                                             inDirectory = .rs.getProjectDirectory(),
                                             maxCount = 200L)
{
   .rs.doListIndex("rs_listIndexedFiles", term, inDirectory, as.integer(maxCount))
})

.rs.addFunction("listIndexedFolders", function(term = "",
                                               inDirectory = .rs.getProjectDirectory(),
                                               maxCount = 200L)
{
   .rs.doListIndex("rs_listIndexedFolders", term, inDirectory, maxCount)
})

.rs.addFunction("listIndexedFilesAndFolders", function(term = "",
                                                       inDirectory = .rs.getProjectDirectory(),
                                                       maxCount = 200L)
{
   .rs.doListIndex("rs_listIndexedFilesAndFolders", term, inDirectory, maxCount)
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
   
   paths <- suppressWarnings(.rs.normalizePath(index$paths, winslash = "/"))
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
##
## Unbox will automatically unbox any 1-length non-list vectors.
.rs.addFunction("toJSON", function(object, unbox = FALSE)
{
   AsIs <- inherits(object, "AsIs") || inherits(object, ".rs.scalar")
   if (is.list(object))
   {
      if (is.null(names(object)))
      {
         return(paste('[', paste(lapply(seq_along(object), function(i) {
            .rs.toJSON(object[[i]], unbox = unbox)
         }), collapse = ','), ']', sep = '', collapse=','))
      }
      else
      {
         return(paste('{', paste(lapply(seq_along(object), function(i) {
            paste(sep = "",
                  '"',
                  .rs.jsonEscapeString(enc2utf8(names(object)[[i]])),
                  '":',
                  .rs.toJSON(object[[i]], unbox = unbox)
            )
         }), collapse = ','), '}', sep = '', collapse = ','))
      }
   }
   else
   {
      n <- length(object)
      
      # NOTE: For type safety we cannot unmarshal NULL as '{}' as e.g. jsonlite does.
      if (is.null(object))
      {
         if (unbox)
            return('null')
         else
            return('[]')
      }
      else if (n == 0)
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
         object <- ifelse(object, "true", "false")
         object[is.na(object)] <- 'null'
      }
      
      if (AsIs || (unbox && n == 1))
         return(paste(object))
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
   # force 'rgl' to use the NULL graphics device (avoid inadvertently
   # opening a graphics device / XQuartz window when retrieving completions)
   rgl.useNULL <- getOption("rgl.useNULL")
   options(rgl.useNULL = TRUE)
   on.exit(options(rgl.useNULL = rgl.useNULL), add = TRUE)
   
   packages <- list(...)
   lapply(packages, function(package) {
      tryCatch(
         .rs.emitPackageInformation(package),
         error = function(e) .rs.emptyFunctionInfo()
      )
   })
})

.rs.addFunction("emitPackageInformation", function(package)
{
   # Explicitly load the library, and do everything we can to hide any
   # package startup messages (because we don't want to put non-JSON
   # on stdout)
   invisible(capture.output(suppressPackageStartupMessages(suppressWarnings({
      
      ## Don't load the package if a corresponding 00LOCK directory exists.
      ## This gives partial protection against attempting to load a package
      ## while another R process is attempting to modify the library directory.
      has00LOCK <- FALSE
      for (libPath in .libPaths())
      {
         globalLockPath <- file.path(libPath, "00LOCK")
         pkgLockPath <- file.path(libPath, paste("00LOCK", basename(package), sep = "-"))
         if (file.exists(globalLockPath) || file.exists(pkgLockPath))
         {
            has00LOCK <- TRUE
            break
         }
      }
      
      success <- if (has00LOCK)
         FALSE
      else
         library(package, character.only = TRUE, quietly = TRUE, logical.return = TRUE)
      
   }))))
   
   if (!success)
      return(.rs.emptyFunctionInfo())
   
   # Get the exported items in the NAMESPACE
   # (for search path + `::` completions).
   ns <- asNamespace(package)
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
         !identical(x, quote(expr = ))
      })
      
      # Record which symbols in the function body handle missingness,
      # to check if missingness of default arguments is handled
      missingEnv <- new.env(parent = emptyenv())
      usedSymbolsEnv <- new.env(parent = emptyenv())
      .rs.recursiveWalk(body(f), function(node) {
         .rs.recordFunctionInformation(node, missingEnv, usedSymbolsEnv)
      })
      
      # Figure out which functions perform NSE.
      # TODO: Figure out which arguments are actually involved in NSE.
      performsNse <- .rs.performsNonstandardEvaluation(f)
      
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
         performs_nse = I(as.integer(performsNse))
      )
   })
   
   # List data objects exported by this package
   datasets <- .rs.listDatasetsProvidedByPackage(package)
   
   # Generate the output
   output <- list(
      package = I(package),
      exports = exports,
      types = types,
      function_info = functionInfo,
      datasets = datasets
   )
   
   # Write the JSON to stdout; parent processes
   json <- paste("#!json:", .rs.toJSON(output))
   cat(json, sep = "\n")
   
   # Return output for debug purposes
   invisible(output)
})

.rs.setVar("nse.primitives", c(
   "quote", "substitute", "match.call", "eval.parent",
   "enquote", "bquote", "evalq", "lazy_dots", "compat_as_lazy_dots",
   "select_vars", "quo", "quos", "enquo", "named_quos"
))

.rs.addFunction("performsNonstandardEvaluation", function(object)
{
   # For S3 generics, search methods as well as the generic for
   # potential usages of non-standard evaluation.
   methods <- .rs.getS3MethodDefinitions(object)
   for (method in methods)
      if (.rs.performsNonstandardEvaluation(method))
         return(TRUE)
   
   if (is.function(object))
      object <- body(object)
   
   nsePrimitives <- .rs.getVar("nse.primitives")
   .rs.recursiveSearch(object,
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
   if (fn(`_node`, ...))
      return(TRUE)
   
   if (is.recursive(`_node`))
      for (i in seq_along(`_node`))
         if (.rs.recursiveSearch(`_node`[[i]], fn, ...))
            return(TRUE)
   
   return(FALSE)
})

.rs.addFunction("recursiveWalk", function(`_node`, fn, ...)
{
   fn(`_node`, ...)
   
   if (is.recursive(`_node`))
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
   dots <- list(...)
   formatted <- if (length(dots)) sprintf(result, ...) else result
   .rs.trimWhitespace(formatted)
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
   unique(as.character(c(
      ".self",
      parsed$field.names,
      parsed$method.names
   )))
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

.rs.addFunction("setEncodingUnknownToUTF8", function(object)
{
   if (is.character(object) && Encoding(object) == "unknown")
      Encoding(object) <- "UTF-8"
   else if (is.list(object))
      return(lapply(object, .rs.setEncodingUnknownToUTF8))
   
   object
})

.rs.addFunction("makePrimitiveWrapper", function(x)
{
   # from the R documentation, args returns:
   #
   #   For a closure, a closure with identical formal argument list but an empty
   #   (NULL) body.
   #
   #   For a primitive, a closure with the documented usage and NULL body. Note
   #   that some primitives do not make use of named arguments and match by
   #   position rather than name.
   #
   # we just need an R closure with the right formals, so args fits nicely
   args(x)
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
   
   # we used to try to load and unload the package library to
   # extract symbol information, but this is not safe to do now
   # loading the DLL also implies running its R_init_* hook, and
   # this can imply loading the package (+ its dependencies) --
   # something we normally want to avoid
   character()
})

.rs.addJsonRpcHandler("extract_chunk_options", function(chunkText)
{
   # Attempt to parse the chunk as R code
   parsed <- try(parse(text = chunkText), silent = TRUE)
   if (inherits(parsed, "try-error"))
      return(list())
   
   # Iterate through the expression tree, looking for calls to opts_chunk$set and
   # extracting their values. Load them up into an environment (which we then return
   # as a list back to the client)
   chunkOptionsEnv <- new.env(parent = emptyenv())
   lapply(parsed, function(node) {
      .rs.recursiveWalk(node, function(node) {
         
         if (!is.call(node) || length(node) < 2)
            return()
         
         # Perhaps not the most efficient, but probably the easiest way to detect
         # appropriate calls to `opts_chunk$set`.
         callName <- as.character(node)
         if (callName == "knitr:::opts_chunk$set" ||
             callName == "knitr::opts_chunk$set" ||
             callName == "opts_chunk$set")
         {
            names <- names(node)
            for (i in 2:length(node))
            {
               key <- names[[i]]
               if (key == "")
                  next
               
               val <- if (is.character(node[[i]]))
                  .rs.surround(node[[i]], with = "\"")
               else
                  format(node[[i]])
               chunkOptionsEnv[[key]] <- val
            }
         }
      })
   })
   
   # Convert to a list, and ensure each element is interpretted as a scalar
   # (rather than an array containing a single element)
   result <- as.list(chunkOptionsEnv)
   for (i in seq_along(result))
      result[[i]] <- .rs.scalar(result[[i]])
   
   return(result)
})

.rs.addFunction("enumerate", function(X, FUN, ...)
{
   keys <- if (is.environment(X)) {
      sort(ls(envir = X))
   } else {
      names(X)
   }
   
   result <- lapply(keys, function(key) {
      FUN(key, X[[key]], ...)
   })
   
   names(result) <- keys
   
   result
})

.rs.addFunction("cutpoints", function(data)
{
   diffed <- diff(c(data[1], data))
   which(diffed != 0)
})

.rs.addFunction("recode", function(data, ..., envir = parent.frame())
{
   dots <- eval(substitute(alist(...)))
   
   for (expr in dots)
   {
      if (length(expr) != 3)
         stop("malformed recoding in .rs.recode()", call. = FALSE)
      
      lhs <- eval(expr[[2]], envir = envir)
      rhs <- eval(expr[[3]], envir = envir)
      data[data == lhs] <- rhs
   }
   
   data
})

.rs.addFunction("evalWithAvailableArguments", function(fn, args)
{
   filtered <- args[names(args) %in% names(formals(fn))]
   call <- c(substitute(fn), filtered)
   mode(call) <- "call"
   eval(call, envir = parent.frame())
})

.rs.addFunction("transposeList", function(list)
{
   do.call(Map, c(c, list, USE.NAMES = FALSE))
})

.rs.addFunction("base64encode", function(data, binary = FALSE)
{
   .Call("rs_base64encode", data, binary)
})

.rs.addFunction("base64encodeFile", function(path)
{
   .Call("rs_base64encodeFile", path)
})

.rs.addFunction("base64decode", function(data, binary = FALSE)
{
   .Call("rs_base64decode", data, binary)
})

.rs.addFunction("CRANDownloadOptionsString", function()
{
   
   # collect options of interest
   options <- options("repos", "download.file.method", "download.file.extra", "HTTPUserAgent")
   if (identical(options[["download.file.method"]], "curl"))
      options[["download.file.extra"]] <- .rs.downloadFileExtraWithCurlArgs()
   
   # drop NULL entries
   options <- Filter(Negate(is.null), options)
   
   # deparse values individually. avoid relying on the format
   # of the deparsed output of the whole expression; see e.g.
   # https://github.com/rstudio/rstudio/issues/4916 for example
   # of where this can fail
   vals <- lapply(options, function(option) {
      
      # replace single quotes with double quotes, so that
      # deparse automatically escapes the inner quotes
      # see: https://github.com/rstudio/rstudio/issues/6597
      # (note these will be translated to single quotes later)
      if (is.character(option))
         option <- gsub("'", "\"", option)
      
      .rs.deparse(option)
   })
   
   # join keys and values
   keyvals <- paste(names(options), vals, sep = " = ")
   
   # create final options command
   opts <- sprintf("options(%s)", paste(keyvals, collapse = ", "))
   
   # NOTE: we need to quote arguments with single quotes as the command will be
   # submitted using double quotes, and embedded quotes in the command are not
   # properly escaped.
   #
   # TODO: handle embedded quotes properly
   gsub("\"", "'", opts, fixed = TRUE)
   
})

.rs.addFunction("slice", function(object,
                                  start = 1,
                                  end = length(object))
{
   n <- length(object)
   if (n == 0)
      return(object)
   
   if (start < 0) start <- n + start
   if (end < 0)   end <- n + end
   
   start <- max(1, start)
   end   <- min(n, end)
   
   if (start > end)
      return(object[0])
   
   return(object[start:end])
})

.rs.addFunction("slotNames", function(object)
{
   # non-S4 objects don't have slot names
   if (!isS4(object))
      return(character())
   
   # try using the official API to retrieve slot names
   slots <- methods::slotNames(object)
   if (is.character(slots))
      return(slots)
   
   # if that fails for some reason (e.g. the package that
   # provides the associated class definition is not loaded)
   # then fall back to using object attributes
   slots <- names(attributes(object))
   setdiff(slots, "class")
 
})

.rs.addFunction("deparse", function(object)
{
   paste(deparse(object, width.cutoff = 500L), collapse = " ")
})

.rs.addFunction("ensureScalarCharacter", function(object)
{
   if (is.character(object) && length(object) == 1)
      return(object)
   
   fmt <- "'%s' is not a length-one character vector"
   stop(sprintf(fmt, .rs.deparse(substitute(object))), call. = FALSE)
})

.rs.addFunction("isR6NewMethod", function(object)
{
   if (!is.function(object))
      return(FALSE)
   
   envir <- environment(object)
   if (!inherits(envir, "R6ClassGenerator"))
      return(FALSE)
   
   identical(object, envir$new)
})

.rs.addFunction("getR6ClassGeneratorMethod", function(object, method)
{
   if (is.function(object))
      object <- environment(object)
   
   if (!is.environment(object))
      return(NULL)
   
   if (!inherits(object, "R6ClassGenerator"))
      return(NULL)
   
   tryCatch(
      object$public_methods[[method]],
      error = function(e) NULL
   )
})

.rs.addFunction("isExternalPointer", function(object)
{
   identical(typeof(object), "externalptr")
})

.rs.addFunction("fileInfo", function(..., extra_cols = TRUE)
{
   suppressWarnings(file.info(..., extra_cols = extra_cols))
})

.rs.addFunction("listDatasetsProvidedByPackage", function(package)
{
   # verify we have a non-empty length-one string
   if (!is.character(package) || length(package) != 1 || !nzchar(package))
      return(character())
   
   # find the installed package location (returns empty vector on failure)
   location <- find.package(package, quiet = TRUE)
   if (!length(location) || !file.exists(location))
      return(character())
   
   # construct path to datalist file
   datalist <- file.path(location, "data/datalist")
   if (!file.exists(datalist))
      return(character())
   
   # read the names of the provided objects
   readLines(datalist, warn = FALSE)
   
})

.rs.addFunction("tryCatch", function(expr)
{
   tryCatch(
      
      withCallingHandlers(
         expr,
         warning = function(w) invokeRestart("muffleWarning"),
         message = function(m) invokeRestart("muffleMessage")
      ),
      
      error = identity
   )
})

.rs.addFunction("resolveAliasedPath", function(path)
{
   .Call("rs_resolveAliasedPath", path, PACKAGE = "(embedding)")
})

.rs.addFunction("readPackageDescription", function(packagePath)
{
   # if this is an installed package with a package metafile,
   # read from that location
   metapath <- file.path(packagePath, "Meta/package.rds")
   if (file.exists(metapath)) {
      metadata <- readRDS(metapath)
      return(as.list(metadata$DESCRIPTION))
   }
   
   # otherwise, attempt to read DESCRIPTION directly
   descPath <- file.path(packagePath, "DESCRIPTION")
   read.dcf(descPath, all = TRUE)
})

.rs.addFunction("readSourceDocument", function(id)
{
   contents <- .Call("rs_readSourceDocument", as.character(id), PACKAGE = "(embedding)")
   if (is.character(contents))
      Encoding(contents) <- "UTF-8"
   contents
})

.rs.addFunction("parsePackageDependencies", function(contents, extension)
{
   # NOTE: the following regular expressions were extracted from knitr;
   # we pull these out here just to avoid potentially loading the knitr
   # package without the user's consent
   code <- if (identical(extension, ".R"))
      contents
   else if (identical(extension, ".Rmd"))
      .rs.extractRCode(contents,
                       "^[\t >]*```+\\s*\\{([a-zA-Z0-9_]+.*)\\}\\s*$",
                       "^[\t >]*```+\\s*$")
   else if (identical(extension, ".Rnw"))
      .rs.extractRCode(contents,
                       "^\\s*<<(.*)>>=.*$",
                       "^\\s*@\\s*(%+.*|)$")
   
   if (is.null(code))
      return(character())
   
   # attempt to parse extracted R code
   parsed <- .rs.tryCatch(parse(text = code, encoding = "UTF-8"))
   if (inherits(parsed, "error")) {
      
      # call 'Sys.setlocale()' to work around an R issue where flags
      # set during parse can effectively be 'leaked' -- 'Sys.setlocale()'
      # effectively resets those flags. see:
      #
      # https://bugs.r-project.org/bugzilla3/show_bug.cgi?id=17484
      # https://github.com/rstudio/rstudio/issues/3658
      #
      Sys.setlocale()
      return(character())

   }

   discoveries <- new.env(parent = emptyenv())

   # for R Markdown docs, scan the YAML header (requires the rmarkdown package and the yaml package,
   # a dependency of rmarkdown)
   if (identical(extension, ".Rmd") && 
       requireNamespace("rmarkdown", quietly = TRUE) &&
       requireNamespace("yaml", quietly = TRUE))
   {
      # extract the front matter from the document; accepts a list of lines and returns a list with
      # $front_matter and $body elements
      partitions <- rmarkdown:::partition_yaml_front_matter(
            strsplit(x = contents, split = "\n", fixed = TRUE)[[1]])

      # did we find some front matter?
      if (!is.null(partitions$front_matter))
      {
         front <- NULL

         tryCatch({
            front <- yaml::read_yaml(text = partitions$front_matter)
         }, error = function(e) {
            # ignore errors when reading YAML; it's very possible that the document's YAML will not
            # be correct at all times (e.g. during editing) 
         })

         # start with an empty output
         output <- NULL

         if (!is.null(names(front$output)))
         {
            # if the output key has children, it will appear as a list name
            # output:
            #   pkg_name::out_fmt:
            #     foo: bar
            output <- names(front$output)[[1]]
         }
         else if (is.character(front$output))
         {
            # if the output key doesn't have children, it will appear as a plain character
            #
            # output: pkg_name::out_fmt
            output <- front$output
         }

         # check for references to an R package in output format
         if (!is.null(output))
         {
            format <- unlist(strsplit(output, "::"))
            if (length(format) > 1)
            {
               discoveries[[format[[1]]]] <- TRUE
            }
         }

         # check for runtime: shiny or parameters (requires the Shiny R package)
         if (identical(front$runtime, "shiny") || 
             identical(front$runtime, "shiny_prerendered") ||
             !is.null(front$params))
         {
            discoveries[["shiny"]] <- TRUE
         }

      }
   }
   
   handleLibraryRequireCall <- function(node) {
      
      # make sure this is a call to the 'library' or 'require' function
      if (!is.call(node))
         return(FALSE)
      
      isLibraryOrRequire <-
         identical(node[[1]], as.name("library")) ||
         identical(node[[1]], as.name("require"))
      
      if (!isLibraryOrRequire)
         return(FALSE)
      
      # attempt to match the call
      matched <- .rs.tryCatch(match.call(base::library, node))
      if (inherits(matched, "error"))
         return(FALSE)
      
      # if the 'package' argument is a character vector of length one, we're done
      isPackageArgumentString <- 
         is.character(matched$package) &&
         length(matched$package) == 1
         
      if (isPackageArgumentString) {
         discoveries[[matched$package]] <<- TRUE
         return(TRUE)
      }
      
      # if it's a symbol, double check character.only argument
      isSafeSymbolLibraryCall <-
         is.symbol(matched$package) &&
         (is.null(matched$character.only) || identical(matched$character.only, FALSE))
      
      if (isSafeSymbolLibraryCall) {
         discoveries[[as.character(matched$package)]] <<- TRUE
         return(TRUE)
      }
      
      FALSE
      
   }
   
   handleRequireNamespaceCall <- function(node) {
      
      if (!is.call(node))
         return(FALSE)
      
      if (!identical(node[[1]], as.name("requireNamespace")))
         return(FALSE)
      
      matched <- .rs.tryCatch(match.call(base::requireNamespace, node))
      if (inherits(matched, "error"))
         return(FALSE)
      
      if (is.character(matched$package) && length(matched$package == 1)) {
         discoveries[[matched$package]] <<- TRUE
         return(TRUE)
      }
      
      FALSE
      
   }
   
   handleColonCall <- function(node) {
      
      # make sure this is a call to the 'library' or 'require' function
      if (!is.call(node))
         return(FALSE)
      
      isColonCall <-
         length(node) == 3 &&
         (identical(node[[1]], as.name("::")) || identical(node[[1]], as.name(":::")))
      
      if (!isColonCall)
         return(FALSE)
      
      # extract package used. note that one can specify the package as
      # both a symbol and as a character string; e.g. this is legal R code
      #
      #   "dplyr"::"mutate"
      #
      # so guard against such usages
      package <- node[[2L]]
      if (is.symbol(package))
         package <- as.character(package)
      if (!is.character(package) || length(package) != 1)
         return(FALSE)
      
      # all looks good; add the discovery
      discoveries[[package]] <<- TRUE
      TRUE
      
   }
   
   .rs.recursiveWalk(parsed, function(node) {
      handleLibraryRequireCall(node) ||
      handleColonCall(node) ||
      handleRequireNamespaceCall(node)
   })
   
   # return discovered packages
   ls(envir = discoveries)
})

.rs.addFunction("discoverPackageDependencies", function(id, extension)
{
   # check to see if we have available packages -- if none, bail
   available <- .rs.availablePackages()
   if (is.null(available$value))
      return(character())
   
   # read the associated source file
   contents <- .rs.readSourceDocument(id)
   if (is.null(contents))
      return(character())

   # parse to find packages
   packages <- .rs.parsePackageDependencies(contents, extension)
   
   # keep only packages that are available in an active repository
   packages <- packages[packages %in% rownames(available$value)]
   
   # figure out which packages aren't actually installed. note that we avoid
   # calling 'installed.packages()' explicitly here as this can be very slow
   # on some NFSes; a plain `list.files()` is fine
   installed <- list.files(.libPaths())
   missing <- setdiff(packages, installed)
   
   # return that list
   missing
   
})

# like strsplit, but doesn't omit a trailing last entry as in e.g.
#
#    > strsplit("a.b.", ".", fixed = TRUE)
#    [[1]]
#    [1] "a" "b"
#
# also unlists the string by default (as the most common usage is to
# split a single string)
.rs.addFunction("strsplit", function(x, pattern, ..., simplify = TRUE)
{
   m <- gregexpr(pattern, x, ...)
   matches <- regmatches(x, m, invert = TRUE)
   if (simplify && length(matches) == 1)
      matches[[1]]
   else
      matches
})

.rs.addJsonRpcHandler("replace_comment_header", function(command, path, code)
{
   .rs.scalar(
      paste(
         deparse(
            do.call(
               "substitute",
               args = list(
                  eval(
                     parse(
                        text = paste("quote(", command, ")", sep = "")
                     )
                  ),
                  list(
                     .code = code,
                     .file = path
                  )
               )
            )
         ),
         collapse = ""
      )
   )
})

.rs.addFunction("withCache", function(name, expr)
{
   cache <- .rs.getVar(name)
   if (!is.null(cache))
      return(cache)
   
   result <- force(expr)
   .rs.setVar(name, result)
   result
})

.rs.addFunction("nullCoalesce", function(x, y)
{
   if (is.null(x)) y else x
})

.rs.addFunction("truncate", function(string, n = 200, marker = "<...>")
{
   if (nchar(string) <= n)
      return(string)
   
   truncated <- substring(string, 1, n - nchar(marker))
   return(paste(truncated, marker))
   
})

.rs.addFunction("formatListForDialog", function(list, sep = ", ", max = 50L)
{
   # count index entries until we get too long
   nc <- 0L
   ns <- nchar(sep)
   for (index in seq_along(list)) {
      nc <- nc + ns + nchar(list[[index]])
      if (nc > max)
         break
   }
   
   # collect items
   n <- length(list)
   items <- list
   
   # subset the list if we overflowed (index didn't reach end)
   # avoid printing 'and 1 other'; no need to subset in that case
   if (index < n - 1L)
      items <- c(list[1:index], paste("and", n - index, "others"))
   
   # paste and truncate once more for safety
   text <- paste(items, collapse = sep)
   .rs.truncate(text, n = max * 2L)
   
})

.rs.addFunction("resolveEnvironment", function(envir)
{
   # if this is already an environment, just return it
   if (is.environment(envir))
      return(envir)
   
   # if this is a numeric, then assume we want an
   # environment by position on the search path
   if (is.numeric(envir))
      return(as.environment(envir))
   
   # treat empty strings as request for globalenv
   if (is.null(envir) || identical(envir, ""))
      return(globalenv())
   
   # if this is the name of something on the search path,
   # then as.environment should suffice
   index <- match(envir, search())
   if (!is.na(index))
      return(as.environment(index))
   
   # if this is the name of a namespace, retrieve that namespace
   if (substring(envir, 1L, 10L) == "namespace:") {
      package <- substring(envir, 11L)
      return(getNamespace(package))
   }
   
   # otherwise, treat 'envir' directly as the name of a namespace
   getNamespace(envir)
   
})
