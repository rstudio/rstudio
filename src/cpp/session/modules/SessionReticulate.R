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

.rs.addFunction("reticulate.replHook", function(buffer, contents, trimmed)
{
   # disallow execution of 'help()' as it will open an interactive terminal
   # that RStudio is unable to interact with
   if (buffer$empty()) {
      if (trimmed %in% c("help", "help()")) {
         msg <- "Interactive Python help is not available within RStudio"
         stop(msg, call. = FALSE)
      }
   }
   
})

options(reticulate.repl.hook = .rs.reticulate.replHook)
