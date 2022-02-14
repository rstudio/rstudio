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

test_that("our list.files, list.dirs hooks function as expected", {
   
   # these tests are only reliable with R 4.2.0, as they need R's native
   # list.files routine to be able to list files with Chinese names
   # and setting the locale seems insufficient for some cases
   skip_if(getRversion() < "4.2.0")
   
   library(testthat)
   
   # use native R routines
   .rs.files.restoreBindings()
   on.exit(.rs.files.replaceBindings(), add = TRUE)
   
   # work in temporary directory
   tdir <- tempfile()
   dir.create(tdir)
   owd <- setwd(tdir)
   on.exit(setwd(owd), add = TRUE)
   
   # create a bunch of sample files
   file.create(".hidden")
   
   file.create("file")
   
   dir.create("dir")
   dir.create("dir/subdir")
   file.create("dir/subdir/file")
   
   dir.create("empty")
   
   dir.create("hasEmptyDir")
   dir.create("hasEmptyDir/empty")
   
   nihao <- enc2utf8("\u4f60\u597d")  # 你好
   dir.create(nihao)
   file.create(paste(nihao, "file", sep = "/"))
   file.create(paste(nihao, "R", sep = "."))
   
   paths <- list(
      ".",
      getwd(),
      chartr("/", "\\", getwd()),
      file.path("..", basename(getwd())),
      "ThisPathDoesNotExist"
   )
   
   arglist <- list(
      path = paths,
      pattern = list(NULL, "[.]R$"),
      all.files = list(FALSE, TRUE),
      full.names = list(FALSE, TRUE),
      recursive = list(FALSE, TRUE),
      ignore.case = list(FALSE, TRUE),
      include.dirs = list(FALSE, TRUE),
      no.. = list(FALSE, TRUE)
   )
   
   crossed <- purrr::cross(arglist)
   for (i in seq_along(crossed)) {
      
      args <- crossed[[i]]
      lhs <- do.call(list.files, args)
      rhs <- do.call(.rs.listFiles, args)
      
      if (.rs.platform.isWindows)
         Encoding(lhs) <- "UTF-8"
      
      expect_equal(lhs, rhs)
      
   }
   
   arglist <- list(
      path = paths,
      full.names = list(FALSE, TRUE),
      recursive = list(FALSE, TRUE)
   )
   
   crossed <- purrr::cross(arglist)
   for (i in seq_along(crossed)) {
      
      args <- crossed[[i]]
      lhs <- do.call(list.dirs, args)
      rhs <- do.call(.rs.listDirs, args)
      
      if (.rs.platform.isWindows)
         Encoding(lhs) <- "UTF-8"
      
      expect_equal(lhs, rhs)
      
   }
   
   setwd(owd)
   unlink(tdir, recursive = TRUE)
   
})
