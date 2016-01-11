#
# SessionDataImport.R
#
# Copyright (C) 2009-16 by RStudio, Inc.
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

.rs.addJsonRpcHandler("preview_data_import", function(path, maxRows, maxCols = 100, maxFactors = 64)
{
  data <- readr::read_csv(path, n_max = maxRows)

  columns <- .rs.describeCols(data, maxCols, maxFactors)

  cnames <- names(data)
  size <- nrow(data)

  for(cname in cnames[-1]) {
    data[[cname]] <- .rs.formatDataColumn(data[[cname]], 1, size)
  }

  list(data = data,
       columns = columns)
})
