#
# serve.jl
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

# The console's Julia server: JuliaConnectoR's, as its main.jl starts it, for
# a client that first sends the token R gave it. R starts it in place of
# main.jl (.rs.julia.runServer in SessionJulia.R).
#
# Arguments: JuliaConnectoR's Julia directory, a port hint, and the file to
# write the port to. The token comes in RSTUDIO_JULIA_TOKEN.

import Sockets

const connector_dir = ARGS[1]
const port_hint = parse(Int, ARGS[2])
const portfile = ARGS[3]

# not passed on to the processes Julia runs
const token = Vector{UInt8}(pop!(ENV, "RSTUDIO_JULIA_TOKEN", ""))
isempty(token) && error("no token was given for the Julia server")

# how long a client has to send the token; each is checked in a task of its own
const TOKEN_TIMEOUT = 10

# R connects as soon as the port file is written; nothing to wait for after
const CONNECTION_TIMEOUT = 60

# as main.jl
import Pkg
if VERSION < v"1.4"
   if get(Pkg.installed(), "Tables", v"0.0") < v"1.0"
      include(joinpath(connector_dir, "dummy_tables.jl"))
      @eval import .Tables
   else
      @eval import Tables
   end
else
   try
      @eval import Tables
   catch ex
      include(joinpath(connector_dir, "dummy_tables.jl"))
      @eval import .Tables
   end
end

using InteractiveUtils

include(joinpath(connector_dir, "RConnector.jl"))

# JuliaConnectoR sends output ahead of each message it writes to R (a result,
# a failure, a callback) by yielding to its output forwarders. RStudio's run
# on a thread of their own, and RStudio sets this to a function that waits for
# them instead (RStudio.drain_output). The method is defined here, rather than
# by RStudio, as serve_repl() runs in the world it was called in.
const rstudio_before_message = Ref{Any}(nothing)

function RConnector.start_result_message(c::RConnector.CommunicatoR{Sockets.TCPSocket})
   before = rstudio_before_message[]
   before === nothing || Base.invokelatest(before)
   return invoke(RConnector.start_result_message, Tuple{RConnector.CommunicatoR}, c)
end

function has_token(sock, token)
   timer = Timer(_ -> close(sock), TOKEN_TIMEOUT)
   try
      received = read(sock, length(token))
      return length(received) == length(token) &&
         reduce(|, xor.(received, token); init = 0x00) == 0x00
   catch
      return false
   finally
      close(timer)
   end
end

function serve_console(port_hint, portfile, token)
   port, server = Sockets.listenany(Sockets.localhost, port_hint)

   # as RConnector.serve: an interrupt throws, rather than ending Julia
   ccall(:jl_exit_on_sigint, Cvoid, (Cint,), 0)

   client = Channel{Sockets.TCPSocket}(1)
   @async while isopen(server)
      sock = try
         Sockets.accept(server)
      catch
         break
      end
      @async has_token(sock, token) ? put!(client, sock) : close(sock)
   end

   # renamed into place, so that R never reads it part-written
   partial = portfile * ".partial"
   open(partial, "w") do io
      write(io, string(Int(port)), '\n')
   end
   mv(partial, portfile; force = true)

   deadline = Timer(CONNECTION_TIMEOUT) do _
      println(stderr, "No client connected to the Julia server in time.")
      exit(1)
   end
   sock = take!(client)
   close(deadline)
   close(server)

   if isdefined(Sockets, :nagle)
      try
         Sockets.nagle(sock, false)
      catch
      end
   end

   # in the main task, as RConnector.serve does; it ends Julia once R leaves
   RConnector.serve_repl(sock)
end

serve_console(port_hint, portfile, token)
