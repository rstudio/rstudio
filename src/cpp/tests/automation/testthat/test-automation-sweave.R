
library(testthat)

self <- remote <- .rs.automation.createRemote()
client <- remote$client

test_that("Braces are inserted and highlighted correctly in Sweave documents", {
   
   documentContents <- .rs.heredoc('
      This is a Sweave document.
      
      <<>>=
      
      @
   ')
      
   remote$documentExecute(".Rnw", documentContents, {
      remote$aceSetCursorPosition(3, 0)
      client$Input.insertText(text = "{ 1 + 1 }")
      tokens <- remote$aceLineTokens(row = 3L)
      values <- vapply(tokens, `[[`, "value", FUN.VALUE = character(1))
      expected <- c("{", " ", "1", " ", "+", " ", "1", " ", "}")
      expect_equal(values, expected)
   })
   
})
