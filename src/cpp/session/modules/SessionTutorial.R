#
# SessionTutorial.R
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

# State ----

.rs.setVar("tutorial.registry", new.env(parent = emptyenv()))



# JSON RPC ----
.rs.addJsonRpcHandler("tutorial_started", function(name, package, url)
{
   .rs.tutorial.setRunningTutorialUrl(name, package, url)
})

.rs.addJsonRpcHandler("tutorial_stop", function(name, package)
{
   .rs.tutorial.stopTutorial(name, package)
})



# Methods ----

.rs.addFunction("tutorial.registryKey", function(name, package)
{
   paste(package, name, sep = "::")
})

.rs.addFunction("tutorial.registryGet", function(name, package)
{
   key <- .rs.tutorial.registryKey(name, package)
   .rs.tutorial.registry[[key]]
})

.rs.addFunction("tutorial.registrySet", function(name, package, tutorial)
{
   key <- .rs.tutorial.registryKey(name, package)
   tutorial$package <- package
   tutorial$name <- name
   .rs.tutorial.registry[[key]] <- tutorial
})

.rs.addFunction("tutorial.registryClear", function(name, package)
{
   key <- .rs.tutorial.registryKey(name, package)
   .rs.tutorial.registry[[key]] <- NULL
})

# TODO: local jobs are stopped when the session is suspended, and so running
# tutorials are stopped as well. do we want to take the extra step to allow
# sessions to suspend with running tutorials active?
.rs.addFunction("tutorial.onSuspend", function(path)
{
})

.rs.addFunction("tutorial.onResume", function(path)
{
})

.rs.addFunction("tutorial.launchBrowser", function(url)
{
   tutorial <- .rs.getVar("tutorial.pendingTutorial")
   .rs.clearVar("tutorial.pendingTutorial")
   
   meta <- .rs.scalarListFromList(tutorial)
   .rs.invokeShinyTutorialViewer(url, meta)
})

.rs.addFunction("tutorial.getRunningTutorial", function(name, package)
{
   .rs.tutorial.registryGet(name, package)
})

.rs.addFunction("tutorial.setRunningTutorial", function(name, package, job)
{
   tutorial <- list(name = name, package = package, job = job)
   .rs.tutorial.registrySet(name, package, tutorial)
})

.rs.addFunction("tutorial.setRunningTutorialUrl", function(name, package, url)
{
   tutorial <- .rs.tutorial.registryGet(name, package)
   tutorial$url <- url
   .rs.tutorial.registrySet(name, package, tutorial)
})

.rs.addFunction("tutorial.clearRunningTutorial", function(name, package)
{
   .rs.tutorial.registryClear(name, package)
})

.rs.addFunction("tutorial.openExistingTutorial", function(name, package)
{
   tutorial <- .rs.tutorial.getRunningTutorial(name, package)
   if (is.null(tutorial))
      return(FALSE)

   url <- tutorial$url
   if (is.null(url))
      return(FALSE)   
   
   job <- tutorial$job
   running <- .rs.tryCatch(.Call("rs_isJobRunning", job, PACKAGE = "(embedding)"))
   if (!identical(running, TRUE))
      return(FALSE)
   
   .rs.tutorial.enqueueClientEvent("navigate", list(url = url))
   TRUE
})

.rs.addFunction("tutorial.runTutorial", function(name, package, shiny_args = NULL)
{
   # if we already have a running tutorial, just open the associated URL
   if (.rs.tutorial.openExistingTutorial(name, package))
      return()
   
   # prepare the call to learnr to run the tutorial
   shiny_args$launch.browser <- quote(rstudioapi:::tutorialLaunchBrowser)
   
   call <- substitute(
      
      learnr::run_tutorial(
         name = name,
         package = package,
         shiny_args = shiny_args
      ),
      
      list(
         name = name,
         package = package,
         shiny_args = shiny_args
      )
      
   )
   
   # write to file
   deparsed <- deparse(call)
   path <- tempfile("rstudio-tutorial-", fileext = ".R")
   writeLines(deparsed, con = path)
   
   # run as job
   job <- .rs.api.runScriptJob(
      path = path,
      name = paste("Tutorial:", name),
      encoding = "UTF-8"
   )
   
   # set and return job id for caller
   .rs.tutorial.setRunningTutorial(name, package, job)
   
   pendingTutorial <- list(name = name, package = package, job = job)
   .rs.setVar("tutorial.pendingTutorial", pendingTutorial)
   
   invisible(job)
   
})

.rs.addFunction("tutorial.stopTutorial", function(name, package)
{
   tutorial <- .rs.tutorial.getRunningTutorial(name, package)
   .rs.api.stopJob(tutorial$job)
   .rs.tutorial.clearRunningTutorial(name, package)
   .rs.tutorial.enqueueClientEvent("stop")
})

.rs.addFunction("tutorial.enqueueClientEvent", function(type, data = list())
{
   eventData <- list(type = .rs.scalar(type), data = data)
   .rs.enqueClientEvent("tutorial_command", eventData)
})

.rs.addFunction("tutorial.findTutorials", function(pkgRoot)
{
   tutorialsRoot <- file.path(pkgRoot, "tutorials")
   if (!file.exists(tutorialsRoot))
      return(list())
   
   tutorialDirs <- sort(list.files(tutorialsRoot, full.names = TRUE))
   tutorials <- lapply(tutorialDirs, .rs.tutorial.findTutorialsImpl)
   Filter(Negate(is.null), tutorials)
   
})

.rs.addFunction("tutorial.findTutorialsImpl", function(tutorialDir)
{
   tutorialFiles <- list.files(
      tutorialDir,
      pattern = "[.]Rmd$",
      full.names = TRUE,
      ignore.case = TRUE
   )
   
   if (length(tutorialFiles) == 0)
      return(NULL)
   
   tutorialFile <- tutorialFiles[[1]]
   contents <- readLines(tutorialFile, encoding = "UTF-8", warn = FALSE)
   yaml <- rmarkdown:::parse_yaml_front_matter(contents)
   
   desc <- yaml$description
   if (is.null(desc))
      desc <- ""
   
   list(
      name        = .rs.scalar(basename(tutorialDir)),
      file        = .rs.scalar(tutorialFile),
      title       = .rs.scalar(yaml$title),
      description = .rs.scalar(desc)
   )
})
