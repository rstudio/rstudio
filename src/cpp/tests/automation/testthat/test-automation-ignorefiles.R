
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())

.rs.test(".positai is added to .gitignore only after the directory exists", {

   parentPath <- remote$files.createDirectory()
   withr::defer({
      try(remote$project.close(), silent = TRUE)
      try(remote$files.remove(parentPath, recursive = TRUE), silent = TRUE)
   })

   projectName <- "PositaiIgnoreTest"
   projectPath <- file.path(parentPath, projectName)
   gitignorePath <- file.path(projectPath, ".gitignore")

   # Create a new directory project with git enabled
   remote$commands.execute("newProject")
   remote$dom.clickElement("#rstudio_label_new_directory_wizard_page")
   remote$dom.clickElement("#rstudio_label_new_project_wizard_page")
   remote$dom.insertText("#rstudio_new_project_directory_name", projectName)
   remote$dom.clickElement("#rstudio_tbb_button_project_parent")
   remote$dom.clickElement(".gwt-DialogBox button[title=\"Go to directory\"]")
   remote$dom.insertText(".gwt-DialogBox input#rstudio_text_entry", parentPath)
   remote$keyboard.insertText("<Enter>")
   remote$dom.clickElement("#rstudio_file_accept_choose")
   remote$dom.setChecked("#rstudio_new_project_git_repo input")
   remote$dom.clickElement("#rstudio_label_create_project_wizard_confirm")

   .rs.waitUntil("project opened", function() {
      grepl(projectName, self$project.getLabel(), fixed = TRUE)
   }, swallowErrors = TRUE)

   # Before the fix, .positai was unconditionally added to .gitignore at
   # project open. Now it should only be added when the directory exists.
   .rs.waitUntil(".gitignore created", function() file.exists(gitignorePath))
   expect_false(any(grepl("^\\.positai$", readLines(gitignorePath))))

   # Create .positai mid-session via the rsession itself. The file monitor
   # should detect it and invoke augmentGitIgnore.
   remote$console.executeExpr({
      dir.create(".positai")
   })

   .rs.waitUntil(".positai added to .gitignore", function() {
      any(grepl("^\\.positai$", readLines(gitignorePath)))
   })
})
