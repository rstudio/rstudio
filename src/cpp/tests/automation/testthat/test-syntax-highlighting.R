
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
      # TODO: Better wait for document ready when checking Ace tokens.
      Sys.sleep(0.1)
      tokens <- remote$aceLineTokens(8L)
      expect_length(tokens, 1L)
      expect_equal(tokens[[1]]$type,  "text")
      expect_equal(tokens[[1]]$value, "1 + 1")
   })
   
})
