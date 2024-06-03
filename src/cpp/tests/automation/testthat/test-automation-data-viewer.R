
library(testthat)

self <- remote <- .rs.automation.newRemote()
on.exit(.rs.automation.deleteRemote(), add = TRUE)

# https://github.com/rstudio/rstudio/pull/14657
test_that("we can use the data viewer with temporary R expressions", {
   remote$consoleExecute("View(subset(mtcars, mpg >= 30))")
   objectId <- remote$domGetObjectId("iframe[class=\"gwt-Frame\"]")
   viewerSrc <- remote$jsCall("function() { return this.src; }", objectId = objectId)
   expect_true(grepl("gridviewer.html", viewerSrc$result$value))
   remote$commandExecute("closeSourceDoc")
})
