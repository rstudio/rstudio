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

test_that("connection list filter removes duplicates appropriately", {
   # Test that duplicates are filtered appropriately
   # Entries can be listed with a modified name if they have a matching installer
   # - 'RStudio'
   # - 'RStudio Pro Drivers'
   # - 'Posit Pro Drivers'

   to_duplicate <- .rs.scalarListFromList(list(
      # connectionReadSnippets()
      list(name="Athena", source="Snippet", type="Snippet"),
      list(name="BigQuery", source="Snippet", type="Snippet"),
      # connectionReadDSN()
      list(name="Databricks", source="DSN", type="Snippet"),
      list(name="MySQL", source="DSN", type="Snippet"),
      list(name="PostgresSQL", source="DSN", type="Snippet"),
      list(name="SQLServer", source="DSN", type="Snippet"),
      # connectionReadPackages()
      list(name="Spark", source="Package", type="Shiny", package="sparklyr"),
      # connectionReadOdbc()
      list(name="Snowflake", source="ODBC", type="Snippet", installer="Homebrew")
   ))
   duplicates_keep <- .rs.scalarListFromList(list(
      # connectionReadOdbc()
      list(name="Databricks", source="ODBC", type="Snippet", installer="RStudio Pro Drivers"),
      list(name="MySQL", source="ODBC", type="Snippet", installer="RStudio"),
      list(name="PostgresSQL", source="ODBC", type="Snippet", installer="Posit Pro Drivers"),
      # connectionReadInstallers()
      list(name="SQLServer", source="Snippet", type="Install", subtype="Odbc")
   ))
   duplicates_reject <- .rs.scalarListFromList(list(
      # connectionReadSnippets()
      list(name="Snowflake", source="Snippet", type="Snippet"),
      # connectionReadDSN()
      list(name="Athena", source="DSN", type="Snippet"),
      # connectionReadOdbc()
      list(name="Spark", source="ODBC", type="Snippet", installer="AWS"),
      list(name="Databricks", source="ODBC", type="Snippet"),
      list(name="Athena", source="ODBC", type="Snippet", installer="apt"),
      # connectionReadPackageInstallers()
      list(name="BigQuery", source="Snippet", type="Install", subtype="Package")
   ))

   connections <- c(to_duplicate, duplicates_keep, duplicates_reject)
   connectionList <- .rs.connectionListFilter(connections)

   expect_length(connectionList, length(to_duplicate) + length(duplicates_keep))
   expect_in(to_duplicate, connectionList)

   # duplicates with updated name
   duplicates_expected <- .rs.scalarListFromList(lapply(duplicates_keep, function(entry) {
      entry$name <- paste(entry$name, .rs.connectionOdbcRStudioDriver(), sep = "")
      entry
   }))
   expect_in(duplicates_expected, connectionList)

   # duplicates that should be removed
   expect_false(any(duplicates_reject %in% connectionList))
})
