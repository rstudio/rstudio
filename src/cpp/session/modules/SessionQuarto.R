#
# SessionQuarto.R
#
# Copyright (C) 2022 by Posit Software, PBC
#
# Unless you have received this program directly from Posit pursuant
# to the terms of a commercial license agreement with Posit, then
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
      }
   }

   return(editor)
})

.rs.addFunction("quarto.renderPreview", function(port) {
   utils::download.file(paste0("http://localhost:", port, "/quarto-render/"),
                        destfile = tempfile(),
                        quiet = TRUE,
                        cacheOK = FALSE)
})

.rs.addFunction("quarto.serveRender", function(port, path) {
   utils::download.file(paste0("http://localhost:", port, "/90B3C9E8-0DBC-4BC0-B164-AA2D5C031B28/", path),
                        destfile = tempfile(),
                        quiet = TRUE,
                        cacheOK = FALSE)
})

