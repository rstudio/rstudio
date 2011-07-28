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

#include <boost/format.hpp>

#include <core/FileSerializer.hpp>

#include <session/SessionUserSettings.hpp>
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
   // ensure project user dir
   FilePath projectUserDir = projectFile.parent().complete(".Rproj.user");
   if (!projectUserDir.exists())
   {
      // create
      Error error = projectUserDir.ensureDirectory();
      if (error)
         return error;

      // mark hidden if we are on win32
#ifdef _WIN32
      error = core::system::makeFileHidden(projectUserDir);
      if (error)
         return error;
#endif
   }

   // create user subdirectory if we have a username
   std::string username = core::system::username();
   if (!username.empty())
   {
      projectUserDir = projectUserDir.complete(username);
      Error error = projectUserDir.ensureDirectory();
      if (error)
         return error;
   }

   // now add context id to form scratch path
   FilePath scratchPath = projectUserDir.complete(userSettings().contextId());
   Error error = scratchPath.ensureDirectory();
   if (error)
      return error;

   // return the path
   *pScratchPath = scratchPath;
   return Success();
}

}  // anonymous namespace


void ProjectContext::indexProjectFile(const FilePath& filePath)
{
   if (filePath.extensionLowerCase() == ".r")
   {
      // read the file (assumes utf8)
      std::string code;
      Error error = core::readStringFromFile(filePath,
                                             &code,
                                             string_utils::LineEndingPosix);
      if (error)
      {
         LOG_ERROR(error);
         return;
      }

      // compute project relative directory (used for context)
      std::string context = filePath.relativePath(directory_);

      // index the source
      sourceIndexes_.push_back(boost::shared_ptr<r_util::RSourceIndex>(
                                 new r_util::RSourceIndex(context, code)));
   }
}

void ProjectContext::indexProjectFiles()
{
    Error error = directory_.childrenRecursive(
                    boost::bind(&ProjectContext::indexProjectFile, this, _2));
    if (error)
       LOG_ERROR(error);
}

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

   // calculate project scratch path
   FilePath scratchPath;
   Error error = computeScratchPath(projectFile, &scratchPath);
   if (error)
   {
      *pUserErrMsg = "unable to initialize project - " + error.summary();
      return error;
   }

   // read project file config
   r_util::RProjectConfig config;
   error = r_util::readProjectFile(projectFile, &config, pUserErrMsg);
   if (error)
      return error;

   // initialize members
   file_ = projectFile;
   directory_ = file_.parent();
   scratchPath_ = scratchPath;
   config_ = config;

   // initialize source index
   if (userSettings().indexingEnabled())
      indexProjectFiles();

   // return success
   return Success();

}


} // namespace projects
} // namesapce session

