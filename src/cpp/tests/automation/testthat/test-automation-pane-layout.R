library(testthat)

# ids of the 4 quadrants in the Pane Layout options panel
PANE_LAYOUT_LEFT_TOP <- "#rstudio_pane_layout_left_top"
PANE_LAYOUT_LEFT_BOTTOM <- "#rstudio_pane_layout_left_bottom"
PANE_LAYOUT_RIGHT_TOP <- "#rstudio_pane_layout_right_top"
PANE_LAYOUT_RIGHT_BOTTOM <- "#rstudio_pane_layout_right_bottom"

# ids of the 4 dropdowns in the Pane Layout options panel
PANE_LAYOUT_LEFT_TOP_SELECT <- "#rstudio_pane_layout_left_top_select"
PANE_LAYOUT_LEFT_BOTTOM_SELECT <- "#rstudio_pane_layout_left_bottom_select"
PANE_LAYOUT_RIGHT_TOP_SELECT <- "#rstudio_pane_layout_right_top_select"
PANE_LAYOUT_RIGHT_BOTTOM_SELECT <- "#rstudio_pane_layout_right_bottom_select"

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

# Helper function to toggle a checkbox
.rs.toggleTab <- function(remote, quadrantClass, tabName) {
   labels <- remote$js.querySelectorAll(paste0(quadrantClass, " label"))

   for (i in seq_len(labels$length)) {
      label <- labels[[i - 1L]]
      if (grepl(tabName, label$innerText, fixed = TRUE)) {
         # Click the label to toggle the checkbox
         remote$dom.clickElement(nodeId = label$nodeId)
         Sys.sleep(0.5) # Allow time for state change
         return(TRUE)
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
   expect_equal(options$length, 4L)
   
   # Verify we have 4 expected texts
   expect_equal(length(expectedTexts), 4L, info = "expectedTexts must contain exactly 4 strings")
   
   # Verify option texts contain expected content
   optionTexts <- character(4)
   for (i in seq_len(4)) {
      optionTexts[i] <- options[[i - 1L]]$text
   }
   
   # Compare each option text against the corresponding expected text
   expect_true(grepl(expectedTexts[1], optionTexts[1]), 
               info = paste("Position 1: expected", expectedTexts[1], "but got", optionTexts[1]))
   expect_true(grepl(expectedTexts[2], optionTexts[2]), 
               info = paste("Position 2: expected", expectedTexts[2], "but got", optionTexts[2]))
   expect_true(grepl(expectedTexts[3], optionTexts[3]), 
               info = paste("Position 3: expected", expectedTexts[3], "but got", optionTexts[3]))
   expect_true(grepl(expectedTexts[4], optionTexts[4]), 
               info = paste("Position 4: expected", expectedTexts[4], "but got", optionTexts[4]))
   
   # If expectedSelectedIndex is provided, verify the selected option
   if (!is.null(expectedSelectedIndex)) {
      expect_true(expectedSelectedIndex >= 1L && expectedSelectedIndex <= 4L, 
                  info = "expectedSelectedIndex must be between 1 and 4")
      
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

   # Close dialog
   remote$keyboard.insertText("<Escape>")
})

.rs.test("Quadrant dropdown shows all four options with correct checkmarks", {
   .rs.openPaneLayoutOptions(remote)

   expectedTexts <- c("Source", "Console", "Environment", "Files")

   # Test all four quadrant dropdowns
   .rs.verifyQuadrantDropdownOptions(remote, PANE_LAYOUT_LEFT_TOP_SELECT, expectedTexts, 1)
   .rs.verifyQuadrantDropdownOptions(remote, PANE_LAYOUT_LEFT_BOTTOM_SELECT, expectedTexts, 2)
   .rs.verifyQuadrantDropdownOptions(remote, PANE_LAYOUT_RIGHT_TOP_SELECT, expectedTexts, 3)
   .rs.verifyQuadrantDropdownOptions(remote, PANE_LAYOUT_RIGHT_BOTTOM_SELECT, expectedTexts, 4)

   # Close dialog
   remote$keyboard.insertText("<Escape>")
})

.rs.test("Quadrant swapping works correctly", {
   .rs.openPaneLayoutOptions(remote)

   sourceInitial <- .rs.getQuadrantDropdownText(remote, PANE_LAYOUT_LEFT_TOP)
   consoleInitial <- .rs.getQuadrantDropdownText(remote, PANE_LAYOUT_LEFT_BOTTOM)
   upperRightInitial <- .rs.getQuadrantDropdownText(remote, PANE_LAYOUT_RIGHT_TOP)
   lowerRightInitial <- .rs.getQuadrantDropdownText(remote, PANE_LAYOUT_RIGHT_BOTTOM)

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

   expect_equal(upperRightAfter, sourceInitial)
   expect_equal(lowerRightAfter, consoleInitial)
   expect_equal(upperLeftAfter, upperRightInitial)
   expect_equal(lowerLeftAfter, lowerRightInitial)

   # Close dialog
   remote$keyboard.insertText("<Escape>")
})

.rs.test("TabSet1 displays correct default checked tabs", {
   .rs.openPaneLayoutOptions(remote)

   # Default TabSet1 should have these tabs checked:
   # Environment, History, Connections, Build, VCS, Tutorial
   expect_true(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_TOP, "Environment"))
   expect_true(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_TOP, "History"))
   expect_true(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_TOP, "Connections"))
   expect_true(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_TOP, "Build"))
   expect_true(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_TOP, "VCS"))
   expect_true(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_TOP, "Tutorial"))

   # These should be unchecked in TabSet1
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_TOP, "Files"))
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_TOP, "Plots"))
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_TOP, "Packages"))
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_TOP, "Help"))
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_TOP, "Viewer"))
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_TOP, "Presentations"))

   # Close dialog
   remote$keyboard.insertText("<Escape>")
})

.rs.test("TabSet2 displays correct default checked tabs", {
   .rs.openPaneLayoutOptions(remote)

   # Default TabSet2 should have these tabs checked:
   # Files, Plots, Packages, Help, Viewer, Presentations
   expect_true(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_BOTTOM, "Files"))
   expect_true(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_BOTTOM, "Plots"))
   expect_true(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_BOTTOM, "Packages"))
   expect_true(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_BOTTOM, "Help"))
   expect_true(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_BOTTOM, "Viewer"))
   expect_true(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_BOTTOM, "Presentations"))

   # These should be unchecked in TabSet2
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_BOTTOM, "Environment"))
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_BOTTOM, "History"))
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_BOTTOM, "Connections"))
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_BOTTOM, "Build"))
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_BOTTOM, "VCS"))
   expect_false(.rs.isTabChecked(remote, PANE_LAYOUT_RIGHT_BOTTOM, "Tutorial"))

   # Close dialog
   remote$keyboard.insertText("<Escape>")
})

# .rs.test("Clicking unchecked tab in one TabSet unchecks it in the other", {
#    .rs.openPaneLayoutOptions(remote)

#    # Files is checked in TabSet2 by default, not in TabSet1
#    expect_false(.rs.isTabChecked(remote, ".rstudio-pane-layout-tabset1", "Files"))
#    expect_true(.rs.isTabChecked(remote, ".rstudio-pane-layout-tabset2", "Files"))

#    # Check Files in TabSet1
#    .rs.toggleTab(remote, ".rstudio-pane-layout-tabset1", "Files")

#    # Verify Files is now checked in TabSet1 and unchecked in TabSet2
#    expect_true(.rs.isTabChecked(remote, ".rstudio-pane-layout-tabset1", "Files"))
#    expect_false(.rs.isTabChecked(remote, ".rstudio-pane-layout-tabset2", "Files"))

#    # Move it back to TabSet2
#    .rs.toggleTab(remote, ".rstudio-pane-layout-tabset2", "Files")

#    # Verify it's back to original state
#    expect_false(.rs.isTabChecked(remote, ".rstudio-pane-layout-tabset1", "Files"))
#    expect_true(.rs.isTabChecked(remote, ".rstudio-pane-layout-tabset2", "Files"))

#    # Close dialog
#    remote$keyboard.insertText("<Escape>")
# })

# .rs.test("Clicking checked tab unchecks it in both TabSets (hiding the tab)", {
#    .rs.openPaneLayoutOptions(remote)

#    # Environment is checked in TabSet1 by default
#    expect_true(.rs.isTabChecked(remote, ".rstudio-pane-layout-tabset1", "Environment"))
#    expect_false(.rs.isTabChecked(remote, ".rstudio-pane-layout-tabset2", "Environment"))

#    # Uncheck Environment in TabSet1
#    .rs.toggleTab(remote, ".rstudio-pane-layout-tabset1", "Environment")

#    # Verify Environment is now unchecked in both TabSets (hidden)
#    expect_false(.rs.isTabChecked(remote, ".rstudio-pane-layout-tabset1", "Environment"))
#    expect_false(.rs.isTabChecked(remote, ".rstudio-pane-layout-tabset2", "Environment"))

#    # Check it again in TabSet1 to restore state
#    .rs.toggleTab(remote, ".rstudio-pane-layout-tabset1", "Environment")

#    # Verify it's checked again
#    expect_true(.rs.isTabChecked(remote, ".rstudio-pane-layout-tabset1", "Environment"))
#    expect_false(.rs.isTabChecked(remote, ".rstudio-pane-layout-tabset2", "Environment"))

#    # Close dialog
#    remote$keyboard.insertText("<Escape>")
# })

# .rs.test("Tab can only be checked in one TabSet at a time", {
#    .rs.openPaneLayoutOptions(remote)

#    # History is in TabSet1 by default
#    expect_true(.rs.isTabChecked(remote, ".rstudio-pane-layout-tabset1", "History"))
#    expect_false(.rs.isTabChecked(remote, ".rstudio-pane-layout-tabset2", "History"))

#    # Try to check History in TabSet2
#    .rs.toggleTab(remote, ".rstudio-pane-layout-tabset2", "History")

#    # Verify History moved from TabSet1 to TabSet2
#    expect_false(.rs.isTabChecked(remote, ".rstudio-pane-layout-tabset1", "History"))
#    expect_true(.rs.isTabChecked(remote, ".rstudio-pane-layout-tabset2", "History"))

#    # Move it back to TabSet1
#    .rs.toggleTab(remote, ".rstudio-pane-layout-tabset1", "History")

#    # Verify it's back in TabSet1
#    expect_true(.rs.isTabChecked(remote, ".rstudio-pane-layout-tabset1", "History"))
#    expect_false(.rs.isTabChecked(remote, ".rstudio-pane-layout-tabset2", "History"))

#    # Close dialog
#    remote$keyboard.insertText("<Escape>")
# })

# .rs.test("All tabs can be moved to one TabSet leaving the other empty", {
#    .rs.openPaneLayoutOptions(remote)

#    # Move all TabSet2 tabs to TabSet1
#    tabsToMove <- c("Files", "Plots", "Packages", "Help", "Viewer", "Presentations")

#    for (tab in tabsToMove) {
#       if (.rs.isTabChecked(remote, ".rstudio-pane-layout-tabset2", tab)) {
#          .rs.toggleTab(remote, ".rstudio-pane-layout-tabset1", tab)
#       }
#    }

#    # Verify all tabs are now in TabSet1
#    allTabs <- c("Environment", "History", "Files", "Plots", "Connections",
#                 "Packages", "Help", "Build", "VCS", "Tutorial", "Viewer",
#                 "Presentations")

#    for (tab in allTabs) {
#       if (tab %in% c("Environment", "History", "Files", "Plots", "Connections",
#                      "Packages", "Help", "Build", "VCS", "Tutorial", "Viewer",
#                      "Presentations")) {
#          expect_true(.rs.isTabChecked(remote, ".rstudio-pane-layout-tabset1", tab))
#          expect_false(.rs.isTabChecked(remote, ".rstudio-pane-layout-tabset2", tab))
#       }
#    }

#    # Move tabs back to restore default state
#    for (tab in tabsToMove) {
#       .rs.toggleTab(remote, ".rstudio-pane-layout-tabset2", tab)
#    }

#    # Close dialog
#    remote$keyboard.insertText("<Escape>")
# })

# .rs.test("TabSet quadrants can be swapped while maintaining their tab configurations", {
#    .rs.openPaneLayoutOptions(remote)

#    # Get initial TabSet positions
#    tabset1Initial <- .rs.getQuadrantDropdownText(remote, ".rstudio-pane-layout-tabset1")
#    tabset2Initial <- .rs.getQuadrantDropdownText(remote, ".rstudio-pane-layout-tabset2")

#    # Remember which tabs are checked in each
#    envInTabSet1 <- .rs.isTabChecked(remote, ".rstudio-pane-layout-tabset1", "Environment")
#    filesInTabSet2 <- .rs.isTabChecked(remote, ".rstudio-pane-layout-tabset2", "Files")

#    expect_true(envInTabSet1)
#    expect_true(filesInTabSet2)

#    # Swap TabSet1 and TabSet2 positions via dropdown
#    # This swaps the quadrant positions but should maintain tab assignments
#    .rs.selectDropdownOption(remote, ".rstudio-pane-layout-tabset1", tabset2Initial)
#    Sys.sleep(1)

#    # Verify the swap occurred in dropdown
#    tabset1After <- .rs.getQuadrantDropdownText(remote, ".rstudio-pane-layout-tabset1")
#    tabset2After <- .rs.getQuadrantDropdownText(remote, ".rstudio-pane-layout-tabset2")

#    expect_equal(tabset1After, tabset2Initial)
#    expect_equal(tabset2After, tabset1Initial)

#    # Tab configurations should remain with their TabSets
#    expect_true(.rs.isTabChecked(remote, ".rstudio-pane-layout-tabset1", "Environment"))
#    expect_true(.rs.isTabChecked(remote, ".rstudio-pane-layout-tabset2", "Files"))

#    # Swap back to restore
#    .rs.selectDropdownOption(remote, ".rstudio-pane-layout-tabset1", tabset1Initial)

#    # Close dialog
#    remote$keyboard.insertText("<Escape>")
# })
