#
# SessionDiagnostics.R
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
   unlist(c(
      .rs.objectsOnSearchPath(excludeGlobalEnv = TRUE),
      .rs.getVar("r.keywords")
   ))
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
      name = "Diagnostics",
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

.rs.addFunction("extractRCode", function(content,
                                         reStart,
                                         reEnd)
{
   # This function should be called with content that is already known
   # to be UTF-8 encoded; however, that encoding can be lost when forming
   # this call so bring the encoding back.
   Encoding(content) <- "UTF-8"
   
   splat <- strsplit(content, "\n", fixed = TRUE)[[1]]
   starts <- grep(reStart, splat, perl = TRUE)
   ends <- grep(reEnd, splat, perl = TRUE)
   
   # Get start/end pairs
   pairs <- lapply(starts, function(i) {
      following <- ends[ends > i]
      if (!length(following)) return(NULL)
      c(i, following[[1]])
   })
   
   # Drop NULL
   pairs[unlist(lapply(pairs, is.null))] <- NULL
   
   new <- character(length(splat))
   for (pair in pairs)
   {
      start <- pair[[1]]
      end <- pair[[2]]
      
      # Ignore pairs that include 'eval = FALSE'.
      if (grepl("eval\\s*=\\s*F", splat[[start]], perl = TRUE))
         next
      
      # Ignore pairs that include 'engine=', assuming they're non-R chunks.
      #
      # 'Rscript' chunks would work standalone and hence the linter would not
      # properly understand that it should discard the parse tree generated
      # from prior chunks, so we just don't lint it.
      if (grepl("engine\\s*=", splat[[start]], perl = TRUE))
         next
      
      # If the chunk end lies immediately after the chunk start, bail
      if (start + 1 == end)
         next
      
      new[(start + 1):(end - 1)] <- splat[(start + 1):(end - 1)]
   }
   
   .rs.scalar(paste(new, collapse = "\n"))
})

.rs.addFunction("lintDirectory", function(directory = .rs.getProjectDirectory())
{
   .Call("rs_lintDirectory", directory)
})

.rs.addJsonRpcHandler("analyze_project", function(directory = .rs.getProjectDirectory())
{
   .rs.lintDirectory(directory)
})
