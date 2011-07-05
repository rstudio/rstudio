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
#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>
#include <core/system/System.hpp>
#include <core/r_util/RProjectFile.hpp>

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

namespace {

const int kStatusOk = 0;
const int kStatusNotExists = 1;
const int kStatusAlreadyExists = 2;
const int kStatusNoWriteAccess = 3;

json::Object createProjectResult(int status, const FilePath& projectFilePath)
{
   json::Object result;
   result["status"] = status;
   if (!projectFilePath.empty())
   {
      result["project_file_path"] = module_context::createAliasedPath(
                                                            projectFilePath);
   }
   else
   {
      result["project_file_path"] = json::Value();
   }
   return result;
}

json::Object createProjectResult(int status)
{
   return createProjectResult(status, FilePath());
}

bool canWriteToProjectDir(const FilePath& projectDirPath)
{
   FilePath testFile = projectDirPath.complete(core::system::generateUuid());
   Error error = core::writeStringToFile(testFile, "test");
   if (error)
   {
      return false;
   }
   else
   {
      error = testFile.removeIfExists();
      if (error)
         LOG_ERROR(error);

      return true;
   }
}

Error createProject(const json::JsonRpcRequest& request,
                    json::JsonRpcResponse* pResponse)
{
   // determine project dir path
   std::string projectDir;
   Error error = json::readParam(request.params, 0, &projectDir);
   if (error)
      return error;
   FilePath projectDirPath = module_context::resolveAliasedPath(projectDir);

   // NOTE: currently we assume that the specified directory already
   // exists (because it was chosen with the choose folder dialog). if
   // our client ui changes and it doesn't exist (likely) then this
   // code needs to auto-create it

   // verify that it doesn't already have a project
   FilePath existingProjFile = r_util::projectFromDirectory(projectDirPath);
   if (!existingProjFile.empty())
   {
      pResponse->setResult(createProjectResult(kStatusAlreadyExists));
      return Success();
   }

   // verify that we can write to the directory
   if (!canWriteToProjectDir(projectDirPath))
   {
      pResponse->setResult(createProjectResult(kStatusNoWriteAccess));
      return Success();
   }

   // create the project file
   FilePath projectFilePath = projectDirPath.complete(
                                    projectDirPath.filename() + ".Rproj");
   error = r_util::writeDefaultProjectFile(projectFilePath);
   if (error)
      return error;

   // return it
   pResponse->setResult(createProjectResult(kStatusOk, projectFilePath));
   return Success();
}


Error openProject(const json::JsonRpcRequest& request,
                 json::JsonRpcResponse* pResponse)
{
   // determine project dir path
   std::string projectDir;
   Error error = json::readParam(request.params, 0, &projectDir);
   if (error)
      return error;
   FilePath projectDirPath = module_context::resolveAliasedPath(projectDir);

   // verify that the directory exists
   if (!projectDirPath.exists())
   {
      pResponse->setResult(createProjectResult(kStatusNotExists));
      return Success();
   }

   // see if there is project file
   FilePath projectFilePath = r_util::projectFromDirectory(projectDirPath);
   if (projectFilePath.empty())
   {
      pResponse->setResult(createProjectResult(kStatusNotExists));
      return Success();
   }

   // test for write access
   if (!canWriteToProjectDir(projectDirPath))
   {
      pResponse->setResult(createProjectResult(kStatusNoWriteAccess));
      return Success();
   }

   // return the project file
   pResponse->setResult(createProjectResult(kStatusOk, projectFilePath));
   return Success();
}

}  // anonymous namespace


Error startup()
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

   // last ditch check for writeabilty of the project directory
   if (!s_activeProjectPath.empty() &&
       !canWriteToProjectDir(s_activeProjectPath.parent()))
   {
      // enque a warning
      json::Object warningBarEvent;
      warningBarEvent["severe"] = false;
      warningBarEvent["message"] =
        "Project '" + s_activeProjectPath.parent().absolutePath() + "' "
        "could not be opened because it is located in a read-only directory.";
      ClientEvent event(client_events::kShowWarningBar, warningBarEvent);
      module_context::enqueClientEvent(event);

      // no project
      s_activeProjectPath = FilePath();
   }


   // save the active project path for the next session
   userSettings().setLastProjectPath(s_activeProjectPath);

   return Success();
}

Error initialize()
{
   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "create_project", createProject))
      (bind(registerRpcMethod, "open_project", openProject))
   ;
   return initBlock.execute();
}

} // namespace projects
} // namesapce session

