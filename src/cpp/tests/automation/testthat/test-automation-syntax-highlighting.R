
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())


.rs.test("Quarto Documents are highlighted as expected", {
   
   documentContents <- .rs.heredoc('
      ---
      title: R Markdown Document
      ---

      ```{r}
      #| echo: true
      ```

      1 + 1
   ')
   
   remote$editor.executeWithContents(".Rmd", documentContents, function(editor) {
      tokens <- as.vector(editor$session$getTokens(8L))
      expect_length(tokens, 1L)
      expect_equal(tokens[[1]]$type,  "text")
      expect_equal(tokens[[1]]$value, "1 + 1")
   })
   
})

# https://github.com/rstudio/rstudio/issues/14652
.rs.test("Quarto callout divs are tokenized correctly", {
   
   documentContents <- .rs.heredoc('
      ::: {.callout 0}
      Hello, world!
      :::
      
      ::: {.callout 1}
      Goodbye, world!
      :::
   ')
   
   remote$console.execute(".rs.writeUserPref(\"rainbow_fenced_divs\", TRUE)")
   remote$editor.executeWithContents(".Rmd", documentContents, function(editor) {
      
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
   remote$console.execute(".rs.writeUserPref(\"rainbow_fenced_divs\", FALSE)")
   
})

# https://github.com/rstudio/rstudio/issues/14699
.rs.test("Quarto chunks receive chunk begin / end markers as expected", {
   
   documentContents <- .rs.heredoc('
      ---
      title: Quarto Document
      ---
      
      ```{r}
      # This is a code chunk.
      ```
   ')
   
   remote$editor.executeWithContents(".qmd", documentContents, function(editor) {
      startWidget <- editor$session$getFoldWidget(4L)
      expect_equal(startWidget, "start")
      
      endWidget <- editor$session$getFoldWidget(6L)
      expect_equal(endWidget, "end")
   })
   
})

# https://github.com/rstudio/rstudio/issues/14592
.rs.test("The sequence '# |' is not tokenized as a Quarto comment prefix", {
  
   documentContents <- .rs.heredoc('
      #|  yaml: true
      # | yaml: false
   ')
   
   remote$editor.executeWithContents(".R", documentContents, function(editor) {
      
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

# https://github.com/rstudio/rstudio/issues/15019
.rs.test("tikz chunks are properly highlighted", {
   
   documentContents <- .rs.heredoc('
      ---
      title: tikz chunks
      ---
      
      ```{tikz}
      % This is a tikz chunk.
      ```
      
      ```{r}
      # This is an R chunk.
      "hello"
      ```
   ')
   
   remote$editor.executeWithContents(".Rmd", documentContents, function(editor) {
      
      token <- as.vector(editor$session$getTokenAt(5, 0))
      expect_match(token$type, "comment")
      expect_equal(token$value, "% This is a tikz chunk.")
      
      token <- as.vector(editor$session$getTokenAt(9, 0))
      expect_match(token$type, "comment")
      expect_equal(token$value, "# This is an R chunk.")
      
      token <- as.vector(editor$session$getTokenAt(10, 0))
      expect_match(token$type, "string")
      expect_equal(token$value, "\"hello\"")
      
   })
   
})

# https://github.com/rstudio/rstudio/issues/12161
.rs.test("nested GitHub chunks are highlighted appropriately", {
   
   contents <- .rs.heredoc('
      ---
      title: "Untitled"
      format: html
      ---
      
      ## Heading 1
      
      ````` markdown
      ``` nested
      This is a nested chunk.
      ```
      `````
      
      ## Heading 2
   ')
   
   remote$editor.executeWithContents(".qmd", contents, function(editor) {
      
      # first header
      token <- as.vector(editor$session$getTokenAt(5, 0))
      expect_equal(token$type, "markup.heading.2")
      
      # line within the nested chunk
      token <- as.vector(editor$session$getTokenAt(9, 0))
      expect_equal(token$type, "support.function")
      
      # second header
      token <- as.vector(editor$session$getTokenAt(13, 0))
      expect_equal(token$type, "markup.heading.2")
      
   })
   
})
