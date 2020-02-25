#
# test-dependencies.R
#
# Copyright (C) 2020 by RStudio, PBC
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

context("dependencies")

test_that("simple topological sort works", {
   # unsorted nodes
   nodes <- c("b", "a", "c")

   # edges a -> b -> c
   edges <- list(
      list(from = "a", to = "b"),
      list(from = "b", to = "c"))
   
   # in this configuration we'd expect to install package c, then package b, then package a
   expect_equal(.rs.topoSortPackages(nodes, edges), 
                c("c", "b", "a"))
})


