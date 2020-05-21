#
# SessionJobs.R
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

# Helpers --------------------------------------------------------------------

.rs.addFunction("scriptActions", function() {
   list(stop = function(id) {
      .Call("rs_stopScriptJob", id, PACKAGE = "(embedding)")
   })
})

# API functions --------------------------------------------------------------

.rs.addApiFunction("addJob", function(name, status = "", progressUnits = 0L,
      actions = NULL, running = FALSE, autoRemove = TRUE, group = "", show = TRUE,
      launcherJob = FALSE, tags = NULL) {

   # validate arguments
   if (missing(name))
      stop("Cannot add a job without a name.")
   if (!is.integer(progressUnits) || progressUnits < 0L || progressUnits > 1000000L)
      stop("progressUnits must be an integer between 1 and 1000000, or 0 to disable progress.")

   # begin tracking job
   .Call("rs_addJob", name, status, progressUnits,
      actions, running, autoRemove, group, show, launcherJob, tags, PACKAGE = "(embedding)")
})

.rs.addApiFunction("removeJob", function(job) {
   if (missing(job))
      stop("Must specify job ID to remove.")
   .Call("rs_removeJob", job, PACKAGE = "(embedding)")
   invisible(NULL)
})

.rs.addApiFunction("setJobProgress", function(job, units) {
   if (missing(job))
      stop("Must specify job ID to set progress for.")
   if (missing(units))
      stop("Must specify number of progress units to set.")
   .Call("rs_setJobProgress", job, units, PACKAGE = "(embedding)")
   invisible(NULL)
})

.rs.addApiFunction("addJobProgress", function(job, units) {
   if (missing(job))
      stop("Must specify job ID to add progress to.")
   if (missing(units))
      stop("Must specify number of progress units to add")
   .Call("rs_addJobProgress", job, units, PACKAGE = "(embedding)")
   invisible(NULL)
})

.rs.addApiFunction("setJobStatus", function(job, status) {
   if (missing(job))
      stop("Must specify job ID to set status for.")
   if (missing(status))
      stop("Must specify job status to update.")
   .Call("rs_setJobStatus", job, status, PACKAGE = "(embedding)")
   invisible(NULL)
})

.rs.addApiFunction("setJobState", function(job, state = c("idle", "running", "succeeded",
                                                          "cancelled", "failed")) {
   if (missing(job))
      stop("Must specify job ID to change state for.")
   state <- match.arg(state)
   .Call("rs_setJobState", job, state, PACKAGE = "(embedding)")
   invisible(NULL)
})

.rs.addApiFunction("addJobOutput", function(job, output, error = FALSE) {
   .Call("rs_addJobOutput", job, output, error, PACKAGE = "(embedding)")
   invisible(NULL)
})

.rs.addApiFunction("runScriptJob", function(path, 
                                            name = NULL,
                                            encoding = "unknown",
                                            workingDir = NULL, 
                                            importEnv = FALSE,
                                            exportEnv = "") {
   if (missing(path))
      stop("Must specify path to R script to run.")
   if (!file.exists(path))
      stop("The R script '", path, "' does not exist.")
   .Call("rs_runScriptJob", path, name, encoding, workingDir, importEnv, exportEnv, 
         PACKAGE = "(embedding)")
})

.rs.addApiFunction("executeJobAction", function(job, action) {
   if (missing(job))
      stop("Must specify job ID to execute action for.")
   .Call("rs_executeJobAction", job, action)
})

.rs.addApiFunction("stopJob", function(job) {
   .rs.api.executeJobAction(job, "stop")
})
