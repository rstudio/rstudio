#
# test-connections.R
#
# Copyright (C) 2022 by Posit Software, PBC
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

library(testthat)

context("connections")

test_that("ODBC ini file is read", {
   # write a test file containing a sample driver
   test_file <- file.path(tempdir(), "odbcinst.ini")
   on.exit(unlink(test_file), add = TRUE)
   writeLines(text = c(
      "[PostgreSQL]",
      "Description = PostgreSQL driver for GNU/Linux",
      "Driver = /usr/lib/psqlodbcw.so",
      "Setup = /usr/lib/libodbcpsqlS.so"),
     con = test_file)

   # ensure this file is read
   bundle <- .rs.odbcBundleReadIni(test_file)
   expect_equal(bundle, 
      list(PostgreSQL = c("Description = PostgreSQL driver for GNU/Linux",
                          "Driver = /usr/lib/psqlodbcw.so",
                          "Setup = /usr/lib/libodbcpsqlS.so")))

   # ensure we get an empty list if no file is present
   bundle <- .rs.odbcBundleReadIni(file.path(tempdir(), "missing.ini"))
   expect_equal(bundle, list())
})
