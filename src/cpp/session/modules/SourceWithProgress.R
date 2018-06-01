#
# SourceWithProgress.R
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

emitProgress <- function(kind, arg, con) {
   cat(paste0("__rs_progress_0__ ", kind, " __ ", arg, 
              " __rs_progress_1__\n"), file = con)
}

sourceWithProgress <- function(script, con) {
   # parse the script
   statements <- parse(file = script)

   # write statement count
   emitProgress("count", length(statements), con)

   # evaluate each statement
   for (idx in seq_along(statements)) {
      # evaluate the statement
      eval(statements[[idx]], envir = globalenv())

      # update progress
      emitProgress("statement", idx, con)
   }

   emitProgress("completed", 1, con)
}
