/*
 * SessionConnections.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#include "SessionConnections.hpp"

#include <boost/foreach.hpp>
#include <boost/algorithm/string/trim.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <core/Log.hpp>
#include <core/Error.hpp>
#include <core/Exec.hpp>

#include <session/SessionModuleContext.hpp>

#include "ConnectionList.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace connections {


namespace {

void onInstalledPackagesChanged()
{
   if (connectionsEnabled())
   {
      ClientEvent event(client_events::kEnableConnections);
      module_context::enqueClientEvent(event);
   }
}

} // anonymous namespace


bool connectionsEnabled()
{
   return module_context::isPackageInstalled("rspark");
}

Error initialize()
{
   // initialize connectionList
   initializeConnectionList();

   // connect to events to track whether we should enable connections
   module_context::events().onPackageLibraryMutated.connect(
                                             onInstalledPackagesChanged);

   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(sourceModuleRFile, "SessionConnections.R"));

   return initBlock.execute();
}


} // namespace connections
} // namespace modules
} // namesapce session
} // namespace rstudio

