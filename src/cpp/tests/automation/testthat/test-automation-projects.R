
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
