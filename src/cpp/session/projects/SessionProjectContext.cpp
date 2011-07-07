/*
 * SessionProjectContext.cpp
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

#include <session/projects/SessionProjects.hpp>

#include <map>

#include <core/FileSerializer.hpp>

#include <session/SessionModuleContext.hpp>

using namespace core;

namespace session {
namespace projects {

namespace {

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

Error computeScratchPath(const FilePath& projectFile, FilePath* pScratchPath)
{
   // projects dir
   FilePath projDir = module_context::userScratchPath().complete("projects");
   Error error = projDir.ensureDirectory();
   if (error)
      return error;

   // read index file
   std::map<std::string,std::string> projectIndex;
   FilePath indexFilePath = projDir.complete("INDEX");
   if (indexFilePath.exists())
   {
      error = core::readStringMapFromFile(indexFilePath, &projectIndex);
      if (error)
         return error;
   }

   // look for this directory in the index file
   std::string projectId;
   for (std::map<std::string,std::string>::const_iterator
         it = projectIndex.begin(); it != projectIndex.end(); ++it)
   {
      if (it->second == projectFile.absolutePath())
      {
         projectId = it->first;
         break;
      }
   }

   // if it wasn't found then generate a new entry and re-write the index
   if (projectId.empty())
   {
      std::string newId = core::system::generateUuid(false);
      projectIndex[newId] = projectFile.absolutePath();
      error = core::writeStringMapToFile(indexFilePath, projectIndex);
      if (error)
         return error;

      projectId = newId;
   }

   // now we have the id, use it to get the directory
   FilePath projectScratchPath = projDir.complete(projectId);
   error = projectScratchPath.ensureDirectory();
   if (error)
      return error;

   // return the path
   *pScratchPath = projectScratchPath;
   return Success();
}

}  // anonymous namespace


Error ProjectContext::initialize(const FilePath& projectFile,
                                 std::string* pUserErrMsg)
{
   // test for project file existence
   if (!projectFile.exists())
   {
      *pUserErrMsg = "the project file does not exist";
      return pathNotFoundError(projectFile.absolutePath(), ERROR_LOCATION);
   }

   // test for writeabilty of parent
   if (!canWriteToProjectDir(projectFile.parent()))
   {
      *pUserErrMsg = "the project directory is not writeable";
      return systemError(boost::system::errc::permission_denied,
                         ERROR_LOCATION);
   }

   // calculate project scratch path (fault back to userScratch if for some
   // reason we can't determine the project scratch path)
   FilePath scratchPath;
   Error error = computeScratchPath(projectFile, &scratchPath);
   if (error)
   {
      LOG_ERROR(error);
      scratchPath = module_context::userScratchPath();
   }

   // read project file config
   r_util::RProjectConfig config;
   error = r_util::readProjectFile(projectFile, &config, pUserErrMsg);
   if (error)
      return error;

   // initialize members and return success
   file_ = projectFile;
   directory_ = file_.parent();
   scratchPath_ = scratchPath;
   config_ = config;
   return Success();
}

} // namespace projects
} // namesapce session

