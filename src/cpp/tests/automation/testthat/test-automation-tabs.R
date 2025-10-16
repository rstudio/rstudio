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

.rs.test("Sidebar width is preserved when adding tabs via pane layout options", {
   # Show the sidebar
   remote$commands.execute("toggleSidebar")
   .rs.waitUntil("sidebar created", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane")
   })

   # Focus the splitter element and resize the sidebar using the left-arrow key
   splitter <- remote$js.querySelector("#rstudio_sidebar_column_splitter")
   splitter$focus()
   for (i in 1:6) {
      remote$keyboard.insertText("<Left>")
   }
   Sys.sleep(0.1)

   # Get the sidebar width
   sidebarElement <- remote$js.querySelector("#rstudio_Sidebar_pane")
   initialWidth <- sidebarElement$offsetWidth
   expect_true(initialWidth > 0, "Sidebar should be visible initially")

   # Open pane layout options
   remote$commands.execute("paneLayout")
   remote$dom.waitForElement(".gwt-DialogBox")
   remote$dom.waitForElement("#rstudio_label_pane_layout_options_panel")

   # Add a tab to the sidebar (e.g., "Files")
   # First verify Files is not in the sidebar
   PANE_LAYOUT_SIDEBAR <- "#rstudio_pane_layout_sidebar"

   # Check if "Files" is already in the sidebar (it shouldn't be by default)
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_SIDEBAR, "Files"))

   # Add "Files" to the sidebar
   expect_true(.rs.toggleTab(remote, PANE_LAYOUT_SIDEBAR, "Files"),
               "Should successfully toggle Files into sidebar")

   # Verify Files is now checked in the sidebar
   expect_true(.rs.isTabChecked(remote, PANE_LAYOUT_SIDEBAR, "Files"),
               "Files should be checked in sidebar after toggling")

   # Apply changes by clicking OK button
   remote$dom.clickElement(selector = "#rstudio_preferences_confirm")
   .rs.waitUntil("pane layout dialog closed", function() {
      !remote$dom.elementExists(".gwt-DialogBox")
   })

   # Wait a bit for the sidebar to be refreshed
   Sys.sleep(0.3)

   # Verify sidebar still exists
   expect_true(remote$dom.elementExists("#rstudio_Sidebar_pane"),
               "Sidebar should still exist after adding tab")

   # Verify the width is preserved (within 5% tolerance)
   # The fix ensures that the sidebar width doesn't change when tabs are added
   sidebarElementAfter <- remote$js.querySelector("#rstudio_Sidebar_pane")
   finalWidth <- sidebarElementAfter$offsetWidth
   widthDifference <- abs(finalWidth - initialWidth)
   percentageDifference <- widthDifference / initialWidth

   expect_true(percentageDifference < 0.05,
               paste0("Sidebar width should be preserved when adding tabs. ",
                      "Initial: ", initialWidth, "px, Final: ", finalWidth, "px, ",
                      "Difference: ", round(percentageDifference * 100, 2), "%"))

   # Cleanup: remove the Files tab from the sidebar
   remote$commands.execute("paneLayout")
   remote$dom.waitForElement(".gwt-DialogBox")
   remote$dom.waitForElement("#rstudio_label_pane_layout_options_panel")

   # Remove "Files" from the sidebar
   expect_true(.rs.toggleTab(remote, PANE_LAYOUT_SIDEBAR, "Files"),
               "Should successfully toggle Files off sidebar")

   # Verify Files is now unchecked in the sidebar
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_SIDEBAR, "Files"),
               "Files should be unchecked in sidebar after toggling")

   # Apply changes by clicking OK button
   remote$dom.clickElement(selector = "#rstudio_preferences_confirm")
   .rs.waitUntil("pane layout dialog closed", function() {
      !remote$dom.elementExists(".gwt-DialogBox")
   })

   # Verify the width is preserved (within 5% tolerance)
   # The fix ensures that the sidebar width doesn't change when tabs are removed
   sidebarElementAfter <- remote$js.querySelector("#rstudio_Sidebar_pane")
   finalWidth <- sidebarElementAfter$offsetWidth
   widthDifference <- abs(finalWidth - initialWidth)
   percentageDifference <- widthDifference / initialWidth

   expect_true(percentageDifference < 0.05,
               paste0("Sidebar width should be preserved when removingtabs. ",
                      "Initial: ", initialWidth, "px, Final: ", finalWidth, "px, ",
                      "Difference: ", round(percentageDifference * 100, 2), "%"))

   # Clean up: move sidebar back to the original width
   splitter <- remote$js.querySelector("#rstudio_sidebar_column_splitter")
   splitter$focus()
   for (i in 1:6) {
      remote$keyboard.insertText("<Right>")
   }
   Sys.sleep(0.1)

   # Clean up: hide the sidebar
   remote$commands.execute("toggleSidebar")
   .rs.waitUntil("sidebar removed", function() {
      !remote$dom.elementExists("#rstudio_Sidebar_pane")
   })
})

.rs.test("Sidebar width (left side) is preserved when adding tabs via pane layout options", {
   # Show the sidebar
   remote$commands.execute("toggleSidebar")
   .rs.waitUntil("sidebar created", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane")
   })

   # Move sidebar to the left
   remote$commands.execute("moveSidebarLeft")
   .rs.waitUntil("sidebar moved left", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane")
   })

   # Focus the splitter element and resize the sidebar using the right-arrow key
   splitter <- remote$js.querySelector("#rstudio_sidebar_column_splitter")
   splitter$focus()
   for (i in 1:6) {
      remote$keyboard.insertText("<Right>")
   }
   Sys.sleep(0.1)

   # Get the initial sidebar width
   sidebarElement <- remote$js.querySelector("#rstudio_Sidebar_pane")
   initialWidth <- sidebarElement$offsetWidth
   expect_true(initialWidth > 0, "Sidebar should be visible initially")

   # Open pane layout options
   remote$commands.execute("paneLayout")
   remote$dom.waitForElement(".gwt-DialogBox")
   remote$dom.waitForElement("#rstudio_label_pane_layout_options_panel")

   # Add a tab to the sidebar (e.g., "Environment")
   # First verify Environment is not in the sidebar
   PANE_LAYOUT_SIDEBAR <- "#rstudio_pane_layout_sidebar"

   # Check if "Environment" is already in the sidebar
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_SIDEBAR, "Environment"))

   # Add "Files" to the sidebar
   expect_true(.rs.toggleTab(remote, PANE_LAYOUT_SIDEBAR, "Environment"),
               "Should successfully toggle Environment into sidebar")

   # Verify Environment is now checked in the sidebar
   expect_true(.rs.isTabChecked(remote, PANE_LAYOUT_SIDEBAR, "Environment"),
               "Environment should be checked in sidebar after toggling")

   # Apply changes by clicking OK button
   remote$dom.clickElement(selector = "#rstudio_preferences_confirm")
   .rs.waitUntil("pane layout dialog closed", function() {
      !remote$dom.elementExists(".gwt-DialogBox")
   })

   # Wait a bit for the sidebar to be refreshed
   Sys.sleep(0.3)

   # Verify sidebar still exists
   expect_true(remote$dom.elementExists("#rstudio_Sidebar_pane"),
               "Sidebar should still exist after adding tab")

   # Verify the width is preserved (within 5% tolerance)
   # The fix ensures that the sidebar width doesn't change when tabs are added
   sidebarElementAfter <- remote$js.querySelector("#rstudio_Sidebar_pane")
   finalWidth <- sidebarElementAfter$offsetWidth
   widthDifference <- abs(finalWidth - initialWidth)
   percentageDifference <- widthDifference / initialWidth

   expect_true(percentageDifference < 0.05,
               paste0("Sidebar width should be preserved when adding tabs. ",
                      "Initial: ", initialWidth, "px, Final: ", finalWidth, "px, ",
                      "Difference: ", round(percentageDifference * 100, 2), "%"))

   # Cleanup: remove the Environment tab from the sidebar
   remote$commands.execute("paneLayout")
   remote$dom.waitForElement(".gwt-DialogBox")
   remote$dom.waitForElement("#rstudio_label_pane_layout_options_panel")

   # Remove "Environment" from the sidebar
   expect_true(.rs.toggleTab(remote, PANE_LAYOUT_SIDEBAR, "Environment"),
               "Should successfully toggle Environment off sidebar")

   # Verify Files is now unchecked in the sidebar
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_SIDEBAR, "Environment"),
               "Environment should be unchecked in sidebar after toggling")

   # Apply changes by clicking OK button
   remote$dom.clickElement(selector = "#rstudio_preferences_confirm")
   .rs.waitUntil("pane layout dialog closed", function() {
      !remote$dom.elementExists(".gwt-DialogBox")
   })

   # Verify the width is preserved (within 5% tolerance)
   # The fix ensures that the sidebar width doesn't change when tabs are removed
   sidebarElementAfter <- remote$js.querySelector("#rstudio_Sidebar_pane")
   finalWidth <- sidebarElementAfter$offsetWidth
   widthDifference <- abs(finalWidth - initialWidth)
   percentageDifference <- widthDifference / initialWidth

   expect_true(percentageDifference < 0.05,
               paste0("Sidebar width should be preserved when removingtabs. ",
                      "Initial: ", initialWidth, "px, Final: ", finalWidth, "px, ",
                      "Difference: ", round(percentageDifference * 100, 2), "%"))

   # Clean up: move sidebar back to the original width
   splitter <- remote$js.querySelector("#rstudio_sidebar_column_splitter")
   splitter$focus()
   for (i in 1:6) {
      remote$keyboard.insertText("<Left>")
   }
   Sys.sleep(0.1)

   # Clean up: move sidebar back to the right
   remote$commands.execute("moveSidebarRight")
   .rs.waitUntil("sidebar moved right", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane")
   })

   # Clean up: hide the sidebar
   remote$commands.execute("toggleSidebar")
   .rs.waitUntil("sidebar removed", function() {
      !remote$dom.elementExists("#rstudio_Sidebar_pane")
   })
})
