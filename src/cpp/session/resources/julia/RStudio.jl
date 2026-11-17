#
# RStudio.jl
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

# Support for RStudio's Julia console. This is loaded into the Julia process
# that JuliaConnectoR starts for the R session (see SessionJulia.R), and is
# called from R over JuliaConnectoR's connection.
module RStudio

import Logging
import Pkg
import REPL

# Inputs evaluated so far; backtraces name them REPL[n], as the Julia REPL does.
const input_count = Ref(0)

# Lines a value's display may use before it is truncated.
const DISPLAY_LINES = 40

# Tells R that an evaluation is over (see `finish_evaluation`).
const evaluation_done = Ref{Any}(nothing)

# The task evaluating input, while it is (see `redirect_interrupt`).
const evaluation_task = Ref{Union{Task, Nothing}}(nothing)

# How long to wait for an interrupt R sent that hasn't landed yet, in seconds.
const INTERRUPT_GRACE = 0.25

# Whether to trace the console's side of things (see `debug`).
const debugging = Ref(false)

"""
    set_debugging(enabled)

Turn tracing on or off; R does so to follow the 'rstudio.julia.debuggingEnabled'
option (see .rs.julia.debug in SessionJulia.R).
"""
function set_debugging(enabled::Bool)
   debugging[] = enabled
   return nothing
end

# Trace a message to stderr, which reaches the console in step with R's own.
# An interrupt landing meanwhile is dropped, so that tracing can't change
# where interrupts go; `finish_evaluation` then waits out its grace period.
function debug(message)
   debugging[] || return
   t = time()
   stamp = Libc.strftime("%H:%M:%S", t) * "." * lpad(floor(Int, 1000 * (t % 1)), 3, '0')
   try
      println(stderr, "[julia-debug Julia ", stamp, "] ", message)
   catch err
      err isa InterruptException || rethrow()
   end
end

"""
    init(done, communicator)

Prepare a freshly started Julia process for the console. `done` is an R
function, called when an evaluation is over; it returns the number of
interrupts R sent during the evaluation. `communicator` is JuliaConnectoR's
connection to R.
"""
function init(done, communicator)
   evaluation_done[] = done
   forward_output(communicator)

   # Pkg warns that its REPL mode is for interactive use, since Julia runs
   # here as a script; the console's ']' commands are typed interactively
   if isdefined(Pkg, :REPLMode) && isdefined(Pkg.REPLMode, :PRINTED_REPL_WARNING)
      Pkg.REPLMode.PRINTED_REPL_WARNING[] = true
   end

   # strings, which JuliaConnectoR translates (it passes a tuple as a proxy)
   return [string(VERSION), string(getpid()), joinpath(Sys.BINDIR, Base.julia_exename())]
end

"""
    forward_output(communicator)

Send stdout and stderr to R, in place of JuliaConnectoR's forwarding.

SIGINT lands in whichever task is running, which during an evaluation that
prints is often the one forwarding its output. JuliaConnectoR's would die
there, and output would stop; the evaluation then blocks once the pipe fills,
and never sees the interrupt. These pass it on to the evaluation instead, and
write each message with interrupts deferred, as one cut short would break the
connection.
"""
function forward_output(communicator)
   RConnector = parentmodule(typeof(communicator))

   # JuliaConnectoR's pipes; closing them ends its forwarding tasks
   previous = (stdout, stderr)

   out, _ = redirect_stdout()
   err, _ = redirect_stderr()
   Logging.global_logger(Logging.SimpleLogger(stderr))

   for stream in previous
      try
         close(stream)
      catch
      end
   end

   @async forward(RConnector, communicator, out, RConnector.STDOUT_INDICATOR)
   @async forward(RConnector, communicator, err, RConnector.STDERR_INDICATOR)
   return nothing
end

function forward(RConnector, communicator, pipe, indicator)
   while isopen(communicator.io) && isopen(pipe)
      bytes = try
         readavailable(pipe)
      catch err
         err isa InterruptException || rethrow()
         redirect_interrupt()
         continue
      end

      isempty(bytes) && continue

      try
         Base.disable_sigint() do
            RConnector.write_output_message(communicator, bytes, indicator)
         end
      catch err
         err isa InterruptException || rethrow()
         redirect_interrupt()
      end
   end
end

# An interrupt meant for the evaluation landed in another task; pass it on.
function redirect_interrupt()
   task = evaluation_task[]
   if task !== nothing && task !== current_task() && !istaskdone(task)
      debug("interrupt landed in the output forwarder; passing it on to the evaluation")
      try
         schedule(task, InterruptException(); error = true)
      catch
      end
   else
      debug("interrupt landed in the output forwarder, with no evaluation to pass it on to")
   end
end

"""
    iscomplete(code)

Is `code` complete input, as the Julia REPL judges it? An open block, string
or bracket makes it incomplete; a syntax error does not, and is reported when
the input is evaluated.
"""
function iscomplete(code::AbstractString)
   ast = Base.parse_input_line(String(code); depwarn = false)
   return !(ast isa Expr && ast.head === :incomplete)
end

"""
    completions(line)

Completions for the end of `line`, as the Julia REPL offers them on Tab: the
token they replace, then their names, then their kinds ("function",
"module", "keyword", "path" or "other"), all in one vector of strings, which
JuliaConnectoR translates (it passes a tuple as a proxy).
"""
function completions(line::AbstractString)
   line = String(line)
   C = REPL.REPLCompletions
   results, range, _ = C.completions(line, lastindex(line), Main)
   token = isempty(range) ? "" : line[first(range):end]

   names = String[]
   kinds = String[]
   for result in results
      # a call's method signatures: help, but not text to insert
      result isa C.MethodCompletion && continue

      push!(names, result isa C.BslashCompletion ? result.completion : C.completion_text(result))
      push!(kinds, completion_kind(result))
   end

   return String[token; names; kinds]
end

function completion_kind(result)
   C = REPL.REPLCompletions
   if result isa C.KeywordCompletion
      return "keyword"
   elseif result isa C.PathCompletion
      return "path"
   elseif result isa C.PackageCompletion
      return "module"
   elseif result isa C.ModuleCompletion
      name = Symbol(result.mod)
      value = try
         Base.invokelatest(isdefined, result.parent, name) ? Base.invokelatest(getglobal, result.parent, name) : nothing
      catch
         nothing
      end
      return value isa Function ? "function" : value isa Module ? "module" : "other"
   end
   return "other"
end

"""
    evaluate(code, mode, width, color, cwd)

Evaluate one input the way the Julia REPL would in `mode` ("julia", "help",
"pkg" or "shell"), printing its value or error.

R and Julia run in separate processes; `cwd` is R's working directory, which
Julia moves to first, so that relative paths mean the same thing in both.
Returns Julia's working directory if the input changed it, and an empty
string otherwise.
"""
function evaluate(code::AbstractString, mode::AbstractString, width::Integer, color::Bool, cwd::AbstractString)
   if isdir(cwd) && cwd != pwd()
      cd(cwd)
   end
   before = pwd()

   io = IOContext(stdout, :limit => true, :displaysize => (DISPLAY_LINES, Int(width)), :module => Main, :color => color)
   errio = IOContext(stderr, :color => color)

   interrupted = 0
   evaluation_task[] = current_task()
   try
      interrupted += run_input(String(code), String(mode), io, errio)
   catch err
      # an interrupt that landed outside the input's own evaluation
      err isa InterruptException || rethrow()
      interrupted += 1
   finally
      evaluation_task[] = nothing
   end

   debug("evaluation over; $interrupted interrupt(s) received")
   finish_evaluation(interrupted)

   flush(stdout)
   flush(stderr)
   return pwd() == before ? "" : pwd()
end

# Evaluate and print, returning the number of interrupts this caught.
function run_input(code::String, mode::String, io::IO, errio::IO)
   try
      value = evaluate_mode(code, mode)
      if value !== nothing && !(mode == "julia" && REPL.ends_with_semicolon(code))
         Base.invokelatest(show, io, MIME("text/plain"), value)
         println(io)
      end
      return 0
   catch err
      stack = Base.ExceptionStack(NamedTuple[
         (exception = entry.exception, backtrace = scrub_backtrace(entry.backtrace))
         for entry in Base.current_exceptions()
      ])
      Base.invokelatest(Base.display_error, errio, stack)
      err isa InterruptException && debug("the evaluation was interrupted")
      return err isa InterruptException ? 1 : 0
   end
end

# Drop the frames below the input's own -- Core.eval, this module's and
# JuliaConnectoR's -- as the Julia REPL drops its own. An interrupt that
# landed outside the input keeps none.
function scrub_backtrace(backtrace)
   frames = stacktrace(backtrace)

   index = findfirst(frame -> parentmodule(frame) === @__MODULE__, frames)
   index === nothing && return frames

   if index > 1 && frames[index - 1].func === :eval
      index -= 1
   end

   return frames[1:index - 1]
end

function evaluate_mode(code::String, mode::String)
   if mode == "help"
      ast = REPL.helpmode(stdout, code, Main)
   elseif mode == "pkg"
      return Base.invokelatest(Pkg.REPLMode.pkgstr, code)
   elseif mode == "shell"
      ast = Expr(:call, Base.repl_cmd, Expr(:call, Base.cmd_gen, Base.shell_parse(code)[1]), stdout)
   else
      # soft scope, as in the Julia REPL: a top-level loop assigns globals
      input_count[] += 1
      ast = REPL.softscope(Base.parse_input_line(code; filename = "REPL[$(input_count[])]"))
   end

   value = Core.eval(Main, ast)
   if mode == "julia"
      Core.eval(Main, Expr(:(=), :ans, QuoteNode(value)))
   end

   return value
end

# R interrupts an evaluation by sending SIGINT. One that lands after the
# evaluation, in JuliaConnectoR's own code, would break the connection to R:
# so R is told that the evaluation is over, and stops sending them, while
# they are deferred; any it sent before then is consumed here.
function finish_evaluation(received::Int)
   done = evaluation_done[]
   done === nothing && return

   Base.sigatomic_begin()
   sent = try
      Int(first(Base.invokelatest(done)))
   catch
      Base.sigatomic_end()
      rethrow()
   end
   received += take_deferred_interrupt()
   debug("R sent $sent interrupt(s); $received received")

   # a signal R sent may still be on its way
   deadline = time() + INTERRUPT_GRACE
   while received < sent && time() < deadline
      try
         sleep(0.005)
      catch err
         err isa InterruptException || rethrow()
         received += 1
         debug("a late interrupt landed, and was consumed")
      end
   end

   if received < sent
      debug("gave up waiting for $(sent - received) interrupt(s)")
   end
end

# Leave the deferred region; an interrupt deferred within it is thrown here.
function take_deferred_interrupt()
   try
      Base.sigatomic_end()
      return 0
   catch err
      err isa InterruptException || rethrow()
      return 1
   end
end

end # module RStudio
