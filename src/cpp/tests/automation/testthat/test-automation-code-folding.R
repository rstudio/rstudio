
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())


# https://github.com/rstudio/rstudio/issues/16541
.rs.test("hierarchical section folding respects heading depth", {

   contents <- .rs.heredoc('
      # Section 1 ----
      code_1 <- 1
      ## Section 1.1 ----
      code_1_1 <- 2
      ## Section 1.2 ----
      code_1_2 <- 3
      # Section 2 ----
      code_2 <- 4
   ')

   # Ensure hierarchical folding is enabled (default).
   remote$console.executeExpr({
      .rs.uiPrefs$hierarchicalSectionFolding$set(TRUE)
   })

   withr::defer({
      remote$console.executeExpr({
         .rs.uiPrefs$hierarchicalSectionFolding$clear()
      })
   })

   remote$editor.executeWithContents(".R", contents, function(editor) {

      session <- editor$session

      # All section headers should be fold starts.
      # (rows are 0-indexed)
      expect_equal(session$getFoldWidget(0), "start")  # # Section 1 ----
      expect_equal(session$getFoldWidget(2), "start")  # ## Section 1.1 ----
      expect_equal(session$getFoldWidget(4), "start")  # ## Section 1.2 ----
      expect_equal(session$getFoldWidget(6), "start")  # # Section 2 ----

      # With hierarchical folding, '# Section 1 ----' should fold
      # all the way to the line before '# Section 2 ----' (row 5),
      # encompassing both ## subsections.
      range <- as.vector(session$getFoldWidgetRange(0))
      expect_equal(range$end$row, 5)

      # '## Section 1.1 ----' should fold up to the line before
      # '## Section 1.2 ----' (row 3).
      range <- as.vector(session$getFoldWidgetRange(2))
      expect_equal(range$end$row, 3)

      # '## Section 1.2 ----' should fold up to the line before
      # '# Section 2 ----' (row 5).
      range <- as.vector(session$getFoldWidgetRange(4))
      expect_equal(range$end$row, 5)

      # '# Section 2 ----' is the last section; should fold to
      # the end of the document (row 8).
      range <- as.vector(session$getFoldWidgetRange(6))
      expect_equal(range$end$row, 8)

   })

})

# https://github.com/rstudio/rstudio/issues/16541
.rs.test("flat section folding stops at any section header", {

   contents <- .rs.heredoc('
      # Section 1 ----
      code_1 <- 1
      ## Section 1.1 ----
      code_1_1 <- 2
      # Section 2 ----
      code_2 <- 3
   ')

   # Disable hierarchical folding to get the old flat behavior.
   remote$console.executeExpr({
      .rs.uiPrefs$hierarchicalSectionFolding$set(FALSE)
   })

   withr::defer({
      remote$console.executeExpr({
         .rs.uiPrefs$hierarchicalSectionFolding$clear()
      })
   })

   remote$editor.executeWithContents(".R", contents, function(editor) {

      session <- editor$session

      # With flat folding, '# Section 1 ----' should fold only to
      # the line before the next section header '## Section 1.1 ----'
      # (row 1), not to '# Section 2 ----'.
      range <- as.vector(session$getFoldWidgetRange(0))
      expect_equal(range$end$row, 1)

      # '## Section 1.1 ----' folds up to the line before '# Section 2 ----'
      # (row 3).
      range <- as.vector(session$getFoldWidgetRange(2))
      expect_equal(range$end$row, 3)

   })

})
