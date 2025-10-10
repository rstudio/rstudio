# tests related to pane and column management

library(testthat)

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

