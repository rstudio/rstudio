
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
      
      # Try accepting the completion
      remote$keyboard.executeShortcut("Tab")
      expect_equal(editor$session$getLine(0L), "hello")
      
   })
   
})

.rs.test("ghost text edit suggestions survive document mutations", {
   
   remote$editor.executeWithContents(".R", "# abc def", function(editor) {
      
      # Insert an edit suggestion on the first line
      remote$console.executeExpr({
         .rs.api.showEditSuggestion(c(1, 3, 1, 6), "ABC")
      })
      
      # Try inserting some prefix matches
      editor$focus()
      remote$keyboard.sendKeys("Right")
      remote$keyboard.sendKeys("1")
      remote$keyboard.sendKeys("2")
      remote$keyboard.sendKeys("3")
      expect_equal(editor$session$getLine(0L), "#123 abc def")
      
      # Try accepting the edit suggestion (should still exist)
      remote$dom.clickElement(".ace_nes-gutter")
      expect_equal(editor$session$getLine(0L), "#123 ABC def")
      
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

.rs.test("ghost text is cleared from old row when newline inserted above", {

   remote$editor.executeWithContents(".R", "\n\n\n\n\n", function(editor) {

      # Insert an edit suggestion on line three
      remote$console.executeExpr({
         .rs.api.showEditSuggestion(c(3, 1, 3, 1), "Hello world!")
      })

      # Verify ghost text is on line 3
      tokens <- as.vector(editor$session$getTokens(2L))
      expect_equal(tokens[[1L]]$value, "Hello world!")

      # Put the cursor on line 1 and insert a newline
      editor$gotoLine(1L)
      editor$insert("\n")

      # Check that ghost text moved to line 4
      tokens <- as.vector(editor$session$getTokens(3L))
      expect_equal(tokens[[1L]]$value, "Hello world!")

      # Check that old line 3 no longer has the ghost text
      tokens <- as.vector(editor$session$getTokens(2L))
      hasSynthetic <- any(vapply(tokens, function(t) isTRUE(t$synthetic), logical(1)))
      expect_false(hasSynthetic)

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
