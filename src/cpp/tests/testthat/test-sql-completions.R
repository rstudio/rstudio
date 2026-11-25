#
# test-sql-completions.R
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

context("sql completions")

# SQL completions reuse R's completion types with other meanings (a table is
# a DATAFRAME, a field a DATASET), so the front end must be able to tell them
# from R completions by their language.
test_that("SQL table and field completions are labeled as SQL", {

   skip_if_not_installed("DBI")
   skip_if_not_installed("RSQLite")

   conn <- DBI::dbConnect(RSQLite::SQLite(), ":memory:")
   on.exit(DBI::dbDisconnect(conn), add = TRUE)
   DBI::dbWriteTable(conn, "mtcars", mtcars)

   ctx <- list(tables = "mtcars", schemas = list(NULL), contextKeyword = "select")

   tables <- .rs.sql.getCompletionsTables("mt", conn, ctx)
   expect_true("mtcars" %in% tables$results)
   expect_equal(as.character(tables$language), "SQL")

   fields <- .rs.sql.getCompletionsFields("mp", conn, ctx)
   expect_true("mpg" %in% fields$results)
   expect_equal(as.character(fields$language), "SQL")

   # the completions offered in a select list merge several sources
   merged <- .rs.sql.getCompletions("select mp", conn, ctx)
   expect_true("mpg" %in% merged$results)
   expect_equal(as.character(merged$language), "SQL")
})
