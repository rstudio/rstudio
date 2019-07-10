#
# SessionReticulate.R
#
# Copyright (C) 2009-18 by RStudio, Inc.
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

.rs.setVar("python.moduleCache", new.env(parent = emptyenv()))

.rs.addJsonRpcHandler("python_get_completions", function(line, ctx)
{
   if (!requireNamespace("reticulate", quietly = TRUE))
      return(.rs.emptyCompletions())
   
   completions <- .rs.tryCatch(.rs.python.getCompletions(line, ctx))
   if (inherits(completions, "error"))
      return(.rs.emptyCompletions(language = "Python"))
   
   .rs.makeCompletions(
      token       = attr(completions, "token"),
      results     = as.character(completions),
      type        = attr(completions, "types"),
      packages    = attr(completions, "source"),
      quote       = FALSE,
      helpHandler = "reticulate:::help_handler",
      language    = "Python"
   )
})

.rs.addJsonRpcHandler("python_go_to_definition", function(line, offset)
{
   # extract the line providing the object definition we're looking for
   text <- .rs.python.extractCurrentExpression(line, offset)
   if (!nzchar(text))
      return(FALSE)
   
   # try extracting this object
   object <- .rs.tryCatch(reticulate::py_eval(text, convert = FALSE))
   if (inherits(object, "error"))
      return(FALSE)
   
   # TODO: use object explorer to view modules? or navigate
   # to their module path? or something similar?
   if (inherits(object, "python.builtin.module"))
      return(FALSE)
   
   # check to see if 'inspect' can find the object sources
   inspect <- reticulate::import("inspect", convert = TRUE)
   info <- .rs.tryCatch(
      list(
         source = inspect$getsourcefile(object),
         line = inspect$findsource(object)[[2]]
      )
   )
   
   if (inherits(info, "error"))
      return(FALSE)
   
   .rs.api.navigateToFile(info$source, info$line + 1L, 1L)
   return(TRUE)
   
})

.rs.addJsonRpcHandler("python_go_to_help", function(line, offset)
{
   text <- .rs.python.extractCurrentExpression(line, offset)
   if (!nzchar(text))
      return(FALSE)
   
   .Call("rs_showPythonHelp", text, PACKAGE = "(embedding)")
   return(TRUE)
})

.rs.addFunction("reticulate.initialize", function()
{
   # try to default to the tkAgg backend (note that we'll
   # force a vanilla 'agg' backend when required otherwise)
   engine <- tolower(Sys.getenv("MPLENGINE"))
   if (engine %in% c("", "qt5agg"))
      Sys.setenv(MPLENGINE = "tkAgg")
})

.rs.addFunction("reticulate.matplotlib.pyplot.loadHook", function(plt)
{
   .rs.setVar("reticulate.matplotlib.show", plt$show)
   plt$show <- .rs.reticulate.matplotlib.showHook
})

.rs.addFunction("reticulate.matplotlib.showHook", function(...)
{
   # read device size
   size <- dev.size(units = "in")
   width <- size[1]; height <- size[2]
   
   # TODO: handle high-DPI displays
   dpi <- 72L
   
   # TODO: get device requested from matplotlib?
   # TODO: handle HTML content?
   path <- tempfile("matplotlib-plot-", fileext = ".png")
   
   plt <- reticulate::import("matplotlib.pyplot", convert = TRUE)
   
   # resize the figure
   figure <- plt$gcf()
   figure$set_dpi(dpi)
   figure$set_size_inches(width, height)
   plt$savefig(path, dpi = figure$dpi)
   
   # now, read that in and create an R plot using e.g. image
   data <- png::readPNG(path, native = TRUE, info = TRUE)
   
   # don't display margins and ensure R doesn't nudge margin size
   opar <- par(
      xaxt = "n", yaxt = "n",
      xaxs = "i", yaxs = "i",
      mar = c(0, 0, 0, 0),
      oma = c(0, 0, 0, 0),
      xpd = NA
   )
   on.exit(par(opar), add = TRUE)
   
   # generate raster
   plot.new()
   graphics::rasterImage(data, 0, 0, 1, 1)
})

.rs.addFunction("reticulate.replInitialize", function()
{
   builtins <- reticulate::import_builtins(convert = FALSE)
   
   # override help method (Python's interactive help does
   # not play well with RStudio)
   help <- builtins$help
   .rs.setVar("reticulate.help", builtins$help)
   builtins$help <- function(...) {
      dots <- list(...)
      if (length(dots) == 0) {
         message("Error: Interactive Python help not available within RStudio")
         return()
      }
      help(...)
   }
   
   # install matplotlib hook if available
   if (requireNamespace("png", quietly = TRUE) &&
       reticulate::py_module_available("matplotlib"))
   {
      matplotlib <- reticulate::import("matplotlib", convert = TRUE)
      
      # force the "Agg" backend (this is necessary as other backends may
      # fail with RStudio if requisite libraries are not available)
      backend <- matplotlib$get_backend()
      if (!identical(tolower(backend), "agg"))
      {
         sys <- reticulate::import("sys", convert = TRUE)
         if ("matplotlib.backends" %in% names(sys$modules))
            matplotlib$pyplot$switch_backend("agg")
         else
            matplotlib$use("agg", warn = FALSE, force = TRUE)
      }
      
      # inject our hook
      plt <- matplotlib$pyplot
      .rs.setVar("reticulate.matplotlib.show", plt$show)
      plt$show <- .rs.reticulate.matplotlib.showHook
   }
   
})

.rs.addFunction("reticulate.replHook", function(buffer, contents, trimmed)
{
   # special handling for commands when buffer is currently empty
   if (buffer$empty())
   {
      # detect help requests, and route to Help pane
      if (grepl("^[?]", trimmed))
      {
         text <- substring(trimmed, 2)
         .Call("rs_showPythonHelp", text, PACKAGE = "(embedding)")
         return(TRUE)
      }
      
      reHelp <- "help\\((.*)\\)"
      if (grepl(reHelp, trimmed))
      {
         text <- gsub(reHelp, "\\1", trimmed)
         .Call("rs_showPythonHelp", text, PACKAGE = "(embedding)")
         return(TRUE)
      }
   }
   
   FALSE
})


.rs.addFunction("reticulate.replTeardown", function()
{
   # restore old help method
   builtins <- reticulate::import_builtins(convert = FALSE)
   builtins$help <- .rs.getVar("reticulate.help")
   
   # restore matplotlib method
   show <- .rs.getVar("reticulate.matplotlib.show")
   if (!is.null(show)) {
      matplotlib <- reticulate::import("matplotlib", convert = TRUE)
      plt <- matplotlib$pyplot
      plt$show <- show
   }
   
})

.rs.addFunction("reticulate.replIsActive", function()
{
   if (.rs.isBrowserActive())
      return(FALSE)
   
   if (!"reticulate" %in% loadedNamespaces())
      return(FALSE)
   
   active <- tryCatch(reticulate:::py_repl_active(), error = identity)
   if (inherits(active, "error"))
      return(FALSE)
   
   active
})

options(reticulate.repl.initialize = .rs.reticulate.replInitialize)
options(reticulate.repl.hook       = .rs.reticulate.replHook)
options(reticulate.repl.teardown   = .rs.reticulate.replTeardown)

.rs.addFunction("python.tokenizationRules", function() {
   
   list(
      
      list(
         pattern = sprintf("(?:%s)\\b", paste(.rs.python.keywords(), collapse = "|")),
         type    = "keyword"
      ),
      
      list(
         pattern = "[[:alpha:]_][[:alnum:]_]*\\b",
         type    = "identifier"
      ),
      
      list(
         pattern = "((\\d+[jJ]|((\\d+\\.\\d*|\\.\\d+)([eE][-+]?\\d+)?|\\d+[eE][-+]?\\d+)[jJ])|((\\d+\\.\\d*|\\.\\d+)([eE][-+]?\\d+)?|\\d+[eE][-+]?\\d+)|(0[xX][\\da-fA-F]+[lL]?|0[bB][01]+[lL]?|(0[oO][0-7]+)|(0[0-7]*)[lL]?|[1-9]\\d*[lL]?))\\b",
         type    = "number"
      ),
      
      list(
         pattern = '["]{3}(.*?)(?:["]{3}|$)',
         type    = "string"
      ),
      
      list(
         pattern = "[']{3}(.*?)(?:[']{3}|$)",
         type    = "string"
      ),
      
      list(
         pattern = '["](?:(?:\\\\.)|(?:[^"\\\\]))*?(?:["]|$)',
         type    = "string"
      ),
      
      list(
         pattern = "['](?:(?:\\\\.)|(?:[^'\\\\]))*?(?:[']|$)",
         type    = "string"
      ),
      
      list(
         pattern = "\\*\\*=?|>>=?|<<=?|<>|!+|//=?|[%&|^=<>*/+-]=?|~",
         type    = "operator"
      ),
      
      list(
         pattern = "[:;.,`@]",
         type    = "special"
      ),
      
      list(
         pattern = "[][)(}{]",
         type    = "bracket"
      ),
      
      list(
         pattern = "#[^\n]*",
         type    = "comment"
      ),
      
      list(
         pattern = "[[:space:]]+",
         type    = "whitespace"
      )
      
   )
   
})

.rs.addFunction("python.token", function(value, type, offset)
{
   list(value = value, type = type, offset = offset)
})

.rs.addFunction("python.tokenize", function(
   code,
   exclude = character(),
   keep.unknown = TRUE)
{
   # vector of tokens
   tokens <- list()
   
   # rules to use
   rules <- .rs.python.tokenizationRules()
   
   # convert to raw vector so we can use 'grepRaw',
   # which supports offset-based search
   raw <- charToRaw(code)
   n <- length(raw)
   
   # record current offset
   offset <- 1
   
   while (offset <= n) {
      
      # record whether we successfully matched a rule
      matched <- FALSE
      
      # iterate through rules, looking for a match
      for (rule in rules) {
         
         # augment pattern to search only from start of requested offset
         pattern <- paste("^(?:", rule$pattern, ")", sep = "")
         match <- grepRaw(pattern, raw, offset = offset, value = TRUE)
         if (length(match) == 0)
            next
         
         # we found a match; record that
         matched <- TRUE
         
         # update our vector of tokens
         token <- .rs.python.token(rawToChar(match), rule$type, offset)
         if (!token$type %in% exclude)
            tokens[[length(tokens) + 1]] <- token
         
         # update offset and break
         offset <- offset + length(match)
         break
         
      }
      
      # if we failed to match anything, consume a single character
      if (!matched) {
         # update tokens
         token <- .rs.python.token(rawToChar(raw[[offset]]), "unknown", offset)
         if (keep.unknown)
            tokens[[length(tokens) + 1]] <- token
         
         # update offset
         offset <- offset + 1
      }
      
   }
   
   class(tokens) <- "tokens"
   tokens
   
})

.rs.addFunction("python.tokenCursor", function(tokens)
{
   .tokens <- tokens
   .offset <- 1L
   .n <- length(tokens)
   
   .lbrackets <- c("(", "{", "[")
   .rbrackets <- c(")", "}", "]")
   .complements <- list(
      "(" = ")", "[" = "]", "{" = "}",
      ")" = "(", "]" = "[", "}" = "{"
   )
   
   tokenValue   <- function() { .tokens[[.offset]]$value  }
   tokenType    <- function() { .tokens[[.offset]]$type   }
   tokenOffset  <- function() { .tokens[[.offset]]$offset }
   cursorOffset <- function() { .offset                   }
   
   moveToOffset <- function(offset) {
      if (offset < 1L)
         .offset <<- 1L
      else if (offset > .n)
         .offset <<- .n
      else
         .offset <<- offset
   }
   
   moveToNextToken <- function(i = 1L) {
      offset <- .offset + i
      if (offset > .n)
         return(FALSE)
      
      .offset <<- offset
      return(TRUE)
   }
   
   moveToPreviousToken <- function(i = 1L) {
      offset <- .offset - i
      if (offset < 1L)
         return(FALSE)
      
      .offset <<- offset
      return(TRUE)
   }
   
   moveRelative <- function(i = 1L) {
      offset <- .offset + i
      if (offset < 1L || offset > .n)
         return(FALSE)
      
      .offset <<- offset
      return(TRUE)
   }
   
   fwdToMatchingBracket <- function() {
      
      token <- .tokens[[.offset]]
      value <- token$value
      if (!value %in% .lbrackets)
         return(FALSE)
      
      lhs <- value
      rhs <- .complements[[lhs]]
      
      count <- 1
      while (moveToNextToken()) {
         value <- tokenValue()
         if (value == lhs) {
            count <- count + 1
         } else if (value == rhs) {
            count <- count - 1
            if (count == 0)
               return(TRUE)
         }
      }
      
      return(FALSE)
   }
   
   bwdToMatchingBracket <- function() {
      
      token <- .tokens[[.offset]]
      value <- token$value
      if (!value %in% .rbrackets)
         return(FALSE)
      
      lhs <- value
      rhs <- .complements[[lhs]]
      
      count <- 1
      while (moveToPreviousToken()) {
         value <- tokenValue()
         if (value == lhs) {
            count <- count + 1
         } else if (value == rhs) {
            count <- count - 1
            if (count == 0)
               return(TRUE)
         }
      }
      
      return(FALSE)
   }
   
   peek <- function(i = 0L) {
      offset <- .offset + i
      if (offset < 1L || offset > .n)
         return(.rs.python.token("", "unknown", -1L))
      return(.tokens[[offset]])
   }
   
   find <- function(predicate, forward = TRUE) {
      if (forward) {
         offset <- .offset + 1L
         while (offset <= .n) {
            token <- .tokens[[offset]]
            if (predicate(token)) {
               .offset <<- offset
               return(TRUE)
            }
            offset <- offset + 1L
         }
         return(FALSE)
      } else {
         offset <- .offset - 1L
         while (offset >= 1L) {
            token <- .tokens[[offset]]
            if (predicate(token)) {
               .offset <<- offset
               return(TRUE)
            }
            offset <- offset - 1L
         }
         return(FALSE)
      }
   }
   
   # move to the start of a Python statement, e.g.
   #
   #    alpha.beta["gamma"]
   #    ^~~~~~~~~<~~~~~~~~^
   #
   moveToStartOfEvaluation <- function() {
      
      repeat {
         
         # skip matching brackets
         if (bwdToMatchingBracket()) {
            if (!moveToPreviousToken())
               return(TRUE)
            next
         }
         
         # if the previous token is an identifier or a '.', move on to it
         previous <- peek(-1L)
         if (previous$value %in% "." || previous$type %in% "identifier") {
            moveToPreviousToken()
            next
         }
         
         break
         
      }
      
      TRUE
   }
   
   list(
      tokenValue              = tokenValue,
      tokenType               = tokenType,
      tokenOffset             = tokenOffset,
      cursorOffset            = cursorOffset,
      moveToNextToken         = moveToNextToken,
      moveToPreviousToken     = moveToPreviousToken,
      fwdToMatchingBracket    = fwdToMatchingBracket,
      bwdToMatchingBracket    = bwdToMatchingBracket,
      moveToOffset            = moveToOffset,
      moveRelative            = moveRelative,
      peek                    = peek,
      find                    = find,
      moveToStartOfEvaluation = moveToStartOfEvaluation
   )
})

.rs.addFunction("python.completions", function(token,
                                               candidates,
                                               source = NULL,
                                               type = NULL,
                                               reorder = TRUE)
{
   # figure out the completions to keep
   pattern <- paste("^\\Q", token, "\\E", sep = "")
   indices <- grep(pattern, candidates, perl = TRUE)
   if (reorder)
      indices <- indices[order(candidates[indices])]
   
   # extract our completions
   completions <- candidates[indices]
   
   # re-order source and type if they were provided
   if (!is.null(source) && length(source) == length(candidates))
      source <- source[indices]
   
   if (!is.null(type) && length(type) == length(candidates))
      type <- type[indices]
   
   attr(completions, "token") <- token
   attr(completions, "source") <- source
   attr(completions, "types") <- type
   attr(completions, "helpHandler") <- "reticulate:::help_handler"
   
   completions
})

.rs.addFunction("python.emptyCompletions", function()
{
   character()
})

.rs.addFunction("python.getCompletionsImports", function(token)
{
   # split into pieces (note that strsplit drops an empty final match
   # so we need to add it back if the token is e.g. 'a.b.')
   pieces <- strsplit(token, ".", fixed = TRUE)[[1]]
   if (grepl("[.]$", token))
      pieces <- c(pieces, "")
   
   # no '.' implies we're completing top-level modules
   if (length(pieces) < 2) {
      completions <- .rs.python.listModules()
      return(.rs.python.completions(token, completions))
   }
   
   # we're completing a sub-module. try to import that module, and
   # then list things we can import from that module. note that importing
   # a module does imply running a load of Python code but other Python
   # front-ends (e.g. IPython) do this as well.
   module <- paste(head(pieces, n = -1), collapse = ".")
   imported <- tryCatch(reticulate::import(module), error = identity)
   if (inherits(imported, "error"))
      return(.rs.emptyCompletions())
   exports <- sort(unique(names(imported)))
   
   postfix <- pieces[length(pieces)]
   completions <- .rs.python.completions(postfix, exports)
   
   # now, bring back full prefix for completions
   if (length(completions)) {
      prefix <- paste(pieces[-length(pieces)], collapse = ".")
      completions <- paste(prefix, completions, sep = ".")
   }
   
   # add in metadata
   attr(completions, "token") <- token
   attr(completions, "types") <- 21
   
   completions
})

.rs.addFunction("python.getCompletionsImportsFrom", function(module, token)
{
   # request completions as though this were <module>.<token>
   pasted <- paste(module, token, sep = ".")
   completions <- .rs.python.getCompletionsImports(pasted)
   
   # fix up the completions (remove the module prefix)
   if (length(completions)) {
      prefix <- paste(module, ".", sep = "")
      completions <- sub(prefix, "", completions, fixed = TRUE)
   }
   
   attr(completions, "token") <- token
   completions
})

.rs.addFunction("python.getCompletionsFiles", function(token)
{
   os <- reticulate::import("os", convert = TRUE)
   token <- gsub("^['\"]|['\"]$", "", token)
   expanded <- path.expand(token)
   
   # find the index of the last slash -- everything following is
   # the completion token; everything before is the directory to
   # search for completions in
   indices <- gregexpr("/", expanded, fixed = TRUE)[[1]]
   if (!identical(c(indices), -1L)) {
      lhs <- substring(expanded, 1, tail(indices, n = 1))
      rhs <- substring(expanded, tail(indices, n = 1) + 1)
      files <- paste(lhs, list.files(lhs), sep = "")
   } else {
      lhs <- "."
      rhs <- expanded
      files <- list.files(os$getcwd())
   }
   
   # form completions (but add extra metadata after)
   completions <- .rs.python.completions(expanded, files)
   attr(completions, "token") <- token
   
   info <- file.info(completions)
   attr(completions, "types") <- ifelse(info$isdir, 16, 15)
   
   completions
})

.rs.addFunction("python.getCompletionsKeys", function(source, token)
{
   builtins <- reticulate::import_builtins(convert = TRUE)
   
   object <- tryCatch(reticulate::py_eval(source, convert = FALSE), error = identity)
   if (inherits(object, "error"))
      return(.rs.python.emptyCompletions())
   
   method <- reticulate::py_get_attr(object, "keys", silent = TRUE)
   if (!inherits(method, "python.builtin.object"))
      return(.rs.python.emptyCompletions())
   
   keys <- reticulate::py_to_r(method)
   candidates <- if (.rs.python.isPython3())
      as.character(builtins$list(reticulate::py_to_r(keys())))
   else
      reticulate::py_to_r(keys())
   
   .rs.python.completions(token, candidates)
   
})

.rs.addFunction("python.getCompletionsArguments", function(source, token)
{
   object <- tryCatch(reticulate::py_eval(source, convert = FALSE), error = identity)
   if (inherits(object, "error"))
      return(.rs.python.emptyCompletions())
   
   arguments <- .rs.python.getFunctionArguments(object)
   
   # paste on an '=' for completions (Python users seem to prefer no
   # spaces between the argument name and value)
   .rs.python.completions(
      token = token,
      candidates = paste(arguments, "=", sep = ""),
      source = source,
      type = .rs.acCompletionTypes$ARGUMENT,
      reorder = FALSE
   )
   
})

.rs.addFunction("python.getFunctionArguments", function(object)
{
   inspect <- reticulate::import("inspect", convert = TRUE)
   
   # for class objects, we'll look up arguments on the associated
   # __init__ method instead
   if (inspect$isclass(object)) {
      
      init <- .rs.tryCatch(reticulate::py_get_attr(object, "__init__"))
      if (inherits(init, "error"))
         return(.rs.python.emptyCompletions())
      
      arguments <- .rs.tryCatch(inspect$getargspec(init)$args)
      if (inherits(arguments, "error"))
         return(.rs.python.emptyCompletions())
      
      return(setdiff(arguments, "self"))
   }
   
   # try a set of methods for extracting these arguments
   methods <- list(
      function() inspect$getargspec(object)$args,
      function() .rs.python.getNumpyFunctionArguments(object)
   )
   
   for (method in methods) {
      arguments <- .rs.tryCatch(method())
      if (!inherits(arguments, "error"))
         return(arguments)
   }
   
   character()
   
})

.rs.addFunction("python.getNumpyFunctionArguments", function(object)
{
   # extract the docstring
   docs <- reticulate::py_get_attr(object, "__doc__")
   if (inherits(docs, "python.builtin.object"))
      docs <- reticulate::py_to_r(docs)
   
   pieces <- strsplit(docs, "\n", fixed = TRUE)[[1]]
   first <- pieces[[1]]
   
   # try munging so that it 'looks' like an R function definition,
   # and then parse it that way. this will obviously fail for certain
   # kinds of Python default arguments but this seems to catch the
   # most common cases for now
   munged <- paste(gsub("[^(]*[(]", "function (", first), "{}")
   parsed <- parse(text = munged)[[1]]
   
   # extract the formal names
   names(parsed[[2]])
})

.rs.addFunction("python.getCompletionsMain", function(token, ctx)
{
   dots <- gregexpr(".", token, fixed = TRUE)[[1]]
   if (identical(c(dots), -1L)) {
      
      # provide completions for main, builtins, keywords
      main     <- reticulate::import_main(convert = FALSE)
      builtins <- reticulate::import_builtins(convert = FALSE)
      
      # figure out object types for main, builtins
      keywords <- .rs.python.keywords()
      candidates <- c(names(main), names(builtins), keywords)
      
      source <- c(
         rep("reticulate:::import_main(convert = FALSE)", length(names(main))),
         rep("reticulate:::import_builtins(convert = FALSE)", length(names(builtins))),
         rep("", length(keywords))
      )
      
      # figure out object types
      type <- c(
         .rs.python.inferObjectTypes(main, names(main)),
         .rs.python.inferObjectTypes(builtins, names(builtins)),
         rep(.rs.acCompletionTypes$KEYWORD, length(keywords))
      )
      
      completions <- .rs.python.completions(
         token = token,
         candidates = candidates,
         source = source,
         type = type
      )
      
      return(completions)
   }
   
   # we had dots; try to evaluate each component piece-by-piece to get
   # to the relevant object providing us with completions
   pieces <- .rs.strsplit(token, ".", fixed = TRUE)
   
   # for the first piece, check to see if it might be a module
   first <- .rs.python.sanitizeForCompletion(pieces[[1]])
   object <- if (first %in% names(ctx$aliases))
   {
      module <- ctx$aliases[[first]]
      .rs.tryCatch(reticulate::import(module, convert = FALSE))
   }
   else
   {
      reticulate::py_eval(first, convert = FALSE)
   }
   
   # now, try to extract sub-pieces from the module / object we received
   i <- 2
   while (i < length(pieces))
   {
      if (inherits(object, "error"))
         break
      
      code <- .rs.python.sanitizeForCompletion(pieces[[i]])
      object <- .rs.tryCatch(reticulate::py_get_attr(object, code))
      i <- i + 1
   }
   if (inherits(object, "error"))
      return(.rs.python.emptyCompletions())
   
   # attempt to get completions
   candidates <- tryCatch(reticulate::py_list_attributes(object), error = identity)
   if (inherits(candidates, "error"))
      return(.rs.python.emptyCompletions())
   
   completions <- .rs.python.completions(
      token      = tail(pieces, n = 1L),
      candidates = candidates,
      source     = head(pieces, n = -1L),
      type       = .rs.python.inferObjectTypes(object, candidates)
   )
   
   completions
})

.rs.addFunction("python.getCompletions", function(line, ctx)
{
   # check for completion of a module name in e.g. 'import nu' or 'from nu'
   re_import <- paste(
      "^[[:space:]]*",      # leading whitespace
      "(?:from|import)",    # from or import
      "[[:space:]]+",       # separating spaces
      "([[:alnum:]._]*)$",  # module name
      sep = ""
   )
   
   matches <- regmatches(line, regexec(re_import, line, perl = TRUE))[[1]]
   if (length(matches) == 2)
      return(.rs.python.getCompletionsImports(matches[[2]]))
   
   # check for completion of submodule
   re_import_from <- paste(
      "^[[:space:]]*",     # leading space
      "from",              # 'from'
      "[[:space:]]+",      # separating spaces
      "([[:alnum:]._]+)",  # module name
      "[[:space:]]+",      # separating spaces
      "import",            # 'import'
      "[[:space:]]+",      # separating spaces
      "\\(?",              # an optional opening bracket (tuple style)
      "[[:space:]]*",      # optional whitespace
      "([^)]*)",           # the rest (including whitespace)
      sep = ""
   )
   
   matches <- regmatches(line, regexec(re_import_from, line, perl = TRUE))[[1]]
   if (length(matches) == 3) {
      
      # extract module from which imports are being drawn
      module <- matches[[2]]
      imports <- matches[[3]]
      
      # figure out the text following the last comma (if any)
      token <- ""
      if (nzchar(imports))
         token <- gsub(".*[[:space:],]", "", imports)
      
      return(.rs.python.getCompletionsImportsFrom(module, token))
      
   }
   
   # tokenize the line and grab the last token
   tokens <- .rs.python.tokenize(
      code = line,
      exclude = c("whitespace", "comment"),
      keep.unknown = FALSE
   )
   
   if (length(tokens) == 0)
      return(.rs.python.emptyCompletions())
   
   # construct token cursor
   cursor <- .rs.python.tokenCursor(tokens)
   cursor$moveToOffset(length(tokens))
   token <- cursor$peek()
   
   # for strings, we may be either completing dictionary keys or files
   if (token$type %in% "string") {
      
      # if there's no prior token, assume this is a file name
      if (!cursor$moveToPreviousToken())
         return(.rs.python.getCompletionsFiles(token$value))
      
      # if the prior token is an open bracket, assume we're completing
      # a dictionary key
      if (cursor$tokenValue() == "[") {
         
         saved <- cursor$peek()
         
         if (!cursor$moveToPreviousToken())
            return(.rs.python.emptyCompletions())
         
         if (!cursor$moveToStartOfEvaluation())
            return(.rs.python.emptyCompletions())
         
         # grab text from this offset
         lhs <- substring(line, cursor$tokenOffset(), saved$offset - 1)
         rhs <- gsub("^['\"]|['\"]$", "", token$value)
         
         # bail if there are any '(' tokens (avoid arbitrary function eval)
         # in theory this screens out tuples but that's okay for now
         tokens <- .rs.python.tokenize(lhs)
         lparen <- Find(function(token) token$value == "(", tokens)
         if (!is.null(lparen))
            return(.rs.python.emptyCompletions())
         
         return(.rs.python.getCompletionsKeys(lhs, rhs))
         
      }
      
      # doesn't look like a dictionary; perform filesystem completion
      return(.rs.python.getCompletionsFiles(token$value))
      
   }
   
   # try to guess if we're trying to autocomplete function arguments
   maybe_function <-
      cursor$peek(0 )$value %in% c("(", ",") ||
      cursor$peek(-1)$value %in% c("(", ",")
   
   if (maybe_function) {
      offset <- cursor$cursorOffset()
      
      # try to find an opening bracket
      repeat {
         
         # skip matching brackets
         if (cursor$bwdToMatchingBracket()) {
            if (!cursor$moveToPreviousToken())
               break
            next
         }
         
         # if we find an opening bracket, check to see if the token to the
         # left is something that is, or could produce, a function
         if (cursor$tokenValue() == "(" &&
             cursor$moveToPreviousToken() &&
             (cursor$tokenValue() == "]" || cursor$tokenType() %in% "identifier"))
         {
            # find code to be evaluted that will produce function
            endToken   <- cursor$peek()
            cursor$moveToStartOfEvaluation()
            startToken <- cursor$peek()
            
            # extract the associated text
            start <- startToken$offset
            end   <- endToken$offset + nchar(endToken$value) - 1
            source <- substring(line, start, end)
            
            # get argument completions
            rhs <- if (token$type %in% "identifier") token$value else ""
            return(.rs.python.getCompletionsArguments(source, rhs))
         }
         
         if (!cursor$moveToPreviousToken())
            break
      }
      
      # if we got here, our attempts to find a function failed, so
      # go home and fall back to the default completion solution
      cursor$moveToOffset(offset)
   }
   
   # start looking backwards
   repeat {
      
      # skip matching brackets
      if (cursor$bwdToMatchingBracket()) {
         if (!cursor$moveToPreviousToken())
            break
         next
      }
      
      # consume identifiers, strings, '.'
      if (cursor$tokenType() %in% c("string", "identifier") ||
          cursor$tokenValue() %in% ".")
      {
         lastType <- cursor$tokenType()
         
         # if we can't move to the previous token, we must be at the
         # start of the token stream, so just consume from here
         if (!cursor$moveToPreviousToken())
            break
         
         # if we moved on to a token of the same type, move back and break
         if (lastType == cursor$tokenType()) {
            cursor$moveToNextToken()
            break
         }
         
         next
      }
      
      # if this isn't a matched token, then move back up a single
      # token and break
      if (!cursor$moveToNextToken())
         return(.rs.python.emptyCompletions())
      
      break
      
   }
   
   source <- substring(line, cursor$tokenOffset())
   .rs.python.getCompletionsMain(source, ctx)
})

.rs.addFunction("python.isPython3", function()
{
   config <- reticulate::py_config()
   grepl("^3", config$version)
})

.rs.addFunction("python.listModules", function()
{
   pkgutil  <- reticulate::import("pkgutil", convert = FALSE)
   builtins <- reticulate::import_builtins(convert = FALSE)
   
   modules <- tryCatch(
      builtins$list(pkgutil$iter_modules()),
      error = identity
   )
   
   if (inherits(modules, "error"))
      return(character())
   
   # convert to R object and extract module names
   modules <- reticulate::py_to_r(modules)
   key <- if (.rs.python.isPython3()) "name" else 2L
   names <- vapply(modules, `[[`, key, FUN.VALUE = character(1))
   sort(unique(names))
})

.rs.addFunction("python.inferObjectTypes", function(object, names)
{
   vapply(names, function(name) {
      
      # attempt to grab attribute (note that this can fail if
      # the object as implemented a custom __getattr__ or similar)
      item <- .rs.tryCatch(reticulate::py_get_attr(object, name))
      if (inherits(item, "error"))
         return(.rs.acCompletionTypes$UNKNOWN)
      
      # try to infer the completion type
      if (inherits(item, "python.builtin.module"))
         .rs.acCompletionTypes$ENVIRONMENT
      else if (inherits(item, "python.builtin.builtin_function_or_method") ||
               inherits(item, "python.builtin.function") ||
               inherits(item, "python.builtin.instancemethod"))
         .rs.acCompletionTypes$FUNCTION
      else if (inherits(item, "pandas.core.frame.DataFrame"))
         .rs.acCompletionTypes$DATAFRAME
      else
         .rs.acCompletionTypes$UNKNOWN
   }, numeric(1))
})

.rs.addFunction("python.generateHtmlHelp", function(code)
{
   Encoding(code) <- "UTF-8"
   
   # remove a '.html' suffix if present
   code <- sub("[.]html$", "", code)
   
   # check for pre-existing generated HTML
   dir <- file.path(tempdir(), "reticulate-python-help")
   if (!.rs.ensureDirectory(dir)) {
      warning("Failed to create Python help directory", call. = FALSE)
      return("")
   }
   
   stem <- utils::URLencode(code, reserved = TRUE)
   path <- file.path(dir, paste(stem, "html", sep = "."))
   if (file.exists(path))
      return(path)
   
   # no HTML file exists; attempt to generate it. try
   # to evaluate the Python code supplied to gain access
   # to the associated object. first attempt to just py_eval
   # it; if that fails, try the more generic pydoc resolver
   pydoc <- reticulate::import("pydoc", convert = TRUE)
   methods <- list(
      function() reticulate::py_eval(code),
      function() pydoc$resolve(code)[[1]]
   )
   
   resolved <- NULL
   for (method in methods) {
      resolved <- .rs.tryCatch(method())
      if (!inherits(resolved, "error"))
         break
   }
   
   if (inherits(resolved, "error")) {
      fmt <- "No Python documentation found for '%s'."
      warning(sprintf(fmt, code), call. = FALSE)
      return("")
   }
   
   # the text provided by the user is likely an alias for
   # the true definition location, so attempt to recover
   # that from the object.
   #
   # TODO: should we maintain our cache of help topics
   # within a session state directory, so they can be
   # reloaded when RStudio is restarted?
   module <- "<unknown>"
   if (reticulate::py_has_attr(resolved, "__module__"))
      module <- resolved[["__module__"]]
   
   name <- "<unknown>"
   if (reticulate::py_has_attr(resolved, "__name__"))
      name <- resolved[["__name__"]]
   
   # if we don't know the module, try to guess based on the class
   if (identical(module, "<unknown>") &&
       reticulate::py_has_attr(resolved, "__class__"))
   {
      builtins <- reticulate::import_builtins(convert = TRUE)
      output <- builtins$repr(resolved[["__class__"]])
      if (grepl("<type '(.*)'>", output))
      {
         class <- gsub("<type '(.*)'>", "\\1", output)
         splat <- .rs.strsplit(class, ".", fixed = TRUE)
         module <- paste(head(splat, n = 1), collapse = ".")
      }
   }
   
   # we have a Python object: generate HTML help for it. we
   # monkey-patch our own HTMLDoc instance so that the heading
   # comes out a little more cleanly
   #
   # TODO: this might fit more naturally as a helper class
   # in the reticulate package
   pydoc <- reticulate::import("pydoc", convert = TRUE)
   objects <- reticulate::py_run_string("

# Create HTML documentation object
import pydoc
html = pydoc.HTMLDoc()

# Override the heading function
def _heading(title, fgcol, bgcol, extra = ''):
   return '''
<table width=\"100%%\" cellspacing=0 cellpadding=2 border=0 summary=\"heading\">
<tr><td><h2>%s</h2></td></tr>
</table>
   ''' % (title)

html.heading = _heading
", local = TRUE)
   
   html <- objects$html
   
   if (inherits(resolved, "numpy.ufunc"))
      page <- html$page(paste("numpy function", name), html$docroutine(resolved, name))
   else
      page <- html$page(pydoc$describe(resolved), html$document(resolved, name))
   
   # remove hard-coded background colors for rows
   page <- gsub("\\s?bgcolor=\"#[0-9a-fA-F]{6}\"", "", page, perl = TRUE)
   
   writeLines(page, con = path)
   path
   
})

.rs.addFunction("python.extractCurrentExpression", function(line, offset)
{
   # tokenize the line
   tokens <- .rs.python.tokenize(line, exclude = c("whitespace", "comment"))
   if (length(tokens) == 0)
      return("")
   
   # find the current token
   n <- length(tokens); index <- n
   while (index >= 1) {
      if (tokens[[index]]$offset <= offset)
         break
      index <- index - 1
   }
   
   cursor <- .rs.python.tokenCursor(tokens)
   cursor$moveToOffset(index)
   
   # try to move to the start of an expression
   while (TRUE) {
      
      # move over matching brackets
      while (cursor$bwdToMatchingBracket())
         if (!cursor$moveToPreviousToken())
            break
      
      # bail if we hit an operator or a ';' -- these
      # are tokens that 'stop' a previous expression
      if (cursor$tokenType() %in% c("operator", "keyword") ||
          cursor$tokenValue() %in% c(";", ","))
      {
         cursor$moveToNextToken()
         break
      }
      
      # if we hit an opening bracket, check to see
      # if there's an identifier before it. if not,
      # this is defining a tuple or a list, and we
      # should bail
      if (cursor$tokenType() %in% "bracket" &&
          cursor$cursorOffset() > 1)
      {
         peek <- tokens[[cursor$cursorOffset() - 1]]
         if (peek$type %in% c("operator", "bracket", "keyword") ||
             peek$value %in% c(";", ","))
         {
            cursor$moveToNextToken()
            break
         }
      }
      
      # move back a token
      if (!cursor$moveToPreviousToken())
         break
   }
   
   startOffset <- cursor$tokenOffset()
   
   # now find the end of the expression
   while (TRUE) {
      
      # bail if we hit a '('
      if (cursor$tokenValue() %in% c("(")) {
         cursor$moveToPreviousToken()
         break
      }
      
      # skip other brackets
      if (cursor$fwdToMatchingBracket())
         if (!cursor$moveToNextToken())
            break
      
      # bail if we hit an operator or a ';' -- these
      # are tokens that 'stop' a previous expression
      if (cursor$tokenType() %in% c("operator", "keyword") ||
          cursor$tokenValue() %in% c(";", ","))
      {
         cursor$moveToPreviousToken()
         break
      }
      
      # move up a token
      if (!cursor$moveToNextToken())
         break
   }
   
   endOffset <- cursor$tokenOffset() + nchar(cursor$tokenValue()) - 1
   
   # extract line of text providing the object to be looked at
   substring(line, startOffset, endOffset)
   
})

.rs.addFunction("python.keywords", function()
{
   keywords <- .rs.getVar("python.keywordList")
   if (length(keywords))
      return(keywords)
   
   keyword <- reticulate::import("keyword", convert = TRUE)
   kwlist <- keyword$kwlist
   
   .rs.setVar("python.keywordList", kwlist)
   kwlist
})

# $ title      : chr "DataFrame"
# $ signature  : chr "DataFrame()"
# $ description: chr "Two-dimensional size-mutable, potentially heterogeneous tabular data"
.rs.addFunction("python.getHelp", function(topic, source)
{
   object <- .rs.tryCatch(reticulate::py_eval(source, convert = FALSE))
   if (inherits(object, "error"))
      return(NULL)
   
   handler <- reticulate:::help_completion_handler.python.builtin.object
   .rs.tryCatch(reticulate::py_suppress_warnings(handler(topic, object)))
})

# $ args            : chr [1:9] "cls" "path" "header" "sep" ...
# $ arg_descriptions: Named chr [1:9] "cls" "path" "header" "sep" ...
.rs.addFunction("python.getParameterHelp", function(source)
{
   error <- list(args = character(), arg_descriptions = character())
   object <- .rs.tryCatch(reticulate::py_eval(source, convert = FALSE))
   if (inherits(object, "error"))
      return(error)
   
   # extract argument names using inspect (note that this can fail for
   # some Python function types; e.g. builtin Python functions)
   inspect <- reticulate::import("inspect", convert = TRUE)
   spec <- .rs.tryCatch(inspect$getargspec(object))
   if (inherits(spec, "error"))
      return(error)
   
   args <- spec$args
   
   # attempt to scrape parameter documentation
   docs <- reticulate::py_get_attr(object, "__doc__", silent = TRUE)
   if (inherits(docs, "python.builtin.object"))
      docs <- reticulate::py_to_r(docs)
   if (is.null(docs))
      docs <- ""
   
   lines <- strsplit(docs, "\n", fixed = TRUE)[[1]]
   
   arg_descriptions <- lapply(args, function(arg) {
      tryCatch({
         # try to find the line where the parameter documentation starts
         pattern <- sprintf("^\\s*%s\\s*:", arg)
         index <- grep(pattern, lines)
         if (!length(index))
            return("")
         index <- index[[1]]
         line <- lines[[index]]
         
         # split into argument name, initial part of description
         desc <- ""
         colon <- regexpr(":", line, fixed = TRUE)
         if (colon != -1L)
            desc <- substring(line, colon + 1)
         
         # now, look to see if the documentation spans multiple lines
         # consume lines of greater indent that the current
         indent <- regexpr("(?:\\S|$)", line)
         start <- end <- index + 1
         while (TRUE) {
            if (regexpr("(?:\\S|$)", lines[[end]]) <= indent)
               break
            end <- end + 1
         }
         
         if (start != end)
            desc <- c(desc, lines[start:(end - 1L)])
         
         paste(gsub("^\\s*|\\s*$", "", desc), collapse = "\n")
         
      }, error = function(e) "")
   })
   
   list(
      args             = as.character(args),
      arg_descriptions = as.character(arg_descriptions)
   )
})

.rs.addFunction("python.sanitizeForCompletion", function(item)
{
   if (.rs.startsWith(item, "[") && .rs.endsWith(item, "]"))
      "[]"
   else if (.rs.startsWith(item, "{") && .rs.endsWith(item, "}"))
      "{}"
   else if (.rs.startsWith(item, "(") && .rs.endsWith(item, ")"))
      "()"
   else
      item
})
