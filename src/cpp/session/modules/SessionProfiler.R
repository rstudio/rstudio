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

.rs.addFunction("profileResources", function()
{
   if (identical(getOption("profvis.prof_extension"), NULL)) {
      options("profvis.prof_extension" = ".rprof")
   }

   tempPath <- .Call(.rs.routines$rs_profilesPath)
   if (!.rs.dirExists(tempPath)) {
      dir.create(tempPath, recursive = TRUE)
   }

   return (list(
      tempPath = tempPath
   ))
})

.rs.addFunction("newProfileFileName" , function(path) {
   profileFileName <- function (path, i) {
      file.path(path, paste("profile", i, ".rprof", sep=""))
   }

   i <- 1
   while (file.exists(profileFileName(path, i))) {
      i <- i + 1
   }

   profileFileName(path, i)
})

.rs.addJsonRpcHandler("start_profiling", function(profilerOptions)
{
   tryCatch({
      resources <- .rs.profileResources()
      fileName <- .rs.newProfileFileName(resources$tempPath)

      Rprof(filename = fileName, line.profiling = TRUE)

      return(list(
         fileName = .rs.scalar(fileName)
      ))
   }, error = function(e) {
      return(list(error = .rs.scalar(e$message)))
   })
})

.rs.addJsonRpcHandler("stop_profiling", function(profilerOptions)
{
   tryCatch({
      Rprof(NULL)

      if (!identical(profilerOptions$fileName, NULL))
      {
         invisible(.Call(.rs.routines$rs_fileEdit, c(profilerOptions$fileName)))
      }

      return(list(
         fileName = .rs.nullOrScalar(profilerOptions$fileName)
      ))
   }, error = function(e) {
      return(list(error = .rs.scalar(e$message)))
   })
})

.rs.addJsonRpcHandler("open_profile", function(profilerOptions)
{
   tryCatch({
      resources <- .rs.profileResources()
      profvis <- profvis::profvis(prof_input = profilerOptions$fileName, split="h")

      htmlFile <- tempfile(fileext = ".html", tmpdir = resources$tempPath)
      htmlwidgets::saveWidget(profvis, htmlFile, selfcontained = FALSE)

      return(list(
         htmlFile = .rs.scalar(paste("profiles/", basename(htmlFile), sep = ""))
      ))
   }, error = function(e) {
      return(list(error = .rs.scalar(e$message)))
   })
})

.rs.addJsonRpcHandler("copy_profile", function(fromPath, toPath)
{
   tryCatch({
      file.copy(fromPath, toPath, overwrite = TRUE)

      return(list(
      ))
   }, error = function(e) {
      return(list(error = .rs.scalar(e$message)))
   })
})

.rs.registerNotifyHook("Rprof", "utils", function(...) 
{
   args <- c(...)
   if (!identical(args, NULL))
   {
      .rs.enqueClientEvent("rprof_started");
   }
   else
   {
      .rs.enqueClientEvent("rprof_stopped");
   }
})

options("profvis.print" = function(x)
{
   invisible(.Call(.rs.routines$rs_fileEdit, c(x$x$message$prof_output)))
})