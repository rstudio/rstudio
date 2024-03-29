#
# test-lists.R
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

context("lists")

test_that("lists converted to scalars", {
   input <- list(1, 2, 3)
   expect_equal(.rs.scalarListFromList(input), 
                list(.rs.scalar(1), .rs.scalar(2), .rs.scalar(3)))
})

test_that("nested lists converted to scalars", {
   input <- list(1, 2, list(3, 4))
   expect_equal(.rs.scalarListFromList(input), 
                list(.rs.scalar(1), .rs.scalar(2), 
                     list(.rs.scalar(3), .rs.scalar(4))))
})

