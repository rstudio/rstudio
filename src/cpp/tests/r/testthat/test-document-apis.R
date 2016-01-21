context("Document APIs")

test_that("insertText() handles various invocations", {
   
   # Replace current selection
   .rs.api.insertText("foo")
   .rs.api.insertText(text = "foo")
   
   # Insert text at raw position (comment lines 3:5)
   .rs.api.insertText(Map(c, 3:5, 1), "#")
   
   # Insert text at raw ranges (uncomment lines 3:5)
   ranges <- Map(c, Map(c, 3:5, 1), Map(c, 3:5, 2))
   .rs.api.insertText(ranges, "")
   
   # Infinity is accepted
   .rs.api.insertText(c(Inf, 1), "# Hello\n")
   .rs.api.insertText(Inf, "# Hello\n")
   
})
