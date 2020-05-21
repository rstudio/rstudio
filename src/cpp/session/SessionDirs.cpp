/*
 * SessionDirs.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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
#include <session/projects/SessionProjects.hpp>
#include <session/prefs/UserPrefs.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace dirs {

FilePath getDefaultWorkingDirectory()
{
   // see if the user has defined a default working directory in preferences
   FilePath defaultWorkingDir;
   std::string initialWorkingDir = prefs::userPrefs().initialWorkingDirectory();
   if (!initialWorkingDir.empty())
   {
      // the user has defined a default; resolve the path
      defaultWorkingDir = module_context::resolveAliasedPath(initialWorkingDir);
   }

   // see if there's a working directory defined in the R session options (set by
   // session-default-working-dir)
   FilePath sessionDefaultWorkingDir = FilePath(session::options().defaultWorkingDir());

   // return the first of these directories that is defined and exists, or the user home directory
   // in the case that neither exists
   if (defaultWorkingDir.exists() && defaultWorkingDir.isDirectory())
      return defaultWorkingDir;
   else if (sessionDefaultWorkingDir.exists() && sessionDefaultWorkingDir.isDirectory())
      return sessionDefaultWorkingDir;
   else
      return session::options().userHomePath();
}

FilePath getActiveSessionInitialWorkingDirectory()
{
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
      return getActiveSessionInitialWorkingDirectory();
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
      return getActiveSessionInitialWorkingDirectory();
   }
}

FilePath getProjectUserDataDir(const ErrorLocation& location)
{
   // get the project directory
   FilePath projectDir = projects::projectContext().directory();

   // presume that the project dir is the data dir then check
   // for a .Ruserdata directory as an alternative
   FilePath dataDir = projectDir;

   FilePath ruserdataDir = projectDir.completeChildPath(".Ruserdata");
   if (ruserdataDir.exists())
   {
      // create user-specific subdirectory if necessary
      FilePath userDir = ruserdataDir.completeChildPath(core::system::username());
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


