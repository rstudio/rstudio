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

# https://github.com/rstudio/rstudio/issues/17471
# Bug is Windows-only: styler's writeLines writes CRLF on Windows, which
# the formatter callers then read back. Without LineEndingPosix normalization
# the character-level diff against the LF-normalized in-memory document
# produces \r insertions before each \n; on the client, Ace's Document.$split
# regex turns each inserted bare \r into another \n, doubling line breaks.
# On macOS/Linux styler writes LF and the bug doesn't manifest.
.rs.test("styler: Reformat Document does not add extra newlines on Windows", {

   skip_on_ci()
   skip_if(!.rs.platform.isWindows, "styler only writes CRLF on Windows")
   if (!remote$package.isInstalled("styler"))
      skip("styler is not installed")

   remote$console.executeExpr({
      .rs.uiPrefs$codeFormatter$set("styler")
   })
   withr::defer(remote$console.executeExpr({
      .rs.uiPrefs$codeFormatter$set("none")
   }))

   initial <- "print(\"test\")\nprint(\"test\")\n\n"
   expected <- "print(\"test\")\nprint(\"test\")\n"
   remote$editor.executeWithContents(".R", initial, function(editor) {
      remote$commands.execute(.rs.appCommands$reformatDocument)
      contents <- .rs.waitUntil("document is reformatted", function() {
         contents <- editor$getValue()
         if (contents != initial)
            return(contents)
         return(FALSE)
      })
      expect_equal(contents, expected)
   })

})

# Regression test for the selection-reformat (formatCode) code path of #17471.
# The full-document path is covered above; this also guards against stray \r
# being left in selection-reformat output if line-ending normalization is
# later dropped from the C++ formatCode handler.
.rs.test("styler: Reformat Code (selection) does not add extra newlines on Windows", {

   skip_on_ci()
   skip_if(!.rs.platform.isWindows, "styler only writes CRLF on Windows")
   if (!remote$package.isInstalled("styler"))
      skip("styler is not installed")

   remote$console.executeExpr({
      .rs.uiPrefs$codeFormatter$set("styler")
   })
   withr::defer(remote$console.executeExpr({
      .rs.uiPrefs$codeFormatter$set("none")
   }))

   initial <- "1+1; 2+2\n3+3; 4+4\n"
   expected <- "1 + 1\n2 + 2\n3 + 3\n4 + 4\n"
   remote$editor.executeWithContents(".R", initial, function(editor) {
      editor$selectAll()
      remote$commands.execute(.rs.appCommands$reformatCode)
      contents <- .rs.waitUntil("selection is reformatted", function() {
         contents <- editor$getValue()
         if (contents != initial)
            return(contents)
         return(FALSE)
      })
      expect_equal(contents, expected)
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
