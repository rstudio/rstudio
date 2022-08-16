#
# SessionCpp.R
#
# Copyright (C) 2022 by Posit, PBC
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

.rs.addJsonRpcHandler("cpp_source_file", function(file)
{
   .rs.api.sendToConsole(.rs.cppSourceFile(file))
})
