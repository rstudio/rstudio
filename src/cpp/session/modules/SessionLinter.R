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
      row.start = rowStart,
      row.end = rowEnd,
      column.start = columnStart,
      column.end = columnEnd,
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
   engine <- .rs.getVar("r.lint.engine")
   if (is.null(engine))
      .rs.internalLinter
   else
      engine
})

.rs.addFunction("lint", function(filePath)
{
   engine <- .rs.getLintEngine()
   engine(filePath)
})

.rs.addFunction("internalLinter", function(filePath)
{
   if (!file.exists(filePath))
      return(list())
      
   filePath <- .rs.normalizePath(filePath)
   objects <- unlist(.rs.objectsOnSearchPath())
   lint <- .Call("rs_parseAndLintRFile", filePath, objects)
   markers <- .rs.createMarkersFromLint(lint, filePath)
   .rs.api.sourceMarkers(
      name = "internal.linter",
      markers = markers,
      basePath = .rs.getProjectDirectory()
   )
})

.rs.addFunction("createMarkersFromLint", function(lint, file) {
   markers <- lapply(lint, function(x) {
      list(
         type = x$type,
         file = file,
         line = x$start.row,
         column = x$start.column,
         message = x$message
      )
   })
   markers
})
