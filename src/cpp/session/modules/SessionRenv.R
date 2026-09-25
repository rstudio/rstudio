#
# SessionRenv.R
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

.rs.setVar("renvCache", new.env(parent = emptyenv()))

.rs.addJsonRpcHandler("renv_init", function(project)
{
   # run script in child process (done so that `renv::init()` doesn't
   # change the state of the running session)
   .rs.executeFunctionInChildProcess(
      callback   = .rs.renv.initCallback,
      data       = list(repos = getOption("repos")),
      workingDir = project
   )
   
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
   renv <- asNamespace("renv")
   
   # read lockfile
   lockpath <- file.path(project, "renv.lock")
   lockfile <- renv$renv_lockfile_read(lockpath)
   
   # grab lockfile records
   # renv internal APIs changed in a recent update, so look for the right
   # accessor method here
   method <- .rs.nullCoalesce(renv$renv_lockfile_records, renv$renv_records)
   packages <- method(lockfile)
   
   # keep only the pieces we need
   filtered <- lapply(packages, `[`, c("Package", "Version", "Source"))
   df <- .rs.rbindList(filtered)
   rownames(df) <- NULL
   df
})

.rs.addFunction("renv.activeProjectPath", function()
{
   tryCatch(renv:::project(), error = function(e) NULL)
})

.rs.addFunction("renv.listPackages", function(project)
{
   # get list of packages
   installedPackagesList <- .rs.listInstalledPackages()
   installedPackages <- .rs.rbindList(installedPackagesList)
   
   # try to read the lockfile (return plain library list if this fails)
   lockfilePackages <- .rs.tryCatch(.rs.renv.readLockfilePackages(project))
   if (inherits(lockfilePackages, "error"))
      return(installedPackages)
   
   # rename columns for conformity of Packrat stuff
   names(lockfilePackages) <- c("name", "packrat.version", "packrat.source")

   # note which packages are in project library
   projectLibrary <- renv:::renv_paths_library(project = project)
   installedPackages[["in.project.library"]] <-
      installedPackages[["library_absolute"]] == projectLibrary

   # merge together
   merge.data.frame(
      x = installedPackages,
      y = lockfilePackages,
      by = "name",
      all.x = TRUE,
      all.y = TRUE
   )
   
})

.rs.addFunction("renv.initCallback", function(repos)
{
   # set active repos
   options(repos = repos)
   
   # avoid timeouts when querying unresponsive R package repositories
   options(renv.config.connect.timeout = 0L)
   options(renv.config.connect.retry = 0L)

   # tell renv that we know what we're doing
   options(renv.consent = TRUE)
   
   # if we're running tests, be quiet during init
   if (!is.na(Sys.getenv("TESTTHAT", unset = NA)))
     return(renv:::quietly(renv::init()))
     
   # otherwise, do a regular init
   renv::init()
})

#' The name of the project's active renv profile, or "" for the default.
.rs.addFunction("renv.activeProfile", function(project)
{
   profile <- Sys.getenv("RENV_PROFILE", unset = "")
   if (!nzchar(profile))
   {
      # renv's activate script reads the profile from this file when the
      # environment doesn't name one
      file <- file.path(project, "renv", "profile")
      contents <- if (file.exists(file))
         .rs.tryCatch(readLines(file, n = 1L, warn = FALSE))

      if (is.character(contents) && length(contents) == 1L)
         profile <- trimws(contents)
   }

   if (identical(profile, "default")) "" else profile
})

#' The path to the project's renv lockfile, found as renv finds it: an
#' RENV_PATHS_LOCKFILE override (a trailing slash names a directory holding
#' renv.lock), else the lockfile of the active profile.
.rs.addFunction("renv.lockfilePath", function(project)
{
   lockfile <- Sys.getenv("RENV_PATHS_LOCKFILE", unset = "")
   if (nzchar(lockfile))
   {
      if (grepl("[/\\\\]$", lockfile))
         lockfile <- paste0(lockfile, "renv.lock")
   }
   else
   {
      profile <- .rs.renv.activeProfile(project)
      lockfile <- if (nzchar(profile))
         file.path("renv", "profiles", profile, "renv.lock")
      else
         "renv.lock"
   }

   if (!.rs.isAbsolutePath(lockfile))
      lockfile <- file.path(project, lockfile)

   lockfile
})

#' The project's renv lockfile, parsed; NULL when it is missing or malformed.
.rs.addFunction("renv.readLockfile", function(project)
{
   path <- .rs.renv.lockfilePath(project)
   if (!utils::file_test("-f", path))
      return(NULL)

   contents <- .rs.tryCatch(paste(readLines(path, warn = FALSE), collapse = "\n"))
   if (inherits(contents, "error"))
      return(NULL)

   lockfile <- .rs.tryCatch(.rs.fromJSON(contents))
   if (inherits(lockfile, "error") || !is.list(lockfile))
      return(NULL)

   lockfile
})

#' The version of R recorded in a parsed lockfile, or "" when it records
#' none (or something other than a version number).
.rs.addFunction("renv.lockfileRVersion", function(lockfile)
{
   version <- lockfile[["R"]][["Version"]]
   if (.rs.rVersionIsValid(version)) version else ""
})

#' Whether a parsed lockfile records any packages to restore.
.rs.addFunction("renv.lockfileHasPackages", function(lockfile)
{
   length(lockfile[["Packages"]]) > 0L
})

#' Whether the project's renv library contains no packages (other than renv
#' itself), as it does right after switching to a different version of R.
.rs.addFunction("renv.projectLibraryIsEmpty", function(project)
{
   library <- .rs.tryCatch(renv:::renv_paths_library(project = project))
   if (inherits(library, "error") || !dir.exists(library))
      return(TRUE)

   packages <- setdiff(list.files(library), "renv")
   length(packages) == 0L
})

#' Compare the version of R a project asks for with the running one.
#'
#' @param project The project directory.
#' @param projectVersion The version of R set in the project file, or "".
#'   It takes precedence over the version in the renv lockfile.
#' @param minimum The oldest version of R this build of RStudio supports.
#' @param renvActive Whether renv is active in the project.
#' @param findInstalled Whether to look for an installed R matching a
#'   mismatched request (only useful where the session can switch R).
#' @param current The version of R in use.
#'
#' @return A list whose 'type' says what to tell the user: "none"; "restore"
#'   when the versions match but the renv library is empty; or "mismatch",
#'   with the 'requested' and 'current' versions, where the request came from
#'   ('source'), whether that R could run ('supported'), and the matching
#'   installation of R, if any ('installed').
.rs.addFunction("projectRVersionCheck", function(project,
                                                 projectVersion,
                                                 minimum,
                                                 renvActive,
                                                 findInstalled,
                                                 current = getRversion())
{
   lockfile <- NULL
   if (nzchar(projectVersion))
   {
      source <- "project"
      requested <- if (.rs.rVersionIsValid(projectVersion)) projectVersion else ""
   }
   else
   {
      source <- "lockfile"
      lockfile <- .rs.renv.readLockfile(project)
      requested <- .rs.renv.lockfileRVersion(lockfile)
   }

   if (!nzchar(requested))
      return(list(type = .rs.scalar("none")))

   current <- as.character(current)
   if (.rs.rVersionMatches(requested, current))
   {
      restore <- identical(source, "lockfile") &&
         renvActive &&
         .rs.renv.lockfileHasPackages(lockfile) &&
         .rs.renv.projectLibraryIsEmpty(project)

      return(list(type = .rs.scalar(if (restore) "restore" else "none")))
   }

   # an installed match is what would run, so its version decides support
   installed <- if (findInstalled) .rs.findInstalledRVersion(requested)
   version <- if (is.null(installed)) requested else as.character(installed$version)

   list(
      type      = .rs.scalar("mismatch"),
      requested = .rs.scalar(requested),
      current   = .rs.scalar(current),
      source    = .rs.scalar(source),
      supported = .rs.scalar(.rs.rVersionSupported(version, minimum)),
      installed = installed
   )
})
