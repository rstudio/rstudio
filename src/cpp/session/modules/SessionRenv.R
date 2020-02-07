#
# SessionRenv.R
#
# Copyright (C) 2009-19 by RStudio, PBC
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

.rs.setVar("renvCache", new.env(parent = emptyenv()))

.rs.addJsonRpcHandler("renv_init", function(project)
{
   # ask renv not to restart since we'll do it ourselves
   .rs.ensureDirectory(project)
   renv::init(project = project, restart = FALSE)
})

.rs.addJsonRpcHandler("renv_actions", function(action)
{
   project <- .rs.getProjectDirectory()
   actions <- renv:::actions(tolower(action), project = project)
   if (length(actions) == 0)
      return(list())
   
   remap <- c(
      "Package"          = "packageName",
      "Library Version"  = "libraryVersion",
      "Library Source"   = "librarySource",
      "Lockfile Version" = "lockfileVersion",
      "Lockfile Source"  = "lockfileSource",
      "Action"           = "action"
   )
   
   matches <- match(names(actions), names(remap), nomatch = 0L)
   names(actions)[matches] <- remap[matches]
   
   data <- lapply(seq_len(nrow(actions)), function(i) {
      as.list(actions[i, ])
   })
   
   .rs.scalarListFromList(data)
})

.rs.addFunction("renv.context", function()
{
   context <- list()
   
   # validate that renv is installed
   location <- find.package("renv", quiet = TRUE)
   context[["installed"]] <- .rs.scalar(length(location) != 0)
   
   # check and see if renv is active
   project <- Sys.getenv("RENV_PROJECT", unset = NA)
   active <- !is.na(project)
   context[["active"]] <- .rs.scalar(active)
   
   # read renv settings for active project
   context[["settings"]] <- list()
   if (active)
   {
      lapply(names(renv::settings), function(setting) {
         context[["settings"]][[setting]] <<-
            renv::settings[[setting]](project = project)
      })
      
      context$settings$snapshot.type      <- .rs.scalar(context$settings$snapshot.type)
      context$settings$use.cache          <- .rs.scalar(context$settings$use.cache)
      context$settings$vcs.ignore.library <- .rs.scalar(context$settings$vcs.ignore.library)
   }
   
   context
   
})

.rs.addFunction("renv.options", function()
{
   context <- .rs.renv.context()
   
   options <- list()
   
   active <- context[["active"]]
   options[["useRenv"]] <- active
   
   if (active)
   {
      options[["projectUseCache"]]         <- context$settings$use.cache
      options[["projectVcsIgnoreLibrary"]] <- context$settings$vcs.ignore.library
      
      options[["userSandboxEnabled"]] <- renv:::renv_config("sandbox.enabled", default = TRUE)
      options[["userShimsEnabled"]]   <- renv:::renv_config("shims.enabled", default = TRUE)
      options[["userUpdatesCheck"]]   <- renv:::renv_config("updates.check", default = TRUE)
   }
   
   .rs.scalarListFromList(options)
   
})

.rs.addFunction("renv.refresh", function()
{
   # get file info on installed packages, lockfile
   project <- renv::project()
   libdir <- renv:::renv_paths_library(project = project)
   
   files <- c(
      file.path(project, "renv.lock"),
      list.files(libdir, full.names = TRUE)
   )
   
   info <- file.info(files, extra_cols = FALSE)
   
   # drop unneeded fields
   new <- info[c("size", "mtime")]
   
   # check for changes
   old <- .rs.renvCache[["modifiedTimes"]]
   
   # have things changed?
   if (identical(old, new))
      return()
   
   # update cache
   .rs.renvCache[["modifiedTimes"]] <- new
   
   # fire events
   .rs.updatePackageEvents()
   .Call("rs_packageLibraryMutated", PACKAGE = "(embedding)")
})

.rs.addFunction("renv.readLockfilePackages", function(project)
{
   lockpath <- file.path(project, "renv.lock")
   lockfile <- renv:::renv_lockfile_read(lockpath)
   records <- renv:::renv_records(lockfile)
   
   `%||%` <- function(x, y) if (is.null(x)) y else x
   
   filtered <- lapply(records, function(record) {
      
      object <- list()
      
      object$Package <- record$Package %||% "(unknown)"
      object$Version <- record$Version %||% "(unknown)"
      object$Source <-
         record$Repository %||%
         record$Source %||%
         "(unknown)"
      
      object
         
   })
   
   df <- .rs.rbindList(filtered)
   rownames(df) <- NULL
   df
   
})

.rs.addFunction("renv.listPackages", function(project)
{
   # get list of packages
   installedPackages <- .rs.listInstalledPackages()
   
   # try to read the lockfile (return plain library list if this fails)
   lockfilePackages <- .rs.tryCatch(.rs.renv.readLockfilePackages(project))
   if (inherits(lockfilePackages, "error"))
      return(installedPackages)
   
   # rename columns for conformity of Packrat stuff
   names(lockfilePackages) <- c("name", "packrat.version", "packrat.source")

   # note which packages are in project library
   lib <- path.expand(installedPackages$library_absolute)
   projlib <- path.expand(renv:::renv_paths_library(project = project))
   
   lib <- gsub("\\", "/", lib, fixed = TRUE)
   projlib <- gsub("\\", "/", projlib, fixed = TRUE)
   
   installedPackages[["in.project.library"]] <- lib == projlib
   
   # merge together
   merged <- merge.data.frame(
      x = installedPackages,
      y = lockfilePackages,
      by = "name",
      all.x = TRUE,
      all.y = TRUE
   )
   
   merged
   
})
