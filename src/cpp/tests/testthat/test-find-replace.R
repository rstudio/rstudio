#
# test-find-replace.R
#
# Copyright (C) 2019 by RStudio, Inc.
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

context("find-replace")

test_that("complete replace regex preview", {
   search_string <- ""
   replace_string <- ""
   file_patterns <- array(0, dim=c(0,0))

   contents <- .rs.invokeRpc("preview_replace",
                             search_string,
                             FALSE,
                             TRUE,
                             "",
                             file_patterns,
                             replace_string,
                             TRUE,
                             FALSE)
   expect_true(TRUE)
})

test_that("complete replace literal with literal", {
   search_string <- ""
   replace_string <- ""
   file_patterns <- array(0, dim=c(0,0))
   contents <- .rs.invokeRpc("complete_replace",
                             search_string,
                             FALSE,
                             TRUE,
                             "",
                             file_patterns,
                             as.integer(1),
                             replace_string,
                             FALSE,
                             FALSE)
   expect_true(TRUE)
})

test_that("complete replace literal with regex", {
   search_string <- ""
   replace_string <- ""
   file_patterns <- array(0, dim=c(0,0))
   contents <- .rs.invokeRpc("complete_replace",
                             search_string,
                             FALSE,
                             TRUE,
                             "",
                             file_patterns,
                             as.integer(1),
                             replace_string,
                             TRUE,
                             FALSE)
   expect_true(TRUE)
})

test_that("complete replace regex with literal", {
   search_string <- ""
   replace_string <- ""
   file_patterns <- array(0, dim=c(0,0))
   contents <- .rs.invokeRpc("complete_replace",
                             search_string,
                             TRUE,
                             TRUE,
                             "",
                             file_patterns,
                             as.integer(1),
                             replace_string,
                             FALSE,
                             FALSE)
   expect_true(TRUE)
})

test_that("complete replace regex with regex", {
   search_string <- ""
   replace_string <- ""
   file_patterns <- array(0, dim=c(0,0))
   contents <- .rs.invokeRpc("complete_replace",
                             search_string,
                             TRUE,
                             TRUE,
                             "",
                             file_patterns,
                             as.integer(1),
                             replace_string,
                             TRUE,
                             FALSE)
   expect_true(TRUE)
})
