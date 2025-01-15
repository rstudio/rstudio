/*
 * ServerMainOverlay.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

#include <shared_core/Error.hpp>
#include <set>

using namespace rstudio::core;

namespace rstudio {
namespace server {
namespace overlay {

Error initialize()
{
   return Success();
}

Error startup()
{
   return Success();
}

Error reloadConfiguration()
{
   return Success();
}

void startShutdown()
{
}

std::set<std::string> interruptProcs()
{
   return std::set<std::string>();
}

void shutdown()
{
}

bool requireLocalR()
{
   return true;
} 

bool isLoadBalanced()
{
   return false;
}
} // namespace overlay
} // namespace server
} // namespace rstudio
