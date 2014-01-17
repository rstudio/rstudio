#
# SessionEnvironment.R
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
.rs.addFunction("valueAsString", function(val)
{
   tryCatch(
   {
      is.scalarOrVector <- function (x) {
         if (is.null(attributes(x)))
         {
            !is.na(c(NULL=TRUE,
                     logical=TRUE,
                     double=TRUE,
                     integer=TRUE,
                     complex=TRUE,
                     character=TRUE)[typeof(x)])
         }
         else
         {
            FALSE
         }
      }

      if (is.scalarOrVector(val))
      {
         if (length(val) == 0)
            return (paste(.rs.getSingleClass(val), " (empty)"))
         if (length(val) == 1)
         {
            if (nchar(val) < 1024)
                return (deparse(val))
            else
                return (paste(substr(val, 1, 1024), " ..."))
         }
         else if (length(val) > 1)
            return (capture.output(str(val)))
         else
            return ("NO_VALUE")
      }
      else if (.rs.isFunction(val))
         return (.rs.getSignature(val))
      else if (is(val, "Date"))
         return (format(val))
      else
         return ("NO_VALUE")
   },
   error = function(e) 
   { 
      # Don't print errors--they'll appear in the R console. Instead, let the
      # client deal with errors by handling the special value NO_VALUE.
   })

   return ("NO_VALUE")
})

.rs.addFunction("valueContents", function(val)
{
   tryCatch(
   {
      # only return the first 100 lines of detail (generally columns)--any more
      # won't be very presentable in the environment pane. the first line
      # generally contains descriptive text, so don't return that.
      output <- capture.output(str(val))
      return (output[min(length(output), 2):min(length(output),100)])
   },
   error = function(e) { })

   return ("NO_VALUE")
})

.rs.addFunction("isFunction", function(val)
{
   is.function(val) || identical(.rs.getSingleClass(val), "C++Function")
})

# used to create description for promises
.rs.addFunction("promiseDescription", function(obj)
{
   # by default, the description should be the expression associated with the
   # object
   description <- paste(deparse(substitute(obj)), collapse="")

   # create a more friendly description for delay-loaded data
   if (substr(description, 1, 16) == "lazyLoadDBfetch(")
   {
      description <- "<Promise>"
   }
   return (description)
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
      fileattr$filename
   }
   else
      ""
})

.rs.addFunction("sourceCodeFromFunction", function(fun)
{
   if (is.null(attr(fun, "srcref")))
   {
      # The function does not have source refs, so deparse it to get a 
      # formatted representation. 
      paste(deparse(fun), collapse="\n")
   }
   else
   {
      # The function has source refs; use them to get exactly the code that
      # was used to create the function.
      paste(capture.output(attr(fun, "srcref")), collapse="\n")
   }
})

.rs.addFunction("simulateSourceRefs", function(var)
{
  fun <-  attr(var, "_rs_callfun")
  call <- attr(var, "_rs_callobj")
  calltext <- attr(var, "_rs_calltext")

  # To proceed, we need the function to look in, and either the raw call
  # object (which we will deparse later) or the text to look for.
  if (is.null(fun) || 
      (is.null(call) && is.null(calltext)) )
     return(c(0L, 0L, 0L, 0L, 0L, 0L))

  lines <- deparse(fun)

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
  singleline <- paste(slines, collapse=" ")
  
  if (is.null(calltext))
  {
     # No call text specified; deparse into a list of lines
     calltext <- deparse(call)
  }
  else
  {
     # Call text specified as a single character vector; split into a list
     # of lines
     calltext <- unlist(strsplit(calltext, "\n", fixed = TRUE))
  }

  calltext <- sub("\\s+$", "", sub("^\\s+", "", calltext))
  calltext <- paste(calltext, collapse=" ")
  pos <- regexpr(calltext, singleline, fixed = TRUE)

  # Return an empty source ref if we couldn't find a match
  if (pos < 0)
     return(c(0L, 0L, 0L, 0L, 0L, 0L))

  # Compute the starting and ending lines
  endpos <- pos + attr(pos, "match.length")
  firstline <- which(offsets >= pos, arr.ind = TRUE)[1] 
  lastline <- which(offsets >= endpos, arr.ind = TRUE)[1]  
  if (is.na(lastline))
     lastline <- length(offsets)

  # Compute the starting and ending character positions within the line, 
  # taking into account the indents we removed earlier. 
  firstchar <- pos - (if (firstline == 1) 0 else offsets[firstline - 1])
  firstchar <- firstchar + indents[firstline]
  lastchar <- endpos - (if (lastline == 1) 0 else offsets[lastline - 1])
  lastchar <- lastchar + indents[lastline]
  result <- as.integer(c(firstline, firstchar, lastline, 
                         lastchar, firstchar, lastchar))
  return(result)
})

.rs.addFunction("functionNameFromCall", function(call)
{
    return(as.character(substitute(call)))
})

.rs.addFunction("argumentListSummary", function(args)
{
    return(paste(lapply(args, function(arg) {
        if (is.language(arg))
            capture.output(print(arg))
        else if (is.environment(arg) || 
                 is.function(arg))
            deparse(substitute(arg))
        else
            as.character(arg)
        }), collapse = ", "))
})

.rs.addFunction("valueDescription", function(obj)
{
   tryCatch(
   {
      if (missing(obj))
      {
        return("Missing argument")
      }
      else if (is(obj, "ore.frame"))
      {
         return(paste(ncol(obj),"columns"))
      }
      else if (is(obj, "externalptr"))
      {
         return("External pointer")
      }
      else if (is.data.frame(obj))
      {
         return(paste(dim(obj)[1],
                      "obs. of",
                      dim(obj)[2],
                      "variables",
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
              || is.raw(obj))
      {
         return(capture.output(str(obj)))
      }
      else
         return("")
   },
   error = function(e) print(e))

   return ("")
})


.rs.addFunction("registerFunctionEditor", function() {

   # save default editor
   defaultEditor <- getOption("editor")

   # ensure we have a scratch file
   scratchFile <- tempfile()
   cat("", file = scratchFile)

   options(editor = function(name, file, title) {

      # use internal editor for files and functions, otherwise
      # delegate to the default editor
      if (is.null(name) || is.function(name)) {

         # if no name then use file
         if (is.null(name)) {
            if (!is.null(file) && nzchar(file))
               targetFile <- file
            else
               targetFile <- scratchFile
         }
         # otherwise it's a function, write it to a file for editing
         else {
            functionSrc <- .rs.deparseFunction(name, TRUE)
            targetFile <- scratchFile
            writeLines(functionSrc, targetFile)
         }

         # invoke the RStudio editor on the file
         if (.Call("rs_editFile", targetFile)) {

            # try to parse it back in
            newFunc <- try(eval.parent(parse(targetFile)),
                           silent = TRUE)
            if (inherits(newFunc, "try-error")) {
               stop(newFunc, "You can attempt to correct the error using ",
                    title, " = edit()")
            }

            return(newFunc)
         }
         else {
            stop("Error occurred while editing function '", name, "'")
         }
      }
      else
         edit(name, file, title, editor=defaultEditor)
   })
})


.rs.addFunction("getSingleClass", function(obj)
{
   className <- "(unknown)"
   tryCatch(className <- class(obj)[1],
            error = function(e) print(e))
   return (className)
})

.rs.addFunction("describeObject", function(env, objName)
{
   obj <- get(objName, env)
   val <- "(unknown)"
   desc <- ""
   size <- object.size(obj)
   len <- length(obj)
   class <- .rs.getSingleClass(obj)
   contents <- list()
   contents_deferred <- FALSE
   # for language objects, don't evaluate, just show the expression
   if (is.language(obj) || is.symbol(obj))
   {
      val <- deparse(obj)
   }
   else
   {
      # for large objects (> half MB), don't try to get the value, just show
      # the size. Some functions (e.g. str()) can cause the object to be
      # copied, which is slow for large objects.
      if (size > 524288)
      {
         len <- if (len > 1) 
                   paste(len, " elements, ", sep="")
                else 
                   ""
         # data frames are likely to be large, but a summary is still helpful
         if (is.data.frame(obj))
         {
            val <- "NO_VALUE"
            desc <- .rs.valueDescription(obj)
         }
         else
         {
            val <- paste("Large ", class, " (", len, 
                         capture.output(print(size, units="auto")), ")", sep="")
         }
         contents_deferred <- TRUE
      }
      else
      {
         val <- .rs.valueAsString(obj)
         desc <- .rs.valueDescription(obj)

         # expandable object--supply contents 
         if (class == "data.table" ||
             class == "cast_df" ||
             class == "xts" ||
             is.list(obj) || 
             is.data.frame(obj) ||
             isS4(obj))
         {
            contents <- .rs.valueContents(obj)
         }
      }
   }
   list (
      name = .rs.scalar(objName),
      type = .rs.scalar(class),
      is_data = .rs.scalar(is.data.frame(obj)),
      value = .rs.scalar(val),
      description = .rs.scalar(desc),
      size = .rs.scalar(size),
      length = .rs.scalar(length(obj)),
      contents = contents,
      contents_deferred = .rs.scalar(contents_deferred))
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
   # look for the environment's given name; if it doesn't have a name, check
   # the callstack to see if it matches the environment in one of the call 
   # frames.
   result <- environmentName(env)
   if (nchar(result) == 0)
      .rs.environmentCallFrameName(env)$name
   else
      result
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
            envs[[length(envs)+1]] <- frame 
            env <- parent.env(env)
         }
         # otherwise, stop here and get names normally
         else
            break
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
      if (nchar(envName) > 0 &&
          envName != "tools:rstudio" &&
          envName != "Autoloads")
      {
         envs[[length(envs)+1]] <-
                        list (name = .rs.scalar(envName),
                              frame = .rs.scalar(0L),
                              local = .rs.scalar(local))
      }
      env <- parent.env(env)
   }
   envs
})

.rs.addFunction("removeObjects", function(objNames, env)
{
   remove(list=unlist(objNames), envir=env)
})

.rs.addFunction("removeAllObjects", function(includeHidden, env)
{
   rm(list=ls(envir=env, all.names=includeHidden), envir=env)
})

.rs.addFunction("getObjectContents", function(objName, env)
{
   .rs.valueContents(get(objName, env));
})

