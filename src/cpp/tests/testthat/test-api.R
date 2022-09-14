#
# test-api.R
#
# Copyright (C) 2022 by Posit Software, PBC
#
# Unless you have received this program directly from Posit pursuant
# to the terms of a commercial license agreement with Posit, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

context("rstudioapi")

test_that("command callbacks are invoked", {
   
   # register a command callback
   invoked <- 0
   handle <- .rs.api.registerCommandCallback("insertChunk", function() {
      invoked <<- invoked + 1 
   })
   
   expect_equal(invoked, 0)
   
   # record a command execution
   .rs.invokeRpc("record_command_execution", "insertChunk")
   
   # this should invoke the callback once
   expect_equal(invoked, 1)
   
   # record a second command execution 
   .rs.invokeRpc("record_command_execution", "insertChunk")
      
   # this should invoke the callback a second time
   expect_equal(invoked, 2)
   
   # unregister the callback
   .rs.api.unregisterCommandCallback(handle)
   
   # record a third command execution
   .rs.invokeRpc("record_command_execution", "insertChunk")
   
   # the callback should not be invoked, so execution count should
   # remain at 2
   expect_equal(invoked, 2)
})

test_that("command stream callbacks are invoked", {
   # register a command stream callback
   commands <- c()
   handle <- .rs.api.registerCommandCallback("*", function(id) {
      commands <<- c(commands, id)
   })
   
   # record several command executions
   .rs.invokeRpc("record_command_execution", "insertChunk")
   .rs.invokeRpc("record_command_execution", "showHelpMenu")
   .rs.invokeRpc("record_command_execution", "startJob")
   
   # ensure that the callback received all 3
   expect_equal(commands, c("insertChunk", "showHelpMenu", "startJob"))
   
   # unregister the callback
   .rs.api.unregisterCommandCallback(handle)
   
   # invoke one more command execution
   .rs.invokeRpc("record_command_execution", "startProfiler")
   
   # this execution should not be received
   expect_equal(commands, c("insertChunk", "showHelpMenu", "startJob"))
})
