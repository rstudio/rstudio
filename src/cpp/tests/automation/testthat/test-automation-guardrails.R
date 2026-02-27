
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())


# Helper: inject guardrail bindings, execute code in the console,
# and verify that the output contains the expected error text.
.rs.guardrails.expectError <- function(expr, pattern = "denied")
{
   code <- paste(deparse(rlang::enexpr(expr)), collapse = "\n")
   remote$console.executeExpr({ .rs.chat.injectBindings() })
   withr::defer(remote$console.executeExpr({ .rs.chat.restoreBindings() }))
   remote$console.clear()
   remote$console.execute(code)
   output <- paste(remote$console.getOutput(), collapse = "\n")
   expect_match(output, pattern)
}

# Helper: inject guardrail bindings, execute code in the console,
# and verify that the output does NOT contain an error.
.rs.guardrails.expectSuccess <- function(expr)
{
   code <- paste(deparse(rlang::enexpr(expr)), collapse = "\n")
   remote$console.executeExpr({ .rs.chat.injectBindings() })
   withr::defer(remote$console.executeExpr({ .rs.chat.restoreBindings() }))
   remote$console.clear()
   remote$console.execute(code)
   output <- paste(remote$console.getOutput(), collapse = "\n")
   expect_no_match(output, "denied")
   expect_no_match(output, "Error")
}


# -- Read guardrails ----------------------------------------------------------

.rs.test("reading ~/.aws/credentials is denied",
{
   .rs.guardrails.expectError(readLines("~/.aws/credentials"))
})

.rs.test("reading ~/.ssh/config is denied",
{
   .rs.guardrails.expectError(readLines("~/.ssh/config"))
})

.rs.test("reading ~/.netrc is denied",
{
   .rs.guardrails.expectError(readLines("~/.netrc"))
})

.rs.test("reading .env files is denied",
{
   envFile <- file.path(tempdir(), ".env")
   writeLines("SECRET=abc", envFile)
   withr::defer(unlink(envFile))
   .rs.guardrails.expectError(readLines(!!envFile))
})

.rs.test("reading .Renviron files is denied",
{
   envFile <- file.path(tempdir(), ".Renviron")
   writeLines("SECRET=abc", envFile)
   withr::defer(unlink(envFile))
   .rs.guardrails.expectError(readLines(!!envFile))
})

.rs.test("reading SSH private keys is denied",
{
   .rs.guardrails.expectError(readLines("~/.ssh/id_rsa"))
})

.rs.test("reading .Rprofile is denied",
{
   rprofile <- file.path(tempdir(), ".Rprofile")
   remote$console.executeExpr({
      writeLines("# profile", !!rprofile)
   })
   withr::defer(remote$console.executeExpr({ unlink(!!rprofile) }))
   .rs.guardrails.expectError(readLines(!!rprofile))
})

.rs.test("reading .env.local variant is denied",
{
   envFile <- file.path(tempdir(), ".env.local")
   remote$console.executeExpr({
      writeLines("SECRET=abc", !!envFile)
   })
   withr::defer(remote$console.executeExpr({ unlink(!!envFile) }))
   .rs.guardrails.expectError(readLines(!!envFile))
})

.rs.test("reading SSH public key is allowed",
{
   pubKey <- file.path(tempdir(), "id_rsa.pub")
   remote$console.executeExpr({
      writeLines("ssh-rsa AAAA...", !!pubKey)
   })
   withr::defer(remote$console.executeExpr({ unlink(!!pubKey) }))
   .rs.guardrails.expectSuccess(readLines(!!pubKey))
})

.rs.test("reading normal files is allowed",
{
   remote$console.executeExpr({
      writeLines("hello", file.path(tempdir(), "guardrail-read.txt"))
   })
   .rs.guardrails.expectSuccess({
      readLines(file.path(tempdir(), "guardrail-read.txt"))
   })
})


# -- Write guardrails ---------------------------------------------------------

.rs.test("writing outside project directory is denied",
{
   path <- file.path(dirname(tempdir()), "not-in-project.txt")
   .rs.guardrails.expectError(writeLines("x", !!path))
})

.rs.test("writing to ~/.ssh is denied",
{
   .rs.guardrails.expectError(writeLines("x", "~/.ssh/test"))
})

.rs.test("writing to tempdir is allowed",
{
   .rs.guardrails.expectSuccess({
      writeLines("hello", tempfile("guardrail-write-"))
   })
})

.rs.test("file.create outside project directory is denied",
{
   path <- file.path(dirname(tempdir()), "not-in-project.txt")
   .rs.guardrails.expectError(file.create(!!path))
})

.rs.test("file.remove outside project directory is denied",
{
   path <- file.path(dirname(tempdir()), "not-in-project.txt")
   .rs.guardrails.expectError(file.remove(!!path))
})

.rs.test("unlink outside project directory is denied",
{
   path <- tempfile("test-file-", tmpdir = dirname(tempdir()))
   file.create(path)
   on.exit(unlink(path))
   
   .rs.guardrails.expectError(unlink(!!path))
})

.rs.test("file.copy to denied path is denied",
{
   src <- file.path(tempdir(), "guardrail-copy-src.txt")
   remote$console.executeExpr({
      writeLines("hello", !!src)
   })
   withr::defer(remote$console.executeExpr({ unlink(!!src) }))
   dest <- file.path(dirname(tempdir()), "guardrail-copy-dest.txt")
   .rs.guardrails.expectError(file.copy(!!src, !!dest))
})

.rs.test("file.rename to denied path is denied",
{
   src <- file.path(tempdir(), "guardrail-rename-src.txt")
   remote$console.executeExpr({
      writeLines("hello", !!src)
   })
   withr::defer(remote$console.executeExpr({ unlink(!!src) }))
   dest <- file.path(dirname(tempdir()), "guardrail-rename-dest.txt")
   .rs.guardrails.expectError(file.rename(!!src, !!dest))
})


# -- Connection guardrails ----------------------------------------------------

.rs.test("file() connection to sensitive path is denied",
{
   .rs.guardrails.expectError({
      file("~/.aws/credentials", open = "r")
   })
})

.rs.test("file() with deferred open to sensitive path is denied",
{
   .rs.guardrails.expectError({
      file("~/.ssh/test")
   })
})


# -- Path traversal -----------------------------------------------------------

.rs.test("path traversal with '..' is rejected",
{
   .rs.guardrails.expectError(
      writeLines("x", file.path(tempdir(), "sub/../../etc/passwd")),
      pattern = "unresolved"
   )
})


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
   expect_no_match(output, "denied")
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
   expect_no_match(output, "denied")
   expect_no_match(output, "not allowed")
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
   expect_no_match(output, "denied")
   expect_no_match(output, "not allowed")
})

