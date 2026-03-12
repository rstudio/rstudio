
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())

.rs.test("chat satellite window can be opened and interacted with", {

   # Ensure the Chat pane is visible, then pop it out.
   remote$commands.execute("activateChat")
   remote$commands.execute("popOutChat")

   # Wait for the satellite window to appear.
   remote$satellites.waitForOpen("Posit Assistant")

   # Verify it shows up in the satellite list.
   satellites <- remote$satellites.list()
   titles <- vapply(satellites, function(t) t$title, FUN.VALUE = character(1))
   expect_true("Posit Assistant" %in% titles)

   # Switch to the satellite and verify we can find an element in it.
   remote$satellites.switchTo("Posit Assistant")
   nodeId <- remote$dom.waitForElement("#rstudio_container")
   expect_true(nodeId > 0L)

   # Switch back to the main window and verify it still works.
   remote$satellites.switchToMain()
   consoleId <- remote$dom.querySelector("#rstudio_console_input")
   expect_true(consoleId > 0L)

   # Close the satellite and verify it's gone.
   remote$commands.execute("returnChatToMain")
   .rs.waitUntil("satellite closes", function() {
      !remote$satellites.isOpen("Posit Assistant")
   })
   expect_false(remote$satellites.isOpen("Posit Assistant"))

   # reset UI back to default layout
   .rs.resetUILayout(remote)
})
