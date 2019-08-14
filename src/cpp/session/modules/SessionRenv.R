#
# SessionRenv.R
#
# Copyright (C) 2009-19 by RStudio, Inc.
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
   switch(
      tolower(action),
      snapshot = .rs.renv.snapshotActions(),
      restore  = .rs.renv.restoreActions(),
      stop("unrecognized action '", action, "'")
   )
})

.rs.addFunction("renv.snapshotActions", function()
{
   project <- .rs.getProjectDirectory()
   before <- renv:::renv_lockfile_load(project)
   after <- renv:::snapshot(project = project, lockfile = NULL)
   diff <- .rs.renv.diff("lockfile", before, "library", after)
   data <- lapply(seq_len(NROW(diff)), function(i) as.list(diff[i, ]))
   .rs.scalarListFromList(data)
})

.rs.addFunction("renv.restoreActions", function()
{
   project <- .rs.getProjectDirectory()
   library <- renv::paths$library(project = project)
   before <- renv:::snapshot(project = project, library = library, lockfile = NULL, type = "simple")
   after <- renv:::renv_lockfile_load(project)
   diff <- .rs.renv.diff("library", before, "lockfile", after)
   diff <- diff[diff$action != "remove", ]
   data <- lapply(seq_len(NROW(diff)), function(i) as.list(diff[i, ]))
   .rs.scalarListFromList(data)
})

.rs.addFunction("renv.records", function(lockfile)
{
   records <- renv:::renv_records(lockfile)
   if (is.null(records)) list() else records
})

.rs.addFunction("renv.diff", function(before.prefix, before,
                                      after.prefix, after)
{
   # wrangle into data frames
   fields <- c("Package", "Version", "Source")
   
   default <- data.frame(
      Package = character(),
      Version = character(),
      Source  = character(),
      stringsAsFactors = FALSE
   )
   
   lhs <- renv:::bapply(unname(.rs.renv.records(before)), `[`, fields)
   if (is.null(lhs))
      lhs <- default
   
   rhs <- renv:::bapply(unname(.rs.renv.records(after)),  `[`, fields)
   if (is.null(rhs))
      rhs <- default
   
   if (nrow(lhs) == 0 && nrow(rhs) == 0)
      return(default)
   
   names(lhs) <- c("packageName", paste(before.prefix, names(lhs)[-1L], sep = ""))
   names(rhs) <- c("packageName", paste(after.prefix, names(rhs)[-1L], sep = ""))
   
   # merge together
   data <- merge(lhs, rhs, by = "packageName", all = TRUE)
   
   # add in actions
   actions <- renv:::renv_lockfile_diff_packages(before, after)
   adf <- data.frame(
      packageName = names(actions),
      action = as.character(actions),
      stringsAsFactors = FALSE
   )
   
   # merge together
   all <- merge(data, adf, by = "packageName", all = TRUE)
   
   # drop empty actions
   all[!is.na(all$action), ]
})

.rs.addFunction("renv.context", function()
{
   context <- list()
   
   # validate that renv is installed
   location <- find.package("renv", quiet = TRUE)
   context[["installed"]] <- length(location) != 0
   
   # check and see if renv is active
   project <- Sys.getenv("RENV_PROJECT", unset = NA)
   context[["active"]] <- !is.na(project)
   
   # return context
   lapply(context, .rs.scalar)
   
})

.rs.addFunction("renv.options", function()
{
   context <- .rs.renv.context()
   
   options <- list()
   
   options[["useRenv"]] <- context[["active"]]
   # TODO: surface options associated with
   # the current project
   
   options
   
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
   packages <- renv:::renv_records(lockfile)
   filtered <- lapply(packages, `[`, c("Package", "Version", "Source"))
   df <- .rs.rbindList(filtered)
   rownames(df) <- NULL
   df
})

.rs.addFunction("renv.listPackages", function(project) {
   
   # get list of packages
   installedPackages <- .rs.listInstalledPackages()
   
   # try to read the lockfile (return plain library list if this fails)
   lockfilePackages <- .rs.tryCatch(.rs.renv.readLockfilePackages(project))
   if (inherits(lockfilePackages, "error"))
      return(installedPackages)
   
   # rename columns for conformity of Packrat stuff
   names(lockfilePackages) <- c("name", "packrat.version", "packrat.source")

   # note which packages are in project library
   installedPackages[["in.project.library"]] <-
      installedPackages$library_absolute == renv:::renv_paths_library(project = project)

   # merge together
   merge.data.frame(
      x = installedPackages,
      y = lockfilePackages,
      by = "name",
      all.x = TRUE,
      all.y = TRUE
   )
   
})
