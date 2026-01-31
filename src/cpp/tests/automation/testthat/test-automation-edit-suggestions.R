
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())

.rs.test("ghost text edit suggestions can be prefix-matched", {
   
   remote$editor.executeWithContents(".R", "", function(editor) {
      
      # Insert an edit suggestion on the first line
      remote$console.executeExpr({
         .rs.api.showEditSuggestion(c(1, 1, 1, 1), "hello")
      })
      
      # Try inserting some prefix matches
      editor$focus()
      remote$keyboard.sendKeys("h", "e")
      expect_equal(editor$session$getLine(0L), "he")
      
   })
   
})

.rs.test("ghost text edit suggestions move on document edit", {
   
   remote$editor.executeWithContents(".R", "\n\n\n\n\n", function(editor) {
      
      # Insert an edit suggestion on line three
      remote$console.executeExpr({
         .rs.api.showEditSuggestion(c(3, 1, 3, 1), "Hello world!")
      })
      
      # Put the cursor on line 1
      editor$gotoLine(1L)
      
      # Insert two newlines
      editor$insert("\n")
      editor$insert("\n")
      
      # Check that ghost text is still visible on line 5
      tokens <- as.vector(editor$session$getTokens(4L))
      expect_equal(tokens[[1L]]$value, "Hello world!")
      
   })
   
})

.rs.test("edit suggestions are displayed inline when appropriate", {
   
   contents <- .rs.heredoc('
      # Create a 3D point.
      point <- function(x, y, z) {}
   ')
   
   remote$editor.executeWithContents(".R", contents, function(editor) {
      
      remote$console.executeExpr({
         .rs.api.showEditSuggestion(c(1, 12, 1, 14), "4D")
      })
      
      tokens <- as.vector(editor$session$getTokens(0L))
      expect_identical(tokens[[2L]]$type, "insertion_preview")
      
      remote$dom.clickElement(".ace_nes-gutter")
      
      tokens <- as.vector(editor$session$getTokens(0L))
      expect_equal(tokens[[1L]]$value, "# Create a 4D point.")
      
   })
   
})
