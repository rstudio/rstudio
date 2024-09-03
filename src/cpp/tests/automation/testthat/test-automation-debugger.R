
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())

# https://github.com/rstudio/rstudio/issues/15072
test_that("the debug position is correct in braced expressions", {

   contents <- .rs.heredoc('
      f <- function() {
         1 + 1
         {
            2 + 2
         }
      }
   ')
   
   remote$documentOpen(".R", contents)
   on.exit(remote$documentClose(), add = TRUE)
   
   # Click to set a breakpoint.
   gutterLayer <- remote$jsObjectViaSelector(".ace_gutter-layer")
   gutterCell <- gutterLayer$children[[3]]
   remote$domClickElement(objectId = gutterCell, horizontalOffset = -6L)
   
   # Clear the current selection if we have one.
   editor <- remote$editorGetInstance()
   editor$clearSelection()
   
   # Source the file.
   remote$commandExecute("sourceActiveDocument")
   
   # Execute the function.
   remote$consoleExecute("f()")
   
   # Check that debug highlighting was set on the fourth row.
   gutterCell <- remote$jsObjectViaSelector(".ace_executing-line")
   gutterParent <- gutterCell$parentElement
   gutterChild <- gutterParent$children[[3]]
   expect_equal(gutterCell$innerText, gutterChild$innerText)
   
   # Get the screen position of the debug rectangle.
   debugLine <- remote$jsObjectViaSelector(".ace_active_debug_line")
   debugRect <- debugLine$getBoundingClientRect()
   
   # Figure out what row that maps to in the editor.
   screenCoords <- editor$session$renderer$pixelToScreenCoordinates(
      debugRect$x + debugRect$width / 2,
      debugRect$y + debugRect$height / 2
   )
   expect_equal(screenCoords$row, 3)
   
   # Exit the debugger.
   remote$keyboardExecute("<Ctrl + 2>", "c", "<Enter>")
   remote$consoleExecuteExpr(rm(list = "f"))
   remote$keyboardExecute("<Ctrl + L>")
   
})
