
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())

test_that("whitespace is trimmed on save appropriately", {
   
   # strip trailing whitespace in this scope
   remote$consoleExecuteExpr({
      .rs.uiPrefs$stripTrailingWhitespace$set(TRUE)
   })
   
   # NOTE: using '\x20' as hex escape for space character
   contents <- .rs.heredoc('
      # comment 1\x20\x20
      # comment 2\x20
      # comment 3\x20\x20\x20
   ')
   
   remote$documentExecute(".R", contents, function(editor) {
      
      # make an edit, then save the document
      editor$gotoLine(4)
      editor$insert("# comment 4\x20\x20\x20")
      remote$commandExecute("saveSourceDoc")
      Sys.sleep(0.1)
      
      # check that whitespace has been removed
      contents <- editor$getValue()
      expect_equal(contents, "# comment 1\n# comment 2\n# comment 3\n# comment 4")
      
   })
   
   # reset pref
   remote$consoleExecuteExpr({
      .rs.uiPrefs$stripTrailingWhitespace$clear()
   })
   
})
