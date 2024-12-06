
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
