
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())

# All 27 path-based guardrail tests (read denials, write denials,
# connection denials, system-file denials, error-message structure, and
# path traversal) were ported to Playwright at
# e2e/rstudio/tests/panes/posit-assistant-chat/chat-guardrails-paths.test.ts.
# Only the four binding-lifecycle tests below remain in BRAT --
# `.rs.chat.safeEval` returns errors as conditions rather than printing
# them to the console, so the Playwright "check console output for
# blocked" pattern doesn't catch them. These tests verify the bindings
# stay clean across reentrant injection and across errored evaluations,
# which is checked indirectly by writing a normal file afterwards and
# confirming no guardrail error is raised.

# -- Binding lifecycle --------------------------------------------------------

.rs.test("bindings are restored after safeEval",
{
   remote$console.clear()

   # Run safeEval, which should inject and then restore bindings
   remote$console.executeExpr({
      .rs.chat.safeEval(quote(1 + 1))
   })

   # After safeEval, bindings should be restored; writeLines should
   # work normally (no guardrail error)
   remote$console.clear()
   remote$console.executeExpr({
      path <- file.path(tempdir(), "guardrail-lifecycle.txt")
      writeLines("test", path)
      unlink(path)
   })

   output <- paste(remote$console.getOutput(), collapse = "\n")
   expect_no_match(output, "blocked")
})

.rs.test("safeEval blocks writes outside project directory",
{
   path <- file.path(dirname(tempdir()), "not-in-project.txt")

   remote$console.clear()
   remote$console.executeExpr({
      result <- .rs.chat.safeEval(quote(
         writeLines("x", !!path)
      ))
   })

   # safeEval returns errors as conditions, not console errors,
   # so check that no file was created
   remote$console.clear()
   remote$console.executeExpr({
      cat(file.exists(!!path))
   })
   output <- paste(remote$console.getOutput(), collapse = "\n")
   expect_match(output, "FALSE")
})

.rs.test("double injection is safe (reentrancy guard)",
{
   remote$console.clear()

   # Call injectBindings twice; the second call should be a no-op.
   # Then restore once -- bindings should be cleanly restored.
   remote$console.executeExpr({
      .rs.chat.injectBindings()
      .rs.chat.injectBindings()
      .rs.chat.restoreBindings()
   })

   # After restore, writeLines should work normally outside tempdir
   remote$console.clear()
   remote$console.executeExpr({
      path <- file.path(tempdir(), "guardrail-reentrant.txt")
      writeLines("test", path)
      unlink(path)
   })

   output <- paste(remote$console.getOutput(), collapse = "\n")
   expect_no_match(output, "blocked")
   expect_no_match(output, "not within")
})

.rs.test("safeEval restores bindings when user code errors",
{
   remote$console.clear()

   # safeEval should restore bindings even when the evaluated code errors
   remote$console.executeExpr({
      .rs.chat.safeEval(quote(stop("user error")))
   })

   # After safeEval, bindings should be restored
   remote$console.clear()
   remote$console.executeExpr({
      path <- file.path(tempdir(), "guardrail-error-restore.txt")
      writeLines("test", path)
      unlink(path)
   })

   output <- paste(remote$console.getOutput(), collapse = "\n")
   expect_no_match(output, "blocked")
   expect_no_match(output, "not within")
})
