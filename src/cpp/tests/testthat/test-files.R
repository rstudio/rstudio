#
# test-files.R
#
# Copyright (C) 2022 by RStudio, PBC
#
# Unless you have received this program directly from RStudio pursuant
# to the terms of a commercial license agreement with RStudio, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

context("files")

test_that("file listings are correct", {

  # Create temporary directory to host a directory listing
  testdir <- file.path(tempdir(), "file-listings")
  dir.create(testdir, recursive = TRUE)
  on.exit(unlink(testdir, recursive = TRUE), add = TRUE)

  # Read old pref values
  alwaysShownFiles <- .rs.api.readRStudioPreference("always_shown_files")
  on.exit(.rs.api.writeRStudioPreference("always_shown_files", as.list(alwaysShownFiles)))
  alwaysShownExts <- .rs.api.readRStudioPreference("always_shown_extensions")
  on.exit(.rs.api.writeRStudioPreference("always_shown_extensions", as.list(alwaysShownExts)))

  # Prepare dummy set of files
  files <- c(
    ".shown",
    ".hidden",
    ".shown.yml",
    ".also.shown.yml",
    ".hidden.yml",
    "normal.R",
    "data.csv")
  for (f in files) {
    writeLines(text = "", con = file.path(testdir, f))
  }

  # Set up preferences
  .rs.api.writeRStudioPreference("always_shown_files", list(
    ".shown.yml",
    ".also.shown.yml"
  ))
  .rs.api.writeRStudioPreference("always_shown_extensions", list(
    ".shown"
   ))

  # Perform a file listing on the directory
  listing <- .rs.invokeRpc("list_files",
                testdir, # Dir to list
                FALSE,   # Monitor listing
                FALSE    # Show hidden files
  )

  # Extract the filenames that were returned
  filenames <- unlist(lapply(listing$files, function(f) {
    basename(f$path)
  }))

  # Check to ensure that we got what we expected
  expect_equal(sort(filenames), c(
    ".also.shown.yml",  # An always shown file
    ".shown",           # An always shown extension
    ".shown.yml",       # An always shown file
    "data.csv",         # A regular file
    "normal.R"          # A regular file
  ))

})

