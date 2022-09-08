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

.rs.setVar("coverageEnv", new.env(parent = emptyenv()))

.rs.addFunction("coverage_update", function(tcov) {
    tcov$filename <- .rs.createAliasedPath(normalizePath(tcov$filename))
    if (is.null(tcov$color)) {
        tcov$color <- ifelse(tcov$value > 0, "#00ff0020", "#ff000020")
    }
    assign("coverage_data", tcov, .rs.coverageEnv)
})

.rs.addJsonRpcHandler("coverage_get_information", function(path) {

    coverage_data <- .rs.coverageEnv[["coverage_data"]]

    info <- list(filename = path, line = integer(), value = integer(), color = character())
    if (!is.null(coverage_data)) 
    {
        data <- coverage_data[coverage_data$filename == path, ]
        info$line  <- data$line
        info$value <- data$value
        info$color <- data$color
    }
    info
})
