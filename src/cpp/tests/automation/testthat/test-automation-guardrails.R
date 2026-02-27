
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())

# Helper: inject guardrail bindings, execute code in the console,
# and verify that the output contains the expected error text.
.rs.guardrails.expectError <- function(code, pattern = "denied") {
   remote$console.executeExpr({ .rs.chat.injectBindings() })
   withr::defer(remote$console.executeExpr({ .rs.chat.restoreBindings() }))
   remote$console.clear()
   remote$console.execute(code)
   output <- paste(remote$console.getOutput(), collapse = "\n")
   expect_match(output, pattern)
}

# Helper: inject guardrail bindings, execute code in the console,
# and verify that the output does NOT contain an error.
.rs.guardrails.expectSuccess <- function(code) {
   remote$console.executeExpr({ .rs.chat.injectBindings() })
   withr::defer(remote$console.executeExpr({ .rs.chat.restoreBindings() }))
   remote$console.clear()
   remote$console.execute(code)
   output <- paste(remote$console.getOutput(), collapse = "\n")
   expect_no_match(output, "denied")
   expect_no_match(output, "Error")
}


# -- Read guardrails ----------------------------------------------------------

.rs.test("reading ~/.aws/credentials is denied", {
   .rs.guardrails.expectError('readLines("~/.aws/credentials")')
})

.rs.test("reading ~/.ssh/config is denied", {
   .rs.guardrails.expectError('readLines("~/.ssh/config")')
})

.rs.test("reading ~/.netrc is denied", {
   .rs.guardrails.expectError('readLines("~/.netrc")')
})

.rs.test("reading .env files is denied", {
   dir <- tempdir()
   envFile <- file.path(dir, ".env")
   writeLines("SECRET=abc", envFile)
   withr::defer(unlink(envFile))
   .rs.guardrails.expectError(sprintf('readLines("%s")', envFile))
})

.rs.test("reading .Renviron files is denied", {
   dir <- tempdir()
   envFile <- file.path(dir, ".Renviron")
   writeLines("SECRET=abc", envFile)
   withr::defer(unlink(envFile))
   .rs.guardrails.expectError(sprintf('readLines("%s")', envFile))
})

.rs.test("reading SSH private keys is denied", {
   .rs.guardrails.expectError('readLines("~/.ssh/id_rsa")')
})

.rs.test("reading SSH public keys is allowed", {
   .rs.guardrails.expectSuccess('readLines("~/.ssh/id_rsa.pub")')
})

.rs.test("reading normal files is allowed", {
   remote$console.executeExpr({
      writeLines("hello", file.path(tempdir(), "readable.txt"))
   })
   .rs.guardrails.expectSuccess(
      sprintf('readLines("%s")', file.path(tempdir(), "readable.txt"))
   )
})


# -- Write guardrails ---------------------------------------------------------

.rs.test("writing outside project directory is denied", {
   .rs.guardrails.expectError('writeLines("x", "/tmp/not-in-project.txt")')
})

.rs.test("writing to ~/.ssh is denied", {
   .rs.guardrails.expectError('writeLines("x", "~/.ssh/test")')
})

.rs.test("writing to tempdir is allowed", {
   path <- file.path(tempdir(), "guardrail-test.txt")
   .rs.guardrails.expectSuccess(sprintf('writeLines("hello", "%s")', path))
   withr::defer(remote$console.executeExpr(unlink(!!path)))
})

.rs.test("file.create outside project directory is denied", {
   .rs.guardrails.expectError('file.create("/tmp/not-in-project.txt")')
})

.rs.test("file.remove outside project directory is denied", {
   .rs.guardrails.expectError('file.remove("/tmp/not-in-project.txt")')
})

.rs.test("unlink outside project directory is denied", {
   .rs.guardrails.expectError('unlink("/tmp/not-in-project.txt")')
})


# -- Connection guardrails ----------------------------------------------------

.rs.test("file() connection to sensitive path is denied", {
   .rs.guardrails.expectError('file("~/.aws/credentials", open = "r")')
})

.rs.test("file() with deferred open to sensitive path is denied", {
   .rs.guardrails.expectError('file("~/.ssh/test")')
})


# -- Path traversal -----------------------------------------------------------

.rs.test("path traversal with '..' is rejected", {
   .rs.guardrails.expectError(
      'writeLines("x", file.path(tempdir(), "sub/../../etc/passwd"))',
      pattern = "unresolved"
   )
})


# -- Binding lifecycle --------------------------------------------------------

.rs.test("bindings are restored after safeEval", {

   remote$console.clear()

   # Run safeEval, which should inject and then restore bindings
   remote$console.executeExpr({
      .rs.chat.safeEval(quote(1 + 1))
   })

   # After safeEval, bindings should be restored; writeLines to /tmp
   # should work normally (no guardrail error)
   remote$console.clear()
   remote$console.executeExpr({
      path <- file.path(tempdir(), "post-restore.txt")
      writeLines("test", path)
      unlink(path)
   })

   output <- paste(remote$console.getOutput(), collapse = "\n")
   expect_no_match(output, "denied")

})

.rs.test("safeEval blocks writes outside project directory", {

   remote$console.clear()
   remote$console.executeExpr({
      result <- .rs.chat.safeEval(quote(
         writeLines("x", "/tmp/not-in-project.txt")
      ))
   })

   output <- paste(remote$console.getOutput(), collapse = "\n")
   # safeEval returns errors as conditions, not console errors,
   # so check that no file was created
   remote$console.clear()
   remote$console.executeExpr({
      cat(file.exists("/tmp/not-in-project.txt"))
   })
   output <- paste(remote$console.getOutput(), collapse = "\n")
   expect_match(output, "FALSE")

})
