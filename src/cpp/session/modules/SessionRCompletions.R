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
          UNKNOWN = 0,
          FUNCTION = 1,
          SINGLE_BRACKET = 2,
          DOUBLE_BRACKET = 3,
          NAMESPACE_EXPORTED = 4,
          NAMESPACE_ALL = 5,
          DOLLAR = 6,
          AT = 7,
          FILE = 8,
          CHUNK = 9,
          ROXYGEN = 10,
          HELP = 11
       )
)

# Sync with RCompletionTypes.java
assign(x = ".rs.acCompletionTypes",
       pos = which(search() == "tools:rstudio"),
       value = list(
          UNKNOWN = 0,
          VECTOR = 1,
          FUNCTION = 2,
          ARGUMENTS = 3,
          DATAFRAME = 4,
          LIST = 5,
          ENVIRONMENT = 6,
          S4 = 7,
          REFERENCE_CLASS = 8,
          FILE = 9,
          CHUNK = 10,
          ROXYGEN = 11,
          HELP = 12,
          STRING = 13,
          PACKAGE = 14,
          KEYWORD = 15
       )
)

.rs.addFunction("getCompletionType", function(object)
{
   if (is.function(object))
      .rs.acCompletionTypes$FUNCTION
   else if (inherits(object, "data.frame"))
      .rs.acCompletionTypes$DATAFRAME
   else if (is.list(object))
      .rs.acCompletionTypes$LIST
   else if (is.environment(object))
      .rs.acCompletionTypes$ENVIRONMENT
   else if (is.vector(object))
      .rs.acCompletionTypes$VECTOR
   else if (isS4(object))
      .rs.acCompletionTypes$S4
   else if (methods::is(object, "refClass"))
      .rs.acCompletionTypes$REFERENCE_CLASS
   else
      .rs.acCompletionTypes$UNKNOWN
})

.rs.addFunction("attemptRoxygenTagCompletion", function(token)
{
   match <- grepl("^@[a-zA-Z0-9]*$", token, perl = TRUE)
   if (!match)
      return(NULL)
   
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
      "@inheritParams ",
      "@keywords ",
      "@method ",
      "@name ",
      "@note ",
      "@noRd",
      "@param ",
      "@rdname ",
      "@references ",
      "@return ",
      "@S3method ",
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
   
   matchingTags <- grep(paste("^", tag, sep=""), tags, value = TRUE)
   
   .rs.makeCompletions(tag,
                       matchingTags,
                       type = .rs.acCompletionTypes$ROXYGEN)
})

.rs.addFunction("getCompletionsFile", function(token)
{
   slashIndices <- gregexpr("/", token, fixed = TRUE)[[1]]
   lastSlashIndex <- slashIndices[length(slashIndices)]
   
   if (lastSlashIndex == -1)
   {
      pattern <- paste("^", .rs.asCaseInsensitiveRegex(.rs.escapeForRegex(token)), sep = "")
      files <- list.files(all.files = TRUE,
                          pattern = pattern,
                          no.. =  TRUE)
   }
   else
   {
      directory <- substring(token, 1, lastSlashIndex - 1)
      file <- substring(token, lastSlashIndex + 1, nchar(token))
      pattern <- paste("^", .rs.asCaseInsensitiveRegex(.rs.escapeForRegex(file)), sep = "")
      listed <- list.files(directory,
                           all.files = TRUE,
                           pattern = pattern,
                           no.. = TRUE)
      
      startsWithLetter <- grepl("^[a-zA-Z0-9]", listed, perl = TRUE)
      first <- which(startsWithLetter)
      last <- which(!startsWithLetter)
      order <- c(first, last)
      listed <- listed[order]
      
      files <- file.path(
         directory,
         listed
      )
   }
   
   isDir <- file.info(files)[, "isdir"] %in% TRUE ## protect against NA
   files[isDir] <- paste(files[isDir], "/", sep = "")
   .rs.makeCompletions(token = token,
                       results = files,
                       packages = ifelse(isDir, "<directory>", "<file>"),
                       type = .rs.acCompletionTypes$FILE,
                       quote = FALSE,
                       excludeOtherCompletions = TRUE)
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
         s3methods <- .rs.getS3MethodsForFunction(functionName, envir)
         objectForDispatch <- .rs.getAnywhere(matchedCall[[2]], envir)
         classes <- class(objectForDispatch)
         
         for (class in c(classes, "default"))
         {
            methodName <- paste(functionName, class, sep = ".")
            method <- .rs.getAnywhere(methodName, envir)
            if (!is.null(method))
            {
               formals <- .rs.getFunctionArgumentNames(method)
               methods <- rep.int(methodName, length(formals))
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
      
      list(
         formals = formals[keep],
         methods = methods[keep]
      )
      
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

.rs.addFunction("getCompletionsFunction", function(token,
                                                   string,
                                                   functionCall,
                                                   discardFirst,
                                                   envir = parent.frame())
{
   result <- .rs.emptyCompletions()
   
   splat <- strsplit(string, ":{2,3}", perl = TRUE)[[1]]
   object <- NULL
   if (length(splat) == 1)
   {
      object <- .rs.getAnywhere(string, envir = envir)
   }
   else if (length(splat) == 2)
   {
      namespaceString <- splat[[1]]
      functionString <- splat[[2]]
      
      if (namespaceString %in% loadedNamespaces())
      {
         object <- tryCatch(
            expr = {
               get(
                  functionString,
                  envir = asNamespace(namespaceString)
               )
            },
            error = function(e) {
               NULL
            }
         )
      }
   }
   
   if (!is.null(object) && is.function(object))
   {
      matchedCall <- .rs.matchCall(object, functionCall)
      formals <- .rs.resolveFormals(token,
                                    object,
                                    string,
                                    functionCall,
                                    matchedCall,
                                    envir)
      
      if (length(formals$formals))
         formals$formals <- paste(formals$formals, "= ")
      
      fguess <- if (length(formals$methods))
         formals$methods[[1]]
      else
         ""
      
      result <- .rs.makeCompletions(
         token = token,
         results = formals$formals,
         packages = formals$methods,
         type = .rs.acCompletionTypes$ARGUMENTS,
         fguess = fguess,
         orderStartsWithAlnumFirst = FALSE
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

.rs.addFunction("getCompletionsNamespace", function(token, string, exportsOnly, envir)
{
   result <- .rs.emptyCompletions()
   
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
         objects(namespace, all.names = TRUE)
      
      completions <- .rs.selectFuzzyMatches(objectNames, token)
      objects <- mget(completions, envir = namespace)
      type <- vapply(objects, FUN.VALUE = numeric(1), USE.NAMES = FALSE, .rs.getCompletionType)
      
      result <- .rs.makeCompletions(
         token = token,
         results = completions,
         packages = string,
         quote = FALSE,
         type = type
      )
   }
   
   result
   
})


.rs.addFunction("emptyCompletions", function()
{
   .rs.makeCompletions(
      token = "",
      results = character(),
      packages = character(),
      quote = logical(),
      type = numeric(),
      fguess = "",
      excludeOtherCompletions = .rs.scalar(FALSE),
      overrideInsertParens = .rs.scalar(FALSE),
      orderStartsWithAlnumFirst = .rs.scalar(TRUE)
   )
})

.rs.addFunction("isEmptyCompletion", function(completions)
{
   length(completions$results) == 0
})

.rs.addFunction("fuzzyMatches", function(completions, token)
{
   .rs.startsWith(tolower(completions), tolower(token))
})

.rs.addFunction("selectFuzzyMatches", function(completions, token)
{
   completions[.rs.fuzzyMatches(completions, token)]
})

.rs.addFunction("formCompletionVector", function(object, default, n)
{
   if (!length(object))
      rep_len(default, n)
   else
      rep_len(object, n)
})

.rs.addFunction("makeCompletions", function(token,
                                            results,
                                            packages = character(),
                                            quote = logical(),
                                            type = numeric(),
                                            fguess = "",
                                            excludeOtherCompletions = FALSE,
                                            overrideInsertParens = FALSE,
                                            orderStartsWithAlnumFirst = TRUE)
{
   # Ensure other 'vector' completions are of the same length as 'results'
   n <- length(results)
   packages <- .rs.formCompletionVector(packages, "", n)
   quote    <- .rs.formCompletionVector(quote, FALSE, n)
   type     <- .rs.formCompletionVector(type, .rs.acCompletionTypes$UNKNOWN, n)
   
   # Favor completions starting with a letter
   if (orderStartsWithAlnumFirst)
   {
      startsWithLetter <- grepl("^[a-zA-Z0-9]", results, perl = TRUE)
      first <- which(startsWithLetter)
      last <- which(!startsWithLetter)
      order <- c(first, last)
      
      results <- results[order]
      packages <- packages[order]
      quote <- quote[order]
      type <- type[order]
   }
   
   list(token = token,
        results = results,
        packages = packages,
        quote = quote,
        type = type,
        fguess = fguess,
        excludeOtherCompletions = .rs.scalar(excludeOtherCompletions),
        overrideInsertParens = .rs.scalar(overrideInsertParens))
})

.rs.addFunction("subsetCompletions", function(completions, indices)
{
   completions$results <- completions$results[indices]
   completions$packages <- completions$packages[indices]
   completions$quote <- completions$quote[indices]
   completions$type <- completions$type[indices]
   
   completions
})

.rs.addFunction("appendCompletions", function(old, new)
{
   old$results <- c(old$results, new$results)
   old$packages <- c(old$packages, new$packages)
   old$quote <- c(old$quote, new$quote)
   old$type <- c(old$type, new$type)
   
   if (length(new$fguess) && new$fguess != "")
      old$fguess <- new$fguess
   
   if (length(new$excludeOtherCompletions) && new$excludeOtherCompletions)
      old$excludeOtherCompletions <- new$excludeOtherCompletions
   
   if (length(new$overrideInsertParens) && new$overrideInsertParens)
      old$overrideInsertParens <- new$overrideInsertParens
   
   old
})

.rs.addFunction("blackListEvaluationDataTable", function(token, string, envir)
{
   tryCatch({
      parsed <- suppressWarnings(parse(text = string))
      if (is.expression(parsed))
      {
         call <- parsed[[1]][[1]]
         if (as.character(call) == "[")
         {
            objectName <- parsed[[1]][[2]]
            object <- .rs.getAnywhere(objectName, envir = envir)
            if (inherits(object, "data.table"))
            {
               .rs.makeCompletions(
                  token = token,
                  results = names(object),
                  packages = paste("[", string, "]", sep = ""),
                  quote = FALSE,
                  type = vapply(object, FUN.VALUE = numeric(1), USE.NAMES = FALSE, .rs.getCompletionType)
               )
            }
         }
      }
      else
      {
         NULL
      }
   }, error = function(e) NULL
   )
   
})

.rs.addFunction("blackListEvaluation", function(token, string, envir)
{
   if (!is.null(result <- .rs.blackListEvaluationDataTable(token, string, envir)))
      return(result)
   
   NULL
   
})

## NOTE: for '@' as well (set with S4 bit)
.rs.addFunction("getCompletionsDollar", function(token, string, envir, isS4)
{
   result <- .rs.emptyCompletions()
   
   ## Blacklist certain evaluations
   if (!is.null(result <- .rs.blackListEvaluation(token, string, envir)))
      return(result)
   
   object <- .rs.getAnywhere(string, envir)
   if (!is.null(object))
   {
      if (isS4 && !inherits(object, "classRepresentation"))
      {
         names <- slotNames(object)
         type <- numeric(length(names))
         for (i in seq_along(names))
         {
            type[[i]] <- .rs.getCompletionType(eval(call("@", object, names[[i]])))
         }
      }
      else
      {
         names <- .rs.getNames(object)
         type <- vapply(object, FUN.VALUE = numeric(1), USE.NAMES = FALSE, .rs.getCompletionType)
      }
      
      keep <- .rs.fuzzyMatches(names, token)
      completions <- names[keep]
      type <- type[keep]
      
      result <- .rs.makeCompletions(
         token = token,
         results = completions,
         packages = paste("[", string, "]", sep = ""),
         quote = FALSE,
         type = type
      )
   }
   
   result
   
})

.rs.addFunction("getCompletionsSingleBracket", function(token,
                                                        string,
                                                        numCommas,
                                                        envir)
{
   result <- .rs.emptyCompletions()
   
   ## Blacklist certain evaluations
   if (!is.null(result <- .rs.blackListEvaluation(token, string, envir)))
      return(result)
   
   object <- .rs.getAnywhere(string, envir)
   if (is.null(object))
      return(result)
   
   completions <- character()
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
         packages = paste("[", string, "]", sep = ""),
         quote = !inherits(object, "data.table"),
         type = .rs.acCompletionTypes$STRING
      )
   }
   
   result
   
})

.rs.addFunction("getCompletionsDoubleBracket", function(token,
                                                        string,
                                                        envir = parent.frame())
{
   result <- .rs.emptyCompletions()
   
   ## Blacklist certain evaluations
   if (!is.null(result <- .rs.blackListEvaluation(token, string, envir)))
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
         packages = paste("[[", string, "]]", sep = ""),
         quote = TRUE,
         type = .rs.acCompletionTypes$STRING,
         overrideInsertParens = TRUE
      )
   }
   
   result
   
})

.rs.addFunction("getCompletionsPackages", function(token, appendColons = FALSE)
{
   allPackages <- Reduce(union, lapply(.libPaths(), list.files))
   completions <- .rs.selectFuzzyMatches(allPackages, token)
   .rs.makeCompletions(token = token,
                       results = if (appendColons && length(completions))
                          paste(completions, "::", sep = "")
                       else
                          completions,
                       packages = completions,
                       quote = !appendColons,
                       type = .rs.acCompletionTypes$PACKAGE)
})

.rs.addFunction("getCompletionsGetOption", function(token)
{
   allOptions <- names(options())
   .rs.makeCompletions(token = token,
                       results = .rs.selectFuzzyMatches(allOptions, token),
                       quote = TRUE,
                       type = .rs.acCompletionTypes$STRING,
                       excludeOtherCompletions = FALSE)
})

.rs.addFunction("getCompletionsOptions", function(token)
{
   allOptions <- names(options())
   completions <- .rs.selectFuzzyMatches(allOptions, token)
   if (length(completions))
      completions <- paste(completions, "= ")
   
   .rs.makeCompletions(token,
                       completions,
                       type = .rs.acCompletionTypes$STRING,
                       excludeOtherCompletions = TRUE)
})

.rs.addFunction("getCompletionsSearchPath", function(token, overrideInsertParens = FALSE)
{
   objects <- .rs.objectsOnSearchPath(token, TRUE)
   objects[["keywords"]] <- c(
      "NULL", "NA", "TRUE", "FALSE", "T", "F", "Inf", "NaN",
      "NA_integer_", "NA_real_", "NA_character_", "NA_complex_"
   )
   
   names <- names(objects)
   results <- unlist(objects, use.names = FALSE)
   packages <- unlist(lapply(1:length(objects), function(i) {
      rep.int(names[i], length(objects[[i]]))
   }))
   
   keep <- .rs.fuzzyMatches(results, token)
   results <- results[keep]
   packages <- packages[keep]
   
   order <- order(results)
   results <- results[order]
   packages <- packages[order]
   
   type <- vapply(seq_along(results), function(i) {
      if (packages[[i]] == "keywords")
         .rs.acCompletionTypes$KEYWORD
      else
         .rs.getCompletionType(get(results[[i]], pos = which(search() == packages[[i]])))
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
                                               functionCall)
{
   result <- tryCatch({
      wrapper <- function(x, which, exact = FALSE) {}
      matched <- .rs.matchCall(wrapper, functionCall)
      if (is.null(matched[["x"]]))
         return(.rs.emptyCompletions())
      objectName <- as.character(matched[["x"]])
      object <- .rs.getAnywhere(objectName)
      completions <- .rs.selectFuzzyMatches(
         names(attributes(object)),
         token
      )
      .rs.makeCompletions(token,
                          completions,
                          paste("attributes(", objectName, ")", sep = ""),
                          quote = TRUE,
                          type = .rs.acCompletionTypes$STRING)
   }, error = function(e) .rs.emptyCompletions())
   result
})


utils:::rc.settings(files = TRUE)
.rs.addJsonRpcHandler("get_completions", function(token,
                                                  string,
                                                  type,
                                                  numCommas,
                                                  functionCallString,
                                                  chainObjectName,
                                                  additionalArgs,
                                                  excludeArgs,
                                                  excludeArgsFromObject)
{
   ## NOTE: these are passed in as lists of strings; convert to character
   additionalArgs <- as.character(additionalArgs)
   excludeArgs <- as.character(excludeArgs)
   
   ## Try to parse the function call string
   functionCall <- tryCatch({
      parse(text = .rs.finishExpression(functionCallString))[[1]]
   }, error = function(e) 
      NULL
   )
   
   ## Handle some special cases early
   
   # help
   if (.rs.acContextTypes$HELP %in% type)
      return(.rs.getCompletionsHelp(token))
   
   # Roxygen
   if (.rs.acContextTypes$ROXYGEN %in% type)
      return(.rs.attemptRoxygenTagCompletion(token))
   
   # No information on completions other than token
   if (!length(string))
   {
      # If there was no token, give up
      if (token == "")
         return(.rs.emptyCompletions())
      
      # Otherwise, complete from the seach path + available packages
      return(.rs.appendCompletions(
         .rs.getCompletionsSearchPath(token),
         .rs.getCompletionsPackages(token, TRUE)
      ))
   }
   
   # library, require, requireNamespace, loadNamespace
   if (string[[1]] %in% c("library", "require", "requireNamespace") &&
          numCommas[[1]] == 0)
   {
      return(.rs.getCompletionsPackages(token))
   }
   
   ## Other special cases (but we may still want completions from
   ## other contexts)
   
   # attr
   completions <- if (string[[1]] == "attr")
   {
      .rs.getCompletionsAttr(token, functionCall)
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
      completions <- .rs.emptyCompletions()
   }
   
   for (i in seq_along(string))
   {
      discardFirst <- type[[i]] == .rs.acContextTypes$FUNCTION && chainObjectName != ""
      completions <- .rs.appendCompletions(
         completions,
         .rs.getRCompletions(token,
                             string[[i]],
                             type[[i]],
                             numCommas[[i]],
                             functionCall,
                             discardFirst,
                             parent.frame())
      )
   }
   
   if (token != "" && 
          type[[1]] %in% c(.rs.acContextTypes$UNKNOWN, .rs.acContextTypes$FUNCTION,
                           .rs.acContextTypes$SINGLE_BRACKET, .rs.acContextTypes$DOUBLE_BRACKET))
      completions <- .rs.appendCompletions(
         completions,
         .rs.getCompletionsSearchPath(token)
      )
   
   ## File-based completions
   if (.rs.acContextTypes$FILE %in% type)
      completions <- .rs.appendCompletions(
         completions,
         .rs.getCompletionsFile(token)
      )
   
   ## Package completions (e.g. `stats::`)
   if (token != "" && length(type) == 1 && type == .rs.acContextTypes$UNKNOWN)
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
                               parent.frame())
   )
   
   ## Override param insertion if the function was 'debug' or 'trace'
   for (i in seq_along(type))
   {
      if (type[[i]] %in% c(.rs.acContextTypes$FUNCTION, .rs.acContextTypes$UNKNOWN))
      {
         ## Blacklist certain functions
         if (string[[i]] %in% c("help", "str"))
         {
            completions$overrideInsertParens <- .rs.scalar(TRUE)
         }
         else
         {
            ## Blacklist based on formals of the function
            object <- .rs.getAnywhere(string[[i]], parent.frame())
            if (is.function(object))
            {
               argNames <- .rs.getFunctionArgumentNames(object)
               if (any(c("f", "fun", "func") %in% tolower(gsub("[^a-zA-Z]", "", argNames))))
                  completions$overrideInsertParens <- .rs.scalar(TRUE)
            }
         }
      }
   }
   
   completions$token <- token
   if (is.null(completions$fguess))
      completions$fguess <- ""
   
   completions$excludeOtherCompletions <- .rs.scalar(type[[1]] %in% c(
      .rs.acContextTypes$DOLLAR,
      .rs.acContextTypes$AT,
      .rs.acContextTypes$NAMESPACE_EXPORTED,
      .rs.acContextTypes$NAMESPACE_ALL
   ))
   
   if (is.null(completions$quote))
      completions$quote <- logical(length(completions$results))
   
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
      parent.frame()
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
            result <- .rs.makeCompletions(
               token = token,
               results = completions,
               packages = paste("[", chainObjectName, "]", sep = ""),
               quote = FALSE,
               type = vapply(object[completions], FUN.VALUE = numeric(1), USE.NAMES = FALSE, .rs.getCompletionType)
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
                                            envir)
{
   if (type %in% c(.rs.acContextTypes$DOLLAR, .rs.acContextTypes$AT))
      .rs.getCompletionsDollar(token, string, envir, type == .rs.acContextTypes$AT)
   else if (type %in% c(.rs.acContextTypes$NAMESPACE_EXPORTED, .rs.acContextTypes$NAMESPACE_ALL))
      .rs.getCompletionsNamespace(token, string, type == .rs.acContextTypes$NAMESPACE_EXPORTED, envir)
   else if (type == .rs.acContextTypes$FUNCTION)
      .rs.getCompletionsFunction(token, string, functionCall, discardFirst, envir)
   else if (type == .rs.acContextTypes$SINGLE_BRACKET)
      .rs.getCompletionsSingleBracket(token, string, numCommas, envir)
   else if (type == .rs.acContextTypes$DOUBLE_BRACKET)
      .rs.getCompletionsDoubleBracket(token, string, envir)
   else
      .rs.emptyCompletions()
   
})

## NOTE: This is a modified version of 'matchAvailableTopics'
## in 'completions.R' of the R sources.
.rs.addFunction("getCompletionsHelp", function(token)
{
   pkgCacheName <- ".completions.attachedPackagesCache"
   helpTopicsName <- ".completions.helpTopics"
   rsEnvPos <- which(search() == "tools:rstudio")
   
   attachedPackagesCache <- tryCatch(
      get(pkgCacheName, pos = rsEnvPos),
      error = function(e) character()
   )
   
   paths <- searchpaths()[substring(search(), 1, 8) == "package:"]
   if (!identical(basename(paths), attachedPackagesCache))
   {
      assign(pkgCacheName,
             basename(paths),
             pos = rsEnvPos)
      
      assign(helpTopicsName,
             unique(unlist(lapply(paths, .rs.readAliases))),
             pos = rsEnvPos)
   }
   
   aliases <- get(helpTopicsName, pos = rsEnvPos)
   completions <- .rs.selectFuzzyMatches(aliases, token)
   
   .rs.makeCompletions(
      token = token,
      results = completions,
      quote = grepl("[^a-zA-Z0-9._]", completions, perl = TRUE),
      type = .rs.acCompletionTypes$HELP,
      overrideInsertParens = TRUE
   )
   
})

.rs.addFunction("readAliases", function(path)
{
   if (file.exists(f <- file.path(path, "help", "aliases.rds")))
      names(readRDS(f))
   else
      character()
})
