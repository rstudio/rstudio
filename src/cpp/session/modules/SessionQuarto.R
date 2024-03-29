#
# SessionQuarto.R
#
# Copyright (C) 2022 by Posit Software, PBC
#
# Unless you have received this program directly from Posit Software pursuant
# to the terms of a commercial license agreement with Posit Software, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

.rs.addFunction("quarto.servePort", function() {
   if (requireNamespace("quarto", quietly = TRUE)) {
      if (!is.null(quarto:::quarto$serve_ps) && quarto:::quarto$serve_ps$is_alive()) {
         if (is.numeric(quarto:::quarto$serve_port)) {
            quarto:::quarto$serve_port
         } else {
            0
         }
      } else {
         0
      }
   } else {
      0
   }
})

.rs.addFunction("quarto.editorConfig", function(editor) {
   scalarChar <- function(x) .rs.scalar(as.character(x))
   if (!is.null(editor$mode)) {
      editor$mode <- scalarChar(editor$mode)
   }
   
   if (is.list(editor$markdown)) {
      if (!is.null(editor$markdown$wrap)) {
         editor$markdown$wrap <- scalarChar(editor$markdown$wrap)
      }
      if (!is.null(editor$markdown$canonical)) {
         editor$markdown$canonical <- scalarChar(editor$markdown$canonical)
      }
      if (is.list(editor$markdown$references)) {
         if (!is.null(editor$markdown$references$location)) {
            editor$markdown$references$location <- scalarChar(editor$markdown$references$location)
         }
         if (!is.null(editor$markdown$references$prefix)) {
            editor$markdown$references$prefix <- scalarChar(editor$markdown$references$prefix)
         }
         if (!is.null(editor$markdown$references$links)) {
            editor$markdown$references$links <- .rs.scalar(as.logical(editor$markdown$references$links))
         }
      }
   }

   return(editor)
})

.rs.addFunction("quarto.renderPreview", function(port, token, path, format) {
   .rs.tryCatch(utils::download.file(
      paste0("http://localhost:", port, "/", token,
            "/?path=", utils::URLencode(path, TRUE),
            ifelse(nzchar(format), paste0("&format=", format), "")),
      destfile = tempfile(),
      quiet = TRUE,
      cacheOK = FALSE
   ))
})


.rs.addFunction("quarto.terminatePreview", function(port) {
   token <- "4231F431-58D3-4320-9713-994558E4CC45"
   .rs.tryCatch(utils::download.file(
      paste0("http://localhost:", port, "/", token),
      destfile = tempfile(),
      quiet = TRUE,
      cacheOK = FALSE
   ))
})


