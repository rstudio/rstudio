#
# test-diagnostics.R
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

library(testthat)

test_that("the diagnostics report leaves out credentials", {

   dir <- tempfile("diagnostics-")
   dir.create(dir)
   on.exit(unlink(dir, recursive = TRUE), add = TRUE)

   renviron <- file.path(dir, "Renviron")
   writeLines(c(
      "github_pat=renviron-lowercase-name",
      "HTTPS_PROXY=http://someone:renviron-url@proxy.example.com:3128"
   ), renviron)

   rprofile <- file.path(dir, "Rprofile")
   writeLines(c(
      "Sys.setenv(\"GITHUB_PAT\" = \"rprofile-quoted-name\")",
      "token <- \"rprofile-arrow\""
   ), rprofile)

   report <- file.path(dir, "report.txt")
   withr::local_envvar(c(
      RSTUDIO_DIAGNOSTICS_REPORT = report,
      R_ENVIRON_USER = renviron,
      R_PROFILE_USER = rprofile,
      PGPASSWORD = "envvar-joined-name",
      DATABASE_URL = "postgres://someone:envvar-url@db.example.com/data"
   ))

   capture.output(source(
      test_path("..", "..", "r", "R", "Diagnostics.R"),
      local = new.env(),
      chdir = TRUE
   ))
   contents <- paste(readLines(report), collapse = "\n")

   secrets <- c(
      "renviron-lowercase-name",
      "renviron-url",
      "rprofile-quoted-name",
      "rprofile-arrow",
      "envvar-joined-name",
      "envvar-url"
   )
   for (secret in secrets)
      expect_false(grepl(secret, contents, fixed = TRUE), info = secret)

   # what was redacted is still identifiable
   expect_match(contents, "PGPASSWORD", fixed = TRUE)
   expect_match(contents, "db.example.com", fixed = TRUE)
   expect_match(contents, "proxy.example.com", fixed = TRUE)

})
