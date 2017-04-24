#
# SessionEnvironment.R
#
# Copyright (C) 2009-16 by RStudio, Inc.
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

.rs.addFunction("valueFromStr", function(val)
{
   .rs.withTimeLimit(1, fail = "<truncated>", {
      capture.output(try(str(val), silent = TRUE))
   })
   
})

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
            quotedVal <- deparse(val)
            if (nchar(quotedVal) < 1024)
                return (quotedVal)
            else
                return (paste(substr(quotedVal, 1, 1024), " ..."))
         }
         else if (length(val) > 1)
            return (.rs.valueFromStr(val))
         else
            return ("NO_VALUE")
      }
      else if (is(val, "python.builtin.object")) {
         return (.rs.valueFromStr(val))
      }
      else if (.rs.isFunction(val))
         return (.rs.getSignature(val))
      else if (is(val, "Date") || is(val, "POSIXct") || is(val, "POSIXlt")) {
         if (length(val) == 1)
           return (format(val))
         else
           return (.rs.valueFromStr(val))
      }
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
      # for Oracle R frames, show the query if it exists
      if (is(val, "ore.frame")) 
      {
        query <- attr(val, "dataQry", exact = TRUE) 

        # no query, show empty
        if (is.null(query))
          return("NO_VALUE") 

        # query, display it
        attributes(query) <- NULL
        return(paste("   Query:", query))
      }

      # only return the first 150 lines of detail (generally columns)--any more
      # won't be very presentable in the environment pane. the first line
      # generally contains descriptive text, so don't return that.
      output <- .rs.valueFromStr(val)
      lines <- length(output)
      if (lines > 150) {
        output <- c(output[2:150], 
                    paste("  [... ", lines - 150, " lines omitted]", 
                          sep = ""))
      } 
      else if (lines > 1) {
        output <- output[-1]
      }
      return(output)
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

  # Remove leading/trailing whitespace on each line, and collapse the lines
  calltext <- sub("\\s+$", "", sub("^\\s+", "", calltext))
  calltext <- paste(calltext, collapse=" ")

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

  result <- as.integer(c(firstline, firstchar, lastline, 
                         lastchar, firstchar, lastchar))
  return(result)
})

.rs.addFunction("functionNameFromCall", function(val)
{
   call <- attr(val, "_rs_call")
   if (is.function(call[[1]]))
      "[Anonymous function]"
   else
      as.character(substitute(call))
})

.rs.addFunction("callSummary", function(val)
{
   deparse(attr(val, "_rs_call"))
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
        sqlTable <- attr(obj, "sqlTable", exact = TRUE)
        if (is.null(sqlTable))
          return("Oracle R frame") 
        else
          return(paste("Oracle R frame:", sqlTable))
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
            functionSrc <- .rs.deparseFunction(name, TRUE, FALSE)
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
   # objects containing null external pointers can crash when
   # evaluated--display generically (see case 4092)
   hasNullPtr <- .rs.hasNullExternalPointer(obj)
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
      size <- object.size(obj)
      len <- length(obj)
   }
   class <- .rs.getSingleClass(obj)
   contents <- list()
   contents_deferred <- FALSE
   # for language objects, don't evaluate, just show the expression
   if (is.language(obj) || is.symbol(obj))
   {
      val <- deparse(obj)
   }
   else if (!hasNullPtr)
   {
      # for large objects (> half MB), don't try to get the value, just show
      # the size. Some functions (e.g. str()) can cause the object to be
      # copied, which is slow for large objects.
      if (size > 524288)
      {
         len_desc <- if (len > 1) 
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
            val <- paste("Large ", class, " (", len_desc, 
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
             class == "ore.frame" ||
             class == "cast_df" ||
             class == "xts" ||
             class == "DataFrame" ||
             is.list(obj) || 
             is.data.frame(obj) ||
             isS4(obj))
         {
            contents <- .rs.valueContents(obj)
         }
      }
   }
   list(
      name = .rs.scalar(objName),
      type = .rs.scalar(class),
      clazz = c(class(obj), typeof(obj)),
      is_data = .rs.scalar(is.data.frame(obj)),
      value = .rs.scalar(val),
      description = .rs.scalar(desc),
      size = .rs.scalar(size),
      length = .rs.scalar(len),
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
         paste("namespace:", result, sep="")
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

# attempt to determine whether the given object contains a null external
# pointer
.rs.addFunction("hasNullExternalPointer", function(obj)
{
   if (isS4(obj)) 
   {
      # this is an S4 object; recursively check its slots for null pointers
      any(sapply(slotNames(obj), function(name) {
         hasNullPtr <- FALSE
         # it's possible to cheat the S4 object system and destroy the contents
         # of a slot via attr<- assignments; in this case slotNames will
         # contain slots that don't exist, and trying to access those slots 
         # throws an error.
         tryCatch({
           hasNullPtr <- .rs.hasNullExternalPointer(slot(obj, name))
           }, 
           error = function(err) {})
         hasNullPtr
      }))
   } 
   else
   {
      # check the object itself for a null pointer
      inherits(obj, "externalptr") && capture.output(print(obj)) == "<pointer: 0x0>"
   }
})



