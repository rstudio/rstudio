
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())


# Scaffold a project containing shiny's bundled '01_hello' example app
# alongside a minimal shinytest2 test file, then open the test file in the
# editor. Returns nothing.
scaffoldShinytest2Project <- function()
{
   remote$project.create("shinytest2-demo", type = "default")

   remote$console.executeExpr({
      examplePath <- file.path(
         system.file("examples", package = "shiny"),
         "01_hello",
         "app.R"
      )
      file.copy(examplePath, "app.R", overwrite = TRUE)

      dir.create("tests/testthat", recursive = TRUE, showWarnings = FALSE)
      writeLines(.rs.heredoc('
         library(shinytest2)

         test_that("01_hello produces stable values", {
            app <- AppDriver$new(name = "hello")
            app$set_inputs(bins = 20)
            app$expect_values()
         })
      '), "tests/testthat/test-shinytest2.R")

      .rs.api.documentOpen("tests/testthat/test-shinytest2.R")
   })
}


.rs.test("shinytest2 test files surface the Shiny test toolbar button", {

   if (!remote$package.isInstalled("shiny"))
      skip("shiny is not installed")

   scaffoldShinytest2Project()
   withr::defer(remote$project.close())

   # The shinytest button only appears when getTestType() returns
   # TestsShinyTest. Without checking AppDriver$new( before test_that(, a
   # shinytest2 file would be classified as plain testthat and this button
   # would never become visible.
   .rs.waitUntil("Shiny test toolbar button appears", function() {
      remote$dom.elementExists("button[title*='shinytest2']")
   })
   
   remote$commands.execute("shinyCompareTest")

   .rs.waitUntil("info dialog appears", function() {
      remote$dom.elementExists("#rstudio_dlg_ok")
   })

   # Dismiss the dialog before exercising the with-diffs branch -- leaving
   # it up makes subsequent UI commands flaky.
   remote$modals.click("ok")
   .rs.waitUntil("info dialog dismissed", function() {
      !remote$dom.elementExists("#rstudio_dlg_ok")
   })

   # The 'with diffs' branch goes through DependencyManager.withTestPackage,
   # which prompts to install shinytest2 if it's missing. Skip rather than
   # block on a modal in unattended runs.
   if (!remote$package.isInstalled("shinytest2"))
      skip("shinytest2 is not installed")

   scaffoldShinytest2Project()
   withr::defer(remote$project.close())

   # Seed a fake pending diff (a '*.new.*' file under _snaps/) so that
   # has_shinytest2_results returns testDirExists = TRUE. Then stub
   # testthat::snapshot_review so the command captures its call rather than
   # actually launching the diffviewer Shiny app.
   remote$console.executeExpr({
      dir.create("tests/testthat/_snaps/hello", recursive = TRUE, showWarnings = FALSE)
      file.create("tests/testthat/_snaps/hello/snapshot.new.png")

      ns <- asNamespace("testthat")
      .rs.original.snapshot_review <- ns$snapshot_review
      unlockBinding("snapshot_review", ns)
      ns$snapshot_review <- function(files = NULL, path = "tests/testthat", ...) {
         cat("CAPTURED snapshot_review path=", path, "\n", sep = "")
         invisible()
      }
      lockBinding("snapshot_review", ns)
   })

   withr::defer({
      remote$console.executeExpr({
         ns <- asNamespace("testthat")
         unlockBinding("snapshot_review", ns)
         ns$snapshot_review <- .rs.original.snapshot_review
         lockBinding("snapshot_review", ns)
      })
   })

   remote$commands.execute("shinyCompareTest")

   .rs.waitUntil("snapshot_review is invoked with a named path argument", function() {
      output <- remote$console.getOutput()
      any(grepl("CAPTURED snapshot_review path=.*tests/testthat$", output))
   })
   
})
