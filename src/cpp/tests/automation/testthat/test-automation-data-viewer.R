
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())


# https://github.com/rstudio/rstudio/pull/14657
test_that("we can use the data viewer with temporary R expressions", {
   remote$consoleExecute("View(subset(mtcars, mpg >= 30))")
   viewerFrame <- remote$jsObjectViaSelector("#rstudio_data_viewer_frame")
   expect_true(grepl("gridviewer.html", viewerFrame$src))
   remote$commandExecute("closeSourceDoc")
})
