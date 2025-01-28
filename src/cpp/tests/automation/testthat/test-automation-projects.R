
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())

.rs.test("can create an empty project with git enabled using wizard UI", {

   projectPath <- remote$files.createDirectory()
   projectName <- "MyTestGitProject"

   remote$commands.execute("newProject")
   remote$dom.clickElement("#rstudio_label_new_directory_wizard_page")
   remote$dom.clickElement("#rstudio_label_new_project_wizard_page")
   remote$dom.insertText("#rstudio_new_project_directory_name", projectName)
   remote$dom.clickElement("#rstudio_tbb_button_project_parent")
   remote$dom.clickElement(".gwt-DialogBox button[title=\"Go to directory\"]")
   remote$dom.insertText(".gwt-DialogBox input#rstudio_text_entry", projectPath)
   remote$keyboard.insertText("<Enter>")
   remote$dom.clickElement("#rstudio_file_accept_choose")
   remote$dom.setChecked("#rstudio_new_project_git_repo input")
   remote$dom.clickElement("#rstudio_label_create_project_wizard_confirm")
   
   # Wait until the project has been successfully opened
   .rs.waitUntil("The new project is opened", function()
   {
      grepl(projectName, self$project.getLabel(), fixed = TRUE)
   }, swallowErrors = TRUE)

   expect_equal(projectName, remote$project.getLabel())
   expect_true(remote$dom.elementExists("#rstudio_workbench_tab_git"))

   remote$project.close()
   remote$files.remove(projectPath, recursive = TRUE)
   
})

.rs.test("can create an empty project without git enabled using wizard UI", {

   projectPath <- remote$files.createDirectory()
   projectName <- "MyTestNoGitProject"

   remote$commands.execute("newProject")
   remote$dom.clickElement("#rstudio_label_new_directory_wizard_page")
   remote$dom.clickElement("#rstudio_label_new_project_wizard_page")
   remote$dom.insertText("#rstudio_new_project_directory_name", projectName)
   remote$dom.clickElement("#rstudio_tbb_button_project_parent")
   remote$dom.clickElement(".gwt-DialogBox button[title=\"Go to directory\"]")
   remote$dom.insertText(".gwt-DialogBox input#rstudio_text_entry", projectPath)
   remote$keyboard.insertText("<Enter>")
   remote$dom.clickElement("#rstudio_file_accept_choose")
   remote$dom.setChecked("#rstudio_new_project_git_repo input", checked = FALSE)
   remote$dom.clickElement("#rstudio_label_create_project_wizard_confirm")
   
   # Wait until the project has been successfully opened
   .rs.waitUntil("The new project is opened", function()
   {
      grepl(projectName, self$project.getLabel(), fixed = TRUE)
   }, swallowErrors = TRUE)

   expect_equal(projectName, remote$project.getLabel())
   expect_false(remote$dom.elementExists("#rstudio_workbench_tab_git"))

   remote$project.close()
   remote$files.remove(projectPath)
})

.rs.test("ProjectId is generated only when needed", {
   
   # Create a new, empty project.
   remote$project.create("ProjectId")
   
   # Check the contents of the .Rproj file.
   remote$console.executeExpr({
      contents <- readLines("ProjectId.Rproj")
      any(grepl("^ProjectId", contents))
   })
   
   # We shouldn't have a ProjectId yet.
   output <- remote$console.getOutput()
   expect_equal(tail(output, 1L), "[1] FALSE")
   
   # Try setting a custom user directory, and then restart.
   remote$console.executeExpr({
      dir.create("Data")
      .rs.uiPrefs$projectUserDataDirectory$set(normalizePath("Data"))
   })
   
   # Restart the session.
   remote$session.restart()
   
   # Check the contents of the .Rproj file.
   remote$console.executeExpr({
      contents <- readLines("ProjectId.Rproj")
      any(grepl("^ProjectId", contents))
   })
   
   # We should now have a ProjectId.
   output <- remote$console.getOutput()
   expect_equal(tail(output, 1L), "[1] TRUE")
   
   # Read the project id.
   remote$console.executeExpr({
      contents <- readLines("ProjectId.Rproj")
      writeLines(grep("^ProjectId", contents, value = TRUE))
   })
   projectId <- tail(remote$console.getOutput(), n = 1L)
   
   # Try clearing the project user data directory, and restarting.
   remote$console.executeExpr({
      .rs.uiPrefs$projectUserDataDirectory$clear()
   })
   
   # Restart once more.
   remote$session.restart()
   
   # The old project ID should be preserved.
   remote$console.executeExpr({
      contents <- readLines("ProjectId.Rproj")
      writeLines(grep("^ProjectId", contents, value = TRUE))
   })
   
   newProjectId <- tail(remote$console.getOutput(), n = 1L)
   expect_equal(projectId, newProjectId)
   
   # Clean up.
   remote$project.close()
   
})
