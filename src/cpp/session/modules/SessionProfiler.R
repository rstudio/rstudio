#
# SessionProfiler.R
#
# Copyright (C) 2009-16 by RStudio, Inc.
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

.rs.addJsonRpcHandler("start_profiling", function(profilerOptions)
{
   tryCatch({
      tempPath <- .Call("rs_profilesPath")
   	fileName <- tempfile(fileext = ".rprof", tmpdir = tempPath)

      Rprof(filename = fileName, line.profiling = TRUE)

      return(list(
         fileName = fileName
      ))
   }, error = function(e) {
      return(list(error = e))
   })
})

.rs.addJsonRpcHandler("stop_profiling", function(profilerOptions)
{
   tryCatch({
      Rprof(NULL)
      
      return(list(
         fileName = profilerOptions$fileName
      ))
   }, error = function(e) {
      return(list(error = e))
   })
})

.rs.addJsonRpcHandler("open_profile", function(profilerOptions)
{
   tryCatch({
      profvis <- profvis::profvis(prof_input = profilerOptions$fileName)

      return(list(
         profvis = profvis
      ))
   }, error = function(e) {
      return(list(error = e))
   })
})
