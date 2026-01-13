
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())

.rs.test("whitespace is trimmed on save appropriately", {
   
   # strip trailing whitespace in this scope
   remote$console.executeExpr({
      .rs.uiPrefs$stripTrailingWhitespace$set(TRUE)
   })
   
   # NOTE: using '\x20' as hex escape for space character
   contents <- .rs.heredoc('
      # comment 1\x20\x20
      # comment 2\x20
      # comment 3\x20\x20\x20
   ')
   
   remote$editor.executeWithContents(".R", contents, function(editor) {
      
      # make an edit, then save the document
      editor$gotoLine(4)
      editor$insert("# comment 4\x20\x20\x20")
      Sys.sleep(1)

      remote$commands.execute("saveSourceDoc")
      Sys.sleep(1)
      
      # check that whitespace has been removed
      contents <- .rs.trimWhitespace(editor$getValue())
      expected <- "# comment 1\n# comment 2\n# comment 3\n# comment 4"
      expect_equal(!!contents, !!expected)
      
   })
   
   # reset pref
   remote$console.executeExpr({
      .rs.uiPrefs$stripTrailingWhitespace$clear()
   })

})

# https://github.com/rstudio/rstudio/issues/16798
.rs.test("whole word search and replace handles dots correctly", {

   contents <- ".hello\n.hello\n.hello"

   remote$editor.executeWithContents(".R", contents, function(editor) {

      # Open find and replace dialog
      remote$commands.execute("findReplace")
      Sys.sleep(0.5)

      # Enable "Whole word" option
      remote$dom.clickElement(".rstudio-find-replace-whole-word-checkbox")

      # Focus the find input
      remote$dom.clickElement(".rstudio-find-replace-find-input")
      
      # Type .hello in the find box
      remote$keyboard.insertText(".hello")
      remote$dom.clickElement(".rstudio-find-replace-find-next-button")

      # Focus the replace input
      remote$dom.clickElement(".rstudio-find-replace-replace-input")
      
      # Type .goodbye in the replace box
      remote$keyboard.insertText(".goodbye")

      # Press Enter twice to replace two entries
      remote$dom.clickElement(".rstudio-find-replace-replace-button")
      remote$dom.clickElement(".rstudio-find-replace-replace-button")
      Sys.sleep(0.5)
      
      # Restore the prior state
      remote$dom.clickElement(".rstudio-find-replace-whole-word-checkbox")

      # Close the find/replace bar
      remote$keyboard.executeShortcut("Escape")
      Sys.sleep(0.5)

      # Check that only two entries were replaced
      contents <- editor$getValue()
      expected <- ".goodbye\n.goodbye\n.hello\n"
      expect_equal(!!contents, !!expected)

   })

})
