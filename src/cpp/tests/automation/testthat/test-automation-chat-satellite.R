
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

   # Switch to the satellite and verify the return button is present.
   result <- remote$satellites.execute("Posit Assistant", function() {
      remote$dom.waitForElement("#rstudio_chat_return_to_main_button")
   })
   expect_true(result > 0L)

   # Verify auto-switch back to the main window works.
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

.rs.test("chat pop-out and return buttons work via DOM clicks", {
   skip_on_ci()

   # Activate the chat pane to ensure the sidebar is visible.
   remote$commands.execute("activateChat")
   .rs.waitUntil("sidebar is visible", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane")
   })

   # Click the pop-out button in the chat pane toolbar.
   remote$dom.clickElement("#rstudio_tb_popoutchat")

   # Verify the satellite window opened.
   remote$satellites.waitForOpen("Posit Assistant")
   expect_true(remote$satellites.isOpen("Posit Assistant"))

   # Switch to the satellite and verify the return-to-main button exists.
   remote$satellites.switchTo("Posit Assistant")
   nodeId <- remote$dom.waitForElement("#rstudio_chat_return_to_main_button")
   expect_true(nodeId > 0L)
   remote$satellites.switchToMain()

   # Verify the sidebar is hidden in the main window.
   .rs.waitUntil("sidebar is hidden", function() {
      !remote$dom.elementExists("#rstudio_Sidebar_pane")
   })

   # Click the return-to-main button in the satellite window.
   remote$satellites.switchTo("Posit Assistant")
   remote$dom.clickElement("#rstudio_chat_return_to_main_button")
   remote$satellites.switchToMain()

   # Verify the satellite window closes.
   .rs.waitUntil("satellite closes", function() {
      !remote$satellites.isOpen("Posit Assistant")
   })
   expect_false(remote$satellites.isOpen("Posit Assistant"))

   # Verify the sidebar is visible again.
   .rs.waitUntil("sidebar is visible again", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane")
   })

   # Verify the sidebar contains the Chat tab.
   chatTab <- remote$dom.querySelector("#rstudio_workbench_tab_posit_assistant")
   expect_true(chatTab > 0L)

   # Reset to default UI state.
   .rs.resetUILayout(remote)
})
