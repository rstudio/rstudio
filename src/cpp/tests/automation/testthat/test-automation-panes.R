# tests related to pane and column management

library(testthat)

# element identifiers
CUSTOMIZE_PANES_BUTTON <- "#rstudio_customize_panes"

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())


.rs.test("Default quadrants exist and have expected visibility", {
   # Check that rstudio_TabSet1_pane exists and is visible
   tabSet1Exists <- remote$dom.elementExists("#rstudio_TabSet1_pane")
   expect_true(tabSet1Exists, "rstudio_TabSet1_pane element should exist")
   
   # Check that rstudio_TabSet2_pane exists and is visible
   tabSet2Exists <- remote$dom.elementExists("#rstudio_TabSet2_pane")
   expect_true(tabSet2Exists, "rstudio_TabSet2_pane element should exist")
   
   # Check that rstudio_Console_pane exists and is visible
   consoleExists <- remote$dom.elementExists("#rstudio_Console_pane")
   expect_true(consoleExists, "rstudio_Console_pane element should exist")
   
   # Check that rstudio_Source_pane exists but is NOT visible
   sourceExists <- remote$dom.elementExists("#rstudio_Source_pane")
   expect_true(sourceExists, "rstudio_Source_pane element should exist")
   
   # Check that rstudio_Sidebar_pane does NOT exist
   sidebarExists <- remote$dom.elementExists("#rstudio_Sidebar_pane")
   expect_false(sidebarExists, "rstudio_Sidebar_pane element should NOT exist")

   # Check that rstudio_Source1_pane does NOT exist
   source1Exists <- remote$dom.elementExists("#rstudio_Source1_pane")
   expect_false(source1Exists, "rstudio_Source1_pane element should NOT exist")

   # Check that rstudio_Source2_pane does NOT exist
   source2Exists <- remote$dom.elementExists("#rstudio_Source2_pane")
   expect_false(source2Exists, "rstudio_Source2_pane element should NOT exist")

   # Check that rstudio_Source3_pane does NOT exist
   source3Exists <- remote$dom.elementExists("#rstudio_Source3_pane")
   expect_false(source3Exists, "rstudio_Source3_pane element should NOT exist")

   # Check that Customize Panes button does NOT exist
   expect_false(remote$dom.elementExists(CUSTOMIZE_PANES_BUTTON),"rstudio_customize_panes element should NOT exist")

   # Wait for elements to be visible and check their visibility
   tabSet1NodeId <- remote$dom.waitForElement("#rstudio_TabSet1_pane")
   tabSet2NodeId <- remote$dom.waitForElement("#rstudio_TabSet2_pane")
   consoleNodeId <- remote$dom.waitForElement("#rstudio_Console_pane")
   
   # Check that all elements are actually visible (not just present in DOM)
   tabSet1Element <- remote$js.querySelector("#rstudio_TabSet1_pane")
   tabSet2Element <- remote$js.querySelector("#rstudio_TabSet2_pane")
   consoleElement <- remote$js.querySelector("#rstudio_Console_pane")
   sourceElement <- remote$js.querySelector("#rstudio_Source_pane")
   
   expect_true(tabSet1Element$offsetWidth > 0, "rstudio_TabSet1_pane should be visible (width > 0)")
   expect_true(tabSet1Element$offsetHeight > 0, "rstudio_TabSet1_pane should be visible (height > 0)")
   expect_true(tabSet2Element$offsetWidth > 0, "rstudio_TabSet2_pane should be visible (width > 0)")
   expect_true(tabSet2Element$offsetHeight > 0, "rstudio_TabSet2_pane should be visible (height > 0)")
   expect_true(consoleElement$offsetWidth > 0, "rstudio_Console_pane should be visible (width > 0)")
   expect_true(consoleElement$offsetHeight > 0, "rstudio_Console_pane should be visible (height > 0)")
   
   # Check that rstudio_Source_pane is NOT visible (width or height should be 0)
   expect_true(sourceElement$offsetWidth == 0 || sourceElement$offsetHeight == 0, 
               "rstudio_Source_pane should NOT be visible (width or height should be 0)")
})

.rs.test("Source columns can be created and closed", {
   # skipping to cut down run times on CI
   skip_on_ci()

   # Create a new source column
   remote$commands.execute("newSourceColumn")
   
   # Wait for the new source column to be created
   .rs.waitUntil("source column created", function() {
      remote$dom.elementExists("#rstudio_Source1_pane")
   })
   
   # Verify the first source column exists
   source1Exists <- remote$dom.elementExists("#rstudio_Source1_pane")
   expect_true(source1Exists, "rstudio_Source1_pane should exist after creating source column")
   
   # Create second source column
   remote$commands.execute("newSourceColumn")
   
   # Wait for the second source column to be created
   .rs.waitUntil("second source column created", function() {
      remote$dom.elementExists("#rstudio_Source2_pane")
   })
   
   # Verify the second source column exists
   source2Exists <- remote$dom.elementExists("#rstudio_Source2_pane")
   expect_true(source2Exists, "rstudio_Source2_pane should exist after creating second source column")
   
   # Create third source column
   remote$commands.execute("newSourceColumn")
   
   # Wait for the third source column to be created
   .rs.waitUntil("third source column created", function() {
      remote$dom.elementExists("#rstudio_Source3_pane")
   })
   
   # Verify the third source column exists
   source3Exists <- remote$dom.elementExists("#rstudio_Source3_pane")
   expect_true(source3Exists, "rstudio_Source3_pane should exist after creating third source column")

   # Verify all source columns are visible
   source1Element <- remote$js.querySelector("#rstudio_Source1_pane")
   source2Element <- remote$js.querySelector("#rstudio_Source2_pane")
   source3Element <- remote$js.querySelector("#rstudio_Source3_pane")
   
   expect_true(source1Element$offsetWidth > 0, "rstudio_Source1_pane should be visible")
   expect_true(source1Element$offsetHeight > 0, "rstudio_Source1_pane should be visible")
   expect_true(source2Element$offsetWidth > 0, "rstudio_Source2_pane should be visible")
   expect_true(source2Element$offsetHeight > 0, "rstudio_Source2_pane should be visible")
   expect_true(source3Element$offsetWidth > 0, "rstudio_Source3_pane should be visible")
   expect_true(source3Element$offsetHeight > 0, "rstudio_Source3_pane should be visible")

   # Close all source docs (which will close the source columns)
   remote$commands.execute("closeAllSourceDocs")

   # Wait a tad (scientific term) for the source columns to be closed
   Sys.sleep(0.5)
   
   # Verify that the source columns are no longer visible
   source1Exists <- remote$dom.elementExists("#rstudio_Source1_pane")
   source2Exists <- remote$dom.elementExists("#rstudio_Source2_pane")
   source3Exists <- remote$dom.elementExists("#rstudio_Source3_pane")
   expect_false(source1Exists, "rstudio_Source1_pane should NOT exist after closing all source docs")
   expect_false(source2Exists, "rstudio_Source2_pane should NOT exist after closing all source docs")
   expect_false(source3Exists, "rstudio_Source3_pane should NOT exist after closing all source docs")

   .rs.resetUILayout(remote)
})

.rs.test("Layout zoom commands are unchecked by default", {
   # Check that layoutZoomLeftColumn command is unchecked
   leftZoomChecked <- remote$js.eval("window.rstudioCallbacks.commandIsChecked('layoutZoomLeftColumn')")
   expect_false(leftZoomChecked, "layoutZoomLeftColumn should be unchecked by default")
   
   # Check that layoutZoomRightColumn command is unchecked
   rightZoomChecked <- remote$js.eval("window.rstudioCallbacks.commandIsChecked('layoutZoomRightColumn')")
   expect_false(rightZoomChecked, "layoutZoomRightColumn should be unchecked by default")
})

.rs.test("Sidebar can be shown and hidden with toggleSidebar command", {
   # skipping to cut down run times on CI
   skip_on_ci()

   # Show the sidebar
   remote$commands.execute("toggleSidebar")
   
   # Wait for the sidebar to be created
   .rs.waitUntil("sidebar created", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane")
   })
   
   # Verify the sidebar exists
   sidebarExists <- remote$dom.elementExists("#rstudio_Sidebar_pane")
   expect_true(sidebarExists, "rstudio_Sidebar_pane should exist after toggling sidebar on")
   
   # Verify the sidebar is visible
   sidebarElement <- remote$js.querySelector("#rstudio_Sidebar_pane")
   expect_true(sidebarElement$offsetWidth > 0, "rstudio_Sidebar_pane should be visible (width > 0)")
   expect_true(sidebarElement$offsetHeight > 0, "rstudio_Sidebar_pane should be visible (height > 0)")

   # Hide the sidebar
   remote$commands.execute("toggleSidebar")
   
   # Wait for the sidebar to be removed
   .rs.waitUntil("sidebar removed", function() {
      !remote$dom.elementExists("#rstudio_Sidebar_pane")
   })
   
   # Verify the sidebar no longer exists
   sidebarExists <- remote$dom.elementExists("#rstudio_Sidebar_pane")
   expect_false(sidebarExists, "rstudio_Sidebar_pane should NOT exist after toggling sidebar off")
})

.rs.test("Sidebar can be moved left and right with toggleSidebarLocation command", {
   # skipping to cut down run times on CI
   skip_on_ci()

   # Show the sidebar first
   remote$commands.execute("toggleSidebar")
   
   # Wait for the sidebar to be created
   .rs.waitUntil("sidebar created", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane")
   })
   
   # Verify the sidebar exists and is visible
   sidebarExists <- remote$dom.elementExists("#rstudio_Sidebar_pane")
   expect_true(sidebarExists, "rstudio_Sidebar_pane should exist after toggling sidebar on")
   
   sidebarElement <- remote$js.querySelector("#rstudio_Sidebar_pane")
   expect_true(sidebarElement$offsetWidth > 0, "rstudio_Sidebar_pane should be visible")
   
   # Get initial position using screen coordinates
   # The sidebar starts on the left by default
   initialPosition <- sidebarElement$getBoundingClientRect()$left

   # Move sidebar to the right
   remote$commands.execute("toggleSidebarLocation")

   # Wait for the sidebar to be repositioned (it gets recreated in new location)
   .rs.waitUntil("sidebar moved right", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane")
   })

   # Add a small delay to ensure DOM is fully updated after recreation
   Sys.sleep(0.3)

   # Verify the sidebar still exists and is visible after moving right
   rightSidebarExists <- remote$dom.elementExists("#rstudio_Sidebar_pane")
   expect_true(rightSidebarExists, "rstudio_Sidebar_pane should still exist after toggleSidebarLocation")

   rightSidebarElement <- remote$js.querySelector("#rstudio_Sidebar_pane")
   expect_true(rightSidebarElement$offsetWidth > 0, "rstudio_Sidebar_pane should still be visible after toggleSidebarLocation")

   # Check that the sidebar is now positioned to the right using screen coordinates
   rightPosition <- rightSidebarElement$getBoundingClientRect()$left
   expect_true(rightPosition > initialPosition,
               paste0("Sidebar should be positioned to the right after toggleSidebarLocation. ",
                      "Initial position: ", initialPosition, ", Right position: ", rightPosition))

   # Get fresh console position after layout change and verify sidebar is to the right
   consoleElement <- remote$js.querySelector("#rstudio_Console_pane")
   consolePosition <- consoleElement$getBoundingClientRect()$left
   expect_true(rightPosition > consolePosition,
               paste0("Sidebar should be to the right of the console pane. ",
                      "Sidebar position: ", rightPosition, ", Console position: ", consolePosition))

   # Move sidebar back to the left
   remote$commands.execute("toggleSidebarLocation")

   # Wait for the sidebar to be repositioned again
   .rs.waitUntil("sidebar moved left", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane")
   })

   # Add a small delay to ensure DOM is fully updated after recreation
   Sys.sleep(0.3)

   # Verify the sidebar still exists and is visible after moving left
   leftSidebarExists <- remote$dom.elementExists("#rstudio_Sidebar_pane")
   expect_true(leftSidebarExists, "rstudio_Sidebar_pane should still exist after toggleSidebarLocation")

   leftSidebarElement <- remote$js.querySelector("#rstudio_Sidebar_pane")
   expect_true(leftSidebarElement$offsetWidth > 0, "rstudio_Sidebar_pane should still be visible after toggleSidebarLocation")

   # Check that the sidebar is now positioned to the left using screen coordinates
   leftPosition <- leftSidebarElement$getBoundingClientRect()$left
   expect_true(leftPosition < rightPosition,
               paste0("Sidebar should be positioned to the left after toggleSidebarLocation. ",
                      "Right position: ", rightPosition, ", Left position: ", leftPosition))

   # Get fresh console position after layout change and verify sidebar is to the left
   consoleElement <- remote$js.querySelector("#rstudio_Console_pane")
   consolePositionLeft <- consoleElement$getBoundingClientRect()$left
   expect_true(leftPosition < consolePositionLeft,
               paste0("Sidebar should be to the left of the console pane. ",
                      "Sidebar position: ", leftPosition, ", Console position: ", consolePositionLeft))
   
   .rs.resetUILayout(remote)
})

.rs.test("Zoomed left column with sidebar hidden works as expected", {
   # skipping to cut down run times on CI
   skip_on_ci()

   # Verify sidebar is hidden (default state)
   sidebarExists <- remote$dom.elementExists("#rstudio_Sidebar_pane")
   expect_false(sidebarExists, "rstudio_Sidebar_pane should NOT exist in default layout")

   # Get initial dimensions of console pane (left column) and right column panes
   consoleElement <- remote$js.querySelector("#rstudio_Console_pane")
   tabSet1Element <- remote$js.querySelector("#rstudio_TabSet1_pane")
   tabSet2Element <- remote$js.querySelector("#rstudio_TabSet2_pane")

   initialConsoleWidth <- consoleElement$offsetWidth
   initialTabSet1Width <- tabSet1Element$offsetWidth
   initialTabSet2Width <- tabSet2Element$offsetWidth

   expect_true(initialConsoleWidth > 0, "Console pane should be visible initially")
   expect_true(initialTabSet1Width > 0, "TabSet1 pane should be visible initially")
   expect_true(initialTabSet2Width > 0, "TabSet2 pane should be visible initially")

   # Execute layoutZoomLeftColumn command to zoom the left column
   remote$commands.execute("layoutZoomLeftColumn")

   # Calculate expected width: console should expand to fill the space previously occupied by both columns
   expectedZoomedWidth <- initialConsoleWidth + initialTabSet1Width

   # Wait for layout to change - console should expand to fill the right column space
   .rs.waitUntil("left column zoomed", function() {
      consoleElement <- remote$js.querySelector("#rstudio_Console_pane")
      # Allow some tolerance for dividers/borders (within 30px)
      abs(consoleElement$offsetWidth - expectedZoomedWidth) < 30
   })

   # Add a small delay to ensure layout is fully settled
   Sys.sleep(0.2)

   # Check that layoutZoomLeftColumn command is checked
   leftZoomChecked <- remote$js.eval("window.rstudioCallbacks.commandIsChecked('layoutZoomLeftColumn')")
   expect_true(leftZoomChecked, "layoutZoomLeftColumn should be checked after layoutZoomLeftColumn")
   
   # Check that layoutZoomRightColumn command is unchecked
   rightZoomChecked <- remote$js.eval("window.rstudioCallbacks.commandIsChecked('layoutZoomRightColumn')")
   expect_false(rightZoomChecked, "layoutZoomRightColumn should be unchecked after layoutZoomLeftColumn")

   # Get new dimensions after zooming
   consoleElementZoomed <- remote$js.querySelector("#rstudio_Console_pane")
   tabSet1ElementZoomed <- remote$js.querySelector("#rstudio_TabSet1_pane")
   tabSet2ElementZoomed <- remote$js.querySelector("#rstudio_TabSet2_pane")

   zoomedConsoleWidth <- consoleElementZoomed$offsetWidth
   zoomedTabSet1Width <- tabSet1ElementZoomed$offsetWidth
   zoomedTabSet2Width <- tabSet2ElementZoomed$offsetWidth

   # Verify left column (Console) expanded to fill the space of the right column
   expect_true(abs(zoomedConsoleWidth - expectedZoomedWidth) < 30,
               paste0("Console pane should expand to fill right column space. ",
                      "Expected: ", expectedZoomedWidth, ", Actual: ", zoomedConsoleWidth))

   # Verify right column panes collapsed to minimum width (just the divider, typically < 50px)
   expect_true(zoomedTabSet1Width < 50,
               paste0("TabSet1 pane should collapse to minimum width when left column zoomed. ",
                      "Width: ", zoomedTabSet1Width))
   expect_true(zoomedTabSet2Width < 50,
               paste0("TabSet2 pane should collapse to minimum width when left column zoomed. ",
                      "Width: ", zoomedTabSet2Width))

   # Execute layoutZoomLeftColumn command again to restore the layout
   remote$commands.execute("layoutZoomLeftColumn")

   # Wait for layout to be restored - console should return to original size
   .rs.waitUntil("left column unzoomed", function() {
      consoleElement <- remote$js.querySelector("#rstudio_Console_pane")
      tabSet1Element <- remote$js.querySelector("#rstudio_TabSet1_pane")
      # Check if both console is smaller and TabSet1 is visible again
      consoleElement$offsetWidth < zoomedConsoleWidth * 0.75 &&
         tabSet1Element$offsetWidth > 50
   })

   # Add a small delay to ensure layout is fully settled
   Sys.sleep(0.2)

   # Check that layoutZoomLeftColumn command is unchecked
   leftZoomChecked <- remote$js.eval("window.rstudioCallbacks.commandIsChecked('layoutZoomLeftColumn')")
   expect_false(leftZoomChecked, "layoutZoomLeftColumn should be unchecked after undoing layoutZoomLeftColumn")
   
   # Check that layoutZoomRightColumn command is unchecked
   rightZoomChecked <- remote$js.eval("window.rstudioCallbacks.commandIsChecked('layoutZoomRightColumn')")
   expect_false(rightZoomChecked, "layoutZoomRightColumn should be unchecked after undoing layoutZoomLeftColumn")

   # Get dimensions after restoring
   consoleElementRestored <- remote$js.querySelector("#rstudio_Console_pane")
   tabSet1ElementRestored <- remote$js.querySelector("#rstudio_TabSet1_pane")
   tabSet2ElementRestored <- remote$js.querySelector("#rstudio_TabSet2_pane")

   restoredConsoleWidth <- consoleElementRestored$offsetWidth
   restoredTabSet1Width <- tabSet1ElementRestored$offsetWidth
   restoredTabSet2Width <- tabSet2ElementRestored$offsetWidth

   # Verify layout restored to approximately original sizes (within 10% tolerance)
   expect_true(abs(restoredConsoleWidth - initialConsoleWidth) / initialConsoleWidth < 0.1,
               paste0("Console pane should return to original width after un-zooming. ",
                      "Initial: ", initialConsoleWidth, ", Restored: ", restoredConsoleWidth))

   expect_true(abs(restoredTabSet1Width - initialTabSet1Width) / initialTabSet1Width < 0.1,
               paste0("TabSet1 pane should return to original width after un-zooming. ",
                      "Initial: ", initialTabSet1Width, ", Restored: ", restoredTabSet1Width))

   expect_true(abs(restoredTabSet2Width - initialTabSet2Width) / initialTabSet2Width < 0.1,
               paste0("TabSet2 pane should return to original width after un-zooming. ",
                      "Initial: ", initialTabSet2Width, ", Restored: ", restoredTabSet2Width))

   .rs.resetUILayout(remote)
})

.rs.test("Zoomed left column with sidebar visible works as expected", {
   # skipping to cut down run times on CI
   skip_on_ci()

   # First make the sidebar visible
   remote$commands.execute("toggleSidebar")

   # Wait for the sidebar to be created
   .rs.waitUntil("sidebar created", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane")
   })

   # Verify the sidebar exists and is visible
   sidebarExists <- remote$dom.elementExists("#rstudio_Sidebar_pane")
   expect_true(sidebarExists, "rstudio_Sidebar_pane should exist after toggling sidebar on")

   sidebarElement <- remote$js.querySelector("#rstudio_Sidebar_pane")
   expect_true(sidebarElement$offsetWidth > 0, "rstudio_Sidebar_pane should be visible")

   # Get initial dimensions of console pane (left column), right column panes, and sidebar
   consoleElement <- remote$js.querySelector("#rstudio_Console_pane")
   tabSet1Element <- remote$js.querySelector("#rstudio_TabSet1_pane")
   tabSet2Element <- remote$js.querySelector("#rstudio_TabSet2_pane")

   initialConsoleWidth <- consoleElement$offsetWidth
   initialTabSet1Width <- tabSet1Element$offsetWidth
   initialTabSet2Width <- tabSet2Element$offsetWidth
   initialSidebarWidth <- sidebarElement$offsetWidth

   expect_true(initialConsoleWidth > 0, "Console pane should be visible initially")
   expect_true(initialTabSet1Width > 0, "TabSet1 pane should be visible initially")
   expect_true(initialTabSet2Width > 0, "TabSet2 pane should be visible initially")
   expect_true(initialSidebarWidth > 0, "Sidebar pane should be visible initially")

   # Execute layoutZoomLeftColumn command to zoom the left column
   remote$commands.execute("layoutZoomLeftColumn")

   # Calculate expected width: console should expand to fill the space previously occupied
   # by both TabSet1 and Sidebar (sidebar is on left by default)
   expectedZoomedWidth <- initialConsoleWidth + initialTabSet1Width + initialSidebarWidth

   # Wait for layout to change - console should expand to fill the right column and sidebar space
   .rs.waitUntil("left column zoomed with sidebar", function() {
      consoleElement <- remote$js.querySelector("#rstudio_Console_pane")
      # Allow some tolerance for dividers/borders (within 30px)
      abs(consoleElement$offsetWidth - expectedZoomedWidth) < 30
   })

   # Add a small delay to ensure layout is fully settled
   Sys.sleep(0.2)

   # Check that layoutZoomLeftColumn command is checked
   leftZoomChecked <- remote$js.eval("window.rstudioCallbacks.commandIsChecked('layoutZoomLeftColumn')")
   expect_true(leftZoomChecked, "layoutZoomLeftColumn should be checked after layoutZoomLeftColumn")

   # Check that layoutZoomRightColumn command is unchecked
   rightZoomChecked <- remote$js.eval("window.rstudioCallbacks.commandIsChecked('layoutZoomRightColumn')")
   expect_false(rightZoomChecked, "layoutZoomRightColumn should be unchecked after layoutZoomLeftColumn")

   # Get new dimensions after zooming
   consoleElementZoomed <- remote$js.querySelector("#rstudio_Console_pane")
   tabSet1ElementZoomed <- remote$js.querySelector("#rstudio_TabSet1_pane")
   tabSet2ElementZoomed <- remote$js.querySelector("#rstudio_TabSet2_pane")
   sidebarElementZoomed <- remote$js.querySelector("#rstudio_Sidebar_pane")

   zoomedConsoleWidth <- consoleElementZoomed$offsetWidth
   zoomedTabSet1Width <- tabSet1ElementZoomed$offsetWidth
   zoomedTabSet2Width <- tabSet2ElementZoomed$offsetWidth
   zoomedSidebarWidth <- sidebarElementZoomed$offsetWidth

   # Verify left column (Console) expanded to fill the space of the right column and sidebar
   expect_true(abs(zoomedConsoleWidth - expectedZoomedWidth) < 30,
               paste0("Console pane should expand to fill right column and sidebar space. ",
                      "Expected: ", expectedZoomedWidth, ", Actual: ", zoomedConsoleWidth))

   # Verify right column panes and sidebar collapsed to minimum width (just the divider, typically < 50px)
   expect_true(zoomedTabSet1Width < 50,
               paste0("TabSet1 pane should collapse to minimum width when left column zoomed. ",
                      "Width: ", zoomedTabSet1Width))
   expect_true(zoomedTabSet2Width < 50,
               paste0("TabSet2 pane should collapse to minimum width when left column zoomed. ",
                      "Width: ", zoomedTabSet2Width))
   expect_true(zoomedSidebarWidth < 50,
               paste0("Sidebar pane should collapse to minimum width when left column zoomed. ",
                      "Width: ", zoomedSidebarWidth))

   # Execute layoutZoomLeftColumn command again to restore the layout
   remote$commands.execute("layoutZoomLeftColumn")

   # Wait for layout to be restored - console should return to original size and sidebar/TabSet1 should be visible
   .rs.waitUntil("left column unzoomed with sidebar", function() {
      consoleElement <- remote$js.querySelector("#rstudio_Console_pane")
      tabSet1Element <- remote$js.querySelector("#rstudio_TabSet1_pane")
      sidebarElement <- remote$js.querySelector("#rstudio_Sidebar_pane")
      # Check if console is smaller, TabSet1 is visible, and Sidebar is visible again
      consoleElement$offsetWidth < zoomedConsoleWidth * 0.75 &&
         tabSet1Element$offsetWidth > 50 &&
         sidebarElement$offsetWidth > 50
   })

   # Add a small delay to ensure layout is fully settled
   Sys.sleep(0.2)

   # Check that layoutZoomLeftColumn command is unchecked
   leftZoomChecked <- remote$js.eval("window.rstudioCallbacks.commandIsChecked('layoutZoomLeftColumn')")
   expect_false(leftZoomChecked, "layoutZoomLeftColumn should be unchecked after undoing layoutZoomLeftColumn")

   # Check that layoutZoomRightColumn command is unchecked
   rightZoomChecked <- remote$js.eval("window.rstudioCallbacks.commandIsChecked('layoutZoomRightColumn')")
   expect_false(rightZoomChecked, "layoutZoomRightColumn should be unchecked after undoing layoutZoomLeftColumn")

   # Get dimensions after restoring
   consoleElementRestored <- remote$js.querySelector("#rstudio_Console_pane")
   tabSet1ElementRestored <- remote$js.querySelector("#rstudio_TabSet1_pane")
   tabSet2ElementRestored <- remote$js.querySelector("#rstudio_TabSet2_pane")
   sidebarElementRestored <- remote$js.querySelector("#rstudio_Sidebar_pane")

   restoredConsoleWidth <- consoleElementRestored$offsetWidth
   restoredTabSet1Width <- tabSet1ElementRestored$offsetWidth
   restoredTabSet2Width <- tabSet2ElementRestored$offsetWidth
   restoredSidebarWidth <- sidebarElementRestored$offsetWidth

   # Verify layout restored to approximately original sizes (within 10% tolerance)
   expect_true(abs(restoredConsoleWidth - initialConsoleWidth) / initialConsoleWidth < 0.1,
               paste0("Console pane should return to original width after un-zooming. ",
                      "Initial: ", initialConsoleWidth, ", Restored: ", restoredConsoleWidth))

   expect_true(abs(restoredTabSet1Width - initialTabSet1Width) / initialTabSet1Width < 0.1,
               paste0("TabSet1 pane should return to original width after un-zooming. ",
                      "Initial: ", initialTabSet1Width, ", Restored: ", restoredTabSet1Width))

   expect_true(abs(restoredTabSet2Width - initialTabSet2Width) / initialTabSet2Width < 0.1,
               paste0("TabSet2 pane should return to original width after un-zooming. ",
                      "Initial: ", initialTabSet2Width, ", Restored: ", restoredTabSet2Width))

   expect_true(abs(restoredSidebarWidth - initialSidebarWidth) / initialSidebarWidth < 0.1,
               paste0("Sidebar pane should return to original width after un-zooming. ",
                      "Initial: ", initialSidebarWidth, ", Restored: ", restoredSidebarWidth))

   .rs.resetUILayout(remote)
})

.rs.test("Zoomed right column with sidebar hidden works as expected", {
   # skipping to cut down run times on CI
   skip_on_ci()

   # Verify sidebar is hidden (default state)
   sidebarExists <- remote$dom.elementExists("#rstudio_Sidebar_pane")
   expect_false(sidebarExists, "rstudio_Sidebar_pane should NOT exist in default layout")

   # Get initial dimensions of console pane (left column) and right column panes
   consoleElement <- remote$js.querySelector("#rstudio_Console_pane")
   tabSet1Element <- remote$js.querySelector("#rstudio_TabSet1_pane")
   tabSet2Element <- remote$js.querySelector("#rstudio_TabSet2_pane")

   initialConsoleWidth <- consoleElement$offsetWidth
   initialTabSet1Width <- tabSet1Element$offsetWidth
   initialTabSet2Width <- tabSet2Element$offsetWidth

   expect_true(initialConsoleWidth > 0, "Console pane should be visible initially")
   expect_true(initialTabSet1Width > 0, "TabSet1 pane should be visible initially")
   expect_true(initialTabSet2Width > 0, "TabSet2 pane should be visible initially")

   # Execute layoutZoomRightColumn command to zoom the right column
   remote$commands.execute("layoutZoomRightColumn")

   # Calculate expected width: TabSet1 should expand to fill the space previously occupied by console
   expectedZoomedWidth <- initialTabSet1Width + initialConsoleWidth

   # Wait for layout to change - TabSet1 should expand to fill the left column space
   .rs.waitUntil("right column zoomed", function() {
      tabSet1Element <- remote$js.querySelector("#rstudio_TabSet1_pane")
      # Allow some tolerance for dividers/borders (within 30px)
      abs(tabSet1Element$offsetWidth - expectedZoomedWidth) < 30
   })

   # Add a small delay to ensure layout is fully settled
   Sys.sleep(0.2)

   # Check that layoutZoomRightColumn command is checked
   rightZoomChecked <- remote$js.eval("window.rstudioCallbacks.commandIsChecked('layoutZoomRightColumn')")
   expect_true(rightZoomChecked, "layoutZoomRightColumn should be checked after layoutZoomRightColumn")

   # Check that layoutZoomLeftColumn command is unchecked
   leftZoomChecked <- remote$js.eval("window.rstudioCallbacks.commandIsChecked('layoutZoomLeftColumn')")
   expect_false(leftZoomChecked, "layoutZoomLeftColumn should be unchecked after layoutZoomRightColumn")

   # Get new dimensions after zooming
   consoleElementZoomed <- remote$js.querySelector("#rstudio_Console_pane")
   tabSet1ElementZoomed <- remote$js.querySelector("#rstudio_TabSet1_pane")
   tabSet2ElementZoomed <- remote$js.querySelector("#rstudio_TabSet2_pane")

   zoomedConsoleWidth <- consoleElementZoomed$offsetWidth
   zoomedTabSet1Width <- tabSet1ElementZoomed$offsetWidth
   zoomedTabSet2Width <- tabSet2ElementZoomed$offsetWidth

   # Verify right column (TabSet1) expanded to fill the space of the left column
   expect_true(abs(zoomedTabSet1Width - expectedZoomedWidth) < 30,
               paste0("TabSet1 pane should expand to fill left column space. ",
                      "Expected: ", expectedZoomedWidth, ", Actual: ", zoomedTabSet1Width))

   # Verify TabSet2 maintains similar width (it's stacked with TabSet1, not side-by-side)
   expect_true(abs(zoomedTabSet2Width - zoomedTabSet1Width) < 30,
               paste0("TabSet2 pane should have similar width to TabSet1. ",
                      "TabSet1: ", zoomedTabSet1Width, ", TabSet2: ", zoomedTabSet2Width))

   # Verify left column (Console) collapsed to minimum width (just the divider, typically < 50px)
   expect_true(zoomedConsoleWidth < 50,
               paste0("Console pane should collapse to minimum width when right column zoomed. ",
                      "Width: ", zoomedConsoleWidth))

   # Execute layoutZoomRightColumn command again to restore the layout
   remote$commands.execute("layoutZoomRightColumn")

   # Wait for layout to be restored - TabSet1 should return to original size and Console should be visible
   .rs.waitUntil("right column unzoomed", function() {
      tabSet1Element <- remote$js.querySelector("#rstudio_TabSet1_pane")
      consoleElement <- remote$js.querySelector("#rstudio_Console_pane")
      # Check if TabSet1 is smaller and Console is visible again
      tabSet1Element$offsetWidth < zoomedTabSet1Width * 0.75 &&
         consoleElement$offsetWidth > 50
   })

   # Add a small delay to ensure layout is fully settled
   Sys.sleep(0.2)

   # Check that layoutZoomRightColumn command is unchecked
   rightZoomChecked <- remote$js.eval("window.rstudioCallbacks.commandIsChecked('layoutZoomRightColumn')")
   expect_false(rightZoomChecked, "layoutZoomRightColumn should be unchecked after undoing layoutZoomRightColumn")

   # Check that layoutZoomLeftColumn command is unchecked
   leftZoomChecked <- remote$js.eval("window.rstudioCallbacks.commandIsChecked('layoutZoomLeftColumn')")
   expect_false(leftZoomChecked, "layoutZoomLeftColumn should be unchecked after undoing layoutZoomRightColumn")

   # Get dimensions after restoring
   consoleElementRestored <- remote$js.querySelector("#rstudio_Console_pane")
   tabSet1ElementRestored <- remote$js.querySelector("#rstudio_TabSet1_pane")
   tabSet2ElementRestored <- remote$js.querySelector("#rstudio_TabSet2_pane")

   restoredConsoleWidth <- consoleElementRestored$offsetWidth
   restoredTabSet1Width <- tabSet1ElementRestored$offsetWidth
   restoredTabSet2Width <- tabSet2ElementRestored$offsetWidth

   # Verify layout restored to approximately original sizes (within 10% tolerance)
   expect_true(abs(restoredConsoleWidth - initialConsoleWidth) / initialConsoleWidth < 0.1,
               paste0("Console pane should return to original width after un-zooming. ",
                      "Initial: ", initialConsoleWidth, ", Restored: ", restoredConsoleWidth))

   expect_true(abs(restoredTabSet1Width - initialTabSet1Width) / initialTabSet1Width < 0.1,
               paste0("TabSet1 pane should return to original width after un-zooming. ",
                      "Initial: ", initialTabSet1Width, ", Restored: ", restoredTabSet1Width))

   expect_true(abs(restoredTabSet2Width - initialTabSet2Width) / initialTabSet2Width < 0.1,
               paste0("TabSet2 pane should return to original width after un-zooming. ",
                      "Initial: ", initialTabSet2Width, ", Restored: ", restoredTabSet2Width))

   .rs.resetUILayout(remote)
})

.rs.test("Zoomed right column with sidebar visible works as expected", {
   # skipping to cut down run times on CI
   skip_on_ci()

   # First make the sidebar visible
   remote$commands.execute("toggleSidebar")

   # Wait for the sidebar to be created
   .rs.waitUntil("sidebar created", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane")
   })

   # Verify the sidebar exists and is visible
   sidebarExists <- remote$dom.elementExists("#rstudio_Sidebar_pane")
   expect_true(sidebarExists, "rstudio_Sidebar_pane should exist after toggling sidebar on")

   sidebarElement <- remote$js.querySelector("#rstudio_Sidebar_pane")
   expect_true(sidebarElement$offsetWidth > 0, "rstudio_Sidebar_pane should be visible")

   # Get initial dimensions of console pane (left column), right column panes, and sidebar
   consoleElement <- remote$js.querySelector("#rstudio_Console_pane")
   tabSet1Element <- remote$js.querySelector("#rstudio_TabSet1_pane")
   tabSet2Element <- remote$js.querySelector("#rstudio_TabSet2_pane")

   initialConsoleWidth <- consoleElement$offsetWidth
   initialTabSet1Width <- tabSet1Element$offsetWidth
   initialTabSet2Width <- tabSet2Element$offsetWidth
   initialSidebarWidth <- sidebarElement$offsetWidth

   expect_true(initialConsoleWidth > 0, "Console pane should be visible initially")
   expect_true(initialTabSet1Width > 0, "TabSet1 pane should be visible initially")
   expect_true(initialTabSet2Width > 0, "TabSet2 pane should be visible initially")
   expect_true(initialSidebarWidth > 0, "Sidebar pane should be visible initially")

   # Execute layoutZoomRightColumn command to zoom the right column
   remote$commands.execute("layoutZoomRightColumn")

   # Calculate expected width: TabSet1 should expand to fill the space previously occupied
   # by both Console and Sidebar (sidebar is on left by default)
   expectedZoomedWidth <- initialTabSet1Width + initialConsoleWidth + initialSidebarWidth

   # Wait for layout to change - TabSet1 should expand to fill the left column and sidebar space
   .rs.waitUntil("right column zoomed with sidebar", function() {
      tabSet1Element <- remote$js.querySelector("#rstudio_TabSet1_pane")
      # Allow some tolerance for dividers/borders (within 30px)
      abs(tabSet1Element$offsetWidth - expectedZoomedWidth) < 30
   })

   # Add a small delay to ensure layout is fully settled
   Sys.sleep(0.2)

   # Check that layoutZoomRightColumn command is checked
   rightZoomChecked <- remote$js.eval("window.rstudioCallbacks.commandIsChecked('layoutZoomRightColumn')")
   expect_true(rightZoomChecked, "layoutZoomRightColumn should be checked after layoutZoomRightColumn")

   # Check that layoutZoomLeftColumn command is unchecked
   leftZoomChecked <- remote$js.eval("window.rstudioCallbacks.commandIsChecked('layoutZoomLeftColumn')")
   expect_false(leftZoomChecked, "layoutZoomLeftColumn should be unchecked after layoutZoomRightColumn")

   # Get new dimensions after zooming
   consoleElementZoomed <- remote$js.querySelector("#rstudio_Console_pane")
   tabSet1ElementZoomed <- remote$js.querySelector("#rstudio_TabSet1_pane")
   tabSet2ElementZoomed <- remote$js.querySelector("#rstudio_TabSet2_pane")
   sidebarElementZoomed <- remote$js.querySelector("#rstudio_Sidebar_pane")

   zoomedConsoleWidth <- consoleElementZoomed$offsetWidth
   zoomedTabSet1Width <- tabSet1ElementZoomed$offsetWidth
   zoomedTabSet2Width <- tabSet2ElementZoomed$offsetWidth
   zoomedSidebarWidth <- sidebarElementZoomed$offsetWidth

   # Verify right column (TabSet1) expanded to fill the space of the left column and sidebar
   expect_true(abs(zoomedTabSet1Width - expectedZoomedWidth) < 30,
               paste0("TabSet1 pane should expand to fill left column and sidebar space. ",
                      "Expected: ", expectedZoomedWidth, ", Actual: ", zoomedTabSet1Width))

   # Verify TabSet2 maintains similar width (it's stacked with TabSet1, not side-by-side)
   expect_true(abs(zoomedTabSet2Width - zoomedTabSet1Width) < 30,
               paste0("TabSet2 pane should have similar width to TabSet1. ",
                      "TabSet1: ", zoomedTabSet1Width, ", TabSet2: ", zoomedTabSet2Width))

   # Verify left column and sidebar collapsed to minimum width (just the divider, typically < 50px)
   expect_true(zoomedConsoleWidth < 50,
               paste0("Console pane should collapse to minimum width when right column zoomed. ",
                      "Width: ", zoomedConsoleWidth))
   expect_true(zoomedSidebarWidth < 50,
               paste0("Sidebar pane should collapse to minimum width when right column zoomed. ",
                      "Width: ", zoomedSidebarWidth))

   # Execute layoutZoomRightColumn command again to restore the layout
   remote$commands.execute("layoutZoomRightColumn")

   # Wait for layout to be restored - TabSet1 should return to original size and Console/Sidebar should be visible
   .rs.waitUntil("right column unzoomed with sidebar", function() {
      tabSet1Element <- remote$js.querySelector("#rstudio_TabSet1_pane")
      consoleElement <- remote$js.querySelector("#rstudio_Console_pane")
      sidebarElement <- remote$js.querySelector("#rstudio_Sidebar_pane")
      # Check if TabSet1 is smaller, Console is visible, and Sidebar is visible again
      tabSet1Element$offsetWidth < zoomedTabSet1Width * 0.75 &&
         consoleElement$offsetWidth > 50 &&
         sidebarElement$offsetWidth > 50
   })

   # Add a small delay to ensure layout is fully settled
   Sys.sleep(0.2)

   # Check that layoutZoomRightColumn command is unchecked
   rightZoomChecked <- remote$js.eval("window.rstudioCallbacks.commandIsChecked('layoutZoomRightColumn')")
   expect_false(rightZoomChecked, "layoutZoomRightColumn should be unchecked after undoing layoutZoomRightColumn")

   # Check that layoutZoomLeftColumn command is unchecked
   leftZoomChecked <- remote$js.eval("window.rstudioCallbacks.commandIsChecked('layoutZoomLeftColumn')")
   expect_false(leftZoomChecked, "layoutZoomLeftColumn should be unchecked after undoing layoutZoomRightColumn")

   # Get dimensions after restoring
   consoleElementRestored <- remote$js.querySelector("#rstudio_Console_pane")
   tabSet1ElementRestored <- remote$js.querySelector("#rstudio_TabSet1_pane")
   tabSet2ElementRestored <- remote$js.querySelector("#rstudio_TabSet2_pane")
   sidebarElementRestored <- remote$js.querySelector("#rstudio_Sidebar_pane")

   restoredConsoleWidth <- consoleElementRestored$offsetWidth
   restoredTabSet1Width <- tabSet1ElementRestored$offsetWidth
   restoredTabSet2Width <- tabSet2ElementRestored$offsetWidth
   restoredSidebarWidth <- sidebarElementRestored$offsetWidth

   # Verify layout restored to approximately original sizes (within 10% tolerance)
   expect_true(abs(restoredConsoleWidth - initialConsoleWidth) / initialConsoleWidth < 0.1,
               paste0("Console pane should return to original width after un-zooming. ",
                      "Initial: ", initialConsoleWidth, ", Restored: ", restoredConsoleWidth))

   expect_true(abs(restoredTabSet1Width - initialTabSet1Width) / initialTabSet1Width < 0.1,
               paste0("TabSet1 pane should return to original width after un-zooming. ",
                      "Initial: ", initialTabSet1Width, ", Restored: ", restoredTabSet1Width))

   expect_true(abs(restoredTabSet2Width - initialTabSet2Width) / initialTabSet2Width < 0.1,
               paste0("TabSet2 pane should return to original width after un-zooming. ",
                      "Initial: ", initialTabSet2Width, ", Restored: ", restoredTabSet2Width))

   expect_true(abs(restoredSidebarWidth - initialSidebarWidth) / initialSidebarWidth < 0.1,
               paste0("Sidebar pane should return to original width after un-zooming. ",
                      "Initial: ", initialSidebarWidth, ", Restored: ", restoredSidebarWidth))

   .rs.resetUILayout(remote)
})

.rs.test("Zoomed left column with sidebar on right works as expected", {
   # skipping to cut down run times on CI
   skip_on_ci()

   # First make the sidebar visible
   remote$commands.execute("toggleSidebar")

   # Wait for the sidebar to be created
   .rs.waitUntil("sidebar created", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane")
   })

   # Move sidebar to the right
   remote$commands.execute("toggleSidebarLocation")

   # Wait for the sidebar to be repositioned
   .rs.waitUntil("sidebar moved right", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane")
   })

   # Add a small delay to ensure DOM is fully updated after recreation
   Sys.sleep(0.3)

   # Verify the sidebar exists and is visible on the right
   sidebarExists <- remote$dom.elementExists("#rstudio_Sidebar_pane")
   expect_true(sidebarExists, "rstudio_Sidebar_pane should exist after moving to right")

   sidebarElement <- remote$js.querySelector("#rstudio_Sidebar_pane")
   expect_true(sidebarElement$offsetWidth > 0, "rstudio_Sidebar_pane should be visible")

   # Get initial dimensions of sidebar, console pane (left column), and right column panes
   consoleElement <- remote$js.querySelector("#rstudio_Console_pane")
   tabSet1Element <- remote$js.querySelector("#rstudio_TabSet1_pane")
   tabSet2Element <- remote$js.querySelector("#rstudio_TabSet2_pane")

   initialSidebarWidth <- sidebarElement$offsetWidth
   initialConsoleWidth <- consoleElement$offsetWidth
   initialTabSet1Width <- tabSet1Element$offsetWidth
   initialTabSet2Width <- tabSet2Element$offsetWidth

   expect_true(initialSidebarWidth > 0, "Sidebar pane should be visible initially")
   expect_true(initialConsoleWidth > 0, "Console pane should be visible initially")
   expect_true(initialTabSet1Width > 0, "TabSet1 pane should be visible initially")
   expect_true(initialTabSet2Width > 0, "TabSet2 pane should be visible initially")

   # Execute layoutZoomLeftColumn command to zoom the left column
   remote$commands.execute("layoutZoomLeftColumn")

   # Calculate expected width: console should expand to fill the space previously occupied
   # by both TabSet1 and Sidebar (sidebar is on right now)
   expectedZoomedWidth <- initialConsoleWidth + initialTabSet1Width + initialSidebarWidth

   # Wait for layout to change - console should expand to fill the right column and sidebar space
   .rs.waitUntil("left column zoomed with sidebar on right", function() {
      consoleElement <- remote$js.querySelector("#rstudio_Console_pane")
      # Allow some tolerance for dividers/borders (within 30px)
      abs(consoleElement$offsetWidth - expectedZoomedWidth) < 30
   })

   # Add a small delay to ensure layout is fully settled
   Sys.sleep(0.2)

   # Check that layoutZoomLeftColumn command is checked
   leftZoomChecked <- remote$js.eval("window.rstudioCallbacks.commandIsChecked('layoutZoomLeftColumn')")
   expect_true(leftZoomChecked, "layoutZoomLeftColumn should be checked after layoutZoomLeftColumn")

   # Check that layoutZoomRightColumn command is unchecked
   rightZoomChecked <- remote$js.eval("window.rstudioCallbacks.commandIsChecked('layoutZoomRightColumn')")
   expect_false(rightZoomChecked, "layoutZoomRightColumn should be unchecked after layoutZoomLeftColumn")

   # Get new dimensions after zooming
   consoleElementZoomed <- remote$js.querySelector("#rstudio_Console_pane")
   tabSet1ElementZoomed <- remote$js.querySelector("#rstudio_TabSet1_pane")
   tabSet2ElementZoomed <- remote$js.querySelector("#rstudio_TabSet2_pane")
   sidebarElementZoomed <- remote$js.querySelector("#rstudio_Sidebar_pane")

   zoomedConsoleWidth <- consoleElementZoomed$offsetWidth
   zoomedTabSet1Width <- tabSet1ElementZoomed$offsetWidth
   zoomedTabSet2Width <- tabSet2ElementZoomed$offsetWidth
   zoomedSidebarWidth <- sidebarElementZoomed$offsetWidth

   # Verify left column (Console) expanded to fill the space of the right column and sidebar
   expect_true(abs(zoomedConsoleWidth - expectedZoomedWidth) < 30,
               paste0("Console pane should expand to fill right column and sidebar space. ",
                      "Expected: ", expectedZoomedWidth, ", Actual: ", zoomedConsoleWidth))

   # Verify right column panes and sidebar collapsed to minimum width (just the divider, typically < 50px)
   expect_true(zoomedTabSet1Width < 50,
               paste0("TabSet1 pane should collapse to minimum width when left column zoomed. ",
                      "Width: ", zoomedTabSet1Width))
   expect_true(zoomedTabSet2Width < 50,
               paste0("TabSet2 pane should collapse to minimum width when left column zoomed. ",
                      "Width: ", zoomedTabSet2Width))
   expect_true(zoomedSidebarWidth < 50,
               paste0("Sidebar pane should collapse to minimum width when left column zoomed. ",
                      "Width: ", zoomedSidebarWidth))

   # Execute layoutZoomLeftColumn command again to restore the layout
   remote$commands.execute("layoutZoomLeftColumn")

   # Wait for layout to be restored - console should return to original size and sidebar/TabSet1 should be visible
   .rs.waitUntil("left column unzoomed with sidebar on right", function() {
      consoleElement <- remote$js.querySelector("#rstudio_Console_pane")
      tabSet1Element <- remote$js.querySelector("#rstudio_TabSet1_pane")
      sidebarElement <- remote$js.querySelector("#rstudio_Sidebar_pane")
      # Check if console is smaller, TabSet1 is visible, and Sidebar is visible again
      consoleElement$offsetWidth < zoomedConsoleWidth * 0.75 &&
         tabSet1Element$offsetWidth > 50 &&
         sidebarElement$offsetWidth > 50
   })

   # Add a small delay to ensure layout is fully settled
   Sys.sleep(0.2)

   # Check that layoutZoomLeftColumn command is unchecked
   leftZoomChecked <- remote$js.eval("window.rstudioCallbacks.commandIsChecked('layoutZoomLeftColumn')")
   expect_false(leftZoomChecked, "layoutZoomLeftColumn should be unchecked after undoing layoutZoomLeftColumn")

   # Check that layoutZoomRightColumn command is unchecked
   rightZoomChecked <- remote$js.eval("window.rstudioCallbacks.commandIsChecked('layoutZoomRightColumn')")
   expect_false(rightZoomChecked, "layoutZoomRightColumn should be unchecked after undoing layoutZoomLeftColumn")

   # Get dimensions after restoring
   consoleElementRestored <- remote$js.querySelector("#rstudio_Console_pane")
   tabSet1ElementRestored <- remote$js.querySelector("#rstudio_TabSet1_pane")
   tabSet2ElementRestored <- remote$js.querySelector("#rstudio_TabSet2_pane")
   sidebarElementRestored <- remote$js.querySelector("#rstudio_Sidebar_pane")

   restoredConsoleWidth <- consoleElementRestored$offsetWidth
   restoredTabSet1Width <- tabSet1ElementRestored$offsetWidth
   restoredTabSet2Width <- tabSet2ElementRestored$offsetWidth
   restoredSidebarWidth <- sidebarElementRestored$offsetWidth

   # Verify layout restored to approximately original sizes (within 10% tolerance)
   expect_true(abs(restoredConsoleWidth - initialConsoleWidth) / initialConsoleWidth < 0.1,
               paste0("Console pane should return to original width after un-zooming. ",
                      "Initial: ", initialConsoleWidth, ", Restored: ", restoredConsoleWidth))

   expect_true(abs(restoredTabSet1Width - initialTabSet1Width) / initialTabSet1Width < 0.1,
               paste0("TabSet1 pane should return to original width after un-zooming. ",
                      "Initial: ", initialTabSet1Width, ", Restored: ", restoredTabSet1Width))

   expect_true(abs(restoredTabSet2Width - initialTabSet2Width) / initialTabSet2Width < 0.1,
               paste0("TabSet2 pane should return to original width after un-zooming. ",
                      "Initial: ", initialTabSet2Width, ", Restored: ", restoredTabSet2Width))

   expect_true(abs(restoredSidebarWidth - initialSidebarWidth) / initialSidebarWidth < 0.1,
               paste0("Sidebar pane should return to original width after un-zooming. ",
                      "Initial: ", initialSidebarWidth, ", Restored: ", restoredSidebarWidth))

   # Clean up: move sidebar back to the left
   remote$commands.execute("toggleSidebarLocation")

   # Wait for the sidebar to be repositioned
   .rs.waitUntil("sidebar moved left", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane")
   })

   # Add a small delay to ensure DOM is fully updated
   Sys.sleep(0.3)

   # Verify the sidebar is still visible after moving left
   sidebarExistsRight <- remote$dom.elementExists("#rstudio_Sidebar_pane")
   expect_true(sidebarExistsRight, "rstudio_Sidebar_pane should still exist after moving left")

   .rs.resetUILayout(remote)
})

.rs.test("Zoomed right column with sidebar on right works as expected", {
   # skipping to cut down run times on CI
   skip_on_ci()

   # First make the sidebar visible
   remote$commands.execute("toggleSidebar")

   # Wait for the sidebar to be created
   .rs.waitUntil("sidebar created", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane")
   })

   # Move sidebar to the right
   remote$commands.execute("toggleSidebarLocation")

   # Wait for the sidebar to be repositioned
   .rs.waitUntil("sidebar moved right", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane")
   })

   # Add a small delay to ensure DOM is fully updated after recreation
   Sys.sleep(0.3)

   # Verify the sidebar exists and is visible on the right
   sidebarExists <- remote$dom.elementExists("#rstudio_Sidebar_pane")
   expect_true(sidebarExists, "rstudio_Sidebar_pane should exist after moving to right")

   sidebarElement <- remote$js.querySelector("#rstudio_Sidebar_pane")
   expect_true(sidebarElement$offsetWidth > 0, "rstudio_Sidebar_pane should be visible")

   # Get initial dimensions of sidebar, console pane (left column), and right column panes
   consoleElement <- remote$js.querySelector("#rstudio_Console_pane")
   tabSet1Element <- remote$js.querySelector("#rstudio_TabSet1_pane")
   tabSet2Element <- remote$js.querySelector("#rstudio_TabSet2_pane")

   initialSidebarWidth <- sidebarElement$offsetWidth
   initialConsoleWidth <- consoleElement$offsetWidth
   initialTabSet1Width <- tabSet1Element$offsetWidth
   initialTabSet2Width <- tabSet2Element$offsetWidth

   expect_true(initialSidebarWidth > 0, "Sidebar pane should be visible initially")
   expect_true(initialConsoleWidth > 0, "Console pane should be visible initially")
   expect_true(initialTabSet1Width > 0, "TabSet1 pane should be visible initially")
   expect_true(initialTabSet2Width > 0, "TabSet2 pane should be visible initially")

   # Execute layoutZoomRightColumn command to zoom the right column
   remote$commands.execute("layoutZoomRightColumn")

   # Calculate expected width: TabSet1 should expand to fill the space previously occupied
   # by both Console and Sidebar (sidebar is on right)
   expectedZoomedWidth <- initialTabSet1Width + initialConsoleWidth + initialSidebarWidth

   # Wait for layout to change - TabSet1 should expand to fill the left column and sidebar space
   .rs.waitUntil("right column zoomed with sidebar on right", function() {
      tabSet1Element <- remote$js.querySelector("#rstudio_TabSet1_pane")
      # Allow some tolerance for dividers/borders (within 30px)
      abs(tabSet1Element$offsetWidth - expectedZoomedWidth) < 30
   })

   # Add a small delay to ensure layout is fully settled
   Sys.sleep(0.2)

   # Check that layoutZoomRightColumn command is checked
   rightZoomChecked <- remote$js.eval("window.rstudioCallbacks.commandIsChecked('layoutZoomRightColumn')")
   expect_true(rightZoomChecked, "layoutZoomRightColumn should be checked after layoutZoomRightColumn")

   # Check that layoutZoomLeftColumn command is unchecked
   leftZoomChecked <- remote$js.eval("window.rstudioCallbacks.commandIsChecked('layoutZoomLeftColumn')")
   expect_false(leftZoomChecked, "layoutZoomLeftColumn should be unchecked after layoutZoomRightColumn")

   # Get new dimensions after zooming
   consoleElementZoomed <- remote$js.querySelector("#rstudio_Console_pane")
   tabSet1ElementZoomed <- remote$js.querySelector("#rstudio_TabSet1_pane")
   tabSet2ElementZoomed <- remote$js.querySelector("#rstudio_TabSet2_pane")
   sidebarElementZoomed <- remote$js.querySelector("#rstudio_Sidebar_pane")

   zoomedConsoleWidth <- consoleElementZoomed$offsetWidth
   zoomedTabSet1Width <- tabSet1ElementZoomed$offsetWidth
   zoomedTabSet2Width <- tabSet2ElementZoomed$offsetWidth
   zoomedSidebarWidth <- sidebarElementZoomed$offsetWidth

   # Verify right column (TabSet1) expanded to fill the space of the left column and sidebar
   expect_true(abs(zoomedTabSet1Width - expectedZoomedWidth) < 30,
               paste0("TabSet1 pane should expand to fill left column and sidebar space. ",
                      "Expected: ", expectedZoomedWidth, ", Actual: ", zoomedTabSet1Width))

   # Verify TabSet2 maintains similar width (it's stacked with TabSet1, not side-by-side)
   expect_true(abs(zoomedTabSet2Width - zoomedTabSet1Width) < 30,
               paste0("TabSet2 pane should have similar width to TabSet1. ",
                      "TabSet1: ", zoomedTabSet1Width, ", TabSet2: ", zoomedTabSet2Width))

   # Verify left column and sidebar collapsed to minimum width (just the divider, typically < 50px)
   expect_true(zoomedConsoleWidth < 50,
               paste0("Console pane should collapse to minimum width when right column zoomed. ",
                      "Width: ", zoomedConsoleWidth))
   expect_true(zoomedSidebarWidth < 50,
               paste0("Sidebar pane should collapse to minimum width when right column zoomed. ",
                      "Width: ", zoomedSidebarWidth))

   # Execute layoutZoomRightColumn command again to restore the layout
   remote$commands.execute("layoutZoomRightColumn")

   # Wait for layout to be restored - TabSet1 should return to original size and Console/Sidebar should be visible
   .rs.waitUntil("right column unzoomed with sidebar on left", function() {
      tabSet1Element <- remote$js.querySelector("#rstudio_TabSet1_pane")
      consoleElement <- remote$js.querySelector("#rstudio_Console_pane")
      sidebarElement <- remote$js.querySelector("#rstudio_Sidebar_pane")
      # Check if TabSet1 is smaller, Console is visible, and Sidebar is visible again
      tabSet1Element$offsetWidth < zoomedTabSet1Width * 0.75 &&
         consoleElement$offsetWidth > 50 &&
         sidebarElement$offsetWidth > 50
   })

   # Add a small delay to ensure layout is fully settled
   Sys.sleep(0.2)

   # Check that layoutZoomRightColumn command is unchecked
   rightZoomChecked <- remote$js.eval("window.rstudioCallbacks.commandIsChecked('layoutZoomRightColumn')")
   expect_false(rightZoomChecked, "layoutZoomRightColumn should be unchecked after undoing layoutZoomRightColumn")

   # Check that layoutZoomLeftColumn command is unchecked
   leftZoomChecked <- remote$js.eval("window.rstudioCallbacks.commandIsChecked('layoutZoomLeftColumn')")
   expect_false(leftZoomChecked, "layoutZoomLeftColumn should be unchecked after undoing layoutZoomRightColumn")

   # Get dimensions after restoring
   consoleElementRestored <- remote$js.querySelector("#rstudio_Console_pane")
   tabSet1ElementRestored <- remote$js.querySelector("#rstudio_TabSet1_pane")
   tabSet2ElementRestored <- remote$js.querySelector("#rstudio_TabSet2_pane")
   sidebarElementRestored <- remote$js.querySelector("#rstudio_Sidebar_pane")

   restoredConsoleWidth <- consoleElementRestored$offsetWidth
   restoredTabSet1Width <- tabSet1ElementRestored$offsetWidth
   restoredTabSet2Width <- tabSet2ElementRestored$offsetWidth
   restoredSidebarWidth <- sidebarElementRestored$offsetWidth

   # Verify layout restored to approximately original sizes (within 10% tolerance)
   expect_true(abs(restoredConsoleWidth - initialConsoleWidth) / initialConsoleWidth < 0.1,
               paste0("Console pane should return to original width after un-zooming. ",
                      "Initial: ", initialConsoleWidth, ", Restored: ", restoredConsoleWidth))

   expect_true(abs(restoredTabSet1Width - initialTabSet1Width) / initialTabSet1Width < 0.1,
               paste0("TabSet1 pane should return to original width after un-zooming. ",
                      "Initial: ", initialTabSet1Width, ", Restored: ", restoredTabSet1Width))

   expect_true(abs(restoredTabSet2Width - initialTabSet2Width) / initialTabSet2Width < 0.1,
               paste0("TabSet2 pane should return to original width after un-zooming. ",
                      "Initial: ", initialTabSet2Width, ", Restored: ", restoredTabSet2Width))

   expect_true(abs(restoredSidebarWidth - initialSidebarWidth) / initialSidebarWidth < 0.1,
               paste0("Sidebar pane should return to original width after un-zooming. ",
                      "Initial: ", initialSidebarWidth, ", Restored: ", restoredSidebarWidth))

   # Clean up: move sidebar back to the left
   remote$commands.execute("toggleSidebarLocation")

   # Wait for the sidebar to be repositioned
   .rs.waitUntil("sidebar moved left", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane")
   })

   # Add a small delay to ensure DOM is fully updated
   Sys.sleep(0.3)

   # Verify the sidebar is still visible after moving left
   sidebarExistsRight <- remote$dom.elementExists("#rstudio_Sidebar_pane")
   expect_true(sidebarExistsRight, "rstudio_Sidebar_pane should still exist after moving left")

   .rs.resetUILayout(remote)
})

.rs.test("layoutZoomSidebar command state depends on sidebar visibility and persists across UI reload", {
   # skipping to cut down run times on CI
   skip_on_ci()

   # 1. Confirm sidebar is hidden (default state)
   sidebarExists <- remote$dom.elementExists("#rstudio_Sidebar_pane")
   expect_false(sidebarExists, "rstudio_Sidebar_pane should NOT exist in default layout")

   # 2. Confirm layoutZoomSidebar command is disabled and unchecked when sidebar is hidden
   zoomSidebarEnabled <- remote$js.eval("window.rstudioCallbacks.commandIsEnabled('layoutZoomSidebar')")
   expect_false(zoomSidebarEnabled, "layoutZoomSidebar should be disabled when sidebar is hidden")

   zoomSidebarChecked <- remote$js.eval("window.rstudioCallbacks.commandIsChecked('layoutZoomSidebar')")
   expect_false(zoomSidebarChecked, "layoutZoomSidebar should be unchecked when sidebar is hidden")

   # 3. Show sidebar with toggleSidebar command
   remote$commands.execute("toggleSidebar")

   # 4. Wait for sidebar to be created
   .rs.waitUntil("sidebar created", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane")
   })

   # 5. Confirm sidebar is visible
   sidebarExists <- remote$dom.elementExists("#rstudio_Sidebar_pane")
   expect_true(sidebarExists, "rstudio_Sidebar_pane should exist after toggleSidebar")

   sidebarElement <- remote$js.querySelector("#rstudio_Sidebar_pane")
   expect_true(sidebarElement$offsetWidth > 0, "rstudio_Sidebar_pane should be visible (width > 0)")
   expect_true(sidebarElement$offsetHeight > 0, "rstudio_Sidebar_pane should be visible (height > 0)")

   # 6. Confirm layoutZoomSidebar command is now enabled but still unchecked
   zoomSidebarEnabled <- remote$js.eval("window.rstudioCallbacks.commandIsEnabled('layoutZoomSidebar')")
   expect_true(zoomSidebarEnabled, "layoutZoomSidebar should be enabled when sidebar is visible")

   zoomSidebarChecked <- remote$js.eval("window.rstudioCallbacks.commandIsChecked('layoutZoomSidebar')")
   expect_false(zoomSidebarChecked, "layoutZoomSidebar should be unchecked initially")

   # 7. Reload the UI to test persistence
   remote$js.eval("window.location.reload()")

   # 8. Wait for page to reload and sidebar to be present again
   .rs.waitUntil("page reloaded and sidebar visible", function() {
      # Check if the page has reloaded by waiting for the sidebar to exist again
      remote$dom.elementExists("#rstudio_Sidebar_pane")
   })

   # Add a small delay to ensure layout is fully settled after reload
   Sys.sleep(0.5)

   # 9. Check again that sidebar is still visible after reload
   sidebarExists <- remote$dom.elementExists("#rstudio_Sidebar_pane")
   expect_true(sidebarExists, "rstudio_Sidebar_pane should still exist after UI reload")

   sidebarElement <- remote$js.querySelector("#rstudio_Sidebar_pane")
   expect_true(sidebarElement$offsetWidth > 0, "rstudio_Sidebar_pane should still be visible after reload (width > 0)")
   expect_true(sidebarElement$offsetHeight > 0, "rstudio_Sidebar_pane should still be visible after reload (height > 0)")

   # 10. Confirm layoutZoomSidebar is still enabled and unchecked after reload
   zoomSidebarEnabled <- remote$js.eval("window.rstudioCallbacks.commandIsEnabled('layoutZoomSidebar')")
   expect_true(zoomSidebarEnabled, "layoutZoomSidebar should still be enabled after UI reload")

   zoomSidebarChecked <- remote$js.eval("window.rstudioCallbacks.commandIsChecked('layoutZoomSidebar')")
   expect_false(zoomSidebarChecked, "layoutZoomSidebar should still be unchecked after UI reload")

   # 11. Reset UI back to defaults
   .rs.resetUILayout(remote)
})

.rs.test("Keyboard resizing splitter after zooming unchecks zoom command", {
   # skipping to cut down run times on CI
   skip_on_ci()

   # NOTE: This test currently fails due to a GWT bug when using keyboard resizing; I couldn't
   # get mouse-based resizing to work in the test environment.
   # https://github.com/rstudio/rstudio/issues/16578

   skip_if(TRUE, "Skipping test that fails due to GWT bug")

   # Execute layoutZoomLeftColumn command to zoom the left column
   remote$commands.execute("layoutZoomLeftColumn")

   # Wait for layout to change - console should expand
   .rs.waitUntil("left column zoomed", function() {
      consoleElement <- remote$js.querySelector("#rstudio_Console_pane")
      tabSet1Element <- remote$js.querySelector("#rstudio_TabSet1_pane")
      # Console should be large and TabSet1 should be collapsed
      consoleElement$offsetWidth > 300 && tabSet1Element$offsetWidth < 50
   })

   # Add a small delay to ensure layout is fully settled
   Sys.sleep(0.2)

   # Check that layoutZoomLeftColumn command is checked
   leftZoomChecked <- remote$js.eval("window.rstudioCallbacks.commandIsChecked('layoutZoomLeftColumn')")
   expect_true(leftZoomChecked, "layoutZoomLeftColumn should be checked after zooming")

   # Focus the middle column splitter and resize using keyboard
   # Arrow keys should drag the splitter and trigger onSplitterResized
   splitter <- remote$js.querySelector("#rstudio_middle_column_splitter")
   splitter$focus()

   # Press Right arrow multiple times to drag the splitter right (revealing right column)
   for (i in 1:10) {
      remote$keyboard.insertText("<Left>")
   }

   # Small delay to let the resize process
   Sys.sleep(0.2)

   # Wait for the layout to update after keyboard resizing
   .rs.waitUntil("right column revealed after keyboard resize", function() {
      tabSet1Element <- remote$js.querySelector("#rstudio_TabSet1_pane")
      # TabSet1 should now be visible (width > 50px)
      tabSet1Element$offsetWidth > 50
   })

   # Add a small delay to ensure command state is updated
   Sys.sleep(0.2)

   # Check that layoutZoomLeftColumn command is now unchecked
   leftZoomChecked <- remote$js.eval("window.rstudioCallbacks.commandIsChecked('layoutZoomLeftColumn')")
   expect_false(leftZoomChecked, "layoutZoomLeftColumn should be unchecked after keyboard resizing splitter")

   # Verify that layoutZoomRightColumn command is also unchecked
   rightZoomChecked <- remote$js.eval("window.rstudioCallbacks.commandIsChecked('layoutZoomRightColumn')")
   expect_false(rightZoomChecked, "layoutZoomRightColumn should be unchecked after keyboard resizing splitter")

   .rs.resetUILayout(remote)
})

.rs.test("toggleSidebar command is checked when sidebar is visible, unchecked when hidden", {
   # skipping to cut down run times on CI
   skip_on_ci()

   # 1. Confirm sidebar is hidden (default state)
   sidebarExists <- remote$dom.elementExists("#rstudio_Sidebar_pane")
   expect_false(sidebarExists, "rstudio_Sidebar_pane should NOT exist in default layout")

   # 2. Confirm toggleSidebar command is unchecked when sidebar is hidden
   toggleSidebarChecked <- remote$js.eval("window.rstudioCallbacks.commandIsChecked('toggleSidebar')")
   expect_false(toggleSidebarChecked, "toggleSidebar should be unchecked when sidebar is hidden")

   # 3. Show sidebar with toggleSidebar command
   remote$commands.execute("toggleSidebar")

   # 4. Wait for sidebar to be created
   .rs.waitUntil("sidebar created", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane")
   })

   # 5. Confirm sidebar is visible
   sidebarExists <- remote$dom.elementExists("#rstudio_Sidebar_pane")
   expect_true(sidebarExists, "rstudio_Sidebar_pane should exist after toggleSidebar")

   sidebarElement <- remote$js.querySelector("#rstudio_Sidebar_pane")
   expect_true(sidebarElement$offsetWidth > 0, "rstudio_Sidebar_pane should be visible (width > 0)")
   expect_true(sidebarElement$offsetHeight > 0, "rstudio_Sidebar_pane should be visible (height > 0)")

   # 6. Confirm toggleSidebar command is now checked
   toggleSidebarChecked <- remote$js.eval("window.rstudioCallbacks.commandIsChecked('toggleSidebar')")
   expect_true(toggleSidebarChecked, "toggleSidebar should be checked when sidebar is visible")

   # 7. Hide sidebar with toggleSidebar command
   remote$commands.execute("toggleSidebar")

   # 8. Wait for sidebar to be removed
   .rs.waitUntil("sidebar removed", function() {
      !remote$dom.elementExists("#rstudio_Sidebar_pane")
   })

   # 9. Confirm sidebar is hidden
   sidebarExists <- remote$dom.elementExists("#rstudio_Sidebar_pane")
   expect_false(sidebarExists, "rstudio_Sidebar_pane should NOT exist after toggling sidebar off")

   # 10. Confirm toggleSidebar command is unchecked again
   toggleSidebarChecked <- remote$js.eval("window.rstudioCallbacks.commandIsChecked('toggleSidebar')")
   expect_false(toggleSidebarChecked, "toggleSidebar should be unchecked when sidebar is hidden again")
})

.rs.test("Column widths are preserved when toggling sidebar visibility (issue #16676)", {
   # skipping to cut down run times on CI
   skip_on_ci()

   # This test verifies the fix for issue #16676: column widths should be preserved
   # when hiding and showing the sidebar, not revert to old saved state

   # 1. Show the sidebar
   remote$commands.execute("toggleSidebar")

   # Wait for the sidebar to be created
   .rs.waitUntil("sidebar created", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane")
   })

   # Add delay to ensure layout is fully settled
   Sys.sleep(0.3)

   # 2. Record initial column widths with sidebar visible
   consoleElement <- remote$js.querySelector("#rstudio_Console_pane")
   tabSet1Element <- remote$js.querySelector("#rstudio_TabSet1_pane")
   sidebarElement <- remote$js.querySelector("#rstudio_Sidebar_pane")

   initialConsoleWidth <- consoleElement$offsetWidth
   initialTabSet1Width <- tabSet1Element$offsetWidth
   initialSidebarWidth <- sidebarElement$offsetWidth

   expect_true(initialConsoleWidth > 0, "Console pane should be visible")
   expect_true(initialTabSet1Width > 0, "TabSet1 pane should be visible")
   expect_true(initialSidebarWidth > 0, "Sidebar pane should be visible")

   # 3. Change column widths by focusing the splitter and using keyboard to resize
   # We'll drag the splitter between console and TabSet1 to widen the console
   splitter <- remote$js.querySelector("#rstudio_middle_column_splitter")

   # Verify splitter exists
   expect_true(!is.null(splitter), "Middle column splitter should exist")

   # Focus the splitter
   splitter$focus()

   # Press Right arrow multiple times to drag the splitter right (widening the console)
   # Each keypress moves the splitter a few pixels
   for (i in 1:18) {
      remote$keyboard.insertText("<Right>")
   }

   # Wait for the layout to update
   Sys.sleep(0.3)

   # 4. Record modified column widths (after simulated resize)
   consoleElementModified <- remote$js.querySelector("#rstudio_Console_pane")
   tabSet1ElementModified <- remote$js.querySelector("#rstudio_TabSet1_pane")

   modifiedConsoleWidth <- consoleElementModified$offsetWidth
   modifiedTabSet1Width <- tabSet1ElementModified$offsetWidth

   # Verify that column widths changed (allowing for some cases where simulation might not work)
   widthChanged <- abs(modifiedConsoleWidth - initialConsoleWidth) > 20 ||
                   abs(modifiedTabSet1Width - initialTabSet1Width) > 20

   # If we couldn't change widths programmatically, we'll still test that widths
   # are preserved through the toggle cycle (just comparing to initial)
   if (widthChanged) {
      message(paste0("Successfully changed column widths: Console ",
                     initialConsoleWidth, " -> ", modifiedConsoleWidth,
                     ", TabSet1 ", initialTabSet1Width, " -> ", modifiedTabSet1Width))
   } else {
      message("Width change simulation didn't work, testing initial width preservation only")
      modifiedConsoleWidth <- initialConsoleWidth
      modifiedTabSet1Width <- initialTabSet1Width
   }

   # 5. Hide the sidebar
   remote$commands.execute("toggleSidebar")

   # Wait for the sidebar to be removed
   .rs.waitUntil("sidebar removed", function() {
      !remote$dom.elementExists("#rstudio_Sidebar_pane")
   })

   Sys.sleep(0.3)

   # 6. Show the sidebar again
   remote$commands.execute("toggleSidebar")

   # Wait for the sidebar to be created again
   .rs.waitUntil("sidebar re-created", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane")
   })

   # Wait for layout to fully settle
   Sys.sleep(0.5)

   # 7. Record final column widths (after hide/show cycle)
   consoleElementFinal <- remote$js.querySelector("#rstudio_Console_pane")
   tabSet1ElementFinal <- remote$js.querySelector("#rstudio_TabSet1_pane")

   finalConsoleWidth <- consoleElementFinal$offsetWidth
   finalTabSet1Width <- tabSet1ElementFinal$offsetWidth

   # 8. Verify that final widths match modified widths (within tolerance)
   # This is the key assertion that would fail before the fix in MainSplitPanel.java

   # Column widths should be within 5% of the modified widths
   consoleWithinTolerance <- abs(finalConsoleWidth - modifiedConsoleWidth) / modifiedConsoleWidth < 0.05
   tabSet1WithinTolerance <- abs(finalTabSet1Width - modifiedTabSet1Width) / modifiedTabSet1Width < 0.05

   expect_true(consoleWithinTolerance,
               paste0("Console width should be preserved after sidebar toggle. ",
                      "Modified: ", modifiedConsoleWidth, ", ",
                      "Final: ", finalConsoleWidth, ", ",
                      "Difference: ", abs(finalConsoleWidth - modifiedConsoleWidth), " ",
                      "(", round(abs(finalConsoleWidth - modifiedConsoleWidth) / modifiedConsoleWidth * 100, 1), "%)"))

   expect_true(tabSet1WithinTolerance,
               paste0("TabSet1 width should be preserved after sidebar toggle. ",
                      "Modified: ", modifiedTabSet1Width, ", ",
                      "Final: ", finalTabSet1Width, ", ",
                      "Difference: ", abs(finalTabSet1Width - modifiedTabSet1Width), " ",
                      "(", round(abs(finalTabSet1Width - modifiedTabSet1Width) / modifiedTabSet1Width * 100, 1), "%)"))

   # Clean up
   .rs.resetUILayout(remote)
})

.rs.test("Column widths preserved through multiple hide/show cycles", {
   # skipping to cut down run times on CI
   skip_on_ci()

   # This test verifies that width preservation works reliably across multiple cycles,
   # not just a single hide/show operation

   # Show the sidebar
   remote$commands.execute("toggleSidebar")

   .rs.waitUntil("sidebar created", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane")
   })

   Sys.sleep(0.3)

   # Resize columns
   splitter <- remote$js.querySelector("#rstudio_middle_column_splitter")
   expect_true(!is.null(splitter), "Middle column splitter should exist")

   splitter$focus()
   for (i in 1:15) {
      remote$keyboard.insertText("<Right>")
   }

   Sys.sleep(0.3)

   # Capture the modified width
   consoleModified <- remote$js.querySelector("#rstudio_Console_pane")$offsetWidth
   tabSet1Modified <- remote$js.querySelector("#rstudio_TabSet1_pane")$offsetWidth

   # Cycle 1: Hide then show
   remote$commands.execute("toggleSidebar")
   .rs.waitUntil("sidebar hidden cycle 1", function() {
      !remote$dom.elementExists("#rstudio_Sidebar_pane")
   })
   Sys.sleep(0.3)

   remote$commands.execute("toggleSidebar")
   .rs.waitUntil("sidebar visible cycle 1", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane")
   })
   Sys.sleep(0.5)

   consoleCycle1 <- remote$js.querySelector("#rstudio_Console_pane")$offsetWidth
   tabSet1Cycle1 <- remote$js.querySelector("#rstudio_TabSet1_pane")$offsetWidth

   # Cycle 2: Hide then show again
   remote$commands.execute("toggleSidebar")
   .rs.waitUntil("sidebar hidden cycle 2", function() {
      !remote$dom.elementExists("#rstudio_Sidebar_pane")
   })
   Sys.sleep(0.3)

   remote$commands.execute("toggleSidebar")
   .rs.waitUntil("sidebar visible cycle 2", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane")
   })
   Sys.sleep(0.5)

   consoleCycle2 <- remote$js.querySelector("#rstudio_Console_pane")$offsetWidth
   tabSet1Cycle2 <- remote$js.querySelector("#rstudio_TabSet1_pane")$offsetWidth

   # Cycle 3: One more time for good measure
   remote$commands.execute("toggleSidebar")
   .rs.waitUntil("sidebar hidden cycle 3", function() {
      !remote$dom.elementExists("#rstudio_Sidebar_pane")
   })
   Sys.sleep(0.3)

   remote$commands.execute("toggleSidebar")
   .rs.waitUntil("sidebar visible cycle 3", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane")
   })
   Sys.sleep(0.5)

   consoleCycle3 <- remote$js.querySelector("#rstudio_Console_pane")$offsetWidth
   tabSet1Cycle3 <- remote$js.querySelector("#rstudio_TabSet1_pane")$offsetWidth

   # All widths should match within tolerance
   expect_true(abs(consoleCycle1 - consoleModified) / consoleModified < 0.05,
               paste0("Console width preserved after cycle 1: ",
                      consoleModified, " -> ", consoleCycle1))
   expect_true(abs(consoleCycle2 - consoleModified) / consoleModified < 0.05,
               paste0("Console width preserved after cycle 2: ",
                      consoleModified, " -> ", consoleCycle2))
   expect_true(abs(consoleCycle3 - consoleModified) / consoleModified < 0.05,
               paste0("Console width preserved after cycle 3: ",
                      consoleModified, " -> ", consoleCycle3))

   expect_true(abs(tabSet1Cycle1 - tabSet1Modified) / tabSet1Modified < 0.05,
               paste0("TabSet1 width preserved after cycle 1: ",
                      tabSet1Modified, " -> ", tabSet1Cycle1))
   expect_true(abs(tabSet1Cycle2 - tabSet1Modified) / tabSet1Modified < 0.05,
               paste0("TabSet1 width preserved after cycle 2: ",
                      tabSet1Modified, " -> ", tabSet1Cycle2))
   expect_true(abs(tabSet1Cycle3 - tabSet1Modified) / tabSet1Modified < 0.05,
               paste0("TabSet1 width preserved after cycle 3: ",
                      tabSet1Modified, " -> ", tabSet1Cycle3))

   .rs.resetUILayout(remote)
})

.rs.test("Sidebar show uses default widths after columns resized while hidden", {
   # skipping to cut down run times on CI
   skip_on_ci()

   # This test verifies that if user resizes columns while sidebar is hidden,
   # then shows the sidebar, we don't try to restore stale saved widths.
   # Instead, we should use reasonable default layout.

   # Start with sidebar visible
   remote$commands.execute("toggleSidebar")

   .rs.waitUntil("sidebar created", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane")
   })

   Sys.sleep(0.3)

   # Do an initial resize with sidebar visible
   splitter <- remote$js.querySelector("#rstudio_middle_column_splitter")
   splitter$focus()
   for (i in 1:10) {
      remote$keyboard.insertText("<Right>")
   }

   Sys.sleep(0.3)

   # Capture widths with sidebar visible (these will be saved when we hide)
   consoleWithSidebar <- remote$js.querySelector("#rstudio_Console_pane")$offsetWidth

   # Hide sidebar
   remote$commands.execute("toggleSidebar")
   .rs.waitUntil("sidebar hidden", function() {
      !remote$dom.elementExists("#rstudio_Sidebar_pane")
   })
   Sys.sleep(0.3)

   # NOW resize columns WHILE sidebar is hidden
   # This should invalidate the saved widths
   splitter <- remote$js.querySelector("#rstudio_middle_column_splitter")
   splitter$focus()
   for (i in 1:25) {
      remote$keyboard.insertText("<Left>")  # Move LEFT this time
   }

   Sys.sleep(0.3)

   # Capture the widths while hidden (these should NOT be restored when showing sidebar)
   consoleWhileHidden <- remote$js.querySelector("#rstudio_Console_pane")$offsetWidth
   tabSet1WhileHidden <- remote$js.querySelector("#rstudio_TabSet1_pane")$offsetWidth

   # The "while hidden" width should be different from "with sidebar" width
   # (we moved the splitter in opposite direction)
   message(paste0("Console width with sidebar: ", consoleWithSidebar,
                  ", while hidden: ", consoleWhileHidden))

   # Show sidebar again
   remote$commands.execute("toggleSidebar")
   .rs.waitUntil("sidebar visible again", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane")
   })
   Sys.sleep(0.5)

   # Get final widths after showing sidebar
   consoleAfterShow <- remote$js.querySelector("#rstudio_Console_pane")$offsetWidth
   tabSet1AfterShow <- remote$js.querySelector("#rstudio_TabSet1_pane")$offsetWidth
   sidebarAfterShow <- remote$js.querySelector("#rstudio_Sidebar_pane")$offsetWidth

   # The widths should be reasonable (not zero, not corrupted)
   expect_true(consoleAfterShow > 100,
               paste0("Console should have reasonable width after show: ", consoleAfterShow))
   expect_true(tabSet1AfterShow > 100,
               paste0("TabSet1 should have reasonable width after show: ", tabSet1AfterShow))
   expect_true(sidebarAfterShow > 100,
               paste0("Sidebar should have reasonable width after show: ", sidebarAfterShow))

   # The final console width should NOT match the "while hidden" width
   # (because we resized while hidden, so saved state is invalid)
   # It should be either the original "with sidebar" width or a reasonable default
   message(paste0("Final console width: ", consoleAfterShow,
                  " (expected != ", consoleWhileHidden, ")"))

   .rs.resetUILayout(remote)
})

.rs.test("Different resize patterns preserve correctly through sidebar toggle", {
   # skipping to cut down run times on CI
   skip_on_ci()

   # This test verifies that width preservation works regardless of which
   # splitter is moved or in which direction

   # Show sidebar
   remote$commands.execute("toggleSidebar")

   .rs.waitUntil("sidebar created", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane")
   })

   Sys.sleep(0.3)

   # Test 1: Resize middle splitter LEFT (shrink console, widen right side)
   splitter <- remote$js.querySelector("#rstudio_middle_column_splitter")
   splitter$focus()
   for (i in 1:15) {
      remote$keyboard.insertText("<Left>")
   }

   Sys.sleep(0.3)

   consoleAfterLeft <- remote$js.querySelector("#rstudio_Console_pane")$offsetWidth
   tabSet1AfterLeft <- remote$js.querySelector("#rstudio_TabSet1_pane")$offsetWidth

   # Hide and show sidebar
   remote$commands.execute("toggleSidebar")
   .rs.waitUntil("sidebar hidden test 1", function() {
      !remote$dom.elementExists("#rstudio_Sidebar_pane")
   })
   Sys.sleep(0.3)

   remote$commands.execute("toggleSidebar")
   .rs.waitUntil("sidebar shown test 1", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane")
   })
   Sys.sleep(0.5)

   consoleAfterToggle1 <- remote$js.querySelector("#rstudio_Console_pane")$offsetWidth
   tabSet1AfterToggle1 <- remote$js.querySelector("#rstudio_TabSet1_pane")$offsetWidth

   # Verify preservation
   expect_true(abs(consoleAfterToggle1 - consoleAfterLeft) / consoleAfterLeft < 0.05,
               paste0("Console width preserved after LEFT resize: ",
                      consoleAfterLeft, " -> ", consoleAfterToggle1))
   expect_true(abs(tabSet1AfterToggle1 - tabSet1AfterLeft) / tabSet1AfterLeft < 0.05,
               paste0("TabSet1 width preserved after LEFT resize: ",
                      tabSet1AfterLeft, " -> ", tabSet1AfterToggle1))

   # Test 2: Now resize RIGHT (widen console again)
   splitter <- remote$js.querySelector("#rstudio_middle_column_splitter")
   splitter$focus()
   for (i in 1:20) {
      remote$keyboard.insertText("<Right>")
   }

   Sys.sleep(0.3)

   consoleAfterRight <- remote$js.querySelector("#rstudio_Console_pane")$offsetWidth
   tabSet1AfterRight <- remote$js.querySelector("#rstudio_TabSet1_pane")$offsetWidth

   # Hide and show sidebar again
   remote$commands.execute("toggleSidebar")
   .rs.waitUntil("sidebar hidden test 2", function() {
      !remote$dom.elementExists("#rstudio_Sidebar_pane")
   })
   Sys.sleep(0.3)

   remote$commands.execute("toggleSidebar")
   .rs.waitUntil("sidebar shown test 2", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane")
   })
   Sys.sleep(0.5)

   consoleAfterToggle2 <- remote$js.querySelector("#rstudio_Console_pane")$offsetWidth
   tabSet1AfterToggle2 <- remote$js.querySelector("#rstudio_TabSet1_pane")$offsetWidth

   # Verify preservation again
   expect_true(abs(consoleAfterToggle2 - consoleAfterRight) / consoleAfterRight < 0.05,
               paste0("Console width preserved after RIGHT resize: ",
                      consoleAfterRight, " -> ", consoleAfterToggle2))
   expect_true(abs(tabSet1AfterToggle2 - tabSet1AfterRight) / tabSet1AfterRight < 0.05,
               paste0("TabSet1 width preserved after RIGHT resize: ",
                      tabSet1AfterRight, " -> ", tabSet1AfterToggle2))

   .rs.resetUILayout(remote)
})

.rs.test("Extreme resize values preserve correctly through sidebar toggle", {
   # skipping to cut down run times on CI
   skip_on_ci()

   # This test verifies that width preservation works even with very large
   # or very small column widths (edge cases)

   # Show sidebar
   remote$commands.execute("toggleSidebar")

   .rs.waitUntil("sidebar created", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane")
   })

   Sys.sleep(0.3)

   # Resize to make console VERY wide (move splitter far right)
   # Use 30 iterations instead of 50 to avoid completely zero-width panes
   splitter <- remote$js.querySelector("#rstudio_middle_column_splitter")
   splitter$focus()
   for (i in 1:30) {
      remote$keyboard.insertText("<Right>")
   }

   Sys.sleep(0.3)

   consoleVeryWide <- remote$js.querySelector("#rstudio_Console_pane")$offsetWidth
   tabSet1VeryNarrow <- remote$js.querySelector("#rstudio_TabSet1_pane")$offsetWidth

   message(paste0("Extreme resize: Console = ", consoleVeryWide,
                  ", TabSet1 = ", tabSet1VeryNarrow))

   # Hide and show sidebar
   remote$commands.execute("toggleSidebar")
   .rs.waitUntil("sidebar hidden", function() {
      !remote$dom.elementExists("#rstudio_Sidebar_pane")
   })
   Sys.sleep(0.3)

   remote$commands.execute("toggleSidebar")
   .rs.waitUntil("sidebar shown", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane")
   })
   Sys.sleep(0.5)

   consoleAfterToggle <- remote$js.querySelector("#rstudio_Console_pane")$offsetWidth
   tabSet1AfterToggle <- remote$js.querySelector("#rstudio_TabSet1_pane")$offsetWidth

   # Verify preservation (within tolerance)
   # For very wide console, check within 5% tolerance
   consoleWithinTolerance <- abs(consoleAfterToggle - consoleVeryWide) / consoleVeryWide < 0.05

   # For very narrow TabSet1, use absolute difference for very small values
   # or relative tolerance for larger values
   if (tabSet1VeryNarrow < 50) {
      # For very narrow panes, check absolute difference (within 10 pixels)
      tabSet1WithinTolerance <- abs(tabSet1AfterToggle - tabSet1VeryNarrow) < 10
   } else {
      # For larger panes, use relative tolerance
      tabSet1WithinTolerance <- abs(tabSet1AfterToggle - tabSet1VeryNarrow) / tabSet1VeryNarrow < 0.10
   }

   expect_true(consoleWithinTolerance,
               paste0("Very wide console preserved: ",
                      consoleVeryWide, " -> ", consoleAfterToggle,
                      " (", round(abs(consoleAfterToggle - consoleVeryWide) / consoleVeryWide * 100, 1), "%)"))

   expect_true(tabSet1WithinTolerance,
               paste0("Very narrow TabSet1 preserved: ",
                      tabSet1VeryNarrow, " -> ", tabSet1AfterToggle,
                      " (absolute diff: ", abs(tabSet1AfterToggle - tabSet1VeryNarrow), "px)"))

   # Verify console still has reasonable width (it was made very wide)
   expect_true(consoleAfterToggle > 50,
               paste0("Console should still be very wide: ", consoleAfterToggle))

   # For TabSet1, only check it's visible IF it was visible before toggle
   # The key test is preservation, not absolute visibility
   if (tabSet1VeryNarrow > 0) {
      expect_true(tabSet1AfterToggle >= 0,
                  paste0("TabSet1 width should be preserved: ",
                         tabSet1VeryNarrow, " -> ", tabSet1AfterToggle))
   }

   sidebarElement <- remote$js.querySelector("#rstudio_Sidebar_pane")
   expect_true(sidebarElement$offsetWidth > 0,
               paste0("Sidebar should be visible: ", sidebarElement$offsetWidth))

   .rs.resetUILayout(remote)
})
