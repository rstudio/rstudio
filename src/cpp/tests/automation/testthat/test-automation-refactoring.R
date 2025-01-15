
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())

# https://github.com/rstudio/rstudio/issues/4961
.rs.test("rename in scope operates across chunks", {
   
   contents <- .rs.heredoc('
      ---
      title: Refactoring
      ---

      ```{r}
      # These should all get selected.
      variable <- 42
      print(variable + variable)
      ```

      ```{r}
      # These should also get selected.
      print(variable + variable)
      ```
      
      ```{r}
      # These should be ignored.
      example <- function(variable) {
         variable
      }
      ```
   ')
   
   remote$editor.executeWithContents(".Rmd", contents, function(editor) {
      
      editor$gotoLine(7, 0)
      remote$commands.execute("renameInScope")
      
      allRanges <- as.vector(editor$selection$rangeList$ranges)
      expect_length(allRanges, 5L)
      
      expectedRanges <- list(
         list(
            start = list(row = 6L, column = 0L),
            end = list(row = 6L, column = 8L)
         ),
         list(
            start = list(row = 7L, column = 6L),
            end = list(row = 7L, column = 14L)
         ),
         list(
            start = list(row = 7L, column = 17L),
            end = list(row = 7L, column = 25L)
         ),
         list(
            start = list(row = 12L, column = 6L),
            end = list(row = 12L, column = 14L)
         ),
         list(
            start = list(row = 12L, column = 17L),
            end = list(row = 12L, column = 25L)
         )
      )
      
      for (i in seq_along(allRanges))
      {
         actual <- allRanges[[i]][c("start", "end")]
         expected <- expectedRanges[[i]]
         expect_equal(!!actual, !!expected)
      }
      
      remote$keyboard.insertText("<Enter>")
      
   })
})
