
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())


.rs.test("the virtualized open file web dialog works", {
   skip_if(.rs.isDesktop())
   
   # work in temporary directory
   remote$console.executeExpr({
      dir <- tempfile("rstudio-automation-")
      dir.create(dir)
      setwd(dir)
   })
   
   # create a bunch of files
   remote$console.executeExpr({
      files <- sprintf("%04i.R", 0:9999)
      invisible(file.create(files))
   })
   
   # show the Open File dialog
   remote$commands.execute(.rs.appCommands$openSourceDoc)
   
   # wait until the modal dialog is shown
   remote$dom.waitForElement(".rstudio_modal_dialog")
   Sys.sleep(1)
   
   # try typing some keys to select a file
   remote$keyboard.sendKeys("5", "1", "5", "9", "<Enter>")
   
   # check that we opened the file we tried to
   tabPanelEl <- remote$js.querySelector("div[aria-label=\"Documents\"]")
   contents <- tabPanelEl$innerText
   expect_equal(.rs.trimWhitespace(contents), "5159.R")
   
   # clean up
   remote$console.executeExpr({
      files <- sprintf("%04i.R", 0:9999)
      unlink(files)
   })
   
})

.rs.test("the virtualized open file web dialog works with <500 files", {
   
   skip_if(.rs.isDesktop())
   
   # work in temporary directory
   remote$console.executeExpr({
      dir <- tempfile("rstudio-automation-")
      dir.create(dir)
      setwd(dir)
   })
   
   # create a bunch of files
   remote$console.executeExpr({
      files <- sprintf("%03i.R", 0:450)
      invisible(file.create(files))
   })
   
   # show the Open File dialog
   remote$commands.execute(.rs.appCommands$openSourceDoc)
   
   # wait until the modal dialog is shown
   remote$dom.waitForElement(".rstudio_modal_dialog")
   Sys.sleep(1)
   
   # try typing some keys to select a file
   remote$keyboard.sendKeys("4", "5", "5", "<Enter>")
   
   # check that we opened the file we tried to
   tabPanelEl <- remote$js.querySelector("div[aria-label=\"Documents\"]")
   contents <- tabPanelEl$innerText
   expect_equal(.rs.trimWhitespace(contents), "450.R")
   
   # clean up
   remote$console.executeExpr({
      unlink(files)
   })
})

# https://github.com/rstudio/rstudio/issues/16329
.rs.test("we don't autosave unchanged documents", {
   
   # make sure we've enabled autosave
   remote$console.executeExpr({
      .rs.uiPrefs$autoSaveOnBlur$set(TRUE)
   })
   
   # create a document on disk
   remote$console.executeExpr({
      con <- tempfile(fileext = ".R")
      writeLines("# hello world", con = con)
      file.edit(con)
   })
   
   # wait for file to open
   Sys.sleep(1)
   
   # get file mtime
   remote$console.executeExpr({
      old <- file.info(con)$mtime
      print(old)
   })
   
   # send focus to the console, source pane, and back again
   remote$commands.execute(.rs.appCommands$activateConsole)
   remote$commands.execute(.rs.appCommands$activateSource)
   remote$commands.execute(.rs.appCommands$activateConsole)
   
   remote$console.executeExpr({
      new <- file.info(con)$mtime
      print(new)
   })
   
   remote$console.executeExpr(old == new)
   output <- remote$console.getOutput(n = 1L)
   expect_equal(output, "[1] TRUE")
   
   # now try editing the document, changing focus, and then
   # checking that the file was properly autosaved
   remote$commands.execute(.rs.appCommands$activateSource)
   editor <- remote$editor.getInstance()
   editor$gotoLine(2L)
   editor$insert("# this is some text")
   remote$commands.execute(.rs.appCommands$activateConsole)
   
   # check that the file was autosaved and the mtime has changed
   remote$console.executeExpr({
      new <- file.info(con)$mtime
      print(new)
   })
   
   remote$console.executeExpr({
      old == new
   })
   
   output <- remote$console.getOutput(1L)
   expect_equal(output, "[1] FALSE")
})
