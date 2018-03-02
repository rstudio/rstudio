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

.rs.addJsonRpcHandler("python_get_completions", function(line)
{
   if (!requireNamespace("reticulate", quietly = TRUE))
      return(.rs.emptyCompletions())
   
   completions <- tryCatch(
      .rs.python.getCompletions(line),
      error = identity
   )
   
   if (inherits(completions, "error"))
      return(.rs.emptyCompletions())
   
   .rs.makeCompletions(
      token     = attr(completions, "token"),
      results   = as.character(completions),
      type      = attr(completions, "types"),
      quote     = FALSE
   )
})

.rs.addFunction("reticulate.replInitialize", function()
{
   # override help method
   builtins <- reticulate::import_builtins(convert = FALSE)
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
})

.rs.addFunction("reticulate.replHook", function(buffer, contents, trimmed)
{
   FALSE
})


.rs.addFunction("reticulate.replTeardown", function()
{
   # restore old help method
   builtins <- reticulate::import_builtins(convert = FALSE)
   builtins$help <- .rs.getVar("reticulate.help")
})

options(reticulate.repl.initialize = .rs.reticulate.replInitialize)
options(reticulate.repl.hook       = .rs.reticulate.replHook)
options(reticulate.repl.teardown   = .rs.reticulate.replTeardown)

.rs.addFunction("python.tokenizationRules", function() {
   
   list(
      
      list(
         pattern = "[[:alpha:]_][[:alnum:]_]*",
         type    = "identifier"
      ),
      
      list(
         pattern = "((\\d+[jJ]|((\\d+\\.\\d*|\\.\\d+)([eE][-+]?\\d+)?|\\d+[eE][-+]?\\d+)[jJ])|((\\d+\\.\\d*|\\.\\d+)([eE][-+]?\\d+)?|\\d+[eE][-+]?\\d+)|(0[xX][\\da-fA-F]+[lL]?|0[bB][01]+[lL]?|(0[oO][0-7]+)|(0[0-7]*)[lL]?|[1-9]\\d*[lL]?))",
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
   exclude = function(token) FALSE,
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
         if (!exclude(token))
            tokens[[length(tokens) + 1]] <- token
         
         # update offset and break
         offset <- offset + length(match)
         break
         
      }
      
      # if we failed to match anything, consume a single character
      if (!matched) {
         # update tokens
         token <- .rs.python.token(rawToChar(raw[[offset]]), "unknown", offset)
         if (keep.unknown && !exclude(token))
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

.rs.addFunction("python.completions", function(token, candidates)
{
   pattern <- paste("^\\Q", token, "\\E", sep = "")
   completions <- sort(grep(pattern, candidates, perl = TRUE, value = TRUE))
   attr(completions, "token") <- token
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
   module <- pieces[[1]]
   
   # no '.' implies we're completing top-level modules
   if (length(pieces) == 1) {
      completions <- .rs.python.listModules()
      return(.rs.python.completions(token, completions))
   }
   
   # we're completing a sub-module. first, find the sub module path,
   # and then attempt to construct the path to the sub module we're
   # looking inside
   imp <- reticulate::import("imp", convert = FALSE)
   info <- tryCatch(imp$find_module(module), error = identity)
   if (inherits(info, "error"))
      return(.rs.python.emptyCompletions())
   
   # construct sub-module path
   path <- reticulate::py_to_r(info[[1]])
   for (component in pieces[-c(1, length(pieces))])
      path <- file.path(path, component)
   
   # list modules in this path
   postfix <- pieces[length(pieces)]
   modules <- .rs.python.listModules(path)
   completions <- .rs.python.completions(postfix, modules)
   
   # now, bring back full prefix for completions
   if (length(completions)) {
      prefix <- paste(pieces[-length(pieces)], collapse = ".")
      completions <- paste(prefix, completions, sep = ".")
   }
   
   # add in metadata
   attr(completions, "token") <- token
   attr(completions, "type") <- 21
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

.rs.addFunction("python.getCompletionsKeys", function(object, token)
{
   method <- reticulate::py_get_attr(object, "keys", silent = TRUE)
   if (!inherits(method, "python.builtin.object"))
      return(.rs.python.emptyCompletions())
   
   keys <- reticulate::py_to_r(method)
   candidates <- reticulate::py_to_r(keys())
   .rs.python.completions(token, candidates)
})

.rs.addFunction("python.getCompletionsArguments", function(object, token)
{
   inspect <- reticulate::import("inspect", convert = TRUE)
   arguments <- inspect$getargspec(object)$args
   
   # paste on an '=' for completions (Python users seem to prefer no
   # spaces between the argument name and value)
   completions <- paste(arguments, "=", sep = "")
   
   .rs.python.completions(token, completions)
})

.rs.addFunction("python.getCompletionsMain", function(token)
{
   dots <- gregexpr(".", token, fixed = TRUE)[[1]]
   if (identical(c(dots), -1L)) {
      # no dots; just try to complete objects from the main module
      main     <- reticulate::import_main(convert = TRUE)
      builtins <- reticulate::import_builtins(convert = TRUE)
      keyword  <- reticulate::import("keyword", convert = TRUE)
      
      candidates <- c(names(main), names(builtins), keyword$kwlist)
      return(.rs.python.completions(token, candidates))
   }
   
   # we had dots; try to evaluate the left-hand side of the dots
   # and then filter on the attributes of the object (if any)
   last <- tail(dots, n = 1)
   lhs <- substring(token, 1, last - 1)
   rhs <- substring(token, last + 1)
   
   # try evaluating the left-hand side
   object <- tryCatch(reticulate::py_eval(lhs, convert = FALSE), error = identity)
   if (inherits(object, "error"))
      return(.rs.python.emptyCompletions())
   
   # attempt to get completions
   candidates <- tryCatch(reticulate::py_list_attributes(object), error = identity)
   if (inherits(candidates, "error"))
      return(.rs.python.emptyCompletions())
   
   # R readline completion, and older versions of RStudio,
   # require us to keep the '.'s (newer versions of RStudio will
   # trim the '.' and display the completions more neatly)
   rhs <- paste(lhs, rhs, sep = ".")
   candidates <- paste(lhs, candidates, sep = ".")
   
   # similar motivation requires us to trim quotations, if we're trying
   # to e.g. complete methods on a string literal
   rhs <- gsub(".*['\"]", "", rhs)
   candidates <- gsub(".*['\"]", "", candidates)
   .rs.python.completions(rhs, candidates)
})

.rs.addFunction("python.getCompletions", function(line)
{
   # check for completion of a module name in e.g. 'import nu' or 'from nu'
   re_import <- paste(
      "^[[:space:]]*",      # leading whitespace
      "(?:from|import)",    # from or import
      "[[:space:]]+",       # separating spaces
      "([[:alnum:]._]+)$",  # module name
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
      "(.*)",              # the rest
      sep = ""
   )
   
   matches <- regmatches(line, regexec(re_import_from, line, perl = TRUE))[[1]]
   if (length(matches) == 3) {
      
      # extract module from which imports are being drawn
      module <- matches[[2]]
      imports <- matches[[3]]
      
      # figure out the text following the last comma
      pieces <- strsplit(imports, ",[[:space:]]*")[[1]]
      if (grepl(",$", imports))
         pieces <- c(pieces, "")
      
      token <- pieces[[length(pieces)]]
      return(.rs.python.getCompletionsImportsFrom(module, token))
      
   }
   
   # tokenize the line and grab the last token
   tokens <- .rs.python.tokenize(
      code = line,
      exclude = function(token) { token$type %in% c("whitespace", "comment") },
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
         
         # attempt to evaluate left-hand side
         evaluated <- tryCatch(reticulate::py_eval(lhs, convert = FALSE), error = identity)
         if (inherits(evaluated, "error"))
            return(.rs.python.emptyCompletions())
         
         return(.rs.python.getCompletionsKeys(evaluated, rhs))
         
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
               return(.rs.python.getCompletionsNone())
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
            text <- substring(line, start, end)
            
            # attempt to evaluate it
            object <- tryCatch(reticulate::py_eval(text, convert = FALSE), error = identity)
            if (inherits(object, "error"))
               break
            
            # success! get argument completions
            rhs <- if (token$type %in% "identifier") token$value else ""
            return(.rs.python.getCompletionsArguments(object, rhs))
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
            return(.rs.python.emptyCompletions())
         next
      }
      
      # consume identifiers, strings, '.'
      if (cursor$tokenType() %in% c("string", "identifier") ||
          cursor$tokenValue() %in% ".")
      {
         # if we can't move to the previous token, we must be at the
         # start of the token stream, so just consume from here
         if (!cursor$moveToPreviousToken())
            break
         next
      }
      
      # if this isn't a matched token, then move back up a single
      # token and break
      if (!cursor$moveToNextToken())
         return(.rs.python.emptyCompletions())
      
      break
      
   }
   
   text <- substring(line, cursor$tokenOffset())
   .rs.python.getCompletionsMain(text)
})

.rs.addFunction("python.listModules", function(path = NULL)
{
   # construct key from provided path
   key <- if (is.null(path)) "<top-level>" else path
   
   # use cached modules if available
   if (!is.null(.rs.python.moduleCache[[key]]))
      return(.rs.python.moduleCache[[key]])
   
   sys      <- reticulate::import("sys", convert = TRUE)
   pkgutil  <- reticulate::import("pkgutil", convert = FALSE)
   builtins <- reticulate::import_builtins(convert = FALSE)
   
   # attempt to list top-level modules. NOTE: iter_modules() expects its
   # input as an array, except when NULL (None)
   if (!is.null(path))
      path <- as.list(path)
   
   modules <- tryCatch(
      builtins$list(pkgutil$iter_modules(path)),
      error = identity
   )
   
   if (inherits(modules, "error"))
      return(character())
   
   # convert to R object and extract module names
   modules <- reticulate::py_to_r(modules)
   names <- vapply(modules, `[[`, 2, FUN.VALUE = character(1))
   
   # if we're completing top-level modules, include builtin modules
   if (is.null(path))
      names <- c(names, as.character(sys$builtin_module_names))
   
   # sort and uniquify
   all <- sort(unique(names))
   
   # cache for quick lookup
   .rs.python.moduleCache[[key]] <- all
   
   all
   
})
