
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

   # Try to find the viewer link in the table. The link's onclick semantics
   # (rather than a specific href) are what matter; click and verify the
   # explorer opens for the right cell below.
   viewerFrame <- remote$js.querySelector("#rstudio_data_viewer_frame")
   linkEl <- viewerFrame$contentWindow$document$querySelector(".viewerLink")
   expect_false(is.null(linkEl))

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

# Helper: returns the iframe document handle for the active data viewer.
# js.querySelector blocks until the iframe and the requested header exist,
# so this also serves as a "wait for the data viewer to render" sync point.
viewerDoc <- function() {
   frame <- remote$js.querySelector("#rstudio_data_viewer_frame")
   doc <- frame$contentWindow$document
   # Force a wait on the first non-rownames header so callers can assume
   # the grid has rendered before they introspect it.
   doc$querySelector('th[data-col-idx="1"]')
   doc
}

.rs.test("data viewer sort headers cycle asc / desc / unsorted", {
   remote$console.execute("View(mtcars)")
   doc <- viewerDoc()

   classOf <- function() {
      doc$querySelector('th[data-col-idx="1"]')$className
   }
   click <- function() {
      doc$querySelector('th[data-col-idx="1"]')$click()
   }

   # Initial state: just "sorting" (unsorted).
   expect_match(classOf(), "(?:^|\\s)sorting(?:\\s|$)", perl = TRUE)

   # First click: ascending.
   click()
   .rs.waitUntil("sorted ascending", function() {
      grepl("sorting_asc", classOf())
   })

   # Second click: descending.
   click()
   .rs.waitUntil("sorted descending", function() {
      grepl("sorting_desc", classOf())
   })

   # Third click: back to unsorted; the asc/desc modifier is gone but the
   # base "sorting" class remains.
   click()
   .rs.waitUntil("unsorted", function() {
      cls <- classOf()
      grepl("sorting", cls) && !grepl("sorting_asc|sorting_desc", cls)
   })

   remote$commands.execute("closeSourceDoc")
})

.rs.test("data viewer pin icon moves a column to the pinned prefix", {
   remote$console.execute("View(mtcars)")
   doc <- viewerDoc()

   colOrder <- function() {
      ths <- doc$querySelectorAll("#data_cols th")
      vapply(seq_len(ths$length), function(i) {
         ths[[i - 1L]]$getAttribute("data-col-idx")
      }, character(1))
   }

   # Initial: rownames (0), then mtcars columns 1..10 in order.
   expect_equal(colOrder()[1:4], c("0", "1", "2", "3"))

   # Pin column 3 (disp). The pin icon has opacity:0 by default; CDP
   # clicks dispatch regardless of visibility.
   doc$querySelector('th[data-col-idx="3"] .pin-icon')$click()
   .rs.waitUntil("column 3 marked pinned", function() {
      grepl("\\bpinned\\b", doc$querySelector('th[data-col-idx="3"]')$className)
   })

   # disp now sits between rownames and the rest of the unpinned columns.
   expect_equal(colOrder()[1:3], c("0", "3", "1"))

   # Toggling the pin again restores the original order.
   doc$querySelector('th[data-col-idx="3"] .pin-icon')$click()
   .rs.waitUntil("column 3 unpinned", function() {
      !grepl("\\bpinned\\b", doc$querySelector('th[data-col-idx="3"]')$className)
   })
   expect_equal(colOrder()[1:4], c("0", "1", "2", "3"))

   remote$commands.execute("closeSourceDoc")
})

.rs.test("data viewer per-object state survives a refresh", {
   # Use a uniquely-named object so its localStorage state can't be
   # contaminated by a previous test that happened to View(mtcars).
   remote$console.executeExpr({
      .rs.persist_test_df <- mtcars
      View(.rs.persist_test_df)
   })
   doc <- viewerDoc()

   # Sort mpg (col 1) descending.
   doc$querySelector('th[data-col-idx="1"]')$click()
   .rs.waitUntil("ascending", function() {
      grepl("sorting_asc", doc$querySelector('th[data-col-idx="1"]')$className)
   })
   doc$querySelector('th[data-col-idx="1"]')$click()
   .rs.waitUntil("descending", function() {
      grepl("sorting_desc", doc$querySelector('th[data-col-idx="1"]')$className)
   })

   # Pin disp (col 3).
   doc$querySelector('th[data-col-idx="3"] .pin-icon')$click()
   .rs.waitUntil("pinned", function() {
      grepl("\\bpinned\\b", doc$querySelector('th[data-col-idx="3"]')$className)
   })

   # Trigger a refresh -- bootstrap clears the DOM, re-fetches, and
   # re-applies persisted state from localStorage. Pin set, sort, and
   # column widths should all come back.
   frame <- remote$js.querySelector("#rstudio_data_viewer_frame")
   frame$contentWindow$refreshData()

   .rs.waitUntil("state restored after refresh", function() {
      th1 <- doc$querySelector('th[data-col-idx="1"]')
      th3 <- doc$querySelector('th[data-col-idx="3"]')
      grepl("sorting_desc", th1$className) &&
         grepl("\\bpinned\\b", th3$className)
   })

   # Cleanup. Closing the tab fires onDismiss which clears the saved
   # state, so subsequent tests don't observe leftover pinning/sort.
   remote$commands.execute("closeSourceDoc")
   remote$console.executeExpr({
      rm(".rs.persist_test_df", envir = .GlobalEnv)
   })
})
