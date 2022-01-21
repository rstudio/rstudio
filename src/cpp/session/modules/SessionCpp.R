#
# SessionCpp.R
#
# Copyright (C) 2022 by RStudio, PBC
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

.rs.addFunction("cppSourceFile", function(file)
{
   lines <- .rs.tryCatch(readLines(file, warn = FALSE))
   call <- call("fun", file)
   
   if (is.character(lines) && any(grepl("cpp11::register", lines)))
      call[[1L]] <- quote(cpp11::cpp_source)
   else 
      call[[1L]] <- quote(Rcpp::sourceCpp)

    deparse(call)
})

.rs.addFunction("cppProjectStyle", function(inDirectory)
{
   # give up if this is not a project or not a package
   if (is.null(inDirectory))
      return("")

   DESCRIPTION <- file.path(inDirectory, "DESCRIPTION")
   if (!file.exists(DESCRIPTION))
      return("")

   # check if Rcpp or cpp11 is mentioned in LinkingTo:
   LinkingTo <- tryCatch(
      read.dcf(DESCRIPTION, all = TRUE)$LinkingTo, 
      error = function(e) {
         NULL
      }
   )      

   if (!is.null(LinkingTo)) {
      if (grepl("Rcpp", LinkingTo)) {
         return("Rcpp")
      } else if (grepl("cpp11", LinkingTo)) {
         return("cpp11")
      }
   }

   # check if cpp11 is vendored
   if (file.exists(file.path(inDirectory, "inst", "include", "cpp11.hpp"))) {
      return("cpp11")
   }

   # give up, i.e. use the user preference
   ""
})

.rs.addJsonRpcHandler("cpp_source_file", function(file)
{
   .rs.api.sendToConsole(.rs.cppSourceFile(file))
})

.rs.addJsonRpcHandler("cpp_project_style", function(inDirectory = .rs.getProjectDirectory()) 
{
    .rs.scalar(.rs.cppProjectStyle(inDirectory))
})
