
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())

.rs.test("https://github.com/rstudio/rstudio/issues/5425", {
   
   remote$editor.executeWithContents(".R", "c(1 #2\n)", function(editor) {
      editor$selectAll()
      remote$commands.execute(.rs.appCommands$reformatCode)
      Sys.sleep(1)
      contents <- editor$getValue()
      expect_equal(contents, "c(\n  1 #2\n)\n")
   })
   
})

.rs.test("Documents can be reformatted on save", {
   
   remote$console.executeExpr({
      .rs.uiPrefs$reformatOnSave$set(TRUE)
      .rs.uiPrefs$codeFormatter$set("styler")
   })

   documentContents <- .rs.heredoc("2+2")
   
   remote$editor.openWithContents(".R", documentContents)
   editor <- remote$editor.getInstance()
   editor$insert("1+1; ")
   remote$keyboard.insertText("<Ctrl + S>")
   Sys.sleep(1)
   contents <- editor$session$doc$getValue()
   expect_equal(contents, "1 + 1\n2 + 2\n")
   
   remote$console.executeExpr({
      .rs.uiPrefs$reformatOnSave$clear()
      .rs.uiPrefs$codeFormatter$clear()
   })
   
})
