#
# test-vcs.R
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

context("vcs")

# Project Options offers Git only when isGitInstalled() says so, which makes
# read_project_options the R-visible view of that check.
git_offered <- function() {
   options <- .rs.invokeRpc("read_project_options")
   "Git" %in% unlist(options$vcs_context$applicable_vcs)
}

# Restore the git_exe_path pref and the PATH when the calling test ends.
local_git_env <- function(envir = parent.frame()) {
   gitExePath <- .rs.api.readRStudioPreference("git_exe_path")
   withr::defer(.rs.api.writeRStudioPreference("git_exe_path", gitExePath), envir = envir)
   withr::local_envvar(PATH = Sys.getenv("PATH"), .local_envir = envir)
}

# Create a stand-in git, which succeeds at anything asked of it (including
# 'git --version'), in a temporary directory. Returns its path.
local_fake_git <- function(envir = parent.frame()) {

   dir <- tempfile("fake-git-")
   dir.create(dir)
   withr::defer(unlink(dir, recursive = TRUE), envir = envir)

   git <- file.path(dir, "git")
   writeLines(c("#!/bin/sh", "exit 0"), git)
   Sys.chmod(git, mode = "0755")
   git
}

# The PATH, less every directory that holds a git.
path_without_git <- function() {
   dirs <- strsplit(Sys.getenv("PATH"), .Platform$path.sep, fixed = TRUE)[[1]]
   paste(dirs[!file.exists(file.path(dirs, "git"))], collapse = .Platform$path.sep)
}

test_that("git stops being offered when the git it relied on goes away", {

   # on Windows, git is also found in its standard install locations, so
   # taking it off the PATH doesn't hide it
   skip_on_os("windows")
   local_git_env()

   git <- local_fake_git()
   .rs.api.writeRStudioPreference("git_exe_path", git)
   expect_true(git_offered())

   # with no other git to fall back on, the explicit git's answer must not
   # outlive the pref
   Sys.setenv(PATH = path_without_git())
   .rs.api.writeRStudioPreference("git_exe_path", "")
   expect_false(git_offered())

})

test_that("git installed part-way through a session is picked up", {

   skip_on_os("windows")
   local_git_env()

   Sys.setenv(PATH = path_without_git())
   .rs.api.writeRStudioPreference("git_exe_path", "")
   expect_false(git_offered())

   # a 'no' isn't remembered, so git showing up on the PATH is noticed
   git <- local_fake_git()
   Sys.setenv(PATH = paste(dirname(git), Sys.getenv("PATH"), sep = .Platform$path.sep))
   expect_true(git_offered())

})
