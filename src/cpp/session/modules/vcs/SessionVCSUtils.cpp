/*
 * SessionVCSUtils.cpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
#include "SessionVCSUtils.hpp"

#include <core/json/Json.hpp>
#include <session/SessionModuleContext.hpp>

using namespace core;

namespace session {
namespace modules {
namespace vcs_utils {

void enqueRefreshEventWithDelay(int delay)
{
   // Sometimes on commit, the subsequent request contains outdated
   // status (i.e. as if the commit had not happened yet). No idea
   // right now what is causing this. Add a delay for commits to make
   // sure the correct state is shown.

   json::Object data;
   data["delay"] = delay;
   module_context::enqueClientEvent(ClientEvent(client_events::kVcsRefresh,
                                                data));
}

void enqueueRefreshEvent()
{
   enqueRefreshEventWithDelay(0);
}

core::json::Object processResultToJson(
      const core::system::ProcessResult& result)
{
   core::json::Object obj;
   obj["output"] = result.stdOut;
   obj["exit_code"] = result.exitStatus;
   return obj;
}

} // namespace vcs_utils
} // namespace modules
} // namespace session
