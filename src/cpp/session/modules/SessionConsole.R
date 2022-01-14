
#
# SessionConsole.R
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

.rs.addJsonRpcHandler("console_follow_hyperlink", function(url, text, params)
{
  # key1=value1:key2:value2 pattern, see https://iterm2.com/documentation-escape-codes.html
  if (identical(params, "")) {
    params <- list()
  } else {
    params <- strsplit(params, ":")[[1L]]

    names  <- sapply(params, function(p) sub("=.+$", "", p))
    params <- lapply(params, function(p) sub("^[^=]+=", "", p))
    names(params) <- names
  }
  
  if (identical(url, "rstudio:help")) {
    .rs.showHelpTopic(params$topic, params$package) 
  } else if (identical(url, "rstudio:vignette")) {
    print(vignette(params$topic, package = params$package))
  } else if (grepl("^rstudio:viewer:", url)) {
    url <- sub("^rstudio:viewer:", "", url)
    return(.rs.api.viewer(url))
  } else if (grepl("^file://", url)) {
    # file://some/file/path
    # file://some/file/path#32
    # file://some/file/path#32,15
    url <- sub("file://", "", url)
    parts <- strsplit(url, ":")[[1L]]
    file <- parts[[1L]]

    line <- -1L
    if (length(parts) > 1) {
      line <- as.numeric(parts[[2L]])
    }

    col <- -1L
    if (length(parts) > 2) {
      col <- as.numeric(parts[[3L]])
    }

    .rs.api.navigateToFile(file, line = line, col = col, moveCursor = TRUE)
  } else {
    utils::browseURL(url)
  }
})
