
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
   
   remote$documentOpen(".R", contents)
   
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

# https://github.com/rstudio/rstudio/issues/15201
.rs.test("package functions can be debugged after build and reload", {
   
   # Create an R package project.
   projectPath <- tempfile("rstudio.automation.", tmpdir = dirname(tempdir()))
   
   remote$consoleExecuteExpr({
      .rs.rpc.package_skeleton(
         packageName = "rstudio.automation",
         packageDirectory = !!projectPath,
         sourceFiles = character(),
         usingRcpp = FALSE
      )
   })
   
   # Open that project.
   remote$consoleExecuteExpr(
      .rs.api.openProject(!!projectPath),
      wait = FALSE
   )
   
   # Wait a bit for the new session to load.
   Sys.sleep(3)

   # Wait until the new project is ready.
   .rs.waitUntil("the new project is opened", function()
   {
      el <- remote$jsObjectViaSelector("#rstudio_project_menubutton_toolbar")
      grepl("rstudio.automation", el$innerText, fixed = TRUE)
   }, swallowErrors = TRUE)
   
   # Close any open documents
   remote$consoleExecuteExpr(
      .rs.api.closeAllSourceBuffersWithoutSaving()
   )
   
   # Add a source document.
   remote$consoleExecuteExpr(file.edit("R/example.R"))
   remote$commandExecute("activateSource")
   
   code <- .rs.heredoc('
      example <- function() {
         1 + 1
         2 + 2
         3 + 3
         4 + 4
         5 + 5
      }
   ')
   
   editor <- remote$editorGetInstance()
   editor$insert(code)
   
   # Save it, and build the package.
   remote$commandExecute("saveSourceDoc")
   remote$commandExecute("buildAll")
   
   .rs.waitUntil("build has completed", function()
   {
      output <- remote$consoleOutput()
      any(output == "> library(rstudio.automation)")
   }, swallowErrors = TRUE)
   
   remote$consoleClear()
   
   # Try adding some breakpoints.
   gutterEls <- remote$jsObjectsViaSelector(".ace_gutter-cell")
   remote$domClickElement(objectId = gutterEls[[3]], horizontalOffset = -4L)
   breakpointEls <- remote$jsObjectsViaSelector(".ace_breakpoint")
   expect_equal(length(breakpointEls), 1L)
   expect_equal(breakpointEls[[1]]$innerText, "3")
   
   remote$domClickElement(objectId = gutterEls[[4]], horizontalOffset = -4L)
   breakpointEls <- remote$jsObjectsViaSelector(".ace_breakpoint")
   expect_equal(length(breakpointEls), 2L)
   expect_equal(breakpointEls[[2]]$innerText, "4")
   
   # Confirm that the object definition is in sync.
   remote$consoleExecuteExpr({
      .rs.isFunctionInSync("example", "R/example.R", "rstudio.automation")
   })
   
   output <- remote$consoleOutput()
   expect_contains(output, "[1] TRUE")
   remote$consoleClear()
   
   # Try running the function, and checking the view.
   remote$consoleExecuteExpr(example())
   
   activeLineEl <- remote$jsObjectViaSelector(".ace_executing-line")
   expect_equal(activeLineEl$innerText, "3")
   remote$commandExecute("activateConsole")
   remote$keyboardExecute("c", "<Enter>")
   
   activeLineEl <- remote$jsObjectViaSelector(".ace_executing-line")
   expect_equal(activeLineEl$innerText, "4")
   remote$keyboardExecute("c", "<Enter>")
   
   # All done testing; close the project.
   remote$documentClose()
   Sys.sleep(1)
   remote$commandExecute("closeProject")
   
   # Wait until the project has closed
   .rs.waitUntil("the project is closed", function()
   {
      el <- remote$jsObjectViaSelector("#rstudio_project_menubutton_toolbar")
      grepl("Project: ", el$innerText, fixed = TRUE)
   }, swallowErrors = TRUE)
    
})
