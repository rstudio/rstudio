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

#include <shared_core/json/Json.hpp>
#include <shared_core/FilePath.hpp>

#include <core/Exec.hpp>
#include <core/system/FileChangeEvent.hpp>

#include <session/projects/SessionProjects.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/SessionClientEvent.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace air {

namespace {

bool isAirTomlFile(const FilePath& filePath)
{
   std::string filename = filePath.getFilename();
   return filename == "air.toml" || filename == ".air.toml";
}

FilePath findProjectAirToml()
{
   if (!projects::projectContext().hasProject())
      return FilePath();
   
   FilePath projectDir = projects::projectContext().directory();
   for (const std::string& filename : { "air.toml", ".air.toml" })
   {
      FilePath airTomlPath = projectDir.completePath(filename);
      if (airTomlPath.exists())
         return airTomlPath;
   }
   
   return FilePath();
}

void fireMonitoredFileChangedEvent(const FilePath& filePath, 
                                   core::system::FileChangeEvent::Type type)
{
   // Get the relative (aliased) path
   std::string path = module_context::createAliasedPath(filePath);
   
   // Create the event data
   json::Object eventData;
   eventData["path"] = path;
   eventData["type"] = static_cast<int>(type);
   
   // Emit the client event
   ClientEvent clientEvent(client_events::kMonitoredFileChanged, eventData);
   module_context::enqueClientEvent(clientEvent);
}

void onFilesChanged(const std::vector<core::system::FileChangeEvent>& events)
{
   for (const auto& event : events)
   {
      FilePath filePath(event.fileInfo().absolutePath());
      
      // Only process air.toml or .air.toml files
      if (isAirTomlFile(filePath))
         fireMonitoredFileChangedEvent(filePath, event.type());
   }
}

void onClientInit()
{
   // Notify client of existing air.toml during client initialization
   FilePath airTomlPath = findProjectAirToml();
   if (airTomlPath.exists())
      fireMonitoredFileChangedEvent(airTomlPath, core::system::FileChangeEvent::FileModified);
}

} // end anonymous namespace

FilePath getAirTomlPath(const FilePath& projectPath)
{
   for (auto&& suffix : { "air.toml", ".air.toml" })
   {
      FilePath airTomlPath = projectPath.completePath(suffix);
      if (airTomlPath.exists())
         return airTomlPath;
   }

   return FilePath();
}

FilePath findAirTomlPath(const core::FilePath& documentPath)
{
   // If the document lives within the active project, then first
   // check for air.toml in the project directory.
   if (projects::projectContext().hasProject())
   {
      FilePath projectPath = projects::projectContext().directory();
      if (documentPath.isWithin(projectPath))
      {
         FilePath airPath = findProjectAirToml();
         if (!airPath.isEmpty())
            return airPath;
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

   // Subscribe to client init event to notify client of existing air.toml
   events().onClientInit.connect(onClientInit);

   // Subscribe to file monitor for air.toml changes
   if (projects::projectContext().hasProject())
   {
      projects::FileMonitorCallbacks callbacks;
      callbacks.onFilesChanged = onFilesChanged;
      projects::projectContext().subscribeToFileMonitor("Air", callbacks);
   }

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(sourceModuleRFile, "SessionAir.R"));
   return initBlock.execute();
}

} // end namespace air
} // end namespace modules
} // end namespace session
} // end namespace rstudio
