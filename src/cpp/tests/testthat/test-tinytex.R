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
#

context("tinytex")

# Deferred initialization, which adds TinyTeX to the PATH in a normal
# session, does not run under --run-script; exercise its helper directly.
test_that("a TinyTeX installation puts pdflatex on the PATH", {
   bin <- .rs.tinytexBin()
   skip_if(is.null(bin), "TinyTeX is not installed")
   skip_if(!nzchar(Sys.which(file.path(bin, "pdflatex"))), "TinyTeX has no pdflatex")

   # start from a PATH with no pdflatex on it, TinyTeX's or any other
   oldPath <- Sys.getenv("PATH")
   on.exit(Sys.setenv(PATH = oldPath), add = TRUE)
   entries <- strsplit(oldPath, .Platform$path.sep, fixed = TRUE)[[1]]
   hasPdflatex <- vapply(entries, FUN.VALUE = logical(1), function(entry) {
      nzchar(Sys.which(file.path(entry, "pdflatex")))
   })
   Sys.setenv(PATH = paste(entries[!hasPdflatex], collapse = .Platform$path.sep))
   skip_if(nzchar(Sys.which("pdflatex")), "pdflatex is still reachable")

   expect_true(.Call("rs_addTinytexToPath", PACKAGE = "(embedding)"))
   expect_true(nzchar(Sys.which("pdflatex")))
})
