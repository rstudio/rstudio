
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())


# https://github.com/rstudio/rstudio/issues/15072
.rs.test("the debug position is correct in braced expressions", {

   contents <- .rs.heredoc('
      f <- function() {
         1 + 1
         {
            2 + 2
         }
      }
   ')
   
   remote$editor.openWithContents(".R", contents)
   
   # Click to set a breakpoint.
   gutterLayer <- remote$js.querySelector(".ace_gutter-layer")
   gutterCell <- gutterLayer$children[[3]]
   remote$dom.clickElement(objectId = gutterCell, horizontalOffset = -6L)
   
   # Clear the current selection if we have one.
   editor <- remote$editor.getInstance()
   editor$clearSelection()
   
   # Source the file.
   remote$commands.execute("sourceActiveDocument")
   Sys.sleep(1)
   
   # Execute the function.
   remote$console.execute("f()")
   
   # Check that debug highlighting was set on the fourth row.
   gutterCell <- remote$js.querySelector(".ace_executing-line")
   gutterParent <- gutterCell$parentElement
   gutterChild <- gutterParent$children[[3]]
   expect_equal(gutterCell$innerText, gutterChild$innerText)
   
   # Get the screen position of the debug rectangle.
   debugLine <- remote$js.querySelector(".ace_active_debug_line")
   debugRect <- debugLine$getBoundingClientRect()
   
   # Figure out what row that maps to in the editor.
   screenCoords <- editor$session$renderer$pixelToScreenCoordinates(
      debugRect$x + debugRect$width / 2,
      debugRect$y + debugRect$height / 2
   )
   expect_equal(screenCoords$row, 3)
   
   # Exit the debugger.
   remote$keyboard.insertText("<Ctrl + 2>", "c", "<Enter>")
   remote$console.executeExpr(rm(list = "f"))
   remote$keyboard.insertText("<Ctrl + L>")
   
})

# https://github.com/rstudio/rstudio/issues/15201
.rs.test("package functions can be debugged after build and reload", {
   
   # Create an R package project.
   remote$project.create(projectName = "rstudio.automation", type = "package")
   
   # Close any open documents
   remote$console.executeExpr(
      .rs.api.closeAllSourceBuffersWithoutSaving()
   )
   
   # Add a source document.
   remote$console.executeExpr(file.edit("R/example.R"))
   remote$commands.execute("activateSource")
   
   code <- .rs.heredoc('
      example <- function() {
         1 + 1
         2 + 2
         3 + 3
         4 + 4
         5 + 5
      }
   ')
   
   editor <- remote$editor.getInstance()
   editor$insert(code)
   Sys.sleep(0.1)
   
   # Save it, and build the package.
   remote$commands.execute("saveSourceDoc")
   remote$commands.execute("buildAll")
   
   .rs.waitUntil("build has completed", function()
   {
      output <- remote$console.getOutput()
      any(output == "> library(rstudio.automation)")
   }, swallowErrors = TRUE)
   
   remote$console.clear()
   
   # Try adding some breakpoints.
   gutterEls <- remote$js.querySelectorAll(".ace_gutter-cell")
   remote$dom.clickElement(objectId = gutterEls[[3]], horizontalOffset = -4L)
   breakpointEls <- remote$js.querySelectorAll(".ace_breakpoint")
   expect_equal(length(breakpointEls), 1L)
   expect_equal(breakpointEls[[1]]$innerText, "3")
   
   remote$dom.clickElement(objectId = gutterEls[[4]], horizontalOffset = -4L)
   breakpointEls <- remote$js.querySelectorAll(".ace_breakpoint")
   expect_equal(length(breakpointEls), 2L)
   expect_equal(breakpointEls[[2]]$innerText, "4")
   
   # Confirm that the object definition is in sync.
   remote$console.executeExpr({
      .rs.isFunctionInSync("example", "R/example.R", "rstudio.automation")
   })
   
   output <- remote$console.getOutput()
   expect_contains(output, "[1] TRUE")
   remote$console.clear()
   
   # Try running the function, and checking the view.
   remote$console.executeExpr(example())
   
   activeLineEl <- remote$js.querySelector(".ace_executing-line")
   expect_equal(activeLineEl$innerText, "3")
   remote$commands.execute("activateConsole")
   remote$keyboard.insertText("c", "<Enter>")
   
   activeLineEl <- remote$js.querySelector(".ace_executing-line")
   expect_equal(activeLineEl$innerText, "4")
   remote$keyboard.insertText("c", "<Enter>")
   
   # All done testing; close the project.
   remote$editor.closeDocument()
   Sys.sleep(1)
   remote$project.close()
})

# https://github.com/rstudio/rstudio/issues/16490
.rs.test("breakpoints in S7 method definitions for S3 generics", {
   
   skip_on_ci()
   
   remote$console.executeExpr({
      if (!requireNamespace("S7", quietly = TRUE))
         install.packages("S7")
   })
   
   contents <- .rs.heredoc('
      s7mean <- S7::new_class("s7mean")
      S7::method(mean, s7mean) <- function(x, ...) {
         print("This is my S7 method.")
      }
   ')
   
   remote$editor.executeWithContents(".R", contents, function(editor) {
      
      # Click to set a breakpoint.
      gutterLayer <- remote$js.querySelector(".ace_gutter-layer")
      gutterCell <- gutterLayer$children[[2L]]
      remote$dom.clickElement(objectId = gutterCell, horizontalOffset = -6L)
      
      # Source the document to activate breakpoints.
      remote$commands.execute(.rs.appCommands$sourceActiveDocument)
      
      # Try and see if the breakpoint fires.
      remote$console.executeExpr({
         object <- structure(1:10, class = "s7mean")
         mean(object)
      })
      
      # Check that line '3' is highlighted.
      activeLineEl <- remote$js.querySelector(".ace_executing-line")
      expect_equal(activeLineEl$innerText, "3")
      
      # Finish the debug session.
      remote$commands.execute("activateConsole")
      remote$keyboard.insertText("c", "<Enter>")
      
      # Check the console output.
      output <- remote$console.getOutput(1L)
      expect_equal(output, "[1] \"This is my S7 method.\"")

   })
})

# Multi-line input at the Browse[N]> prompt previously triggered continuation
# prompts ("+ ") that called setBrowserActive(false), clearing the captured
# browser environment mid-eval and confusing debugger introspection.
.rs.test("browser state survives multi-line input at the Browse prompt", {

   contents <- .rs.heredoc('
      f <- function() {
         x <- 1
         browser()
         x + 1
      }
   ')

   remote$editor.openWithContents(".R", contents)
   remote$commands.execute("sourceActiveDocument")

   # Wait for the source to complete and the function to be defined.
   .rs.waitUntil("function f is defined", function() {
      tryCatch({
         remote$console.executeExpr(stopifnot(is.function(f)))
         TRUE
      }, error = function(e) FALSE)
   })

   # Run the function -- hits browser().
   remote$console.execute("f()")

   # Verify we're at the browser prompt with a debug highlight.
   .rs.waitUntil("debug highlight appears", function() {
      remote$dom.elementExists(".ace_executing-line")
   })

   # Submit a multi-line expression at Browse[N]>. The {...} block forces R
   # to parse line-by-line, issuing continuation prompts ("+ ") in between.
   # Pre-fix, those continuation prompts called setBrowserActive(false),
   # which cleared the cached browser state mid-eval so the body of the
   # block would observe .rs.isBrowserActive() == FALSE and an empty
   # browser environment.
   multilineCode <- .rs.heredoc('
      {
         status <- .rs.isBrowserActive()
         envIsGlobal <- identical(
            .Call("rs_getBrowserEnv", PACKAGE = "(embedding)"),
            globalenv()
         )
         cat("DBG isBrowserActive:", status,
             "envIsGlobal:", envIsGlobal, "\n")
      }
   ')
   remote$console.execute(multilineCode)

   .rs.waitUntil("multi-line eval finished", function()
   {
      # We might get put into a nested debugger invocation. If this happens,
      # then make sure we step through it.
      output <- remote$console.getOutput(n = 1L)
      if (grepl("^debug at", output))
      {
         remote$keyboard.sendKeys("c", "<Enter>")
         return(FALSE)
      }
      
      any(grepl("^DBG isBrowserActive: ", output))
   })

   output <- remote$console.getOutput()
   hits <- output[grepl("^DBG isBrowserActive: ", output)]
   line <- hits[length(hits)]
   expect_match(line, "isBrowserActive: TRUE")
   expect_match(line, "envIsGlobal: FALSE")

   # Debug highlight should still be visible after the multi-line eval.
   expect_true(remote$dom.elementExists(".ace_executing-line"))

   # Exit the debugger and clean up.
   remote$keyboard.insertText("<Ctrl + 2>", "c", "<Enter>")
   remote$console.executeExpr(rm(list = "f"))
   remote$keyboard.insertText("<Ctrl + L>")
})

# https://github.com/rstudio/rstudio/issues/17481
# Hitting a breakpoint at the top level of a sourced script (where the
# captured browser environment is globalenv()) previously caused
# isBrowseActive() to return FALSE -- onConsolePrompt then took an early
# return without firing kContextDepthChanged, so the debug highlight and
# debugger toolbar never appeared.
.rs.test("debugger UI appears for top-level breakpoints in sourced scripts", {

   contents <- .rs.heredoc('
      x <- 1
      y <- 22
      print(x + y)
   ')

   remote$editor.openWithContents(".R", contents)

   # Place a breakpoint. Note: gutterLayer$children is a JS HTMLCollection
   # accessed with 0-based indexing through the BRAT R wrapper, so [[2L]]
   # is the third gutter cell -- the line containing print(x + y).
   gutterLayer <- remote$js.querySelector(".ace_gutter-layer")
   gutterCell <- gutterLayer$children[[2L]]
   remote$dom.clickElement(objectId = gutterCell, horizontalOffset = -6L)

   editor <- remote$editor.getInstance()
   editor$clearSelection()

   # Source the document -- the breakpoint should fire at the print() line.
   remote$commands.execute("sourceActiveDocument")

   # Wait for the debug highlight to settle. The auto-step past .doTrace
   # (BreakpointManager.onContextDepthChanged) can leave the highlight
   # transiently pointing at a neighbouring line before the post-step
   # events arrive, so poll for the actual line number ("3", matching
   # the print() line) rather than just element existence.
   .rs.waitUntil("debug highlight settles at top-level breakpoint", function() {
      if (!remote$dom.elementExists(".ace_executing-line"))
         return(FALSE)
      activeLineEl <- remote$js.querySelector(".ace_executing-line")
      identical(activeLineEl$innerText, "3")
   })

   activeLineEl <- remote$js.querySelector(".ace_executing-line")
   expect_equal(activeLineEl$innerText, "3")

   # Exit the debugger and confirm the script actually completes (not
   # hung): print(x + y) should produce "[1] 23" once execution resumes.
   remote$keyboard.insertText("<Ctrl + 2>", "c", "<Enter>")
   .rs.waitUntil("script completes after debugger exit", function() {
      output <- remote$console.getOutput()
      any(grepl("^\\[1\\] 23$", output))
   })
   remote$keyboard.insertText("<Ctrl + L>")
})

# https://github.com/rstudio/rstudio/issues/9450
# Clicking a gutter cell that already has a breakpoint should remove both
# the in-memory entry and the visual marker. Reports of "sticky" breakpoints
# in the original issue describe markers that survive the second click.
.rs.test("clicking a top-level breakpoint marker toggles it off", {

   contents <- .rs.heredoc('
      x <- 1
      y <- 2
      z <- x + y
   ')

   remote$editor.executeWithContents(".R", contents, function(editor) {

      # children[[1L]] is the second gutter cell -- 0-based indexing through
      # the BRAT R wrapper, so this targets line 2 ("y <- 2"). Top-level
      # breakpoints land in STATE_ACTIVE immediately, surfacing as the
      # "ace_breakpoint" class with no STATE_PROCESSING / pending flicker.
      # horizontalOffset of -6L targets the breakpoint area in the
      # top-level (non-package) gutter; package projects render a
      # slightly different gutter and use -4L (see the package test below).
      gutterLayer <- remote$js.querySelector(".ace_gutter-layer")
      gutterCell <- gutterLayer$children[[1L]]
      remote$dom.clickElement(objectId = gutterCell, horizontalOffset = -6L)

      .rs.waitUntil("breakpoint marker appears", function() {
         remote$dom.elementExists(".ace_breakpoint")
      })
      breakpointEls <- remote$js.querySelectorAll(".ace_breakpoint")
      expect_equal(length(breakpointEls), 1L)

      # Click the same gutter cell to toggle the breakpoint off.
      gutterLayer <- remote$js.querySelector(".ace_gutter-layer")
      gutterCell <- gutterLayer$children[[1L]]
      remote$dom.clickElement(objectId = gutterCell, horizontalOffset = -6L)

      # Marker (in any of its visual states) should be gone.
      .rs.waitUntil("breakpoint marker is removed", function() {
         !remote$dom.elementExists(".ace_breakpoint") &&
            !remote$dom.elementExists(".ace_pending-breakpoint") &&
            !remote$dom.elementExists(".ace_inactive-breakpoint")
      })
      expect_false(remote$dom.elementExists(".ace_breakpoint"))
      expect_false(remote$dom.elementExists(".ace_pending-breakpoint"))
      expect_false(remote$dom.elementExists(".ace_inactive-breakpoint"))
   })
})

# https://github.com/rstudio/rstudio/issues/9450
# "Clear All Breakpoints" should remove every marker on every line. The
# original issue reports cases where some markers survived the command.
.rs.test("Clear All Breakpoints removes every breakpoint marker", {

   contents <- .rs.heredoc('
      x <- 1
      y <- 2
      z <- x + y
   ')

   remote$editor.executeWithContents(".R", contents, function(editor) {

      # Place a top-level breakpoint on each of the three lines.
      for (idx in 0:2)
      {
         gutterLayer <- remote$js.querySelector(".ace_gutter-layer")
         gutterCell <- gutterLayer$children[[idx]]
         remote$dom.clickElement(objectId = gutterCell, horizontalOffset = -6L)
      }

      .rs.waitUntil("three breakpoint markers present", function() {
         breakpointEls <- remote$js.querySelectorAll(".ace_breakpoint")
         length(breakpointEls) == 3L
      })
      breakpointEls <- remote$js.querySelectorAll(".ace_breakpoint")
      expect_equal(length(breakpointEls), 3L)

      # Invoke Clear All Breakpoints; this fires a Yes/No confirmation
      # before BreakpointManager.clearAllBreakpoints() runs.
      remote$commands.execute("debugClearBreakpoints")
      remote$dom.waitForElement("#rstudio_dlg_yes")
      remote$dom.clickElement("#rstudio_dlg_yes")

      .rs.waitUntil("all breakpoint markers removed", function() {
         !remote$dom.elementExists(".ace_breakpoint") &&
            !remote$dom.elementExists(".ace_pending-breakpoint") &&
            !remote$dom.elementExists(".ace_inactive-breakpoint")
      })
      expect_false(remote$dom.elementExists(".ace_breakpoint"))
      expect_false(remote$dom.elementExists(".ace_pending-breakpoint"))
      expect_false(remote$dom.elementExists(".ace_inactive-breakpoint"))
   })
})

# https://github.com/rstudio/rstudio/issues/9450
# Pisca46's report: a package breakpoint that has been clicked off should
# stay off across a package rebuild. PackageLoadedEvent triggers
# BreakpointManager.updatePackageBreakpoints(); with the breakpoint already
# removed from breakpoints_, no marker should reappear.
.rs.test("cleared package breakpoints stay cleared across rebuild", {

   remote$project.create(projectName = "rstudio.automation", type = "package")
   withr::defer({
      remote$editor.closeDocument()
      # Defensive pause: closeDocument() returns before the IDE has
      # fully unwound the document; closing the project too eagerly
      # races with the unsaved-changes path and the dirty-state probe
      # of the editor instance.
      Sys.sleep(1)
      remote$project.close()
   })

   # Close any open documents from the project template.
   remote$console.executeExpr(
      .rs.api.closeAllSourceBuffersWithoutSaving()
   )

   # Add a source document with a function we can set a breakpoint inside.
   remote$console.executeExpr(file.edit("R/example.R"))
   remote$commands.execute("activateSource")

   code <- .rs.heredoc('
      example <- function() {
         1 + 1
         2 + 2
         3 + 3
      }
   ')

   editor <- remote$editor.getInstance()
   editor$insert(code)
   .rs.waitUntil("code is present in editor", function() {
      grepl("example <- function", editor$getValue(), fixed = TRUE)
   })

   remote$commands.execute("saveSourceDoc")
   remote$commands.execute("buildAll")
   # Short-circuit with the last lines of console output if the build
   # crashes or errors -- otherwise a generic .rs.waitUntil() timeout
   # shows up downstream as a confusing subscript error on gutterEls[[3]].
   .rs.waitUntil("build has completed", function() {
      output <- remote$console.getOutput()
      if (any(grepl("Execution halted|^ERROR\\b", output)))
         stop("build failed:\n",
              paste(utils::tail(output, 20), collapse = "\n"))
      any(output == "> library(rstudio.automation)")
   })
   remote$console.clear()

   # Place a function breakpoint on line 3 ("2 + 2") -- this exercises the
   # TYPE_FUNCTION / package path that goes through trace() in R, as opposed
   # to the simpler TYPE_TOPLEVEL flow in the earlier tests. The horizontal
   # offset of -4L (vs -6L for top-level files above) targets the package
   # gutter's breakpoint area; the value mirrors the #15201 test ("package
   # functions can be debugged after build and reload") and is empirically
   # calibrated -- package projects render a slightly different gutter.
   gutterEls <- remote$js.querySelectorAll(".ace_gutter-cell")
   remote$dom.clickElement(objectId = gutterEls[[3]], horizontalOffset = -4L)
   .rs.waitUntil("breakpoint marker appears", function() {
      remote$dom.elementExists(".ace_breakpoint")
   })

   # Click the same gutter cell again to remove the breakpoint.
   gutterEls <- remote$js.querySelectorAll(".ace_gutter-cell")
   remote$dom.clickElement(objectId = gutterEls[[3]], horizontalOffset = -4L)
   .rs.waitUntil("breakpoint marker is removed", function() {
      !remote$dom.elementExists(".ace_breakpoint") &&
         !remote$dom.elementExists(".ace_pending-breakpoint") &&
         !remote$dom.elementExists(".ace_inactive-breakpoint")
   })

   # Rebuild the package. PackageLoadedEvent fires on completion and runs
   # BreakpointManager.updatePackageBreakpoints("rstudio.automation", true);
   # with breakpoints_ empty, this should be a no-op.
   remote$console.clear()
   remote$commands.execute("buildAll")
   .rs.waitUntil("rebuild has completed", function() {
      output <- remote$console.getOutput()
      if (any(grepl("Execution halted|^ERROR\\b", output)))
         stop("rebuild failed:\n",
              paste(utils::tail(output, 20), collapse = "\n"))
      any(output == "> library(rstudio.automation)")
   })

   # No marker of any kind should have reappeared.
   expect_false(remote$dom.elementExists(".ace_breakpoint"))
   expect_false(remote$dom.elementExists(".ace_pending-breakpoint"))
   expect_false(remote$dom.elementExists(".ace_inactive-breakpoint"))
})
