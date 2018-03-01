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

.rs.addJsonRpcHandler("python_get_completions", function(line)
{
   if (!requireNamespace("reticulate", quietly = TRUE))
      return(.rs.emptyCompletions())
   
   completions <- tryCatch(
      reticulate:::py_completer(line),
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
