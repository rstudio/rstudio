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

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())

# Helper function to open the Pane Layout options
.rs.openPaneLayoutOptions <- function(remote) {
   remote$commands.execute("paneLayout")

   # Wait for the Options dialog to appear with Pane Layout selected
   remote$dom.waitForElement(".gwt-DialogBox")

   # Wait for pane layout panel to be visible
   remote$dom.waitForElement("#rstudio_label_pane_layout_options_panel")
}

# Helper function to get dropdown element for a quadrant
.rs.getQuadrantDropdown <- function(remote, quadrantClass) {
   return(remote$js.querySelector(paste0(quadrantClass, " select")))
}

# Helper function to get dropdown text for a quadrant
.rs.getQuadrantDropdownText <- function(remote, quadrantClass) {
   dropdown <- .rs.getQuadrantDropdown(remote, quadrantClass)
   if (!is.null(dropdown)) {
      return(dropdown$options[[dropdown$selectedIndex]]$text)
   }
   return(NULL)
}

# Helper function to select dropdown option by text
.rs.selectDropdownOption <- function(remote, quadrantClass, optionText) {
   dropdown <- .rs.getQuadrantDropdown(remote, quadrantClass)
   if (!is.null(dropdown)) {
      options <- dropdown$options
      for (i in seq_len(options$length)) {
         if (options[[i - 1L]]$text == optionText) {
            # Set the selectedIndex property directly on the DOM element
            remote$js.eval(paste0("document.querySelector('", quadrantClass, " select').selectedIndex = ", i - 1L))
            # Trigger change event
            remote$js.eval(paste0("document.querySelector('", quadrantClass, " select').dispatchEvent(new Event('change'))"))
            Sys.sleep(1) # Allow time for swap
            break
         }
      }
   }
}

# Helper function to check if a checkbox is checked
.rs.isTabChecked <- function(remote, quadrantClass, tabName) {
   # Find checkbox with label matching tabName
   # checkboxes <- remote$js.querySelectorAll(paste0(quadrantClass, " input[type='checkbox']"))
   labels <- remote$js.querySelectorAll(paste0(quadrantClass, " label"))

   for (i in seq_len(length(labels))) {
      label <- labels[[i]]
      if (grepl(tabName, label$innerText, fixed = TRUE)) {
         # Find corresponding checkbox
         forAttr <- label$getAttribute("for")
         if (!is.null(forAttr)) {
            checkbox <- remote$js.querySelector(paste0("#", forAttr))
            if (!is.null(checkbox)) {
               return(checkbox$checked)
            }
         }
      }
   }
   return(FALSE)
}

# Helper function to fetch the checked state of a list of tabs
.rs.getTabCheckedState <- function(remote, quadrantClass, tabNames) {
   # Get all labels and checkboxes in one query
   labels <- remote$js.querySelectorAll(paste0(quadrantClass, " label"))
   
   # Create a map of tab names to their checked state
   result <- logical(length(tabNames))
   names(result) <- tabNames
   
   # Process all labels once
   for (i in seq_len(length(labels))) {
      label <- labels[[i]]
      labelText <- label$innerText
      
      # Check if this label matches any of the requested tab names
      for (j in seq_along(tabNames)) {
         if (grepl(tabNames[j], labelText, fixed = TRUE)) {
            # Find corresponding checkbox
            forAttr <- label$getAttribute("for")
            if (!is.null(forAttr)) {
               checkbox <- remote$js.querySelector(paste0("#", forAttr))
               if (!is.null(checkbox)) {
                  result[j] <- checkbox$checked
                  break  # Found the checkbox for this tab
               }
            }
         }
      }
   }
   
   return(result)
}

# Helper function to toggle a tab checkbox
.rs.toggleTab <- function(remote, quadrantClass, tabName) {
   checkboxes <- remote$js.querySelectorAll(paste0(quadrantClass, " input[type='checkbox']"))

   for (i in seq_len(length(checkboxes))) {
      checkbox <- checkboxes[[i]]
      # Get the label associated with this checkbox
      checkboxId <- checkbox$id
      if (!is.null(checkboxId) && checkboxId != "") {
         label <- remote$js.querySelector(paste0("label[for='", checkboxId, "']"))
         if (!is.null(label)) {
            labelText <- label$innerText
            if (grepl(tabName, labelText, fixed = TRUE)) {
               currentState <- checkbox$checked
               newState <- !currentState

               # Scroll the checkbox into view before clicking
               # This ensures checkboxes in scrollable containers are visible
               tryCatch({
                  # First scroll the checkbox into view
                  remote$js.eval(paste0("document.querySelector('#", checkboxId, "').scrollIntoView({block: 'center', inline: 'nearest'})"))
                  Sys.sleep(0.1)  # Small wait for scroll to complete

                  # Now click the checkbox
                  selector <- paste0("#", checkboxId)
                  remote$dom.setChecked(selector, checked = newState)
                  Sys.sleep(0.2)
                  return(TRUE)
               }, error = function(e) {
                  print(e)
                  return(FALSE)
               })
            }
         }
      }
   }
   return(FALSE)
}

# Helper function to verify tabs in a quadrant dropdown
.rs.verifyQuadrantTabs <- function(remote, quadrantId, expectedTabs) {
   # Get the dropdown text for the quadrant
   quadrantText <- .rs.getQuadrantDropdownText(remote, quadrantId)
   
   # Split comma-separated text into array of strings
   quadrantTabs <- strsplit(quadrantText, ", ")[[1]]
   
   # Check for expected values
   for (expectedTab in expectedTabs) {
      expect_true(expectedTab %in% quadrantTabs, 
                  info = paste("Expected tab", expectedTab, "not found in", quadrantId))
   }
}

# Helper function to verify dropdown options for a quadrant
.rs.verifyQuadrantDropdownOptions <- function(remote, selector, expectedTexts, expectedSelectedIndex = NULL) {
   # Get all options
   options <- remote$js.querySelector(selector)$options
   
   # Verify we have at least 1 expected text (fail if 0)
   expect_true(length(expectedTexts) > 0L, info = "expectedTexts must contain at least 1 string")
   
   # Verify the number of options matches the number of expected texts
   expect_equal(options$length, length(expectedTexts), 
                info = paste("Expected", length(expectedTexts), "options but found", options$length))
   
   # Verify option texts contain expected content
   optionTexts <- character(length(expectedTexts))
   for (i in seq_len(length(expectedTexts))) {
      optionTexts[i] <- options[[i - 1L]]$text
   }
   
   # Compare each option text against the corresponding expected text
   for (i in seq_along(expectedTexts)) {
      expect_true(grepl(expectedTexts[i], optionTexts[i]), 
                  info = paste("Position", i, ": expected", expectedTexts[i], "but got", optionTexts[i]))
   }
   
   # If expectedSelectedIndex is provided, verify the selected option
   if (!is.null(expectedSelectedIndex)) {
      expect_true(expectedSelectedIndex >= 1L && expectedSelectedIndex <= length(expectedTexts), 
                  info = paste("expectedSelectedIndex must be between 1 and", length(expectedTexts)))
      
      # Get the selected option (0-based index in JavaScript)
      selectedOption <- options[[expectedSelectedIndex - 1L]]
      expect_true(selectedOption$selected, 
                  info = paste("Option at index", expectedSelectedIndex, "is not selected"))
   }
}

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
   expectedSidebarTabs <- c("Sidebar on Right")
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
   .rs.verifyQuadrantDropdownOptions(remote, PANE_LAYOUT_SIDEBAR_SELECT, expectedTexts, 2)

   # Close dialog
   remote$keyboard.insertText("<Escape>")
})

.rs.test("Quadrant swapping works correctly", {
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
                    "Files", "Plots", "Packages", "Help", "Viewer", "Presentations", "Chat")
   
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
   expect_false(tabStates["Chat"], info = "Chat should be unchecked in TabSet1")

   # Close dialog
   remote$keyboard.insertText("<Escape>")
})

.rs.test("TabSet2 displays correct default checked tabs", {
   .rs.openPaneLayoutOptions(remote)

   allTabNames <- c("Environment", "History", "Connections", "Build", "VCS", "Tutorial",
                    "Files", "Plots", "Packages", "Help", "Viewer", "Presentations", "Chat")
   
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
   expect_false(tabStates["Chat"], info = "Chat should be unchecked in TabSet2")

   # Close dialog
   remote$keyboard.insertText("<Escape>")
})

.rs.test("Sidebar displays correct default checked tabs", {
   .rs.openPaneLayoutOptions(remote)

   allTabNames <- c("Environment", "History", "Connections", "Build", "VCS", "Tutorial",
                    "Files", "Plots", "Packages", "Help", "Viewer", "Presentations", "Chat")

   tabStates <- .rs.getTabCheckedState(remote, PANE_LAYOUT_SIDEBAR, allTabNames)

   # Default Sidebar should have these tabs checked
   expect_true(tabStates["Chat"], info = "Chat should be checked in Sidebar")

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
   .rs.openPaneLayoutOptions(remote)

   # Chat is checked in Sidebar by default, not in TabSet1 or TabSet2
   expect_true(.rs.isTabChecked(remote, PANE_LAYOUT_SIDEBAR, "Chat"))
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_TOP, "Chat"))
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_BOTTOM, "Chat"))

   # Check Chat in TabSet1
   expect_true(.rs.toggleTab(remote, PANE_LAYOUT_RIGHT_TOP, "Chat"))

   # Verify Chat is now checked in TabSet1 and unchecked in Sidebar and TabSet2
   expect_true(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_TOP, "Chat"))
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_SIDEBAR, "Chat"))
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_BOTTOM, "Chat"))

   # Move it to TabSet2
   expect_true(.rs.toggleTab(remote, PANE_LAYOUT_RIGHT_BOTTOM, "Chat"))

   # Verify Chat is now checked in TabSet2 and unchecked in Sidebar and TabSet1
   expect_true(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_BOTTOM, "Chat"))
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_SIDEBAR, "Chat"))
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_TOP, "Chat"))

   # Move it back to Sidebar
   expect_true(.rs.toggleTab(remote, PANE_LAYOUT_SIDEBAR, "Chat"))

   # Verify it's back to original state
   expect_true(.rs.isTabChecked(remote, PANE_LAYOUT_SIDEBAR, "Chat"))
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_TOP, "Chat"))
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_BOTTOM, "Chat"))

   # Close dialog
   remote$keyboard.insertText("<Escape>")
})

.rs.test("Clicking checked tab unchecks it in both TabSets (hiding the tab)", {
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
