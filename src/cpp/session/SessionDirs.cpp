/*
 * SessionDirs.cpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

#include "SessionDirs.hpp"

#include <r/session/RSession.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionUserSettings.hpp>
#include <session/projects/SessionProjects.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace dirs {

FilePath getDefaultWorkingDirectory()
{
   // calculate using user settings
   FilePath defaultWorkingDir = userSettings().initialWorkingDirectory();

   // return it if it exists, otherwise use the default user home path
   if (defaultWorkingDir.exists() && defaultWorkingDir.isDirectory())
      return defaultWorkingDir;
   else
      return session::options().userHomePath();
}

FilePath getInitialWorkingDirectory()
{
   // check for a project
   if (projects::projectContext().hasProject())
   {
      return projects::projectContext().directory();
   }

   // check for working dir in project none
   else if (options().sessionScope().isProjectNone())
   {
      // if this is the initial session then use the default working directory
      // (reset initial to false so this is one shot thing)
      using namespace module_context;
      if (activeSession().initial())
      {
         activeSession().setInitial(false);
         return getDefaultWorkingDirectory();
      }

      FilePath workingDirPath = module_context::resolveAliasedPath(
                      module_context::activeSession().workingDir());
      if (workingDirPath.exists())
         return workingDirPath;
      else
         return getDefaultWorkingDirectory();
   }

   // see if there is an override from the environment (perhaps based
   // on a folder drag and drop or other file association)
   FilePath workingDirPath = session::options().initialWorkingDirOverride();
   if (workingDirPath.exists() && workingDirPath.isDirectory())
   {
      return workingDirPath;
   }
   else
   {
      // if not then just return default working dir
      return getDefaultWorkingDirectory();
   }
}

FilePath getProjectUserDataDir(const ErrorLocation& location)
{
   // get the project directory
   FilePath projectDir = projects::projectContext().directory();

   // presume that the project dir is the data dir then check
   // for a .Ruserdata directory as an alternative
   FilePath dataDir = projectDir;

   FilePath ruserdataDir = projectDir.childPath(".Ruserdata");
   if (ruserdataDir.exists())
   {
      // create user-specific subdirectory if necessary
      FilePath userDir = ruserdataDir.childPath(core::system::username());
      Error error = userDir.ensureDirectory();
      if (!error)
      {
         dataDir = userDir;
      }
      else
      {
         core::log::logError(error, location);
      }
   }

   // return the data dir
   return dataDir;
}

// NOTE: mirrors behavior of WorkbenchContext.getREnvironmentPath on the client
FilePath rEnvironmentDir()
{
   // for projects
   if (projects::projectContext().hasProject())
   {
      return getProjectUserDataDir(ERROR_LOCATION);
   }

   // for desktop the current path
   else if (session::options().programMode() == kSessionProgramModeDesktop)
   {
      return FilePath::safeCurrentPath(session::options().userHomePath());
   }

   // for server the initial working dir
   else
   {
      return getInitialWorkingDirectory();
   }
}

FilePath rHistoryDir()
{
   // for projects
   if (projects::projectContext().hasProject())
   {
      return getProjectUserDataDir(ERROR_LOCATION);
   }

   // for server we use the default working directory
   else if (session::options().programMode() == kSessionProgramModeServer)
   {
      return getDefaultWorkingDirectory();
   }

   // for desktop we take the current path
   else
   {
      return FilePath::safeCurrentPath(session::options().userHomePath());
   }
}

} // namespace dirs
} // namespace session
} // namespace rstudio


