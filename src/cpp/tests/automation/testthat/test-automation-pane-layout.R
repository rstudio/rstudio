library(testthat)

# ids of the 4 quadrants in the Pane Layout options panel
PANE_LAYOUT_LEFT_TOP <- "#rstudio_pane_layout_left_top"
PANE_LAYOUT_LEFT_BOTTOM <- "#rstudio_pane_layout_left_bottom"
PANE_LAYOUT_RIGHT_TOP <- "#rstudio_pane_layout_right_top"
PANE_LAYOUT_RIGHT_BOTTOM <- "#rstudio_pane_layout_right_bottom"
PANE_LAYOUT_SIDEBAR <- "#rstudio_pane_layout_sidebar"

# ids of the 4 dropdowns in the Pane Layout options panel
PANE_LAYOUT_LEFT_TOP_SELECT <- "#rstudio_pane_layout_left_top_select"
PANE_LAYOUT_LEFT_BOTTOM_SELECT <- "#rstudio_pane_layout_left_bottom_select"
PANE_LAYOUT_RIGHT_TOP_SELECT <- "#rstudio_pane_layout_right_top_select"
PANE_LAYOUT_RIGHT_BOTTOM_SELECT <- "#rstudio_pane_layout_right_bottom_select"
PANE_LAYOUT_SIDEBAR_SELECT <- "#rstudio_pane_layout_sidebar_select"

# sidebar visible checkbox
PANE_LAYOUT_SIDEBAR_VISIBLE <- "#rstudio_pane_layout_sidebar_visible"

PANE_LAYOUT_RESET_LINK <- "#rstudio_pane_layout_reset_link"

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())

# --------------------------------------------------------------------------------------------------
# Tests follow...
# --------------------------------------------------------------------------------------------------

.rs.test("Pane Layout dialog displays with correct default quadrant configuration", {
   .rs.openPaneLayoutOptions(remote)

   # Verify all four quadrants are displayed
   expect_true(remote$dom.elementExists(PANE_LAYOUT_LEFT_TOP))
   expect_true(remote$dom.elementExists(PANE_LAYOUT_LEFT_BOTTOM))
   expect_true(remote$dom.elementExists(PANE_LAYOUT_RIGHT_TOP))
   expect_true(remote$dom.elementExists(PANE_LAYOUT_RIGHT_BOTTOM))
   expect_true(remote$dom.elementExists(PANE_LAYOUT_SIDEBAR))

   # Verify default dropdown selections
   sourceText <- .rs.getQuadrantDropdownText(remote, PANE_LAYOUT_LEFT_TOP)
   expect_equal(sourceText, "Source")

   consoleText <- .rs.getQuadrantDropdownText(remote, PANE_LAYOUT_LEFT_BOTTOM)
   expect_equal(consoleText, "Console")

   # Check that TabSet1 dropdown shows correct text. Note that RStudio Pro also has a "Databricks"
   # tab; the test will still pass if there are more tabs than expected.
   expectedTabSet1Tabs <- c("Environment", "History", "Connections", "Build", "VCS", "Tutorial")
   .rs.verifyQuadrantTabs(remote, PANE_LAYOUT_RIGHT_TOP, expectedTabSet1Tabs)

   # Check that TabSet2 dropdown shows correct text.
   expectedTabSet2Tabs <- c("Files", "Plots", "Packages", "Help", "Viewer", "Presentations")
   .rs.verifyQuadrantTabs(remote, PANE_LAYOUT_RIGHT_BOTTOM, expectedTabSet2Tabs)

   # Check that Sidebar dropdown shows correct text
   expectedSidebarTabs <- c("Sidebar on Left")
   .rs.verifyQuadrantTabs(remote, PANE_LAYOUT_SIDEBAR, expectedSidebarTabs)

   # Check that Sidebar visible checkbox is unchecked
   expect_false(remote$dom.isChecked(remote$dom.querySelector(PANE_LAYOUT_SIDEBAR_VISIBLE)))

   # Close dialog
   remote$keyboard.insertText("<Escape>")
})

.rs.test("Quadrant dropdown shows all options with correct checkmarks", {
   .rs.openPaneLayoutOptions(remote)

   expectedTexts <- c("Source", "Console", "Environment", "Files")

   # Test all four quadrant dropdowns
   .rs.verifyQuadrantDropdownOptions(remote, PANE_LAYOUT_LEFT_TOP_SELECT, expectedTexts, 1)
   .rs.verifyQuadrantDropdownOptions(remote, PANE_LAYOUT_LEFT_BOTTOM_SELECT, expectedTexts, 2)
   .rs.verifyQuadrantDropdownOptions(remote, PANE_LAYOUT_RIGHT_TOP_SELECT, expectedTexts, 3)
   .rs.verifyQuadrantDropdownOptions(remote, PANE_LAYOUT_RIGHT_BOTTOM_SELECT, expectedTexts, 4)
   
   # Test sidebar dropdown
   expectedTexts <- c("Sidebar on Left", "Sidebar on Right")
   .rs.verifyQuadrantDropdownOptions(remote, PANE_LAYOUT_SIDEBAR_SELECT, expectedTexts, 1)

   # Close dialog
   remote$keyboard.insertText("<Escape>")
})

.rs.test("Quadrant swapping works correctly", {
    # skipping to cut down run times on CI
   skip_on_ci()

   .rs.openPaneLayoutOptions(remote)

   sourceInitial <- .rs.getQuadrantDropdownText(remote, PANE_LAYOUT_LEFT_TOP)
   consoleInitial <- .rs.getQuadrantDropdownText(remote, PANE_LAYOUT_LEFT_BOTTOM)
   upperRightInitial <- .rs.getQuadrantDropdownText(remote, PANE_LAYOUT_RIGHT_TOP)
   lowerRightInitial <- .rs.getQuadrantDropdownText(remote, PANE_LAYOUT_RIGHT_BOTTOM)
   sidebarInitial <- .rs.getQuadrantDropdownText(remote, PANE_LAYOUT_SIDEBAR)

   # Swap Source and Console by selecting Console in Source dropdown
   .rs.selectDropdownOption(remote, PANE_LAYOUT_LEFT_TOP, consoleInitial)

   # Verify the swap occurred
   upperLeftAfter <- .rs.getQuadrantDropdownText(remote, PANE_LAYOUT_LEFT_TOP)
   lowerLeftAfter <- .rs.getQuadrantDropdownText(remote, PANE_LAYOUT_LEFT_BOTTOM)

   expect_equal(upperLeftAfter, consoleInitial)
   expect_equal(lowerLeftAfter, sourceInitial)

   # Swap TabSet1 and TabSet2 by selecting TabSet2 in TabSet1 dropdown
   .rs.selectDropdownOption(remote, PANE_LAYOUT_RIGHT_TOP, lowerRightInitial)

   # Verify the swap occurred
   upperRightAfter <- .rs.getQuadrantDropdownText(remote, PANE_LAYOUT_RIGHT_TOP)
   lowerRightAfter <- .rs.getQuadrantDropdownText(remote, PANE_LAYOUT_RIGHT_BOTTOM)

   expect_equal(upperRightAfter, lowerRightInitial)
   expect_equal(lowerRightAfter, upperRightInitial)

   # Swap lower-left with upper-right
   .rs.selectDropdownOption(remote, PANE_LAYOUT_LEFT_BOTTOM, upperRightAfter)

   # Swap lower-right with upper-left
   .rs.selectDropdownOption(remote, PANE_LAYOUT_RIGHT_BOTTOM, consoleInitial)

   # Verify the swap occurred and didn't affect the other swap quadrants
   upperRightAfter <- .rs.getQuadrantDropdownText(remote, PANE_LAYOUT_RIGHT_TOP)
   lowerRightAfter <- .rs.getQuadrantDropdownText(remote, PANE_LAYOUT_RIGHT_BOTTOM)
   upperLeftAfter <- .rs.getQuadrantDropdownText(remote, PANE_LAYOUT_LEFT_TOP)
   lowerLeftAfter <- .rs.getQuadrantDropdownText(remote, PANE_LAYOUT_LEFT_BOTTOM)
   sidebarAfter <- .rs.getQuadrantDropdownText(remote, PANE_LAYOUT_SIDEBAR)

   expect_equal(upperRightAfter, sourceInitial)
   expect_equal(lowerRightAfter, consoleInitial)
   expect_equal(upperLeftAfter, upperRightInitial)
   expect_equal(lowerLeftAfter, lowerRightInitial)
   expect_equal(sidebarAfter, sidebarInitial)

   # Close dialog
   remote$keyboard.insertText("<Escape>")
})

.rs.test("TabSet1 displays correct default checked tabs", {
   .rs.openPaneLayoutOptions(remote)

   allTabNames <- c("Environment", "History", "Connections", "Build", "VCS", "Tutorial",
                    "Files", "Plots", "Packages", "Help", "Viewer", "Presentations", "Posit Assistant")
   
   tabStates <- .rs.getTabCheckedState(remote, PANE_LAYOUT_RIGHT_TOP, allTabNames)

   # Default TabSet1 should have these tabs checked
   expect_true(tabStates["Environment"], info = "Environment should be checked in TabSet1")
   expect_true(tabStates["History"], info = "History should be checked in TabSet1")
   expect_true(tabStates["Connections"], info = "Connections should be checked in TabSet1")
   expect_true(tabStates["Build"], info = "Build should be checked in TabSet1")
   expect_true(tabStates["VCS"], info = "VCS should be checked in TabSet1")
   expect_true(tabStates["Tutorial"], info = "Tutorial should be checked in TabSet1")

   # These should be unchecked in TabSet1
   expect_false(tabStates["Files"], info = "Files should be unchecked in TabSet1")
   expect_false(tabStates["Plots"], info = "Plots should be unchecked in TabSet1")
   expect_false(tabStates["Packages"], info = "Packages should be unchecked in TabSet1")
   expect_false(tabStates["Help"], info = "Help should be unchecked in TabSet1")
   expect_false(tabStates["Viewer"], info = "Viewer should be unchecked in TabSet1")
   expect_false(tabStates["Presentations"], info = "Presentations should be unchecked in TabSet1")
   expect_false(tabStates["Posit Assistant"], info = "Posit Assistant should be unchecked in TabSet1")

   # Close dialog
   remote$keyboard.insertText("<Escape>")
})

.rs.test("TabSet2 displays correct default checked tabs", {
   .rs.openPaneLayoutOptions(remote)

   allTabNames <- c("Environment", "History", "Connections", "Build", "VCS", "Tutorial",
                    "Files", "Plots", "Packages", "Help", "Viewer", "Presentations", "Posit Assistant")
   
   tabStates <- .rs.getTabCheckedState(remote, PANE_LAYOUT_RIGHT_BOTTOM, allTabNames)

   # Default TabSet2 should have these tabs checked
   expect_true(tabStates["Files"], info = "Files should be checked in TabSet2")
   expect_true(tabStates["Plots"], info = "Plots should be checked in TabSet2")
   expect_true(tabStates["Packages"], info = "Packages should be checked in TabSet2")
   expect_true(tabStates["Help"], info = "Help should be checked in TabSet2")
   expect_true(tabStates["Viewer"], info = "Viewer should be checked in TabSet2")
   expect_true(tabStates["Presentations"], info = "Presentations should be checked in TabSet2")

   # These should be unchecked in TabSet2
   expect_false(tabStates["Environment"], info = "Environment should be unchecked in TabSet2")
   expect_false(tabStates["History"], info = "History should be unchecked in TabSet2")
   expect_false(tabStates["Connections"], info = "Connections should be unchecked in TabSet2")
   expect_false(tabStates["Build"], info = "Build should be unchecked in TabSet2")
   expect_false(tabStates["VCS"], info = "VCS should be unchecked in TabSet2")
   expect_false(tabStates["Tutorial"], info = "Tutorial should be unchecked in TabSet2")
   expect_false(tabStates["Posit Assistant"], info = "Posit Assistant should be unchecked in TabSet2")

   # Close dialog
   remote$keyboard.insertText("<Escape>")
})

.rs.test("Sidebar displays correct default checked tabs", {
   .rs.openPaneLayoutOptions(remote)

   allTabNames <- c("Environment", "History", "Connections", "Build", "VCS", "Tutorial",
                    "Files", "Plots", "Packages", "Help", "Viewer", "Presentations", "Posit Assistant")

   tabStates <- .rs.getTabCheckedState(remote, PANE_LAYOUT_SIDEBAR, allTabNames)

   # Default Sidebar should have these tabs checked
   expect_true(tabStates["Posit Assistant"], info = "Posit Assistant should be checked in Sidebar")

   # These should be unchecked in Sidebar
   expect_false(tabStates["Files"], info = "Files should be unchecked in Sidebar")
   expect_false(tabStates["Plots"], info = "Plots should be unchecked in Sidebar")
   expect_false(tabStates["Packages"], info = "Packages should be unchecked in TabSet2")
   expect_false(tabStates["Help"], info = "Help should be unchecked in Sidebar")
   expect_false(tabStates["Viewer"], info = "Viewer should be unchecked in Sidebar")
   expect_false(tabStates["Presentations"], info = "Presentations should be unchecked in Sidebar")
   expect_false(tabStates["Environment"], info = "Environment should be unchecked in Sidebar")
   expect_false(tabStates["History"], info = "History should be unchecked in TabSet2")
   expect_false(tabStates["Connections"], info = "Connections should be unchecked in Sidebar")
   expect_false(tabStates["Build"], info = "Build should be unchecked in Sidebar")
   expect_false(tabStates["VCS"], info = "VCS should be unchecked in Sidebar")
   expect_false(tabStates["Tutorial"], info = "Tutorial should be unchecked in Sidebar")

   # Close dialog
   remote$keyboard.insertText("<Escape>")
})

.rs.test("Clicking unchecked tab in one TabSet unchecks it in the other", {
    # skipping to cut down run times on CI
   skip_on_ci()

   .rs.openPaneLayoutOptions(remote)

   # Files is checked in TabSet2 by default, not in TabSet1
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_TOP, "Files"))
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_SIDEBAR, "Files"))
   expect_true(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_BOTTOM, "Files"))

   # Check Files in TabSet1
   expect_true(.rs.toggleTab(remote, PANE_LAYOUT_RIGHT_TOP, "Files"))

   # Verify Files is now checked in TabSet1 and unchecked in TabSet2
   expect_true(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_TOP, "Files"))
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_SIDEBAR, "Files"))
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_BOTTOM, "Files"))

   # Move it back to TabSet2
   expect_true(.rs.toggleTab(remote, PANE_LAYOUT_RIGHT_BOTTOM, "Files"))

   # Verify it's back to original state
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_TOP, "Files"))
   expect_true(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_BOTTOM, "Files"))
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_SIDEBAR, "Files"))

   # Close dialog
   remote$keyboard.insertText("<Escape>")
})

.rs.test("Clicking unchecked tab in TabSet1 unchecks it in the Sidebar", {
    # skipping to cut down run times on CI
   skip_on_ci()

   .rs.openPaneLayoutOptions(remote)

   # Files is checked in TabSet2 by default, not in TabSet1 or Sidebar
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_SIDEBAR, "Files"))
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_TOP, "Files"))
   expect_true(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_BOTTOM, "Files"))

   # Check Files in Sidebar
   expect_true(.rs.toggleTab(remote, PANE_LAYOUT_SIDEBAR, "Files"))

   # Verify Files is now checked in Sidebar and unchecked in TabSet1 and TabSet2
   expect_true(.rs.isTabChecked(remote, PANE_LAYOUT_SIDEBAR, "Files"))
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_TOP, "Files"))
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_BOTTOM, "Files"))

   # Check Files in TabSet1
   expect_true(.rs.toggleTab(remote, PANE_LAYOUT_RIGHT_TOP, "Files"))

   # Verify Files is now checked in TabSet1 and unchecked in Sidebar and TabSet2
   expect_true(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_TOP, "Files"))
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_SIDEBAR, "Files"))
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_BOTTOM, "Files"))

   # Move it back to TabSet2
   expect_true(.rs.toggleTab(remote, PANE_LAYOUT_RIGHT_BOTTOM, "Files"))

   # Verify Files is now checked in TabSet2 and unchecked in Sidebar and TabSet1
   expect_true(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_BOTTOM, "Files"))
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_SIDEBAR, "Files"))
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_TOP, "Files"))

   # Close dialog
   remote$keyboard.insertText("<Escape>")
})

.rs.test("Clicking checked tab unchecks it in both TabSets (hiding the tab)", {
    # skipping to cut down run times on CI
   skip_on_ci()

   .rs.openPaneLayoutOptions(remote)

   # Environment is checked in TabSet1 by default
   expect_true(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_TOP, "Environment"))
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_BOTTOM, "Environment"))

   # Uncheck Environment in TabSet1
   expect_true(.rs.toggleTab(remote, PANE_LAYOUT_RIGHT_TOP, "Environment"))

   # Verify Environment is now unchecked in both TabSets (hidden)
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_TOP, "Environment"))
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_BOTTOM, "Environment"))

   # Check it again in TabSet1 to restore state
   expect_true(.rs.toggleTab(remote, PANE_LAYOUT_RIGHT_TOP, "Environment"))

   # Verify it's checked again
   expect_true(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_TOP, "Environment"))
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_BOTTOM, "Environment"))

   # Close dialog
   remote$keyboard.insertText("<Escape>")
})

.rs.test("All tabs can be moved to one TabSet leaving the other empty", {
    # skipping to cut down run times on CI
   skip_on_ci()

   .rs.openPaneLayoutOptions(remote)

   # Check that TabSet1 and TabSet2 contain expected tabs at the start of the test
   initialTabSet1Tabs <- c("Environment", "History", "Connections", "Build", "VCS", "Tutorial")
   .rs.verifyQuadrantTabs(remote, PANE_LAYOUT_RIGHT_TOP, initialTabSet1Tabs)
   initialTabSet2Tabs <- c("Files", "Plots", "Packages", "Help", "Viewer", "Presentations")
   .rs.verifyQuadrantTabs(remote, PANE_LAYOUT_RIGHT_BOTTOM, initialTabSet2Tabs)

   # Move all TabSet2 tabs to TabSet1
   tabsToMove <- c("Files", "Plots", "Packages", "Help", "Viewer", "Presentations")
   for (i in seq_along(tabsToMove)) {
      .rs.toggleTab(remote, PANE_LAYOUT_RIGHT_TOP, tabsToMove[i])
   }

   # Verify the tabs that should have been moved
   movedTabs <- c("Files", "Plots", "Packages", "Help", "Viewer", "Presentations")
   topState <- .rs.getTabCheckedState(remote, PANE_LAYOUT_RIGHT_TOP, movedTabs)
   bottomState <- .rs.getTabCheckedState(remote, PANE_LAYOUT_RIGHT_BOTTOM, movedTabs)
   for (i in seq_along(movedTabs)) {
      expect_true(topState[i],
                  info = paste(movedTabs[i], "should be checked in TabSet1"))
      expect_false(bottomState[i],
                  info = paste(movedTabs[i], "should not be checked in TabSet2"))
   }

   # Move tabs back
   for (tab in tabsToMove) {
      .rs.toggleTab(remote, PANE_LAYOUT_RIGHT_BOTTOM, tab)
   }

   # Sanity check that the tabs are in the original positions
   .rs.verifyQuadrantTabs(remote, PANE_LAYOUT_RIGHT_TOP, initialTabSet1Tabs)
   .rs.verifyQuadrantTabs(remote, PANE_LAYOUT_RIGHT_BOTTOM, initialTabSet2Tabs)

   # Close dialog
   remote$keyboard.insertText("<Escape>")
})

.rs.test("TabSet quadrants can be swapped while maintaining their tab configurations", {
    # skipping to cut down run times on CI
   skip_on_ci()

   .rs.openPaneLayoutOptions(remote)

   # Get initial TabSet positions
   tabset1Initial <- .rs.getQuadrantDropdownText(remote, PANE_LAYOUT_RIGHT_TOP)
   tabset2Initial <- .rs.getQuadrantDropdownText(remote, PANE_LAYOUT_RIGHT_BOTTOM)

   # Remember which tabs are checked in each
   expect_true(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_TOP, "Environment"))
   expect_true(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_BOTTOM, "Files"))

   # Swap TabSet1 and TabSet2 positions via dropdown
   # This swaps the quadrant positions but should maintain tab assignments
   .rs.selectDropdownOption(remote, PANE_LAYOUT_RIGHT_TOP, tabset2Initial)
   Sys.sleep(0.5)

   # Verify the swap occurred in dropdown
   upperLeftAfter <- .rs.getQuadrantDropdownText(remote, PANE_LAYOUT_RIGHT_TOP)
   lowerLeftAfter <- .rs.getQuadrantDropdownText(remote, PANE_LAYOUT_RIGHT_BOTTOM)

   expect_equal(upperLeftAfter, tabset2Initial)
   expect_equal(lowerLeftAfter, tabset1Initial)

   # Tab configurations should remain with their TabSets
   expect_true(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_BOTTOM, "Environment"))
   expect_true(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_TOP, "Files"))

   # Swap back to restore
   .rs.selectDropdownOption(remote, PANE_LAYOUT_RIGHT_TOP, tabset1Initial)

   # Close dialog
   remote$keyboard.insertText("<Escape>")
})

.rs.test("Reset link restores all pane layout settings to defaults", {
    # skipping to cut down run times on CI
   skip_on_ci()

   .rs.openPaneLayoutOptions(remote)

   # Make sidebar visible
   expect_false(remote$dom.isChecked(remote$dom.querySelector(PANE_LAYOUT_SIDEBAR_VISIBLE)),
                info = "Sidebar should be initially unchecked")
   remote$dom.clickElement(PANE_LAYOUT_SIDEBAR_VISIBLE)
   expect_true(remote$dom.isChecked(remote$dom.querySelector(PANE_LAYOUT_SIDEBAR_VISIBLE)),
               info = "Sidebar should now be checked")

   # Move sidebar to right (away from the default of left)
   .rs.selectDropdownOption(remote, PANE_LAYOUT_SIDEBAR, "Sidebar on Right")
   sidebarPosition <- .rs.getQuadrantDropdownText(remote, PANE_LAYOUT_SIDEBAR)
   expect_equal(sidebarPosition, "Sidebar on Right", info = "Sidebar should be on right")

   # Add some tabs to the sidebar (Files, Environment, History)
   expect_true(.rs.toggleTab(remote, PANE_LAYOUT_SIDEBAR, "Files"))
   expect_true(.rs.toggleTab(remote, PANE_LAYOUT_SIDEBAR, "Environment"))
   expect_true(.rs.toggleTab(remote, PANE_LAYOUT_SIDEBAR, "History"))

   # Verify tabs were added to sidebar
   expect_true(.rs.isTabChecked(remote, PANE_LAYOUT_SIDEBAR, "Files"))
   expect_true(.rs.isTabChecked(remote, PANE_LAYOUT_SIDEBAR, "Environment"))
   expect_true(.rs.isTabChecked(remote, PANE_LAYOUT_SIDEBAR, "History"))

   # Swap some quadrants
   .rs.selectDropdownOption(remote, PANE_LAYOUT_LEFT_TOP, "Console")
   upperLeftAfter <- .rs.getQuadrantDropdownText(remote, PANE_LAYOUT_LEFT_TOP)
   expect_equal(upperLeftAfter, "Console", info = "Upper left should be Console after swap")

   # Now click the reset link
   expect_true(remote$dom.elementExists(PANE_LAYOUT_RESET_LINK), info = "Reset link should exist")
   remote$dom.clickElement(PANE_LAYOUT_RESET_LINK)
   Sys.sleep(0.5)

   # Verify all settings are back to defaults

   # 1. Sidebar should be unchecked (hidden by default, even though Posit Assistant is in sidebar)
   expect_false(remote$dom.isChecked(remote$dom.querySelector(PANE_LAYOUT_SIDEBAR_VISIBLE)),
                info = "Sidebar visible checkbox should be unchecked after reset")

   # 2. Sidebar should be on left (the default)
   sidebarPositionAfter <- .rs.getQuadrantDropdownText(remote, PANE_LAYOUT_SIDEBAR)
   expect_equal(sidebarPositionAfter, "Sidebar on Left",
                info = "Sidebar should be on left after reset")

   # 3. Only Posit Assistant should be checked in Sidebar
   expect_true(.rs.isTabChecked(remote, PANE_LAYOUT_SIDEBAR, "Posit Assistant"),
               info = "Posit Assistant should be checked in Sidebar after reset")
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_SIDEBAR, "Files"),
                info = "Files should not be checked in Sidebar after reset")
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_SIDEBAR, "Environment"),
                info = "Environment should not be checked in Sidebar after reset")
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_SIDEBAR, "History"),
                info = "History should not be checked in Sidebar after reset")

   # 4. Quadrants should be back to defaults
   sourceText <- .rs.getQuadrantDropdownText(remote, PANE_LAYOUT_LEFT_TOP)
   expect_equal(sourceText, "Source", info = "Upper left should be Source after reset")

   consoleText <- .rs.getQuadrantDropdownText(remote, PANE_LAYOUT_LEFT_BOTTOM)
   expect_equal(consoleText, "Console", info = "Lower left should be Console after reset")

   # 5. Verify TabSet1 tabs are back to defaults
   expectedTabSet1Tabs <- c("Environment", "History", "Connections", "Build", "VCS", "Tutorial")
   .rs.verifyQuadrantTabs(remote, PANE_LAYOUT_RIGHT_TOP, expectedTabSet1Tabs)

   # 6. Verify TabSet2 tabs are back to defaults
   expectedTabSet2Tabs <- c("Files", "Plots", "Packages", "Help", "Viewer", "Presentations")
   .rs.verifyQuadrantTabs(remote, PANE_LAYOUT_RIGHT_BOTTOM, expectedTabSet2Tabs)

   # Close dialog
   remote$keyboard.insertText("<Escape>")
})

.rs.test("Sidebar visibility checkbox auto-updates based on tab assignments", {
    # skipping to cut down run times on CI
   skip_on_ci()

   .rs.openPaneLayoutOptions(remote)

   # Verify sidebar visibility checkbox is initially unchecked (default state)
   expect_false(remote$dom.isChecked(remote$dom.querySelector(PANE_LAYOUT_SIDEBAR_VISIBLE)),
                info = "Sidebar should be initially unchecked")

   # Verify sidebar has no visible tabs initially (Posit Assistant is checked but hidden by default)
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_SIDEBAR, "Files"),
                info = "Files should not be in sidebar initially")
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_SIDEBAR, "Environment"),
                info = "Environment should not be in sidebar initially")

   # Check a tab in the sidebar (add Files to sidebar)
   expect_true(.rs.toggleTab(remote, PANE_LAYOUT_SIDEBAR, "Files"))

   # Verify Files is now checked in sidebar
   expect_true(.rs.isTabChecked(remote, PANE_LAYOUT_SIDEBAR, "Files"),
               info = "Files should now be checked in sidebar")

   # Verify sidebar visibility checkbox was automatically checked
   expect_true(remote$dom.isChecked(remote$dom.querySelector(PANE_LAYOUT_SIDEBAR_VISIBLE)),
               info = "Sidebar visibility checkbox should auto-check when adding first visible tab")

   # Add another tab to the sidebar (Environment)
   expect_true(.rs.toggleTab(remote, PANE_LAYOUT_SIDEBAR, "Environment"))
   expect_true(.rs.isTabChecked(remote, PANE_LAYOUT_SIDEBAR, "Environment"),
               info = "Environment should now be checked in sidebar")

   # Sidebar visibility should still be checked
   expect_true(remote$dom.isChecked(remote$dom.querySelector(PANE_LAYOUT_SIDEBAR_VISIBLE)),
               info = "Sidebar visibility should remain checked with multiple tabs")

   # Uncheck one tab (Files) - sidebar should still be visible because Environment remains
   expect_true(.rs.toggleTab(remote, PANE_LAYOUT_SIDEBAR, "Files"))
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_SIDEBAR, "Files"),
                info = "Files should no longer be checked in sidebar")
   expect_true(.rs.isTabChecked(remote, PANE_LAYOUT_SIDEBAR, "Environment"),
               info = "Environment should still be checked in sidebar")

   # Sidebar visibility should still be checked (still has Environment)
   expect_true(remote$dom.isChecked(remote$dom.querySelector(PANE_LAYOUT_SIDEBAR_VISIBLE)),
               info = "Sidebar visibility should remain checked when tabs remain")

   # Uncheck Environment
   expect_true(.rs.toggleTab(remote, PANE_LAYOUT_SIDEBAR, "Environment"))
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_SIDEBAR, "Environment"),
                info = "Environment should no longer be checked in sidebar")

   # Also uncheck Posit Assistant (which is checked by default) to truly empty the sidebar
   expect_true(.rs.toggleTab(remote, PANE_LAYOUT_SIDEBAR, "Posit Assistant"))
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_SIDEBAR, "Posit Assistant"),
                info = "Posit Assistant should no longer be checked in sidebar")

   # Verify sidebar visibility checkbox was automatically unchecked
   expect_false(remote$dom.isChecked(remote$dom.querySelector(PANE_LAYOUT_SIDEBAR_VISIBLE)),
                info = "Sidebar visibility checkbox should auto-uncheck when removing last visible tab")

   # User can still manually check the visibility checkbox to show empty sidebar
   remote$dom.clickElement(PANE_LAYOUT_SIDEBAR_VISIBLE)
   expect_true(remote$dom.isChecked(remote$dom.querySelector(PANE_LAYOUT_SIDEBAR_VISIBLE)),
               info = "User should be able to manually check sidebar visibility for empty sidebar")

   # User can manually uncheck it again
   remote$dom.clickElement(PANE_LAYOUT_SIDEBAR_VISIBLE)
   expect_false(remote$dom.isChecked(remote$dom.querySelector(PANE_LAYOUT_SIDEBAR_VISIBLE)),
                info = "User should be able to manually uncheck sidebar visibility")

   # Close dialog
   remote$keyboard.insertText("<Escape>")
})
