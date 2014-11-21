#
# SessionData.R
#
# Copyright (C) 2009-12 by RStudio, Inc.
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

.rs.addFunction( "formatDataColumn", function(x, start, len, ...)
{
   # extract the visible part of the column
   col <- x[start:min(length(x), start+len)]

   if (is.numeric(col)) {
     # show numbers as doubles
     storage.mode(col) <- "double"
   } else {
     # show everything else as characters
     col <- as.character(col)
   }
   format(col, trim = TRUE, justify = "none", ...)
})

.rs.addFunction( "formatRowNames", function(x, start, len) 
{
  rownames <- row.names(x)
  rownames[start:min(length(rownames), start+len)]
})

.rs.addFunction( "applySort", function(x, col, dir) 
{
  if (identical(dir, "desc")) {
    x[order(-x[,col]),]
  } else {
    x[order(x[,col]),]
  }
})


.rs.addFunction("findOwningEnv", function(name, env = parent.frame()) {
   while (environmentName(env) != "R_EmptyEnv" && 
          !exists(name, where = env, inherits = FALSE)) 
   {
     env <- parent.env(env)
   }
   env
})

.rs.registerReplaceHook("View", "utils", function(original, x, title) {

   # generate title if necessary
   if (missing(title))
      title <- deparse(substitute(x))[1]

   name <- ""
   env <- emptyenv()

   # if the argument is the name of a variable, we can monitor it in its
   # environment, and don't need to make a copy for viewing
   if (is(substitute(x), "name"))
   {
     name <- deparse(substitute(x))
     env <- .rs.findOwningEnv(name)
   }
   
   # call viewData 
   invisible(.Call("rs_viewData", force(x), title, name, env))
})

.rs.addFunction("initializeDataViewer", function(server) {
    if (server) {
        .rs.registerReplaceHook("edit", "utils", function(original, name, ...) {
            if (is.data.frame(name) || is.matrix(name))
                stop("Editing of data frames and matrixes is not supported in RStudio.", call. = FALSE)
            else
                original(name, ...)
        })
    }
})


