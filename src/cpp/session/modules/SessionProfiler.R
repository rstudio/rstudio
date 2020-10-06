#
# SessionProfiler.R
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

.rs.setOptionDefault("profvis.print", function(x)
{
   .rs.profilePrint(x)
})

.rs.setOptionDefault("profvis.prof_extension", ".Rprof")

.rs.addFunction("profilesPath", function()
{
   .Call("rs_profilesPath", PACKAGE = "(embedding)")
})

.rs.addFunction("profileResources", function()
{
   tempPath <- getOption(
      "profvis.prof_output",
      default = .rs.profilesPath()
   )
   
   if (!.rs.dirExists(tempPath))
      dir.create(tempPath, recursive = TRUE)

   list(tempPath = tempPath)
})

.rs.addJsonRpcHandler("start_profiling", function(profilerOptions)
{
   tryCatch({
      resources <- .rs.profileResources()
      fileName <- tempfile(fileext = ".Rprof", tmpdir = resources$tempPath)

      Rprof(filename = fileName, line.profiling = TRUE, memory.profiling = TRUE)

      list(fileName = .rs.scalar(fileName))
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
         .rs.enqueClientEvent("rprof_created", list(
            path = .rs.scalar(profilerOptions$fileName)
         ));
      }

      return(list(
         fileName = .rs.scalar(profilerOptions$fileName)
      ))
   }, error = function(e) {
      return(list(error = .rs.scalar(e$message)))
   })
})

.rs.addJsonRpcHandler("open_profile", function(profilerOptions)
{
   tryCatch({
      resources <- .rs.profileResources()
      htmlFile <- normalizePath(tempfile(fileext = ".html", tmpdir = resources$tempPath), winslash = "/", mustWork = FALSE)

      if (identical(profilerOptions$profvis, NULL)) {
         if (identical(tools::file_ext(profilerOptions$fileName), "Rprof")) {
            profvis <- profvis::profvis(prof_input = profilerOptions$fileName, split = "h")
            htmlwidgets::saveWidget(profvis, htmlFile, selfcontained = TRUE)
         }
         else {
            .rs.rpc.copy_profile(profilerOptions$fileName, htmlFile)
         }
      }
      else {
         profvis <- profilerOptions$profvis
         htmlwidgets::saveWidget(profvis, htmlFile, selfcontained = TRUE)
      }

      return(list(
         htmlPath = .rs.scalar(paste("profiles/", basename(htmlFile), sep = "")),
         htmlLocalPath = .rs.scalar(htmlFile)
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
}, namespace = TRUE)

.rs.addFunction("profilePrint", function(x)
{
   result <- .rs.rpc.open_profile(list(
      profvis = x
   ))

   .rs.enqueClientEvent("rprof_created", result);
})

.rs.addJsonRpcHandler("clear_profile", function(filePath, htmlPath)
{
   tryCatch({
      resources <- .rs.profileResources()

      pathPrefix <- tools::file_path_sans_ext(basename(filePath))
      filePrefix <- tools::file_path_sans_ext(basename(htmlPath))
      
      rprofFile <- file.path(resources$tempPath, paste(pathPrefix, ".Rprof", sep = ""))
      if (file.exists(rprofFile)) {
         file.remove(rprofFile)
      }

      profileHtml <- file.path(resources$tempPath, paste(filePrefix, ".html", sep = ""))
      if (file.exists(profileHtml)) {
         file.remove(profileHtml)
      }

      profileDir <- file.path(resources$tempPath, paste(filePrefix, "_files", sep = ""))
      if (file.exists(profileDir)) {
         unlink(profileDir, recursive = TRUE)
      }

      rsconnectDir <- file.path(resources$tempPath, "rsconnect", "documents", paste(filePrefix, ".html", sep = ""))
      if (.rs.dirExists(rsconnectDir)) {
         unlink(rsconnectDir, recursive = TRUE)
      }

      return(list(
      ))
   }, error = function(e) {
      return(list(error = .rs.scalar(e$message)))
   })
})

.rs.addJsonRpcHandler("profile_sources", function(filePath, normPath)
{
   tryCatch({
      validPath <- ""
      paths <- c(filePath, normPath)
      found <- file.exists(paths)
      
      if (any(found == TRUE)) {
         validPath <- paths[[which(found == TRUE)[[1]]]]
      }

      return(.rs.scalar(validPath))
   }, error = function(e) {
      return(list(error = .rs.scalar(e$message)))
   })
})
