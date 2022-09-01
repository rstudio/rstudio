#
# SessionCodeCoverage.R
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

.rs.addJsonRpcHandler("coverage_get_information", function(path) {

    # mockup data
    line <- c(120L, 128L, 129L)
    value <- c(1L, 0L, 0L)

    list( 
        filename = .rs.scalar("R/summarise.R"), 
        line = line,
        value = value
    )
})
