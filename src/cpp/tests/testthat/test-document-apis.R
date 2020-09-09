#
# test-document-apis.R
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

context("document API")

# TODO: These rely on client WaitFor methods, and so cannot be tested
test_that("insertText() handles various invocations", {
   
   skip("NYI")

   # Replace current selection
   .rs.api.insertText("foo")
   .rs.api.insertText(text = "foo")
   
   # Insert text at raw position (comment lines 3:5)
   .rs.api.insertText(Map(c, 3:5, 1), "#")
   
   # Insert text at raw ranges (uncomment lines 3:5)
   ranges <- Map(c, Map(c, 3:5, 1), Map(c, 3:5, 2))
   .rs.api.insertText(ranges, "")
   
   # Infinity is accepted
   .rs.api.insertText(c(Inf, 1), "# Hello\n")
   .rs.api.insertText(Inf, "# Hello\n")
   
})
