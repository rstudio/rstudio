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

.rs.setVar("tutorial.jobs", new.env(parent = emptyenv()))



# JSON RPC ----
.rs.addJsonRpcHandler("tutorial_stop", function() {
   .rs.tutorial.stopTutorial()
})



# Methods ----

.rs.addFunction("tutorial.launchBrowser", function(url)
{
   # if this is a newly-launched tutorial, tag the associated
   # job with the generated URL
   pendingJob <- .rs.getVar("tutorial.pendingJob")
   if (!is.null(pendingJob))
   {
      .rs.tutorial.setTutorialJobUrl(
         pendingJob$package,
         pendingJob$name,
         url
      )
   }
   
   .rs.clearVar("tutorial.pendingJob")
   .rs.invokeShinyTutorialViewer(url)
})

.rs.addFunction("tutorial.getTutorialJob", function(package, name)
{
   key <- paste(package, name, sep = "::")
   .rs.tutorial.jobs[[key]]
})

.rs.addFunction("tutorial.setTutorialJob", function(package, name, id)
{
   key <- paste(package, name, sep = "::")
   .rs.tutorial.jobs[[key]] <- list(id = id)
})

.rs.addFunction("tutorial.setTutorialJobUrl", function(package, name, url)
{
   key <- paste(package, name, sep = "::")
   .rs.tutorial.jobs[[key]][["url"]] <- url
})

.rs.addFunction("tutorial.runTutorial", function(name, package, shiny_args = NULL)
{
   # if we already have a running tutorial, just open the associated URL
   job <- .rs.tutorial.getTutorialJob(package, name)
   if (!is.null(job) && !is.null(job$url))
   {
      url <- job$url
      return(.rs.tutorial.launchBrowser(url))
   }
   
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
   id <- .rs.api.runScriptJob(
      path = path,
      name = paste("Tutorial:", name),
      encoding = "UTF-8"
   )
   
   # set and return job id for caller
   .rs.tutorial.setTutorialJob(package, name, id)
   
   pendingJob <- list(package = package, name = name, id = id)
   .rs.setVar("tutorial.pendingJob", pendingJob)
   
   invisible(id)
   
})

.rs.addFunction("tutorial.stopTutorial", function()
{
   id <- .rs.tutorial.getActiveTutorialId()
   .rs.api.stopJob(id)
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
