
library(testthat)

self <- remote <- .rs.automation.createRemote(mode = "desktop")
client <- remote$client

test_that("Quarto Documents are highlighted as expected", {
   
   documentContents <- .rs.heredoc('
      ---
      title: R Markdown Document
      ---

      ```{r}
      #| echo: true
      ````

      1 + 1
   ')
   
   remote$documentExecute(".Rmd", documentContents, {
      tokens <- remote$aceLineTokens(8L)
      expect_length(tokens, 1L)
      expect_equal(tokens[[1]]$type,  "text")
      expect_equal(tokens[[1]]$value, "1 + 1")
   })
   
})

test_that("Sweave documents are highlighted as expected", {
  
   documentContents <- .rs.heredoc('
      
      This is a Sweave document.
      
      <<>>=
      
      @
      
   ')
   
   remote$documentExecute(".Rnw", documentContents, {
      remote$aceSetCursorPosition(3, 0)
      client$Input.insertText(text = "{ 1 + 1 }")
      tokens <- remote$aceLineTokens(row = 3L)
      expect_length(tokens, 5L)
   })
   
})
