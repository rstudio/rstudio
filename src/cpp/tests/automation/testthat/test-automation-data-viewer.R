
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

# Cell text and column names ride td.textContent, so the browser handles
# entity encoding -- we only assert the observable security property
# (HTML-special chars never escape into the DOM as markup) instead of
# pinning down which entity forms come back from innerHTML, since
# textContent only encodes <, >, and & and leaves quotes as plain text.
.rs.test("data viewer renders HTML-special cell values as text, not markup", {
   remote$console.executeExpr({
      .rs.escape_test_df <- data.frame(
         a = c("<script>x</script>", "tom & jerry", "\"quoted\"", "it's"),
         stringsAsFactors = FALSE
      )
      View(.rs.escape_test_df)
   })
   doc <- viewerDoc()

   # Nothing user-supplied should have escaped into a real DOM element.
   expect_equal(doc$querySelectorAll("#gridBody script")$length, 0)

   # Round-trip via textContent: the visible text matches the original
   # values exactly, including the quote characters that textContent
   # leaves alone.
   tds <- doc$querySelectorAll("#gridBody .textCell")
   bodyText <- ""
   for (i in seq_len(tds$length)) {
      bodyText <- paste0(bodyText, tds[[i - 1L]]$textContent, "\n")
   }
   expect_match(bodyText, "<script>x</script>", fixed = TRUE)
   expect_match(bodyText, "tom & jerry", fixed = TRUE)
   expect_match(bodyText, "\"quoted\"", fixed = TRUE)
   expect_match(bodyText, "it's", fixed = TRUE)

   # Spot-check innerHTML for the chars textContent does encode, so a
   # regression to raw <, >, & in the rendered DOM is still caught.
   bodyHtml <- ""
   for (i in seq_len(tds$length)) {
      bodyHtml <- paste0(bodyHtml, tds[[i - 1L]]$innerHTML, "\n")
   }
   expect_match(bodyHtml, "&lt;script&gt;", fixed = TRUE)
   expect_match(bodyHtml, "tom &amp; jerry", fixed = TRUE)

   remote$commands.execute("closeSourceDoc")
   remote$console.executeExpr({
      rm(".rs.escape_test_df", envir = .GlobalEnv)
   })
})

.rs.test("data viewer renders HTML-special column names as text, not markup", {
   remote$console.executeExpr({
      .rs.escape_hdr_df <- data.frame(x = 1, check.names = FALSE)
      names(.rs.escape_hdr_df) <- "<b>&\"'"
      View(.rs.escape_hdr_df)
   })
   doc <- viewerDoc()

   th <- doc$querySelector('th[data-col-idx="1"]')

   # No <b> element should have appeared in the header from the column
   # name alone -- if escaping regressed, this query would find one.
   expect_equal(th$querySelectorAll("b")$length, 0)

   # The visible header text matches the original column name verbatim.
   expect_match(th$textContent, "<b>&\"'", fixed = TRUE)

   # innerHTML still encodes <, >, and & -- guard against a regression
   # that would set innerHTML directly from raw col_name.
   html <- th$innerHTML
   expect_match(html, "&lt;b&gt;", fixed = TRUE)
   expect_match(html, "&amp;", fixed = TRUE)
   expect_false(grepl("<b>", html, fixed = TRUE))

   remote$commands.execute("closeSourceDoc")
   remote$console.executeExpr({
      rm(".rs.escape_hdr_df", envir = .GlobalEnv)
   })
})
