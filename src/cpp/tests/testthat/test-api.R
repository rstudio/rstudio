#
# test-api.R
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

context("rstudioapi")
test_that("invalid marker type generates informative error", {
   NULL_type <- list(type = NULL,
                     # 'file' would have to point to an actual R script to
                     # prevent the error 'The system cannot find the path
                     # specified' in normalizePath() in case of a valid 'type'.
                     file = file.path(".", "R", "fake_script.R"),
                     line = 1L,
                     column = 1L,
                     message = "Some message")
   char0_type <- NULL_type
   char0_type[["type"]] <- character(0)
   vector_type <- NULL_type
   vector_type[["type"]] <- c("warning", "error")
   
   expect_error(
      .rs.api.sourceMarkers(name = "with_NULL_type", markers = list(NULL_type)),
      regexp = "Invalid marker type", fixed = TRUE
   )
   
   expect_error(
      .rs.api.sourceMarkers(name = "with_char0_type", markers = list(char0_type)),
      regexp = "Invalid marker type", fixed = TRUE
   )
   
   expect_error(
      .rs.api.sourceMarkers(name = "with_vector_type", markers = list(vector_type)),
      regexp = "Invalid marker type", fixed = TRUE
   )
})

# Tests comments out until https://github.com/rstudio/rstudio/issues/12275 is resolved
# test_that("command callbacks are invoked", {
# 
#    # register a command callback
#    invoked <- 0
#    handle <- .rs.api.registerCommandCallback("insertChunk", function() {
#       invoked <<- invoked + 1
#    })
# 
#    expect_equal(invoked, 0)
# 
#    # record a command execution
#    .rs.invokeRpc("record_command_execution", "insertChunk")
# 
#    # this should invoke the callback once
#    expect_equal(invoked, 1)
# 
#    # record a second command execution
#    .rs.invokeRpc("record_command_execution", "insertChunk")
# 
#    # this should invoke the callback a second time
#    expect_equal(invoked, 2)
# 
#    # unregister the callback
#    .rs.api.unregisterCommandCallback(handle)
# 
#    # record a third command execution
#    .rs.invokeRpc("record_command_execution", "insertChunk")
# 
#    # the callback should not be invoked, so execution count should
#    # remain at 2
#    expect_equal(invoked, 2)
# })
# 
# test_that("command stream callbacks are invoked", {
#    # register a command stream callback
#    commands <- c()
#    handle <- .rs.api.registerCommandCallback("*", function(id) {
#       commands <<- c(commands, id)
#    })
# 
#    # record several command executions
#    .rs.invokeRpc("record_command_execution", "insertChunk")
#    .rs.invokeRpc("record_command_execution", "showHelpMenu")
#    .rs.invokeRpc("record_command_execution", "startJob")
# 
#    # ensure that the callback received all 3
#    expect_equal(commands, c("insertChunk", "showHelpMenu", "startJob"))
# 
#    # unregister the callback
#    .rs.api.unregisterCommandCallback(handle)
# 
#    # invoke one more command execution
#    .rs.invokeRpc("record_command_execution", "startProfiler")
# 
#    # this execution should not be received
#    expect_equal(commands, c("insertChunk", "showHelpMenu", "startJob"))
# })
