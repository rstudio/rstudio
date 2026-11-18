#
# test-julia.R
#
# Copyright (C) 2026 by Posit Software, PBC
#
# Unless you have received this program directly from Posit Software pursuant
# to the terms of a commercial license agreement with Posit Software, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

context("julia")

test_that("complete UTF-8 text is passed on whole", {
   # built from code points, to keep this file ASCII
   texts <- c(
      "",
      "abc",
      intToUtf8(c(0x63, 0x61, 0x66, 0xE9)),
      intToUtf8(0x2208),
      intToUtf8(0x1F600),
      intToUtf8(c(0x61, 0xE9, 0x2208, 0x1F600))
   )

   for (text in texts)
   {
      bytes <- charToRaw(enc2utf8(text))
      expect_identical(.rs.julia.completeUtf8Length(bytes), length(bytes), info = text)
   }
})

test_that("a character cut off part-way through is held back", {
   # 2-, 3- and 4-byte characters, after one complete character
   for (char in intToUtf8(c(0xE9, 0x2208, 0x1F600), multiple = TRUE))
   {
      bytes <- c(charToRaw("a"), charToRaw(enc2utf8(char)))
      for (cut in seq_len(length(bytes) - 2L))
      {
         truncated <- bytes[seq_len(length(bytes) - cut)]
         expect_identical(.rs.julia.completeUtf8Length(truncated), 1L, info = paste(char, cut))
      }
   }
})

test_that("bytes that aren't UTF-8 aren't held back", {
   # stray continuation bytes, with no lead byte among the last four
   bytes <- as.raw(c(0x61, 0x80, 0x80, 0x80, 0x80))
   expect_identical(.rs.julia.completeUtf8Length(bytes), length(bytes))
})

test_that("the help handler only answers completion help", {
   expect_identical(.rs.julia.helpHandler("url", "println", "Main"), "")
   expect_null(.rs.julia.helpHandler("parameter", NULL, "Main"))

   # a completion that isn't a binding, such as a field, has no help to find
   expect_null(.rs.julia.helpHandler("completion", "length", ""))
})

# Skip a test that needs a live Julia the console supports. The tests stop
# Julia when they're done; don't take a running session with them, whether
# the console's or one started through JuliaConnectoR directly.
skipUnlessJulia <- function()
{
   skip_if_not_installed("JuliaConnectoR")
   skip_if(is.null(.rs.julia.executable()), "Julia isn't installed")
   skip_if(.rs.julia.isFirstStart(), "Julia's first start installs packages")

   # e.g. 'julia version 1.13.4'
   version <- tryCatch(
      suppressWarnings(system2(.rs.julia.executable(), "--version", stdout = TRUE, stderr = FALSE)),
      error = function(e) character()
   )
   version <- regmatches(version, regexpr("[0-9]+\\.[0-9]+", version))
   skip_if(length(version) == 0L, "Julia's version can't be told")
   skip_if(numeric_version(version) < .rs.julia.minimumVersion, "Julia is older than the console supports")

   skip_if(!is.null(.rs.julia.state$pid), "a Julia session is already running")
   skip_if(!is.null(asNamespace("JuliaConnectoR")$pkgLocal$con), "a Julia session is already running")
}

test_that("completions and their help come from Julia", {
   skipUnlessJulia()

   on.exit(.rs.julia.stop(), add = TRUE)
   .rs.julia.start()

   types <- .rs.acCompletionTypes

   # a module's member, typed so that the client asks the help handler
   completions <- .rs.julia.getCompletions("Base.Mat")
   expect_identical(completions$token, "Mat")
   expect_identical(completions$helpHandler, .rs.scalar(".rs.julia.helpHandler"))
   math <- match("Math", completions$results)
   expect_false(is.na(math))
   expect_identical(completions$packages[[math]], "Base")
   expect_identical(completions$type[[math]], types$ENVIRONMENT)

   # a mutating function, and a macro, which isn't called with parentheses
   completions <- .rs.julia.getCompletions("pushfirs")
   expect_identical(completions$type[completions$results == "pushfirst!"], types$FUNCTION)
   completions <- .rs.julia.getCompletions("@tim")
   expect_identical(completions$type[completions$results == "@time"], types$UNKNOWN)

   # documentation, headed by the signature it begins with (even one wrapped
   # over several lines); an example after it stays in the description. (Not
   # a raw string, which needs R 4.0.)
   JuliaConnectoR::juliaEval('
      """
          rstudio_test_add(x,
                           y = 1)

      Add `y` to `x`.

          rstudio_test_add(2)
      """
      rstudio_test_add(x, y = 1) = x + y
   ')

   help <- .rs.julia.helpHandler("completion", "rstudio_test_add", "Main")
   expect_identical(help$signature, "rstudio_test_add(x, y = 1)")
   expect_identical(help$title, "")
   expect_match(help$description, "Add `y` to `x`.", fixed = TRUE)
   expect_match(help$description, "rstudio_test_add(2)", fixed = TRUE)
   expect_false(help$has_help)

   # or, without one, by the topic
   help <- .rs.julia.helpHandler("completion", "Math", "Base")
   expect_identical(help$signature, "")
   expect_identical(help$title, "Math")

   # the process is told apart from others, before it is ever killed
   expect_true(.rs.julia.isJuliaProcess(.rs.julia.state$pid))
   expect_false(.rs.julia.isJuliaProcess(Sys.getpid()))
})

test_that("output reaches R before the result that follows it", {
   skipUnlessJulia()

   on.exit(.rs.julia.stop(), add = TRUE)
   .rs.julia.start()
   skip_if(
      JuliaConnectoR::juliaEval("Threads.nthreads(:interactive)") < 2,
      "Julia has no interactive thread for the output (JULIA_NUM_THREADS is set)"
   )

   # a task user code spawns, busy on the default thread, doesn't hold up the
   # output; it allocates, so as not to hold up the garbage collector too
   JuliaConnectoR::juliaEval('
      rstudio_test_busy = Threads.@spawn begin
         t = time(); n = 0
         while time() - t < 15; n += length(string(n)); end
      end;
   ')

   # JuliaConnectoR used directly, on the console's connection: each result
   # comes after its output, without waiting for the forwarders to time out
   started <- Sys.time()
   for (i in 1:10)
   {
      code <- sprintf('println("line %1$d"); %1$d', i)
      output <- capture.output(value <- JuliaConnectoR::juliaEval(code))
      expect_identical(output, sprintf("line %d", i))
      expect_identical(value, i)
   }
   expect_lt(as.numeric(difftime(Sys.time(), started, units = "secs")), 5)
})

test_that("only the console starts Julia with serve.jl", {
   skipUnlessJulia()

   jc <- asNamespace("JuliaConnectoR")
   original <- jc$juliaConnection
   expect_false(identical(body(original), body(.rs.julia.connectoRConnection)))

   on.exit(.rs.julia.stop(), add = TRUE)
   .rs.julia.start()
   expect_match(JuliaConnectoR::juliaEval("PROGRAM_FILE"), "serve[.]jl$")
   expect_identical(jc$juliaConnection, original)

   # JuliaConnectoR used directly, outside the console, is left alone
   .rs.julia.stop()
   expect_match(JuliaConnectoR::juliaEval("PROGRAM_FILE"), "main[.]jl$")
})

# Did the server close the connection within `timeout` seconds?
isConnectionClosed <- function(con, timeout)
{
   socketSelect(list(con), timeout = timeout) && length(readBin(con, "raw", 1L)) == 0L
}

openTestConnection <- function(port)
{
   socketConnection("127.0.0.1", port, blocking = TRUE, open = "r+b", timeout = 2)
}

test_that("the console's Julia server serves the client that sends its token", {
   skipUnlessJulia()

   token <- paste0(.rs.createUUID(), .rs.createUUID())
   port <- .rs.julia.runServer(token)

   # one that connects first and says nothing doesn't hold up the others
   silent <- openTestConnection(port)
   on.exit(close(silent), add = TRUE)

   # nor is one with the wrong token served
   wrong <- openTestConnection(port)
   on.exit(close(wrong), add = TRUE)
   writeBin(charToRaw(paste(rev(strsplit(token, "")[[1L]]), collapse = "")), wrong)
   expect_true(isConnectionClosed(wrong, 10))

   # the client with the token is, and ends Julia when it leaves
   right <- openTestConnection(port)
   on.exit(close(right), add = TRUE)
   writeBin(charToRaw(token), right)
   expect_false(isConnectionClosed(right, 1))
   writeBin(asNamespace("JuliaConnectoR")$BYEBYE, right)
   expect_true(isConnectionClosed(right, 5))

   # with the server gone, the silent one is closed too
   expect_true(isConnectionClosed(silent, 5))
})

test_that("Julia ends with R, even while it's busy", {
   # on Windows, the job R's system2() puts Julia in ends it with R instead
   skip_on_os("windows")
   skipUnlessJulia()

   on.exit(.rs.julia.stop(kill = FALSE), add = TRUE)
   .rs.julia.start()
   pid <- .rs.julia.state$pid

   # a watchdog like the one Julia started for R, for a process that ends
   # while Julia is busy; Julia's process group goes with it
   JuliaConnectoR::juliaEval("rstudio_test_sleep = run(`sleep 1`; wait = false)")
   sleeper <- JuliaConnectoR::juliaEval("getpid(rstudio_test_sleep)")
   .rs.julia.call("RStudio.start_watchdog", as.integer(sleeper))
   expect_error(.rs.julia.evaluate("while true; sleep(0.01); end"), "The Julia session ended")

   deadline <- Sys.time() + 5
   while (.rs.julia.isJuliaProcess(pid) && Sys.time() < deadline)
      Sys.sleep(0.1)

   expect_false(.rs.julia.isJuliaProcess(pid))
})
