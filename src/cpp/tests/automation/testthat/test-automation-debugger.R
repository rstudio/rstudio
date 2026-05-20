
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())

# The Playwright suite covers most debugger tests now -- see
# e2e/rstudio/tests/panes/debugger/debugger.test.ts and
# debugger_extras.test.ts. These two package-build tests remain in BRAT:
# both create a full R package project, build it, and depend on
# devtools/library() resolving in the rsession's R install. The build
# cycle (~minutes) and the project-restart side effects don't fit the
# Playwright fixture model well, so we keep BRAT coverage for them.

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
