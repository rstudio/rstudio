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

# writes a diagnostics report to `report` and returns its contents
write_diagnostics_report <- function(report) {

   capture.output(source(
      test_path("..", "..", "r", "R", "Diagnostics.R"),
      local = new.env(),
      chdir = TRUE
   ))

   paste(readLines(report), collapse = "\n")

}

expect_no_secrets <- function(contents, secrets) {
   for (secret in secrets)
      expect_false(grepl(secret, contents, fixed = TRUE), info = secret)
}

test_that("the diagnostics report leaves out credentials", {

   dir <- tempfile("diagnostics-")
   dir.create(dir)
   on.exit(unlink(dir, recursive = TRUE), add = TRUE)

   renviron <- file.path(dir, "Renviron")
   writeLines(c(
      "github_pat=renviron-lowercase-name",
      "GITHUB_PAT_READONLY=renviron-suffixed-name",
      "HTTPS_PROXY=http://renviron-user:renviron-url@proxy.example.com:3128",
      "HTTP_PROXY=renviron-bare-user:renviron-bare@bare-proxy.example.com:3128",
      "ALL_PROXY=http://renviron-at@corp.example:renviron-at-pass@at-proxy.example.com"
   ), renviron)

   rprofile <- file.path(dir, "Rprofile")
   writeLines(c(
      "options(diagnostics.test = \"rprofile-kept\")",
      "Sys.setenv(\"GITHUB_PAT\" = \"rprofile-quoted-name\")",
      "token <- \"rprofile-arrow\"",
      "Sys.setenv(EXTRA_HEADER = \"Bearer rprofile-first\", TOKEN = \"rprofile-second\")"
   ), rprofile)

   report <- file.path(dir, "report.txt")
   withr::local_envvar(c(
      RSTUDIO_DIAGNOSTICS_REPORT = report,
      R_ENVIRON_USER = renviron,
      R_PROFILE_USER = rprofile,
      PGPASSWORD = "envvar-joined-name",
      DATABASE_URL = "postgres://envvar-user:envvar-url@db.example.com/data",
      NOTIFY_URL = "https://notify.example.com?to=someone@mail.example.com"
   ))

   contents <- write_diagnostics_report(report)

   expect_no_secrets(contents, c(
      "renviron-lowercase-name",
      "renviron-suffixed-name",
      "renviron-user",
      "renviron-url",
      "renviron-bare",
      "renviron-at",
      "rprofile-quoted-name",
      "rprofile-arrow",
      "rprofile-first",
      "rprofile-second",
      "envvar-joined-name",
      "envvar-user",
      "envvar-url"
   ))

   # the files were dumped, and what was redacted is still identifiable
   expect_match(contents, "rprofile-kept", fixed = TRUE)
   expect_match(contents, "PGPASSWORD", fixed = TRUE)
   expect_match(contents, "db.example.com", fixed = TRUE)
   expect_match(contents, "@proxy.example.com:3128", fixed = TRUE)
   expect_match(contents, "@bare-proxy.example.com:3128", fixed = TRUE)
   expect_match(contents, "@at-proxy.example.com", fixed = TRUE)

   # an '@' after the host is not taken for credentials
   expect_match(
      contents,
      "https://notify.example.com?to=someone@mail.example.com",
      fixed = TRUE
   )

})

test_that("the diagnostics report leaves out credentials from the diagnostics binary", {

   skip_on_os("windows")

   dir <- tempfile("diagnostics-")
   dir.create(dir)
   on.exit(unlink(dir, recursive = TRUE), add = TRUE)

   # Diagnostics.R runs the binary found next to a dev build's rpostback
   postback <- file.path(dir, "session", "postback", "rpostback")
   dir.create(dirname(postback), recursive = TRUE)
   binary <- file.path(dir, "diagnostics", "diagnostics")
   dir.create(dirname(binary))
   writeLines(c(
      "#!/bin/sh",
      "echo '\"url\": \"https://binary-user:binary-pass@mirror.example.com/cran/\"'"
   ), binary)
   Sys.chmod(binary, "755")

   report <- file.path(dir, "report.txt")
   withr::local_envvar(c(
      RSTUDIO_DIAGNOSTICS_REPORT = report,
      RS_RPOSTBACK_PATH = postback
   ))

   contents <- write_diagnostics_report(report)

   expect_no_secrets(contents, c("binary-user", "binary-pass"))
   expect_match(contents, "@mirror.example.com/cran/", fixed = TRUE)

})
