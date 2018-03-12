#
# test-jobs.R
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

context("jobs")

test_that("jobs can be added", {
   # add a job
   jobId <- .rs.api.addJob(name = "job1")

   # retrieve all jobs
   jobs <- .rs.invokeRpc("get_jobs")

   # find job in the resultant list
   job <- jobs[[jobId]]
   expect_equal(name, job[["name"]])

   # clean up
   .rs.api.removeJob(jobId)
})

