#
# regenerate-css.R
#
# Copyright (C) 2020 by RStudio, PBC
#
# Unless you have received this program directly from RStudio pursuant
# to the terms of a commercial license agreement with RStudio, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#

# Manual helper for updating expected CSS values for tmTheme conversion. 
# 
# Preconditions:
# 
# 1. "themes" object in environment from test-themes.R
# 2. Working directory set to "testthat"
#

units <- as.integer(length(themes))
job <- rstudioapi::jobAdd("Update themes", 
                          progressUnits = units, 
                          running = TRUE,
                          autoRemove = FALSE)

.rs.enumerate(themes, function(key, value) {
  tryCatch({
     rstudioapi::jobAddOutput(job, paste0(key, "... "))
     converted <- .rs.convertTmTheme(
        .rs.parseTmTheme(
           file.path(
              "themes",
              "tmThemes",
              paste0(key, ".tmTheme"))))
     rstudioapi::jobSetStatus(job, key)
     writeLines(converted$theme,
                file.path("themes", "acecss", paste0(value$fileName, ".css")))
     rstheme <- .rs.convertAceTheme(key, converted$theme, value$isDark)
     writeLines(rstheme, file.path("themes", "rsthemes", paste0(value$fileName, ".rstheme")))
     rstudioapi::jobAddOutput(job, "OK\n")
     rstudioapi::jobAddProgress(job, 1)
  }, error = function(e) {
    rstudioapi::jobSetState(job, "failed")
    stop(e)
  })
})

rstudioapi::jobSetState(job, "succeeded")
