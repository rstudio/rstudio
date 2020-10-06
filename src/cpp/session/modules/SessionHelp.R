#
# SessionHelp.R
#
# Copyright (C) 2020 by RStudio, PBC
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

.rs.addFunction( "httpdPortIsFunction", function() {
   is.function(tools:::httpdPort)
})

.rs.addFunction( "httpdPort", function()
{
   if (.rs.httpdPortIsFunction())
      as.character(tools:::httpdPort())
   else
      as.character(tools:::httpdPort)
})

.rs.addFunction("initHelp", function(port, isDesktop)
{ 
   # function to set the help port
   setHelpPort <- function() {
      if (.rs.httpdPortIsFunction()) {
         tools:::httpdPort(port)
      } else {
         env <- environment(tools::startDynamicHelp)
         unlockBinding("httpdPort", env)
         assign("httpdPort", port, envir = env)
         lockBinding("httpdPort", env)   
      }
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
      if (.rs.httpdPort() <= 0L)
         suppressWarnings(suppressMessages(tools::startDynamicHelp()))
      
      # if couldn't start it then set the help port directly so that
      # help requests still flow through our local peer connection
      if (.rs.httpdPort() <= 0L)
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
      if (.rs.httpdPort() > 0L)
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

.rs.setVar("topicsEnv", new.env(parent = emptyenv()))

.rs.addJsonRpcHandler("suggest_topics", function(query)
{
   pkgpaths <- path.package(quiet = TRUE)
   
   # read topics from
   topics <- lapply(pkgpaths, function(pkgpath) tryCatch({
      
      if (exists(pkgpath, envir = .rs.topicsEnv))
         return(get(pkgpath, envir = .rs.topicsEnv))
      
      aliases <- file.path(pkgpath, "help/aliases.rds")
      index <- file.path(pkgpath, "help/AnIndex")
      
      value <- if (file.exists(aliases)) {
         names(readRDS(aliases))
      } else if (file.exists(index)) {
         data <- read.table(index, sep = "\t")
         data[, 1]
      }
      
      assign(pkgpath, value, envir = .rs.topicsEnv)
      
   }, error = function(e) NULL))
   
   flat <- unlist(topics, use.names = FALSE)
   
   # order matches by subsequence match score
   scores <- .rs.scoreMatches(tolower(flat), tolower(query))
   ordered <- flat[order(scores)]
   matches <- unique(ordered[.rs.isSubsequence(tolower(ordered), tolower(query))])
   
   # force first character to match, but allow typos after.
   # also keep matches with one or more leading '.', so that e.g.
   # the prefix 'libpaths' can match '.libPaths'
   if (nzchar(query)) {
      first <- substring(query, 1, 1)
      pattern <- sprintf("^[.]*[%s]", first)
      matches <- grep(pattern, matches, value = TRUE, perl = TRUE)
   }
   
   matches
   
})

.rs.addFunction("getHelpFromObject", function(object, envir, name = NULL)
{
   # Try to find the associated namespace of the object
   namespace <- NULL
   if (is.primitive(object))
      namespace <- "base"
   else if (is.function(object))
   {
      envString <- .rs.format(environment(object))[[1L]]
      
      # Strip out the irrelevant bits of the package name. We'd like
      # to just use 'regexpr' but its output is funky with older versions
      # of R.
      if (!grepl("namespace:", envString))
         return()
      
      namespace <- sub(".*namespace:", "", envString)
      namespace <- sub(">.*", "", namespace)
   }
   else if (isS4(object))
      namespace <- attr(class(object), "package")
   
   if (is.null(namespace))
      return()
   
   # Get objects from that namespace
   ns <- try(asNamespace(namespace), silent = TRUE)
   if (inherits(ns, "try-error"))
      return()
   
   # Datasets don't live in the namespace -- ennumerate them separately
   # We have to avoid the 'base' package, though.
   datasets <- tryCatch(
      suppressWarnings(data(package = namespace)),
      error = function(e) NULL
   )
   
   objectNames <- objects(ns, all.names = TRUE)
   datasetNames <- unname(grep(" ", datasets$results[, "Item"], fixed = TRUE, value = TRUE, invert = TRUE))
   
   objects <- tryCatch(
      mget(objectNames, envir = ns, inherits = TRUE),
      error = function(e) NULL
   )
   
   # Try to get the datasets from the namespace. These will only exist if they have
   # been explicitly loaded, so be careful to tryCatch here.
   data <- lapply(datasetNames, function(x) {
      tryCatch(
         get(x, envir = ns),
         error = function(e) NULL
      )
   })
   
   # Combine them together
   if (length(data))
   {
      objects <- c(objects, data)
      objectNames <- c(objectNames, datasetNames)
   }
   
   # Find which object is actually identical to the one we have
   success <- FALSE
   for (i in seq_along(objects))
   {
      if (!identical(class(object), class(objects[[i]])))
         next
      
      # Once again, 'ignore.environment' is not available in older R's
      # identical, so construct and eval a call to 'base::identical'.
      formals <- as.list(formals(base::identical))
      formals$x <- object
      formals$y <- objects[[i]]
      if ("ignore.environment" %in% names(formals))
         formals[["ignore.environment"]] <- TRUE
      
      result <- tryCatch(
         do.call(base::identical, formals),
         error = function(e) FALSE
      )
      
      if (result)
      {
         success <- TRUE
         break
      }
   }
   
   if (success)
   {
      # Use that name for the help lookup
      object <- objects[[i]]
      objectName <- objectNames[[i]]
      
      # use the function name seen in the
      # source document if provided
      sigName <- if (is.null(name))
         objectName
      else
         name
      
      # Get the associated signature for functions
      signature <- NULL
      if (is.function(object))
         signature <- sub("function ", sigName, .rs.getSignature(object))
      
      result <- .rs.getHelp(topic = objectName, package = namespace, sig = signature)
      if (length(result))
         return(result)
      
      # If the previous lookup failed, perhaps it was an S3 method for which no
      # documentation was available. Fall back to generic documentation.
      dotPos <- gregexpr(".", objectName, fixed = TRUE)[[1]]
      for (i in seq_along(dotPos))
      {
         maybeGeneric <- substring(objectName, 1, dotPos[[i]] - 1)
         methods <- suppressWarnings(
            tryCatch(
               eval(substitute(methods(x), list(x = maybeGeneric)), envir = envir),
               error = function(e) NULL
            )
         )
         
         if (objectName %in% methods)
         {
            result <- .rs.getHelp(maybeGeneric)
            if (length(result))
               return(result)
         }
      }
   }
   
   # Fail -- return NULL
   NULL
})

.rs.addJsonRpcHandler("get_help", function(what, from, type)
{
   # Protect against missing type
   if (!length(type))
      return()
   
   # If we've encoded the package and function in 'what', pull it out
   if (grepl("|||", what, fixed = TRUE))
   {
      splat <- strsplit(what, "|||", fixed = TRUE)[[1]]
      from <- splat[[1]]
      what <- splat[[2]]
   }
   
   # Avoid install.packages hook
   if (what == "install.packages" &&
       type == .rs.acCompletionTypes$ARGUMENT &&
       is.null(from))
      return(.rs.getHelp("install.packages", "utils"))
   
   # Help for options
   if (type == .rs.acCompletionTypes$OPTION)
      return(.rs.getHelp("options", "base", subset = FALSE))
   
   if (type %in% c(.rs.acCompletionTypes$S4_GENERIC,
                   .rs.acCompletionTypes$S4_METHOD))
   {
      # Try getting methods for the method from the associated package
      if (!is.null(help <- .rs.getHelp(paste(what, "methods", sep = "-"), from)))
         return(help)
      
      # Try getting help from anywhere
      if (!is.null(help <- .rs.getHelp(what, from)))
         return(help)
      
      # Give up
      return()
   }
   
   if (type %in% c(.rs.acCompletionTypes$FUNCTION,
                   .rs.acCompletionTypes$S4_GENERIC,
                   .rs.acCompletionTypes$S4_METHOD,
                   .rs.acCompletionTypes$R5_METHOD))
      return(.rs.getHelpFunction(what, from))
   else if (type == .rs.acCompletionTypes$ARGUMENT)
      return(.rs.getHelpArgument(what, from, parent.frame()))
   else if (type == .rs.acCompletionTypes$PACKAGE)
      return(.rs.getHelpPackage(what))
   else if (length(from) && length(what))
      return(.rs.getHelp(what, from))
   else
      return()
})

.rs.addJsonRpcHandler("get_custom_help", function(helpHandler,
                                                  topic,
                                                  source,
                                                  language)
{
   # use own handler for Python language help
   if (identical(language, "Python"))
      return(.rs.python.getHelp(topic, source))
   
   helpHandlerFunc <- tryCatch(eval(parse(text = helpHandler)), 
                               error = function(e) NULL)
   if (!is.function(helpHandlerFunc))
      return()
   
   results <- helpHandlerFunc("completion", topic, source)
   if (!is.null(results))
      results$description <- .rs.markdownToHTML(results$description)
     
   results 
})

.rs.addJsonRpcHandler("get_custom_parameter_help", function(helpHandler,
                                                            source,
                                                            language)
{
   # use own handler for Python language help
   if (identical(language, "Python"))
      return(.rs.python.getParameterHelp(source))
   
   helpHandlerFunc <- tryCatch(eval(parse(text = helpHandler)), 
                               error = function(e) NULL)
   if (!is.function(helpHandlerFunc))
      return()
   
   results <- helpHandlerFunc("parameter", NULL, source)
   if (!is.null(results)) {
      results$arg_descriptions <- sapply(results$arg_descriptions, 
                                         .rs.markdownToHTML)
   }
   
   results
})

.rs.addJsonRpcHandler("show_custom_help_topic", function(helpHandler, topic, source) {
   
   helpHandlerFunc <- tryCatch(eval(parse(text = helpHandler)), 
                               error = function(e) NULL)
   if (!is.function(helpHandlerFunc))
      return()
   
   url <- helpHandlerFunc("url", topic, source)
   if (!is.null(url) && nzchar(url)) # handlers return "" for no help topic
      utils::browseURL(url)
})


.rs.addFunction("getHelpFunction", function(name, src, envir = parent.frame())
{
   # If 'src' is the name of something on the searchpath, get that object
   # from the seach path, then attempt to get help based on that object
   pos <- match(src, search(), nomatch = -1L)
   if (pos >= 0)
   {
      object <- tryCatch(get(name, pos = pos), error = function(e) NULL)
      if (!is.null(object))
         return(.rs.getHelpFromObject(object, envir, name))
   }
   
   # Otherwise, check to see if there is an object 'src' in the global env
   # from which we can pull the object
   container <- tryCatch(eval(parse(text = src), envir = .GlobalEnv), error = function(e) NULL)
   if (!is.null(container))
   {
      object <- tryCatch(eval(call("$", container, name)), error = function(e) NULL)
      if (!is.null(object))
         return(.rs.getHelpFromObject(object, envir, name))
   }
   
   # Otherwise, try to get help in the vanilla way
   .rs.getHelp(name, src, getSignature = TRUE)
})

.rs.addFunction("getHelpPackage", function(pkgName)
{
   # We might be getting the completion with colons appended, so strip those out.
   pkgName <- sub(":*$", "", pkgName, perl = TRUE)
   topic <- paste(pkgName, "-package", sep = "")
   .rs.getHelp(topic, pkgName)
})

.rs.addFunction("getHelpArgument", function(functionName, src, envir)
{
   if (is.null(src))
   {
      object <- .rs.getAnywhere(functionName, envir)
      if (!is.null(object))
         return(.rs.getHelpFromObject(object, envir, functionName))
   }
   else
   {
      pos <- match(src, search(), nomatch = -1L)
      
      if (pos >= 0)
      {
         object <- tryCatch(get(functionName, pos = pos), error = function(e) NULL)
         if (!is.null(object))
            return(.rs.getHelpFromObject(object, envir, functionName))
      }
   }
   
   .rs.getHelp(functionName, src)
})

.rs.addFunction("makeHelpCall", function(topic,
                                         package = NULL,
                                         help_type = "html")
{
   substitute(utils::help(TOPIC, package = PACKAGE, help_type = "html"),
              list(TOPIC = topic,
                   PACKAGE = package))
})

.rs.addFunction("getHelp", function(topic,
                                    package = "",
                                    sig = NULL,
                                    subset = TRUE,
                                    getSignature = FALSE)
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
   
   # If 'package' is the name of something on the search path, then we
   # attempt to resolve the object and get its help.
   if (length(package))
   {
      pos <- match(package, search(), nomatch = -1L)
      if (pos >= 0)
      {
         object <- tryCatch(get(topic, pos = pos), error = function(e) NULL)
         if (is.null(object))
            return(NULL)
         return(.rs.getHelpFromObject(object, envir, topic))
      }
   }
   
   helpfiles <- NULL
   if (!length(package) || package == "") {
      helpfiles <- utils::help(topic, help_type = "html")
   } else {
      helpfiles <- tryCatch(
         
         expr = {
            call <- .rs.makeHelpCall(topic, package)
            eval(call)
         },
         
         error = function(e) {
            return(NULL)
         }
      )
   }
   
   if (length(helpfiles) <= 0)
      return ()
   
   file = helpfiles[[1]]
   path <- dirname(file)
   dirpath <- dirname(path)
   pkgname <- basename(dirpath)
   
   query <- paste("/library/", pkgname, "/html/", basename(file), ".html", sep = "")
   html <- suppressWarnings(tools:::httpd(query, NULL, NULL))$payload
   
   match = suppressWarnings(regexpr('<body>.*</body>', html))
   if (match < 0)
   {
      html = NULL
   }
   else
   {
      html = substring(html, match + 6, match + attr(match, 'match.length') - 1 - 7)
      
      if (subset)
      {   
         slotsMatch <- suppressWarnings(regexpr('<h3>Slots</h3>', html))
         detailsMatch <- suppressWarnings(regexpr('<h3>Details</h3>', html))
         
         match <- if (slotsMatch > detailsMatch) slotsMatch else detailsMatch
         if (match >= 0)
            html = substring(html, 1, match - 1)
      }
   }
   
   # Try to resolve function signatures for help
   if (is.null(sig) && getSignature)
   {
      object <- NULL
      if (length(package) &&
             package != "" &&
             package %in% loadedNamespaces())
      {
         object <- tryCatch(
            get(topic, envir = asNamespace(package)),
            error = function(e) NULL
         )
      }
      
      if (!length(object))
      {
         object <- tryCatch(
            get(topic, pos = globalenv()),
            error = function(e) NULL
         )
      }
      
      sig <- .rs.getSignature(object)
      sig <- gsub('^function ', topic, sig)
   }
   
   list('html' = html, 'signature' = sig, 'pkgname' = pkgname)
})

.rs.addJsonRpcHandler("show_help_topic", function(what, from, type)
{
   # strip off a 'package:' prefix if necessary
   if (is.character(from) && nzchar(from))
      from <- sub("^package:", "", from)
   
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
   exactMatch = help(query, help_type = "html")
   if (length(exactMatch) == 1)
   {
      print(exactMatch)
      return()
   }
   else
   {
      paste("help/doc/html/Search?pattern=",
            utils::URLencode(query, reserved = TRUE),
            "&title=1&keyword=1&alias=1",
            sep = "")
   }
})
