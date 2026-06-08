#
# test-chat.R
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

context("chat")

test_that("manifestDownloadCommand sets a 30s timeout and downloads to a temp file", {
   cmd <- .rs.chat.manifestDownloadCommand("https://cdn.posit.co/posit-ai/manifest.json")
   expect_true(grepl("options(timeout = 30L)", cmd, fixed = TRUE))
   expect_true(grepl("tempfile()", cmd, fixed = TRUE))
   expect_true(grepl("download.file(", cmd, fixed = TRUE))
   expect_true(grepl("cat(readLines(", cmd, fixed = TRUE))
   # the URL is deparsed (quoted) into the command
   expect_true(grepl("https://cdn.posit.co/posit-ai/manifest.json", cmd, fixed = TRUE))
})

test_that("manifestDownloadCommand injects download.file.method when set", {
   withr::with_options(list(download.file.method = "libcurl"), {
      cmd <- .rs.chat.manifestDownloadCommand("https://example.test/m.json")
      expect_true(grepl("method = \"libcurl\"", cmd, fixed = TRUE))
   })
})

test_that("manifestDownloadCommand injects download.file.extra when set", {
   withr::with_options(list(download.file.extra = c("--cacert", "/etc/ca.pem")), {
      cmd <- .rs.chat.manifestDownloadCommand("https://example.test/m.json")
      expect_true(grepl("extra = ", cmd, fixed = TRUE))
      expect_true(grepl("--cacert", cmd, fixed = TRUE))
   })
})

test_that("manifestDownloadCommand omits method/extra when unset", {
   withr::with_options(list(download.file.method = NULL, download.file.extra = NULL), {
      cmd <- .rs.chat.manifestDownloadCommand("https://example.test/m.json")
      expect_false(grepl("method = ", cmd, fixed = TRUE))
      expect_false(grepl("extra = ", cmd, fixed = TRUE))
   })
})

test_that("manifestDownloadCommand propagates download failures as a non-zero exit", {
   # Some download.file methods return a non-zero status with only a warning, so
   # the command must check the status and stop() -- otherwise a failed download
   # would exit 0 with a partial/empty body and be parsed as a valid manifest.
   cmd <- .rs.chat.manifestDownloadCommand("https://example.test/m.json")
   expect_true(grepl("status <- download.file(", cmd, fixed = TRUE))
   expect_true(grepl("stop(paste(", cmd, fixed = TRUE))
})
