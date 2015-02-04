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

.rs.setVar(
   "r.keywords",
   c(
      "TRUE",
      "FALSE",
      "NA",
      "NA_real_",
      "NA_complex_",
      "NA_integer_",
      "NA_character_",
      "NULL",
      "Inf",
      "else",
      "in"
   )
)

.rs.addFunction("setLintEngine", function(engine)
{
   if (identical(engine, "internal"))
      .rs.setVar("r.lint.engine", .rs.internalLintEngine)
   else if (!is.function(engine))
      stop("'engine' must be a function taking a single argument (file path)")
   else
      .rs.setVar("r.lint.engine", engine)
})

.rs.addFunction("getLintEngine", function()
{
   engine <- .rs.getVar("r.lint.engine")
   if (is.null(engine))
      .rs.internalLintEngine
   else
      engine
})

.rs.addFunction("lint", function(filePath)
{
   engine <- .rs.getLintEngine()
   engine(filePath)
})

.rs.addFunction("availableRSymbols", function()
{
   unlist(c(.rs.objectsOnSearchPath(), .rs.getVar("r.keywords")))
})

.rs.addFunction("internalLintEngine", function(filePath)
{
   if (!file.exists(filePath))
      return(list())
      
   filePath <- .rs.normalizePath(filePath, mustWork = TRUE)
   lint <- .rs.lintRFile(filePath)
   invisible(.rs.showLintMarkers(lint, filePath))
})

.rs.addFunction("lintRFile", function(filePath)
{
   .Call("rs_lintRFile", filePath)
})

.rs.addFunction("showLintMarkers", function(lint, filePath)
{
   markers <- .rs.createMarkersFromLint(lint, filePath)
   .rs.api.sourceMarkers(
      name = "Linter",
      markers = markers,
      basePath = .rs.getProjectDirectory()
   )
})

.rs.addFunction("createMarkersFromLint", function(lint, file) {
   lapply(lint, function(x) {
      list(
         type = x$type,
         file = file,
         line = x$start.row,
         column = x$start.column,
         message = x$message,
         messageHTML = FALSE
      )
   })
})
