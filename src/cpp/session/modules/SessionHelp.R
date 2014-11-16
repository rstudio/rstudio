#
# SessionHelp.R
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

# use html help 
options(help_type = "html")

.rs.addFunction( "httpdPort", function()
{
   as.character(tools:::httpdPort)
})

.rs.addFunction("initHelp", function(port, isDesktop)
{ 
   # function to set the help port directly
   setHelpPort <- function() {
      env <- environment(tools::startDynamicHelp)
      unlockBinding("httpdPort", env)
      assign("httpdPort", port, envir = env)
      lockBinding("httpdPort", env)
   }
   
   # for desktop mode see if R can successfully initialize the httpd
   # server -- if it can't then perhaps localhost ports are blocked,
   # in this case we take over help entirely
   if (isDesktop) 
   {
      # start the help server if it hasn't previously been started
      # (suppress warnings and messages because if there is a problem
      # binding to a local port we are going to patch this up by 
      # redirecting all traffic to our local peer)
      if (tools:::httpdPort <= 0L)
         suppressWarnings(suppressMessages(tools::startDynamicHelp()))
      
      # if couldn't start it then set the help port directly so that
      # help requests still flow through our local peer connection
      if (tools:::httpdPort <= 0L)
      {
         setHelpPort()
         return (TRUE)
      }
      else
      {
         return (FALSE)
      }
   }
   # always take over help in server mode
   else 
   { 
      # stop the help server if it was previously started e.g. by .Rprofile
      if (tools:::httpdPort > 0L)
         suppressMessages(tools::startDynamicHelp(start=FALSE))
      
      # set the help port
      setHelpPort()
      
      # indicate we should handle custom internally
      return (TRUE)
   }
})

.rs.addFunction( "handlerLookupError", function(path, query=NULL, ...)
{
   payload = paste(
      "<h3>R Custom HTTP Handler Not Found</h3>",
      "<p>Unable to locate custom HTTP handler for",
      "<i>", path, "</i>",
      "<p>Is the package which implements this HTTP handler loaded?</p>")
   
   list(payload, "text/html", character(), 404)
});

.rs.addJsonRpcHandler("suggest_topics", function(prefix)
{
   if (getRversion() >= "3.0.0")
      sort(utils:::matchAvailableTopics("", prefix))
   else
      sort(utils:::matchAvailableTopics(prefix))
})

.rs.addFunction("getHelpFromObject", function(object)
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
   objectNames <- objects(ns)
   objects <- mget(objectNames, envir = ns)
   
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
   objectName <- objectNames[[i]]
   .rs.getHelp(topic = objectName, package = namespace)
   
})

.rs.addJsonRpcHandler("get_help", function(what, from, type)
{
   if (type == .rs.acCompletionTypes$OPTION)
      return(.rs.getHelp("options", "base", subset = FALSE))
   
   if (type %in% c(.rs.acCompletionTypes$S4_GENERIC,
                   .rs.acCompletionTypes$S4_METHOD))
   {
      # Try getting methods for the method from the associated package
      if (!is.null(help <- .rs.getHelp(paste(what, "-methods"), from)))
         return(help)
      
      # Try getting help from anywhere
      if (!is.null(help <- .rs.getHelp(what)))
         return(help)
      
      # Give up
      return()
   }
   
   if (type == .rs.acCompletionTypes$FUNCTION)
      return(.rs.getHelpFunction(what, from))
   else if (type == .rs.acCompletionTypes$ARGUMENT)
      return(.rs.getHelpArgument(what, from))
   else if (type == .rs.acCompletionTypes$PACKAGE)
      return(.rs.getHelpPackage(what))
   else if (length(from) && length(what))
      return(.rs.getHelp(what, from))
   else
      return()
})

.rs.addFunction("getHelpFunction", function(name, src, envir = parent.frame())
{
   # If 'src' is the name of something on the searchpath, get that object
   # from the seach path, then attempt to get help based on that object
   pos <- match(src, search(), nomatch = -1L)
   if (pos > 0)
   {
      object <- tryCatch(get(name, pos = pos), error = function(e) NULL)
      if (!is.null(object))
         return(.rs.getHelpFromObject(object))
   }
   
   # Otherwise, check to see if there is an object 'src' in the global env
   # from which we can pull the object
   container <- tryCatch(eval(parse(text = src), envir = .GlobalEnv), error = function(e) NULL)
   if (!is.null(container))
   {
      object <- tryCatch(eval(call("$", container, name)), error = function(e) NULL)
      if (!is.null(object))
         return(.rs.getHelpFromObject(object))
   }
   
   # Otherwise, try to get help in the vanilla way
   .rs.getHelp(name, src)
})

.rs.addFunction("getHelpPackage", function(pkgName)
{
   # We might be getting the completion with colons appended, so strip those out.
   pkgName <- sub(":*$", "", pkgName, perl = TRUE)
   topic <- paste(pkgName, "-package", sep = "")
   .rs.getHelp(topic, pkgName)
})

.rs.addFunction("getHelpArgument", function(functionName, src)
{
   # If 'src' is NULL, assume that we're trying to get something in the global env
   # (this implies we're getting arguments from a user defined function)
   if (is.null(src))
      pos <- 1
   else
      pos <- match(src, search(), nomatch = -1L)
   
   if (pos > 0)
   {
      object <- tryCatch(get(functionName, pos = pos), error = function(e) NULL)
      if (!is.null(object))
         return(.rs.getHelpFromObject(object))
   }
   
   .rs.getHelp(functionName, src)
})

.rs.addFunction("makeHelpCall", function(topic, package = NULL, help_type = "html")
{
   substitute(utils::help(TOPIC, package = PACKAGE, help_type = "html"),
              list(TOPIC = topic,
                   PACKAGE = package))
})

.rs.addFunction("getHelp", function(topic, package = "", subset = TRUE)
{
   # Completions from the search path might have the 'package:' prefix, so
   # lets strip that out.
   package <- sub("package:", "", package, fixed = TRUE)
   
   # Ensure topic is not zero-length
   if (!length(topic))
      topic <- ""
   
   # If the topic is not provided, but we're getting help on e.g.
   # 'stats::rnorm', then split up the topic into the appropriate pieces.
   if (!length(package) && any(grepl(":{2,3}", topic, perl = TRUE)))
   {
      splat <- strsplit(topic, ":{2,3}", perl = TRUE)[[1]]
      topic <- splat[[2]]
      package <- splat[[1]]
   }
   
   helpfiles <- NULL
   if (!length(package) || package == "") {
      helpfiles <- utils::help(topic, help_type = "html")
   } else {
      # NOTE: this can fail if there is no such package 'package'
      helpfiles <- tryCatch(
         
         expr = {
            # NOTE: help does lazy evaluation on 'package',
            # so we have to manually construct the call
            call <- .rs.makeHelpCall(topic, package)
            eval(call)
         },
         
         error = function(e) {
            return(NULL)
         }
      )
   }
   
   .rs.processHelpFileHTML(helpfiles, subset = subset)
})

.rs.addFunction("processHelpFileHTML", function(helpfiles, subset = TRUE)
{
   if (length(helpfiles) <= 0)
      return ()
   
   file = helpfiles[[1]]
   path <- dirname(file)
   dirpath <- dirname(path)
   pkgname <- basename(dirpath)
   
   html = tools:::httpd(paste("/library/", 
                              pkgname, 
                              "/html/", 
                              basename(file),
                              ".html", sep=""),
                        NULL,
                        NULL)$payload
   
   match = suppressWarnings(regexpr('<body>.*</body>', html))
   if (match < 0)
   {
      html = NULL
   }
   else
   {
      html <- substring(html, match + 6, match + attr(match, 'match.length') - 1 - 7)
      
      if (subset)
      {
         slotsMatch <- suppressWarnings(regexpr('<h3>Slots</h3>', html))
         detailsMatch <- suppressWarnings(regexpr('<h3>Details</h3>', html))
         
         match <- if (slotsMatch > detailsMatch) slotsMatch else detailsMatch
         if (match >= 0)
            html = substring(html, 1, match - 1)
      }
   }
   
   obj = tryCatch(get(topic, pos=globalenv()),
                  error = function(e) NULL)
   
   if (is.function(obj))
   {
      sig = .rs.getSignature(obj)
      sig = gsub('^function ', topic, sig)
   }
   else
   {
      sig = NULL
   }
   
   list('html' = html, 'signature' = sig, 'pkgname' = pkgname)
})

.rs.addJsonRpcHandler("show_help_topic", function(what, from, type)
{
   if (type == .rs.acCompletionTypes$FUNCTION)
      .rs.showHelpTopicFunction(what, from)
   else if (type == .rs.acCompletionTypes$ARGUMENT)
      .rs.showHelpTopicArgument(from)
   else if (type == .rs.acCompletionTypes$PACKAGE)
      .rs.showHelpTopicPackage(what)
   else
      .rs.showHelpTopic(what, from)
})

.rs.addFunction("showHelpTopicFunction", function(topic, package)
{
   # Package may actually be a name from the search path, so strip that off.
   package <- sub("^package:", "", package, perl = TRUE)
   
   if (is.null(package) && grepl(":{2,3}", topic, perl = TRUE))
   {
      splat <- strsplit(topic, ":{2,3}", perl = TRUE)[[1]]
      topic <- splat[[2]]
      package <- splat[[1]]
   }
   
   if (!is.null(package))
      requireNamespace(package, quietly = TRUE)
   
   call <- .rs.makeHelpCall(topic, package)
   print(eval(call, envir = parent.frame()))
})

.rs.addFunction("showHelpTopicArgument", function(functionName)
{
   topic <- functionName
   pkgName <- NULL
   
   if (grepl(":{2,3}", functionName, perl = TRUE))
   {
      splat <- strsplit(functionName, ":{2,3}", perl = TRUE)[[1]]
      topic <- splat[[2]]
      pkgName <- splat[[1]]
   }
   
   call <- .rs.makeHelpCall(topic, pkgName)
   print(eval(call, envir = parent.frame()))
})

.rs.addFunction("showHelpTopicPackage", function(pkgName)
{
   pkgName <- sub(":*$", "", pkgName)
   topic <- paste(pkgName, "-package", sep = "")
   call <- .rs.makeHelpCall(topic, pkgName)
   print(eval(call, envir = parent.frame()))
})

.rs.addFunction("showHelpTopic", function(topic, package)
{
   call <- .rs.makeHelpCall(topic, package)
   print(eval(call, envir = parent.frame()))
})

.rs.addJsonRpcHandler("search", function(query)
{
   exactMatch = help(query, help_type="html")
   if (length(exactMatch) == 1)
   {
      print(exactMatch)
      return ()
   }
   else
   {
      paste("help/doc/html/Search?pattern=",
            utils::URLencode(query, reserved = TRUE),
            "&title=1&keyword=1&alias=1",
            sep = "")
   }
})