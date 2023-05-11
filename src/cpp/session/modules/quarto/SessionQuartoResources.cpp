/*
 * SessionQuartoResources.cpp
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

#include "SessionQuartoResources.hpp"

#include <core/PerformanceTimer.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionQuarto.hpp>

#include <session/prefs/UserState.hpp>

#define kQuartoResourcesPath "/quarto/resources/"

using namespace rstudio::core;
using namespace rstudio::session::quarto;
using namespace rstudio::session::module_context;

namespace rstudio {
namespace session {
namespace modules {
namespace quarto {
namespace resources {

namespace {

void handleQuartoResources(const http::Request& request, http::Response* pResponse)
{
   // determine path
   std::string path = http::util::pathAfterPrefix(request, kQuartoResourcesPath);

   // find the file and serve it
   QuartoConfig config = quartoConfig();
   FilePath resourcesPath(config.resources_path);
   FilePath quartoResource = resourcesPath.completeChildPath(path);

   pResponse->setCacheableFile(quartoResource, request);
}

} // anonymous namespace


Error initialize()
{
   QuartoConfig config = quartoConfig();
   if (config.enabled)
   {
      return module_context::registerUriHandler(kQuartoResourcesPath, handleQuartoResources);
   }
   else
   {
      return Success();
   }
}

} // namespace resources
} // namespace quarto
} // namespace modules
} // namespace session
} // namespace rstudio
