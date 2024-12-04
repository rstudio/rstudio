#
# SessionAutomationTools.R
#
# Copyright (C) 2024 by Posit Software, PBC
#
# Unless you have received this program directly from Posit Software pursuant
# to the terms of a commercial license agreement with Posit Software, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#


#' Execute an RStudio command
#' 
#' Execute a built-in command by name. Available commands can be found in
#' src/gwt/src/org/rstudio/studio/client/workbench/commands/Commands.cmd.xml.
#' 
#' @param command The command to execute
#' @return None
.rs.automation.addRemoteFunction("commandExecute", function(command)
{
   jsCode <- deparse(substitute(
      window.rstudioCallbacks.commandExecute(command),
      list(command = command)
   ))
   
   self$jsExec(jsCode)
})

#' Is an element marked with aria-hidden="true"
#' 
#' Checks if an element is marked with aria-hidden="true".
#' 
#' @param jsObject The JavaScript object to check.
#' @return TRUE if aria-hidden="true" is present, FALSE otherwise
#'
.rs.addFunction("automation.tools.isAriaHidden", function(jsObject)
{
   isAriaHidden <- .rs.tryCatch(jsObject$ariaHidden)
   # no aria-hidden attribute is equivalent to FALSE
   ifelse(inherits(isAriaHidden, "error"), FALSE, as.logical(isAriaHidden))
})

#' Create a temporary folder
#' 
#' Creates a temporary folder with the prefix "rstudio.automation" using the
#' RStudio console.
#' 
#' @return The path to the created folder.
#'
.rs.automation.addRemoteFunction("createTempFolder", function()
{
   path <- tempfile("rstudio.automation.", tmpdir = dirname(tempdir()))
   self$consoleExecute(sprintf("dir.create('%s', recursive = TRUE, showWarnings = FALSE)", path))
   path
})

#' Delete a folder
#' 
#' Recursively deletes the specified folder using the RStudio console.
#' 
#' @param folder Folder to delete
#' @return None
#'
.rs.automation.addRemoteFunction("deleteFolder", function(folder)
{
   self$consoleExecute(sprintf("unlink('%s', recursive = TRUE)", folder))
})

#' Get checked state of an element
#' 
#' Determine if an element has the "checked" attribute.
#' 
#' @param nodeId The nodeId of the element to check
#' @return TRUE if element is checked, FALSE if not checked
#'
.rs.automation.addRemoteFunction("getCheckboxStateByNodeId", function(nodeId)
{
   response <- self$client$DOM.getAttributes(nodeId)
   attributes <- response$attributes
   checkedIndex <- which(attributes == "checked")
   isChecked <- length(checkedIndex) > 0
})

#' Set a checkbox to "checked" state by clicking it
#' 
#' Sets a checkbox to the checked state no matter which state it is currently in.
#' 
#' @param selector The css selector of the checkbox element
#' @return None
#' 
.rs.automation.addRemoteFunction("ensureChecked", function(selector)
{
   nodeId <- self$waitForElement(selector)
   if (!self$getCheckboxStateByNodeId(nodeId))
   {
      self$domClickElementByNodeId(nodeId)
   }
})

#' Set a checkbox to "unchecked" state by clicking it
#' 
#' Sets a checkbox to the unchecked state no matter which state it is currently in.
#' 
#' @param selector The css selector of the checkbox element
#' @return None
#' 
.rs.automation.addRemoteFunction("ensureUnchecked", function(selector)
{
   nodeId <- self$waitForElement(selector)
   if (self$getCheckboxStateByNodeId(nodeId))
   {
      self$domClickElementByNodeId(nodeId)
   }
})

#' Click an element
#' 
#' Clicks an element once it exists and is visible
#' 
#' @param selector The css selector of the element to click
#' @return None
#' 
.rs.automation.addRemoteFunction("clickElement", function(selector)
{
   self$waitForVisibleElement(selector)
   self$domClickElement(selector)
})

#' Focus an element and enter text
#' 
#' Focuses the indicated element and enters the provided text.
#' 
#' @param selector The css selector of the element
#' @param ... The text to enter
#' @return None
#' 
.rs.automation.addRemoteFunction("enterText", function(selector, ...)
{
   self$waitForElement(selector)
   self$jsObjectViaSelector(selector)$focus()
   self$keyboardExecute(...)
})

#' Wait for an element to exist in the DOM
#' 
#' Wait for an element to exist and to optionally match a custom predicate.
#' 
#' @param selector The css selector for the element
#' @param predicate The optional predicate function to apply
#' @return The nodeId of the element
#' 
.rs.automation.addRemoteFunction("waitForElement", function(selector,
                                                            predicate = NULL)
{
   # Query for the requested node.
   document <- self$client$DOM.getDocument(depth = 0L)
   
   # Wait until we have a node ID.
   nodeId <- 0L
   .rs.waitUntil(selector, function()
   {
      response <- self$client$DOM.querySelector(document$root$nodeId, selector)
      nodeId <<- response$nodeId
      return(nodeId != 0)
   })
   
   # Wait until the predicate is true.
   if (!is.null(predicate))
   {
      .rs.waitUntil(selector, function()
      {
         predicate(nodeId)
      })
   }
   
   # Return the resolved node id.
   nodeId
})

#' Wait for an element to be visible
#' 
#' Wait for an element to be visible. Visibility is determined via the box model.
#' 
#' @param selector The css selector for the element
#' @return The nodeId of the element
#' 
.rs.automation.addRemoteFunction("waitForVisibleElement", function(selector)
{
   nodeId <- self$waitForElement(selector)

   # Wait until it is visible
   .rs.waitUntil(sprintf("Waiting for nodeId %d to be visible", nodeId), function()
   {
      boxModel <- self$client$DOM.getBoxModel(nodeId = nodeId)
      !is.null(boxModel)
   })
   
   # Return the resolved node id.
   nodeId
})

#' Skip test if R package not installed
#' 
#' Skip the current test if the indicated R package is not installed. Uses the RStudio
#' console.
#' 
#' @param package The name of the required R package
#' @return None
#' 
.rs.automation.addRemoteFunction("skipIfNotInstalled", function(package)
{
   self$consoleExecuteExpr(find.package(!!package, quiet = TRUE))
   output <- self$consoleOutput()
   isInstalled <- tail(output, n = 1L) != "character(0)"
   testthat::skip_if_not(
      condition = isInstalled,
      message   = sprintf("package '%s' is not installed", package)
   )
})

#' Trigger a keyboard shortcut
#' 
#' Mimic the user typing a keyboard shortcut. Supports combinations such as "Ctrl + L".
#' Recognized modifier keys are "Alt", "Ctrl", "Meta", "Cmd", "Command", and "Shift".
#' 
#' @param shortcut Keyboard shortcut to execute
#' @return None
#' 
.rs.automation.addRemoteFunction("shortcutExecute", function(shortcut)
{
   parts <- tolower(strsplit(shortcut, "\\s*\\+\\s*", perl = TRUE)[[1L]])
   
   modifiers <- 0L
   if ("alt" %in% parts)
      modifiers <- bitwOr(modifiers, 1L)
   if ("ctrl" %in% parts)
      modifiers <- bitwOr(modifiers, 2L)
   if ("meta" %in% parts)
      modifiers <- bitwOr(modifiers, 4L)
   if ("shift" %in% parts)
      modifiers <- bitwOr(modifiers, 8L)
   
   # 'cmd' means 'meta' on macOS, 'ctrl' otherwise
   if ("cmd" %in% parts || "command" %in% parts)
   {
      modifier <- ifelse(.rs.platform.isMacos, 4L, 2L)
      modifiers <- bitwOr(modifiers, modifier)
   }
   
   key <- tail(parts, n = 1L)
   code <- .rs.automationConstants.keyToKeyCodeMap[[key]]
   if (is.null(code))
      stop(sprintf("couldn't convert key '%s' to key code", key))
   
   self$client$Input.dispatchKeyEvent(
      type                  = "keyDown",
      modifiers             = modifiers,
      windowsVirtualKeyCode = code
   )
   
   self$client$Input.dispatchKeyEvent(
      type                  = "keyUp",
      modifiers             = modifiers,
      windowsVirtualKeyCode = code
   )
   
})

#' Return the completions shown for given text
#' 
#' Types supplied text, triggers completions with Tab key, and returns the completions.
#' 
#' @param text The text to type before triggering the completions
#' @returns The completions
#' 
.rs.automation.addRemoteFunction("completionsRequest", function(text = "")
{
   # Generate the autocomplete pop-up.
   self$keyboardExecute(text, "<Tab>")
   
   # Get the completion list from the pop-up
   completionListEl <- self$jsObjectViaSelector("#rstudio_popup_completions")
   completionText <- completionListEl$innerText
   
   # Dismiss the popup.
   self$keyboardExecute("<Escape>")
   
   # Remove any inserted code.
   for (i in seq_len(nchar(text)))
   {
      self$keyboardExecute("<Backspace>")
   }
   
   # Extract just the completion items (remove package annotations)
   parts <- strsplit(completionText, "\n{2,}")[[1]]
   parts <- gsub("\\n.*", "", parts)
   
   # Return those parts
   parts
})

#' Create and open a document
#' 
#' Create a temporary document using supplied information and open it in source pane. Uses
#' the RStudio console.
#' 
#' @param ext File extension
#' @param contents Text to write to the document
#' @returns None
.rs.automation.addRemoteFunction("documentOpen", function(ext, contents)
{
   # Write document contents to file.
   documentPath <- tempfile("document-", fileext = ext)
   documentPath <- chartr("\\", "/", documentPath)
   writeLines(contents, con = documentPath)
   
   # Open that document in the attached editor.
   code <- sprintf(".rs.api.documentOpen(\"%s\")", documentPath)
   self$consoleExecute(code)
   
   self$waitForElement("#rstudio_source_text_editor")
})

#' Create and open a document
#' 
#' Create a temporary document using supplied information, open it in source pane, and invoke
#' a callback, supplying the editor instance containing the document. Uses the RStudio console.
#' 
#' @param ext File extension
#' @param contents Text to write to the document
#' @param callback Callback to invoke with the editor instance containing the document
#' @returns None
#' 
.rs.automation.addRemoteFunction("documentExecute", function(ext, contents, callback)
{
   # Write document contents to file.
   documentPath <- tempfile("document-", fileext = ext)
   documentPath <- chartr("\\", "/", documentPath)
   writeLines(contents, con = documentPath)
   
   # Open that document in the attached editor.
   code <- sprintf(".rs.api.documentOpen(\"%s\")", documentPath)
   self$consoleExecute(code)
   
   # Set an exit handler so we close and clean up the console history after.
   on.exit({
      self$documentClose()
      self$shortcutExecute("Ctrl + L")
   }, add = TRUE)
   
   # Wait until the element is focused.
   .rs.waitUntil("source editor is focused", function()
   {
      className <- self$jsExec("document.activeElement.className")
      length(className) && grepl("ace_text-input", className)
   })
   
   # Get a reference to the editor in that instance.
   editor <- self$editorGetInstance()
   
   # Wait a small bit, so Ace can tokenize the document.
   Sys.sleep(0.2)
   
   # Invoke callback with the editor instance.
   callback(editor)
})

#' Close the current document
#' 
#' Closes the currently active document, using the console.
#' 
#' @returns None
#' 
.rs.automation.addRemoteFunction("documentClose", function()
{
   self$consoleExecute("invisible(.rs.api.documentClose())")
})
