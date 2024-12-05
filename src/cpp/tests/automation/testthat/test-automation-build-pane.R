library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())


.rs.test("we can test a file in the build pane", {

   # TODO: Hangs on CI.
   skip_on_ci()

   # Create a project.
   remote$project.create(type = "package")

   # Create and open the test file.
   remote$console.executeExpr({
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
   remote$commands.execute("testTestthatFile")

   # Wait until we get "Test complete" in the Build pane.
   jsBuildOutput <- remote$js.querySelector("#rstudio_workbench_panel_build .ace_editor")
   .rs.waitUntil("Tests have finished running", function()
   {
      grepl("\nTest complete\n", jsBuildOutput$innerText, fixed = TRUE)
   })

   # Check that it has some color via xtermColor.
   html <- jsBuildOutput$innerHTML
   expect_match(html, "xtermColor")

   # Close the project
   remote$project.close()
})

