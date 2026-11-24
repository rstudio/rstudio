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

include("png.jl")

# Inputs evaluated so far; backtraces name them REPL[n], as the Julia REPL does.
const input_count = Ref(0)

# Lines a value's display may use before it is truncated.
const DISPLAY_LINES = 40

# Tells R that an evaluation is over (see `finish_evaluation`).
const evaluation_done = Ref{Any}(nothing)

# Shows an image in the Plots pane: an R function (see `ConsoleDisplay`).
const plot_callback = Ref{Any}(nothing)

# Where an evaluation's values are shown as text: stdout, with the console's
# width and colors (see `evaluate`).
const value_io = Ref{Union{IO, Nothing}}(nothing)

"""
    RStudio.plot_background[]

What the Plots pane fills the space around an image with, where the image's
shape doesn't match the pane's:

- `:auto` (the default): the image's border color, when the border is one
  color, as a plot's background is; otherwise black or white, whichever is
  nearer the border's average lightness. A transparent border is left alone.
- `nothing`: the pane's own background.
- a color R understands, such as `"black"` or `"#1e1e1e"`.
"""
const plot_background = Ref{Any}(:auto)

# How long to wait for an interrupt R sent that hasn't landed yet, in seconds.
const INTERRUPT_GRACE = 0.25

# On Windows, whether a console interrupt may reach Julia (see
# `install_console_handler`): only during an evaluation.
const accept_interrupts = Threads.Atomic{Bool}(false)

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

# Trace a message, given in parts, to stderr, which reaches the console in step
# with R's own. Callers pass parts rather than interpolating them: building a
# string allocates, and an allocation is where an interrupt can land. One
# landing here is dropped, so that tracing can't change where interrupts go;
# `finish_evaluation` then waits out its grace period. So is a failed write.
function debug(parts...)
   debugging[] || return
   try
      t = time()
      stamp = Libc.strftime("%H:%M:%S", t) * "." * lpad(floor(Int, 1000 * (t % 1)), 3, '0')
      println(stderr, "[julia-debug Julia ", stamp, "] ", parts...)
   catch
   end
end

"""
    init(done, communicator, rpid, show_plot)

Prepare a freshly started Julia process for the console. `done` is an R
function, called when an evaluation is over; it returns the number of
interrupts R sent during the evaluation. `communicator` is JuliaConnectoR's
connection to R, and `rpid` R's process ID. `show_plot` is an R function that
draws an image's pixels on R's graphics device (see `ConsoleDisplay`).
"""
function init(done, communicator, rpid, show_plot)
   # leave R's process group: RStudio interrupts R by signalling the whole
   # group, and a SIGINT that Julia doesn't expect breaks its connection to
   # R. R sends Julia its own (see .rs.julia.interrupt in SessionJulia.R)
   if Sys.isunix()
      ccall(:setpgid, Cint, (Cint, Cint), 0, 0)
      start_watchdog(Int(rpid))
   elseif Sys.iswindows()
      install_console_handler()
   end

   evaluation_done[] = done
   plot_callback[] = show_plot
   forward_output(communicator)
   install_display()

   # serve.jl has every message JuliaConnectoR writes (a result, a failure, a
   # callback) wait for the output written before it; see drain_output
   if isdefined(Main, :rstudio_before_message)
      Main.rstudio_before_message[] = drain_output
   end

   # Pkg warns that its REPL mode is for interactive use, since Julia runs
   # here as a script; the console's ']' commands are typed interactively
   if isdefined(Pkg, :REPLMode) && isdefined(Pkg.REPLMode, :PRINTED_REPL_WARNING)
      Pkg.REPLMode.PRINTED_REPL_WARNING[] = true
   end

   # strings, which JuliaConnectoR translates (it passes a tuple as a proxy)
   return [string(VERSION), string(getpid()), joinpath(Sys.BINDIR, Base.julia_exename())]
end

# Run on a thread of the console's, which Julia adopts; nonzero means handled.
# Only CTRL_C_EVENT (0) and CTRL_BREAK_EVENT (1) are interrupts: the console
# closing, logoff and shutdown go on to Julia's handler.
function console_event(event::UInt32)::Cint
   return event <= 1 && !accept_interrupts[] ? Cint(1) : Cint(0)
end

"""
    install_console_handler()

Let only interrupts meant for an evaluation reach Julia, on Windows. Julia
shares R's console there (system2() starts it without one of its own), and
RStudio interrupts R by sending CTRL_BREAK to the whole console: that reaches
Julia too, which interrupts an evaluation as it should, but one landing while
Julia waits for R breaks JuliaConnectoR's connection. Windows calls the
console's handlers newest first, so this one, added after Julia's own, drops
the event unless an evaluation is running. (Windows has no SIGINT for
another process, so R sends nothing itself; see .rs.julia.interrupt.)
"""
function install_console_handler()
   # a 32-bit handler needs stdcall, which @cfunction can't declare
   Sys.WORD_SIZE == 64 || return

   handler = @cfunction(console_event, Cint, (UInt32,))
   ccall((:SetConsoleCtrlHandler, "kernel32"), stdcall, Cint, (Ptr{Cvoid}, Cint), handler, 1)
   return nothing
end

"""
    start_watchdog(rpid)

Make sure Julia ends with R. An idle Julia ends when its connection to R
closes, but a busy one doesn't read it, and nothing else ends it: Julia isn't
R's child (system2() starts it through a shell), nor in R's process group. So
a shell in Julia's process group waits for R to go, then kills the group. It
ignores the interrupts R sends the group, and Julia ends it on a normal exit;
if Julia is killed instead, the group outlives it with the watchdog in it, so
the ID the watchdog kills can't have gone to another process.
"""
function start_watchdog(rpid::Int)
   pgid = ccall(:getpgrp, Cint, ())
   pgid == getpid() || return

   # 'kill -s KILL': dash (/bin/sh on Debian and Ubuntu) rejects the '--'
   # in 'kill -KILL -- -pgid', and the group would be left running
   script = """
      trap '' INT
      while kill -0 $rpid 2>/dev/null; do sleep 1; done
      kill -s KILL -- -$pgid
      """
   watchdog = run(pipeline(`/bin/sh -c $script`; stdin = devnull, stdout = devnull, stderr = devnull); wait = false)

   atexit() do
      try
         kill(watchdog)
      catch
      end
   end
end

"""
    forward_output(communicator)

Send stdout and stderr to R, in place of JuliaConnectoR's forwarding.

An interrupt lands in whichever task is running on the main thread. One that
landed in a forwarding task would end it (as JuliaConnectoR's forwarding
does), or, passed on to the evaluation with schedule(), race the event loop
waking the evaluation from a write, which ends the forwarding too: either way
the evaluation then blocks once its output fills the pipe, and never sees the
interrupt. So the forwarders run on a thread other than the main one, where no
interrupt is delivered, and defer interrupts besides; without a spare thread,
they run on the main thread, and an interrupt waits for the evaluation's task.
Messages to R then wait for the output before them to be forwarded (see
`drain_output`).
"""
function forward_output(communicator)
   RConnector = parentmodule(typeof(communicator))

   # JuliaConnectoR's pipes; closing them ends its forwarding tasks
   previous = (stdout, stderr)

   out, out_writer = redirect_stdout()
   err, err_writer = redirect_stderr()
   Logging.global_logger(Logging.SimpleLogger(stderr))

   for stream in previous
      try
         close(stream)
      catch
      end
   end

   empty!(forwarders)
   for (pipe, writer, indicator) in ((out, out_writer, RConnector.STDOUT_INDICATOR), (err, err_writer, RConnector.STDERR_INDICATOR))
      f = Forwarder(pipe, writer, indicator)
      f.task = start_forwarder(() -> forward(RConnector, communicator, f))
      push!(forwarders, f)
   end
   return nothing
end

# Start a forwarder off the main thread (thread 1, which interrupts are
# delivered to). Best is an interactive thread of its own (R starts Julia with
# one; see .rs.julia.runServer): tasks that user code spawns run on the
# default threads, where a long computation that never yields would hold the
# forwarder up. Failing that, the pool the main thread isn't in; failing that,
# the main thread itself.
function start_forwarder(forward::Function)
   for tid in 2:Threads.maxthreadid()
      Threads.threadpool(tid) === :interactive || continue
      task = Task(forward)
      task.sticky = true
      ccall(:jl_set_task_tid, Cint, (Any, Cint), task, tid - 1)
      return schedule(task)
   end

   main = Threads.threadpool(1)
   if main !== :default && Threads.nthreads(:default) > 0
      return Threads.@spawn :default forward()
   elseif main !== :interactive && Threads.nthreads(:interactive) > 0
      return Threads.@spawn :interactive forward()
   else
      return @async forward()
   end
end

# One output stream's forwarding: the pipe it reads, the stream writing to that
# pipe, and the sync marks (see `drain_output`) written to it and forwarded.
mutable struct Forwarder
   const pipe::Base.PipeEndpoint
   const writer::IO
   const indicator::UInt8
   const synced::Threads.Atomic{Int}
   const ack::Base.Event
   marked::Int
   task::Union{Task, Nothing}
end

Forwarder(pipe, writer, indicator) =
   Forwarder(pipe, writer, indicator, Threads.Atomic{Int}(0), Base.Event(true), 0, nothing)

const forwarders = Forwarder[]

# Marks a point in a stream's output (see `drain_output`). It starts with a
# NUL, which R drops from output anyway, so output is only held back (in case
# a mark was split across reads) when it ends with one.
const SYNC_MARK = Vector{UInt8}(codeunits("\0RStudio.drain_output\0"))

# How long to wait for the output to be forwarded, in seconds.
const DRAIN_TIMEOUT = 5.0

"""
    drain_output()

Wait until the output written so far has been sent to R. The forwarders run
on a thread of their own, where the yield() with which JuliaConnectoR lets
output go ahead of a result doesn't let them run first; so the end of an
evaluation, and every message to R (see serve.jl), waits for them, lest it
overtake the output. A mark is written to each stream, and its forwarder
acknowledges the mark once the output before it has been sent.
"""
function drain_output()
   Libc.flush_cstdio()

   pending = Forwarder[]
   for f in forwarders
      (f.task === nothing || istaskdone(f.task)) && continue

      # one that hasn't caught up with an earlier mark is held up somehow:
      # waiting for it again would hold up every message
      f.synced[] < f.marked && continue

      try
         write(f.writer, SYNC_MARK)
         f.marked += 1
         push!(pending, f)
      catch err
         debug("couldn't mark the output: ", typeof(err))
      end
   end
   isempty(pending) && return

   # wakes the wait below when the time is up
   timer = Timer(_ -> foreach(f -> notify(f.ack), pending), DRAIN_TIMEOUT)
   deadline = time() + DRAIN_TIMEOUT
   try
      for f in pending
         while f.synced[] < f.marked && time() < deadline
            wait(f.ack)
         end
         if f.synced[] < f.marked
            debug("output not forwarded within ", DRAIN_TIMEOUT, "s")
         end
      end
   finally
      close(timer)
   end
end

# JuliaConnectoR's messages are written under its lock, so the forwarders'
# output can't interleave with a result written from the main thread.
function forward(RConnector, communicator, f::Forwarder)
   Base.disable_sigint() do
      held = UInt8[]
      while isopen(communicator.io) && isopen(f.pipe)
         bytes = readavailable(f.pipe)
         isempty(bytes) && continue
         held = forward_chunk(RConnector, communicator, f, isempty(held) ? bytes : vcat(held, bytes))
      end
   end
end

# Send output up to each sync mark in it, acknowledging the mark. Returns the
# end of the output if it could be the start of a mark split across reads,
# which is held back until more is read.
function forward_chunk(RConnector, communicator, f::Forwarder, bytes::Vector{UInt8})
   start = 1
   while (range = findnext(SYNC_MARK, bytes, start)) !== nothing
      send_output(RConnector, communicator, f, bytes, start, first(range) - 1)
      start = last(range) + 1
      Threads.atomic_add!(f.synced, 1)
      notify(f.ack)
   end

   held = partial_mark_length(bytes, start)
   send_output(RConnector, communicator, f, bytes, start, length(bytes) - held)
   return bytes[end - held + 1:end]
end

function send_output(RConnector, communicator, f::Forwarder, bytes, first, last)
   first <= last || return
   RConnector.write_output_message(communicator, bytes[first:last], f.indicator)
end

# The length of the longest end of `bytes` (from `start` on) that begins a mark.
function partial_mark_length(bytes::Vector{UInt8}, start::Int)
   n = length(bytes)
   for k in min(length(SYNC_MARK) - 1, n - start + 1):-1:1
      view(bytes, n - k + 1:n) == view(SYNC_MARK, 1:k) && return k
   end
   return 0
end

"""
    ConsoleDisplay()

Shows values as the Julia REPL's display does, but for RStudio: an image in
the Plots pane, anything else as text in the console. It sits on top of
Julia's display stack (see `install_display`), so it shows both the values
the console prints and those user code passes to `display()`; a display that
user code or a package pushes later goes above it, as in the Julia REPL.
"""
struct ConsoleDisplay <: AbstractDisplay end

const PNG = MIME("image/png")
const TEXT = MIME("text/plain")

Base.Multimedia.displayable(::ConsoleDisplay, ::MIME"text/plain") = true
Base.Multimedia.displayable(::ConsoleDisplay, ::MIME"image/png") = plot_callback[] !== nothing

function Base.display(d::ConsoleDisplay, @nospecialize(x))
   if displayable(d, PNG) && showable(PNG, x)
      display(d, PNG, x)
   else
      display(d, TEXT, x)
   end
   return nothing
end

function Base.display(::ConsoleDisplay, mime::MIME"text/plain", @nospecialize(x))
   io = something(value_io[], stdout)
   show(io, mime, x)
   println(io)
   return nothing
end

# R draws the image on its graphics device, from pixels rather than the PNG,
# so it needs no PNG reader (see png.jl, and .rs.julia.showPlot).
function Base.display(d::ConsoleDisplay, mime::MIME"image/png", @nospecialize(x))
   callback = plot_callback[]
   if callback === nothing
      throw(MethodError(display, (d, mime, x)))
   end

   buffer = IOBuffer()
   show(buffer, mime, x)
   pixels, width, height = decode_png(take!(buffer))
   background = background_color(plot_background[], pixels, width, height)

   path = tempname()
   try
      write(path, pixels)

      # Julia waits for R's reply while R draws, and an interrupt landing in
      # that wait would leave the reply unread, breaking the connection; so
      # one waits until the reply is in, and interrupts the evaluation then
      Base.disable_sigint() do
         Base.invokelatest(callback, path, width, height, background)
      end
   finally
      rm(path; force = true)
   end
   return nothing
end

# The color to fill around an image with, for R: "" for none. See
# `plot_background`.
function background_color(setting, pixels::Vector{Int32}, width::Int, height::Int)
   if setting === :auto
      return something(border_color(pixels, width, height), "")
   elseif setting === nothing
      return ""
   elseif setting isa AbstractString
      return String(setting)
   else
      throw(ArgumentError("RStudio.plot_background[] must be :auto, nothing, or a color name or hex string"))
   end
end

# A packed nativeRaster pixel's red, green, blue and alpha.
function unpack_rgba(pixel::Int32)
   p = reinterpret(UInt32, pixel)
   return (Int(p & 0xff), Int((p >> 8) & 0xff), Int((p >> 16) & 0xff), Int(p >> 24))
end

# How far a sample may stray from the border's most common color and still
# count as that color: antialiasing, dithering, a stray mark.
const BORDER_TOLERANCE = 8

# The share of the border that must be one color for that to be the image's
# background; a plot's frame or a legend may touch the edge.
const BORDER_UNIFORM_SHARE = 0.9

# The color around an image, as "#rrggbb", from its border pixels; nothing
# for a border that's mostly transparent, where the pane's own background
# already shows through.
function border_color(pixels::Vector{Int32}, width::Int, height::Int)
   border = Int32[]
   for x in 1:width
      push!(border, pixels[x], pixels[(height - 1) * width + x])
   end
   for y in 2:height - 1
      push!(border, pixels[(y - 1) * width + 1], pixels[y * width])
   end

   opaque = [unpack_rgba(p)[1:3] for p in border if unpack_rgba(p)[4] >= 0xf8]
   if 2 * length(opaque) < length(border)
      return nothing
   end

   counts = Dict{NTuple{3, Int}, Int}()
   for rgb in opaque
      counts[rgb] = get(counts, rgb, 0) + 1
   end
   _, common = findmax(counts)

   near = count(rgb -> maximum(abs.(rgb .- common)) <= BORDER_TOLERANCE, opaque)
   rgb = if near >= BORDER_UNIFORM_SHARE * length(opaque)
      common
   else
      # sRGB luma, against the midpoint
      lightness = sum(((r, g, b),) -> 0.2126r + 0.7152g + 0.0722b, opaque) / length(opaque)
      lightness < 128 ? (0, 0, 0) : (255, 255, 255)
   end

   return "#" * join(string.(rgb; base = 16, pad = 2))
end

# Put the console's display on top of Julia's display stack. Julia's own
# text display, below it, writes to the stdout Julia started with, which
# `forward_output` has replaced: it is made to follow the new one.
function install_display()
   displays = Base.Multimedia.displays
   for i in eachindex(displays)
      if displays[i] isa Base.TextDisplay
         displays[i] = Base.TextDisplay(stdout)
      end
   end

   filter!(d -> !(d isa ConsoleDisplay), displays)
   pushdisplay(ConsoleDisplay())
   return nothing
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
"module", "keyword", "path" or "other"), then the modules to look up their
documentation in (see `documentation`), all in one vector of strings, which
JuliaConnectoR translates (it passes a tuple as a proxy).
"""
function completions(line::AbstractString)
   line = String(line)
   C = REPL.REPLCompletions
   results, range, _ = C.completions(line, lastindex(line), Main)
   token = isempty(range) ? "" : line[first(range):end]

   names = String[]
   kinds = String[]
   sources = String[]
   for result in results
      # a call's method signatures: help, but not text to insert
      result isa C.MethodCompletion && continue

      # newer Julia versions keep a backslash completion's text in a field
      # that completion_text doesn't read
      text = hasfield(typeof(result), :completion) ? result.completion : C.completion_text(result)
      push!(names, text)
      push!(kinds, completion_kind(result))
      push!(sources, completion_source(result))
   end

   return String[token; names; kinds; sources]
end

# The module to look up a completion's documentation in, as `string` names it;
# "" for one that isn't a binding, such as a field, whose name alone would
# find some other binding's documentation.
function completion_source(result)
   C = REPL.REPLCompletions
   if result isa C.ModuleCompletion
      return string(result.parent)
   elseif result isa C.KeywordCompletion
      return "Main"
   end
   return ""
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
      # a macro is a function, but isn't called with parentheses
      startswith(result.mod, "@") && return "other"

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
    documentation(name, source)

The documentation for `name` in the module `source` names (as `completions`
gives it), as `?name` shows it at the Julia REPL: the signature it begins
with, if any, and then the rest, as markdown. Both are empty when `source`
names no module.
"""
function documentation(name::AbstractString, source::AbstractString)
   name = String(name)
   mod = find_module(String(source))
   mod === nothing && return String["", ""]

   docs = Core.eval(mod, REPL.helpmode(devnull, name, mod))
   text = sprint(show, MIME("text/markdown"), docs)

   # cross-references only resolve in Documenter's builds
   text = replace(text, r"\[([^\]]*)\]\(@ref[^)]*\)" => s"\1")

   # a leading code block that isn't a signature, such as an example, stays;
   # one of several signatures stays too, with the others. (Julia 1.10 shows
   # a docstring's indented code without the language that later ones name.)
   signature = ""
   m = match(r"^```(?:julia)?\n((?:[^\n]*\n)*?)```\n*", text)
   if m !== nothing
      lines = split(chomp(m.captures[1]), '\n')
      if is_signature(lines[1], name)
         # the first signature, which can wrap over several lines
         n = 1
         while n < length(lines) && !balanced(join(lines[1:n]))
            n += 1
         end

         if balanced(join(lines[1:n]))
            signature = join(strip.(lines[1:n]), " ")
            if n == length(lines)
               text = replace(text, m.match => ""; count = 1)
            end
         end
      end
   end

   return String[signature, text]
end

balanced(text) = count(==('('), text) == count(==(')'), text)

# Does `line` show how `name` is used: called, or alone (as a keyword is)?
function is_signature(line::AbstractString, name::String)
   return line == name || startswith(line, name * "(") || startswith(line, name * "{")
end

# The module that `path` names, as `string` gives a module's name, or nothing.
# A package's module is found among those loaded, as it may be bound in Main
# under another name, or not at all.
function find_module(path::String)
   isempty(path) && return nothing

   parts = split(path, '.')
   root = Symbol(parts[1])
   mod = root === :Main ? Main : nothing
   if mod === nothing
      loaded = Base.loaded_modules_array()
      index = findfirst(m -> nameof(m) === root, loaded)
      index === nothing && return nothing
      mod = loaded[index]
   end

   for part in parts[2:end]
      name = Symbol(part)
      isdefined(mod, name) || return nothing
      mod = getglobal(mod, name)
      mod isa Module || return nothing
   end

   return mod
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
   interrupted = 0
   before = ""
   accept_interrupts[] = true
   try
      # set up within the protected region too; and move first, as pwd()
      # throws while Julia's working directory is one that has been deleted.
      # A directory that can't be entered leaves Julia where it is
      if isdir(cwd)
         try
            cd(cwd)
         catch err
            err isa InterruptException && rethrow()
         end
      end
      before = pwd()

      value_io[] = IOContext(stdout, :limit => true, :displaysize => (DISPLAY_LINES, Int(width)), :module => Main, :color => color)
      errio = IOContext(stderr, :color => color)

      interrupted += run_input(String(code), String(mode), errio)
   catch err
      # an interrupt that landed outside the input's own evaluation
      err isa InterruptException || rethrow()
      interrupted += 1
   finally
      # R sends interrupts until `finish_evaluation` tells it to stop: from
      # here on they wait for it to count them, and it is told even when the
      # input's error couldn't be reported
      Base.sigatomic_begin()
      accept_interrupts[] = false
      value_io[] = nothing
      debug("evaluation over; ", interrupted, " interrupt(s) received")
      try
         drain_output()
      catch err
         debug("couldn't drain the output: ", typeof(err))
      end
      finish_evaluation(interrupted)
   end

   # an interrupt during setup leaves no input to have moved Julia; nor is
   # there a directory to follow once the input has deleted Julia's own
   after = try
      pwd()
   catch
      ""
   end
   if isempty(before) || isempty(after) || after == before
      return ""
   end
   return after
end

# Evaluate and show the value, returning the number of interrupts this caught.
# The value goes through the display stack, as in the Julia REPL, which lets a
# plot reach the Plots pane (see `ConsoleDisplay`).
function run_input(code::String, mode::String, errio::IO)
   try
      value = evaluate_mode(code, mode)
      if value !== nothing && !(mode == "julia" && REPL.ends_with_semicolon(code))
         Base.invokelatest(display, value)
      end
      return 0
   catch err
      # An interrupt that arrives while the error is reported (a second press,
      # say) would cut the report short, and leave nothing to show for the
      # first: it waits until the report is out, and is counted with the rest
      Base.sigatomic_begin()
      late = 0
      try
         stack = Base.ExceptionStack(NamedTuple[
            (exception = entry.exception, backtrace = scrub_backtrace(entry.backtrace))
            for entry in Base.current_exceptions()
         ])
         Base.invokelatest(Base.display_error, errio, stack)
      finally
         late = take_deferred_interrupt()
      end

      err isa InterruptException && debug("the evaluation was interrupted")
      return (err isa InterruptException ? 1 : 0) + late
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
# so, with interrupts deferred (see `evaluate`), R is told that the evaluation
# is over and stops sending them; any it sent before then is consumed here.
function finish_evaluation(received::Int)
   done = evaluation_done[]
   if done === nothing
      take_deferred_interrupt()
      return
   end

   sent = try
      Int(first(Base.invokelatest(done)))
   catch
      Base.sigatomic_end()
      rethrow()
   end
   debug("R sent ", sent, " interrupt(s)")
   received += take_deferred_interrupt()

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
      debug("gave up waiting for ", sent - received, " interrupt(s)")
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
