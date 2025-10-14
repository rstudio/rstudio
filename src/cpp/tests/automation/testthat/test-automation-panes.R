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
   
   # Check that Customize Panes button exists and is hidden
   expect_true(remote$dom.elementExists(CUSTOMIZE_PANES_BUTTON), "rstudio_customize_panes element should exist")
   customizePanesButtonElement <- remote$js.querySelector("#rstudio_customize_panes")
   expect_false(customizePanesButtonElement$offsetWidth > 0, "rstudio_customize_panes should be visible (width > 0)")
   expect_false(customizePanesButtonElement$offsetHeight > 0, "rstudio_customize_panes should be visible (height > 0)")

   # Hide the sidebar
   remote$commands.execute("toggleSidebar")
   
   # Wait for the sidebar to be removed
   .rs.waitUntil("sidebar removed", function() {
      !remote$dom.elementExists("#rstudio_Sidebar_pane")
   })
   
   # Verify the sidebar no longer exists
   sidebarExists <- remote$dom.elementExists("#rstudio_Sidebar_pane")
   expect_false(sidebarExists, "rstudio_Sidebar_pane should NOT exist after toggling sidebar off")

   # Check that Customize Panes button no longer exists
   expect_false(remote$dom.elementExists(CUSTOMIZE_PANES_BUTTON), "rstudio_customize_panes element should NOT exist")
})

.rs.test("Sidebar can be moved left and right with moveSidebar commands", {
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
   # The sidebar starts on the right by default
   initialPosition <- sidebarElement$getBoundingClientRect()$left
   
   # Move sidebar to the left
   remote$commands.execute("moveSidebarLeft")
   
   # Wait for the sidebar to be repositioned (it gets recreated in new location)
   .rs.waitUntil("sidebar moved left", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane")
   })

   # Add a small delay to ensure DOM is fully updated after recreation
   Sys.sleep(0.3)
   
   # Verify the sidebar still exists and is visible after moving left
   leftSidebarExists <- remote$dom.elementExists("#rstudio_Sidebar_pane")
   expect_true(leftSidebarExists, "rstudio_Sidebar_pane should still exist after moveSidebarLeft")
   
   leftSidebarElement <- remote$js.querySelector("#rstudio_Sidebar_pane")
   expect_true(leftSidebarElement$offsetWidth > 0, "rstudio_Sidebar_pane should still be visible after moveSidebarLeft")

   # Check that the sidebar is now positioned to the left using screen coordinates
   leftPosition <- leftSidebarElement$getBoundingClientRect()$left
   expect_true(leftPosition < initialPosition,
               paste0("Sidebar should be positioned to the left after moveSidebarLeft. ",
                      "Initial position: ", initialPosition, ", Left position: ", leftPosition))

   # Get fresh console position after layout change and verify sidebar is to the left
   consoleElement <- remote$js.querySelector("#rstudio_Console_pane")
   consolePosition <- consoleElement$getBoundingClientRect()$left
   expect_true(leftPosition < consolePosition,
               paste0("Sidebar should be to the left of the console pane. ",
                      "Sidebar position: ", leftPosition, ", Console position: ", consolePosition))
   
   # Move sidebar back to the right
   remote$commands.execute("moveSidebarRight")
   
   # Wait for the sidebar to be repositioned again
   .rs.waitUntil("sidebar moved right", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane")
   })

   # Add a small delay to ensure DOM is fully updated after recreation
   Sys.sleep(0.3)
   
   # Verify the sidebar still exists and is visible after moving right
   rightSidebarExists <- remote$dom.elementExists("#rstudio_Sidebar_pane")
   expect_true(rightSidebarExists, "rstudio_Sidebar_pane should still exist after moveSidebarRight")
   
   rightSidebarElement <- remote$js.querySelector("#rstudio_Sidebar_pane")
   expect_true(rightSidebarElement$offsetWidth > 0, "rstudio_Sidebar_pane should still be visible after moveSidebarRight")

   # Check that the sidebar is now positioned to the right using screen coordinates
   rightPosition <- rightSidebarElement$getBoundingClientRect()$left
   expect_true(rightPosition > leftPosition,
               paste0("Sidebar should be positioned to the right after moveSidebarRight. ",
                      "Left position: ", leftPosition, ", Right position: ", rightPosition))

   # Get fresh console position after layout change and verify sidebar is to the right
   consoleElement <- remote$js.querySelector("#rstudio_Console_pane")
   consolePositionRight <- consoleElement$getBoundingClientRect()$left
   expect_true(rightPosition > consolePositionRight,
               paste0("Sidebar should be to the right of the console pane. ",
                      "Sidebar position: ", rightPosition, ", Console position: ", consolePositionRight))
   
   # Clean up: hide the sidebar
   remote$commands.execute("toggleSidebar")
   
   # Wait for the sidebar to be removed
   .rs.waitUntil("sidebar removed", function() {
      !remote$dom.elementExists("#rstudio_Sidebar_pane")
   })
})

.rs.test("Zoomed left column with sidebar hidden works as expected", {
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
})

.rs.test("Zoomed left column with sidebar visible works as expected", {
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
   # by both TabSet1 and Sidebar (sidebar is on right by default)
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

   # Clean up: hide the sidebar
   remote$commands.execute("toggleSidebar")

   # Wait for the sidebar to be removed
   .rs.waitUntil("sidebar removed", function() {
      !remote$dom.elementExists("#rstudio_Sidebar_pane")
   })
})

.rs.test("Zoomed right column with sidebar hidden works as expected", {
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
})

.rs.test("Zoomed right column with sidebar visible works as expected", {
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
   # by both Console and Sidebar (sidebar is on right by default)
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

   # Clean up: hide the sidebar
   remote$commands.execute("toggleSidebar")

   # Wait for the sidebar to be removed
   .rs.waitUntil("sidebar removed", function() {
      !remote$dom.elementExists("#rstudio_Sidebar_pane")
   })
})

.rs.test("Zoomed left column with sidebar on left works as expected", {
   # First make the sidebar visible
   remote$commands.execute("toggleSidebar")

   # Wait for the sidebar to be created
   .rs.waitUntil("sidebar created", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane")
   })

   # Move sidebar to the left
   remote$commands.execute("moveSidebarLeft")

   # Wait for the sidebar to be repositioned
   .rs.waitUntil("sidebar moved left", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane")
   })

   # Add a small delay to ensure DOM is fully updated after recreation
   Sys.sleep(0.3)

   # Verify the sidebar exists and is visible on the left
   sidebarExists <- remote$dom.elementExists("#rstudio_Sidebar_pane")
   expect_true(sidebarExists, "rstudio_Sidebar_pane should exist after moving to left")

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
   # by both TabSet1 and Sidebar (sidebar is on left now)
   expectedZoomedWidth <- initialConsoleWidth + initialTabSet1Width + initialSidebarWidth

   # Wait for layout to change - console should expand to fill the right column and sidebar space
   .rs.waitUntil("left column zoomed with sidebar on left", function() {
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
   .rs.waitUntil("left column unzoomed with sidebar on left", function() {
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

   # Clean up: move sidebar back to the right
   remote$commands.execute("moveSidebarRight")

   # Wait for the sidebar to be repositioned
   .rs.waitUntil("sidebar moved right", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane")
   })

   # Add a small delay to ensure DOM is fully updated
   Sys.sleep(0.3)

   # Verify the sidebar is still visible after moving right
   sidebarExistsRight <- remote$dom.elementExists("#rstudio_Sidebar_pane")
   expect_true(sidebarExistsRight, "rstudio_Sidebar_pane should still exist after moving right")

   # Hide the sidebar
   remote$commands.execute("toggleSidebar")

   # Wait for the sidebar to be removed
   .rs.waitUntil("sidebar removed", function() {
      !remote$dom.elementExists("#rstudio_Sidebar_pane")
   })

   # Verify the sidebar is now hidden
   sidebarExists <- remote$dom.elementExists("#rstudio_Sidebar_pane")
   expect_false(sidebarExists, "rstudio_Sidebar_pane should NOT exist after toggling off")
})

.rs.test("Zoomed right column with sidebar on left works as expected", {
   # First make the sidebar visible
   remote$commands.execute("toggleSidebar")

   # Wait for the sidebar to be created
   .rs.waitUntil("sidebar created", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane")
   })

   # Move sidebar to the left
   remote$commands.execute("moveSidebarLeft")

   # Wait for the sidebar to be repositioned
   .rs.waitUntil("sidebar moved left", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane")
   })

   # Add a small delay to ensure DOM is fully updated after recreation
   Sys.sleep(0.3)

   # Verify the sidebar exists and is visible on the left
   sidebarExists <- remote$dom.elementExists("#rstudio_Sidebar_pane")
   expect_true(sidebarExists, "rstudio_Sidebar_pane should exist after moving to left")

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
   # by both Console and Sidebar (sidebar is on left)
   expectedZoomedWidth <- initialTabSet1Width + initialConsoleWidth + initialSidebarWidth

   # Wait for layout to change - TabSet1 should expand to fill the left column and sidebar space
   .rs.waitUntil("right column zoomed with sidebar on left", function() {
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

   # Clean up: move sidebar back to the right
   remote$commands.execute("moveSidebarRight")

   # Wait for the sidebar to be repositioned
   .rs.waitUntil("sidebar moved right", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane")
   })

   # Add a small delay to ensure DOM is fully updated
   Sys.sleep(0.3)

   # Verify the sidebar is still visible after moving right
   sidebarExistsRight <- remote$dom.elementExists("#rstudio_Sidebar_pane")
   expect_true(sidebarExistsRight, "rstudio_Sidebar_pane should still exist after moving right")

   # Hide the sidebar
   remote$commands.execute("toggleSidebar")

   # Wait for the sidebar to be removed
   .rs.waitUntil("sidebar removed", function() {
      !remote$dom.elementExists("#rstudio_Sidebar_pane")
   })

   # Verify the sidebar is now hidden
   sidebarExists <- remote$dom.elementExists("#rstudio_Sidebar_pane")
   expect_false(sidebarExists, "rstudio_Sidebar_pane should NOT exist after toggling off")
})
