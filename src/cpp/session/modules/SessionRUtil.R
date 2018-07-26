#
# SessionRUtil.R
#
# Copyright (C) 2009-17 by RStudio, Inc.
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

.rs.addFunction("isNullExternalPointer", function(object)
{
   .Call("rs_isNullExternalPointer", object, PACKAGE = "(embedding)")
})

.rs.addFunction("readIniFile", function(filePath)
{
   as.list(.Call("rs_readIniFile", filePath))
})

.rs.addFunction("runAsyncRProcess", function(
   code,
   workingDir  = getwd(),
   onStarted   = function() {},
   onContinue  = function() {},
   onStdout    = function(output) {},
   onStderr    = function(output) {},
   onCompleted = function(exitStatus) {})
{
   callbacks <- list(
      started   = onStarted,
      continue  = onContinue,
      stdout    = onStdout,
      stderr    = onStderr,
      completed = onCompleted
   )
   
   .Call(
      "rs_runAsyncRProcess",
      as.character(code),
      normalizePath(workingDir, mustWork = TRUE),
      as.list(callbacks),
      PACKAGE = "(embedding)"
   )
})
