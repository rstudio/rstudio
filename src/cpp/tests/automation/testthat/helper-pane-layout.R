# Shared helper functions for pane layout testing
# These functions are used across multiple test files for interacting with
# the Pane Layout options dialog and managing pane configurations.

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
