#
# SessionJulia.R
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

# A Julia REPL in the console, in the manner of reticulate's Python REPL.
#
# JuliaConnectoR runs Julia in a separate process and talks to it over a
# socket. The REPL drives that connection itself rather than going through
# juliaEval(), for two reasons: juliaEval() stops Julia altogether when R is
# interrupted, where the REPL interrupts just the evaluation; and it strips
# ANSI escapes from Julia's output, which the console renders.

# synchronize with JuliaEvent.java
.rs.setVar("juliaEvents", list(
   REPL_INITIALIZED = "repl_initialized",
   REPL_BUSY        = "repl_busy",
   REPL_TEARDOWN    = "repl_teardown"
))

.rs.setVar("julia.state", list2env(parent = emptyenv(), list(

   # is the REPL reading input?
   active     = FALSE,

   # lines of input read so far that don't yet make up complete Julia input
   buffer     = character(),

   # is a call to Julia in progress?
   calling    = FALSE,

   # is Julia evaluating REPL input, so that SIGINT may be sent to it?
   evaluating = FALSE,

   # SIGINTs sent during the current evaluation
   sigints    = 0L,

   # interrupts received while waiting on the current call
   interrupts = 0L,

   # the Julia process, as of when the console's helpers were loaded into it
   pid        = NULL,
   version    = NULL,
   executable = NULL,

   # the incomplete end of a UTF-8 character, by output stream
   partial    = list(),

   # whether Julia was last told to trace its side (see .rs.julia.debug)
   debugging  = NULL

)))

# the console input that starts the REPL; synchronize with
# isJuliaReplEnter() in SessionConsoleInput.cpp
.rs.setVar("julia.replCommand", ".rs.julia.repl()")

# oldest Julia the console's helpers (RStudio.jl) are known to work with
.rs.setVar("julia.minimumVersion", "1.10")

.rs.addFunction("julia.debuggingEnabled", function()
{
   isTRUE(getOption("rstudio.julia.debuggingEnabled"))
})

#' Write a Debugging Message
#'
#' With `options(rstudio.julia.debuggingEnabled = TRUE)`, the REPL's
#' exchanges with Julia -- interrupts above all -- are traced in the console,
#' Julia's side of them included (see `debug` in RStudio.jl).
#'
#' @param fmt,... A format string and its arguments, as for sprintf().
.rs.addFunction("julia.debug", function(fmt, ...)
{
   if (!.rs.julia.debuggingEnabled())
      return(invisible(NULL))

   message <- if (length(list(...))) sprintf(fmt, ...) else fmt
   stamp <- format(Sys.time(), "%H:%M:%OS3")
   cat(sprintf("[julia-debug R %s] %s\n", stamp, message), file = stderr())
})

.rs.addFunction("julia.enqueueClientEvent", function(type, data = list())
{
   data <- list(type = .rs.scalar(type), data = data)
   .rs.enqueClientEvent("julia_event", data)
})

.rs.addFunction("julia.replIsActive", function()
{
   isTRUE(.rs.julia.state$active)
})

#' Start a Julia REPL in the Console
#'
#' Julia input is read, evaluated and printed until 'exit' or 'quit' returns
#' the console to R. Julia runs in a process of its own, started through
#' JuliaConnectoR on first use and kept for the rest of the R session, so
#' Julia's state survives leaving and re-entering the REPL.
#'
#' As in the Julia REPL, input starting with '?' looks up help, ']' runs a
#' Pkg command, and ';' a shell command.
#'
#' @param quiet Whether to skip the banner printed on entry.
.rs.addFunction("julia.repl", function(quiet = getOption("rstudio.julia.repl.quiet", FALSE))
{
   state <- .rs.julia.state
   if (state$active)
      return(invisible(NULL))

   .rs.julia.start()

   state$active <- TRUE
   state$buffer <- character()
   on.exit(.rs.julia.replTeardown(), add = TRUE)

   .rs.julia.enqueueClientEvent(
      .rs.juliaEvents$REPL_INITIALIZED,
      .rs.julia.describe()
   )

   if (!quiet)
   {
      fmt <- c(
         "Julia %s (%s)",
         "JuliaConnectoR %s REPL -- Julia in a process of its own, connected to R.",
         "Enter 'exit' or 'quit' to exit the REPL and return to R."
      )

      msg <- sprintf(
         paste(fmt, collapse = "\n"),
         state$version,
         state$executable,
         utils::packageVersion("JuliaConnectoR")
      )

      message(msg)
   }

   repeat
   {
      status <- tryCatch(
         .rs.julia.replIteration(),
         interrupt = function(cnd) {
            # as in the Julia REPL, an interrupt at the prompt drops the
            # input buffered so far
            .rs.julia.state$buffer <- character()
            "interrupted"
         }
      )

      if (identical(status, "quit"))
         break
   }

   invisible(NULL)
})

.rs.addFunction("julia.replTeardown", function()
{
   state <- .rs.julia.state
   state$active <- FALSE
   state$buffer <- character()

   .rs.julia.enqueueClientEvent(.rs.juliaEvents$REPL_TEARDOWN)
})

#' Read and Evaluate One Line of REPL Input
#'
#' Lines are buffered until they make up complete Julia input, as the Julia
#' REPL judges it, and the whole input is then evaluated.
#'
#' @return "quit" when the REPL should end; otherwise an empty string.
.rs.addFunction("julia.replIteration", function()
{
   state <- .rs.julia.state
   continuing <- length(state$buffer) > 0

   # the Julia REPL's continuation lines are indented under 'julia> '
   prompt <- if (continuing) "       " else "julia> "
   contents <- readline(prompt)
   trimmed <- .rs.trimWhitespace(contents)

   if (!continuing)
   {
      # mirrors isReplExit() in SessionConsoleInput.cpp; Julia has no
      # 'quit', and its 'exit()' would end the Julia process itself
      if (trimmed %in% c("exit", "quit", "exit()", "quit()"))
         return("quit")

      if (!nzchar(trimmed))
         return("")

      mode <- .rs.julia.inputMode(trimmed)
      if (mode != "julia")
      {
         .rs.julia.evaluate(.rs.trimWhitespace(substring(trimmed, 2L)), mode)
         return("")
      }
   }

   code <- paste(c(state$buffer, contents), collapse = "\n")
   if (!isTRUE(.rs.julia.call("RStudio.iscomplete", code)))
   {
      state$buffer <- c(state$buffer, contents)
      return("")
   }

   state$buffer <- character()
   .rs.julia.evaluate(code, "julia")
   ""
})

#' Julia REPL Mode of an Input Line
#'
#' @param line A line of input, with surrounding whitespace removed.
#'
#' @return "help", "pkg" or "shell" for a line starting with the character
#'   that enters that mode in the Julia REPL; otherwise "julia".
.rs.addFunction("julia.inputMode", function(line)
{
   switch(
      substring(line, 1L, 1L),
      "?" = "help",
      "]" = "pkg",
      ";" = "shell",
      "julia"
   )
})

#' Evaluate REPL Input in Julia
#'
#' Julia prints the value or error itself, as its REPL would, and output
#' reaches the console as it is produced.
#'
#' @param code The input, as a single string.
#' @param mode The REPL mode to evaluate it in; see `.rs.julia.inputMode()`.
.rs.addFunction("julia.evaluate", function(code, mode = "julia")
{
   .rs.julia.enqueueClientEvent(
      .rs.juliaEvents$REPL_BUSY,
      list(busy = .rs.scalar(TRUE))
   )

   on.exit(
      .rs.julia.enqueueClientEvent(
         .rs.juliaEvents$REPL_BUSY,
         list(busy = .rs.scalar(FALSE))
      ),
      add = TRUE
   )

   width <- as.integer(getOption("width", 80L))
   color <- !identical(.rs.readUiPref("ansi_console_mode"), "off")

   state <- .rs.julia.state
   debugging <- .rs.julia.debuggingEnabled()
   if (!identical(debugging, state$debugging))
   {
      .rs.julia.call("RStudio.set_debugging", debugging)
      state$debugging <- debugging
   }

   # Julia reports the end of the evaluation before it replies (see
   # .rs.julia.onEvaluationDone)
   .rs.julia.debug("evaluating (%s): %s", mode, encodeString(substring(code, 1L, 60L)))
   state$evaluating <- TRUE
   state$sigints <- 0L
   on.exit(state$evaluating <- FALSE, add = TRUE)

   cwd <- .rs.julia.call("RStudio.evaluate", code, mode, width, color, getwd())

   # Julia changed directory (e.g. with cd()); follow it, as R would with an
   # embedded interpreter sharing its process
   if (is.character(cwd) && nzchar(cwd) && dir.exists(cwd))
      setwd(cwd)

   invisible(NULL)
})

#' Complete Julia REPL Input
#'
#' Offers what the Julia REPL would on Tab, for console input in the REPL.
#'
#' @param line The console input up to the cursor.
.rs.addFunction("julia.getCompletions", function(line)
{
   # Julia can only be asked while it is idle at the prompt
   result <- tryCatch(
      .rs.julia.call("RStudio.completions", line),
      error = function(e) NULL
   )

   if (!is.character(result) || length(result) == 0L)
      return(.rs.emptyCompletions(language = "Julia"))

   # the token, then the completions' names, then their kinds
   n <- (length(result) - 1L) %/% 2L
   token <- result[[1L]]
   names <- result[seq_len(n) + 1L]
   kinds <- result[seq_len(n) + 1L + n]

   types <- .rs.acCompletionTypes
   type <- rep(types$UNKNOWN, length(kinds))
   type[kinds == "function"] <- types$FUNCTION
   type[kinds == "module"]   <- types$PACKAGE
   type[kinds == "keyword"]  <- types$KEYWORD
   type[kinds == "path"]     <- types$FILE

   .rs.makeCompletions(
      token    = token,
      results  = names,
      type     = type,
      quote    = FALSE,
      language = "Julia"
   )
})

#' Start Julia for the Console
#'
#' Starts Julia through JuliaConnectoR if it isn't running yet, and loads the
#' console's helpers (RStudio.jl) into it if they aren't loaded yet.
.rs.addFunction("julia.start", function()
{
   if (!requireNamespace("JuliaConnectoR", quietly = TRUE))
   {
      stop(
         "The 'JuliaConnectoR' package is required for the Julia console.\n",
         "Install it with install.packages(\"JuliaConnectoR\").",
         call. = FALSE
      )
   }

   # a Julia that has since gone away (say, it was killed) leaves
   # JuliaConnectoR holding a dead connection
   connected <- !is.null(asNamespace("JuliaConnectoR")$pkgLocal$con)

   # the first start shows nothing for a long while; say why
   if (!connected && .rs.julia.isFirstStart())
   {
      message(
         "Julia hasn't been started with JuliaConnectoR before. It will now install and ",
         "precompile the Julia packages it needs (Tables.jl), which can take a few minutes."
      )
   }

   loaded <- tryCatch(
      .rs.julia.isLoaded(),
      error = function(e) {
         if (!connected)
            stop(e)
         .rs.julia.stop()
         .rs.julia.isLoaded()
      }
   )

   if (isTRUE(loaded) && !is.null(.rs.julia.state$pid))
      return(invisible(TRUE))

   version <- JuliaConnectoR::juliaEval("string(VERSION)")
   if (numeric_version(sub("[-+].*", "", version)) < .rs.julia.minimumVersion)
   {
      stop(
         sprintf("The Julia console requires Julia %s or newer; ", .rs.julia.minimumVersion),
         sprintf("JuliaConnectoR started Julia %s.", version),
         call. = FALSE
      )
   }

   path <- file.path(
      .Call("rs_rResourcesPath", PACKAGE = "(embedding)"),
      "julia",
      "RStudio.jl"
   )

   JuliaConnectoR::juliaLet("Base.include(Main, path); nothing", path = path)

   info <- .rs.julia.call(
      "RStudio.init",
      .rs.julia.onEvaluationDone,
      asNamespace("JuliaConnectoR")$pkgLocal$communicator
   )

   state <- .rs.julia.state
   state$version    <- info[[1L]]
   state$pid        <- as.integer(info[[2L]])
   state$executable <- info[[3L]]
   state$partial    <- list()
   state$debugging  <- NULL

   .rs.julia.debug("loaded the console's helpers into Julia %s (pid %d)", state$version, state$pid)

   invisible(TRUE)
})

#' Are the Console's Helpers Loaded into Julia?
#'
#' Starts Julia through JuliaConnectoR if it isn't running yet. Julia is
#' looked for as JuliaConnectoR looks for it (JULIA_BINDIR, then the PATH),
#' then where it's commonly installed: RStudio's PATH often lacks those
#' places, as with Homebrew's on macOS when RStudio is started from Finder.
.rs.addFunction("julia.isLoaded", function()
{
   bindir <- .rs.julia.findBinDir()
   if (!is.null(bindir))
   {
      path <- Sys.getenv("PATH")
      on.exit(Sys.setenv(PATH = path), add = TRUE)
      Sys.setenv(PATH = paste(bindir, path, sep = .Platform$path.sep))
   }

   JuliaConnectoR::juliaEval("isdefined(Main, :RStudio)")
})

#' Will Julia's Start Be a First One?
#'
#' JuliaConnectoR needs Tables.jl, and installs it the first time it starts
#' Julia with a depot that lacks it; Julia then precompiles it, as it does
#' again for each new Julia version. That takes a minute or more, during which
#' nothing is shown. Precompiled Tables.jl in the depot means it's done.
#'
#' @return TRUE when Tables.jl hasn't been precompiled for the Julia that
#'   would be started; FALSE otherwise, or when that can't be told.
.rs.addFunction("julia.isFirstStart", function()
{
   julia <- .rs.julia.executable()
   if (is.null(julia))
      return(FALSE)

   version <- tryCatch(
      suppressWarnings(system2(julia, "--version", stdout = TRUE, stderr = FALSE)),
      error = function(e) character()
   )

   # e.g. 'julia version 1.13.4'
   matches <- regmatches(version, regexpr("[0-9]+\\.[0-9]+", version))
   if (length(matches) == 0L)
      return(FALSE)

   compiled <- file.path(.rs.julia.depotPath(), "compiled", paste0("v", matches[[1L]]), "Tables")
   !dir.exists(compiled)
})

#' The Julia Depot Packages Are Installed In
#'
#' The first entry of JULIA_DEPOT_PATH, or Julia's default.
.rs.addFunction("julia.depotPath", function()
{
   entries <- strsplit(Sys.getenv("JULIA_DEPOT_PATH"), .Platform$path.sep, fixed = TRUE)[[1L]]
   entries <- entries[nzchar(entries)]
   if (length(entries))
      return(entries[[1L]])

   home <- if (identical(.Platform$OS.type, "windows")) Sys.getenv("USERPROFILE") else path.expand("~")
   file.path(home, ".julia")
})

#' The Julia Executable to Start
#'
#' @return The path to the Julia that JuliaConnectoR will start (see
#'   .rs.julia.isLoaded), or NULL when there is none.
.rs.addFunction("julia.executable", function()
{
   exe <- if (identical(.Platform$OS.type, "windows")) "julia.exe" else "julia"

   bindir <- Sys.getenv("JULIA_BINDIR")
   if (nzchar(bindir))
      return(file.path(bindir, exe))

   path <- Sys.which("julia")
   if (nzchar(path))
      return(unname(path))

   bindir <- .rs.julia.findBinDir()
   if (is.null(bindir))
      return(NULL)

   file.path(bindir, exe)
})

#' Find Julia Where JuliaConnectoR Wouldn't
#'
#' @return The directory of a Julia executable that JuliaConnectoR wouldn't
#'   find on its own, or NULL when it would (or there is none).
.rs.addFunction("julia.findBinDir", function()
{
   if (nzchar(Sys.getenv("JULIA_BINDIR")) || nzchar(Sys.which("julia")))
      return(NULL)

   exe <- if (identical(.Platform$OS.type, "windows")) "julia.exe" else "julia"
   candidates <- c(
      file.path(path.expand("~"), ".juliaup", "bin", exe),
      file.path(Sys.getenv("LOCALAPPDATA"), "Programs", "Julia", "bin", exe),
      file.path("/opt/homebrew/bin", exe),
      file.path("/usr/local/bin", exe),
      rev(sort(Sys.glob("/Applications/Julia-*.app/Contents/Resources/julia/bin/julia")))
   )

   for (candidate in candidates)
   {
      if (file.exists(candidate))
         return(dirname(candidate))
   }

   NULL
})

.rs.addFunction("julia.describe", function()
{
   state <- .rs.julia.state
   list(
      version    = .rs.scalar(state$version),
      executable = .rs.scalar(state$executable)
   )
})

#' Call a Julia Function over JuliaConnectoR's Connection
#'
#' This stands in for JuliaConnectoR's juliaCall(), so that an interrupt
#' reaches Julia as SIGINT rather than ending the Julia process, and so that
#' Julia's output keeps its ANSI escapes.
#'
#' An interrupt must not cut a message short, which would leave the
#' connection unusable, and suspendInterrupts() can't prevent that here: R
#' raises a pending interrupt whenever a socket read waits for data. So an
#' interrupt while the call is in progress is passed on to Julia, and the
#' read resumed (see .rs.julia.onInterrupt).
#'
#' The function is looked up when it is called, through invokelatest():
#' JuliaConnectoR resolves names in the world it started in, before the
#' console's helpers were loaded, which Julia 1.12 and newer warn about.
#'
#' @param name The name of the Julia function.
#' @param ... Arguments to the function, translated by JuliaConnectoR.
#'
#' @return The function's result, translated by JuliaConnectoR.
.rs.addFunction("julia.call", function(name, ...)
{
   jc <- asNamespace("JuliaConnectoR")
   con <- jc$pkgLocal$con
   if (is.null(con))
      stop("Julia is not running.", call. = FALSE)

   # R code run while a read waits (e.g. an RPC handler) mustn't start a
   # call of its own in the middle of this one's messages
   state <- .rs.julia.state
   if (isTRUE(state$calling))
      stop("Julia is busy.", call. = FALSE)

   # a call cut short (by an interrupt that can't be resumed, say) leaves its
   # messages half read or written, and Julia can't be talked to any more
   completed <- FALSE
   state$calling <- TRUE
   state$interrupts <- 0L
   on.exit({
      state$calling <- FALSE
      if (!completed)
      {
         .rs.julia.debug("call to %s cut short; stopping Julia", name)
         .rs.julia.stop()
      }
   }, add = TRUE)

   reply <- withCallingHandlers(
      .rs.julia.callImpl(con, name, list(...)),
      interrupt = .rs.julia.onInterrupt
   )

   completed <- TRUE
   if (identical(reply$type, "fail"))
   {
      .rs.julia.debug("call to %s failed", name)
      stop(reply$value, call. = FALSE)
   }

   reply$value
})

#' Exchange a Call's Messages with Julia
#'
#' @return Julia's reply: a list with `type` "result" or "fail", and `value`.
.rs.addFunction("julia.callImpl", function(con, name, args)
{
   jc <- asNamespace("JuliaConnectoR")

   writeBin(jc$CALL_INDICATOR, con)
   jc$writeString("Base.invokelatest")
   jc$writeList(c(list(JuliaConnectoR::juliaExpr(name)), args))

   repeat
   {
      if (!socketSelect(list(con), timeout = 0.1))
         next

      reply <- .rs.julia.readMessage(con)
      if (reply$type %in% c("result", "fail"))
         return(reply)
   }
})

#' Handle an Interrupt During a Julia Call
#'
#' A calling handler: the interrupt is passed on to Julia, and the call
#' resumed where it was interrupted.
#'
#' @param cnd The interrupt condition.
.rs.addFunction("julia.onInterrupt", function(cnd)
{
   .rs.julia.interrupt()

   # by name: R's own 'resume' restart has no names to match on otherwise
   if (!is.null(findRestart("resume")))
      invokeRestart("resume")
})

#' Read One Message from Julia
#'
#' Output is written to the console as it arrives, and callbacks into R are
#' answered as JuliaConnectoR would.
#'
#' @param con JuliaConnectoR's connection, with data waiting to be read.
#'
#' @return A list with the message's `type` ("result", "fail", "output" or
#'   "callback") and, for a result or failure, its `value`.
.rs.addFunction("julia.readMessage", function(con)
{
   jc <- asNamespace("JuliaConnectoR")

   # data was waiting, so nothing to read means Julia closed the connection
   type <- readBin(con, "raw", 1L)
   if (length(type) == 0L)
      .rs.julia.onConnectionLost()

   if (type == jc$STDOUT_INDICATOR || type == jc$STDERR_INDICATOR)
   {
      stream <- if (type == jc$STDOUT_INDICATOR) "stdout" else "stderr"
      size <- readBin(.rs.julia.readBytes(con, 4L), "integer", size = 4L)
      .rs.julia.writeOutput(.rs.julia.readBytes(con, size), stream)
      return(list(type = "output"))
   }
   else if (type == jc$RESULT_INDICATOR)
   {
      return(list(type = "result", value = jc$readElement()))
   }
   else if (type == jc$FAIL_INDICATOR)
   {
      return(list(type = "fail", value = jc$readString()))
   }
   else if (type == jc$CALL_INDICATOR)
   {
      call <- jc$readCall()
      callback <- get(call$name, envir = jc$pkgLocal$callbacks)
      jc$answerCallback(callback, call$args)
      return(list(type = "callback"))
   }

   .rs.julia.onConnectionLost(sprintf("unexpected message type 0x%s", format(type)))
})

#' Read Bytes from Julia's Connection
#'
#' The connection's reads time out and come back short while Julia is quiet,
#' so a short read is retried until the bytes arrive -- unless the connection
#' turned out to be closed.
#'
#' @param con JuliaConnectoR's connection.
#' @param n The number of bytes to read.
.rs.addFunction("julia.readBytes", function(con, n)
{
   bytes <- readBin(con, "raw", n)
   while (length(bytes) < n)
   {
      if (!socketSelect(list(con), timeout = 1))
         next

      more <- readBin(con, "raw", n - length(bytes))
      if (length(more) == 0L)
         .rs.julia.onConnectionLost()

      bytes <- c(bytes, more)
   }

   bytes
})

#' Write Julia Output to the Console
#'
#' Julia sends output in chunks as it is produced, which can split a UTF-8
#' character; the incomplete end of a chunk is held back for the next one.
#'
#' @param bytes The output, as a raw vector.
#' @param stream The stream it was written to: "stdout" or "stderr".
.rs.addFunction("julia.writeOutput", function(bytes, stream)
{
   state <- .rs.julia.state
   bytes <- c(state$partial[[stream]], bytes)

   complete <- .rs.julia.completeUtf8Length(bytes)
   state$partial[[stream]] <- bytes[seq_len(length(bytes) - complete) + complete]

   bytes <- bytes[seq_len(complete)]
   bytes <- bytes[bytes != as.raw(0L)]
   if (length(bytes) == 0L)
      return(invisible(NULL))

   text <- rawToChar(bytes)
   Encoding(text) <- "UTF-8"

   file <- if (identical(stream, "stderr")) stderr() else stdout()
   cat(text, file = file)
})

#' Length of the Complete UTF-8 Prefix of a Byte Sequence
#'
#' @param bytes A raw vector holding UTF-8 text, possibly cut off part-way
#'   through its last character.
#'
#' @return The number of bytes up to the end of the last complete character.
.rs.addFunction("julia.completeUtf8Length", function(bytes)
{
   n <- length(bytes)

   # a character takes at most 4 bytes, so its lead byte is among the last 4
   for (i in rev(seq_len(n))[seq_len(min(n, 4L))])
   {
      byte <- as.integer(bytes[i])

      # a continuation byte; the character starts earlier
      if (byte >= 0x80 && byte < 0xC0)
         next

      # a single-byte character, or the lead byte of a longer one
      size <- if (byte >= 0xF0) 4L else if (byte >= 0xE0) 3L else if (byte >= 0xC0) 2L else 1L
      return(if (n - i + 1L >= size) n else i - 1L)
   }

   n
})

#' Interrupt Julia
#'
#' Called when R is interrupted during a Julia call. During an evaluation,
#' Julia is sent SIGINT, which interrupts it; other calls return promptly, and
#' are left to finish. Code that never reaches a point where Julia can be
#' interrupted keeps running, so a third interrupt stops Julia altogether.
#'
#' Windows has no SIGINT for another process, and R can't resume a call
#' interrupted there, so an interrupt stops Julia.
.rs.addFunction("julia.interrupt", function()
{
   state <- .rs.julia.state
   .rs.julia.debug("interrupted (evaluating: %s)", state$evaluating)

   if (identical(.Platform$OS.type, "windows"))
   {
      .rs.julia.stop()
      stop("Julia can't be interrupted on Windows, and was stopped.", call. = FALSE)
   }

   state$interrupts <- state$interrupts + 1L
   if (state$interrupts >= 3L)
   {
      .rs.julia.stop()
      stop("Julia did not respond to the interrupt, and was stopped.", call. = FALSE)
   }

   if (state$evaluating && !is.null(state$pid))
   {
      state$sigints <- state$sigints + 1L
      .rs.julia.debug("sending SIGINT %d to Julia (pid %d)", state$sigints, state$pid)
      tools::pskill(state$pid, tools::SIGINT)
   }

   invisible(NULL)
})

#' Note the End of an Evaluation
#'
#' Julia calls this once an evaluation is over, with interrupts deferred, and
#' consumes any SIGINT that hasn't landed yet before it replies: one landing
#' after that would break JuliaConnectoR's connection.
#'
#' @return The number of SIGINTs sent during the evaluation.
.rs.addFunction("julia.onEvaluationDone", function()
{
   state <- .rs.julia.state
   state$evaluating <- FALSE
   .rs.julia.debug("evaluation done; %d SIGINT(s) sent", state$sigints)
   state$sigints
})

#' Stop Julia
#'
#' Ends the Julia process; the next use of Julia starts a new one.
.rs.addFunction("julia.stop", function()
{
   state <- .rs.julia.state
   pid <- state$pid
   state$pid <- NULL
   state$partial <- list()
   .rs.julia.debug("stopping Julia (pid %s)", if (is.null(pid)) "unknown" else pid)

   # closes the connection; a busy Julia won't notice, hence the kill
   if (requireNamespace("JuliaConnectoR", quietly = TRUE))
      tryCatch(JuliaConnectoR::stopJulia(), error = function(e) NULL)

   if (!is.null(pid))
      tools::pskill(pid, tools::SIGKILL)

   invisible(NULL)
})

.rs.addFunction("julia.onConnectionLost", function(reason = "Julia closed the connection")
{
   .rs.julia.debug("connection lost: %s", reason)
   .rs.julia.stop()
   stop(sprintf("The Julia session ended (%s).", reason), call. = FALSE)
})
