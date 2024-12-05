
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
      remote$commands.execute("saveSourceDoc")
      Sys.sleep(0.1)
      
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
