#
# test-global-calling-handlers.R
#
# Copyright (C) 2026 by Posit Software, PBC
#
# Unless you have received this program directly from Posit Software pursuant
# to the terms of a commercial license agreement with Posit Software, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

context("global calling handlers")

test_that("global calling handlers initialize safely at the top level", {
   skip_if(getRversion() < "4.0.0")

   # testthat installs condition handlers, which prevent globalCallingHandlers()
   # from registering handlers. Run the actual initializer in a vanilla Rscript
   # process, passing its body and the handler-selection body from this session.
   # Serialize only the bodies so no session environments reach the subprocess.
   definitions <- tempfile(fileext = ".rds")
   on.exit(unlink(definitions), add = TRUE)
   saveRDS(list(
      handlers = body(.rs.globalCallingHandlers.handlers),
      initialize = body(.rs.globalCallingHandlers.initialize)
   ), definitions)

   rscript <- file.path(R.home("bin"),
                       if (.Platform$OS.type == "windows") "Rscript.exe" else "Rscript")
   output <- suppressWarnings(system2(
      rscript,
      c("--vanilla", shQuote("resources/global-calling-handlers.R"),
        shQuote(definitions)),
      stdout = TRUE,
      stderr = TRUE
   ))
   status <- attr(output, "status")
   if (is.null(status))
      status <- 0L
   expect_equal(status, 0L, info = paste(output, collapse = "\n"))
})
