
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())


test_that("Braces are inserted and highlighted correctly in Sweave documents", {
   
   documentContents <- .rs.heredoc('
      This is a Sweave document.
      
      <<>>=
      
      @
   ')
      
   remote$documentExecute(".Rnw", documentContents, function(editor) {
      editor$gotoLine(4L, 0L)
      remote$client$Input.insertText(text = "{ 1 + 1 }")
      Sys.sleep(1)  # wait for Ace to tokenize
      tokens <- as.vector(editor$session$getTokens(3L))
      values <- vapply(tokens, `[[`, "value", FUN.VALUE = character(1))
      expected <- c("{", " ", "1", " ", "+", " ", "1", " ", "}")
      expect_equal(values, expected)
   })
   
})
