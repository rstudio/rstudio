
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
  if (!grepl("://", url)) {
    stop("missing scheme")
  }
  
  scheme <- sub("://.*$", "", url)
  handler <- switch(scheme, 
    # rstatshelp:://stats/rnorm
    # rstatshelp:://stats::rnorm
    rstatshelp = function(url) {
      url <- sub("rstatshelp://", "", url)
      parts <- strsplit(url, "/")[[1L]]

      if (length(parts) == 2L) {
        .rs.showHelpTopic(topic = parts[[2L]], package = parts[[1L]])
      } else {
        # help::rnorm
        .rs.showHelpTopic(topic = parts[[1L]], package = NULL)
      }
    }, 

    # file:://some/file/path
    # file:://some/file/path#32
    # file:://some/file/path#32,15
    file = function(url) {
      url <- sub("file://", "", url)  
      parts <- strsplit(url, "#")[[1L]]
      file <- parts[[1L]]
      line <- -1L
      col <- -1L

      if (length(parts) == 2L) {
        location <- strsplit(parts[[2L]], ",")[[1L]]
        line <- as.numeric(location[[1L]])
        if (length(location) == 2L) {
          col <- as.numeric(location[[2L]])
        }
      }
      
      .rs.api.navigateToFile(file, line = line, col = col, moveCursor = TRUE)
    }, 

    # anything else opens in the browser, or the viewer
    # http://example.com
    function(url) {
      viewer <- utils::browseURL
      fun <- if (identical(params, "target=viewer")) {
        viewer <- .rs.api.viewer
      }
      viewer(url)
    }
    
  )
  handler(url)

})
