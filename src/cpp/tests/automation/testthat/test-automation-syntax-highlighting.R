
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())


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
   
   remote$documentExecute(".Rmd", documentContents, function(editor) {
      tokens <- as.vector(editor$session$getTokens(8L))
      expect_length(tokens, 1L)
      expect_equal(tokens[[1]]$type,  "text")
      expect_equal(tokens[[1]]$value, "1 + 1")
   })
   
})

# https://github.com/rstudio/rstudio/issues/14652
test_that("Quarto callout divs are tokenized correctly", {
   
   documentContents <- .rs.heredoc('
      ::: {.callout 0}
      Hello, world!
      :::
      
      ::: {.callout 1}
      Goodbye, world!
      :::
   ')
   
   remote$consoleExecute(".rs.writeUserPref(\"rainbow_fenced_divs\", TRUE)")
   remote$documentExecute(".Rmd", documentContents, function(editor) {
      
      tokens <- as.vector(editor$session$getTokens(0L))
      expect_length(tokens, 2L)
      expect_equal(tokens[[1]]$type,  "fenced_div_0")
      expect_equal(tokens[[1]]$value, ":::")
      expect_equal(tokens[[2]]$type,  "fenced_div_text_0")
      expect_equal(tokens[[2]]$value, " {.callout 0}")
      
      tokens <- as.vector(editor$session$getTokens(4L))
      expect_length(tokens, 2L)
      expect_equal(tokens[[1]]$type,  "fenced_div_1")
      expect_equal(tokens[[1]]$value, ":::")
      expect_equal(tokens[[2]]$type,  "fenced_div_text_1")
      expect_equal(tokens[[2]]$value, " {.callout 1}")
      
      state <- editor$session$getState(5L)
      expect_equal(state, "start")
   })
   remote$consoleExecute(".rs.writeUserPref(\"rainbow_fenced_divs\", FALSE)")
   
})

# https://github.com/rstudio/rstudio/issues/14699
test_that("Quarto chunks receive chunk begin / end markers as expected", {
   
   documentContents <- .rs.heredoc('
      ---
      title: Quarto Document
      ---
      
      ```{r}
      # This is a code chunk.
      ```
   ')
   
   remote$documentExecute(".qmd", documentContents, function(editor) {
      startWidget <- editor$session$getFoldWidget(4L)
      expect_equal(startWidget, "start")
      
      endWidget <- editor$session$getFoldWidget(6L)
      expect_equal(endWidget, "end")
   })
   
})

# https://github.com/rstudio/rstudio/issues/14592
test_that("The sequence '# |' is not tokenized as a Quarto comment prefix", {
  
   documentContents <- .rs.heredoc('
      #|  yaml: true
      # | yaml: false
   ')
   
   remote$documentExecute(".R", documentContents, function(editor) {
      
      tokens <- as.vector(editor$session$getTokens(0L))
      firstToken <- tokens[[1L]]
      expect_equal(firstToken$type, "comment.doc.tag")
      expect_equal(firstToken$value, "#|")
      
      tokens <- as.vector(editor$session$getTokens(1L))
      expect_equal(tokens, list(
         list(
            type   = "comment",
            value  = "# | yaml: false",
            column = 0
         )
      ))
      
   })
   
})
