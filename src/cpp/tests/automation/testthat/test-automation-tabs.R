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

PANE_LAYOUT_SIDEBAR_VISIBLE <- "#rstudio_pane_layout_sidebar_visible"

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
   # skipping to cut down run times on CI
   skip_on_ci()

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

   # Keep the sidebar visible despite having no tabs
   remote$dom.clickElement(PANE_LAYOUT_SIDEBAR_VISIBLE)
   expect_true(remote$dom.isChecked(remote$dom.querySelector(PANE_LAYOUT_SIDEBAR_VISIBLE)),
               info = "Sidebar should now be checked")

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

   .rs.resetUILayout(remote)
})

.rs.test("Sidebar width (left side) is preserved when adding tabs via pane layout options", {
   # skipping to cut down run times on CI
   skip_on_ci()

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

   # Keep the sidebar visible despite having no tabs
   remote$dom.clickElement(PANE_LAYOUT_SIDEBAR_VISIBLE)
   expect_true(remote$dom.isChecked(remote$dom.querySelector(PANE_LAYOUT_SIDEBAR_VISIBLE)),
               info = "Sidebar should now be checked")

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

   .rs.resetUILayout(remote)
})

.rs.test("layoutZoomEnvironment zooms environment pane and toggles back", {
   # skipping to cut down run times on CI
   skip_on_ci()

   # Verify Environment panel exists and is visible
   expect_true(remote$dom.elementExists("#rstudio_workbench_panel_environment"),
               "Environment panel should exist")

   # Get initial dimensions of Environment panel, Console, and TabSet2
   environmentPanel <- remote$js.querySelector("#rstudio_workbench_panel_environment")
   consoleElement <- remote$js.querySelector("#rstudio_Console_pane")
   tabSet2Element <- remote$js.querySelector("#rstudio_TabSet2_pane")

   initialEnvWidth <- environmentPanel$offsetWidth
   initialEnvHeight <- environmentPanel$offsetHeight
   initialConsoleWidth <- consoleElement$offsetWidth
   initialTabSet2Width <- tabSet2Element$offsetWidth
   initialTabSet2Height <- tabSet2Element$offsetHeight

   expect_true(initialEnvWidth > 0, "Environment panel should be visible initially")
   expect_true(initialEnvHeight > 0, "Environment panel should be visible initially")
   expect_true(initialConsoleWidth > 0, "Console should be visible initially")
   expect_true(initialTabSet2Width > 0, "TabSet2 should be visible initially")
   expect_true(initialTabSet2Height > 0, "TabSet2 should be visible initially")

   # Execute layoutZoomEnvironment command to zoom the environment pane
   remote$commands.execute("layoutZoomEnvironment")

   # Wait for layout to change - environment should expand significantly
   .rs.waitUntil("environment zoomed", function() {
      envPanel <- remote$js.querySelector("#rstudio_workbench_panel_environment")
      consoleElem <- remote$js.querySelector("#rstudio_Console_pane")
      # Environment should expand significantly and console should collapse
      envPanel$offsetWidth > initialEnvWidth * 1.5 &&
         consoleElem$offsetWidth < 50
   })

   # Add a small delay to ensure layout is fully settled
   Sys.sleep(0.2)

   # Check that layoutZoomEnvironment command is checked
   envZoomChecked <- remote$js.eval("window.rstudioCallbacks.commandIsChecked('layoutZoomEnvironment')")
   expect_true(envZoomChecked, "layoutZoomEnvironment should be checked after zooming")

   # Get zoomed dimensions
   environmentPanelZoomed <- remote$js.querySelector("#rstudio_workbench_panel_environment")
   consoleElementZoomed <- remote$js.querySelector("#rstudio_Console_pane")
   tabSet2ElementZoomed <- remote$js.querySelector("#rstudio_TabSet2_pane")

   zoomedEnvWidth <- environmentPanelZoomed$offsetWidth
   zoomedEnvHeight <- environmentPanelZoomed$offsetHeight
   zoomedConsoleWidth <- consoleElementZoomed$offsetWidth
   zoomedTabSet2Width <- tabSet2ElementZoomed$offsetWidth
   zoomedTabSet2Height <- tabSet2ElementZoomed$offsetHeight

   # Verify environment panel expanded to fill the UI
   expect_true(zoomedEnvWidth > initialEnvWidth * 1.5,
               paste0("Environment panel should expand significantly in width. ",
                      "Initial: ", initialEnvWidth, ", Zoomed: ", zoomedEnvWidth))

   expect_true(zoomedEnvHeight > initialEnvHeight * 1.5,
               paste0("Environment panel should expand significantly in height. ",
                      "Initial: ", initialEnvHeight, ", Zoomed: ", zoomedEnvHeight))

   # Verify Console (left column) collapsed to minimum width
   expect_true(zoomedConsoleWidth < 50,
               paste0("Console should collapse to minimum width when environment zoomed. ",
                      "Width: ", zoomedConsoleWidth))

   # Verify TabSet2 collapsed to minimum (check both width and height as it could collapse either way)
   expect_true(zoomedTabSet2Width < 50 || zoomedTabSet2Height < 50,
               paste0("TabSet2 should collapse to minimum size when environment zoomed. ",
                      "Width: ", zoomedTabSet2Width, ", Height: ", zoomedTabSet2Height))

   # Execute layoutZoomEnvironment command again to toggle back to previous state
   remote$commands.execute("layoutZoomEnvironment")

   # Wait for layout to be restored
   .rs.waitUntil("environment unzoomed", function() {
      envPanel <- remote$js.querySelector("#rstudio_workbench_panel_environment")
      consoleElem <- remote$js.querySelector("#rstudio_Console_pane")
      tabSet2Elem <- remote$js.querySelector("#rstudio_TabSet2_pane")
      # Environment should shrink back, Console should expand, TabSet2 should expand
      envPanel$offsetWidth < zoomedEnvWidth * 0.75 &&
         consoleElem$offsetWidth > 50 &&
         (tabSet2Elem$offsetWidth > 50 || tabSet2Elem$offsetHeight > 50)
   })

   # Add a small delay to ensure layout is fully settled
   Sys.sleep(0.2)

   # Check that layoutZoomEnvironment command is unchecked after toggling back
   envZoomChecked <- remote$js.eval("window.rstudioCallbacks.commandIsChecked('layoutZoomEnvironment')")
   expect_false(envZoomChecked, "layoutZoomEnvironment should be unchecked after toggling back")

   # Get restored dimensions
   environmentPanelRestored <- remote$js.querySelector("#rstudio_workbench_panel_environment")
   consoleElementRestored <- remote$js.querySelector("#rstudio_Console_pane")
   tabSet2ElementRestored <- remote$js.querySelector("#rstudio_TabSet2_pane")

   restoredEnvWidth <- environmentPanelRestored$offsetWidth
   restoredEnvHeight <- environmentPanelRestored$offsetHeight
   restoredConsoleWidth <- consoleElementRestored$offsetWidth
   restoredTabSet2Width <- tabSet2ElementRestored$offsetWidth
   restoredTabSet2Height <- tabSet2ElementRestored$offsetHeight

   # Verify layout restored to approximately original sizes (within 10% tolerance)
   expect_true(abs(restoredEnvWidth - initialEnvWidth) / initialEnvWidth < 0.1,
               paste0("Environment panel width should be restored. ",
                      "Initial: ", initialEnvWidth, ", Restored: ", restoredEnvWidth))

   expect_true(abs(restoredEnvHeight - initialEnvHeight) / initialEnvHeight < 0.1,
               paste0("Environment panel height should be restored. ",
                      "Initial: ", initialEnvHeight, ", Restored: ", restoredEnvHeight))

   expect_true(abs(restoredConsoleWidth - initialConsoleWidth) / initialConsoleWidth < 0.1,
               paste0("Console width should be restored. ",
                      "Initial: ", initialConsoleWidth, ", Restored: ", restoredConsoleWidth))

   expect_true(abs(restoredTabSet2Width - initialTabSet2Width) / initialTabSet2Width < 0.1,
               paste0("TabSet2 width should be restored. ",
                      "Initial: ", initialTabSet2Width, ", Restored: ", restoredTabSet2Width))

   expect_true(abs(restoredTabSet2Height - initialTabSet2Height) / initialTabSet2Height < 0.1,
               paste0("TabSet2 height should be restored. ",
                      "Initial: ", initialTabSet2Height, ", Restored: ", restoredTabSet2Height))

   # Close the untitled source document that is created in this scenario
   remote$commands.execute("closeAllSourceDocs")
   Sys.sleep(0.5)

   .rs.resetUILayout(remote)
})
