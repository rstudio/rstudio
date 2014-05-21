/*
 * SessionPackrat.cpp
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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

#include "SessionPackrat.hpp"

#include <core/Exec.hpp>

#include <core/system/FileMonitor.hpp>

#include <session/projects/SessionProjects.hpp>

#include <session/SessionModuleContext.hpp>

using namespace core;

namespace session {
namespace modules { 
namespace packrat {

namespace {

void onFileChanged(FilePath sourceFilePath)
{
   // if the changed file path is in the Packrat library folder, 
   // we may need to initiate a snapshot 
   
   // if the changed file is an R source file, we may need to test for
   // changes to the list of used packages
   
   // if the changed file is the Packrat lock file, we may need to tell
   // the client to ask the user to initiate a restore
   std::cerr << "Packrat file change detected: " 
             << sourceFilePath.absolutePath() << std::endl;
}

void onFilesChanged(const std::vector<core::system::FileChangeEvent>& changes)
{
   BOOST_FOREACH(const core::system::FileChangeEvent& fileChange, changes)
   {
      FilePath changedFilePath(fileChange.fileInfo().absolutePath());
      onFileChanged(changedFilePath);
   }
}

} // anonymous namespace

Error initialize()
{
   using boost::bind;
   using namespace module_context;

   // listen for changes to the project files 
   session::projects::FileMonitorCallbacks cb;
   cb.onFilesChanged = onFilesChanged;
   projects::projectContext().subscribeToFileMonitor("Packrat", cb);
   module_context::events().onSourceEditorFileSaved.connect(onFileChanged);

   ExecBlock initBlock;
   /*

   add e.g. RPC handlers and R file source commands to perform on init

   initBlock.addFunctions()
      (bind(sourceModuleRFile, "SessionPackrat.R"));
   */

   return initBlock.execute();
}

} // namespace packrat
} // namespace modules
} // namespace session

