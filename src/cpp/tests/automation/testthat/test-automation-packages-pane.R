# Tests for the Packages pane, including checkbox functionality
# https://github.com/rstudio/rstudio/issues/16842

library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())


RSTUDIO_WORKBENCH_TAB_PACKAGES <- "#rstudio_workbench_tab_packages"
RSTUDIO_WORKBENCH_PANEL_PACKAGES <- "#rstudio_workbench_panel_packages"

# Helper function to click the Packages tab and wait for it to be visible
.rs.packages.selectTab <- function(remote) {
   remote$dom.clickElement(RSTUDIO_WORKBENCH_TAB_PACKAGES)
   .rs.waitUntil("packages panel visible", function() {
      panel <- remote$js.querySelector(RSTUDIO_WORKBENCH_PANEL_PACKAGES)
      !is.null(panel) && panel$offsetWidth > 0 && panel$offsetHeight > 0
   })
}

# Helper function to find a package checkbox by package name
# Returns the checkbox element or NULL if not found
.rs.packages.findCheckbox <- function(remote, packageName) {
   selector <- sprintf(
      "%s input[type='checkbox'][aria-label='%s']",
      RSTUDIO_WORKBENCH_PANEL_PACKAGES,
      packageName
   )
   remote$js.querySelector(selector)
}

# Helper function to check if a package checkbox is checked
.rs.packages.isChecked <- function(remote, packageName) {
   checkbox <- .rs.packages.findCheckbox(remote, packageName)
   if (is.null(checkbox)) return(NA)
   isTRUE(checkbox$checked)
}

# Helper function to click a package checkbox
# Scrolls the checkbox into view before clicking to handle packages not currently visible
.rs.packages.clickCheckbox <- function(remote, packageName) {
   selector <- sprintf(
      "%s input[type='checkbox'][aria-label='%s']",
      RSTUDIO_WORKBENCH_PANEL_PACKAGES,
      packageName
   )

   # Scroll the checkbox into view first
   jsCode <- sprintf(
      "document.querySelector(\"%s\").scrollIntoView({block: 'center', inline: 'nearest'})",
      selector
   )
   remote$js.eval(jsCode)
   Sys.sleep(0.1)  # Small delay for scroll to complete

   # Now click the checkbox
   remote$dom.clickElement(selector)
}

# Helper function to wait for package state to update
.rs.packages.waitForState <- function(remote, packageName, expectedState, waitTimeSecs = 5) {
   .rs.waitUntil(
      sprintf("package %s checkbox state is %s", packageName, expectedState),
      function() {
         .rs.packages.isChecked(remote, packageName) == expectedState
      },
      waitTimeSecs = waitTimeSecs
   )
}


.rs.test("packages pane checkbox reflects loaded state (non-renv)", {

   # Use a common package that should be installed but likely not loaded
   testPackage <- "MASS"
   remote$console.executeExpr({
      if (!.rs.isPackageInstalled(!!testPackage))
         install.packages(!!testPackage)
   })

   # Ensure the package is not currently attached
   remote$console.executeExpr({
      pkg <- "MASS"
      if (paste0("package:", pkg) %in% search()) {
         detach(paste0("package:", pkg), character.only = TRUE)
      }
   })
   Sys.sleep(0.5)

   # Select the Packages tab
   .rs.packages.selectTab(remote)
   Sys.sleep(0.5)

   # Verify the checkbox exists and is unchecked
   checkbox <- .rs.packages.findCheckbox(remote, testPackage)
   expect_false(is.null(checkbox), info = paste("Checkbox for", testPackage, "should exist"))
   expect_false(checkbox$checked, info = paste(testPackage, "should not be attached initially"))

   # Click the checkbox to load the package
   .rs.packages.clickCheckbox(remote, testPackage)

   # Wait for the checkbox state to update
   .rs.packages.waitForState(remote, testPackage, TRUE)

   # Verify the checkbox is now checked
   expect_true(
      .rs.packages.isChecked(remote, testPackage),
      info = paste("Checkbox should be checked after loading", testPackage)
   )

   # Verify the package is actually loaded
   remote$console.executeExpr({
      paste0("package:MASS") %in% search()
   })
   output <- remote$console.getOutput()
   expect_equal(tail(output, 1L), "[1] TRUE", info = "MASS should be attached")

   # Click the checkbox again to unload the package
   .rs.packages.clickCheckbox(remote, testPackage)

   # Wait for the checkbox state to update
   .rs.packages.waitForState(remote, testPackage, FALSE)

   # Verify the checkbox is now unchecked
   expect_false(
      .rs.packages.isChecked(remote, testPackage),
      info = paste("Checkbox should be unchecked after unloading", testPackage)
   )

   # Verify the package is actually unloaded
   remote$console.executeExpr({
      paste0("package:MASS") %in% search()
   })
   output <- remote$console.getOutput()
   expect_equal(tail(output, 1L), "[1] FALSE", info = "MASS should be detached")

})


.rs.test("packages pane checkbox reflects loaded state (renv project with cache)", {

   # Skip on CI as this test requires renv to be installed and takes time
   skip_on_ci()

   # Check if renv is installed
   remote$console.executeExpr({
      .rs.isPackageInstalled("renv")
   })
   output <- remote$console.getOutput()
   renvInstalled <- grepl("TRUE", tail(output, 1L))

   if (!renvInstalled) {
      skip("renv is not installed")
   }

   # Create a new project with renv
   projectPath <- remote$files.createDirectory()
   projectName <- "RenvTestProject"
   fullProjectPath <- file.path(projectPath, projectName)

   # Create project directory and initialize renv
   remote$console.executeExpr({
      dir.create(!!fullProjectPath, recursive = TRUE)

      # Create a minimal DESCRIPTION file so renv can identify dependencies
      writeLines(c(
         "Type: Project",
         "Description: Test project for packages pane"
      ), file.path(!!fullProjectPath, "DESCRIPTION"))

      # Create .Rproj file
      writeLines(c(
         "Version: 1.0",
         "RestoreWorkspace: No",
         "SaveWorkspace: No"
      ), file.path(!!fullProjectPath, paste0(!!projectName, ".Rproj")))
   })

   # Open the project
   remote$console.executeExpr({
      .rs.api.openProject(!!fullProjectPath)
   })

   # Wait for project to open
   .rs.waitUntil("project opened", function() {
      grepl(projectName, remote$project.getLabel(), fixed = TRUE)
   }, waitTimeSecs = 30)

   # Initialize renv with cache enabled (default)
   remote$console.execute("renv::init(bare = TRUE)")

   # Wait for renv initialization to complete
   .rs.waitUntil("renv initialized", function() {
      remote$console.executeExpr({
         file.exists("renv.lock")
      })
      output <- remote$console.getOutput()
      grepl("TRUE", tail(output, 1L))
   }, waitTimeSecs = 60)

   # Install a test package into the renv library
   # Using a small package that's likely available
   testPackage <- "mime"
   remote$console.execute(sprintf("renv::install('%s')", testPackage))

   # Wait for installation to complete
   .rs.waitUntil("package installed", function() {
      remote$console.executeExpr({
         .rs.isPackageInstalled(!!testPackage)
      })
      output <- remote$console.getOutput()
      grepl("TRUE", tail(output, 1L))
   }, waitTimeSecs = 120)

   # Ensure the package is not attached
   remote$console.executeExpr({
      pkgSearch <- paste0("package:", !!testPackage)
      if (pkgSearch %in% search()) {
         detach(pkgSearch, character.only = TRUE, unload = TRUE)
      }
   })
   Sys.sleep(0.5)

   # Refresh the packages pane
   remote$commands.execute("refreshPackages")
   Sys.sleep(1)

   # Select the Packages tab
   .rs.packages.selectTab(remote)
   Sys.sleep(0.5)

   # Verify the checkbox exists and is unchecked
   checkbox <- .rs.packages.findCheckbox(remote, testPackage)
   expect_false(is.null(checkbox), info = paste("Checkbox for", testPackage, "should exist"))
   expect_false(checkbox$checked, info = paste(testPackage, "should not be attached initially"))

   # Click the checkbox to load the package
   .rs.packages.clickCheckbox(remote, testPackage)

   # Wait for the checkbox state to update
   # This is the key test - with renv cache, the checkbox should update correctly
   .rs.packages.waitForState(remote, testPackage, TRUE, waitTimeSecs = 10)

   # Verify the checkbox is now checked
   expect_true(
      .rs.packages.isChecked(remote, testPackage),
      info = paste("Checkbox should be checked after loading", testPackage, "(renv with cache)")
   )

   # Verify the package is actually loaded
   remote$console.executeExpr({
      paste0("package:", !!testPackage) %in% search()
   })
   output <- remote$console.getOutput()
   expect_equal(tail(output, 1L), "[1] TRUE", info = paste(testPackage, "should be attached"))

   # Click the checkbox again to unload the package
   .rs.packages.clickCheckbox(remote, testPackage)

   # Wait for the checkbox state to update
   .rs.packages.waitForState(remote, testPackage, FALSE, waitTimeSecs = 10)

   # Verify the checkbox is now unchecked
   expect_false(
      .rs.packages.isChecked(remote, testPackage),
      info = paste("Checkbox should be unchecked after unloading", testPackage, "(renv with cache)")
   )

   # Verify the package is actually unloaded
   remote$console.executeExpr({
      paste0("package:", !!testPackage) %in% search()
   })
   output <- remote$console.getOutput()
   expect_equal(tail(output, 1L), "[1] FALSE", info = paste(testPackage, "should be detached"))

   # Clean up - close project and remove files
   remote$project.close()
   remote$files.remove(projectPath, recursive = TRUE)

})


.rs.test("packages pane shows correct initial checkbox state", {

   # Test that when a package is already loaded, the checkbox reflects that
   testPackage <- "stats"  # stats is always loaded

   # Select the Packages tab
   .rs.packages.selectTab(remote)
   Sys.sleep(0.5)

   # stats should always be attached (it's a base package)
   checkbox <- .rs.packages.findCheckbox(remote, testPackage)
   expect_false(is.null(checkbox), info = paste("Checkbox for", testPackage, "should exist"))
   expect_true(checkbox$checked, info = paste(testPackage, "should be shown as attached"))

})
