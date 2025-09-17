/*
 * SessionAir.cpp
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

#include "SessionAir.hpp"

#include <boost/bind.hpp>

#include <core/Exec.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace air {

bool hasAirToml(const FilePath& projectPath)
{
   for (auto&& suffix : { "air.toml", ".air.toml" })
   {
      FilePath airPath = projectPath.completePath(suffix);
      if (airPath.exists())
         return true;
   }

   return false;
}

core::Error initialize()
{
   using boost::bind;
   using namespace module_context;

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(sourceModuleRFile, "SessionAir.R"));
   return initBlock.execute();
}

} // end namespace air
} // end namespace modules
} // end namespace session
} // end namespace rstudio
