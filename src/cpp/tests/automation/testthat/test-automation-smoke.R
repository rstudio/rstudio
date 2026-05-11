
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())

# Trivial smoke test for iterating on the automation harness itself
# (e.g., output capture, exit-code wiring). Keep the body minimal so
# Chrome launch dominates the wall time; this file exists so we don't
# have to run the full debugger suite to validate harness changes.
.rs.test("automation harness smoke test", {
   expect_true(TRUE)
})
