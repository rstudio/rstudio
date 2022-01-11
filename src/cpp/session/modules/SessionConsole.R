
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
  # `params=` follows the key1=value1:key2:value2 pattern
  # https://iterm2.com/documentation-escape-codes.html
  #
  # This at the moment is only used for params = c(target = "viewer")
  if (identical(params, "")) {
    params <- list()
  } else {
    params <- strsplit(params, ":")

    names  <- sapply(params, function(p) sub("=.+$", "", p))
    params <- lapply(params, function(p) sub("^[^=]+=", "", p))
    names(params) <- names
  }
  
  if (grepl("^file://", url)) {
    # file:://some/file/path
    # file:://some/file/path#32
    # file:://some/file/path#32,15
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
    # anything else goes through utils::browseURL() or the viewer
    # utils::browseURL() in turn knows how to open local help files
    # e.g. "http://localhost:8787/library/base/html/mean.html" in the help pane
    # in the help pane
    if (identical(params$target, "viewer")) {
      .rs.api.viewer(url)
    } else {
      utils::browseURL(url)
    }
  }
})
