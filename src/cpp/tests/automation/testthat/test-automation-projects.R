
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
   remote$project.waitFor(projectName)

   expect_equal(projectName, remote$project.getLabel())
   expect_true(remote$dom.elementExists("#rstudio_workbench_tab_git"))

   remote$project.close()
   remote$files.remove(projectPath)
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
   remote$ensureUnchecked("#rstudio_new_project_git_repo input")
   remote$dom.clickElement("#rstudio_label_create_project_wizard_confirm")
   remote$project.waitFor(projectName)

   expect_equal(projectName, remote$project.getLabel())
   expect_false(remote$dom.elementExists("#rstudio_workbench_tab_git"))

   remote$project.close()
   remote$files.remove(projectPath)
})
