/*
 * SessionConsoleProcessApi.hpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef SESSION_CONSOLE_PROCESS_API_HPP
#define SESSION_CONSOLE_PROCESS_API_HPP

#include <session/SessionConsoleProcess.hpp>

#include <r/RRoutines.hpp>
#include <r/RExec.hpp>

#include "SessionConsoleProcessTable.hpp"

namespace rstudio {
namespace core {
   class Error;
}
}

namespace rstudio {
namespace session {
namespace console_process {

// ------------------------------------------------------------------------
// RPC API implementations
// ------------------------------------------------------------------------

namespace rpc {

core::Error procStart(const core::json::JsonRpcRequest& request, core::json::JsonRpcResponse* pResponse);
core::Error procInterrupt(const core::json::JsonRpcRequest& request, core::json::JsonRpcResponse* pResponse);
core::Error procReap(const core::json::JsonRpcRequest& request, core::json::JsonRpcResponse* pResponse);
core::Error procWriteStdin(const core::json::JsonRpcRequest& request, core::json::JsonRpcResponse* pResponse);
core::Error procSetSize(const core::json::JsonRpcRequest& request, core::json::JsonRpcResponse* pResponse);
core::Error procSetCaption(const core::json::JsonRpcRequest& request, core::json::JsonRpcResponse* pResponse);
core::Error procSetTitle(const core::json::JsonRpcRequest& request, core::json::JsonRpcResponse* pResponse);
core::Error procEraseBuffer(const core::json::JsonRpcRequest& request, core::json::JsonRpcResponse* pResponse);
core::Error procGetBufferChunk(const core::json::JsonRpcRequest& request, core::json::JsonRpcResponse* pResponse);
core::Error procUseRpc(const core::json::JsonRpcRequest& request, core::json::JsonRpcResponse* pResponse);

// Determine if a given process handle exists in the table; used by client
// to detect stale consoleprocs.
core::Error procTestExists(const core::json::JsonRpcRequest& request, core::json::JsonRpcResponse* pResponse);

// Notification from client of currently-selected terminal.
core::Error procNotifyVisible(const core::json::JsonRpcRequest& request, core::json::JsonRpcResponse* pResponse);

} // rpc API

// ------------------------------------------------------------------------
// R API implementations
// ------------------------------------------------------------------------

namespace api {

// Return vector of all terminal ids (captions)
SEXP rs_getAllTerminals();

// Create a terminal with given id (caption). If null, create with automatically
// generated name. Returns resulting name in either case.
SEXP rs_createNamedTerminal(SEXP typeSEXP);

// Returns busy state of a terminal (i.e. does the shell have any child
// processes?)
SEXP rs_isTerminalBusy(SEXP terminalsSEXP);

// Returns running state of a terminal (i.e. does the shell have a shell process?)
SEXP rs_isTerminalRunning(SEXP terminalsSEXP);

// Returns bunch of metadata about a terminal instance.
SEXP rs_getTerminalContext(SEXP terminalSEXP);

// Return buffer for a terminal, optionally stripping out Ansi codes.
SEXP rs_getTerminalBuffer(SEXP idSEXP, SEXP stripSEXP);

// Kill terminal and its processes.
SEXP rs_killTerminal(SEXP terminalsSEXP);

// Get last-selected terminal as reported by client
SEXP rs_getVisibleTerminal();

// Clear terminal buffer
SEXP rs_clearTerminal(SEXP idSEXP);

// Send text to the terminal
SEXP rs_sendToTerminal(SEXP idSEXP, SEXP textSEXP);

// Activate a terminal to ensure it is running (and optionally visible).
SEXP rs_activateTerminal(SEXP idSEXP, SEXP showSEXP);

} // R API

} // namespace console_process
} // namespace session
} // namespace rstudio

#endif // SESSION_CONSOLE_PROCESS_API_HPP

