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

test_that("jobs can be added and removed", {
   # add a job
   jobId <- .rs.api.addJob(name = "job1")

   # retrieve all jobs
   jobs <- .rs.invokeRpc("get_jobs")

   # find job in the resultant list
   job <- jobs[[jobId]]
   expect_equal("job1", job[["name"]])

   # clean up
   .rs.api.removeJob(jobId)
   jobs <- .rs.invokeRpc("get_jobs")
   expect_equal(NULL, jobs[[jobId]])
})

test_that("job progress is updated", {
   jobId <- .rs.api.addJob(name = "job2", progressUnits = 100L)

   .rs.api.setJobProgress(jobId, 50L)
   .rs.api.addJobProgress(jobId, 10L)

   jobs <- .rs.invokeRpc("get_jobs")
   job <- jobs[[jobId]]

   # progress should be updated
   expect_equal(job[["progress"]], 60L)

   # adding progress to the job should put it in the running state
   expect_equal(job[["state_description"]], "running")
})

test_that("can't set progress of a non-ranged job", {
   jobId <- .rs.api.addJob(name = "job3")

   expect_error(.rs.api.addJobProgress(jobId, 10L))
})

test_that("property writes persist", {
   jobId <- .rs.api.addJob(name = "job4", status = "initializing")

   jobs <- .rs.invokeRpc("get_jobs")
   expect_equal(jobs[[jobId]][["status"]], "initializing")
   expect_equal(jobs[[jobId]][["state_description"]], "idle")

   .rs.api.setJobStatus(jobId, "reticulating splines")
   .rs.api.setJobState(jobId, "running")
   jobs <- .rs.invokeRpc("get_jobs")

   expect_equal(jobs[[jobId]][["status"]], "reticulating splines")
   expect_equal(jobs[[jobId]][["state_description"]], "running")
})

test_that("auto remove property is respected", {
   jobId <- .rs.api.addJob(name = "job5", autoRemove = FALSE)
   .rs.api.setJobState(jobId, "succeeded")

   jobs <- .rs.invokeRpc("get_jobs")
   expect_equal(jobs[[jobId]][["id"]], jobId)

   jobId <- .rs.api.addJob(name = "job6", autoRemove = TRUE)
   .rs.api.setJobState(jobId, "succeeded")

   jobs <- .rs.invokeRpc("get_jobs")
   expect_equal(jobs[[jobId]], NULL)
})


test_that("job timers are started", {
   jobId <- .rs.api.addJob(name = "job7", autoRemove = FALSE)

   # complete job immediately
   .rs.api.setJobState(jobId, "succeeded")

   jobs <- .rs.invokeRpc("get_jobs")
   job <- jobs[[jobId]]

   expect_true(job[["recorded"]] > 0)
   expect_true(job[["started"]] > 0)
   expect_true(job[["completed"]] > 0)
   expect_true(job[["recorded"]] <= job[["started"]])
   expect_true(job[["completed"]] <= job[["started"]])
})

