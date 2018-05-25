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

sourceWithProgress <- function(script, progress) {
   output <- file(progress, open = "at")

   # parse the script
   statements <- parse(file = script)

   # write statement count
   writeLines(paste("count", length(statements), sep = ","), con = output)

   # evaluate each statement
   for (idx in seq_along(statements)) {
      # evaluate the statement
      eval(statements[[idx]], envir = globalenv())

      # update progress
      writeLines(paste("statement", idx, sep = ","), con = output)
   }

   writeLines(paste("completed", 1, sep = ","), con = output)
   close(output)
}
