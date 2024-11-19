
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())

.rs.test("can create an empty project in new folder using wizard UI", {

   projectPath <- .rs.automation.tools.createTempFolder(remote)
   projectName <- "MyTestProject"

   remote$commandExecute("newProject")
   remote$clickElement("#rstudio_label_new_directory_wizard_page")
   remote$clickElement("#rstudio_label_new_project_wizard_page")
   remote$enterText("#rstudio_new_project_directory_name", projectName)
   remote$clickElement("#rstudio_tbb_button_project_parent")
   remote$clickElement(".gwt-DialogBox button[title=\"Go to directory\"]")
   remote$enterText(".gwt-DialogBox input#rstudio_text_entry", projectPath)
   remote$keyboardExecute("<Enter>")
   remote$clickElement("#rstudio_file_accept_choose")
   remote$clickElement("#rstudio_new_project_git_repo")
   remote$clickElement("#rstudio_label_create_project_wizard_confirm")
   .rs.automation.tools.waitForProjectToOpen(remote, projectName)

   expect_equal(projectName, .rs.automation.tools.getProjectDropdownLabel(remote))

   remote$projectClose()
   .rs.automation.tools.deleteFolder(remote, projectPath)
})
