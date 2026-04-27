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
   
   skip_on_ci()
   if (!remote$package.isInstalled("styler"))
      skip("styler is not installed")
   
   remote$console.executeExpr({
      .rs.uiPrefs$reformatOnSave$set(TRUE)
      .rs.uiPrefs$codeFormatter$set("styler")
   })

   name <- basename(tempfile("script-", fileext = ".R"))
   remote$editor.openDocument(name)
   
   editor <- remote$editor.getInstance()
   editor$insert("1+1; 2+2")
   remote$keyboard.insertText("<Ctrl + S>")
   contents <- .rs.waitUntil("document is reformatted", function() {
      contents <- editor$session$doc$getValue()
      if (contents != "1+1; 2+2")
         return(contents)
      return(FALSE)
   })
   expect_equal(contents, "1 + 1\n2 + 2\n")
   remote$editor.closeDocument()
   
   remote$console.executeExpr({
      .rs.uiPrefs$reformatOnSave$set(FALSE)
      .rs.uiPrefs$codeFormatter$set("none")
   })
})

.rs.test("air: documents can be reformatted on save", {
   
   # make sure 'air' is installed
   remote$console.executeExpr({
      writeLines(Sys.which("air"))
   })
   
   output <- remote$console.getOutput(1L)
   if (!nzchar(output))
      skip("air is not installed")
   
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
   contents <- .rs.waitUntil("document is reformatted", function() {
      contents <- editor$session$doc$getValue()
      if (contents != "example<-function(){1+1;2+2}")
         return(contents)
      return(FALSE)
   })
   expect_equal(contents, "example <- function() {\n    1 + 1\n    2 + 2\n}\n")
   remote$editor.closeDocument()
   
   remote$console.executeExpr({
      .rs.uiPrefs$reformatOnSave$set(FALSE)
      .rs.uiPrefs$codeFormatter$set("none")
      .rs.uiPrefs$useAirFormatter$set(FALSE)
   })

})

# https://github.com/rstudio/rstudio/issues/16721
.rs.test("air: format on save does not use air when useAirFormatter is false", {

   # make sure 'air' is installed
   remote$console.executeExpr({
      writeLines(Sys.which("air"))
   })

   output <- remote$console.getOutput(1L)
   if (!nzchar(output))
      skip("air is not installed")

   # enable reformat on save, but leave useAirFormatter disabled
   remote$console.executeExpr({
      .rs.uiPrefs$reformatOnSave$set(TRUE)
      .rs.uiPrefs$codeFormatter$set("none")
      .rs.uiPrefs$useAirFormatter$set(FALSE)
   })

   # write an air.toml so Air would have config if it ran
   airToml <- .rs.heredoc('
      [format]
      line-width = 80
      indent-width = 4
   ')

   remote$console.executeExpr({
      writeLines(!!airToml, con = "air.toml")
   })

   name <- basename(tempfile("script-", fileext = ".R"))
   remote$editor.openDocument(name)

   editor <- remote$editor.getInstance()
   editor$insert("1+1;2+2")
   remote$keyboard.insertText("<Ctrl + S>")
   Sys.sleep(2)

   # verify the code was NOT reformatted by Air
   # (Air would reformat "1+1;2+2" to "1 + 1\n2 + 2\n")
   contents <- editor$getValue()
   expect_true(startsWith(contents, "1+1;2+2"))
   remote$editor.closeDocument()

   remote$console.executeExpr({
      .rs.uiPrefs$reformatOnSave$set(FALSE)
      .rs.uiPrefs$codeFormatter$set("none")
   })

})

remote$project.close()
