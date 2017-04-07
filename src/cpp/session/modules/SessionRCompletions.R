#
# SessionRCompletions.R
#
# Copyright (C) 2014 by RStudio, Inc.
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

# Put the autocompletion types in the .rs.Env environment so they're accessible
# everywhere (sync with RCompletionManager.java)
assign(x = ".rs.acContextTypes",
       pos = which(search() == "tools:rstudio"),
       value = list(
          UNKNOWN            =  0,
          FUNCTION           =  1,
          SINGLE_BRACKET     =  2,
          DOUBLE_BRACKET     =  3,
          NAMESPACE_EXPORTED =  4,
          NAMESPACE_ALL      =  5,
          DOLLAR             =  6,
          AT                 =  7,
          FILE               =  8,
          CHUNK              =  9,
          ROXYGEN            = 10,
          HELP               = 11,
          ARGUMENT           = 12,
          PACKAGE            = 13
       )
)

# Sync with RCompletionTypes.java
assign(x = ".rs.acCompletionTypes",
       pos = which(search() == "tools:rstudio"),
       value = list(
          UNKNOWN     =  0,
          VECTOR      =  1,
          ARRAY       =  2,
          DATAFRAME   =  3,
          LIST        =  4,
          ENVIRONMENT =  5,
          FUNCTION    =  6,
          ARGUMENT    =  7,
          S4_CLASS    =  8,
          S4_OBJECT   =  9,
          S4_GENERIC  = 10,
          S4_METHOD   = 11,
          R5_CLASS    = 12,
          R5_OBJECT   = 13,
          R5_METHOD   = 14,
          FILE        = 15,
          DIRECTORY   = 16,
          CHUNK       = 17,
          ROXYGEN     = 18,
          HELP        = 19,
          STRING      = 20,
          PACKAGE     = 21,
          KEYWORD     = 22,
          OPTION      = 23,
          DATASET     = 24,
          CONTEXT     = 99
       )
)

.rs.addFunction("getCompletionType", function(object)
{
   # Reference classes
   if (inherits(object, "refMethodDef"))
      .rs.acCompletionTypes$R5_METHOD
   else if (inherits(object, "refObjectGenerator"))
      .rs.acCompletionTypes$R5_CLASS
   else if (inherits(object, "refClass"))
      .rs.acCompletionTypes$R5_OBJECT
   
   # S4
   else if (isS4(object))
   {
      if (inherits(object, "standardGeneric") ||
          inherits(object, "nonstandardGenericFunction"))
         .rs.acCompletionTypes$S4_GENERIC
      else if (inherits(object, "MethodDefinition"))
         .rs.acCompletionTypes$S4_METHOD
      else
         .rs.acCompletionTypes$S4_OBJECT
   }
   
   # Base
   else if (is.function(object))
      .rs.acCompletionTypes$FUNCTION
   else if (is.array(object))
      .rs.acCompletionTypes$ARRAY
   else if (inherits(object, "data.frame"))
      .rs.acCompletionTypes$DATAFRAME
   else if (is.list(object))
      .rs.acCompletionTypes$LIST
   else if (is.environment(object))
      .rs.acCompletionTypes$ENVIRONMENT
   else if (is.vector(object))
      .rs.acCompletionTypes$VECTOR
   else if (is.factor(object))
      .rs.acCompletionTypes$VECTOR
   else
      .rs.acCompletionTypes$UNKNOWN
})

.rs.addFunction("attemptRoxygenTagCompletion", function(token)
{
   # Allow for roxygen completions when no token is available
   match <- token == "" || grepl("^@[a-zA-Z0-9]*$", token, perl = TRUE)
   if (!match)
      return(.rs.emptyCompletions(excludeOtherCompletions = TRUE))
   
   tag <- sub(".*(?=@)", '', token, perl = TRUE)
   
   # All known Roxygen2 tags, in alphabetical order
   tags <- c(
      "@aliases ",
      "@author ",
      "@concepts ",
      "@describeIn ",
      "@description ",
      "@details ",
      "@docType ",
      "@evalRd ",
      "@example ",
      "@examples ",
      "@export",
      "@exportClass ",
      "@exportMethod ",
      "@family ",
      "@field ",
      "@format ",
      "@import ",
      "@importClassesFrom ",
      "@importFrom ",
      "@importMethodsFrom ",
      "@include ",
      "@inherit ",
      "@inheritDotParams ",
      "@inheritParams ",
      "@inheritSection ",
      "@keywords ",
      "@md",
      "@method ",
      "@name ",
      "@note ",
      "@noMd",
      "@noRd",
      "@param ",
      "@rawRd ",
      "@rawNamespace ",
      "@rdname ",
      "@references ",
      "@return ",
      "@section ",
      "@seealso ",
      "@slot ",
      "@source ",
      "@template ",
      "@templateVar ",
      "@title ",
      "@usage ",
      "@useDynLib "
   )
   
   matchingTags <- grep(paste("^", tag, sep = ""), tags, value = TRUE)
   
   .rs.makeCompletions(tag,
                       matchingTags,
                       type = .rs.acCompletionTypes$ROXYGEN,
                       excludeOtherCompletions = TRUE)
})

.rs.addFunction("getCompletionsVignettes", function(token)
{
   doGetCompletionsVignettes <- function(token, vignettes)
   {
      vignettes <- sort(vignettes)
      .rs.makeCompletions(token = token,
                          results = .rs.selectFuzzyMatches(vignettes, token),
                          quote = TRUE,
                          type = .rs.acCompletionTypes$VIGNETTE,
                          excludeOtherCompletions = TRUE)
   }
   
   if (!is.null(vignettes <- .rs.get("vignettes")))
      return(doGetCompletionsVignettes(token, vignettes$results[, "Item"]))
   
   vignettes <- vignette()
   .rs.assign("vignettes", vignettes)
   doGetCompletionsVignettes(token, vignettes$results[, "Item"])
})

.rs.addFunction("getCompletionsFile", function(token,
                                               path = getwd(),
                                               quote = FALSE,
                                               directoriesOnly = FALSE)
{
   path <- suppressWarnings(.rs.normalizePath(path, winslash = "/"))
   
   ## Separate the token into a 'directory' prefix, and a 'name' prefix. We need
   ## to prefix the prefix as we will need to prepend it onto completions for
   ## non-relative completions.
   tokenPrefix <- ""
   tokenName <- token
   
   tokenSlashIndices <- c(gregexpr("[/\\\\]", token, perl = TRUE)[[1]])
   if (!identical(tokenSlashIndices, -1L))
   {
      maxIndex <- max(tokenSlashIndices)
      tokenPrefix <- substring(token, 1, maxIndex)
      tokenName <- substring(token, maxIndex + 1)
   }
   
   ## Figure out the directory to list files in.
   
   # The token itself might dictate the directory we want to search in, e.g.
   # if we see `~`, `/`, `[a-zA-Z]:`. In those cases, override the path argument.
   regex <- "^[~/\\\\]|^[a-zA-Z]:[/\\\\]"
   if (nzchar(token) && length(grep(regex, token, perl = TRUE)))
   {
      directory <- path <-
         suppressWarnings(normalizePath(dirname(paste(token, ".", sep = ""))))
   }
   
   # Check to see if there are delimiters in the token. If there are, but it's
   # a relative path, we need to further qualify the directory.
   else
   {
      if (!identical(tokenSlashIndices, -1L))
      {
         maxIndex <- max(tokenSlashIndices)
         fullPath <- file.path(path, substring(token, 1, maxIndex - 1))
         directory <- suppressWarnings(.rs.normalizePath(fullPath, winslash = "/"))
      }
      else
      {
         directory <- path
      }
   }
   
   # If we're trying to get completions from a directory that doesn't
   # exist, give up
   if (!file.exists(directory))
      return(.rs.emptyCompletions(excludeOtherCompletions = TRUE))
   
   # When checking if the path lies within the project directory,
   # we just do a substring check -- so make sure that the path
   # is actually within the project directory (append a trailing
   # slash to the directory we check)
   projDirEndsWithSlash <-
      paste(gsub("/*$", "", .rs.getProjectDirectory()), "/", sep = "")
   
   directory <-
      paste(gsub("/*$", "", directory), "/", sep = "")
   
   # If the directory lies within a folder that we're monitoring
   # for indexing, use that.
   cacheable <- TRUE
   usingFileMonitor <-
      .rs.hasFileMonitor() &&
      .rs.startsWith(directory, projDirEndsWithSlash)
   
   absolutePaths <- character()
   if (usingFileMonitor)
   {
      if (directoriesOnly)
         index <- .rs.getIndexedFolders(tokenName, directory)
      else
         index <- .rs.getIndexedFilesAndFolders(tokenName, directory)
      
      cacheable <- !index$more_available
      absolutePaths <- index$paths
   }
   
   # Merge in completions from the current directory.
   dirPaths <- .rs.listFilesFuzzy(directory, tokenName)
   if (directoriesOnly)
   {
      dirInfo <- file.info(dirPaths)
      dirPaths <- dirPaths[dirInfo$isdir]
   }
   absolutePaths <- sort(union(absolutePaths, dirPaths))
   
   ## Bail out early if we didn't get any completions.
   if (!length(absolutePaths))
      return(.rs.emptyCompletions(excludeOtherCompletions = TRUE))
   
   ## Because the completions returned will replace the whole token,
   ## we need to be careful in how we construct the return result. In particular,
   ## we need to preserve the way the directory has been specified.
   ##
   ## Note that the directory may or may not end with a trailing slash,
   ## depending on how it was normalized. We have to check for that when
   ## determining the offset for constructing the relative path. Normally,
   ## the trailing slash is included for root directories, e.g. `/` on Unix-alikes
   ## or `C:/` on Windows.
   offset <- if (grepl("[/\\\\]$", directory, perl = TRUE))
      nchar(directory) + 1L
   else
      nchar(directory) + 2L
   
   relativePaths <- substring(absolutePaths, offset)
   
   ## Order completions starting with alpha-numeric entries first if the
   ## token name is blank
   if (!nzchar(tokenName))
   {
      startsWithAlnum <- grepl("^[[:alnum:]]", relativePaths, perl = TRUE)
      order <- c(
         which(startsWithAlnum),
         which(!startsWithAlnum)
      )
      
      relativePaths <- relativePaths[order]
      absolutePaths <- absolutePaths[order]
   }
   
   paths <- paste(tokenPrefix, relativePaths, sep = "")
   
   # If we were able to use the file index and only wanted directories, we know that
   # all completions must be directories
   if (usingFileMonitor && directoriesOnly)
   {
      type <- rep.int(.rs.acCompletionTypes$DIRECTORY, length(absolutePaths))
   }
   
   # Otherwise, query the file info for the set of completions we're using
   else
   {
      isDir <- file.info(absolutePaths)[, "isdir"] %in% TRUE ## protect against NA
      type <- ifelse(isDir,
                     .rs.acCompletionTypes$DIRECTORY,
                     .rs.acCompletionTypes$FILE)
      
      if (directoriesOnly)
      {
         paths <- paths[isDir]
         type <- type[isDir]
      }
   }
   
   # Order completions by depth
   matches <- gregexpr("/", paths, fixed = TRUE)
   depth <- vapply(matches, function(match) {
      if (identical(c(match), -1L))
         0
      else
         length(match)
   }, numeric(1))
   
   idx <- order(depth)
   paths <- paths[idx]
   type <- type[idx]
   
   .rs.makeCompletions(token = token,
                       results = paths,
                       type = type,
                       quote = quote,
                       excludeOtherCompletions = TRUE,
                       cacheable = cacheable)
})

.rs.addFunction("resolveFormals", function(token,
                                           object,
                                           functionName,
                                           functionCall,
                                           matchedCall,
                                           envir)
{
   tryCatch({
      
      if (length(functionCall) > 1 && .rs.isS3Generic(object))
      {
         objectForDispatch <- .rs.getAnywhere(matchedCall[[2]], envir)
         classes <- class(objectForDispatch)
         for (class in c(classes, "default"))
         {
            # It is possible that which S3 method will be used may depend on where
            # the generic f is called from: getS3method returns the method found if
            # f were called from the same environment.
            call <- substitute(
               utils::getS3method(functionName, class),
               list(functionName = functionName,
                    class = class)
            )
            
            method <- tryCatch(
               eval(call, envir = envir),
               error = function(e) NULL
            )
            
            if (!is.null(method))
            {
               formals <- .rs.getFunctionArgumentNames(method)
               methods <- rep.int(
                  paste(functionName, class, sep = "."),
                  length(formals)
               )
               break
            }
         }
      }
      else
      {
         formals <- .rs.getFunctionArgumentNames(object)
         methods <- rep.int(functionName, length(formals))
      }
      
      keep <- .rs.fuzzyMatches(formals, token) &
         !(formals %in% names(functionCall)) ## leave out formals already in call
      
      return(list(
         formals = formals[keep],
         methods = methods[keep]
      ))
      
   }, error = function(e) NULL
   )
})

.rs.addFunction("matchCall", function(func,
                                      call)
{
   ## NOTE: the ugliness here is necessary to handle missingness in calls
   ## e.g. `x <- call[[i]]` fails to assign to `x` if `call[[i]]` is missing
   i <- 1
   while (TRUE)
   {
      if (i > length(call))
         break
      
      if (length(call[[i]]) == 1 && is.symbol(call[[i]]) && as.character(call[[i]]) == "...")
         call <- call[-i]
      else
         i <- i + 1
   }
   
   tryCatch(match.call(func, call), error = function(e) call)
})

.rs.addFunction("getCompletionsInstallPackages", function(token)
{
   contrib.url <- contrib.url(getOption("repos"), getOption("pkgType"))
   
   packages <- Reduce(union, lapply(contrib.url, function(url) {
      
      # Try to get a package list from the cache
      result <- .rs.getCachedAvailablePackages(url)
      
      # If it's null, there were no packages available
      if (is.null(result))
         .rs.downloadAvailablePackages(url)
      
      # Try again
      result <- .rs.getCachedAvailablePackages(url)
      
      # And return
      result
   }))
   
   .rs.makeCompletions(token = token,
                       results = .rs.selectFuzzyMatches(packages, token),
                       quote = TRUE,
                       type = .rs.acCompletionTypes$PACKAGE,
                       excludeOtherCompletions = TRUE)
   
})

.rs.addFunction("resolveObjectFromFunctionCall", function(functionCall,
                                                          envir)
{
   string <- capture.output(base::print(functionCall[[1]]))[[1]]
   splat <- strsplit(string, ":{2,3}", perl = TRUE)[[1]]
   object <- NULL
   if (length(splat) == 1)
   {
      object <- .rs.getAnywhere(.rs.stripSurrounding(string), envir = envir)
   }
   else if (length(splat) == 2)
   {
      namespaceString <- .rs.stripSurrounding(splat[[1]])
      functionString <- string <- .rs.stripSurrounding(splat[[2]])
      
      if (namespaceString %in% loadedNamespaces())
      {
         object <- tryCatch(
            eval(parse(text = functionString),
                 envir = asNamespace(namespaceString)),
            error = function(e) NULL
         )
      }
   }
   
   # Special casing for variants of read.table which hide additional
   # arguments in ...
   if ("utils" %in% loadedNamespaces())
   {
      readers <- list(
         utils::read.csv,
         utils::read.csv2,
         utils::read.delim,
         utils::read.delim2
      )
      
      if (any(sapply(readers, identical, object)))
         object <- utils::read.table
      
      # Similarily for write.csv
      writers <- list(
         utils::write.csv,
         utils::write.csv2
      )
      
      if (any(sapply(writers, identical, object)))
         object <- utils::write.table
   }
   
   object
   
})

.rs.addFunction("getCompletionsFunction", function(token,
                                                   string,
                                                   functionCall,
                                                   numCommas,
                                                   discardFirst,
                                                   envir = parent.frame())
{
   result <- .rs.emptyCompletions()
   object <- .rs.resolveObjectFromFunctionCall(functionCall, envir)
   
   if (!is.null(object) && is.function(object))
   {
      matchedCall <- .rs.matchCall(object, functionCall)
      
      # Try to figure out what function arguments are
      # eligible for completion. Note that, on success,
      # this should be an R list with character fields
      # 'formals' and (optionally) 'methods'.
      formals <- NULL
      
      ## Special cases
      # We handle special cases for function argument
      # completions first.
      # If we're completing a knitr getter function, then try
      # to produce auto-completions for potential argument
      # names
      if (.rs.isKnitrObject(object))
      {
         ns <- asNamespace("knitr")
         
         # Get the knitr getters and setters for various
         # options
         tryGetKnitrGetter <- function(name, ns = asNamespace("knitr"))
            tryCatch(get(name, envir = ns)$get, error = function(e) NULL)
         
         tryGetKnitrSetter <- function(name, ns = asNamespace("knitr"))
            tryCatch(get(name, envir = ns)$set, error = function(e) NULL)
         
         tryGet <- function(name, ns = asNamespace("knitr"))
            list(getter = tryGetKnitrGetter(name, ns),
                 setter = tryGetKnitrSetter(name, ns))
         
         knitrOpts <- list(
            tryGet("opts_chunk", ns),
            tryGet("opts_knit", ns),
            tryGet("opts_current", ns),
            tryGet("opts_template", ns),
            tryGet("knit_hooks", ns),
            tryGet("knit_theme", ns)
         )
         
         for (opt in knitrOpts)
         {
            # If we're identical to the getter, short-circuit
            # and just return the names of parameters
            if (identical(object, opt$getter))
            {
               results <- .rs.selectFuzzyMatches(
                  names(opt$getter()),
                  token
               )
               
               return(.rs.makeCompletions(token = token,
                                          results = results,
                                          type = .rs.acCompletionTypes$STRING,
                                          quote = TRUE))
            }
            
            # If we're identical to the setter, get the
            # names from the getter as named arguments to use
            if (identical(object, opt$setter))
            {
               formals <- list(formals = names(opt$getter()))
               break
            }
         }
         
      }
      
      # Resolve formals from a classGeneratorFunction based
      # on its slots
      if (is.null(formals) &&
          inherits(object, "classGeneratorFunction"))
      {
         try(silent = TRUE, {
            class <- object@className
            defn <- getClass(class)
            slots <- defn@slots
            formals <- list(
               formals = names(slots),
               methods = rep(class, length(slots))
            )
         })
      }
      
      # Resolve formals from the function itself
      if (is.null(formals))
      {
         formals <- .rs.resolveFormals(token,
                                       object,
                                       string,
                                       functionCall,
                                       matchedCall,
                                       envir)
      }
      
      if (length(formals$formals))
         formals$formals <- paste(formals$formals, "= ")
      
      # If we're getting completions for the `base::c` function, just discard
      # the argument completions, since other context completions are more
      # likely and more useful
      if (identical(object, base::c) ||
          identical(object, base::list))
      {
         formals <- list(formals = character(),
                         methods = character())
      }
      
      # Get the current argument -- we can resolve this based on
      # 'numCommas' and the number of named formals. The idea is, e.g.
      # in a function call
      #
      #     rnorm(|, n = 1, sd = 2)
      #
      # we should infer that the current argument name is 'mean'. We do this
      # by looking at which arguments have yet to be matched, and using the
      # first argument in that list.
      activeArg <- .rs.getActiveArgument(object, matchedCall)
      
      if (!length(activeArg) || is.na(activeArg))
         activeArg <- ""
      
      # Special casing for 'group_by' from dplyr
      # TODO: Should we just allow for any function named 'group_by', ie,
      # enable this even if 'dplyr' isn't loaded?
      if (!is.null(activeArg) && activeArg == "..." &&
          "dplyr" %in% loadedNamespaces() &&
          identical(object, get("group_by", envir = asNamespace("dplyr"))))
      {
         .data <- .rs.getAnywhere(matchedCall[[".data"]], envir = envir)
         if (!is.null(.data))
         {
            .names <- .rs.getNames(.data)
            if (length(matchedCall) >= 3)
               .names <- setdiff(.names, as.character(matchedCall)[3:length(matchedCall)])
            
            return(.rs.makeCompletions(token = token,
                                       results = .names,
                                       quote = FALSE,
                                       type = .rs.acCompletionTypes$CONTEXT))
         }
      }
      
      # Get completions for the current active argument
      argCompletions <- .rs.getCompletionsArgument(
         token = token,
         activeArg = activeArg,
         functionCall = functionCall,
         envir = envir
      )
      
      # If the active argument was 'formula' and we were able
      # to retrieve completions, it is unlikely that we also
      # want search path completions or otherwise -- just return
      # those completions.
      if (identical(activeArg, "formula") &&
          !is.null(argCompletions) &&
          !.rs.isEmptyCompletion(argCompletions))
      {
         return(argCompletions)
      }
      
      fguess <- if (length(formals$methods))
         formals$methods[[1]]
      else
         ""
      
      result <- .rs.appendCompletions(
         argCompletions,
         .rs.makeCompletions(
            token = token,
            results = formals$formals,
            packages = formals$methods,
            type = .rs.acCompletionTypes$ARGUMENT,
            fguess = fguess,
            orderStartsWithAlnumFirst = FALSE
         )
      )
   }
   
   if (discardFirst)
   {
      result$results <- tail(result$results, length(result$results) - 1)
      result$packages <- tail(result$packages, length(result$packages) - 1)
      result$quote <- tail(result$quote, length(result$quote) - 1)
      result$type <- tail(result$type, length(result$type) - 1)
   }
   
   result
   
})

.rs.addFunction("getSourceIndexCompletions", function(token)
{
   .Call("rs_getSourceIndexCompletions", token)
})

.rs.addFunction("getCompletionsNamespace", function(token, string, exportsOnly, envir)
{
   result <- .rs.emptyCompletions(excludeOtherCompletions = TRUE)
   
   if (!(string %in% loadedNamespaces()))
   {
      tryCatch(
         suppressPackageStartupMessages(requireNamespace(string, quietly = TRUE)),
         error = function(e) NULL
      )
   }
   
   if (string %in% loadedNamespaces())
   {
      namespace <- asNamespace(string)
      objectNames <- if (exportsOnly)
         getNamespaceExports(namespace)
      else
      {
         # Take advantage of 'sorted' argument if available,
         # as we only want to sort a filtered subset and will
         # sort again later
         arguments <- list()
         arguments[["name"]] <- namespace
         arguments[["all.names"]] <- TRUE
         if ("sorted" %in% names(formals(objects)))
            arguments[["sorted"]] <- FALSE
         
         do.call(objects, arguments)
      }
      
      # For `::`, we also want to grab items in the 'lazydata' environment
      # within the namespace.
      lazydata <- new.env(parent = emptyenv())
      dataNames <- character()
      if (exportsOnly && exists(".__NAMESPACE__.", envir = namespace))
      {
         .__NAMESPACE__. <- get(".__NAMESPACE__.", envir = namespace)
         if (exists("lazydata", envir = .__NAMESPACE__.))
         {
            lazydata  <- get("lazydata", envir = .__NAMESPACE__.)
            dataNames <- objects(lazydata, all.names = TRUE)
         }
      }
      
      # Filter our results
      objectNames <- .rs.selectFuzzyMatches(objectNames, token)
      dataNames   <- .rs.selectFuzzyMatches(dataNames, token)
      
      # Collect the object types
      objectTypes <- vapply(
         mget(objectNames, envir = namespace, inherits = TRUE),
         FUN.VALUE = numeric(1),
         USE.NAMES = FALSE,
         .rs.getCompletionType
      )
      
      # Let 'lazydata' be lazy -- don't force evaluation.
      dataTypes <- rep(.rs.acCompletionTypes$DATAFRAME, length(dataNames))
      
      # Construct a data.frame to hold our results (because we'll
      # need to re-sort our items)
      df <- data.frame(
         names = c(objectNames, dataNames),
         types = c(objectTypes, dataTypes),
         stringsAsFactors = FALSE
      )
      
      # Sort by 'names'
      df <- df[order(df$names), ]
      
      # Construct the completion result
      completions <- df$names
      type <- df$types
      
      result <- .rs.makeCompletions(
         token = token,
         results = completions,
         packages = string,
         quote = FALSE,
         type = type,
         excludeOtherCompletions = TRUE
      )
   }
   
   result
   
})


.rs.addFunction("emptyCompletions", function(excludeOtherCompletions = FALSE,
                                             overrideInsertParens = FALSE,
                                             orderStartsWithAlnumFirst = TRUE)
{
   .rs.makeCompletions(
      token = "",
      results = character(),
      packages = character(),
      quote = logical(),
      type = numeric(),
      fguess = "",
      excludeOtherCompletions = .rs.scalar(excludeOtherCompletions),
      overrideInsertParens = .rs.scalar(overrideInsertParens),
      orderStartsWithAlnumFirst = .rs.scalar(orderStartsWithAlnumFirst)
   )
})

.rs.addFunction("isEmptyCompletion", function(completions)
{
   length(completions$results) == 0
})

.rs.addFunction("fuzzyMatches", function(completions, token)
{
   reStrip     <- "(?!^)[._]"
   token       <- gsub(reStrip, "", token, perl = TRUE)
   completions <- gsub(reStrip, "", completions, perl = TRUE)
   .rs.startsWith(tolower(completions), tolower(token))
})

.rs.addFunction("selectFuzzyMatches", function(completions, token)
{
   types <- attr(completions, "types")
   matches <- .rs.fuzzyMatches(completions, token)
   completions <- completions[matches]
   if (!is.null(types))
      attr(completions, "types") <- types[matches]
   completions
})

.rs.addFunction("formCompletionVector", function(object, default, n)
{
   if (!length(object))
      rep.int(default, n)
   else
      rep.int(object, n)
})

.rs.addFunction("makeCompletions", function(token,
                                            results,
                                            packages = character(),
                                            quote = logical(),
                                            type = numeric(),
                                            fguess = "",
                                            excludeOtherCompletions = FALSE,
                                            overrideInsertParens = FALSE,
                                            orderStartsWithAlnumFirst = TRUE,
                                            cacheable = TRUE,
                                            helpHandler = NULL)
{
   if (is.null(results))
      results <- character()
   
   # Ensure other 'vector' completions are of the same length as 'results'
   n        <- length(results)
   packages <- .rs.formCompletionVector(packages, "", n)
   quote    <- .rs.formCompletionVector(quote, FALSE, n)
   type     <- .rs.formCompletionVector(type, .rs.acCompletionTypes$UNKNOWN, n)
   
   # Favor completions starting with a letter
   if (orderStartsWithAlnumFirst)
   {
      startsWithLetter <- grepl("^[a-zA-Z0-9]", results, perl = TRUE)
      
      first <- which(startsWithLetter)
      last  <- which(!startsWithLetter)
      order <- c(first, last)
      
      results  <- results[order]
      packages <- packages[order]
      quote    <- quote[order]
      type     <- type[order]
   }
   
   # Avoid generating too many completions
   limit <- 2000
   if (length(results) > limit)
   {
      cacheable <- FALSE
      idx <- seq_len(limit)
      
      results  <- results[idx]
      packages <- packages[idx]
      quote    <- quote[idx]
      type     <- type[idx]
   }
   
   list(token = token,
        results = results,
        packages = packages,
        quote = quote,
        type = type,
        fguess = fguess,
        excludeOtherCompletions = .rs.scalar(excludeOtherCompletions),
        overrideInsertParens = .rs.scalar(overrideInsertParens),
        cacheable = .rs.scalar(cacheable),
        helpHandler = .rs.scalar(helpHandler))
})

.rs.addFunction("subsetCompletions", function(completions, indices)
{
   for (name in c("results", "packages", "quote", "type"))
      completions[[name]] <- completions[[name]][indices]
   
   completions
})

.rs.addFunction("appendCompletions", function(old, new)
{
   for (name in c("results", "packages", "quote", "type"))
      old[[name]] <- c(old[[name]], new[[name]])
   
   # resolve duplicates -- a completion is duplicated if its result
   # and package are identical (if 'type' or 'quote' differs, it's probably a bug?)
   drop <- intersect(
      which(duplicated(old$results)),
      which(duplicated(old$packages))
   )
   
   if (length(drop))
   {
      for (name in c("results", "packages", "quote", "type"))
         old[[name]] <- old[[name]][-c(drop)]
   }
   
   if (length(new$fguess) && new$fguess != "")
      old$fguess <- new$fguess
   
   if (length(new$excludeOtherCompletions) && new$excludeOtherCompletions)
      old$excludeOtherCompletions <- new$excludeOtherCompletions
   
   if (length(new$overrideInsertParens) && new$overrideInsertParens)
      old$overrideInsertParens <- new$overrideInsertParens
   
   # If one completion states we are not cacheable, that setting is 'sticky'
   if (length(new$cacheable) && !new$cacheable)
      old$cacheable <- new$cacheable
   
   if (length(new$helpHandler))
      old$helpHandler <- new$helpHandler
   
   old
})

.rs.addFunction("sortCompletions", function(completions,
                                            token,
                                            shortestFirst = FALSE)
{
   scores <- .rs.scoreMatches(completions$results, token)
   
   # Put package completions at the end
   idx <- completions$type == .rs.acCompletionTypes$PACKAGE
   scores[idx] <- scores[idx] + 10
   
   # Protect against NULL / otherwise invalid scores.
   # TODO: figure out what, upstream, might cause this
   if (length(scores))
   {
      order <- if (shortestFirst)
         order(scores, nchar(completions$results))
      else
         order(scores)
      completions <- .rs.subsetCompletions(completions, order)
   }
   
   completions
})

.rs.addFunction("blackListEvaluationDataTable", function(token, string, functionCall, envir)
{
   tryCatch({
      # NOTE: We don't retrieve the name from the function call as this could
      # be a complex parse tree (e.g. dt[, y := 1][, y]$x); it is substantially
      # easier to strip the object name from the 'string' constituting the parsed object
      bracketIdx <- c(regexpr("[", string, fixed = TRUE))
      objectName <- if (bracketIdx == -1)
         string
      else
         substring(string, 1L, bracketIdx - 1L)
      
      object <- .rs.getAnywhere(objectName, envir = envir)
      if (inherits(object, "data.table"))
      {
         names <- names(object)
         results <- .rs.selectFuzzyMatches(names, token)
         .rs.makeCompletions(
            token = token,
            results = results,
            packages = string,
            quote = FALSE,
            type = vapply(object, FUN.VALUE = numeric(1), USE.NAMES = FALSE, .rs.getCompletionType)
         )
      }
   }, error = function(e) NULL
   )
   
})

.rs.addFunction("blackListEvaluation", function(token, string, functionCall, envir)
{
   if (!is.null(result <- .rs.blackListEvaluationDataTable(token, string, functionCall, envir)))
      return(result)
   
   NULL
   
})

## NOTE: for '@' as well (set in the 'isAt' parameter)
.rs.addFunction("getCompletionsDollar", function(token, string, functionCall, envir, isAt)
{
   result <- .rs.emptyCompletions(excludeOtherCompletions = TRUE)
   
   ## Blacklist certain evaluations
   if (!is.null(blacklist <- .rs.blackListEvaluation(token, string, functionCall, envir)))
   {
      blacklist$excludeOtherCompletions <- .rs.scalar(TRUE)
      return(blacklist)
   }
   
   object <- .rs.getAnywhere(string, envir)
   
   if (!is.null(object))
   {
      allNames <- character()
      names <- character()
      type <- numeric()
      helpHandler <- NULL
      
      if (isAt)
      {
         if (isS4(object))
         {
            tryCatch({
               allNames <- .slotNames(object)
               names <- .rs.selectFuzzyMatches(allNames, token)
               
               # NOTE: Getting the types forces evaluation; we avoid that if
               # there are too many names to evaluate.
               if (length(names) > 2E2)
                  type <- .rs.acCompletionTypes$UNKNOWN
               else
               {
                  type <- numeric(length(names))
                  for (i in seq_along(names))
                  {
                     type[[i]] <- suppressWarnings(tryCatch(
                        .rs.getCompletionType(eval(call("@", quote(object), names[[i]]))),
                        error = function(e) .rs.acCompletionTypes$UNKNOWN
                     ))
                  }
               }
            }, error = function(e) NULL
            )
         }
      }
      else
      {
         # Check to see if an overloaded .DollarNames method has been provided,
         # and use that to resolve names if possible.
         dollarNamesMethod <- .rs.getDollarNamesMethod(object, TRUE, envir = envir)
         if (is.function(dollarNamesMethod))
         {
            allNames <- dollarNamesMethod(object)
            
            # rJava will include closing parentheses as part of the
            # completion list -- clean those up and use them to infer
            # symbol types
            if ("rJava" %in% loadedNamespaces() &&
                identical(environment(dollarNamesMethod), asNamespace("rJava")))
            {
               types <- ifelse(
                  grepl("[()]$", allNames),
                  .rs.acCompletionTypes$FUNCTION,
                  .rs.acCompletionTypes$UNKNOWN
               )
               allNames <- gsub("[()]*$", "", allNames)
               attr(allNames, "types") <- types
            }
            
            # check for custom helpHandler
            helpHandler <- attr(allNames, "helpHandler", exact = TRUE)
         }
         
         # Reference class generators / objects
         else if (inherits(object, "refObjectGenerator"))
         {
            allNames <- Reduce(union, list(
               objects(object@generator@.xData, all.names = TRUE),
               objects(object$def@refMethods, all.names = TRUE),
               c("new", "help", "methods", "fields", "lock", "accessors")
            ))
         }
         
         # Reference class objects
         else if (inherits(object, "refClass"))
         {
            suppressWarnings(tryCatch({
               refClassDef <- object$.refClassDef
               allNames <- c(
                  ls(refClassDef@fieldPrototypes, all.names = TRUE),
                  ls(refClassDef@refMethods, all.names = TRUE)
               )
               
               # Place the 'less interesting' methods lower
               baseMethods <- c("callSuper", "copy", "export", "field",
                                "getClass", "getRefClass", "import", "initFields",
                                "show", "trace", "untrace", "usingMethods")
               
               allNames <- c(
                  setdiff(allNames, baseMethods),
                  baseMethods
               )
            }, error = function(e) NULL))
         }
         
         # Objects for which 'names' returns non-NULL. This branch is used
         # to provide completions for S4 objects, essentially adhering to
         # the `.DollarNames.default` behaviour. (It looks like if an S4
         # object is able to supply names through the `names` method, then
         # those names are also valid for `$` completions.)
         else if (isS4(object) && length(names(object)))
         {
            allNames <- .rs.getNames(object)
         }
         
         # Environments (note that some S4 objects 'are' environments;
         # e.g. 'hash' objects from the 'hash' package)
         else if (is.environment(object))
         {
            allNames <- .rs.getNames(object)
         }
         
         # Other objects
         else
         {
            # '$' operator is invalid for atomic vectors
            if (is.atomic(object))
               return(.rs.emptyCompletions(excludeOtherCompletions = TRUE))
            
            # Don't allow S4 objects for dollar name resolution
            # They will need to have defined a .DollarNames method, which
            # should have been resolved previously
            if (!isS4(object))
            {
               allNames <- .rs.getNames(object)
            }
         }
         
         names <- .rs.selectFuzzyMatches(allNames, token)

         # See if types were provided
         types <- attr(names, "types")
         if (is.integer(types) && length(types) == length(names))
            type <- types
         
         # NOTE: Getting the types forces evaluation; we avoid that if
         # there are too many names to evaluate.
         else if (length(names) > 2E2)
            type <- .rs.acCompletionTypes$UNKNOWN
         else
         {
            type <- numeric(length(names))
            for (i in seq_along(names))
            {
               type[[i]] <- suppressWarnings(tryCatch(
                  if (is.environment(object) && bindingIsActive(names[[i]], object))
                     .rs.acCompletionTypes$UNKNOWN
                  else
                     .rs.getCompletionType(eval(call("$", quote(object), names[[i]]))),
                  error = function(e) .rs.acCompletionTypes$UNKNOWN
               ))
            }
         }
      }
      
      result <- .rs.makeCompletions(
         token = token,
         results = names,
         packages = string,
         quote = FALSE,
         type = type,
         excludeOtherCompletions = TRUE,
         helpHandler = helpHandler
      )
   }
   
   result
   
})

.rs.addFunction("getCompletionsSingleBracket", function(token,
                                                        string,
                                                        functionCall,
                                                        numCommas,
                                                        envir)
{
   result <- .rs.emptyCompletions()
   
   ## Blacklist certain evaluations
   if (!is.null(result <- .rs.blackListEvaluation(token, string, functionCall, envir)))
      return(result)
   
   object <- .rs.getAnywhere(string, envir)
   if (is.null(object))
      return(result)
   
   completions <- character()
   
   # Get completions from dimension names for arrays
   if (is.array(object) && !is.null(dn <- dimnames(object)))
   {
      if (numCommas + 1 <= length(dn))
         completions <- dimnames(object)[[numCommas + 1]]
      
      string <- if (numCommas == 0)
         paste("rownames(", string, ")", sep = "")
      else if (numCommas == 1)
         paste("colnames(", string, ")", sep = "")
      else
         paste("dimnames(", string, ")[", numCommas + 1, "]", sep = "")
   }
   
   # Get completions from rownames for data.frames if we have no commas, e.g.
   # `mtcars[|`
   else if (inherits(object, "data.frame") &&
            numCommas == 0)
   {
      completions <- rownames(object)
   }
   
   # Just get the names of the object
   else
   {
      completions <- .rs.getNames(object)
   }
   
   completions <- .rs.selectFuzzyMatches(completions, token)
   
   if (length(completions))
   {
      result <- .rs.makeCompletions(
         token = token,
         results = completions,
         packages = string,
         quote = !inherits(object, "data.table"),
         type = .rs.acCompletionTypes$STRING
      )
   }
   
   result
   
})

.rs.addFunction("getCompletionsDoubleBracket", function(token,
                                                        string,
                                                        functionCall,
                                                        envir = parent.frame())
{
   result <- .rs.emptyCompletions()
   
   ## Blacklist certain evaluations
   if (!is.null(result <- .rs.blackListEvaluation(token, string, functionCall, envir)))
      return(result)
   
   object <- .rs.getAnywhere(string, envir)
   if (is.null(object))
      return(result)
   
   completions <- .rs.getNames(object)
   completions <- .rs.selectFuzzyMatches(completions, token)
   
   if (length(completions))
   {
      result <- .rs.makeCompletions(
         token = token,
         results = completions,
         packages = string,
         quote = TRUE,
         type = .rs.acCompletionTypes$STRING,
         overrideInsertParens = TRUE
      )
   }
   
   result
   
})

.rs.addFunction("listIndexedPackages", function()
{
   .Call("rs_listIndexedPackages")
})

.rs.addFunction("getCompletionsPackages", function(token,
                                                   appendColons = FALSE,
                                                   excludeOtherCompletions = FALSE,
                                                   quote = !appendColons)
{
   allPackages <- basename(.rs.listIndexedPackages())
   
   # In case indexing is disabled, include any loaded namespaces by default
   allPackages <- union(allPackages, loadedNamespaces())
   
   # Not sure why 'DESCRIPTION' might show up here, but let's take it out
   allPackages <- setdiff(allPackages, "DESCRIPTION")
   
   # Also remove any '00LOCK' directories -- these might be leftovers
   # from earlier failed package installations
   if (length(allPackages))
   {
      isLockDir <- substring(allPackages, 1, 6) == "00LOCK"
      allPackages <- allPackages[!isLockDir]
   }
   
   # Construct our completions, and we're done
   completions <- .rs.selectFuzzyMatches(allPackages, token)
   .rs.makeCompletions(token = token,
                       results = if (appendColons && length(completions))
                          paste(completions, "::", sep = "")
                       else
                          completions,
                       packages = completions,
                       quote = quote,
                       type = .rs.acCompletionTypes$PACKAGE,
                       excludeOtherCompletions = excludeOtherCompletions)
})

.rs.addFunction("getCompletionsData", function(token)
{
   # Suppress warnings e.g.
   #
   #    Warning messages:
   #       1: In data(package = .packages(all.available = TRUE)) :
   #       datasets have been moved from package 'base' to package 'datasets'
   #    2: In data(package = .packages(all.available = TRUE)) :
   #       datasets have been moved from package 'stats' to package 'datasets'
   #
   availableData <- suppressWarnings(data()$results)
   
   # Don't include aliases
   indices <- intersect(
      which(.rs.fuzzyMatches(availableData[, "Item"], token)),
      grep(" ", availableData[, "Item"], fixed = TRUE, invert = TRUE)
   )
   
   results <- availableData[indices, "Item"]
   packages <- availableData[indices, "Package"]
   
   .rs.makeCompletions(token,
                       results,
                       packages,
                       quote = TRUE,
                       type = .rs.acCompletionTypes$DATASET)
})

.rs.addFunction("getCompletionsGetOption", function(token)
{
   allOptions <- names(options())
   .rs.makeCompletions(token = token,
                       results = .rs.selectFuzzyMatches(allOptions, token),
                       packages = "options",
                       quote = TRUE,
                       type = .rs.acCompletionTypes$OPTION)
})

.rs.addFunction("getCompletionsOptions", function(token)
{
   allOptions <- names(options())
   completions <- .rs.selectFuzzyMatches(allOptions, token)
   if (length(completions))
      completions <- paste(completions, "= ")
   
   .rs.makeCompletions(token,
                       completions,
                       type = .rs.acCompletionTypes$OPTION)
})

.rs.addFunction("getCompletionsActiveFrame", function(token,
                                                      envir)
{
   currentEnv <- envir
   encounteredEnvs <- list()
   
   completions <- character()
   types <- numeric()
   
   empty <- emptyenv()
   while (!(identical(currentEnv, empty) ||
            identical(currentEnv, .GlobalEnv) ||
            identical(currentEnv, .BaseNamespaceEnv)))
   {
      for (i in seq_along(encounteredEnvs))
      {
         if (identical(currentEnv, encounteredEnvs[[i]]))
            return(.rs.emptyCompletions())
      }
      
      objects <- objects(currentEnv, all.names = TRUE)
      
      completions <- c(completions, objects)
      types <- c(
         types,
         vapply(objects, USE.NAMES = FALSE, FUN.VALUE = numeric(1), function(object) {
            tryCatch(
               .rs.getCompletionType(get(object, envir = currentEnv, inherits = TRUE)),
               error = function(e) .rs.acCompletionTypes$UNKNOWN
            )
         })
      )
      
      encounteredEnvs <- c(encounteredEnvs, currentEnv)
      currentEnv <- parent.env(currentEnv)
   }
   
   keep <- .rs.fuzzyMatches(completions, token)
   .rs.makeCompletions(token = token,
                       results = completions[keep],
                       type = types[keep])
})

.rs.addFunction("getNAMESPACEImportedSymbols", function(documentId)
{
   .Call("rs_getNAMESPACEImportedSymbols", documentId)
})

.rs.addFunction("getCompletionsNAMESPACE", function(token, documentId)
{
   symbols <- .rs.getNAMESPACEImportedSymbols(documentId)
   
   if (!length(symbols))
      return(.rs.emptyCompletions())
   
   n <- vapply(symbols, function(x) length(x[[1]]), USE.NAMES = FALSE, FUN.VALUE = numeric(1))
   
   output <- list(
      exports = unlist(lapply(symbols, `[[`, "exports"), use.names = FALSE),
      types = unlist(lapply(symbols, `[[`, "types"), use.names = FALSE),
      packages = rep.int(names(symbols), times = n)
   )
   
   keep <- .rs.fuzzyMatches(output$exports, token)
   
   .rs.makeCompletions(token = token,
                       results = output$exports[keep],
                       packages = output$packages[keep],
                       type = output$types[keep],
                       quote = FALSE)
})

.rs.addFunction("getCompletionsSearchPath", function(token,
                                                     overrideInsertParens = FALSE)
{
   objects <- if (.rs.startsWith(token, "."))
      .rs.objectsOnSearchPath(".")
   else
      .rs.objectsOnSearchPath()
   
   objects[["keywords"]] <- c(
      "NULL", "NA", "TRUE", "FALSE", "T", "F", "Inf", "NaN",
      "NA_integer_", "NA_real_", "NA_character_", "NA_complex_"
   )
   
   names <- names(objects)
   results <- unlist(objects, use.names = FALSE)
   packages <- unlist(lapply(1:length(objects), function(i) {
      rep.int(names[i], length(objects[[i]]))
   }))
   
   # Keywords are really from the base package
   packages[packages == "keywords"] <- "base"
   
   # discover completion matches for this token
   keep <- .rs.fuzzyMatches(results, token)
   results <- results[keep]
   packages <- packages[keep]
   
   # remove duplicates (assume first element masks next)
   dupes    <- duplicated(results)
   results  <- results[!dupes]
   packages <- packages[!dupes]
   
   # re-order the completion results (lexically)
   order <- order(results)
   
   # If the token is 'T' or 'F', prefer 'TRUE' and 'FALSE' completions
   if (token == "T")
   {
      TRUEpos <- which(results == "TRUE")
      order <- c(TRUEpos, order[-c(TRUEpos)])
   }
   
   if (token == "F")
   {
      FALSEpos <- which(results == "FALSE")
      order <- c(FALSEpos, order[-c(FALSEpos)])
   }
   
   results <- results[order]
   packages <- packages[order]
   
   type <- vapply(seq_along(results), function(i) {
      if (packages[[i]] == "keywords")
         .rs.acCompletionTypes$KEYWORD
      else if (packages[[i]] == "")
         ## Don't try to evaluate these as we don't want to force promises in debug contexts
         .rs.acCompletionTypes$UNKNOWN
      else
         tryCatch(
            .rs.getCompletionType(get(results[[i]], pos = which(search() == packages[[i]]))),
            error = function(e) .rs.acCompletionTypes$UNKNOWN
         )
   }, FUN.VALUE = numeric(1), USE.NAMES = FALSE)
   
   .rs.makeCompletions(token = token,
                       results = results,
                       packages = packages,
                       quote = FALSE,
                       type = type,
                       overrideInsertParens = overrideInsertParens)
   
})

.rs.addFunction("finishExpression", function(string)
{
   .Call("rs_finishExpression", as.character(string))
})

.rs.addFunction("getCompletionsAttr", function(token,
                                               functionCall,
                                               envir)
{
   result <- tryCatch({
      wrapper <- function(x, which, exact = FALSE) {}
      matched <- .rs.matchCall(wrapper, functionCall)
      if (is.null(matched[["x"]]))
         return(.rs.emptyCompletions())
      objectExpr <- matched[["x"]]
      objectName <- capture.output(print(objectExpr))
      object <- eval(objectExpr, envir = envir)
      
      completions <- .rs.selectFuzzyMatches(
         names(attributes(object)),
         token
      )
      
      result <- .rs.makeCompletions(
         token,
         completions,
         paste("attributes(", objectName, ")", sep = ""),
         quote = TRUE,
         type = .rs.acCompletionTypes$STRING
      )
      
   }, error = function(e) .rs.emptyCompletions())
   result
})

.rs.addFunction("isBrowserActive", function()
{
   .Call("rs_isBrowserActive")
})

# NOTE: This function attempts to find an active frame (if
# any) in the current caller's context, which may be a
# function environment if we're currently in the debugger,
# and just the .GlobalEnv otherwise. Because the
# '.rs.rpc.get_completions' call gets its own set of 'stack
# frames', we must poke into 'sys.frames()' and get the last
# frame not part of that stack. In practice, this means
# discarding the current frame, as well as any extra frames
# from the caller (hence, the 'offset' argument). Of course,
# when there is no debug frame active, we just want to draw
# our completions from the global environment.
.rs.addFunction("getActiveFrame", function(offset = 1L)
{
   frames <- c(.GlobalEnv, sys.frames())
   frames[[length(frames) - 1L - offset]]
})

.rs.addFunction("getCompletionsNativeRoutine", function(token, interface)
{
   # For a package which has dynamic symbol loading, just get the strings.
   # For other packages, search the namespace for the symbol.
   loadedDLLs <- getLoadedDLLs()
   routines <- lapply(loadedDLLs, getDLLRegisteredRoutines)
   
   isDynamic <- unlist(lapply(loadedDLLs, `[[`, "dynamicLookup"))
   
   dynRoutines <- c(
      routines[isDynamic],
      routines["(embedding)"]
   )
   
   dynRoutineNames <- lapply(dynRoutines, function(x) {
      names(x[[interface]])
   })
   
   if (is.null(names(dynRoutineNames)))
      return(.rs.emptyCompletions())
   
   dynResults <- .rs.namedVectorAsList(dynRoutineNames)
   dynIndices <- .rs.fuzzyMatches(dynResults$values, token)
   
   .rs.makeCompletions(token = token,
                       results = dynResults$values[dynIndices],
                       packages = dynResults$names[dynIndices],
                       quote = TRUE,
                       type = .rs.acCompletionTypes$STRING)
   
})

.rs.addFunction("mightBeShinyFunction", function(string)
{
   # Try seeing if there's a function with this name on the
   # search path, and return TRUE if it has 'inputId' or
   # 'outputId' as parameters.
   result <- tryCatch(get(string, pos = 0), error = function(e) NULL)
   if (is.function(result) && !is.primitive(result))
   {
      formalNames <- names(formals(result))
      if (any(c("inputId", "outputId") %in% formalNames))
         return(TRUE)
   }
      
   # Try seeing if there's a function of this name
   # in the 'shiny' completion database.
   shinyFunctions <- .rs.getInferredCompletions("shiny")$functions
   if (string %in% names(shinyFunctions))
      return(TRUE)
   
   return(FALSE)
})

.rs.addFunction("getKnitParamsForDocument", function(documentId)
{
   .Call("rs_getKnitParamsForDocument", documentId)
})

.rs.addFunction("knitParams", function(content)
{
   if (!("knitr" %in% loadedNamespaces()))
      if (!requireNamespace("knitr", quietly = TRUE))
         return(NULL)
   
   if (!("knit_params" %in% getNamespaceExports(asNamespace("knitr"))))
      return(NULL)
   
   knitr::knit_params(content)
})

.rs.addFunction("getCompletionsRMarkdownParams", function(token, type, documentId)
{
   # TODO: Bail if 'params' is already defined?
   if (exists("params", envir = .GlobalEnv))
      return(NULL)
   
   params <- .rs.getKnitParamsForDocument(documentId)
   if (!length(params))
      return(.rs.emptyCompletions())
   
   names <- vapply(params, FUN.VALUE = character(1), USE.NAMES = FALSE, function(x) {
      x$name
   })
   
   completions <- .rs.selectFuzzyMatches(names, token)
   .rs.makeCompletions(token = token,
                       results = completions)
})

.rs.addFunction("injectKnitrParamsObject", function(documentId)
{
   if (exists("params", envir = .GlobalEnv))
      return(FALSE)
   
   params <- .rs.getKnitParamsForDocument(documentId)
   if (!length(params))
      return(FALSE)
   
   mockedObject <- lapply(params, `[[`, "value")
   names(mockedObject) <- unlist(lapply(params, `[[`, "name"))
   class(mockedObject) <- "rstudio_mock"
   assign("params", mockedObject, envir = .GlobalEnv)
   
   return(TRUE)
})

.rs.addFunction("removeKnitrParamsObject", function()
{
   if (exists("params", envir = .GlobalEnv))
   {
      params <- get("params", envir = .GlobalEnv)
      if (inherits(params, "rstudio_mock"))
         remove(params, envir = .GlobalEnv) 
   }
})

.rs.addFunction("getCompletionsEnvironmentVariables", function(token)
{
   candidates <- names(Sys.getenv())
   results <- .rs.selectFuzzyMatches(candidates, token)
   
   .rs.makeCompletions(token = token,
                       results = results,
                       quote = TRUE,
                       type = .rs.acCompletionTypes$STRING)
})

.rs.addJsonRpcHandler("get_completions", function(token,
                                                  string,
                                                  type,
                                                  numCommas,
                                                  functionCallString,
                                                  chainObjectName,
                                                  additionalArgs,
                                                  excludeArgs,
                                                  excludeArgsFromObject,
                                                  filePath,
                                                  documentId,
                                                  line)
{
   # Ensure UTF-8 encoding, as that's the encoding set when passed down from
   # the client
   token <- .rs.setEncodingUnknownToUTF8(token)
   string <- .rs.setEncodingUnknownToUTF8(string)
   functionCallString <- .rs.setEncodingUnknownToUTF8(functionCallString)
   chainObjectName <- .rs.setEncodingUnknownToUTF8(chainObjectName)
   additionalArgs <- .rs.setEncodingUnknownToUTF8(additionalArgs)
   excludeArgs <- .rs.setEncodingUnknownToUTF8(excludeArgs)
   excludeArgsFromObject <- .rs.setEncodingUnknownToUTF8(excludeArgsFromObject)
   filePath <- .rs.setEncodingUnknownToUTF8(filePath)
   
   # Inject 'params' into the global env to provide for completions in
   # parameterized R Markdown documents
   if (.rs.injectKnitrParamsObject(documentId))
      on.exit(.rs.removeKnitrParamsObject(), add = TRUE)

   # If custom completions have been set through
   # 'rc.options("custom.completions")',
   # then use the internal R completions instead.
   if (.rs.isCustomCompletionsEnabled()) {
      
      completions <- tryCatch(
         .rs.getCustomRCompletions(line),
         error = identity
      )
      
      if (!inherits(completions, "error"))
         return(completions)
   }
   
   # Get the currently active frame
   envir <- .rs.getActiveFrame()
   
   filePath <- suppressWarnings(.rs.normalizePath(filePath))
   
   ## NOTE: these are passed in as lists of strings; convert to character
   additionalArgs <- as.character(additionalArgs)
   excludeArgs <- as.character(excludeArgs)
   
   ## For magrittr completions, we may see a pipe thrown in as part of the 'string'
   ## and 'functionCallString'. In such a case, we need to strip off everything before
   ## a '%>%' and signal to drop the first argument for that function.
   dropFirstArgument <- FALSE
   if (length(string))
   {
      pipes <- c("%>%", "%<>%", "%T>%", "%>>%")
      pattern <- paste(pipes, collapse = "|")
      
      stringPipeMatches <- gregexpr(
         pattern, string[[1]], perl = TRUE
      )[[1]]
      
      if (!identical(c(stringPipeMatches), -1L))
      {
         n <- length(stringPipeMatches)
         idx <- stringPipeMatches[n] + attr(stringPipeMatches, "match.length")[n]
         dropFirstArgument <- TRUE
         string[[1]] <- gsub("^[\\s\\n]*", "", substring(string[[1]], idx), perl = TRUE)
         
         ## Figure out the 'parent object' of the call. We munge the
         ## function call and place that back in, so S3 dispatch and such
         ## can work.
         firstPipeIdx <- stringPipeMatches[[1]]
         parentObject <- .rs.trimWhitespace(
            substring(functionCallString, 1, firstPipeIdx - 1L)
         )
         
         functionCallString <- gsub(
            "^\\s*",
            "",
            substring(functionCallString, idx),
            perl = TRUE
         )
         
         # Add the argument back in
         functionCallString <- sub(
            "(",
            paste("(", parentObject, ",", sep = ""),
            functionCallString,
            fixed = TRUE
         )
      }
   }
   
   ## Try to parse the function call string
   functionCall <- tryCatch({
      parse(text = .rs.finishExpression(functionCallString))[[1]]
   }, error = function(e)
      NULL
   )
   
   ## Handle some special cases early
   
   # custom help handler for arguments
   if (.rs.acContextTypes$FUNCTION %in% type) {
      scope <- string[[1]]
      custom <- .rs.findCustomHelpContext(scope, "help_formals_handler")
      if (!is.null(custom)) {
         formals <- custom$handler(custom$topic, custom$source)
         if (!is.null(formals)) {
            results <- paste(formals$formals, "= ")
            results <- .rs.selectFuzzyMatches(results, token)
            return(.rs.makeCompletions(
               token = token,
               results = results,
               packages = scope,
               type = .rs.acCompletionTypes$ARGUMENT,
               excludeOtherCompletions = TRUE,
               helpHandler = formals$helpHandler)
            )
         } else {
            return (.rs.emptyCompletions(excludeOtherCompletions = TRUE))
         }
      }
   }
   
   # help
   if (.rs.acContextTypes$HELP %in% type)
      return(.rs.getCompletionsHelp(token))
   
   # Roxygen
   if (.rs.acContextTypes$ROXYGEN %in% type)
      return(.rs.attemptRoxygenTagCompletion(token))
   
   # install.packages
   if (length(string) && string[[1]] == "install.packages" && numCommas[[1]] == 0)
      return(.rs.getCompletionsInstallPackages(token))
   
   # example / help
   if (nzchar(token) &&
       length(string) &&
       string[[1]] %in% c("help", "example") &&
       numCommas[[1]] == 0)
   {
      return(.rs.getCompletionsHelp(token, quote = TRUE))
   }
   
   # vignettes
   if (length(string) && string[[1]] == "vignette" && numCommas[[1]] == 0)
      return(.rs.getCompletionsVignettes(token))
   
   # .Call, .C, .Fortran, .External
   if (length(string) && 
       string[[1]] %in% c(".Call", ".C", ".Fortran", ".External") &&
       numCommas[[1]] == 0)
   {
      completions <- .rs.appendCompletions(
         .rs.getCompletionsNativeRoutine(token, string[[1]]),
         .rs.getCompletionsSearchPath(token))
      return(completions)
   }
   
   # data
   if (.rs.acContextTypes$FUNCTION %in% type &&
       string[[1]] == "data" &&
       numCommas[[1]] == 0)
      return(.rs.getCompletionsData(token))
   
   # package name
   if (.rs.acContextTypes$PACKAGE %in% type)
      return(.rs.getCompletionsPackages(token = token,
                                        appendColons = TRUE,
                                        excludeOtherCompletions = TRUE))
   
   # environment variables
   if (length(string) &&
       string[[1]] %in% c("Sys.getenv", "Sys.setenv") &&
       numCommas[[1]] == 0)
      return(.rs.getCompletionsEnvironmentVariables(token))
   
   # No information on completions other than token
   if (!length(string))
   {
      # If there was no token, give up
      if (token == "")
         return(.rs.emptyCompletions(excludeOtherCompletions = TRUE))
      
      # Otherwise, complete from the seach path + available packages
      completions <- Reduce(.rs.appendCompletions, list(
         .rs.getCompletionsSearchPath(token),
         .rs.getCompletionsNAMESPACE(token, documentId),
         .rs.getCompletionsPackages(token, TRUE),
         .rs.getCompletionsActiveFrame(token, envir),
         .rs.getCompletionsLibraryContext(token,
                                          string,
                                          type,
                                          numCommas,
                                          functionCall,
                                          dropFirstArgument,
                                          documentId,
                                          envir)
      ))
      
      # remove all queries that start with a '.' unless the
      # token itself starts with a dot
      if (!.rs.startsWith(token, "."))
      {
         startsWithDot <- .rs.startsWith(completions$results, ".")
         completions <- .rs.subsetCompletions(completions, which(!startsWithDot))
      }
      
      # try completing from the source index if this is an
      # R file within a package
      if (.rs.isRScriptInPackageBuildTarget(filePath))
      {
         # get the active package
         pkgName <- .rs.packageNameForSourceFile(filePath)
         completions <- .rs.appendCompletions(
            completions,
            .rs.getCompletionsActivePackage(token, pkgName)
         )
      }
      
      # Ensure case-sensitive matches come first.
      return(.rs.sortCompletions(completions, token))
   }
   
   # transform 'install.packages' to 'utils::install.packages' to avoid
   # getting arguments from the RStudio hook
   if ("install.packages" %in% string[[1]])
   {
      fn <- .rs.getAnywhere("install.packages", envir)
      if (is.function(fn) && identical(names(formals(fn)), "..."))
      {
         string[[1]] <- "utils::install.packages"
      }
   }
   
   # library, require, requireNamespace, loadNamespace
   if (string[[1]] %in% c("library", "require", "requireNamespace") &&
       numCommas[[1]] == 0)
   {
      quote <- !(string[[1]] %in% c("library", "require"))
      return(.rs.getCompletionsPackages(token, 
                                        excludeOtherCompletions = TRUE,
                                        quote = quote))
   }
   
   ## File-based completions
   if (.rs.acContextTypes$FILE %in% type)
   {
      whichIndex <- which(type == .rs.acContextTypes$FILE)
      
      tokenToUse <- string[[whichIndex]]
      
      directoriesOnly <- FALSE
      if (length(string) > whichIndex)
      {
         if (string[[whichIndex + 1]] %in% c("list.files",
                                             "list.dirs",
                                             "dir",
                                             "setwd"))
         {
            directoriesOnly <- TRUE
         }
      }
      
      # NOTE: For Markdown link completions, we overload the meaning of the
      # function call string here, and use it as a signal to generate paths
      # relative to the R markdown path.
      isMarkdownLink <- identical(functionCallString, "useFile")
      isRmd <- .rs.endsWith(tolower(filePath), ".rmd")
      
      path <- NULL
      
      if (!isMarkdownLink && isRmd) 
      {
         # if in an Rmd file, ask it for its desired working dir (can be changed
         # with the knitr root.dir option)
         path <- .Call("rs_getRmdWorkingDir", filePath, documentId)
      }

      if (is.null(path) && (isMarkdownLink || isRmd)) 
      {
         # for links, or R Markdown without an explicit working dir, use the
         # base directory of the file
         path <- suppressWarnings(.rs.normalizePath(dirname(filePath)))
      }

      if (is.null(path))
      {
         # in all other cases, use the current working directory for
         # completions
         path <- getwd()
      }
      
      return(.rs.getCompletionsFile(token = tokenToUse,
                                    path = path,
                                    quote = FALSE,
                                    directoriesOnly = directoriesOnly))
   }
   
   # Shiny completions
   
   ## Completions for server.r (from ui.r)
   if (type[[1]] %in% c(.rs.acContextTypes$DOLLAR,
                        .rs.acContextTypes$DOUBLE_BRACKET) &&
       tolower(basename(filePath)) == "server.r" &&
       string[[1]] %in% c("input", "output"))
   {
      completions <- .rs.getCompletionsFromShinyUI(token, filePath, string[[1]], type[[1]])
      if (!is.null(completions))
         return(completions)
   }
   
   ## Completions for server.r, on session
   if (type[[1]] == .rs.acContextTypes$DOLLAR &&
       tolower(basename(filePath)) == "server.r" &&
       string[[1]] == "session")
   {
      completions <- .rs.getCompletionsShinySession(token)
      if (!is.null(completions))
         return(completions)
   }
   
   ## Completions for ui.r (from server.r)
   if (type[[1]] == .rs.acContextTypes$FUNCTION &&
       tolower(basename(filePath)) == "ui.r" &&
       numCommas[[1]] == 0 &&
       .rs.mightBeShinyFunction(string[[1]]))
   {
      completions <- .rs.getCompletionsFromShinyServer(token, filePath, string[[1]], type[[1]])
      if (!is.null(completions))
         return(completions)
   }
   
   ## Other special cases (but we may still want completions from
   ## other contexts)
   
   # attr
   completions <- if (string[[1]] == "attr")
   {
      .rs.getCompletionsAttr(token, functionCall, envir)
   }
   
   # getOption
   else if (string[[1]] == "getOption" && numCommas[[1]] == 0)
   {
      .rs.getCompletionsGetOption(token)
   }
   
   # options
   else if (string[[1]] == "options" && type == .rs.acContextTypes$FUNCTION)
   {
      .rs.getCompletionsOptions(token)
   }
   
   # no special case (start with empty completions)
   else
   {
      .rs.emptyCompletions()
   }
   
   ## If we are getting completions from a '$', '@', '::' or ':::' then we do not
   ## want to look for completions in other contexts
   # dollar context
   dontLookBack <- length(type) && type[[1]] %in% c(
      .rs.acContextTypes$DOLLAR, .rs.acContextTypes$AT,
      .rs.acContextTypes$NAMESPACE_EXPORTED,
      .rs.acContextTypes$NAMESPACE_ALL
   )
   
   if (dontLookBack)
   {
      # dollar context
      if (type[[1]] %in% c(.rs.acContextTypes$DOLLAR, .rs.acContextTypes$AT))
      {
         completions <- .rs.getCompletionsDollar(
            token,
            string[[1]],
            functionCall,
            envir,
            type[[1]] == .rs.acContextTypes$AT
         )
      }
      
      # namespace context
      else if (type[[1]] %in% c(.rs.acContextTypes$NAMESPACE_EXPORTED,
                                .rs.acContextTypes$NAMESPACE_ALL))
      {
         completions <- .rs.getCompletionsNamespace(
            token,
            string[[1]],
            type[[1]] == .rs.acContextTypes$NAMESPACE_EXPORTED,
            envir
         )
      }
   }
   
   # otherwise, look through the contexts and pick up completions
   else
   {
      for (i in seq_along(string))
      {
         discardFirst <-
            (dropFirstArgument) ||
            (type[[i]] == .rs.acContextTypes$FUNCTION && chainObjectName != "")
         
         completions <- .rs.appendCompletions(
            completions,
            .rs.getRCompletions(token,
                                string[[i]],
                                type[[i]],
                                numCommas[[i]],
                                functionCall,
                                discardFirst,
                                documentId,
                                envir)
         )
      }
   }
   
   # get completions from the search path for the 'generic' contexts
   if (token != "" &&
       type[[1]] %in% c(.rs.acContextTypes$UNKNOWN,
                        .rs.acContextTypes$FUNCTION,
                        .rs.acContextTypes$ARGUMENT,
                        .rs.acContextTypes$SINGLE_BRACKET,
                        .rs.acContextTypes$DOUBLE_BRACKET))
   {
      discardFirst <-
         (dropFirstArgument) ||
         (type[[1]] == .rs.acContextTypes$FUNCTION && chainObjectName != "")
      
      completions <- Reduce(.rs.appendCompletions, list(
         completions,
         .rs.getCompletionsSearchPath(token),
         .rs.getCompletionsNAMESPACE(token, documentId),
         .rs.getCompletionsActiveFrame(token, envir),
         .rs.getCompletionsLibraryContext(token,
                                          string[[1]],
                                          type[[1]],
                                          numCommas[[1]],
                                          functionCall,
                                          discardFirst,
                                          documentId,
                                          envir)
      ))
      
      if (.rs.isRScriptInPackageBuildTarget(filePath))
      {
         pkgName <- .rs.packageNameForSourceFile(filePath)
         completions <- .rs.appendCompletions(
            completions,
            .rs.getCompletionsActivePackage(token, pkgName)
         )
      }
   }
   
   ## Package completions (e.g. `stats::`)
   if (token != "" && .rs.acContextTypes$UNKNOWN %in% type)
      completions <- .rs.appendCompletions(
         completions,
         .rs.getCompletionsPackages(token, TRUE)
      )
   
   ## If the caller has supplied information about chain completions (e.g.
   ## for completions from
   ##
   ##    x %>% foo() %>% bar()
   ##
   ## then retrieve those completions.
   completions <- .rs.appendCompletions(
      completions,
      .rs.getRChainCompletions(token,
                               chainObjectName,
                               additionalArgs,
                               excludeArgs,
                               excludeArgsFromObject,
                               discardFirst,
                               envir)
   )
   
   ## Override param insertion if the function was 'debug' or 'trace'
   for (i in seq_along(type))
   {
      if (type[[i]] %in% c(.rs.acContextTypes$FUNCTION, .rs.acContextTypes$UNKNOWN))
      {
         ## Blacklist certain functions
         if (string[[i]] %in% c("help", "str", "args", "debug", "debugonce", "trace"))
         {
            completions$overrideInsertParens <- .rs.scalar(TRUE)
         }
         else
         {
            ## Blacklist based on formals of the function
            object <- .rs.getAnywhere(string[[i]], envir)
            if (is.function(object))
            {
               argNames <- .rs.getFunctionArgumentNames(object)
               if (any(c("f", "fun", "func") %in% tolower(gsub("[^a-zA-Z]", "", argNames))))
                  completions$overrideInsertParens <- .rs.scalar(TRUE)
            }
         }
      }
   }
   
   if (nzchar(token))
      completions <- .rs.sortCompletions(completions, token)
   
   completions$token <- token
   completions
   
})

.rs.addJsonRpcHandler("get_dplyr_join_completions", function(token,
                                                             leftDataName,
                                                             rightDataName,
                                                             verb,
                                                             cursorPos)
{
   .rs.getDplyrJoinCompletions(
      token,
      leftDataName,
      rightDataName,
      verb,
      cursorPos,
      .rs.getActiveFrame()
   )
   
})

.rs.addJsonRpcHandler("get_dplyr_join_completions_string", function(token,
                                                                    string,
                                                                    cursorPos)
{
   result <- tryCatch(
      
      expr = {
         parsed <- parse(text = string)[[1]]
         verb <- as.character(parsed[[1]])
         func <- get(verb, envir = asNamespace("dplyr"))
         matched <- .rs.matchCall(func, parsed)
         leftName <- as.character(matched[["x"]])
         rightName <- as.character(matched[["y"]])
         .rs.getDplyrJoinCompletions(
            token,
            leftName,
            rightName,
            verb,
            cursorPos,
            parent.frame()
         )
      },
      
      error = function(e) {
         .rs.emptyCompletions()
      }
   )
   
   result
})

.rs.addFunction("getDplyrJoinCompletions", function(token,
                                                    leftDataName,
                                                    rightDataName,
                                                    verb,
                                                    cursorPos,
                                                    envir)
{
   result <- .rs.emptyCompletions()
   
   leftData <- .rs.getAnywhere(leftDataName, envir)
   rightData <- .rs.getAnywhere(rightDataName, envir)
   
   if (cursorPos == "left" && !is.null(leftData))
   {
      completions <- .rs.selectFuzzyMatches(
         .rs.getNames(leftData),
         token
      )
      result <- .rs.makeCompletions(
         token = token,
         results = completions,
         packages = leftDataName,
         quote = TRUE,
         type = vapply(leftData[completions], FUN.VALUE = numeric(1), USE.NAMES = FALSE, .rs.getCompletionType),
         excludeOtherCompletions = TRUE
      )
   }
   else if (cursorPos == "right" && !is.null(rightData))
   {
      completions <- .rs.selectFuzzyMatches(
         .rs.getNames(rightData),
         token
      )
      result <- .rs.makeCompletions(
         token = token,
         results = completions,
         packages = rightDataName,
         quote = TRUE,
         type = vapply(rightData[completions], FUN.VALUE = numeric(1), USE.NAMES = FALSE, .rs.getCompletionType),
         excludeOtherCompletions = TRUE
      )
   }
   
   result
   
})

.rs.addFunction("getRChainCompletions", function(token,
                                                 chainObjectName,
                                                 additionalArgs,
                                                 excludeArgs,
                                                 excludeArgsFromObject,
                                                 discardFirst,
                                                 envir)
{
   ## chainObjectName will be provided if the client detected
   ## that we were performing completions within an e.g.
   ## `%>%` chain -- use completions from the associated data object.
   result <- .rs.emptyCompletions()
   
   if (!is.null(chainObjectName) && !excludeArgsFromObject)
   {
      object <- .rs.getAnywhere(chainObjectName, envir = envir)
      if (length(object))
      {
         objectNames <- .rs.getNames(object)
         if (length(objectNames))
         {
            completions <- .rs.selectFuzzyMatches(objectNames, token)
            type <- vapply(completions, FUN.VALUE = numeric(1), USE.NAMES = FALSE, function(i) {
               .rs.getCompletionType(object[[i]])
            })
            
            result <- .rs.makeCompletions(
               token = token,
               results = completions,
               packages = paste("[", chainObjectName, "]", sep = ""),
               quote = FALSE,
               type = type
            )
         }
      }
   }
   
   if (length(additionalArgs))
   {
      argsToAdd <- .rs.selectFuzzyMatches(additionalArgs, token)
      result <- .rs.appendCompletions(
         result,
         .rs.makeCompletions(
            token = token,
            results = argsToAdd,
            packages = paste("*", chainObjectName, "*", sep = ""),
            type = .rs.acCompletionTypes$UNKNOWN
         )
      )
   }
   
   if (length(excludeArgs))
   {
      indices <- which(!(result$results %in% excludeArgs))
      result <- .rs.subsetCompletions(result, indices)
   }
   
   result
   
})

.rs.addFunction("getRCompletions", function(token,
                                            string,
                                            type,
                                            numCommas,
                                            functionCall,
                                            discardFirst,
                                            documentId,
                                            envir)
{
   .rs.appendCompletions(
      .rs.getCompletionsLibraryContext(token, string, type, numCommas, functionCall, discardFirst, documentId, envir),
      if (type == .rs.acContextTypes$FUNCTION)
         .rs.getCompletionsFunction(token, string, functionCall, numCommas, discardFirst, envir)
      else if (type == .rs.acContextTypes$ARGUMENT)
         .rs.getCompletionsArgument(token, string, functionCall, envir)
      else if (type == .rs.acContextTypes$SINGLE_BRACKET)
         .rs.getCompletionsSingleBracket(token, string, functionCall, numCommas, envir)
      else if (type == .rs.acContextTypes$DOUBLE_BRACKET)
         .rs.getCompletionsDoubleBracket(token, string, functionCall, envir)
      else
         .rs.emptyCompletions()
   )
})

## NOTE: This is a modified version of 'matchAvailableTopics'
## in 'completions.R' of the R sources.
.rs.addFunction("getCompletionsHelp", function(token,
                                               quote = FALSE)
{
   ## Attempt to find the help topic item in the local
   ## cache, as reading files from disk multiple times
   ## could be slow
   pkgCacheName <- ".completions.attachedPackagesCache"
   helpTopicsName <- ".completions.helpTopics"
   rsEnvPos <- which(search() == "tools:rstudio")
   
   attachedPackagesCache <- tryCatch(
      get(pkgCacheName, pos = rsEnvPos),
      error = function(e) character()
   )
   
   ## If the the current search paths have changed, invalidate
   ## the cache and update our aliases
   paths <- searchpaths()[substring(search(), 1, 8) == "package:"]
   if (!identical(basename(paths), attachedPackagesCache))
   {
      assign(pkgCacheName,
             basename(paths),
             pos = rsEnvPos)
      
      # Get the set of help topics
      topics <- lapply(paths, .rs.readAliases)
      names(topics) <- basename(paths)
      
      assign(helpTopicsName,
             topics,
             pos = rsEnvPos)
   }
   
   aliases <- get(helpTopicsName, pos = rsEnvPos)
   
   ## If the token is of the form `<pkg>::<topic>`,
   ## then attempt to get the topic 'topic' for that
   ## package.
   if (regexpr(":{2,3}", token, perl = TRUE) != -1)
   {
      splat <- strsplit(token, ":{2,3}", perl = TRUE)[[1]]
      pkg <- splat[[1]]
      
      token <- if (length(splat) > 1)
         splat[[2]]
      else
         ""
      
      ## If the help topics for this package have not been loaded,
      ## explicitly load and save them now.
      if (!(pkg %in% names(aliases)))
      {
         pkgPath <- tryCatch(
            find.package(pkg, quiet = TRUE),
            error = function(e) NULL
         )
         
         if (!is.null(pkgPath))
         {
            aliases[[pkg]] <- .rs.readAliases(pkgPath)
            assign(helpTopicsName, aliases, pos = rsEnvPos)
         }
      }
      
      aliases <- tryCatch(
         aliases[[pkg]],
         error = function(e) character()
      )
   }
   
   aliases <- unlist(aliases)
   results <- .rs.selectFuzzyMatches(aliases, token)
   
   completions <- .rs.makeCompletions(
      token = token,
      results = results,
      quote = quote,
      type = .rs.acCompletionTypes$HELP,
      overrideInsertParens = TRUE
   )
   
   .rs.sortCompletions(completions, token)

})

.rs.addFunction("readAliases", function(path)
{
   if (!length(path))
      return(character())

   if (file.exists(f <- file.path(path, "help", "aliases.rds")))
      names(readRDS(f))
   else
      character()
})

.rs.addFunction("getCompletionsActivePackage", function(token, pkgName)
{
   # Get completions from the source index
   sourceIndexCompletions <- .rs.getSourceIndexCompletions(token)
   
   # format completions for return to R
   if (!length(sourceIndexCompletions$completions))
   {
      completions <- .rs.emptyCompletions()
   }
   else
   {
      results <- sourceIndexCompletions$completions
      
      # TODO: more granular lookup on the object type for source index completions
      type <- ifelse(sourceIndexCompletions$isFunction,
                     .rs.acCompletionTypes$FUNCTION,
                     .rs.acCompletionTypes$UNKNOWN)
      
      completions <- .rs.makeCompletions(token = token,
                                         results = results,
                                         packages = pkgName,
                                         quote = FALSE,
                                         type = type)
   }
   
   # get completions from the imports of the package
   importCompletions <- tryCatch(
      getNamespaceImports(asNamespace(pkgName)),
      error = function(e) NULL
   )
   
   # workaround for devtools::load_all() clobbering the
   # namespace imports
   if (length(importCompletions) && is.null(names(importCompletions))) {
      try({
         env <- new.env(parent = emptyenv())
         for (el in importCompletions) {
            # Length one element: get all exports from that package
            if (length(el) == 1) {
               env[[el]] <- c(env[[el]], getNamespaceExports(asNamespace(el)))
            }
            
            # Length >1 element: 'importFrom()'.
            else if (length(el) > 1) {
               env[[el]] <- c(env[[el]], tail(el, n = -1))
            }
         }
         importCompletions <- as.list(env)
      }, silent = TRUE)
   }
   
   # remove 'base' element if it's just TRUE
   if (length(importCompletions))
   {
      if (isTRUE(importCompletions[["base"]]))
         importCompletions$base <- NULL
   }
   
   # if we have import completions, use them
   if (length(importCompletions) && !is.null(names(importCompletions)))
   {
      importCompletionsList <- .rs.namedVectorAsList(importCompletions)
      
      # filter completions
      indices <- .rs.fuzzyMatches(importCompletionsList$values, token)
      importCompletionsList <- lapply(importCompletionsList, function(x) {
         x[indices]
      })
      
      objects <- lapply(seq_along(importCompletionsList$values), function(i) {
         
         tryCatch(
            expr = get(importCompletionsList$values[[i]],
                       envir = asNamespace(importCompletionsList$names[[i]])),
            error = function(e) NULL
         )
      })
      
      type <- vapply(objects, FUN.VALUE = numeric(1), USE.NAMES = FALSE, .rs.getCompletionType)
      
      completions <- .rs.appendCompletions(
         completions,
         .rs.makeCompletions(token = token,
                             results = importCompletionsList$values,
                             packages = paste("package:", importCompletionsList$names, sep = ""),
                             type = type)
      )
   }
   
   completions
   
})

.rs.addFunction("getCompletionsFromShinyServer", function(token, filePath, string, type)
{
   dir <- dirname(filePath)
   serverPath <- file.path(dir, "server.R")
   uiPath <- file.path(dir, "ui.R")
   name <- tolower(basename(filePath))
   
   if (!file.exists(serverPath))
      return(NULL)
   
   completions <- .rs.shinyServerCompletions(serverPath)
   results <- if (.rs.endsWith(string, "Input"))
      .rs.selectFuzzyMatches(completions$input, token)
   else
      .rs.selectFuzzyMatches(completions$output, token)
   
   return(.rs.makeCompletions(token = token,
                              results = results,
                              packages = serverPath,
                              quote = TRUE,
                              type = .rs.acCompletionTypes$CONTEXT,
                              excludeOtherCompletions = TRUE))
})

.rs.addFunction("getCompletionsFromShinyUI", function(token, filePath, string, type)
{
   dir <- dirname(filePath)
   serverPath <- file.path(dir, "server.R")
   uiPath <- file.path(dir, "ui.R")
   name <- tolower(basename(filePath))
   
   if (!file.exists(uiPath))
      return(NULL)
   
   completions <- .rs.shinyUICompletions(uiPath)
   results <- if (string == "input")
      .rs.selectFuzzyMatches(completions$input, token)
   else
      .rs.selectFuzzyMatches(completions$output, token)
   
   return(.rs.makeCompletions(token = token,
                              results = results,
                              packages = uiPath,
                              quote = type == .rs.acContextTypes$DOUBLE_BRACKET,
                              type = .rs.acCompletionTypes$CONTEXT,
                              excludeOtherCompletions = type == .rs.acContextTypes$DOLLAR))
})

.rs.addFunction("shinyUICompletions", function(file)
{
   # Check to see if we can re-use cached completions
   fileCacheName <- paste(file, "shinyUILastModifiedTime", sep = "-")
   completionsCacheName <- paste(file, "shinyUICompletions", sep = "-")
   
   info <- file.info(file)
   mtime <- info[1, "mtime"]
   if (identical(mtime, .rs.get(fileCacheName)) &&
       !is.null(.rs.get(completionsCacheName)))
   {
      return(.rs.get(completionsCacheName))
   }
   
   # Otherwise, get the completions
   parsed <- tryCatch(
      suppressWarnings(parse(file)),
      error = function(e) NULL
   )
   
   if (is.null(parsed))
      return(NULL)
   
   # We fill environments (since these can grow efficiently / by reference)
   inputEnv <- new.env(parent = emptyenv())
   outputEnv <- new.env(parent = emptyenv())
   
   inputCount <- new.env(parent = emptyenv())
   inputCount$count <- 1
   
   outputCount <- new.env(parent = emptyenv())
   outputCount$count <- 1
   
   shinyFunctions <- .rs.getInferredCompletions("shiny")$functions
   lapply(parsed, function(object) {
      .rs.doShinyUICompletions(object, inputEnv, outputEnv, inputCount, outputCount, shinyFunctions)
   })
   
   completions <- list(input = unlist(mget(objects(inputEnv), envir = inputEnv), use.names = FALSE),
                       output = unlist(mget(objects(outputEnv), envir = outputEnv), use.names = FALSE))
   
   .rs.assign(fileCacheName, mtime)
   .rs.assign(completionsCacheName, completions)
   
   completions
   
})

.rs.addFunction("extractFunctionNameFromCall", function(object)
{
   if (!is.call(object))
      return("")
   
   if (.rs.isSymbolCalled(object[[1]], "::") ||
       .rs.isSymbolCalled(object[[1]], ":::") ||
       (is.character(object[[1]]) && object[[1]] %in% c("::", ":::")))
   {
      if (length(object) > 2)
         return(as.character(object[[3]]))
      else
         return("")
   }
   
   if (is.character(object[[1]]) || is.symbol(object[[1]]))
      return(as.character(object[[1]]))
   
   if (is.call(object[[1]]))
      return(.rs.extractFunctionNameFromCall(object[[1]]))
   
   return("")
   
})

.rs.addFunction("doShinyUICompletions", function(object,
                                                 inputs,
                                                 outputs,
                                                 inputCount,
                                                 outputCount,
                                                 shinyFunctions)
{
   if (is.call(object) && length(object) > 1)
   {
      functionName <- .rs.extractFunctionNameFromCall(object)
      firstArgName <- if (is.character(object[[2]]) && length(object[[2]]) == 1)
         object[[2]]
      else
         ""
      
      if (nzchar(firstArgName))
      {
         functionArgs <- shinyFunctions[[functionName]]
         if ("outputId" %in% functionArgs || .rs.endsWith(functionName, "Output"))
         {
            outputCount$count <- outputCount$count + 1
            outputs[[as.character(outputCount$count)]] <- firstArgName
         }
         
         else if ("inputId" %in% functionArgs || .rs.endsWith(functionName, "Input"))
         {
            inputCount$count <- inputCount$count + 1
            inputs[[as.character(inputCount$count)]] <- firstArgName
         }
      }
      
      for (j in 2:length(object))
      {
         if (is.call(object[[j]]))
         {
            .rs.doShinyUICompletions(object[[j]],
                                     inputs,
                                     outputs,
                                     inputCount,
                                     outputCount,
                                     shinyFunctions)
         }
      }
   }
   
})

.rs.addFunction("shinyServerCompletions", function(file)
{
   # Check to see if we can re-use cached completions
   fileCacheName <- paste(file, "shinyServerLastModifiedTime", sep = "-")
   completionsCacheName <- paste(file, "shinyServerCompletions", sep = "-")
   
   info <- file.info(file)
   mtime <- info[1, "mtime"]
   if (identical(mtime, .rs.get(fileCacheName)) &&
       !is.null(.rs.get(completionsCacheName)))
   {
      return(.rs.get(completionsCacheName))
   }
   
   # Otherwise, get the completions
   parsed <- tryCatch(
      suppressWarnings(parse(file)),
      error = function(e) NULL
   )
   
   if (is.null(parsed))
      return(NULL)
   
   # We fill environments (since these can grow efficiently / by reference)
   inputEnv <- new.env(parent = emptyenv())
   outputEnv <- new.env(parent = emptyenv())
   
   inputCount <- new.env(parent = emptyenv())
   inputCount$count <- 1
   
   outputCount <- new.env(parent = emptyenv())
   outputCount$count <- 1
   
   lapply(parsed, function(object) {
      .rs.doShinyServerCompletions(object, inputEnv, outputEnv, inputCount, outputCount)
   })
   
   completions <- list(input = unlist(mget(objects(inputEnv), envir = inputEnv), use.names = FALSE),
                       output = unlist(mget(objects(outputEnv), envir = outputEnv), use.names = FALSE))
   
   .rs.assign(fileCacheName, mtime)
   .rs.assign(completionsCacheName, completions)
   
   completions
   
})

.rs.addFunction("doShinyServerCompletions", function(object,
                                                     inputs,
                                                     outputs,
                                                     inputCount,
                                                     outputCount)
{
   if (is.call(object))
   {
      operator <- as.character(object[[1]])
      if (operator == "$" || operator == "[[")
      {
         name <- if (is.symbol(object[[2]]))
            as.character(object[[2]])
         else if (is.character(object[[2]]) && length(object[[2]]) == 1)
            object[[2]]
         else
            ""
         
         value <- as.character(object[[3]])
         if (name == "output")
         {
            outputCount$count <- outputCount$count + 1
            outputs[[as.character(outputCount$count)]] <- .rs.stripSurrounding(value)
         }
         
         if (name == "input")
         {
            inputCount$count <- inputCount$count + 1
            inputs[[as.character(inputCount$count)]] <- .rs.stripSurrounding(value)
         }
      }
      
      if (length(object) > 1)
         for (j in 2:length(object))
         {
            if (is.call(object[[j]]))
            {
               .rs.doShinyServerCompletions(object[[j]],
                                            inputs,
                                            outputs,
                                            inputCount,
                                            outputCount)
            }
         }
   }
   
})

.rs.addFunction("getCompletionsShinySession", function(token)
{
   # Use cached completions if possible
   if (!is.null(results <- .rs.get("shinySessionCompletions")))
      return(results)
   
   # Get completions from shiny if available
   if (!("shiny" %in% loadedNamespaces()))
      return(NULL)
   
   # Check to see if this version of Shiny has a completions
   # function we can use
   completionGetter <- tryCatch(
      eval(call(":::", "shiny", "session_completions")),
      error = function(e) NULL
   )
   
   if (is.null(completionGetter))
      return(NULL)
   
   # Get the completions from Shiny
   completions <- tryCatch(
      completionGetter(),
      error = function(e) NULL
   )
   
   if (is.null(completions))
      return(NULL)
   
   # Ensure that this is a character vector
   if (!is.character(completions))
      return(NULL)
   
   # Return completions
   results <- .rs.selectFuzzyMatches(completions, token)
   output <- .rs.makeCompletions(token = token,
                                 results = results,
                                 type = .rs.acCompletionTypes$CONTEXT,
                                 excludeOtherCompletions = TRUE)
   
   # Cache for later use
   .rs.assign("shinySessionCompletions", output)
   
   output
   
})

.rs.addFunction("getCompletionsArgument", function(token,
                                                   activeArg,
                                                   functionCall = NULL,
                                                   envir = NULL)
{
   completions <- .rs.emptyCompletions()
   
   object <- if (!is.null(functionCall))
      .rs.resolveObjectFromFunctionCall(functionCall, envir)
   
   matchedCall <- if (!is.null(object))
      .rs.matchCall(object, functionCall)
   
   # Get completions from a data object name if the active argument is 'formula'.
   # Useful for formula incantations in e.g.
   #
   #    lm(|, data = mtcars)
   #
   # In this special case, we _override_ argument completions (since they
   # are often less interesting in this special case)
   if (.rs.startsWith(activeArg, "form") &&
       !is.null(matchedCall[["data"]]))
   {
      dataObject <- .rs.getAnywhere(matchedCall[["data"]])
      if (!is.null(dataObject))
      {
         dataNames <- .rs.getNames(dataObject)
         return(.rs.makeCompletions(
            token = token,
            results = .rs.selectFuzzyMatches(dataNames, token),
            quote = FALSE
         ))
      }
   }
      
   # Check for knitr chunk completions, if possible
   if ("knitr" %in% loadedNamespaces())
   {
      ns <- asNamespace("knitr")
      tryGet <- function(name, ns)
         tryCatch(get(name, envir = ns)$set, error = function(e) NULL)
      
      setter <- tryGet("opts_chunk", ns)
      if (identical(object, setter))
      {
         opts <- knitr:::opts_chunk_attr
         if (activeArg %in% names(opts))
         {
            # Options to fill in the various if statements
            foundCompletions <- FALSE
            results <- character()
            quote <- TRUE
            
            if (is.list(opts[[activeArg]]))
            {
               foundCompletions <- TRUE
               potentials <- unlist(opts[[activeArg]])
               results <- .rs.selectFuzzyMatches(potentials, token)
               quote <- TRUE
            }
            
            if (identical(opts[[activeArg]], "logical"))
            {
               foundCompletions <- TRUE
               potentials <- c(TRUE, FALSE)
               results <- .rs.selectFuzzyMatches(potentials, token)
               quote <- FALSE
            }
            
            if (foundCompletions)
               return(.rs.makeCompletions(token = token,
                                          type = .rs.acCompletionTypes$ARGUMENT,
                                          quote = quote,
                                          results = results))
         }
      }
   }
   
   # Get 'help', 'example' completions
   if (activeArg %in% "topic" &&
       (identical(object, utils::example) ||
        identical(object, utils::help)))
   {
      completions <- .rs.appendCompletions(
         completions,
         .rs.getCompletionsHelp(token, quote = TRUE)
      )
   }
   
   # Get package names for completions
   if (activeArg %in% c("pkg", "package"))
      completions <- .rs.appendCompletions(
         completions,
         .rs.getCompletionsPackages(token = token,
                                    appendColons = FALSE,
                                    excludeOtherCompletions = FALSE)
      )
   
   completions
})

.rs.addFunction("listInferredPackages", function(documentId)
{
   .Call("rs_listInferredPackages", documentId)
})

.rs.addFunction("getInferredCompletions", function(packages = character(),
                                                   simplify = TRUE)
{
   result <- .Call("rs_getInferredCompletions", as.character(packages))
   if (simplify && length(result) == 1)
      return(result[[1]])
   result
})

.rs.addFunction("getCompletionsLibraryContextArgumentNames", function(token,
                                                                      string,
                                                                      discardFirst,
                                                                      functionCall,
                                                                      completions)
{
   ## Figure out which package + args we actually want to use
   pkg <- NULL
   args <- NULL
   for (i in seq_along(completions))
   {
      if (string[[1]] %in% names(completions[[i]]$functions))
      {
         pkg <- names(completions)[[i]]
         args <- completions[[i]]$functions[[string[[1]]]]
         break
      }
   }
   
   # Bail if we couldn't find anything
   if (is.null(pkg))
      return(.rs.emptyCompletions())
   
   ## Make a dummy function to match a call against
   argsText <- paste(args, collapse = ", ")
   dummyFunction <- tryCatch(
      suppressWarnings(
         eval(parse(text = paste("function(", argsText, ") {}", sep = "")))
      ), error = function(e) NULL
   )
   if (is.null(dummyFunction))
      return(.rs.emptyCompletions())
   
   matchedCall <- .rs.matchCall(dummyFunction, functionCall)
   if (is.null(matchedCall))
      return(.rs.emptyCompletions())
   
   formals <- .rs.resolveFormals(token,
                                 dummyFunction,
                                 string[[1]],
                                 functionCall,
                                 matchedCall,
                                 envir)
   
   # Protect against failure
   if (is.null(formals))
      return(.rs.emptyCompletions())
   
   results <- .rs.selectFuzzyMatches(formals$formals, token)
   if (length(results))
      results <- paste(results, "= ")
   
   if (discardFirst && length(results))
      results <- results[-1]
   
   .rs.makeCompletions(token = token,
                       results = results,
                       packages = paste(pkg, string[[1]], sep = "|||"),
                       type = .rs.acCompletionTypes$ARGUMENT,
                       fguess = string[[1]])
})

.rs.addFunction("getCompletionsLibraryContext", function(token,
                                                         string,
                                                         type,
                                                         numCommas,
                                                         functionCall,
                                                         discardFirst,
                                                         documentId,
                                                         envir)
{
   # Bail on null / empty documentId (necessary for e.g. the console)
   if (is.null(documentId) || !nzchar(documentId))
      return(.rs.emptyCompletions())
   
   ## If we detect that particular 'library' calls are in the source document,
   ## and those packages are actually available (but the package is not currently loaded),
   ## then we get an asynchronously-updated set of completions. We enocde them as 'context'
   ## completions just so the user has a small hint that, even though we provide the
   ## completions, the package isn't actually loaded.
   packages <- .rs.listInferredPackages(documentId)
   if (!length(packages))
      return(.rs.emptyCompletions())
   
   # Remove any packages that are on the search path
   searchNames <- paste("package", packages, sep = ":")
   packages <- packages[!(searchNames %in% search())]
   
   if (!length(packages))
      return(.rs.emptyCompletions())
   
   completions <- .rs.getInferredCompletions(packages, simplify = FALSE)
   results <- .rs.emptyCompletions()
   
   # If we're getting completions for a particular function's arguments,
   # use those
   if (length(type) && type[[1]] == .rs.acContextTypes$FUNCTION)
   {
      results <- .rs.appendCompletions(
         results,
         .rs.getCompletionsLibraryContextArgumentNames(token,
                                                       string,
                                                       discardFirst,
                                                       functionCall,
                                                       completions)
      )
   }
   
   # Add completions for exported items.
   results <- .rs.appendCompletions(
      results, 
      Reduce(.rs.appendCompletions, lapply(seq_along(completions), function(i) {
         
         completion <- completions[[i]]
         package <- names(completions)[[i]]
         
         keep <- .rs.fuzzyMatches(completion$exports, token)
         .rs.makeCompletions(token = token,
                             results = completion$exports[keep],
                             packages = package,
                             type = completion$types[keep])
      }))
   )
   
})

.rs.addFunction("isKnitrObject", function(object)
{
   if (!("knitr" %in% loadedNamespaces()))
      return(FALSE)
   
   ns <- asNamespace("knitr")
   if (identical(environment(object), ns))
      return(TRUE)
   
   parent <- tryCatch(
      parent.env(environment(object)),
      error = function(e) NULL
   )
   
   if (identical(parent, ns))
      return(TRUE)
   
   return(FALSE)
})

.rs.addJsonRpcHandler("transform_snippet", function(snippet)
{
   # Extract any R code from the snippet
   reRCode <- "`[Rr]\\s+[^`]+`"
   matches <- gregexpr(reRCode, snippet, perl = TRUE)[[1]]
   if (matches == -1)
      return(snippet)
   
   match.length <- attr(matches, "match.length")
   if (is.null(match.length))
      return(snippet)
   
   ranges <- lapply(seq_along(matches), function(i) {
      c(matches[[i]], matches[[i]] + match.length[[i]])
   })
   
   extracted <- lapply(ranges, function(range) {
      substring(
         snippet,
         range[[1]] + 2, # skip "`r "
         range[[2]] - 2 # leave out trailing "`"
      )
   })
   
   frame <- parent.frame()
   evaluated <- lapply(extracted, function(code) {
      tryCatch({
         captured <- capture.output(
            result <- paste(collapse = " ", as.character(suppressWarnings(
               eval(parse(text = code), envir = frame)
            )))
         )
         
         if (!length(result) && length(captured))
            paste(captured, collapse = " ")
         else
            result
         
      }, error = function(e) ""
      )
   })
   
   newSnippet <- snippet
   for (i in seq_along(evaluated))
   {
      range <- ranges[[i]]
      text <- substring(snippet, range[[1]], range[[2]] - 1)
      replacement <- evaluated[[i]]
      newSnippet <- gsub(text, replacement, newSnippet, fixed = TRUE)
   }
   
   .rs.scalar(newSnippet)
})

.rs.addFunction("isCustomCompletionsEnabled", function()
{
   completer <- utils::rc.getOption("custom.completer")
   is.function(completer)
})

.rs.addFunction("getCustomRCompletions", function(line)
{
   utils:::.assignLinebuffer(line)
   utils:::.assignEnd(nchar(line))
   token <- utils:::.guessTokenFromLine()
   utils:::.completeToken()
   results <- utils:::.retrieveCompletions()
   
   packages <- sub('^package:', '', .rs.which(results))
   
   # ensure spaces around =
   results <- sub("=$", " = ", results)
   
   choose = packages == '.GlobalEnv'
   results.sorted = c(results[choose], results[!choose])
   packages.sorted = c(packages[choose], packages[!choose])
   
   packages.sorted = sub('^\\.GlobalEnv$', '', packages.sorted)
   
   .rs.makeCompletions(
      token = token,
      results = results.sorted,
      packages = packages.sorted
   )
})
