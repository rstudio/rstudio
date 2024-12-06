
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())


# https://github.com/rstudio/rstudio/pull/14657
.rs.test("we can use the data viewer with temporary R expressions", {
   remote$console.execute("View(subset(mtcars, mpg >= 30))")
   viewerFrame <- remote$js.querySelector("#rstudio_data_viewer_frame")
   expect_true(grepl("gridviewer.html", viewerFrame$src))
   remote$commands.execute("closeSourceDoc")
})

.rs.test("viewer filters function as expected", {
   
   # Start viewing a data.frame with a list column.
   remote$console.executeExpr({
      data <- data.frame(x = letters)
      data$y <- lapply(letters, as.list)
      row.names(data) <- LETTERS
      View(data)
   })
   
   # Filter to entries with a 'K'.
   remote$dom.clickElement("#data_editing_toolbar .search")
   remote$keyboard.insertText("K", "<Space>")
   
   # Try to find the viewer link in the table.
   # Confirm it has the expected href.
   viewerFrame <- remote$js.querySelector("#rstudio_data_viewer_frame")
   linkEl <- viewerFrame$contentWindow$document$querySelector(".viewerLink")
   expect_equal(linkEl$href, "javascript:window.listViewerCallback(\"K\", 2)")
   
   # Try to click it.
   linkEl$focus()
   linkEl$click()
   
   # Confirm that a new explorer tab was opened.
   currentTabEl <- remote$js.querySelector(".rstudio_source_panel .gwt-TabLayoutPanelTab-selected")
   tabTitle <- .rs.trimWhitespace(currentTabEl$innerText)
   expect_equal(tabTitle, "data[\"K\", 2]")
   
   # Close any open documents.
   remote$keyboard.insertText("<Ctrl + W>", "<Ctrl + W>", "<Ctrl + L>")
   
})
