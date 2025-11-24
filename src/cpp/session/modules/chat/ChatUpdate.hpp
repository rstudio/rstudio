/*
 * ChatUpdate.hpp
 *
 * Copyright (C) 2025 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef RSTUDIO_SESSION_MODULES_CHAT_UPDATE_HPP
#define RSTUDIO_SESSION_MODULES_CHAT_UPDATE_HPP

namespace rstudio {
namespace core {
class Error;
} // namespace core
} // namespace rstudio

namespace rstudio {
namespace core {
namespace json {
struct JsonRpcRequest;
class JsonRpcResponse;
} // namespace json
} // namespace core
} // namespace rstudio

namespace rstudio {
namespace session {
namespace modules {
namespace chat {

/**
 * Check for updates on startup.
 *
 * This function is called during session initialization to check if a newer
 * version of the Posit AI package is available. The check is silent and non-blocking.
 *
 * @return Success() always (errors are logged but don't fail initialization)
 */
core::Error checkForUpdatesOnStartup();

/**
 * RPC handler: Check if an update is available.
 *
 * Returns cached results from the startup check.
 *
 * @param request RPC request
 * @param pResponse RPC response with update information
 * @return Success() or error
 */
core::Error chatCheckForUpdates(const core::json::JsonRpcRequest& request,
                                core::json::JsonRpcResponse* pResponse);

/**
 * RPC handler: Install an available update.
 *
 * Downloads and installs the update, stopping the backend if running.
 * This is a synchronous operation that may take some time.
 *
 * @param request RPC request
 * @param pResponse RPC response
 * @return Success() or error
 */
core::Error chatInstallUpdate(const core::json::JsonRpcRequest& request,
                              core::json::JsonRpcResponse* pResponse);

/**
 * RPC handler: Get the current update installation status.
 *
 * @param request RPC request
 * @param pResponse RPC response with status information
 * @return Success() or error
 */
core::Error chatGetUpdateStatus(const core::json::JsonRpcRequest& request,
                                core::json::JsonRpcResponse* pResponse);

} // end namespace chat
} // end namespace modules
} // end namespace session
} // end namespace rstudio

#endif /* RSTUDIO_SESSION_MODULES_CHAT_UPDATE_HPP */
