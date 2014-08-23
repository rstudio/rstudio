#
# SessionDataImport.R
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
        quote=quote,
        defaultStringsAsFactors=default.stringsAsFactors())
})

.rs.addJsonRpcHandler("get_output_preview", function(path, header, sep, decimal, quote)
{
   nrows <- 20
   output <- .rs.parseDataFile(path, header=header, sep=sep, dec=decimal, quote=quote, nrows=nrows)

   list(output=output,
        outputNames=names(output),
        header=header,
        separator=sep,
        quote=quote,
        defaultStringsAsFactors=default.stringsAsFactors())
})
