
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
