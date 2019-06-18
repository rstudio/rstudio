#
# SessionRenv.R
#
# Copyright (C) 2009-19 by RStudio, Inc.
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

.rs.addFunction("renv.context", function()
{
   print("hello!")
   
   context <- list()
   
   # validate that renv is installed
   location <- find.package("renv", quiet = TRUE)
   context[["renv_installed"]] <- length(location) != 0
   
   # check and see if renv is active
   project <- Sys.getenv("RENV_PROJECT", unset = NA)
   context[["renv_active"]] <- !is.na(project)
   
   # return context
   context
   
})
