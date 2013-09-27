/*
 * SessionShiny.cpp
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

#include "SessionShiny.hpp"

#include <core/Error.hpp>

#include <r/RExec.hpp>

#include <session/SessionOptions.hpp>
#include <session/SessionModuleContext.hpp>

using namespace core;

namespace session {
namespace modules { 
namespace shiny {

namespace {

void onPackageLoaded(const std::string& pkgname)
{
   // we need an up to date version of shiny when running in server mode
   // to get the websocket protocol/path and port randomizing changes
   if (session::options().programMode() == kSessionProgramModeServer)
   {
      if (pkgname == "shiny")
      {
         // version check
         bool hasRequiredVersion = false;
         Error error = r::exec::evaluateString(
              ".rs.getPackageVersion('shiny') > 0.7", &hasRequiredVersion);
         if (error)
            LOG_ERROR(error);

         // print warning if necessary
         if (!hasRequiredVersion)
         {
            module_context::consoleWriteError("\nWARNING: To run Shiny "
              "applications with RStudio Server you need to install the "
              "latest version of the Shiny package from Github. You can "
              "do this using the devtools package as follows:\n\n"
              "   install.packages('devtools')\n"
              "   devtools::install_github('shiny', 'rstudio')\n\n");
         }
      }
   }
}

} // anonymous namespace



Error initialize()
{
   module_context::events().onPackageLoaded.connect(onPackageLoaded);

   return Success();
}


} // namespace crypto
} // namespace modules
} // namesapce session

