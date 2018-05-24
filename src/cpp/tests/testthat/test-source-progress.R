#
# test-source-progress.R
#
# Copyright (C) 2009-18 by RStudio, Inc.
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

context("source-progress")

# one-time setup; load progress script
source(file.path(.rs.sessionModulePath(), "SourceWithProgress.R"))

test_that("statements in script are executed", {
   sourceWithProgress("resources/source-progress/assignment.R")

   expect_equal(2, var)
})

