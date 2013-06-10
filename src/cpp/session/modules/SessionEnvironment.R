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
      else
         return ("NO_VALUE")
   },
   error = function(e) print(e))

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
   error = function(e) print(e))

   return ("NO_VALUE")
})

.rs.addFunction("isFunction", function(val)
{
   is.function(val) || identical(.rs.getSingleClass(val), "C++Function")
})

.rs.addFunction("promiseDescription", function(obj)
{
   # by default, the description should be the expression associated with the
   # promise
   description <- capture.output(substitute(obj))

   # create a more friendly description for delay-loaded data
   if (substr(description, 1, 16) == "lazyLoadDBfetch(")
   {
      description <- "Data (not yet loaded)"
   }
   return (description)
})

.rs.addFunction("sourceFileFromRef", function(srcref)
{
    return(capture.output(print(attr(srcref, "srcfile"))))
})

.rs.addFunction("argumentListSummary", function(args)
{
    argSummary <- ""
    for (arg in args)
    {
        thisArg <- if (is.language(arg))
                capture.output(print(arg))
            else
                as.character(arg)
        argSummary <- paste(argSummary, thisArg, sep =
            if (argSummary == "") "" else ", ")
    }
    return(argSummary)
})

.rs.addFunction("valueDescription", function(obj)
{
   tryCatch(
   {
      if (is(obj, "ore.frame"))
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
         return(paste("Environment with ", length(obj), " object(s) "))
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
              || is.factor(obj))
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


.rs.addJsonRpcHandler("remove_all_objects", function(includeHidden)
{
   env = globalenv()
   rm(list=ls(envir=env, all.names=includeHidden), envir=env)
})

.rs.addFunction("getSingleClass", function(obj)
{
   className <- "(unknown)"
   tryCatch(className <- class(obj)[1],
            error = function(e) print(e))
   return (className)
})

