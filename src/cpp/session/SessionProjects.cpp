/*
 * SessionProjects.cpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "SessionProjects.hpp"

#include <core/FilePath.hpp>
#include <core/Settings.hpp>
#include <core/system/System.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionUserSettings.hpp>

#include "SessionPersistentState.hpp"

using namespace core;

namespace session {

namespace {

FilePath s_activeProjectPath;

void onSuspend(Settings*)
{
   // on suspend write out current project path as the one to use
   // on resume. we read this back in initalize (rather than in
   // the onResume handler) becuase we need it very early in the
   // processes lifetime and onResume happens too late
   persistentState().setNextSessionProjectPath(s_activeProjectPath);
}

void onResume(const Settings&) {}

}  // anonymous namespace


namespace module_context {

FilePath activeProjectDirectory()
{
   if (!s_activeProjectPath.empty())
   {
      if (s_activeProjectPath.parent().exists())
         return s_activeProjectPath.parent();
      else
         return FilePath();
   }
   else
   {
      return FilePath();
   }
}


FilePath activeProjectFilePath()
{
   return s_activeProjectPath;
}

} // namespace module_context


namespace projects {

Error initialize()
{
   // register suspend handler
   using namespace module_context;
   addSuspendHandler(SuspendHandler(onSuspend, onResume));
   // see if there is a project path hard-wired for the next session
   // (this would be used for a switch to project or for the resuming of
   // a suspended session)
   FilePath nextSessionProjectPath = persistentState().nextSessionProjectPath();
   if (!nextSessionProjectPath.empty())
   {
      // reset next session project path so its a one shot deal
      persistentState().setNextSessionProjectPath(FilePath());

      // clear any initial context settings which may be leftover
      // by a re-instatiation of rsession by desktop
      session::options().clearInitialContextSettings();

      // check for existence and set
      if (nextSessionProjectPath.exists())
      {
         s_activeProjectPath = nextSessionProjectPath;
      }
      else
      {
         LOG_WARNING_MESSAGE("Next session project path doesn't exist: " +
                             nextSessionProjectPath.absolutePath());
         s_activeProjectPath = FilePath();
      }
   }

   // check for envrionment variable (file association)
   else if (!session::options().initialProjectPath().empty())
   {
      s_activeProjectPath = session::options().initialProjectPath();
   }

   // check for other working dir override (implies a launch of a file
   // but not of a project)
   else if (!session::options().initialWorkingDirOverride().empty())
   {
      s_activeProjectPath = FilePath();
   }

   // check for restore based on settings
   else if (userSettings().alwaysRestoreLastProject() &&
            userSettings().lastProjectPath().exists())
   {
      s_activeProjectPath = userSettings().lastProjectPath();
   }

   // else no active project for this session
   else
   {
      s_activeProjectPath = FilePath();
   }

   // save the active project path for the next session
   userSettings().setLastProjectPath(s_activeProjectPath);

   return Success();
}

} // namespace projects
} // namesapce session

