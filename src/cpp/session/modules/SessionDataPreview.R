#
# SessionDataPreview.R
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

.rs.addFunction("previewDataFrame", function(data)
{
   max <- 100

   columns <- list()
   if (ncol(data)) {
      columns <- .rs.describeCols(data, max)
      if (ncol(data) > max) {
         columns <- head(columns, max)
         data <- data[, max]
      }
   }
      
   cnames <- names(data)
   rows <- nrow(data)

   if (rows > max) {
      data <- head(data, max)
      rows <- nrow(data)
   }

   for(i in seq_along(data)) {
      data[[i]] <- .rs.formatDataColumn(data[[i]], 1, rows)
   }


   preview <- list(
      data = unname(data),
      columns = columns
   )

   .rs.enqueClientEvent("data_output_completed", preview)
})
