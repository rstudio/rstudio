
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())

# All other tests in this file are covered by
# e2e/rstudio/tests/panes/editor/edit_suggestions.test.ts. This one remains
# until the focus issue in the Playwright counterpart is resolved: typing
# into an editor that has an active ghost-text suggestion routes the first
# char to the editor and the rest to the console, so the Playwright test
# can't reproduce the user-typing case BRAT exercises here.

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
