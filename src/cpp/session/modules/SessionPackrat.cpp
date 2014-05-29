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

#include <r/RExec.hpp>

#include <session/projects/SessionProjects.hpp>
#include <session/SessionModuleContext.hpp>

#include "SessionPackages.hpp"

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

bool isPackratModeOn()
{
   // if we're not in a project, we don't support Packrat mode hooks
   if (!projects::projectContext().hasProject())
      return false;

   // if we are in a project, attempt to ascertain whether Packrat mode is on
   // for the project (it's OK if this fails; by default we presume packrat
   // mode to be off)
   bool packratMode = false;
   FilePath dir = projects::projectContext().directory();
   r::exec::RFunction("packrat:::isPackratModeOn", 
                      dir.absolutePath()).call(&packratMode);
   return packratMode;
}

bool isPackratManagedRPackage()
{
    // if we're not in a project, bail
    if (!projects::projectContext().hasProject())
        return false;

    // get the current working directory
    FilePath dir = projects::projectContext().directory();

    // bail if this isn't a package
    if (!core::r_util::isPackageDirectory(dir))
        return false;

    // check if the project is packified
    bool isPackratProject;
    r::exec::RFunction("packrat:::checkPackified",
                       dir.absolutePath()).call(&isPackratProject);
    return isPackratProject;
}

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

   initBlock.addFunctions()
      (bind(sourceModuleRFile, "SessionPackrat.R"));

   return initBlock.execute();
}

} // namespace packrat
} // namespace modules
} // namespace session

