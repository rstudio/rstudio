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

# !diagnostics suppress=client,self

#' Execute an RStudio command
#' 
#' Execute a built-in command by name. Available commands can be found in
#' src/gwt/src/org/rstudio/studio/client/workbench/commands/Commands.cmd.xml.
#' 
#' @param command The command to execute
#' @return None
.rs.automation.addRemoteFunction("commands.execute", function(command)
{
   jsCode <- deparse(substitute(
      window.rstudioCallbacks.commandExecute(command),
      list(command = command)
   ))
   
   self$js.eval(jsCode)
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

#' Create a directory
#' 
#' Creates a new directory. Roughly equivalent to calling `mkdir -p`.
#' When `path` is `NULL`, a temporary directory is created and used.
#' 
#' @return The path to the created folder.
#'
.rs.automation.addRemoteFunction("files.createDirectory", function(path = NULL)
{
   path <- .rs.nullCoalesce(
      path,
      tempfile("rstudio.automation.", tmpdir = dirname(tempdir()))
   )
   
   self$console.executeExpr({
      dir.create(!!path, recursive = TRUE, showWarnings = FALSE)
   })
   
   path
   
})

#' Remove a file or folder
#' 
#' Remove a file or folder. Use `recursive = TRUE` to delete directories
#' which might not already be empty.
#' 
#' @param path The path to a file to be removed.
#' @return None
#'
.rs.automation.addRemoteFunction("files.remove", function(path, recursive = TRUE)
{
   self$console.executeExpr({
      unlink(!!path, recursive = !!recursive)
   })
})

#' Get checked state of an element
#' 
#' Determine if an element has the "checked" attribute.
#' 
#' @param nodeId The nodeId of the element to check
#' @return TRUE if element is checked, FALSE if not checked
#'
.rs.automation.addRemoteFunction("dom.isChecked", function(nodeId)
{
   response <- self$client$DOM.getAttributes(nodeId)
   attributes <- response$attributes
   checkedIndex <- which(attributes == "checked")
   length(checkedIndex) > 0
})

#' Set a checkbox to "checked" state by clicking it
#' 
#' Sets a checkbox to the checked state no matter which state it is currently in.
#' 
#' @param selector The css selector of the checkbox element
#' @return None
#' 
.rs.automation.addRemoteFunction("dom.setChecked", function(selector, checked = TRUE)
{
   nodeId <- self$dom.waitForElement(selector)
   if (checked != self$dom.isChecked(nodeId))
      self$dom.clickElement(nodeId = nodeId)
})

#' Focus an element and enter text
#' 
#' Focuses the indicated element and enters the provided text.
#' 
#' @param selector The css selector of the element
#' @param ... The text to enter
#' @return None
#' 
.rs.automation.addRemoteFunction("dom.insertText", function(selector, ...)
{
   self$dom.waitForElement(selector)
   self$js.querySelector(selector)$focus()
   self$keyboard.insertText(...)
})

#' Wait for an element to exist in the DOM
#' 
#' Wait for an element to exist and to optionally match a custom predicate.
#' 
#' @param selector The css selector for the element
#' @param predicate The optional predicate function to apply
#' @param waitUntilVisible Boolean; should we also wait until the requested element is visible?
#' @return The nodeId of the element
#' 
.rs.automation.addRemoteFunction("dom.waitForElement", function(selector,
                                                                predicate = NULL,
                                                                waitUntilVisible = TRUE)
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
   
   # Wait until the elemnt is visible if requested.
   if (waitUntilVisible)
   {
      message <- sprintf("Waiting for node %d to be visible", nodeId)
      .rs.waitUntil(message, function()
      {
         boxModel <- self$client$DOM.getBoxModel(nodeId = nodeId)
         !is.null(boxModel)
      })
   }
   
   # Return the resolved node id.
   nodeId
})

#' Check if a package is installed
#' 
#' @param package The name of an \R package.
#' @returns `TRUE` if the package is installed; `FALSE` otherwise.
.rs.automation.addRemoteFunction("package.isInstalled", function(package)
{
   self$console.executeExpr(find.package(!!package, quiet = TRUE))
   output <- self$console.getOutput()
   tail(output, n = 1L) != "character(0)"
})

#' Trigger a keyboard shortcut
#' 
#' Mimic the user typing a keyboard shortcut. Supports combinations such as "Ctrl + L".
#' Recognized modifier keys are "Alt", "Ctrl", "Meta", "Cmd", "Command", and "Shift".
#' 
#' @param shortcut Keyboard shortcut to execute
#' @return None
#' 
.rs.automation.addRemoteFunction("keyboard.executeShortcut", function(shortcut)
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
   
   # TODO: These effectively fill out the same values on the associated
   # JavaScript KeyEvent, so ideally we'd populate all the parameters here.
   # Right now, we're just doing the minimal necessary bits.
   self$client$Input.dispatchKeyEvent(
      type                  = "keyDown",
      key                   = key,
      modifiers             = modifiers,
      windowsVirtualKeyCode = code
   )
   
   self$client$Input.dispatchKeyEvent(
      type                  = "keyUp",
      key                   = key,
      modifiers             = modifiers,
      windowsVirtualKeyCode = code
   )
   
})

.rs.automation.addRemoteFunction("keyboard.insertText", function(...)
{
   reShortcut <- "^\\<(.*)\\>$"
   for (input in list(...))
   {
      if (grepl(reShortcut, input, perl = TRUE))
      {
         shortcut <- sub(reShortcut, "\\1", input, perl = TRUE)
         self$keyboard.executeShortcut(shortcut)
      }
      else if (nzchar(input))
      {
         self$client$Input.insertText(input)
      }
   }
})

.rs.automation.addRemoteFunction("keyboard.sendKeys", function(...)
{
   reShortcut <- "^\\<(.*)\\>$"
   for (input in list(...))
   {
      if (grepl(reShortcut, input, perl = TRUE))
      {
         shortcut <- sub(reShortcut, "\\1", input, perl = TRUE)
         self$keyboard.executeShortcut(shortcut)
      }
      else if (nzchar(input))
      {
         self$keyboard.executeShortcut(input)
      }
   }
})

#' Return the completions shown for given text
#' 
#' Types supplied text, triggers completions with Tab key, and returns the completions.
#' 
#' @param text The text to type before triggering the completions
#' @returns The completions
#' 
.rs.automation.addRemoteFunction("completions.request", function(text = "")
{
   # Generate the autocomplete pop-up.
   self$keyboard.insertText(text, "<Tab>")
   
   # Get the completion list from the pop-up
   completionListEl <- self$js.querySelector("#rstudio_popup_completions")
   completionText <- completionListEl$innerText
   
   # Dismiss the popup.
   self$keyboard.insertText("<Escape>")
   
   # Remove any inserted code.
   for (i in seq_len(nchar(text)))
   {
      self$keyboard.insertText("<Backspace>")
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
.rs.automation.addRemoteFunction("editor.openWithContents", function(ext, contents)
{
   # Write document contents to file.
   documentPath <- tempfile("document-", fileext = ext)
   documentPath <- chartr("\\", "/", documentPath)
   writeLines(contents, con = documentPath)
   
   # Open that document in the attached editor.
   self$console.executeExpr({
      .rs.api.documentOpen(!!documentPath)
   })
   
   self$dom.waitForElement("#rstudio_source_text_editor")
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
.rs.automation.addRemoteFunction("editor.executeWithContents", function(ext, contents, callback)
{
   # Write document contents to file.
   documentPath <- tempfile("document-", fileext = ext)
   documentPath <- chartr("\\", "/", documentPath)
   writeLines(contents, con = documentPath)
   
   # Open that document in the attached editor.
   code <- sprintf(".rs.api.documentOpen(\"%s\")", documentPath)
   self$console.execute(code)
   
   # Set an exit handler so we close and clean up the console history after.
   on.exit({
      self$editor.closeDocument()
      self$keyboard.executeShortcut("Ctrl + L")
   }, add = TRUE)
   
   # Wait until the element is focused.
   .rs.waitUntil("source editor is focused", function()
   {
      className <- self$js.eval("document.activeElement.className")
      length(className) && grepl("ace_text-input", className)
   })
   
   # Get a reference to the editor in that instance.
   editor <- self$editor.getInstance()
   
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
.rs.automation.addRemoteFunction("editor.closeDocument", function()
{
   self$console.execute("invisible(.rs.api.documentClose())")
})
