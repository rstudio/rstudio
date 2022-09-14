/*
 * ServerMainOverlay.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <shared_core/Error.hpp>

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

void shutdown()
{
}

bool requireLocalR()
{
   return true;
} 

} // namespace overlay
} // namespace server
} // namespace rstudio
