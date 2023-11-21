#
# test-help.R
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

context("help")

test_that(".rs.getHelp() handles empty and NULL package", {
    help_rnorm1 <- .rs.getHelp("rnorm")
    help_rnorm2 <- .rs.getHelp("rnorm", package = "")
    help_rnorm3 <- .rs.getHelp("rnorm", package = NULL)
    help_rnorm4 <- .rs.getHelp("rnorm", package = "stats")
    help_rnorm5 <- .rs.getHelp("stats::rnorm")
    help_rnorm6 <- .rs.getHelp("rnorm", "package:stats")

    expect_identical(help_rnorm1, help_rnorm2)
    expect_identical(help_rnorm1, help_rnorm3)
    expect_identical(help_rnorm1, help_rnorm4)
    expect_identical(help_rnorm1, help_rnorm5)
    expect_identical(help_rnorm1, help_rnorm6)
})
