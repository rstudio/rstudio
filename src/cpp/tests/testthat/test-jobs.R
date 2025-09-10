#
# test-jobs.R
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

test_that("job output is persisted", {
   jobId <- .rs.api.addJob(name = "job8", autoRemove = FALSE, running = TRUE)
   .rs.api.addJobOutput(jobId, "NormalOutput1")
   .rs.api.addJobOutput(jobId, "NormalOutput2")
   .rs.api.addJobOutput(jobId, "ErrorOutput", error = TRUE)
   .rs.api.setJobState(jobId, "failed")
   output <- .rs.invokeRpc("job_output", jobId, 0L)
   expect_true(length(output) == 3)
})

test_that("jobs can be cleaned up", {
    # add a couple of jobs
   job8 <- .rs.api.addJob(name = "job8", autoRemove = FALSE, running = TRUE)
   job9 <- .rs.api.addJob(name = "job9", autoRemove = FALSE, running = TRUE)

   # complete one
   .rs.api.setJobState(job8, "failed")

   jobs <- .rs.invokeRpc("get_jobs")
   expect_true(job8 %in% names(jobs))

   # clean it up
   .rs.invokeRpc("clear_background_jobs")

   # there should be 1 job remaining (only the completed job should be cleared)
   jobs <- .rs.invokeRpc("get_jobs")
   expect_true(job9 %in% names(jobs))
   expect_false(job8 %in% names(jobs))
})

test_that("custom job actions are executed", {
   mutable <- 0
   job10 <- .rs.api.addJob(name = "job10", autoRemove = FALSE, running = TRUE,
                           actions = list(
                              mutate = function(id) { mutable <<- 1 }))
   expect_equal(0, mutable)

   # execute the custom action and validate that the value becomes what we want
   .rs.api.executeJobAction(job10, "mutate")
   expect_equal(1, mutable)
})

test_that("job tags can be set and retrieved", {
   theTags <- c("one", "two", "four")
   jobId <- .rs.api.addJob(name = "job11", tags = theTags)
   jobs <- .rs.invokeRpc("get_jobs")
   job <- jobs[[jobId]]
   expect_true(sum(job[["tags"]] == theTags) == length(theTags))
})

test_that("script jobs run and can be replayed", {
   
   # helper function to wait for a job to finish running
   waitForJob <- function(id)
   {
      # wait a bit before first query to avoid timing failures
      Sys.sleep(1L)
      
      # wait for the job to finish
      tries <- 0L
      repeat
      {
         state <- .Call("rs_getJobState", id, PACKAGE = "(embedding)")
         if (is.null(state))
            stop("Job ", id, " does not exist.")
         
         if (state %in% c("idle", "running"))
         {
            tries <- tries + 1L
            if (tries > 60L)
               stop("Giving up on job ", id, " after 60 seconds (state: ", state, ")")

            # wait and perform background processing
            Sys.sleep(1L)
            .Call("rs_performBackgroundProcessing", FALSE, PACKAGE = "(embedding)")
         }
         else if (state %in% c("succeeded", "cancelled", "failed"))
         {
            return(state)
         }
         else
         {
            stop("Unexpected job state '", state, "'")
         }
         
      }
   }

   # tell the script job which file to create by creating a global variable
   filePath <- tempfile(pattern = "test", fileext = ".txt")
   assign("filePath", filePath, envir = globalenv())

   # create a script to run as a job
   scriptPath <- tempfile("script-job-", fileext = ".R")
   scriptCode <- quote({
      writeLines("Hello, world!", con = filePath)
   })
   writeLines(deparse(scriptCode), con = scriptPath)
   
   # run the script job
   jobId <- .rs.api.runScriptJob(
      path = scriptPath,
      name = "Test Job",
      importEnv = TRUE
   )

   # wait for the script job to finish running
   status <- waitForJob(jobId)
   expect_equal(status, "succeeded")
   expect_true(file.exists(filePath))

   # clean up the file it made and verify that it's gone
   unlink(filePath)
   expect_false(file.exists(filePath))

   # now replay the job to put the file back
   .rs.api.executeJobAction(jobId, "replay")
   status <- waitForJob(jobId)

   # it should be back
   expect_equal(status, "succeeded")
   expect_true(file.exists(filePath))

   # clean it up one last time
   unlink(filePath)
})


