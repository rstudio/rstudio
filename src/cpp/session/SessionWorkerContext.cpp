/*
 * SessionWorkerContext.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#include <string>

#include <shared_core/Error.hpp>
#include <core/Thread.hpp>
#include <core/json/JsonRpc.hpp>

#include <session/SessionClientEvent.hpp>
#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace worker_context {

// Worker RPC methods don't hold up an HTTP connection while the operation
// executes. Instead, they return immediately and provide the results later,
// using the client event queue.
Error registerWorkerRpcMethod(const std::string& name,
                              const json::JsonRpcFunction& function)
{
   return module_context::registerRpcMethod(name,
                                            boost::bind(module_context::executeAsync,
                                                        function,
                                                        _1,
                                                        _2));
}

} // namespace worker_context
} // namespace session
} // namespace rstudio
