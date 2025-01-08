
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())


.rs.test("Braces are inserted and highlighted correctly in Sweave documents", {
   
   documentContents <- .rs.heredoc('
      This is a Sweave document.
      
      <<>>=
      
      @
   ')
      
   remote$editor.executeWithContents(".Rnw", documentContents, function(editor) {
      editor$gotoLine(4L, 0L)
      remote$client$Input.insertText(text = "{ 1 + 1 }")
      Sys.sleep(1)  # wait for Ace to tokenize
      tokens <- as.vector(editor$session$getTokens(3L))
      values <- vapply(tokens, `[[`, "value", FUN.VALUE = character(1))
      expected <- c("{", " ", "1", " ", "+", " ", "1", " ", "}")
      expect_equal(values, expected)
   })
   
})

# https://github.com/rstudio/rstudio/issues/15574
.rs.test("Background chunk highlight in Sweave documents is correct", {
   
   contents <- .rs.heredoc('
      \\begin{document}
      
      This is some text.
      
      <<chunk>>=
      print(1 + 1)
      @
      
      This is some more text.
      
      \\end{document}
   ')
   
   remote$editor.executeWithContents(".Rnw", contents, function(editor) {
      
      markers <- as.vector(editor$session$getMarkers(0L))
      highlightMarkers <- Filter(function(marker) {
         grepl("background_highlight", marker$clazz)
      }, markers)
      
      expect_equal(length(highlightMarkers), 3L)
      expect_equal(highlightMarkers[[1L]]$range$start$row, 4L)
      expect_equal(highlightMarkers[[2L]]$range$start$row, 5L)
      expect_equal(highlightMarkers[[3L]]$range$start$row, 6L)
      
   })
   
})
