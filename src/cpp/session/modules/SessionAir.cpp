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

#include <shared_core/FilePath.hpp>

#include <core/Exec.hpp>

#include <session/projects/SessionProjects.hpp>
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

FilePath findAirToml(const core::FilePath& documentPath)
{
   // If the document lives within the active project, then first
   // check for air.toml in the project directory.
   if (projects::projectContext().hasProject())
   {
      FilePath projectPath = projects::projectContext().directory();
      if (documentPath.isWithin(projectPath))
      {
         for (auto&& suffix : { "air.toml", ".air.toml" })
         {
            FilePath airPath = projectPath.completePath(suffix);
            if (airPath.exists())
               return airPath;
         }

         return FilePath();
      }
   }

   // Otherwise, the user might be editing a file that belongs to
   // a different project. Check and see if the document lives in
   // a path that contains an air.toml in a parent directory somewhere.
   //
   // Avoid traversing into or beyond the home directory.
   FilePath parentPath = documentPath.getParent();
   FilePath homePath = module_context::userHomePath();
   FilePath homeParent = homePath.getParent();
   while (true)
   {
      for (auto&& suffix : { "air.toml", ".air.toml" })
      {
         FilePath airPath = parentPath.completePath(suffix);
         if (airPath.exists())
            return airPath;
      }

      if (parentPath.getParent() == homeParent)
         return FilePath();

      auto&& path = parentPath.getAbsolutePath();

#ifdef _WIN32
      std::replace(path.begin(), path.end(), '\\', '/');
#endif

      auto count = std::count(path.begin(), path.end(), '/');
      if (count <= 2)
         return FilePath();

      parentPath = parentPath.getParent();
   }
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
