# tests related to feature tabs (Console, Environment, History, etc.)

library(testthat)

RSTUDIO_WORKBENCH_TAB_CONSOLE <- "#rstudio_workbench_tab_console"
RSTUDIO_WORKBENCH_TAB_TERMINAL <- "#rstudio_workbench_tab_terminal"
RSTUDIO_WORKBENCH_TAB_BACKGROUND_JOBS <- "#rstudio_workbench_tab_background_jobs"
RSTUDIO_WORKBENCH_TAB_ENVIRONMENT <- "#rstudio_workbench_tab_environment"
RSTUDIO_WORKBENCH_TAB_HISTORY <- "#rstudio_workbench_tab_history"
RSTUDIO_WORKBENCH_TAB_FILES <- "#rstudio_workbench_tab_files"
RSTUDIO_WORKBENCH_TAB_PLOTS <- "#rstudio_workbench_tab_plots"
RSTUDIO_WORKBENCH_TAB_CONNECTIONS <- "#rstudio_workbench_tab_connections"
RSTUDIO_WORKBENCH_TAB_PACKAGES <- "#rstudio_workbench_tab_packages"
RSTUDIO_WORKBENCH_TAB_HELP <- "#rstudio_workbench_tab_help"
RSTUDIO_WORKBENCH_TAB_TUTORIAL <- "#rstudio_workbench_tab_tutorial"
RSTUDIO_WORKBENCH_TAB_VIEWER <- "#rstudio_workbench_tab_viewer"

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())

# Helper function to check if an element exists and is visible
checkElementExistsAndVisible <- function(selector) {
  # Check that the element exists
  elementExists <- remote$dom.elementExists(selector)
  expect_true(elementExists, paste(selector, "element should exist"))
  
  # Check that the element is visible
  element <- remote$js.querySelector(selector)
  expect_true(element$offsetWidth > 0, paste(selector, "should be visible (width > 0)"))
  expect_true(element$offsetHeight > 0, paste(selector, "should be visible (height > 0)"))
}

# Helper function to check if an element is selected (aria-selected="true")
# Returns TRUE if element exists and has aria-selected="true"
# Returns FALSE if element exists and has aria-selected="false" OR if element doesn't exist
isElementSelected <- function(selector) {
  # Check if element exists
  if (!remote$dom.elementExists(selector)) {
    return(FALSE)
  }
  
  # Get the element and check its aria-selected attribute
  element <- remote$js.querySelector(selector)
  ariaSelected <- element$getAttribute("aria-selected")
  
  return(ariaSelected == "true")
}

.rs.test("Core tabs exists and are visible", {
   checkElementExistsAndVisible(RSTUDIO_WORKBENCH_TAB_CONSOLE)
   checkElementExistsAndVisible(RSTUDIO_WORKBENCH_TAB_TERMINAL)
   checkElementExistsAndVisible(RSTUDIO_WORKBENCH_TAB_BACKGROUND_JOBS)
   checkElementExistsAndVisible(RSTUDIO_WORKBENCH_TAB_ENVIRONMENT)
   checkElementExistsAndVisible(RSTUDIO_WORKBENCH_TAB_HISTORY)
   checkElementExistsAndVisible(RSTUDIO_WORKBENCH_TAB_FILES)
   checkElementExistsAndVisible(RSTUDIO_WORKBENCH_TAB_PLOTS)
   checkElementExistsAndVisible(RSTUDIO_WORKBENCH_TAB_CONNECTIONS)
   checkElementExistsAndVisible(RSTUDIO_WORKBENCH_TAB_PACKAGES)
   checkElementExistsAndVisible(RSTUDIO_WORKBENCH_TAB_HELP)
   checkElementExistsAndVisible(RSTUDIO_WORKBENCH_TAB_TUTORIAL)
   checkElementExistsAndVisible(RSTUDIO_WORKBENCH_TAB_VIEWER)
})

.rs.test("Tab selection works correctly", {
   expect_true(isElementSelected(RSTUDIO_WORKBENCH_TAB_ENVIRONMENT))
   expect_false(isElementSelected(RSTUDIO_WORKBENCH_TAB_HISTORY))
   
   # Click on the History tab
   remote$dom.clickElement(selector = RSTUDIO_WORKBENCH_TAB_HISTORY)
   Sys.sleep(0.2)
   expect_false(isElementSelected(RSTUDIO_WORKBENCH_TAB_ENVIRONMENT))
   expect_true(isElementSelected(RSTUDIO_WORKBENCH_TAB_HISTORY))

   # reselect the Environment tab
   remote$dom.clickElement(selector = RSTUDIO_WORKBENCH_TAB_ENVIRONMENT)
   Sys.sleep(0.2)
   expect_true(isElementSelected(RSTUDIO_WORKBENCH_TAB_ENVIRONMENT))

   expect_true(isElementSelected(RSTUDIO_WORKBENCH_TAB_FILES))
   expect_false(isElementSelected(RSTUDIO_WORKBENCH_TAB_PLOTS))
   
   # Click on the Plots tab
   remote$dom.clickElement(selector = RSTUDIO_WORKBENCH_TAB_PLOTS)
   Sys.sleep(0.2)
   expect_false(isElementSelected(RSTUDIO_WORKBENCH_TAB_FILES))
   expect_true(isElementSelected(RSTUDIO_WORKBENCH_TAB_PLOTS))

   # reselect the Files tab
   remote$dom.clickElement(selector = RSTUDIO_WORKBENCH_TAB_FILES)
   Sys.sleep(0.2)
})

