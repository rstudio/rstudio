
library(testthat)

self <- remote <- .rs.automation.newRemote()
on.exit(.rs.automation.deleteRemote(), add = TRUE)

# https://github.com/rstudio/rstudio/pull/14657
test_that("we can use the data viewer with temporary R expressions", {
   remote$consoleExecute("View(subset(mtcars, mpg >= 30))")
   viewerFrame <- remote$jsObjectViaSelector("iframe[class=\"gwt-Frame\"]")
   expect_true(grepl("gridviewer.html", viewerFrame$src))
   remote$commandExecute("closeSourceDoc")
})
