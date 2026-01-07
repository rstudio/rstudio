library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())

remote$project.create("format")

remote$console.executeExpr({
   .rs.uiPrefs$codeFormatter$set("none")
   .rs.uiPrefs$reformatOnSave$set(FALSE)
})

.rs.test("https://github.com/rstudio/rstudio/issues/5425", {
   
   remote$editor.executeWithContents(".R", "c(1 #2\n)", function(editor) {
      editor$selectAll()
      remote$commands.execute(.rs.appCommands$reformatCode)
      Sys.sleep(1)
      contents <- editor$getValue()
      expect_equal(contents, "c(\n  1 #2\n)\n")
   })
   
})

.rs.test("styler: documents can be reformatted on save", {
   # skipping due to failure that needs to be investigated
   skip_on_ci()
   
   remote$console.executeExpr({
      .rs.uiPrefs$reformatOnSave$set(TRUE)
      .rs.uiPrefs$codeFormatter$set("styler")
   })

   name <- basename(tempfile("script-", fileext = ".R"))
   remote$editor.openDocument(name)
   
   editor <- remote$editor.getInstance()
   editor$insert("1+1; 2+2")
   remote$keyboard.insertText("<Ctrl + S>")
   contents <- editor$session$doc$getValue()
   expect_equal(contents, "1 + 1\n2 + 2\n")
   remote$editor.closeDocument()
   
   remote$console.executeExpr({
      .rs.uiPrefs$reformatOnSave$set(FALSE)
      .rs.uiPrefs$codeFormatter$set("none")
   })
})

.rs.test("air: documents can be reformatted on save", {
   # skipping due to failure that needs to be investigated
   skip_on_ci()
   
   remote$console.executeExpr({
      .rs.uiPrefs$reformatOnSave$set(TRUE)
      .rs.uiPrefs$codeFormatter$set("none")
      .rs.uiPrefs$useAirFormatter$set(TRUE)
   })
   
   airToml <- .rs.heredoc('
      [format]
      line-width = 80
      indent-width = 4
      indent-style = "space"
      line-ending = "auto"
      persistent-line-breaks = true
      exclude = []
      default-exclude = true
      skip = []
      table = []
      default-table = true
   ')
   
   remote$console.executeExpr({
      writeLines(!!airToml, con = "air.toml")
   })
   
   name <- basename(tempfile("script-", fileext = ".R"))
   remote$editor.openDocument(name)
   
   editor <- remote$editor.getInstance()
   editor$insert("example<-function(){1+1;2+2}")
   remote$keyboard.insertText("<Ctrl + S>")
   contents <- editor$session$doc$getValue()
   expect_equal(contents, "example <- function() {\n    1 + 1\n    2 + 2\n}\n")
   remote$editor.closeDocument()
   
   remote$console.executeExpr({
      .rs.uiPrefs$reformatOnSave$set(FALSE)
      .rs.uiPrefs$codeFormatter$set("none")
      .rs.uiPrefs$useAirFormatter$set(FALSE)
   })

})

remote$project.close()
