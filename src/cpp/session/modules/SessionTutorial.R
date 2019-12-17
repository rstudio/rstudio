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

# JSON RPC ----
.rs.addJsonRpcHandler("tutorial_stop", function() {
   .rs.tutorial.stopTutorial()
})



# Methods ----

.rs.addFunction("tutorial.setActiveTutorialId", function(id)
{
   .rs.setVar("activeTutorialId", id)
})

.rs.addFunction("tutorial.getActiveTutorialId", function()
{
   .rs.getVar("activeTutorialId")
})

.rs.addFunction("tutorial.runTutorial", function(name, package, shiny_args = NULL)
{
   # TODO: if we already have a running tutorial, stop it?
   
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
   .rs.tutorial.setActiveTutorialId(id)
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
