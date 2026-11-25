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

# Turn the Julia console on or off (the 'julia_console_enabled' preference)
# for the rest of the calling test.
localJuliaConsole <- function(enabled = TRUE, envir = parent.frame())
{
   original <- .rs.api.readRStudioPreference("julia_console_enabled")
   withr::defer(.rs.api.writeRStudioPreference("julia_console_enabled", original), envir = envir)
   .rs.api.writeRStudioPreference("julia_console_enabled", enabled)
}

test_that("Julia won't start once the user turns the Julia console off", {
   # on by default
   expect_true(.rs.julia.isEnabled())

   localJuliaConsole(FALSE)
   expect_false(.rs.julia.isEnabled())
   expect_error(.rs.julia.start(), "is turned off")
   expect_error(.rs.julia.repl(quiet = TRUE), "is turned off")
   expect_false(.rs.julia.replIsActive())
})

test_that("Julia won't start when an administrator turns it off", {
   # julia-enabled defaults to on
   expect_true(.rs.julia.isAllowed())

   # the administrator's choice comes before the user's
   localJuliaConsole(TRUE)

   # testthat can't mock bindings in the locked tools:rstudio environment
   rsEnv <- as.environment("tools:rstudio")
   original <- get(".rs.julia.isAllowed", envir = rsEnv)
   on.exit(assign(".rs.julia.isAllowed", original, envir = rsEnv), add = TRUE)
   assign(".rs.julia.isAllowed", function() FALSE, envir = rsEnv)

   expect_error(.rs.julia.start(), "disabled by the administrator")
   expect_error(.rs.julia.repl(quiet = TRUE), "disabled by the administrator")
   expect_false(.rs.julia.replIsActive())
})

test_that("the console won't use a Julia server that other R processes share", {
   skip_if_not_installed("JuliaConnectoR")
   localJuliaConsole(TRUE)
   withr::local_envvar(JULIACONNECTOR_SERVER = "localhost:11980")

   expect_error(.rs.julia.start(), "shared Julia server at localhost:11980", fixed = TRUE)
   expect_error(.rs.julia.repl(quiet = TRUE), "shared Julia server", fixed = TRUE)
   expect_false(.rs.julia.replIsActive())
})

juliaIsSuspendable <- function()
{
   .Call("rs_juliaIsSuspendable", PACKAGE = "(embedding)")
}

test_that("a session with a running Julia isn't suspended", {
   on.exit(.Call("rs_juliaSetProcess", .rs.julia.state$pid, PACKAGE = "(embedding)"), add = TRUE)

   # this R process stands in for a running Julia
   .Call("rs_juliaSetProcess", Sys.getpid(), PACKAGE = "(embedding)")
   expect_false(juliaIsSuspendable())

   .Call("rs_juliaSetProcess", NULL, PACKAGE = "(embedding)")
   expect_true(juliaIsSuspendable())

   # a Julia that has since gone: no process has an ID this large
   .Call("rs_juliaSetProcess", .Machine$integer.max, PACKAGE = "(embedding)")
   expect_true(juliaIsSuspendable())
})

test_that("a JuliaConnectoR without the internals the console uses is told apart", {
   expect_setequal(
      .rs.julia.missingConnectoRInternals(new.env()),
      c(
         "juliaConnection", "getJuliaExecutablePath", "getJuliaEnv",
         "readElement", "readString", "readCall", "answerCallback",
         "writeString", "writeList",
         "CALL_INDICATOR", "RESULT_INDICATOR", "FAIL_INDICATOR",
         "STDOUT_INDICATOR", "STDERR_INDICATOR",
         "pkgLocal"
      )
   )

   # with writeCallMessage(), the console doesn't write a call's parts itself
   newer <- list2env(list(writeCallMessage = function(name, args) NULL))
   missing <- .rs.julia.missingConnectoRInternals(newer)
   expect_false(any(c("writeString", "writeList", "writeCallMessage") %in% missing))

   skip_if_not_installed("JuliaConnectoR")
   expect_identical(.rs.julia.missingConnectoRInternals(asNamespace("JuliaConnectoR")), character())
})

test_that("the help handler only answers completion help", {
   expect_identical(.rs.julia.helpHandler("url", "println", "Main"), "")
   expect_null(.rs.julia.helpHandler("parameter", NULL, "Main"))

   # a completion that isn't a binding, such as a field, has no help to find
   expect_null(.rs.julia.helpHandler("completion", "length", ""))
})

test_that("of several installed Julias, the newest is found first", {
   root <- tempfile("julia-installs-")
   on.exit(unlink(root, recursive = TRUE), add = TRUE)

   # in the order a sort by name gives
   for (version in c("1.10.0", "1.11.0-rc1", "1.9.3"))
   {
      bin <- file.path(root, paste0("Julia-", version), "bin")
      dir.create(bin, recursive = TRUE)
      file.create(file.path(bin, "julia"))
   }

   installs <- .rs.julia.newestInstalls(file.path(root, "Julia-*", "bin", "julia"))
   expect_identical(
      basename(dirname(dirname(installs))),
      c("Julia-1.11.0-rc1", "Julia-1.10.0", "Julia-1.9.3")
   )

   expect_identical(.rs.julia.newestInstalls(file.path(root, "Julia-*", "bin", "julia.exe")), character())
})

# Skip a test that needs a live Julia the console supports, and turn the
# Julia console on for the rest of a test that isn't skipped. The tests stop
# Julia when they're done; don't take a running session with them, whether
# the console's or one started through JuliaConnectoR directly.
skipUnlessJulia <- function(envir = parent.frame())
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

   localJuliaConsole(TRUE, envir)
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

test_that("display() shows text in the console, as values are", {
   skipUnlessJulia()

   on.exit(.rs.julia.stop(), add = TRUE)
   .rs.julia.start()

   output <- capture.output(.rs.julia.evaluate('display("rstudio"); 42'))
   expect_identical(output, c('"rstudio"', "42"))
})

test_that("an interrupt that arrives while an error is reported doesn't cut the report short", {
   # the error's own report sends the signal, which Windows doesn't have
   skip_on_os("windows")
   skipUnlessJulia()

   on.exit(.rs.julia.stop(), add = TRUE)
   .rs.julia.start()

   # the sleep gives Julia time to notice the signal within the report
   JuliaConnectoR::juliaEval('
      struct RStudioTestError <: Exception end
      function Base.showerror(io::IO, ::RStudioTestError)
         ccall(:kill, Cint, (Cint, Cint), getpid(), 2)
         sleep(0.5)
         print(io, "rstudio test error, reported in full")
      end
   ')

   output <- capture.output(.rs.julia.evaluate("throw(RStudioTestError())"), type = "message")
   expect_match(paste(output, collapse = "\n"), "rstudio test error, reported in full", fixed = TRUE)

   # and Julia carries on
   expect_identical(capture.output(.rs.julia.evaluate("40 + 2")), "42")
})

test_that("ans holds the last value, also once it has been read", {
   skipUnlessJulia()

   on.exit(.rs.julia.stop(), add = TRUE)
   .rs.julia.start()

   # Julia 1.10 and 1.11 refuse to assign an 'ans' that Main has read
   .rs.julia.evaluate("ans")
   expect_identical(capture.output(.rs.julia.evaluate("40 + 2")), "42")
   expect_identical(capture.output(.rs.julia.evaluate("ans + 1")), "43")
})

# Write a PNG of `colors`, a matrix of R colors, one pixel each: drawn at its
# own size, and uninterpolated, it keeps them.
writeTestPng <- function(colors, path)
{
   png(path, width = ncol(colors), height = nrow(colors), type = "cairo", bg = "transparent")
   grid::grid.raster(as.raster(colors), interpolate = FALSE)
   dev.off()
}

# Make the PNG at `path` the image the Julia global `name` shows.
defineTestImage <- function(name, path)
{
   # an identical redefinition is allowed
   JuliaConnectoR::juliaEval('
      struct RStudioTestImage
         png::Vector{UInt8}
      end
      Base.show(io::IO, ::MIME"image/png", x::RStudioTestImage) = write(io, x.png)
   ')
   JuliaConnectoR::juliaLet(sprintf("global %s = RStudioTestImage(read(path)); nothing", name), path = path)
}

# The grobs on the current page, named by kind ("rect", "rastergrob").
pageGrobs <- function()
{
   grobs <- lapply(grid::grid.ls(print = FALSE)$name, grid::grid.get)
   names(grobs) <- vapply(grobs, function(grob) class(grob)[[1L]], character(1L))
   grobs
}

# A 4 x 5 matrix of colors: `border` along the edge (recycled), `middle` inside.
framedColors <- function(border, middle)
{
   colors <- matrix(middle, nrow = 4L, ncol = 5L)
   edge <- row(colors) %in% c(1L, 4L) | col(colors) %in% c(1L, 5L)
   colors[edge] <- rep_len(border, sum(edge))
   colors
}

test_that("a value that renders as an image is drawn on R's graphics device", {
   skipUnlessJulia()
   skip_if_not(capabilities("cairo"), "R can't write PNGs with cairo")

   colors <- matrix(c("red", "#00FF00", "blue", "white", "black", "#808080"), nrow = 2L, byrow = TRUE)
   path <- tempfile(fileext = ".png")
   on.exit(unlink(path), add = TRUE)
   writeTestPng(colors, path)

   # nativeRaster's packing: red in the lowest byte, row by row from the top
   rgba <- col2rgb(t(colors), alpha = TRUE)
   packed <- rgba[1L, ] + rgba[2L, ] * 2^8 + rgba[3L, ] * 2^16 + rgba[4L, ] * 2^24
   expected <- as.integer(ifelse(packed >= 2^31, packed - 2^32, packed))

   on.exit(.rs.julia.stop(), add = TRUE)
   .rs.julia.start()
   defineTestImage("rstudio_test_image", path)

   pdf(NULL)
   on.exit(dev.off(), add = TRUE)

   # shown, nothing printed
   output <- capture.output(.rs.julia.evaluate("rstudio_test_image"))
   expect_identical(output, character())

   shown <- pageGrobs()$rastergrob
   expect_s3_class(shown$raster, "nativeRaster")
   expect_identical(dim(shown$raster), c(2L, 3L))
   expect_identical(as.integer(shown$raster), expected)

   # as is one passed to display(), on a page of its own; one that a
   # semicolon hides isn't
   .rs.julia.evaluate("display(rstudio_test_image)")
   displayed <- pageGrobs()$rastergrob
   expect_false(identical(displayed$name, shown$name))

   .rs.julia.evaluate("rstudio_test_image;")
   expect_identical(pageGrobs()$rastergrob$name, displayed$name)
})

test_that("the space around an image is filled as its border suggests", {
   skipUnlessJulia()
   skip_if_not(capabilities("cairo"), "R can't write PNGs with cairo")

   images <- list(
      # one color: that color
      rstudio_test_navy  = framedColors("navy", "yellow"),
      # mixed: black or white, by the border's lightness
      rstudio_test_dark  = framedColors(c("black", "#303030"), "white"),
      rstudio_test_light = framedColors(c("white", "#D0D0D0"), "black"),
      # transparent: left alone
      rstudio_test_clear = framedColors("transparent", "red")
   )

   on.exit(.rs.julia.stop(), add = TRUE)
   .rs.julia.start()
   for (name in names(images))
   {
      path <- tempfile(fileext = ".png")
      on.exit(unlink(path), add = TRUE)
      writeTestPng(images[[name]], path)
      defineTestImage(name, path)
   }

   pdf(NULL)
   on.exit(dev.off(), add = TRUE)

   .rs.julia.evaluate("rstudio_test_navy")
   expect_identical(pageGrobs()$rect$gp$fill, "#000080")
   .rs.julia.evaluate("rstudio_test_dark")
   expect_identical(pageGrobs()$rect$gp$fill, "#000000")
   .rs.julia.evaluate("rstudio_test_light")
   expect_identical(pageGrobs()$rect$gp$fill, "#ffffff")
   .rs.julia.evaluate("rstudio_test_clear")
   expect_null(pageGrobs()$rect)

   # or as the user says
   .rs.julia.evaluate('RStudio.plot_background[] = "red";')
   .rs.julia.evaluate("rstudio_test_navy")
   expect_identical(pageGrobs()$rect$gp$fill, "red")

   .rs.julia.evaluate("RStudio.plot_background[] = nothing;")
   .rs.julia.evaluate("rstudio_test_navy")
   expect_null(pageGrobs()$rect)
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
   expect_false(juliaIsSuspendable())

   # JuliaConnectoR used directly, outside the console, is left alone
   .rs.julia.stop()
   expect_true(juliaIsSuspendable())
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
