#
# SessionReticulate.R
#
# Copyright (C) 2009-17 by RStudio, Inc.
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

.rs.addFunction("reticulate.objectEnvironment", function()
{
   # TODO: do we want to attach reticulate objects to the
   # global environment, or perhaps instead just place in
   # a special environment on the search path?
   base::globalenv()
})

.rs.addFunction("reticulate.execute", function(code)
{
   # synchronize the R global environment with Python
   .rs.reticulate.synchronizePython()
   
   # run the associated code
   code <- paste(code, collapse = "\n")
   Encoding(code) <- "UTF-8"
   status <- reticulate::py_run_string(code, convert = FALSE)
   
   # re-synchronize changes following execution
   .rs.reticulate.synchronizeR()
})

.rs.addFunction("reticulate.synchronizePython", function(envir = .GlobalEnv)
{
   # get Python main module
   main <- reticulate::import_main(convert = FALSE)
   
   # iterate over keys in environment and synchronize
   keys <- ls(envir = envir, all.names = TRUE)
   for (key in keys) {
      
      # avoid overriding imported modules
      if (key %in% names(main)) {
         pyObject <- main[[key]]
         if (inherits(pyObject, "python.builtin.module"))
            next
      }
      
      # send to Python
      # TODO: how should we report errors here?
      value <- envir[[key]]
      tryCatch(
         main[[key]] <- value,
         error = function(e) NULL
      )
   }
   
   invisible(main)
})

.rs.addFunction("reticulate.synchronizeR", function(keys = NULL)
{
   # list objects in R, Python
   main <- reticulate::import_main(convert = FALSE)
   
   # collect objects that will be synchronized with the R session
   if (is.null(keys)) {
      keys <- grep("^_", names(main), invert = TRUE, value = TRUE)
   }
   
   for (key in keys) {
      
      # skip modules
      object <- main[[key]]
      if (inherits(object, "python.builtin.module"))
         next
      
      # generate wrapper and make available to R
      wrapper <- reticulate::py_to_r(object)
      assign(key, wrapper, envir = .rs.reticulate.objectEnvironment())
   }
   
   # return status invisibly
   invisible(main)
})