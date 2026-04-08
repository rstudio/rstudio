library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())

# --------------------------------------------------------------------------------------------------
# Tests follow...
# --------------------------------------------------------------------------------------------------

.rs.test("Chat pane does not refresh when Global Options dismissed without changes", {
   skip_on_ci()

   # Close sidebar on exit even if the test errors
   withr::defer({
      if (remote$dom.elementExists("#rstudio_Sidebar_pane")) {
         remote$commands.execute("toggleSidebar")
         .rs.waitUntil("sidebar hidden", function() {
            !remote$dom.elementExists("#rstudio_Sidebar_pane")
         })
      }
   })

   # Ensure the sidebar is visible so the chat pane is showing
   if (!remote$dom.elementExists("#rstudio_Sidebar_pane")) {
      remote$commands.execute("toggleSidebar")
      .rs.waitUntil("sidebar visible", function() {
         remote$dom.elementExists("#rstudio_Sidebar_pane")
      })
   }

   # Activate the chat tab to ensure it's the selected tab in the sidebar
   remote$commands.execute("activateChat")

   # Wait for the chat pane iframe to exist
   .rs.waitUntil("chat iframe exists", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane iframe")
   })

   # Let the chat pane finish its initial load sequence. The pane writes content
   # via setFrameContent() which triggers multiple load events (about:blank
   # navigation + doc.open/write/close). We need these to settle before
   # attaching our listener.
   Sys.sleep(2)

   # Attach a load event counter on the iframe element. Any reload -- whether
   # via setUrl() or doc.open()/write()/close() -- fires the iframe's load
   # event. This avoids contentDocument access (which may be cross-origin
   # restricted in the BRAT environment).
   remote$js.eval("(() => {
      var f = document.querySelector('#rstudio_Sidebar_pane iframe');
      f._bratReloadCount = 0;
      f.addEventListener('load', function() { f._bratReloadCount++; });
   })()")

   # Open Global Options dialog
   remote$commands.execute("showOptions")
   remote$dom.waitForElement(".gwt-DialogBox")

   # Click OK without making any changes
   remote$dom.clickElement(selector = "#rstudio_preferences_confirm")
   .rs.waitUntil("dialog closed", function() {
      !remote$dom.elementExists(".gwt-DialogBox")
   })

   # Brief pause to allow any spurious reload to start
   Sys.sleep(1)

   # Verify no load events fired -- chat did NOT refresh
   reloadCount <- remote$js.eval(
      "document.querySelector('#rstudio_Sidebar_pane iframe')._bratReloadCount")
   expect_equal(reloadCount, 0L,
                info = "Chat iframe should not reload when options dismissed without changes")
})

.rs.test("Posit Assistant pane survives R session restart", {
   skip_on_ci()

   # Close sidebar on exit even if the test errors
   withr::defer({
      if (remote$dom.elementExists("#rstudio_Sidebar_pane")) {
         remote$commands.execute("toggleSidebar")
         .rs.waitUntil("sidebar hidden", function() {
            !remote$dom.elementExists("#rstudio_Sidebar_pane")
         })
      }
   })

   # 1. Show the sidebar
   if (!remote$dom.elementExists("#rstudio_Sidebar_pane")) {
      remote$commands.execute("toggleSidebar")
      .rs.waitUntil("sidebar visible", function() {
         remote$dom.elementExists("#rstudio_Sidebar_pane")
      })
   }

   # Activate the chat tab and wait for iframe
   remote$commands.execute("activateChat")
   .rs.waitUntil("chat iframe exists", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane iframe")
   })

   # 2. Wait for iframe to render "Posit Assistant Not Installed" heading
   .rs.waitUntil("iframe h2 shows expected text", function() {
      iframe <- remote$js.querySelector("#rstudio_Sidebar_pane iframe")
      h2 <- iframe$contentWindow$document$querySelector("h2")
      identical(h2$innerText, "Posit Assistant Not Installed")
   }, swallowErrors = TRUE)
   iframe <- remote$js.querySelector("#rstudio_Sidebar_pane iframe")
   h2 <- iframe$contentWindow$document$querySelector("h2")
   expect_equal(h2$innerText, "Posit Assistant Not Installed")

   # 3. Hide the sidebar
   remote$commands.execute("toggleSidebar")
   .rs.waitUntil("sidebar hidden", function() {
      !remote$dom.elementExists("#rstudio_Sidebar_pane")
   })

   # 4. Restart the R session
   remote$session.restart()

   # 5. Show sidebar again, verify content survived restart
   remote$commands.execute("toggleSidebar")
   .rs.waitUntil("sidebar visible", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane")
   })
   remote$commands.execute("activateChat")
   .rs.waitUntil("chat iframe exists", function() {
      remote$dom.elementExists("#rstudio_Sidebar_pane iframe")
   })

   # Wait for iframe content to fully render after restart
   .rs.waitUntil("iframe h2 shows expected text after restart", function() {
      iframe <- remote$js.querySelector("#rstudio_Sidebar_pane iframe")
      h2 <- iframe$contentWindow$document$querySelector("h2")
      identical(h2$innerText, "Posit Assistant Not Installed")
   }, swallowErrors = TRUE)
   iframe <- remote$js.querySelector("#rstudio_Sidebar_pane iframe")
   h2 <- iframe$contentWindow$document$querySelector("h2")
   expect_equal(h2$innerText, "Posit Assistant Not Installed")

   # Wait for install button to appear, then verify
   .rs.waitUntil("install button rendered", function() {
      iframe <- remote$js.querySelector("#rstudio_Sidebar_pane iframe")
      btn <- iframe$contentWindow$document$querySelector("#install-btn")
      !is.null(btn) && nzchar(btn$innerText)
   }, swallowErrors = TRUE)
   button <- iframe$contentWindow$document$querySelector("#install-btn")
   expect_false(is.null(button))
   expect_equal(button$innerText, "Install Posit Assistant")
   expect_false(button$disabled)
})
