#
# test-tinytex.R
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

context("tinytex")

test_that("a TinyTeX installation puts pdflatex on the PATH after startup", {
   bin <- .rs.tinytexBin()
   skip_if(is.null(bin), "TinyTeX is not installed")
   skip_if(!nzchar(Sys.which(file.path(bin, "pdflatex"))), "TinyTeX has no pdflatex")

   # client init no longer probes TeX; deferred init adds TinyTeX instead
   expect_true(nzchar(Sys.which("pdflatex")))
})
