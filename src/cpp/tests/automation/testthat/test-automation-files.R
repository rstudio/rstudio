
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
