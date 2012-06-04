#
# SessionRPubs.R
#
# Copyright (C) 2009-1012 by RStudio, Inc.
#
# This program is licensed to you under the terms of version 3 of the
# GNU General Public License. This program is distributed WITHOUT ANY
# EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# GPL (http://www.gnu.org/licenses/gpl-3.0.txt) for more details.
#
#

#' Upload an HTML file to RPubs
#' 
#' This function uploads an HTML file to rpubs.com. If the upload succeeds a 
#' list that includes an \code{id} and \code{continueUrl} is returned. A browser
#' should be opened to the \code{continueURl} to complete publishing of the 
#' document. If an error occurs then a diagnostic message is returned in the 
#' \code{error} element of the list.
#' 
#' @param title The title of the document.
#' @param htmlFile The path to the HTML file to upload.
#' @param id If this upload is an update of an existing document then the id
#'   parameter should specify the document id to update. Note that the id is
#'   provided as an element of the list returned by successful calls to 
#'   \code{rpubsUpload}.
#' @param properties A named list containing additional document properties 
#'   (RPubs doesn't currently expect any additional properties, this parameter 
#'   is reserved for future use).
#' @param method Method to be used for uploading. "internal" uses a plain http
#'   socket connection; "curl" uses the curl binary to do an https upload;
#'   "rcurl" uses the RCurl package to do an https upload; and "auto" uses 
#'   the best available method searched for in the following order: "curl", 
#'   "rcurl", and then "internal". The global default behavior can be 
#'   configured by setting the \code{rpubs.upload.method} option (the default
#'   is "auto").
#'   
#' @return A named list. If the upload was successful then the list contains a 
#'   \code{id} element that can be used to subsequently update the document as
#'   well as a \code{continueUrl} elment that provides a URL that a browser
#'   should be opened to in order to complete publishing of the document. If the
#'   upload fails then the list contains an \code{error} element which contains
#'   an explanation of the error that occurred.
#'   
#' @examples 
#' \dontrun{
#' # upload a document
#' result <- rpubsUpload("My document title", "Document.html")
#' if (!is.null(result$continueUrl))
#'    browseURL(result$continueUrl)
#' else
#'    stop(result$error)
#'    
#' # update the same document with a new title
#' updateResult <- rpubsUpload("My updated title", "Document.html", result$id)
#' }
rpubsUpload <- function(title, 
                        htmlFile, 
                        id = NULL,
                        properties = list(), 
                        method = getOption("rpubs.upload.method")) {
   
   # validate inputs
   if (!is.character(title))
      stop("title must be specified") 
   if (nzchar(title) == FALSE)
      stop("title pmust be a non-empty string")
   if (!is.character(htmlFile))
      stop("htmlFile parameter must be specified")
   if (!file.exists(htmlFile))
      stop("specified htmlFile does not exist")
   if (!is.list(properties))
      stop("properties paramater must be a named list")
   
   # resolve method to auto if necessary
   if (is.null(method))
     method <- "auto"
     
 
   parseHeader <- function(header) {
      split <- strsplit(header, ": ")[[1]]
      if (length(split) == 2)
         return (list(name = split[1], value = split[2]))
      else
         return (NULL)
   }
   
   jsonEscapeString <- function(value) {
      chars <- strsplit(value, "")[[1]]
      chars <- vapply(chars, function(x) {
         if (x %in% c('"', '\\', '/'))
            paste('\\', x, sep='')
         else if (charToRaw(x) < 20)
            paste('\\u', toupper(format(as.hexmode(as.integer(charToRaw(x))), 
                                        width=4)), 
                  sep='')
         else
            x
      }, character(1))
      paste(chars, sep="", collapse="")
   }
   
   jsonProperty <- function(name, value) {
      paste("\"", 
            jsonEscapeString(enc2utf8(name)), 
            "\" : \"", 
            jsonEscapeString(enc2utf8(value)), 
            "\"", 
            sep="")
   }
   
   regexExtract <- function(re, input) {
      match <- regexec(re, input)
      matchLoc <- match[1][[1]]
      if (length(matchLoc) > 1) {
         matchLen <-attributes(matchLoc)$match.length
         url <- substr(input, matchLoc[2], matchLoc[2] + matchLen[2]-1)
         return (url)
      }
      else {
         return (NULL)
      }
   }
   
   # NOTE: we parse the json naively using a regex because:
   #  - We don't want to take a dependency on a json library for just this case
   #  - We know the payload is an ascii url so we don't need a robust parser
   parseContinueUrl <- function(continueUrl) {
      regexExtract("\\{\\s*\"continueUrl\"\\s*:\\s*\"([^\"]+)\"\\s*\\}",
                   continueUrl)
   }
   
   parseHttpStatusCode <- function(statusLine) {
      statusCode <- regexExtract("HTTP/[0-9]+\\.[0-9]+ ([0-9]+).*", statusLine)
      if (is.null(statusCode))
         return (-1)
      else
         return (as.integer(statusCode))
   }
   
   pathFromId <- function(id) {
      split <- strsplit(id, "^https?://[^/]+")[[1]]
      if (length(split) == 2)
         return (split[2])
      else
         return (NULL)
   }
   
   buildPackage <- function(title, 
                            htmlFile, 
                            properties = list()) {
      
      # build package.json
      packageJson <- "{"
      packageJson <- paste(packageJson, jsonProperty("title", title), ",")
      for (name in names(properties)) {
         if (nzchar(name) == FALSE)
            stop("all properties must be named")   
         value <- properties[[name]]
         packageJson <- paste(packageJson, jsonProperty(name, value), ",")
      }
      packageJson <- substr(packageJson, 1, nchar(packageJson)-1)
      packageJson <- paste(packageJson,"}")
      
      # create a tempdir to build the package in and copy the files to it
      fileSep <- .Platform$file.sep
      packageDir <- tempfile()
      dir.create(packageDir)
      packageFile <- function(fileName) {
         paste(packageDir,fileName,sep=fileSep)
      }
      writeLines(packageJson, packageFile("package.json"))
      file.copy(htmlFile, packageFile("index.html"))
       
      # switch to the package dir for building
      oldWd <- getwd()
      setwd(packageDir)
      on.exit(setwd(oldWd))
      
      # create the tarball
      tarfile <- tempfile("package", fileext = ".tar.gz")
      utils::tar(tarfile, files = ".", compression = "gzip")
      
      # return the full path to the tarball
      return (tarfile)
   }
   
   readResponse <- function(conn) {
      # read status code
      resp <- readLines(conn, 1)
      statusCode <- parseHttpStatusCode(resp[1])
      
      # read response headers
      contentLength <- NULL
      location <- NULL
      repeat {
         resp <- readLines(conn, 1)
         if (nzchar(resp) == 0)
            break()
         
         header <- parseHeader(resp)
         if (!is.null(header))
         {
            if (identical(header$name, "Content-Type"))
               contentType <- header$value
            if (identical(header$name, "Content-Length"))
               contentLength <- as.integer(header$value)
            if (identical(header$name, "Location"))
               location <- header$value
         }
      }
      
      # read the response content
      content <- rawToChar(readBin(conn, what = 'raw', n=contentLength))
      
      # return list
      list(status = statusCode,
           location = location,
           contentType = contentType,
           content = content)
   }
   
   # internal sockets implementation of upload (supports http-only)
   internalUpload <- function(path,
                              contentType,
                              headers,
                              packageFile) {
      
      # read file in binary mode
      fileLength <- file.info(packageFile)$size
      fileContents <- readBin(packageFile, what="raw", n=fileLength)
      
      # build http request
      request <- NULL
      request <- c(request, paste("POST ", path, " HTTP/1.1\r\n", sep=""))
      request <- c(request, "User-Agent: RStudio\r\n")
      request <- c(request, "Host: api.rpubs.com\r\n")
      request <- c(request, "Accept: */*\r\n")
      request <- c(request, paste("Content-Type: ", 
                                  contentType, 
                                  "\r\n", 
                                  sep=""))
      request <- c(request, paste("Content-Length: ", 
                                  fileLength, 
                                  "\r\n", 
                                  sep=""))
      for (name in names(headers))
      {
         request <- c(request, 
                      paste(name, ": ", headers[[name]], "\r\n", sep=""))
      }
      request <- c(request, "\r\n")
      
      # open socket connection
      conn <- socketConnection(host="api.rpubs.com",
                               port=80,
                               open="w+b",
                               blocking=TRUE)
      on.exit(close(conn))
      
      # write the request header and file payload
      writeBin(charToRaw(paste(request,collapse="")), conn, size=1)
      writeBin(fileContents, conn, size=1)
      
      # read the response
      readResponse(conn)      
   }
   
   
   rcurlUpload <- function(path,
                           contentType,
                           headers,
                           packageFile) {
      
      require(RCurl)
         
      # url to post to
      url <- paste("https://api.rpubs.com", path, sep="")
      
      # upload package file
      params <- list(file = fileUpload(filename = packageFile,
                                       contentType = contentType))
      
      # use custom header and text gatherers
      options <- curlOptions(url)
      headerGatherer <- basicHeaderGatherer()
      options$headerfunction <- headerGatherer$update
      textGatherer <- basicTextGatherer()
      options$writefunction <- textGatherer$update
      
      # add extra headers
      extraHeaders <- as.character(headers)
      names(extraHeaders) <- names(headers)
      options$httpheader <- extraHeaders
      
      # post the form
      postForm(paste("https://api.rpubs.com", path, sep=""),
                           .params = params,
                           .opts = options,
                           useragent = "RStudio")
  
      # return list
      headers <- headerGatherer$value()
      if ("Location" %in% names(headers))
         location <- headers[["Location"]]
      else
         location <- NULL
      list(status = as.integer(headers[["status"]]),
           location = location,
           contentType <- headers[["Content-Type"]],
           content = textGatherer$value())
      
   }
   
   curlUpload <- function(path,
                          contentType,
                          headers,
                          packageFile) {  
      
      fileLength <- file.info(packageFile)$size
      
      extraHeaders <- character()
      for (header in names(headers))
      {
         extraHeaders <- paste(extraHeaders, "--header")
         extraHeaders <- paste(extraHeaders,  
                               paste(header,":",headers[[header]], sep=""))
      }
      
      outputFile <- tempfile()
      
      command <- paste("curl", 
                       "-X", 
                       "POST",
                       "--data-binary",
                       shQuote(paste("@", packageFile, sep="")),
                       "-i",
                       "--header", paste("Content-Type:",contentType, sep=""),
                       "--header", paste("Content-Length:", fileLength, sep=""),
                       extraHeaders,
                       "--header", "Expect:",
                       "--silent",
                       "--show-error",
                       "-o", shQuote(outputFile),
                       paste("https://api.rpubs.com", path, sep=""))
      
      result <- system(command)
      
      if (result == 0) {
        fileConn <- file(outputFile, "rb")
        on.exit(close(fileConn))
        readResponse(fileConn)
      } else {
        stop(paste("Upload failed (curl error", result, "occurred)"))
      }
   }
   
   uploadFunction <- NULL
   if (is.function(method)) {
      uploadFunction <- method
   } else if (identical("auto", method)) {
      if (nzchar(Sys.which("curl")))
         uploadFunction <- curlUpload
      else if (suppressWarnings(require("RCurl", quietly=TRUE)))
         uploadFunction <- rcurlUpload
      else
         uploadFunction <- internalUpload
   } else if (identical("internal", method)) {
      uploadFunction <- internalUpload
   } else if (identical("curl",  method)) {
      uploadFunction <- curlUpload
   } else if (identical("rcurl", method)) {
      uploadFunction <- rcurlUpload
   } else {
      stop(paste("Invalid upload method specified:",method))  
   }
   
   # build the package
   packageFile <- buildPackage(title, htmlFile, properties)
   
   # determine whether this is a new doc or an update
   isUpdate <- FALSE
   path <- "/api/v1/document"
   headers <- list()
   headers$Connection <- "close"
   if (!is.null(id)) {
      isUpdate <- TRUE
      path <- pathFromId(id)
      headers$`X-HTTP-Method-Override` <- "PUT"
   }
   
   
   # send the request
   result <- uploadFunction(path, 
                            "application/x-compressed",
                            headers,
                            packageFile)
   
   # check for success
   succeeded <- FALSE
   if (isUpdate && (result$status == 200))
      succeeded <- TRUE
   else if (result$status == 201)
      succeeded <- TRUE   
   
   # mark content as UTF-8
   content <- result$content
   Encoding(content) <- "UTF-8"
   
   # return either id & continueUrl or error
   if (succeeded) {
      return (list(id = ifelse(isUpdate, id, result$location), 
                   continueUrl = parseContinueUrl(content)))
   }
   else {
      return (list(error = content))
   }
}

