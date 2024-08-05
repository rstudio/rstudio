
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())


test_that("we can test a file in the build pane", {
   
   # Create a package project.
   remote$consoleExecuteExpr({
      projectPath <- tempfile("rstudio", tmpdir = dirname(tempdir()))
      usethis::create_package(path = projectPath, open = FALSE)
      .rs.api.openProject(projectPath)
   })

   # Wait until the new project is open.
   Sys.sleep(1)
   .rs.waitUntil("The new project is opened", function()
   {
      tryCatch({
         jsProjectMenuButton <- remote$jsObjectViaSelector("#rstudio_project_menubutton_toolbar")
         grepl("rstudio", jsProjectMenuButton$innerText)
      }, error = function(e) FALSE)
   })
   
   # Create and open the test file.
   remote$consoleExecuteExpr({
      dir.create("tests/testthat", recursive = TRUE)
      testContents <- .rs.heredoc('
         test_that("we can run a test", {
            expect_equal(2 + 2, 4)
         })
      ')
      writeLines(testContents, con = "tests/testthat/test-example.R")
      .rs.api.documentOpen("tests/testthat/test-example.R")
   })
   
   # Run tests.
   remote$commandExecute("testTestthatFile")
   
   # Wait until we get "Test complete" in the Build pane.
   jsBuildOutput <- remote$jsObjectViaSelector("#rstudio_workbench_panel_build .ace_editor")
   .rs.waitUntil("Tests have finished running", function()
   {
      grepl("\nTest complete\n", jsBuildOutput$innerText, fixed = TRUE)
   })
   
   # Check that it has some color via xtermColor.
   html <- jsBuildOutput$innerHTML
   expect_match(html, "xtermColor")
   
   # Close the project
   remote$domClickElement("#rstudio_project_menubutton_toolbar")
   remote$domClickElement("#rstudio_label_close_project_command")
   
})
