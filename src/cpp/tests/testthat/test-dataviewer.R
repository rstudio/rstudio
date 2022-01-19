#
# test-dataviewer.R
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

context("data viewer")

test_that(".rs.formatDataColumnDispatch() iterates over the classes", {
    registerS3method("[", "foo",  function(x, ...) {
        structure(NextMethod(), class = class(x))
    })
    registerS3method("format", "foo", function(x, ...) {
        rep("Hi there!", length(x))
    })
    
    x <- structure("x", class = c("foo", "bar", "character"))
    expect_equal(.rs.formatDataColumnDispatch(x), "Hi there!")
    expect_equal(.rs.formatDataColumn(x, 1, 1), "Hi there!")

    x <- structure("x", class = c("bar", "foo", "character"))
    expect_equal(.rs.formatDataColumnDispatch(x), "Hi there!")
    expect_equal(.rs.formatDataColumn(x, 1, 1), "Hi there!")
})
