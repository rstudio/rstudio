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
   invisible(engine(filePath))
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

.rs.addFunction("extractRCodeFromRMarkdownDocument", function(content)
{
   splat <- strsplit(content, "\n", fixed = TRUE)[[1]]
   starts <- grep("^\\s*[`]{3}{r.*}\\s*$", splat, perl = TRUE)
   ends <- grep("^\\s*[`]{3}\\s*$", splat, perl = TRUE)
   
   if (length(starts) != length(ends))
      return(.rs.scalar(""))
   
   new <- character(length(splat))
   for (i in seq_along(starts))
   {
      start <- starts[i]
      end <- ends[i]
      
      # Ignore pairs that include 'engine=', assuming they're non-R chunks
      if (grepl("engine\\s*=", splat[[start]], perl = TRUE))
         next
      
      new[(start + 1):(end - 1)] <- splat[(start + 1):(end - 1)]
   }
   
   .rs.scalar(paste(new, collapse = "\n"))
})



