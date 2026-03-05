
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())


.rs.test("a terminal can be created and is visible", {

   # ensure no terminals are open
   remote$console.executeExpr({
      rstudioapi::terminalKill(rstudioapi::terminalList())
   })

   # create a new terminal
   remote$console.executeExpr({
      rstudioapi::terminalCreate(show = TRUE)
   })

   # wait for the xterm widget to appear
   .rs.waitUntil("xterm widget appears", function() {
      remote$dom.elementExists(".xterm")
   })

   # wait until we have a terminal tab
   terminalTab <- NULL
   .rs.waitUntil("terminal tab is available", function() {
      terminalTab <<- remote$js.querySelector("#rstudio_workbench_tab_terminal")
      !is.null(terminalTab)
   }, swallowErrors = TRUE)
   
   # make sure it's selected
   expect_equal(terminalTab$getAttribute("aria-selected"), "true")

   # verify the xterm widget has non-zero dimensions
   xterm <- remote$js.querySelector(".xterm")
   expect_true(xterm$offsetWidth > 0)
   expect_true(xterm$offsetHeight > 0)
   
   # return focus to console
   remote$commands.execute("activateConsole")
   
   # clean up
   remote$console.executeExpr({
      rstudioapi::terminalKill(rstudioapi::terminalList())
   })

})

.rs.test("we can run commands in the terminal", {
   
   # ensure no terminals are open
   remote$console.executeExpr({
      rstudioapi::terminalKill(rstudioapi::terminalList())
   })
   
   # create a new terminal
   remote$console.executeExpr({
      rstudioapi::terminalCreate(show = TRUE)
   })
   
   # wait for the xterm widget to appear
   .rs.waitUntil("xterm widget appears", function() {
      remote$dom.elementExists(".xterm")
   })
   
   # wait until we have a terminal tab
   terminalTab <- NULL
   .rs.waitUntil("terminal tab is available", function() {
      terminalTab <<- remote$js.querySelector("#rstudio_workbench_tab_terminal")
      !is.null(terminalTab)
   }, swallowErrors = TRUE)
   
   # make sure it's selected
   expect_equal(terminalTab$getAttribute("aria-selected"), "true")
   
   # try executing a basic command
   remote$keyboard.insertText("expr 1 + 1")
   remote$keyboard.executeShortcut("Enter")

   # wait for terminal output before capturing
   Sys.sleep(1)

   remote$commands.execute("sendTerminalToEditor")
   
   editor <- remote$editor.getInstance()
   contents <- editor$session$doc$getValue()
   contents <- gsub("\r?\n+", "\n", contents, perl = TRUE)
   expect_true(grepl("expr 1 + 1\n2\n", contents, fixed = TRUE))
   
   # return focus to console
   remote$commands.execute("activateConsole")
   
   # clean up
   remote$console.executeExpr({
      rstudioapi::terminalKill(rstudioapi::terminalList())
   })
   
})
