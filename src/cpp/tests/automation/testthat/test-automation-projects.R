
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())

.rs.test("can create an empty project with git enabled using wizard UI", {

   projectPath <- remote$createTempFolder()
   projectName <- "MyTestGitProject"

   remote$commandExecute("newProject")
   remote$clickElement("#rstudio_label_new_directory_wizard_page")
   remote$clickElement("#rstudio_label_new_project_wizard_page")
   remote$enterText("#rstudio_new_project_directory_name", projectName)
   remote$clickElement("#rstudio_tbb_button_project_parent")
   remote$clickElement(".gwt-DialogBox button[title=\"Go to directory\"]")
   remote$enterText(".gwt-DialogBox input#rstudio_text_entry", projectPath)
   remote$keyboardExecute("<Enter>")
   remote$clickElement("#rstudio_file_accept_choose")
   remote$ensureChecked("#rstudio_new_project_git_repo")
   remote$clickElement("#rstudio_label_create_project_wizard_confirm")
   remote$ide.waitForProjectToOpen(projectName)

   expect_equal(projectName, remote$getProjectDropdownLabel())
   expect_true(remote$elementExists("#rstudio_workbench_tab_git"))

   remote$projectClose()
   remote$deleteFolder(projectPath)
})

.rs.test("can create an empty project without git enabled using wizard UI", {

   projectPath <- remote$createTempFolder()
   projectName <- "MyTestNoGitProject"

   remote$commandExecute("newProject")
   remote$clickElement("#rstudio_label_new_directory_wizard_page")
   remote$clickElement("#rstudio_label_new_project_wizard_page")
   remote$enterText("#rstudio_new_project_directory_name", projectName)
   remote$clickElement("#rstudio_tbb_button_project_parent")
   remote$clickElement(".gwt-DialogBox button[title=\"Go to directory\"]")
   remote$enterText(".gwt-DialogBox input#rstudio_text_entry", projectPath)
   remote$keyboardExecute("<Enter>")
   remote$clickElement("#rstudio_file_accept_choose")
   remote$ensureUnchecked("#rstudio_new_project_git_repo")
   remote$clickElement("#rstudio_label_create_project_wizard_confirm")
   remote$ide.waitForProjectToOpen(projectName)

   expect_equal(projectName, remote$getProjectDropdownLabel())
   expect_false(remote$elementExists("#rstudio_workbench_tab_git"))

   remote$projectClose()
   remote$deleteFolder(projectPath)
})
