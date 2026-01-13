
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

# https://github.com/rstudio/rstudio/issues/16657
.rs.test("R files highlight hex color strings", {

   # Tokens for 'col1 <- "#ff0000"':
   # 1: col1 (identifier)
   # 2: ' ' (text)
   # 3: <- (keyword.operator)
   # 4: ' ' (text)
   # 5: " (string)
   # 6: #ff0000 (string.color)
   # 7: " (string)

   documentContents <- .rs.heredoc('
      col1 <- "#ff0000"
      col2 <- "#00ff00ff"
      col3 <- "#abc"
   ')

   remote$editor.executeWithContents(".R", documentContents, function(editor) {

      # Test #rrggbb format
      tokens <- as.vector(editor$session$getTokens(0L))
      colorToken <- tokens[[6L]]
      expect_equal(colorToken$type, "string.color")
      expect_equal(colorToken$value, "#ff0000")
      expect_equal(colorToken$bg, "#ff0000")

      # Test #rrggbbaa format
      tokens <- as.vector(editor$session$getTokens(1L))
      colorToken <- tokens[[6L]]
      expect_equal(colorToken$type, "string.color")
      expect_equal(colorToken$value, "#00ff00ff")
      expect_equal(colorToken$bg, "#00ff00ff")

      # Test #rgb format
      tokens <- as.vector(editor$session$getTokens(2L))
      colorToken <- tokens[[6L]]
      expect_equal(colorToken$type, "string.color")
      expect_equal(colorToken$value, "#abc")
      expect_equal(colorToken$bg, "#abc")

   })

})

# https://github.com/rstudio/rstudio/issues/16657
.rs.test("R files highlight named color strings", {

   # Tokens for 'col1 <- "red"':
   # 1: col1 (identifier)
   # 2: ' ' (text)
   # 3: <- (keyword.operator)
   # 4: ' ' (text)
   # 5: " (string)
   # 6: red (string.color)
   # 7: " (string)
   #
   # For non-color 'col3 <- "notacolor"':
   # 1-4: same as above
   # 5: "notacolor" (string) - single token since not a recognized color

   documentContents <- .rs.heredoc('
      col1 <- "red"
      col2 <- "steelblue4"
      col3 <- "notacolor"
   ')

   remote$editor.executeWithContents(".R", documentContents, function(editor) {

      # Test named color "red"
      tokens <- as.vector(editor$session$getTokens(0L))
      colorToken <- tokens[[6L]]
      expect_equal(colorToken$type, "string.color")
      expect_equal(colorToken$value, "red")
      expect_equal(colorToken$bg, "#ff0000")

      # Test named color "steelblue4"
      tokens <- as.vector(editor$session$getTokens(1L))
      colorToken <- tokens[[6L]]
      expect_equal(colorToken$type, "string.color")
      expect_equal(colorToken$value, "steelblue4")
      expect_equal(colorToken$bg, "#36648b")

      # Test non-color string (should be regular string token)
      tokens <- as.vector(editor$session$getTokens(2L))
      stringToken <- tokens[[5L]]
      expect_equal(stringToken$type, "string")
      expect_equal(stringToken$value, "\"notacolor\"")

   })

})

# https://github.com/rstudio/rstudio/issues/16657
.rs.test("YAML files highlight hex color strings", {

   # Tokens for 'color1: "#ff0000"':
   # 1: color1 (meta.tag)
   # 2: ': ' (keyword.operator) - includes trailing space
   # 3: " (string)
   # 4: #ff0000 (string.color)
   # 5: " (string)

   documentContents <- .rs.heredoc('
      color1: "#ff0000"
      color2: "#00ff00ff"
      color3: "#abc"
   ')

   remote$editor.executeWithContents(".yml", documentContents, function(editor) {

      # Test #rrggbb format
      tokens <- as.vector(editor$session$getTokens(0L))
      colorToken <- tokens[[4L]]
      expect_equal(colorToken$type, "string.color")
      expect_equal(colorToken$value, "#ff0000")
      expect_equal(colorToken$bg, "#ff0000")

      # Test #rrggbbaa format
      tokens <- as.vector(editor$session$getTokens(1L))
      colorToken <- tokens[[4L]]
      expect_equal(colorToken$type, "string.color")
      expect_equal(colorToken$value, "#00ff00ff")
      expect_equal(colorToken$bg, "#00ff00ff")

      # Test #rgb format
      tokens <- as.vector(editor$session$getTokens(2L))
      colorToken <- tokens[[4L]]
      expect_equal(colorToken$type, "string.color")
      expect_equal(colorToken$value, "#abc")
      expect_equal(colorToken$bg, "#abc")

   })

})

# https://github.com/rstudio/rstudio/issues/16657
.rs.test("YAML files highlight named color strings", {

   # Tokens for 'color1: "red"':
   # 1: color1 (meta.tag)
   # 2: ': ' (keyword.operator)
   # 3: " (string)
   # 4: red (string.color)
   # 5: " (string)
   #
   # For non-color 'color3: "notacolor"':
   # 1: color3 (meta.tag)
   # 2: ': ' (keyword.operator)
   # 3: "notacolor" (string) - single token since not a recognized color

   documentContents <- .rs.heredoc('
      color1: "red"
      color2: "steelblue4"
      color3: "notacolor"
   ')

   remote$editor.executeWithContents(".yml", documentContents, function(editor) {

      # Test named color "red"
      tokens <- as.vector(editor$session$getTokens(0L))
      colorToken <- tokens[[4L]]
      expect_equal(colorToken$type, "string.color")
      expect_equal(colorToken$value, "red")
      expect_equal(colorToken$bg, "#ff0000")

      # Test named color "steelblue4"
      tokens <- as.vector(editor$session$getTokens(1L))
      colorToken <- tokens[[4L]]
      expect_equal(colorToken$type, "string.color")
      expect_equal(colorToken$value, "steelblue4")
      expect_equal(colorToken$bg, "#36648b")

      # Test non-color string (should be regular string token)
      tokens <- as.vector(editor$session$getTokens(2L))
      stringToken <- tokens[[3L]]
      expect_equal(stringToken$type, "string")
      expect_equal(stringToken$value, "\"notacolor\"")

   })

})

# https://github.com/rstudio/rstudio/issues/16657
.rs.test("YAML files highlight unquoted named colors", {

   # Tokens for 'color1: red':
   # 1: color1 (meta.tag)
   # 2: ': ' (keyword.operator)
   # 3: red (string.color)
   #
   # For non-color 'color3: notacolor':
   # 1: color3 (meta.tag)
   # 2: ': ' (keyword.operator)
   # 3: notacolor (text)

   documentContents <- .rs.heredoc('
      color1: red
      color2: steelblue4
      color3: notacolor
   ')

   remote$editor.executeWithContents(".yml", documentContents, function(editor) {

      # Test named color "red"
      tokens <- as.vector(editor$session$getTokens(0L))
      colorToken <- tokens[[3L]]
      expect_equal(colorToken$type, "string.color")
      expect_equal(colorToken$value, "red")
      expect_equal(colorToken$bg, "#ff0000")

      # Test named color "steelblue4"
      tokens <- as.vector(editor$session$getTokens(1L))
      colorToken <- tokens[[3L]]
      expect_equal(colorToken$type, "string.color")
      expect_equal(colorToken$value, "steelblue4")
      expect_equal(colorToken$bg, "#36648b")

      # Test non-color (should be regular text token)
      tokens <- as.vector(editor$session$getTokens(2L))
      textToken <- tokens[[3L]]
      expect_equal(textToken$type, "text")
      expect_equal(textToken$value, "notacolor")

   })

})
