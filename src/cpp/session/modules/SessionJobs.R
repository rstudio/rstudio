#
# SessionJobs.R
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

.rs.addApiFunction("addJob", function(name, status = "", progressUnits = 0L,
      actions = NULL, estimate = 0L, estimateRemaining = FALSE, running = FALSE, 
      autoRemove = FALSE, group = "") {

   # validate arguments
   if (missing(name))
      stop("Cannot add a job without a name.")
   if (!is.integer(progressUnits) || progressUnits < 1L || progressUnits > 1000000L)
      stop("progressUnits must be an integer between 1 and 1000000, or 0 to disable progress.")
   if (isTRUE(estimateRemaining) && identical(progressUnits, 0))
      stop("Must specify progressUnits in order to estimate remaining time.")

   # begin tracking job
   .Call("rs_addJob", name, status, progressUnits,
      actions, estimate, estimateRemaining, running, autoRemove, group)
})
