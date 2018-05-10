#
# test-s3-override.R
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

context("s3-override")

test_that("s3 methods can be overridden", {
   # create S3 object to print
   foo <- list(1, 2, 3)
   class(foo) <- "s3_test"

   # create override method
   answer <- function(x, ...) {
      return(c(42, 42))
   }
   
   # confirm that we get the base behavior by default
   expect_equal(c(1, 3), range(foo))

   # inject the override
   .rs.addS3Override("range.s3_test", answer)

   # confirm that the override has been applied
   expect_equal(c(42, 42), range(foo))

   # now remove the override and confirm that the base behavior has been restored
   .rs.removeS3Override("range.s3_test")
   expect_equal(c(1, 3), range(foo))
})

