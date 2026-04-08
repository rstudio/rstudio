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
   .rs.withS3OverridesDisabled({
      .rs.withTimeLimit(1, fail = "<truncated>", {
         capture.output(try(str(val), silent = TRUE))
      })
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
   else if (is.character(val) && !is.object(val) && !is.array(val))
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
      #
      # however, str.default() may incorrectly omit the description header
      # for classed lists when a user-defined str method exists in the global
      # environment (even if that method wasn't dispatched to). detect this
      # by checking if the first line is indented (a data line) vs unindented
      # (a header line). see https://github.com/rstudio/rstudio/issues/16985
      hasHeader <- n > 0 && !.rs.startsWith(output[[1L]], " ")

      if (n > 150)
      {
         fmt <- "  [... %i lines omitted]"
         tail <- sprintf(fmt, n - 150)
         if (hasHeader)
            output <- c(output[2:150], tail)
         else
            output <- c(output[1:150], tail)
      }
      else if (n > 1 && hasHeader)
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

# used to create description for promises (legacy path for R < 4.6.0)
.rs.addFunction("promiseDescription", function(obj)
{
   # NOTE: substitute() returns the expression associated with a promise,
   # without forcing it to be evaluated
   expr <- substitute(obj)
   .rs.promiseExprDescription(expr)
})

# describe a promise from its pre-extracted expression
.rs.addFunction("promiseExprDescription", function(expr)
{
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
   if (is.null(srcref))
      return("")

   # resolve file path for this srcref
   srcfile <- attr(srcref, "srcfile")
   if (is.null(srcfile) || is.null(srcfile$filename))
      return("")

   # check for absolute path in srcref
   filename <- enc2utf8(srcfile$filename)
   if (.rs.isAbsolutePath(filename))
      return(filename)

   # if the path was not absolute, we need to resolve it relative
   # to the working directory associated with the srcref
   if (!is.null(srcfile$wd)) {
      wd <- enc2utf8(srcfile$wd)
      filename <- paste(c(wd, filename), collapse = "/")
   }

   normalizePath(filename, winslash = "/", mustWork = FALSE)
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

  lines <- .rs.deparseFunction(fun, TRUE, FALSE)

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
  if (length(linepref) == 0L || linepref <= 0L)
  {
     endpos <- pos[[1L]] + attr(pos, "match.length")[[1L]]
     pos <- pos[[1L]]
  }
  else if (length(pos) > 1)
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
     {
        best <- best[1]
     }
     
     endpos <- pos[best] + attr(pos, "match.length")[best]
     pos <- pos[best]
  }
  else
  {
     endpos <- pos[[1L]] + attr(pos, "match.length")[[1L]]
     pos <- pos[[1L]]
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
     lastchar <- lastchar + indents[lastline] - 1
  }

  result <- c(
     firstline, firstchar,
     lastline,  lastchar,
     firstchar, lastchar
  )

  as.integer(result)
  
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
      # use .row_names_info() to avoid materializing altrep vectors
      # https://github.com/rstudio/rstudio/issues/13540
      dims <- .Call("rs_dim", obj, PACKAGE = "(embedding)")
      if (is.null(dims))
         dims <- c(-1L, -1L)

      # extract rows, columns
      nr <- dims[[1L]]
      nc <- dims[[2L]]

      # build message
      msg <- sprintf(
         "%s obs. of %s %s",
         if (is.na(nr) || nr < 0L) "??" else as.character(nr),
         if (is.na(nc) || nc < 0L) "??" else as.character(nc),
         if (!is.na(nc) && nc != 1L) "variables" else "variable"
      )

      return(msg)
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

.rs.addFunction("describeObject", function(objName, env)
{
   obj <- get(objName, env)

   # https://github.com/rstudio/rstudio/issues/9328
   if (missing(obj))
      obj <- as.name("Missing argument")

   if (inherits(obj, "python.builtin.object"))
      return(.rs.reticulate.describeObject(objName, env))

   # For S4 objects whose defining package isn't loaded, operations like
   # length(), is(), and str() can trigger S4 method dispatch, which calls
   # .requirePackage() to load the package namespace and its native DLL.
   # If the DLL is broken or missing, this can crash the R session at the
   # OS level (e.g. STATUS_ENTRYPOINT_NOT_FOUND on Windows) in a way that
   # R's error handling cannot intercept. Return a safe minimal description
   # that avoids any S4-dispatching operations.
   # https://github.com/rstudio/rstudio/issues/17353
   if (.rs.isUnloadedS4(obj))
   {
      cls <- class(obj)
      pkg <- attr(cls, "package")
      desc <- sprintf("Formal class '%s' [package \"%s\" not loaded]", cls[[1L]], pkg)
      return(list(
         name              = .rs.scalar(objName),
         type              = .rs.scalar(cls[[1L]]),
         clazz             = c(cls, typeof(obj)),
         is_data           = .rs.scalar(FALSE),
         value             = .rs.scalar(desc),
         description       = .rs.scalar(desc),
         size              = .rs.scalar(0L),
         is_size_estimated = .rs.scalar(FALSE),
         length            = .rs.scalar(0L),
         contents          = list(),
         contents_deferred = .rs.scalar(FALSE)
      ))
   }

   val <- "(unknown)"
   desc <- ""

   # some objects (e.g. ALTREP) have compact representations that are forced to materialize if
   # an attempt is made to compute their metrics exactly; avoid computing the size for these
   size <- .rs.estimatedObjectSize(obj)
   len <- length(obj)

   class <- .rs.getSingleClass(obj)
   contents <- list()
   contents_deferred <- FALSE
   is_size_estimated <- FALSE

   # for language objects, don't evaluate, just show the expression
   if (is.symbol(obj))
   {
      val <- as.character(obj)
   }
   else if (is.language(obj))
   {
      val <- .rs.describeCall(obj)
   }
   else
   {
      # for large objects (> half MB), don't try to get the value, just show
      # the size. Some functions (e.g. str()) can cause the object to be
      # copied, which is slow for large objects.
      if (size > 524288)
      {
         len_desc <- if (len > 1)
         {
            paste(len, "elements, ")
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
            is_size_estimated <- identical(attr(size, "estimate"), TRUE)
            size_formatted <- format(size, units = "auto", standard = "SI")
            if (is_size_estimated)
               size_formatted <- paste(">", size_formatted)
            
            fmt <- "Large %s (%s%s)"
            val <- sprintf(fmt, class, len_desc, size_formatted)
         }
         contents_deferred <- TRUE
      }
      else
      {
         val <- .rs.valueAsString(obj)
         desc <- .rs.valueDescription(obj)

         # expandable object--supply contents
         if (is.list(obj) || is.data.frame(obj) || isS4(obj) ||
             inherits(obj, c("data.table", "ore.frame", "cast_df", "xts", "DataFrame")))
         {
            if (.rs.hasAltrep(obj))
            {
               # don't prefetch content for altreps
               val <- "NO_VALUE"
               contents_deferred <- TRUE
            }
            else
            {
               # normal object
               contents <- .rs.valueContents(obj)
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
      is_size_estimated = .rs.scalar(is_size_estimated),
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

   # Guard against S4 objects whose defining package isn't loaded.
   # See the parallel guard in .rs.describeObject() for details.
   # https://github.com/rstudio/rstudio/issues/17353
   if (.rs.isUnloadedS4(object))
      return(list())

   .rs.valueContents(object)
})

# Check if an object is an S4 instance whose defining package namespace
# is not currently loaded. Operations on such objects (length(), str(), etc.)
# can trigger namespace loading and crash the session if the package's native
# DLL is broken or missing. https://github.com/rstudio/rstudio/issues/17353
.rs.addFunction("isUnloadedS4", function(obj)
{
   if (!isS4(obj))
      return(FALSE)

   cls <- class(obj)
   pkg <- attr(cls, "package")

   is.character(pkg) &&
      length(pkg) == 1L &&
      !is.na(pkg) &&
      nzchar(pkg) &&
      !identical(pkg, ".GlobalEnv") &&
      !isNamespaceLoaded(pkg)
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

.rs.addFunction("environment.isSerializable", function()
{
   tryCatch(
      .rs.environment.isSerializableImpl(globalenv(), 1L),
      error = function(e) TRUE
   )
})

.rs.addFunction("environment.isSerializableImpl", function(value, depth)
{
   # Avoid overly-deep recursions.
   if (depth >= 8L)
      return(TRUE)

   # Skip overly-large objects.
   n <- length(value)
   if (isTRUE(n >= 10000L))
      return(TRUE)

   # Objects containing external pointers cannot be serialized.
   if (typeof(value) %in% c("externalptr", "weakref"))
      return(FALSE)

   # Check for 'known-safe' object classes.
   if (inherits(value, c("data.frame", "igraph")))
      return(TRUE)

   # Check for 'known-unsafe' object classes.
   if (inherits(value, c("ArrowObject", "DBIConnection", "python.builtin.object")))
       return(FALSE)

   # Iterate through other recursive objects.
   if (is.environment(value)) {
      keys <- ls(envir = value, all.names = TRUE)
      for (key in keys) {
         if (!.rs.environment.isSerializableImpl(value[[key]], depth + 1L)) {
            return(FALSE)
         }
      }
   } else if (is.recursive(value)) {
      for (i in seq_along(value)) {
         if (!.rs.environment.isSerializableImpl(value[[i]], depth + 1L)) {
            return(FALSE)
         }
      }
   }

   # Assume that other kinds of objects can be restored.
   TRUE
})

.rs.addFunction("objectSize", function(object)
{
   envir <- parent.frame()
   result <- .Call("rs_objectSize", object, envir, PACKAGE = "(embedding)")
   class(result) <- "object_size"
   result
})

.rs.addFunction("computeNodeSize", function()
{
   as.integer(utils::object.size(quote(expr = )))
})

.rs.addFunction("computeVectorSize", function()
{
   as.integer(utils::object.size(logical()))
})

.rs.addFunction("estimatedObjectSize", function(x)
{
   # avoid invoking object.size() on large character vectors,
   # as this can be slow
   n <- 1E5
   if (is.character(x) && length(unclass(x)) >= n)
   {
      result <- n * .Machine$sizeof.pointer
      class(result) <- "object_size"
      attr(result, "estimate") <- TRUE
      return(result)
   }

   .rs.objectSize(x)
})

# Find the function context for the browser, or at a given depth.
#
# When depth == 0 (BROWSER_FUNCTION), finds the outermost function whose
# execution environment matches the browser's environment (from C++).
# When depth > 0, finds the function at the given depth (inner->outer numbering).
#
# Returns list(depth, env) where depth is the inner->outer depth and
# env is the function's closure environment, or list(0, globalenv()) if
# no matching context was found.
# Capture the current environment at the browser prompt. This function
# is injected into the browser REPL by RReadConsole so that it is
# evaluated by R in the browser's evaluation environment (rho).
# parent.frame() then returns that environment, which is the function
# being debugged. The result is stored via .Call for C++ to read.
.rs.addFunction("captureCurrentEnvironment", function()
{
   invisible(.Call("rs_setCapturedBrowserEnv", parent.frame(), PACKAGE = "(embedding)"))
})

#
# @param depth When 0, find the function context associated with the
#   active browser. When > 0, find the function context at the given
#   inner-to-outer depth.
# @param browserEnv The browser context's closure environment, captured
#   by .rs.captureCurrentEnvironment() which runs in the browser REPL.
#   NULL when not browsing.
.rs.addFunction("getFunctionContext", function(depth = 0L, browserEnv = NULL)
{
   nframe <- sys.nframe() - 1L  # skip our own frame
   if (nframe < 1L)
      return(list(depth = 0L, env = globalenv()))

   foundDepth <- 0L
   foundEnv <- globalenv()

   # Iterate inner-to-outer (matching C++ RCNTXT convention)
   contextDepth <- 0L
   for (i in rev(seq_len(nframe)))
   {
      contextDepth <- contextDepth + 1L
      cloenv <- sys.frame(i)

      if (depth == 0L)
      {
         # BROWSER_FUNCTION mode: find the outermost function whose
         # environment matches the browser's environment
         if (!is.null(browserEnv) && identical(cloenv, browserEnv))
         {
            foundDepth <- contextDepth
            foundEnv <- cloenv
            # keep going -- we want the outermost match
         }
      }
      else if (contextDepth >= depth)
      {
         foundDepth <- contextDepth
         foundEnv <- cloenv
         break
      }
   }

   list(depth = foundDepth, env = foundEnv)
})

# Check if the topmost function on the call stack has the hideFromDebugger
# attribute, indicating it's a debugger-internal function.
.rs.addFunction("inDebugHiddenContext", function()
{
   nframe <- sys.nframe() - 1L  # skip our own frame
   if (nframe < 1L)
      return(FALSE)

   # Walk inner-to-outer looking for the first function context
   for (i in rev(seq_len(nframe)))
   {
      fn <- sys.function(i)

      # If we find a debugger internal function before any user function,
      # hide it from the user callstack.
      if (isTRUE(attr(fn, "hideFromDebugger", exact = TRUE)))
         return(TRUE)

      # If we find a function with source refs (user code), don't hide
      origFn <- .rs.originalFunction(fn)
      if (!is.null(attr(origFn, "srcref", exact = TRUE)))
         return(FALSE)
   }

   FALSE
})

# Helper: get the "original" function, unwrapping S4 trace wrappers
.rs.addFunction("originalFunction", function(fn)
{
   if (isS4(fn))
   {
      orig <- attr(fn, "original")
      if (!is.null(orig)) return(orig)
   }
   fn
})

# Helper: check if a srcref is valid (not NULL, not a symbol like <in-bc-interp>)
.rs.addFunction("isValidSrcref", function(srcref)
{
   !is.null(srcref) && is.integer(srcref)
})

# Helper: resolve source references, handling byte-code compiled contexts.
# For byte-compiled code, the srcref on the call may be the symbol
# <in-bc-interp> rather than a real srcref. In that case, we try to find
# the real srcref by matching the function against the call stack.
.rs.addFunction("resolveCallSrcref", function(callSrcref, callfun)
{
   if (.rs.isValidSrcref(callSrcref))
      return(callSrcref)

   # check for byte-code srcref (a symbol rather than integer vector)
   if (!is.symbol(callSrcref))
      return(NULL)

   # try to resolve via .rs.resolveContextSourceRefs
   tryCatch(
      .rs.resolveContextSourceRefs(callfun),
      error = function(e) NULL
   )
})

# Helper: extract source reference fields as a named list
.rs.addFunction("srcrefData", function(srcref)
{
   if (.rs.isValidSrcref(srcref))
   {
      list(
         line_number          = srcref[1L],
         end_line_number      = srcref[3L],
         character_number     = srcref[5L],
         end_character_number = srcref[6L]
      )
   }
   else
   {
      list(
         line_number          = 0L,
         end_line_number      = 0L,
         character_number     = 0L,
         end_character_number = 0L
      )
   }
})

# Build call frame information for the debugger using sys.*() functions.
#
# This replaces the C++ callFramesAsJson() which walked the RCNTXT linked list.
#
# @param targetDepth The function-context depth of interest (1-based).
#   The function/source context at this depth are returned separately for
#   further introspection by the caller.
# @param lineDebugState A list with 'lastDebugText' and 'lastDebugLine',
#   or NULL if not available.
#
# @return A list with components:
#   - frames: a list of frame descriptor lists (one per function context)
#   - context_callfun: the callfun at targetDepth (or NULL)
#   - context_cloenv: the cloenv at targetDepth (or NULL)
#   - src_context_callfun: the source context's callfun at targetDepth (or NULL)
#   - src_context_call: the source context's call at targetDepth (or NULL)
.rs.addFunction("callFrames", function(targetDepth = 0L,
                                       lineDebugState = NULL,
                                       currentSrcref = NULL)
{
   # Exclude our own frame(s) from the stack. When called from C++ via
   # r::exec::RFunction, our frame is at sys.nframe(). We want only the
   # frames that existed before this call.
   ownFrame <- sys.nframe()
   nframe <- ownFrame - 1L

   if (nframe < 1L)
   {
      return(list(frames = list(), context = NULL, src_context = NULL))
   }

   # sys.parents() and sys.calls() include our own frame; we subset below.
   parents <- sys.parents()[seq_len(nframe)]
   calls   <- sys.calls()[seq_len(nframe)]

   # -- Phase 1: collect srcref-to-environment mapping --
   # For each frame i with a valid srcref, map it to the parent frame's
   # environment. This tells us "where in the parent's code was frame i called?"
   # We iterate inner-to-outer so the first-match-wins policy selects the
   # innermost (most current/specific) srcref, matching the old C++ behavior.
   envSrcrefMap <- new.env(parent = emptyenv())

   for (i in rev(seq_len(nframe)))
   {
      callSrcref <- attr(calls[[i]], "srcref", exact = TRUE)
      callfun <- sys.function(i)
      srcref <- .rs.resolveCallSrcref(callSrcref, callfun)
      if (is.null(srcref))
         next

      p <- parents[i]
      if (p == 0L)
         next

      parentEnv <- sys.frame(p)
      key <- format(parentEnv)
      if (is.null(envSrcrefMap[[key]]))
      {
         envSrcrefMap[[key]] <- list(srcref = srcref, callfun = callfun, call = calls[[i]])
      }
   }

   # -- Phase 2: detect browser context --
   # The browser environment is tracked by C++ (set from onConsolePrompt
   # via getFunctionContext). We read it via .Call because during promise
   # forcing, the browser's environment may not correspond to any
   # sys.frame() visible from R.
   browserCloenv <- .Call("rs_getBrowserEnv", PACKAGE = "(embedding)")
   browserUsed <- FALSE

   # -- Phase 3: build frame descriptors --
   # Iterate from innermost to outermost frame to match the C++ convention
   # where the RCNTXT walk starts at R_GlobalContext (innermost) and goes
   # outward. This means depth 1 = innermost function context.
   frames <- vector("list", nframe)
   contextDepth <- 0L

   resultContext <- NULL
   resultSrcContext <- NULL
   updatedLastDebugLine <- NULL

   for (i in rev(seq_len(nframe)))
   {
      callfun <- sys.function(i)
      cloenv  <- sys.frame(i)
      call    <- calls[[i]]

      contextDepth <- contextDepth + 1L
      origFun <- .rs.originalFunction(callfun)

      # Function name
      functionName <- tryCatch(
         .rs.functionNameFromCall(call),
         error = function(e) ""
      )

      # Error handler / debug hidden checks (attribute-based)
      isErrorHandler <- !is.null(attr(origFun, "errorHandlerType", exact = TRUE))
      isHidden <- isTRUE(attr(origFun, "hideFromDebugger", exact = TRUE))

      # Shiny function label
      shinyLabel <- attr(origFun, "_rs_shinyDebugLabel", exact = TRUE)
      if (is.null(shinyLabel)) shinyLabel <- ""

      # Source reference resolution:
      # For the innermost frame (contextDepth == 1), use the runtime srcref
      # passed from C++ (R_GetCurrentSrcref), which reflects the evaluator's
      # current position inside the function. For outer frames, use the
      # envSrcrefMap which maps each frame's env to the srcref of the call
      # made from that frame (set by the next-inner frame's call srcref).
      if (contextDepth == 1L && .rs.isValidSrcref(currentSrcref))
      {
         srcContext <- list(srcref = currentSrcref, callfun = callfun, call = call)
      }
      else
      {
         envKey <- format(cloenv)
         mapped <- envSrcrefMap[[envKey]]
         if (!is.null(mapped))
         {
            srcContext <- mapped
         }
         else
         {
            # Fall back to the srcref on this context's own call
            callSrcref <- attr(call, "srcref", exact = TRUE)
            resolved <- .rs.resolveCallSrcref(callSrcref, callfun)
            srcContext <- list(srcref = resolved, callfun = callfun, call = call)
         }
      }

      srcref <- srcContext$srcref
      isRealSrcref <- .rs.isValidSrcref(srcref)
      isSourceEquiv <- identical(cloenv, globalenv()) && isRealSrcref

      # File name from srcref
      fileName <- ""
      if (isRealSrcref)
      {
         fileName <- tryCatch(
            .rs.sourceFileFromRef(srcref),
            error = function(e) ""
         )
      }

      aliasedFileName <- .rs.createAliasedPath(fileName)

      # Source ref data (line/character numbers)
      srcrefInfo <- .rs.srcrefData(srcref)

      # Lines from source (for real srcrefs)
      linesText <- ""
      if (isRealSrcref)
      {
         linesText <- tryCatch(
            .rs.readSrcrefLines(srcref, TRUE),
            error = function(e) ""
         )
      }

      # Simulated source refs (for code without real srcrefs)
      if (!isRealSrcref)
      {
         info <- "_rs_sourceinfo"
         attr(info, "_rs_callfun") <- origFun

         isBrowserFrame <- !is.null(browserCloenv) && !browserUsed &&
             identical(cloenv, browserCloenv)
         if (isBrowserFrame)
         {
            browserUsed <- TRUE
            if (!is.null(lineDebugState))
            {
               attr(info, "_rs_calltext") <- lineDebugState$lastDebugText
               attr(info, "_rs_lastline") <- lineDebugState$lastDebugLine
            }
         }
         else
         {
            # Use the call from the next innermost frame (if any) to locate
            # our position within this function's deparsed body. This matches
            # the old C++ code which iterated inner-to-outer and used
            # prevContext.call() -- the inner frame's call tells us where
            # in the outer function's code execution currently is.
            innerIdx <- i + 1L
            if (innerIdx <= nframe)
            {
               attr(info, "_rs_callobj") <- calls[[innerIdx]]
            }
         }

         simSrcref <- tryCatch(
            .rs.simulateSourceRefs(info),
            error = function(e) c(0L, 0L, 0L, 0L, 0L, 0L)
         )

         # Update lastDebugLine for the browser frame (propagated back to C++)
         if (isBrowserFrame && .rs.isValidSrcref(simSrcref))
         {
            updatedLastDebugLine <- simSrcref[1L] - 1L
         }

         srcrefInfo <- .rs.srcrefData(simSrcref)
      }

      # Call summary
      callSummary <- tryCatch(
         .rs.callSummary(call),
         error = function(e) ""
      )

      frame <- c(
         list(
            context_depth          = contextDepth,
            function_name          = functionName,
            is_error_handler       = isErrorHandler,
            is_hidden              = isHidden,
            is_source_equiv        = isSourceEquiv,
            file_name              = fileName,
            aliased_file_name      = aliasedFileName,
            real_sourceref         = isRealSrcref
         ),
         srcrefInfo,
         list(
            lines                  = linesText,
            function_line_number   = 1L,
            call_summary           = callSummary,
            shiny_function_label   = shinyLabel
         )
      )

      frames[[contextDepth]] <- .rs.scalarListFromList(frame)

      # Track the context at the target depth
      if (contextDepth == targetDepth)
      {
         resultContext <- list(
            callfun        = callfun,
            cloenv         = cloenv,
            call           = call,
            functionName   = functionName,
            originalCallfun = origFun,
            hasSourceRefs  = !is.null(attr(origFun, "srcref", exact = TRUE)),
            callFunSourceRefs = attr(origFun, "srcref", exact = TRUE)
         )
         resultSrcContext <- srcContext
      }

   }

   # Trim to actual depth (in case nframe included our own frames)
   frames <- frames[seq_len(contextDepth)]

   list(
      frames             = frames,
      context            = resultContext,
      src_context        = resultSrcContext,
      lastDebugLine      = updatedLastDebugLine
   )
})
