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

// TODO: switch to file based semantics

// TODO: detecting copy/move/network:
/*
    - read INDEX
    - read id file

    - if INDEX contains id mapped to correct path then use

    - if INDEX doesn't not contain id or path then create new

    - if INDEX has id but path doesn't match then may have been
      a move or a copy so create a copy of the scratch dir and
      associate it with a new id

    - if has an id file but nothing in the index at all then
      create a brand new entry/dir using that id
*/



#include <session/projects/SessionProjects.hpp>

#include <core/FilePath.hpp>
#include <core/Settings.hpp>
#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>
#include <core/system/System.hpp>
#include <core/FileSerializer.hpp>
#include <core/r_util/RProjectFile.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionUserSettings.hpp>
#include <session/SessionPersistentState.hpp>

using namespace core;

namespace session {
namespace projects {

namespace {

FilePath s_projectFilePath;
FilePath s_projectDirectory;
FilePath s_projectScratchPath;

void onSuspend(Settings*)
{
   // on suspend write out current project path as the one to use
   // on resume. we read this back in initalize (rather than in
   // the onResume handler) becuase we need it very early in the
   // processes lifetime and onResume happens too late
   persistentState().setNextSessionProjectPath(s_projectFilePath);
}

void onResume(const Settings&) {}

FilePath determineActiveProjectScratchPath()
{
   // projects dir
   FilePath projDir = module_context::userScratchPath().complete("projects");
   Error error = projDir.ensureDirectory();
   if (error)
   {
      LOG_ERROR(error);
      return FilePath();
   }

   // read index file
   std::map<std::string,std::string> projectIndex;
   FilePath indexFilePath = projDir.complete("INDEX");
   if (indexFilePath.exists())
   {
      error = core::readStringMapFromFile(indexFilePath, &projectIndex);
      if (error)
      {
         LOG_ERROR(error);
         return FilePath();
      }
   }

   // look for this directory in the index file
   std::string projectId;
   FilePath projectDir = projects::projectDirectory();
   for (std::map<std::string,std::string>::const_iterator
         it = projectIndex.begin(); it != projectIndex.end(); ++it)
   {
      if (it->second == projectDir.absolutePath())
      {
         projectId = it->first;
         break;
      }
   }

   // if it wasn't found then generate a new entry and re-write the index
   if (projectId.empty())
   {
      std::string newId = core::system::generateUuid(false);
      projectIndex[newId] = projectDir.absolutePath();
      error = core::writeStringMapToFile(indexFilePath, projectIndex);
      if (error)
      {
         LOG_ERROR(error);
         return FilePath();
      }

      projectId = newId;
   }

   // now we have the id, use it to get the directory
   FilePath projectScratchPath = projDir.complete(projectId);
   error = projectScratchPath.ensureDirectory();
   if (error)
   {
      LOG_ERROR(error);
      return FilePath();
   }

   // return the path
   return projectScratchPath;
}

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


bool projectIsActive()
{
   return !projectFilePath().empty();
}

FilePath projectFilePath()
{
   return s_projectFilePath;
}

FilePath projectDirectory()
{
   return s_projectDirectory;
}

FilePath projectScratchPath()
{
   return s_projectScratchPath;
}

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
         s_projectFilePath = nextSessionProjectPath;
      }
      else
      {
         LOG_WARNING_MESSAGE("Next session project path doesn't exist: " +
                             nextSessionProjectPath.absolutePath());
         s_projectFilePath = FilePath();
      }
   }

   // check for envrionment variable (file association)
   else if (!session::options().initialProjectPath().empty())
   {
      s_projectFilePath = session::options().initialProjectPath();
   }

   // check for other working dir override (implies a launch of a file
   // but not of a project)
   else if (!session::options().initialWorkingDirOverride().empty())
   {
      s_projectFilePath = FilePath();
   }

   // check for restore based on settings
   else if (userSettings().alwaysRestoreLastProject() &&
            userSettings().lastProjectPath().exists())
   {
      s_projectFilePath = userSettings().lastProjectPath();
   }

   // else no active project for this session
   else
   {
      s_projectFilePath = FilePath();
   }

   // last ditch check for writeabilty of the project directory
   if (!s_projectFilePath.empty() &&
       !canWriteToProjectDir(s_projectFilePath.parent()))
   {
      // enque a warning
      json::Object warningBarEvent;
      warningBarEvent["severe"] = false;
      warningBarEvent["message"] =
        "Project '" + s_projectFilePath.parent().absolutePath() + "' "
        "could not be opened because it is located in a read-only directory.";
      ClientEvent event(client_events::kShowWarningBar, warningBarEvent);
      module_context::enqueClientEvent(event);

      // no project
      s_projectFilePath = FilePath();
   }

   // save the active project path for the next session
   userSettings().setLastProjectPath(s_projectFilePath);

   // if we have a project file then compute the directory & scratch path
   if (!s_projectFilePath.empty())
   {
      // project directory
      s_projectDirectory = s_projectFilePath.parent();

      // project scratch path (fault back to userScratch if for some
      // reason we can't determine the project scratch path)
      s_projectScratchPath = determineActiveProjectScratchPath();
      if (s_projectScratchPath.empty())
         s_projectScratchPath = module_context::userScratchPath();
   }

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

