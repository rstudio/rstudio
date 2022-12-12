#
# SessionEnvironment.R
#
# Copyright (C) 2022 by Posit Software, PBC
#
# Unless you have received this program directly from Posit Software pursuant
# to the terms of a commercial license agreement with Posit Software, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

.rs.addJsonRpcHandler("is_function_masked", function(functionName,
                                                     packageName)
{
   result <- tryCatch(
      .rs.isFunctionMasked(functionName, packageName),
      error = function(e) FALSE
   )
   
   .rs.scalar(result)
})

.rs.addFunction("isFunctionMasked", function(functionName,
                                             packageName)
{
   globalValue  <- get(
      x = functionName,
      envir = globalenv(),
      mode = "function"
   )
   
   packageValue <- get(
      x = functionName,
      envir = asNamespace(packageName),
      mode = "function"
   )
   
   !identical(globalValue, packageValue)
})

.rs.addFunction("valueFromStr", function(val)
{
   .rs.withTimeLimit(1, fail = "<truncated>", {
      capture.output(try(str(val), silent = TRUE))
   })
   
})

.rs.addFunction("valueAsString", function(val)
{
   tryCatch(
      .rs.valueAsStringImpl(val),
      error = function(e) "NO_VALUE"
   )
})

.rs.addFunction("valueAsStringImpl", function(val)
{
   if (missing(val))
   {
      "<missing>"
   }
   else if (is.null(val))
   {
      "NULL"
   }
   else if (is.character(val) && !is.object(val))
   {
      # for plain character variables, we 'mock' the behavior of str()
      # while avoiding the potential for re-encoding (as this could mangle
      # UTF-8 characters on Windows)
      n <- length(val)
      if (n == 0)
      {
         "character (empty)"
      }
      else if (n == 1)
      {
         encodeString(val, quote = "\"", na.encode = FALSE)
      }
      else
      {
         encoded <- encodeString(
            head(val, n = 128L),
            quote = "\"",
            na.encode = FALSE
         )
         
         fmt <- "chr [1:%i] %s"
         txt <- sprintf(fmt, n, paste(encoded, collapse = " "))
         .rs.truncate(txt)
      }
   }
   else if (is.raw(val))
   {
      if (length(val) == 0)
      {
         "raw (empty)"
      }
      else
      {
         .rs.valueFromStr(val)
      }
   }
   else if (is.atomic(val) && is.null(attributes(val)))
   {
      n <- length(val)
      if (n == 0)
      {
         paste(.rs.getSingleClass(val), " (empty)")
      }
      else if (n == 1)
      {
         .rs.truncate(.rs.deparse(val), 1024L)
      }
      else
      {
         .rs.valueFromStr(val)
      }
   }
   else if (inherits(val, "python.builtin.object"))
   {
      .rs.valueFromStr(val)
   }
   else if (.rs.isFunction(val))
   {
      .rs.getSignature(val)
   }
   else if (inherits(val, c("Date", "POSIXct", "POSIXlt")))
   {
      if (length(val) == 1)
      {
         format(val, usetz = TRUE)
      }
      else
      {
         .rs.valueFromStr(val)
      }
   }
   else
   {
      "NO_VALUE"
   }
})

.rs.addFunction("valueContents", function(val)
{
   tryCatch(
      .rs.valueContentsImpl(val),
      error = function(e) "NO_VALUE"
   )
})

.rs.addFunction("valueContentsImpl", function(val)
{
   if (inherits(val, "ore.frame"))
   {
      # for Oracle R frames, show the query if it exists
      query <- attr(val, "dataQry", exact = TRUE)
      
      # no query, show empty
      if (is.null(query))
         return("NO_VALUE") 
      
      # query, display it
      attributes(query) <- NULL
      paste("   Query:", query)
   }
   else if (inherits(val, "pandas.core.frame.DataFrame"))
   {
      output <- reticulate::py_to_r(val$to_string(max_rows = 150L))
      strsplit(output, "\n", fixed = TRUE)[[1]]
   }
   else
   {
      output <- .rs.valueFromStr(val)
      
      n <- length(output)
      
      # only return the first 150 lines of detail (generally columns)--any more
      # won't be very presentable in the environment pane. the first line
      # generally contains descriptive text, so don't return that.
      if (n > 150)
      {
         fmt <- "  [... %i lines omitted]"
         tail <- sprintf(fmt, n - 150)
         output <- c(output[2:150], tail)
      }
      else if (n > 1)
      {
        output <- output[-1]
      }
      
      output
   }
})

.rs.addFunction("isFunction", function(val)
{
   is.function(val) || identical(.rs.getSingleClass(val), "C++Function")
})

# used to create description for promises
.rs.addFunction("promiseDescription", function(obj)
{
   # NOTE: substitute() returns the expression associated with a promise,
   # without forcing it to be evaluated
   expr <- substitute(obj)
   
   # if this appears to be a call to 'lazyLoadDBfetch()', that implies
   # this is lazy-loaded data (typically associated with an R package).
   # handle those up front
   isLazyLoad <-
      is.call(expr) &&
      is.name(expr[[1L]]) &&
      identical(expr[[1L]], as.name("lazyLoadDBfetch"))
   
   if (isLazyLoad)
      return("<Promise>")
   
   # for some calls, e.g. those formed manually or those evaluated via
   # do.call(), the 'expression' here might just be an already-evaluated
   # R object. in such a case, we want to avoid deparsing the object as
   # it could be expensive (especially for large objects).
   if (!is.call(expr))
      return(.rs.valueDescription(expr))
   
   # we have a call; try to deparse it
   .rs.describeCall(expr)
})

# used to create descriptions for language objects and symbols
.rs.addFunction("languageDescription", function(env, objectName)
{
   desc <- "Missing object"
   tryCatch(
   {
      desc <- capture.output(print(get(objectName, env)))
   },
   error = function(e)
   {
      # silently ignore; if the object can't be retrieved from the
      # environment, treat it as missing
   },
   finally =
   {
      return(desc)
   })
})

.rs.addFunction("sourceFileFromRef", function(srcref)
{
   if (!is.null(srcref))
   {
      fileattr <- attr(srcref, "srcfile")
      enc2utf8(fileattr$filename)
   }
   else
      ""
})

# Given a function and some content inside that function, returns a vector
# in the standard R source reference format that represents the location of
# the content in the deparsed representation of the function.
.rs.addFunction("simulateSourceRefs", function(var)
{
  # Read arguments from attached attributes to the input (these can't be passed
  # naked from RStudio)
  fun <-  attr(var, "_rs_callfun")
  call <- attr(var, "_rs_callobj")
  calltext <- attr(var, "_rs_calltext")
  linepref <- attr(var, "_rs_lastline")

  # To proceed, we need the function to look in, and either the raw call
  # object (which we will deparse later) or the text to look for.
  if (is.null(fun) || 
      (is.null(call) && is.null(calltext)) )
     return(c(0L, 0L, 0L, 0L, 0L, 0L))

  lines <- .rs.deparseFunction(fun, FALSE, FALSE)

  # Remember the indentation level on each line (added by deparse), and remove
  # it along with any other leading or trailing whitespace. 
  indents <- nchar(sub("\\S.*", "", lines))
  slines <- sub("\\s+$", "", sub("^\\s+", "", lines))

  # Compute the character position of the start of each line, and collapse the
  # lines to a character vector of length 1. 
  nchars <- 0
  offsets <- integer(length(slines))
  for (i in 1:length(slines)) {
    nchars <- nchars + nchar(slines[i]) + 1
    offsets[i] <- nchars
  }
  
  singleline <- paste(slines, collapse = " ")
  
  if (is.null(calltext))
  {
     # No call text specified; deparse into a list of lines
     # limit length of deparsed output to avoid issues with very large calls
     # 
     # this implies that we might be unable to highlight calls larger than
     # the below number of lines, but in practice such a large highlight
     # would be unlikely to be useful
     # https://github.com/rstudio/rstudio/issues/5158
     calltext <- .rs.deparse(call, nlines = 200L)
  }
  else
  {
     # Call text specified as a single character vector; split into a list
     # of lines
     calltext <- unlist(strsplit(calltext, "\n", fixed = TRUE))
  }

  # Remove leading/trailing whitespace on each line, and collapse the lines
  calltext <- sub("\\s+$", "", sub("^\\s+", "", calltext))
  calltext <- paste(calltext, collapse = " ")

  # Any call text supplied is presumed UTF-8 unless we know otherwise
  if (Encoding(calltext) == "unknown")
     Encoding(calltext) <- "UTF-8"

  # NULL is output by R when it doesn't have an expression to output; don't
  # try to match it to code
  if (identical(calltext, "NULL")) 
     return(c(0L, 0L, 0L, 0L, 0L, 0L))

  pos <- gregexpr(calltext, singleline, fixed = TRUE)[[1]]
  if (length(pos) > 1)
  {
     # There is more than one instance of the call text in the function; try 
     # to pick the first match past the preferred line.
     best <- which(pos > offsets[linepref])
     if (length(best) == 0)
     {
        # No match past the preferred line, just pick the match closest
        best <- which.min(abs(linepref - pos))
     }
     else
        best <- best[1]
     endpos <- pos[best] + attr(pos, "match.length")[best]
     pos <- pos[best]
  }
  else
  {
     endpos <- pos + attr(pos, "match.length")
  }

  # Return an empty source ref if we couldn't find a match
  if (length(pos) == 0 || pos < 0)
     return(c(0L, 0L, 0L, 0L, 0L, 0L))

  # Compute the starting and ending lines
  firstline <- which(offsets >= pos, arr.ind = TRUE)[1]
  lastline <- which(offsets >= endpos, arr.ind = TRUE)[1]
  if (is.na(lastline))
     lastline <- length(offsets)

  # Compute the starting and ending character positions within the line, 
  # taking into account the indents we removed earlier. 
  firstchar <- pos - (if (firstline == 1) 0 else offsets[firstline - 1])
  firstchar <- firstchar + indents[firstline]

  # If the match is a block ({ ... }) and contains more than a few lines, 
  # match the first line instead of the whole block; having the entire contents
  # of the code browser highlighted is not useful. 
  if (substr(calltext, 1, 1) == "{" &&
      substr(calltext, nchar(calltext), nchar(calltext)) == "}" &&
      lastline - firstline > 5)
  {
     lastline <- firstline
     lastchar <- offsets[firstline] - pos
  }
  else
  {
     lastchar <- endpos - (if (lastline == 1) 0 else offsets[lastline - 1])
     lastchar <- lastchar + indents[lastline]
  }

  result <- as.integer(c(firstline, firstchar,
                         lastline,  lastchar,
                         firstchar, lastchar))
  return(result)
})

.rs.addFunction("functionNameFromCall", function(val)
{
   call <- .rs.nullCoalesce(
      attr(val, "_rs_call", exact = TRUE),
      val
   )
   
   if (is.function(call[[1]]))
      return("[Anonymous function]")
   
   if (is.name(call[[1L]]))
      return(as.character(call[[1L]]))
   
   .rs.deparse(call[[1L]])
})

.rs.addFunction("sanitizeCall", function(object)
{
   if (missing(object))
   {
      return(object)
   }
   else if (is.call(object))
   {
      # if this is the concatenation of a large number of objects,
      # then just use a shorter representation of the call
      callee <- object[[1L]]
      long <-
         is.name(callee) &&
         length(object) > 20
      
      if (long)
         return(as.call(list(callee, quote(...))))
      
      # sanitize each call entry separately
      for (i in seq_along(object))
      {
         # assigning NULL to object will remove that entry
         # https://github.com/rstudio/rstudio/issues/9299
         sanitized <- .rs.sanitizeCall(object[[i]])
         if (!missing(sanitized) && !is.null(sanitized))
            object[[i]] <- sanitized
      }
      
      # return object
      object
   }
   else if (is.pairlist(object))
   {
      # handle pairlists specially, primarily because they're
      # used directly within function calls
      object
   }
   else if (!is.language(object))
   {
      # if the object would be very expensive to deparse,
      # just use a placeholder instead
      #
      # note that we still want to accept literals here,
      # e.g. 42, "abc"
      if (!is.object(object) && length(object) == 1)
      {
         object
      }
      else
      {
         type <- .rs.explorer.objectType(object)
         as.name(sprintf("<%s>", type))
      }
   }
   else
   {
      object
   }
})

.rs.addFunction("callSummary", function(val)
{
   call <- .rs.nullCoalesce(
      attr(val, "_rs_call", exact = TRUE),
      val
   )
   
   # some calls might be very large when deparsed, especially when
   # they include R objects which have already been evaluated. this
   # happens most often with calls like:
   #
   #   do.call("fn", list(object))
   #
   # where 'object' is something very large when deparsed. we avoid
   # issues by replacing such objects with a short identifier of their
   # type / class
   call <- .rs.sanitizeCall(call)
   
   # deparse call
   .rs.deparse(call)
   
})

.rs.addFunction("valueDescription", function(obj)
{
   tryCatch(
      .rs.valueDescriptionImpl(obj),
      error = function(e) ""
   )
})

.rs.addFunction("valueDescriptionImpl", function(obj)
{
   if (missing(obj))
   {
      return("Missing argument")
   }
   else if (is.null(obj))
   {
      return("NULL")
   }
   else if (is(obj, "ore.frame"))
   {
      sqlTable <- attr(obj, "sqlTable", exact = TRUE)
      if (is.null(sqlTable))
         return("Oracle R frame") 
      else
         return(paste("Oracle R frame:", sqlTable))
   }
   else if (.rs.isExternalPointer(obj))
   {
      class <- class(obj)
      if (length(class) && !identical(class, "externalptr"))
      {
         fmt <- "External pointer of class '%s'"
         return(sprintf(fmt, class[[1]]))
      }
      else
      {
         return("External pointer")
      }
   }
   else if (is.data.frame(obj))
   {
      return(paste(dim(obj)[1],
                   "obs. of",
                   dim(obj)[2],
                   ifelse(dim(obj)[2] == 1, "variable", "variables"),
                   sep=" "))
   }
   else if (is.environment(obj))
   {
      return("Environment")
   }
   else if (isS4(obj))
   {
      return(paste("Formal class ", is(obj)))
   }
   else if (is.list(obj))
   {
      return(paste("List of ", length(obj)))
   }
   else if (is.matrix(obj)
            || is.numeric(obj)
            || is.factor(obj)
            || is.raw(obj) 
            || is.character(obj)
            || is.logical(obj))
   {
      return(.rs.valueFromStr(obj))
   }
   else
   {
      return("")
   }
})

.rs.addFunction("editor", function(name, file = "", title = file, ...)
{
   # if 'name' is missing, we're likely being invoked by
   # 'utils::file.edit()', so just edit the requested file
   if (missing(name) || is.null(name))
      return(.Call("rs_editFile", file, PACKAGE = "(embedding)"))
   
   # otherwise, we're more likely being invoked by 'edit()', which
   # requires the parsed file to be valid R code -- so handle that
   
   # if no file has been supplied, generate one for the user
   if (is.null(file) || !nzchar(file)) {
      file <- tempfile("rstudio-scratch-", fileext = ".R")
      on.exit(unlink(file), add = TRUE)
   }
   
   # write deparsed R object to file
   deparsed <- if (is.function(name))
      .rs.deparseFunction(name, useSource = TRUE, asString = FALSE)
   else
      deparse(name)
   writeLines(deparsed, con = file)
   
   # invoke edit
   .Call("rs_editFile", file, PACKAGE = "(embedding)")
   
   # attempt to parse-eval the generated content
   eval(parse(file), envir = globalenv())
   
})

.rs.addFunction("registerFunctionEditor", function()
{
   options(editor = .rs.editor)
})


.rs.addFunction("getSingleClass", function(obj)
{
   class(obj)[[1L]]
})

.rs.addFunction("describeCall", function(call)
{
   # defend against very large calls; e.g. those with R objects inlined
   # as part of the call (can happen in do.call contexts)
   #
   # this is primarily used for the Environment pane, where we try to display
   # a single-line summary of an R object -- so we can enforce that when
   # deparsing calls here as well
   #
   # https://github.com/rstudio/rstudio/issues/5158
   sanitized <- .rs.sanitizeCall(call)
   val1 <- .rs.deparse(sanitized, nlines = 1L)
   val2 <- .rs.deparse(sanitized, nlines = 2L)
   
   # indicate if there is more output
   val <- if (!identical(val1, val2))
      paste(val1, "<...>")
   else
      val1
})

.rs.addFunction("describeObject", function(env, objName, computeSize = TRUE)
{
   obj <- get(objName, env)
   
   # https://github.com/rstudio/rstudio/issues/9328
   if (missing(obj))
      obj <- as.name("Missing argument")
   
   if (inherits(obj, "python.builtin.object"))
      return(.rs.reticulate.describeObject(objName, env))
   
   # NOTE (kevin): we previously screened R objects for null pointers here,
   # as we had seen in the distant past that attempting to introspect such
   # objects would cause an R session crash. that no longer appears to be
   # the case so we now no longer perform this check here.
   #
   # https://github.com/rstudio/rstudio/issues/4741
   # https://github.com/rstudio/rstudio/issues/5546
   #
   # however, just in case some users are still effected, we allow users to
   # opt-in to checking for null pointers if required.
   checkNullPtr <- .rs.readUiPref("check_null_external_pointers")
   hasNullPtr <- if (identical(checkNullPtr, TRUE))
      .Call("rs_hasExternalPointer", obj, TRUE, PACKAGE = "(embedding)")
   else
      FALSE
   
   if (hasNullPtr) 
   {
      val <- "<Object with null pointer>"
      desc <- "An R object containing a null external pointer"
      size <- 0
      len <- 0
   }
   else 
   {
      val <- "(unknown)"
      desc <- ""

      # some objects (e.g. ALTREP) have compact representations that are forced to materialize if
      # an attempt is made to compute their metrics exactly; avoid computing the size for these
      size <- if (computeSize) object.size(obj) else 0
      len <- length(obj)
   }
   
   class <- .rs.getSingleClass(obj)
   contents <- list()
   contents_deferred <- FALSE
   
   # for language objects, don't evaluate, just show the expression
   if (is.symbol(obj))
   {
      val <- as.character(obj)
   }
   else if (is.language(obj))
   {
      .rs.describeCall(obj)  
   }
   else if (!hasNullPtr)
   {
      # for large objects (> half MB), don't try to get the value, just show
      # the size. Some functions (e.g. str()) can cause the object to be
      # copied, which is slow for large objects.
      if (size > 524288)
      {
         len_desc <- if (len > 1)
         {
            paste(len, " elements, ", sep = "")
         }
         else
         {
            ""
         }
         
         # data frames are likely to be large, but a summary is still helpful
         if (is.data.frame(obj))
         {
            val <- "NO_VALUE"
            desc <- .rs.valueDescription(obj)
         }
         else
         {
            fmt <- "Large %s (%s %s)"
            val <- sprintf(fmt, class, len_desc, format(size, units = "auto", standard = "SI"))
         }
         contents_deferred <- TRUE
      }
      else
      {
         val <- .rs.valueAsString(obj)
         desc <- .rs.valueDescription(obj)

         # expandable object--supply contents 
         if (is.list(obj) ||  is.data.frame(obj) || isS4(obj) ||
             inherits(obj, c("data.table", "ore.frame", "cast_df", "xts", "DataFrame")))
         {
            if (computeSize)
            {
               # normal object
               contents <- .rs.valueContents(obj)
            }
            else
            {
               # don't prefetch content for altreps
               val <- "NO_VALUE"
               contents_deferred <- TRUE
            }
         }
      }
   }
   
   list(
      name              = .rs.scalar(objName),
      type              = .rs.scalar(class),
      clazz             = c(class(obj), typeof(obj)),
      is_data           = .rs.scalar(is.data.frame(obj)),
      value             = .rs.scalar(val),
      description       = .rs.scalar(desc),
      size              = .rs.scalar(size),
      length            = .rs.scalar(len),
      contents          = contents,
      contents_deferred = .rs.scalar(contents_deferred)
   )
   
})

# returns the name and frame number of an environment from a call frame
.rs.addFunction("environmentCallFrameName", function(env)
{
   numCalls <- length(sys.calls())
   result <- list()
   for (i in 1:numCalls)
   {
      if (identical(sys.frame(i), env))
      {
         calldesc <- paste(deparse(sys.call(i)[[1]]), "()", sep="")
         result <- list(name = .rs.scalar(calldesc), 
                        frame = .rs.scalar(i),
                        local = .rs.scalar(TRUE))
         break
      }
   }
   if (identical(result, list()))
      list(name = .rs.scalar("unknown"), frame = .rs.scalar(0L))
   else
      result
})

# indicate whether the given environment is local (i.e. it comes before the
# global environment in the search path)
.rs.addFunction("environmentIsLocal", function(env)
{
   while (!identical(env, emptyenv())) 
   {
      # if one of this environment's parents is the global environment, it's
      # local
      env = parent.env(env)
      if (identical(env, globalenv())) 
         return(TRUE)
   }
   return(FALSE)
})

.rs.addFunction("environmentName", function(env)
{
   # global environment 
   if (identical(env, globalenv()))
     return(".GlobalEnv")

   # base environment
   if (identical(env, baseenv()))
     return("package:base")

   # look for the environment's given name; if it doesn't have a name, check
   # the callstack to see if it matches the environment in one of the call 
   # frames.
   result <- environmentName(env)
   
   if (nchar(result) == 0)
      .rs.environmentCallFrameName(env)$name
   else
   {
      # resolve namespaces
      if ((result %in% loadedNamespaces()) && 
          identical(asNamespace(result), env))
         paste("namespace:", result, sep = "")
      else
         result
   }
})

.rs.addFunction("environmentList", function(startEnv)
{
   env <- startEnv
   envs <- list()
   local <- TRUE
   # if starting above the global environment, the environments will be
   # unnamed. to provide sensible names for them, look for a matching frame in
   # the callstack.
   if (!identical(env, globalenv()))
   {
      while (!identical(env, globalenv()) &&
             !identical(env, emptyenv()))
      {
         frame <- .rs.environmentCallFrameName(env)
         # if this frame is from the callstack, store it and proceed
         if (frame$frame > 0)
         {
            envs[[length(envs) + 1]] <- frame
            env <- parent.env(env)
         }
         # otherwise, stop here and get names normally
         else
         {
            break
         }
      }
   }
   # we're now past the call-frame portion of the stack; proceed normally
   # through the rest of the search path.
   while (!identical(env, emptyenv()))
   {
      # mark all environments as local until we reach the global 
      # environment
      if (identical(env, globalenv())) 
         local <- FALSE

      envName <- environmentName(env)

      # hide the RStudio internal tools environment and the autoloads
      # environment, and any environment that doesn't have a name
      if (nzchar(envName) && !envName %in% c("tools:rstudio", "Autoloads"))
      {
         envs[[length(envs) + 1]] <- list(
            name = .rs.scalar(envName),
            frame = .rs.scalar(0L),
            local = .rs.scalar(local)
         )
      }
      env <- parent.env(env)
   }
   envs
})

.rs.addFunction("removeObjects", function(objNames, env)
{
   objects <- unlist(objNames)
   rm(list = objects, envir = env)
})

.rs.addFunction("removeAllObjects", function(includeHidden, env)
{
   objects <- ls(envir = env, all.names = includeHidden)
   rm(list = objects, envir = env)
})

.rs.addFunction("getObjectContents", function(objName, env)
{
   object <- get(objName, envir = env)
   .rs.valueContents(object)
})

.rs.addFunction("isAltrep", function(var)
{
   .Call("rs_isAltrep", var, PACKAGE = "(embedding)")
})

.rs.addFunction("hasAltrep", function(var)
{
   .Call("rs_hasAltrep", var, PACKAGE = "(embedding)")
})

.rs.addFunction("resolveContextSourceRefs", function(callfun)
{
   calls <- sys.calls()
   
   for (i in seq_along(calls))
   {
      fn <- sys.function(i)
      if (identical(fn, callfun))
      {
         srcref <- attr(calls[[i]], "srcref", exact = TRUE)
         return(srcref)
      }
   }
   
   NULL
   
})

