
library(testthat)

self <- remote <- .rs.automation.newRemote()
on.exit(.rs.automation.deleteRemote(), add = TRUE)


test_that("Braces are inserted and highlighted correctly in Sweave documents", {
   
   documentContents <- .rs.heredoc('
      This is a Sweave document.
      
      <<>>=
      
      @
   ')
      
   remote$documentExecute(".Rnw", documentContents, {
      Sys.sleep(0.1)
      remote$editorSetCursorPosition(3, 0)
      Sys.sleep(0.1)
      client$Input.insertText(text = "{ 1 + 1 }")
      tokens <- remote$editorLineTokens(row = 3L)
      values <- vapply(tokens, `[[`, "value", FUN.VALUE = character(1))
      expected <- c("{", " ", "1", " ", "+", " ", "1", " ", "}")
      expect_equal(values, expected)
   })
   
})
