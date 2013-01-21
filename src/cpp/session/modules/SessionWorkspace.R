#
# SessionWorkspace.R
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

.rs.addFunction("isFunction", function(val)
{
   is.function(val) || identical(.rs.getSingleClass(val), "C++Function")
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
      else if (.rs.isFunction(val))
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
      if (is(obj, "ore.frame"))
      {
         return(paste(ncol(obj),"columns"))
      }
      else if (is.data.frame(obj))
      {
         return(paste(dim(obj)[1],
                      "obs. of",
                      dim(obj)[2],
                      "variables",
                      sep=" "))
      }
      else if (is.matrix(obj))
      {
         return(paste(nrow(obj),
                      "x",
                      ncol(obj),
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

.rs.addFunction("parseDataFile", function(path, header, sep, dec, quote, nrows) {
   data <- tryCatch(
      # try to use read.csv directly if possible (since this is a common case
      # and since LibreOffice spreadsheet exports produce files unparsable
      # by read.table). check Workspace.makeCommand if we want to deduce
      # other more concrete read calls.
      if (identical(sep,",") && identical(dec,".") && identical(quote,"\""))
         read.csv(path, header=header, nrows=nrows)
      else
         read.table(path, header=header, sep=sep, dec=dec, quote=quote, nrows=nrows),
      error=function(e) {
         data.frame(Error=e$message)
      })

   oldWidth <- options('width')$width
   options(width=1000)
   output <- format(data)
   options(width=oldWidth)
   return(output)
})

.rs.addJsonRpcHandler("download_data_file", function(url)
{
   # download the file
   downloadPath <- tempfile("data")
   download.file(url, downloadPath)

   # return the path
   downloadInfo <- list()
   downloadInfo$path = downloadPath

   # also return a suggested variable name
   downloadInfo$varname <- "dataset"
   urlBasename <- basename(url)
   if (length(urlBasename) > 0)
   {
      fileComponents <- unlist(strsplit(urlBasename, ".", fixed = TRUE))
      components <- length(fileComponents)
      if (components >= 1)
         downloadInfo$varname <- paste(fileComponents[1:components-1],
                                       collapse=".")
   }

   return (downloadInfo)
})

.rs.addJsonRpcHandler("get_data_preview", function(path)
{
   nrows <- 20

   lines <- readLines(path, n=nrows, warn=F)

   # Drop comment lines, leaving the significant ones
   siglines <- grep("^[^#].*", lines, value=TRUE)
   firstline <- siglines[1]

   dataline <- siglines[2]
   if (is.na(dataline) || length(grep("[^\\s]+", dataline)) == 0)
      dataline <- firstline

   sep <- ''
   if (length(grep("\\t", firstline)) > 0) {
      sep <- "\t"
   } else if (length(grep(";", firstline)) > 0) {
      sep <- ";"
   } else if (length(grep(",", firstline)) > 0) {
      sep <- ","
   }

   dec <- '.'
   if (length(grep("\\.", dataline)) == 0
         && length(grep(",", dataline)) > 0
         && sep != ",")
   {
      dec <- ','
   }

   header <- length(grep("[0-9]", firstline)) == 0

   quote <- "\""

   output <- .rs.parseDataFile(path,
                               header=header,
                               sep=sep,
                               dec=dec,
                               quote=quote,
                               nrows=nrows)

   list(inputLines=paste(lines, collapse="\n"),
        output=output,
        outputNames=names(output),
        header=header,
        separator=sep,
        decimal=dec,
        quote=quote)
})

.rs.addJsonRpcHandler("get_output_preview", function(path, header, sep, decimal, quote)
{
   nrows <- 20
   output <- .rs.parseDataFile(path, header=header, sep=sep, dec=decimal, quote=quote, nrows=nrows)

   list(output=output,
        outputNames=names(output),
        header=header,
        separator=sep,
        quote=quote)
})
