#
# SessionLinter.R
#
# Copyright (C) 2015 by RStudio, Inc.
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

.rs.addFunction("makeLintObject", function(rowStart,
                                           rowEnd = rowStart,
                                           columnStart,
                                           columnEnd = columnStart,
                                           text,
                                           type)
{
   validTypes <- c("info", "warning", "error")
   if (length(type) != 1 ||
       !(type %in% validTypes))
      stop("Invalid lint type '", type, "'; must be one of ",
           paste(shQuote(validTypes), collapse = ", "))
   
   text <- paste(text, collapse = "\n")
   
   list(
      rowStart = rowStart,
      rowEnd = rowEnd,
      columnStart = columnStart,
      columnEnd = columnEnd,
      text = text,
      type = type
   )
   
})

.rs.addFunction("setLintEngine", function(engine)
{
   if (identical(engine, "internal"))
      .rs.setVar("r.lint.engine", .rs.internalLinter)
})

.rs.addFunction("getLintEngine", function()
{
   .rs.getVar("r.lint.engine")
})

.rs.addFunction("getLint", function(filePath)
{
   engine <- .rs.getLintEngine()
   engine(filePath)
})

.rs.addFunction("internalLinter", function(filePath)
{
   
   if (!file.exists(filePath))
      return(list())
   
   parsed <- tryCatch(
      parse(file = filePath),
      error = function(e) return(e)
   )
   
   if (inherits(parsed, "simpleError"))
   {
      splat <- strsplit(parsed$message, "\n", perl = TRUE)[[1]]
      row <- as.integer(gsub(":.*", "", splat[[2]]))
      column <- c(regexpr("^", splat[[3]], fixed = TRUE))
      text <- gsub(".*:\\s*", "", splat[[1]])
      return(list(.rs.makeLintObject(
         rowStart = row,
         columnStart = column,
         text = text,
         type = "error"
      )))
   }
   
   # Construct a scope tree from the parse tree -- ie, closure scopes
   parsed <- parse(text = "a <- 1; c(function(d) a + .__nothing__. + d + 1, b, c)")
   root <- new.env(parent = emptyenv())
   availableSymbols <- unlist(.rs.objectsOnSearchPath())
   .rs.populateLintTree(parsed, root, availableSymbols)
   
})

.rs.addFunction("getVariablesInScope", function(node) {
   
   vars <- character()
   empty <- emptyenv()
   
   parent <- node
   while (!identical(parent, empty))
   {
      vars <- c(vars, parent$local.variables)
      if (length(parent$arguments))
         vars <- c(vars, names(parent$arguments))
      parent <- parent.env(parent)
   }
   
   vars
})

.rs.addFunction("populateLintTree", function(object, node, availableSymbols)
{
   if (is.expression(object))
      lapply(object, function(x) .rs.populateLintTree(x, node, availableSymbols))
   
   if (is.symbol(object))
   {
      symbolName <- as.character(object)
      if (!(symbolName %in% availableSymbols) &&
          !(symbolName %in% .rs.getVariablesInScope(node)))
      {
         node$missing.references <- c(node$missing.references, symbolName)
      }
   }
   
   if (is.call(object)) {
      
      srcref <- attr(object, "srcref")
      
      callName <- as.character(object[[1]])[[1]]
      if (callName %in% c("<-", "=", "<<-"))
         node$local.variables <- c(node$local.variables, as.character(object[[2]]))
      
      if (callName %in% c("::", ":::"))
      {
         pkgName <- as.character(object[[2]])
         symbolName <- as.character(object[[3]])
         
         if (pkgName %in% loadedNamespaces())
         {
            pkgSymbols <- ls(asNamespace(pkgName), all.names = TRUE)
            if (!(symbolName %in% pkgSymbols))
               node$missing.references <- c(node$missing.references, symbolName)
         }
      }
      
      if (callName == "function")
      {
         newChild <- new.env(parent = node)
         newChild$arguments <- object[[2]]
         node$children <- c(node$children, newChild)
         return(.rs.populateLintTree(object[[3]], newChild, availableSymbols))
      }
      
      lapply(object, function(x) .rs.populateLintTree(x, node, availableSymbols))
      
   }
   
   
})
