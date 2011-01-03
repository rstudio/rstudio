#
# SessionWorkspace.R
#
# Copyright (C) 2009-11 by RStudio, Inc.
#
# This program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

.rs.addJsonRpcHandler("save_workspace", function(filename)
{
   # remember whether the file is new (for file change event)
   if (file.exists(filename))
      fileChangeType = 4
   else
      fileChangeType = 1
   
   # save the file 
   save.image(filename)
})

# NOTE: rpc calls should really be returning structured error values
# that the client can translate into end user error messages. the below
# implementation actually invokes an error dialog on the client directly
# which is definitley not the right long-term direction!
.rs.addJsonRpcHandler("load_workspace", function(filename)
{
   if (file.exists(filename))
   {
      load(filename, envir=globalenv())
   }
   else
   {
      .rs.showErrorMessage("Workspace Error",
                          paste("The file", filename, "does not exist."));
   }
})

.rs.addFunction("valueAsString", function(val)
{
   tryCatch(
   {
      is.scalar <- function (x) {
         if (length(x) == 1 && is.null(attributes(x)))
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

      if (is.scalar(val))
      {
         if (nchar(val) < 100)
            return (deparse(val))
         else
            return ("NO_VALUE")
      }
      else if (is.function(val))
         return (.rs.getSignature(val))
      else
         return ("NO_VALUE")
   },
   error = function(e) print(e))

   return ("NO_VALUE")
})

.rs.addFunction("valueDescription", function(obj)
{
   tryCatch(
   {
      if (is.data.frame(obj))
      {
         return(paste(dim(obj)[1],
                      "obs. of",
                      dim(obj)[2],
                      "variables",
                      sep=" "))
      }
      else if (is.matrix(obj))
      {
         return(paste(ncol(obj),
                      "x",
                      nrow(obj),
                      " ",
                      typeof(obj),
                      " matrix",
                      sep=""))
      }
      else
         return("")
   },
   error = function(e) print(e))

   return ("")
})

.rs.addJsonRpcHandler("remove_all_objects", function()
{
   env = globalenv()
   rm(list=ls(envir=env, all.names=TRUE), envir=env)
})

.rs.addFunction("getSingleClass", function(obj)
{
   className <- "(unknown)"
   tryCatch(className <- class(obj)[1],
            error = function(e) print(e))
   return (className)
})

.rs.addJsonRpcHandler("list_objects", function()
{
   globals = ls(envir=globalenv())
   globalValues = lapply(globals, function (name) {
                            get(name, envir=globalenv(), inherits=FALSE)
                         })
   types = sapply(globalValues, .rs.getSingleClass, USE.NAMES=FALSE)
   lengths = sapply(globalValues, length, USE.NAMES=FALSE)
   values = sapply(globalValues, .rs.valueAsString, USE.NAMES=FALSE)
   extra = sapply(globalValues, .rs.valueDescription, USE.NAMES=FALSE)
   
   result = list(name=globals,
                       type=types,
                       len=lengths,
                       value=values,
                       extra=extra)
   #print(result)
   result
})

.rs.addJsonRpcHandler("get_object_value", function(name)
{
   value = get(name, envir=globalenv(), inherits=FALSE)
   strval = paste(deparse(value), collapse="\n")
   
   list(name=name,
        type=.rs.getSingleClass(value),
        len=length(value),
        value=strval,
        extra=.rs.valueDescription(value))
})

.rs.addJsonRpcHandler("set_object_value", function(name, value)
{
   assign(name, eval(parse(text=value), envir=globalenv()), envir=globalenv(), inherits=FALSE)
   NULL
})

.rs.addFunction("parseDataFile", function(path, header, sep, quote, nrows) {
   data <- tryCatch(
      read.table(path, header=header, sep=sep, quote=quote, nrows=nrows),
      error=function(e) {
         data.frame(Error=e$message)
      })

   oldWidth <- options('width')$width
   options(width=1000)
   output <- format(data)
   options(width=oldWidth)
   return(output)
})

.rs.addJsonRpcHandler("get_data_preview", function(path)
{
   nrows <- 20

   lines <- readLines(path, n=nrows, warn=F)

   # Drop comment lines, leaving the significant ones
   siglines <- grep("^[^#].*", lines, value=TRUE)
   firstline <- siglines[1]

   sep <- ''
   if (length(grep("\\t", firstline)) > 0)
      sep <- "\t"
   else if (length(grep(",", firstline)) > 0)
      sep <- ","
   else if (length(grep(";", firstline)) > 0)
      sep <- ";"

   header <- length(grep("[0-9]", firstline)) == 0

   quote <- "\""

   output <- .rs.parseDataFile(path, header=header, sep=sep, quote=quote, nrows=nrows)

   list(inputLines=paste(lines, collapse="\n"),
        output=output,
        outputNames=names(output),
        header=header,
        separator=sep,
        quote=quote)
})

.rs.addJsonRpcHandler("get_output_preview", function(path, header, sep, quote)
{
   nrows <- 20
   output <- .rs.parseDataFile(path, header=header, sep=sep, quote=quote, nrows=nrows)

   list(output=output,
        outputNames=names(output),
        header=header,
        separator=sep,
        quote=quote)
})
