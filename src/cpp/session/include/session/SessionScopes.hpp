/*
 * SessionScopes.hpp
 *
 * Copyright (C) 2009-2015 by RStudio, Inc.
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

// Everything is defined inine here so we can share code between
// rsession and rworkspace without linking

#ifndef SESSION_SCOPES_HPP
#define SESSION_SCOPES_HPP

#include <boost/bind.hpp>
#include <boost/foreach.hpp>

#include <core/FilePath.hpp>
#include <core/FileSerializer.hpp>
#include <core/StringUtils.hpp>
#include <core/system/System.hpp>
#include <core/json/Json.hpp>
#include <core/json/JsonRpc.hpp>

#ifndef _WIN32
#include <core/system/FileMode.hpp>
#endif

#include <core/r_util/RSessionContext.hpp>

#include <session/projects/ProjectsSettings.hpp>
#include <session/projects/SessionProjectSharing.hpp>

namespace rstudio {
namespace session {

namespace {

inline core::FilePath projectIdsFilePath(const core::FilePath& userScratchPath)
{
   core::FilePath filePath = userScratchPath.childPath(
                                    kProjectsSettings "/project-id-mappings");
   core::Error error = filePath.parent().ensureDirectory();
   if (error)
      LOG_ERROR(error);
   return filePath;
}

inline std::map<std::string,std::string> projectIdsMap(
                                       const core::FilePath& projectIdsPath)
{
   std::map<std::string,std::string> idMap;
   if (projectIdsPath.exists())
   {
      core::Error error = core::readStringMapFromFile(projectIdsPath, &idMap);
      if (error)
         LOG_ERROR(error);
   }
   return idMap;
}

inline std::string toFilePath(const core::r_util::ProjectId& projectId,
                              const core::FilePath& userScratchPath,
                              const core::FilePath& sharedStoragePath)
{
   // try the map first; it contains both our own projects and shared projects
   // that we've opened
   core::FilePath projectIdsPath = projectIdsFilePath(userScratchPath);
   std::map<std::string,std::string> idMap = projectIdsMap(projectIdsPath);
   std::map<std::string,std::string>::iterator it;

   // see if the project came from another user
   bool fromOtherUser = !projectId.userId().empty() &&
            projectId.userId() != core::r_util::obfuscatedUserId(::getuid());

   // if it did, use the fully qualified name; otherwise, use just the project
   // ID (our own projects are stored unqualified in the map)
   if (fromOtherUser)
      it = idMap.find(projectId.asString());
   else
      it = idMap.find(projectId.id());

   if (it != idMap.end())
   {
      // we found it!
      return it->second;
   }
   else if (fromOtherUser)
   {
      // this project does not belong to us; see if it has an entry in shared
      // storage
      core::FilePath projectEntryPath =
            sharedStoragePath.complete(kProjectSharedDir)
                             .complete(projectId.asString() + kProjectEntryExt);
      if (projectEntryPath.exists())
      {
         // get the path from shared storage
         std::string entryContents;
         core::Error error = core::readStringFromFile(projectEntryPath,
                                                      &entryContents);
         if (error)
         {
            LOG_ERROR(error);
            return "";
         }

         // read the contents
         core::json::Value projectEntryVal;
         if (!core::json::parse(entryContents, &projectEntryVal))
         {
            error = core::Error(core::json::errc::ParseError,
                                      ERROR_LOCATION);
            error.addProperty("path", projectEntryPath.absolutePath());
            LOG_ERROR(error);
            return "";
         }

         // extract the path
         std::string projectPath;
         if (projectEntryVal.type() == core::json::ObjectType)
         {
            core::json::Object obj = projectEntryVal.get_obj();
            core::json::Object::iterator it = obj.find(kProjectEntryDir);
            if (it != obj.end() && it->second.type() == core::json::StringType)
            {
               projectPath = it->second.get_str();
            }
         }

         // ensure we got a path from the shared project data
         if (projectPath.empty())
         {
            error = core::systemError(boost::system::errc::invalid_argument,
                             "No project directory found in " kProjectEntryDir,
                             ERROR_LOCATION);
            error.addProperty("path", projectEntryPath.absolutePath());
            LOG_ERROR(error);
            return "";
         }

         // save the path to our own mapping so we can reverse lookup later
         core::FilePath projectIdsPath = projectIdsFilePath(userScratchPath);
         std::map<std::string,std::string> idMap = projectIdsMap(projectIdsPath);
         idMap[projectId.asString()] = projectPath;
         error = core::writeStringMapToFile(projectIdsPath, idMap);
         if (error)
            LOG_ERROR(error);

         // return the path
         return projectPath;
      }
   }
   return "";
}

inline core::r_util::ProjectId toProjectId(const std::string& projectDir,
                             const core::FilePath& userScratchPath)
{
   // get the id map
   core::FilePath projectIdsPath = projectIdsFilePath(userScratchPath);
   std::map<std::string,std::string> idMap = projectIdsMap(projectIdsPath);

   // look for this value
   typedef std::map<std::string,std::string>::value_type ProjId;
   BOOST_FOREACH(ProjId projId, idMap)
   {
      if (projId.second == projectDir)
      {
         return core::r_util::ProjectId(projId.first);
      }
   }

   // if we didn't find it then we need to generate a new one (loop until
   // we find one that isn't already in the map)
   std::string id;
   while (id.empty())
   {
      std::string candidateId = core::r_util::generateScopeId();
      if (idMap.find(candidateId) == idMap.end())
         id = candidateId;
   }

   // add it to the map then save the map
   idMap[id] = projectDir;
   core::Error error = core::writeStringMapToFile(projectIdsPath, idMap);
   if (error)
      LOG_ERROR(error);

   // ensure the file has restrictive permissions
#ifndef _WIN32
   error = core::system::changeFileMode(projectIdsPath,
                                        core::system::UserReadWriteMode);
   if (error)
      LOG_ERROR(error);
#endif

   // return the id
   return core::r_util::ProjectId(id);
}


} // anonymous namespace

inline core::r_util::ProjectIdToFilePath projectIdToFilePath(
                                    const core::FilePath& userScratchPath,
                                    const core::FilePath& sharedProjectPath)
{
   return boost::bind(toFilePath, _1, userScratchPath, sharedProjectPath);
}

inline core::r_util::FilePathToProjectId filePathToProjectId(
                                    const core::FilePath& userScratchPath)
{
   return boost::bind(toProjectId, _1, userScratchPath);
}

inline core::r_util::ProjectId projectToProjectId(
                            const core::FilePath& userScratchPath,
                            const std::string& project)
{
   if (project == kProjectNone)
      return core::r_util::ProjectId(kProjectNoneId);
   else
      return session::filePathToProjectId(userScratchPath)(project);
}



} // namespace session
} // namespace rstudio

#endif /* SESSION_SCOPES_HPP */

