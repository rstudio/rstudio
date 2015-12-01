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
#include <sys/stat.h>
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

inline core::Error projectPathFromEntry(const core::FilePath& projectEntry,
                                        std::string* pPath)
{
   // get the path from shared storage
   std::string entryContents;
   core::Error error = core::readStringFromFile(projectEntry,
                                                &entryContents);
   if (error)
      return error;

   // read the contents
   core::json::Value projectEntryVal;
   if (!core::json::parse(entryContents, &projectEntryVal))
   {
      error = core::Error(core::json::errc::ParseError,
                                ERROR_LOCATION);
      error.addProperty("path", projectEntry.absolutePath());
      return error;
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
      error.addProperty("path", projectEntry.absolutePath());
      return error;
   }

   *pPath = projectPath;
   return core::Success();
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
         // extract the path from the entry
         std::string projectPath;
         core::Error error = projectPathFromEntry(projectEntryPath,
                                                  &projectPath);
         if (error)
         {
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

#ifndef _WIN32
inline std::string sharedProjectId(const core::FilePath& sharedStoragePath,
                                   const std::string& projectDir)
{
   // enumerate the project entries in shared storage (this should succeed)
   std::vector<core::FilePath> projectEntries;
   core::Error error = sharedStoragePath.complete(kProjectSharedDir)
                                        .children(&projectEntries);
   if (error)
   {
      LOG_ERROR(error);
      return "";
   }

   BOOST_FOREACH(const core::FilePath& projectEntry, projectEntries)
   {
      // skip files that don't look like project entries
      if (projectEntry.extensionLowerCase() != kProjectEntryExt)
         continue;

      std::string projectPath;
      error = projectPathFromEntry(projectEntry, &projectPath);
      if (error)
      {
         // this is very much expected (we aren't going to be able to examine
         // the contents of most project entries)
         continue;
      }
      else if (projectDir == projectPath)
      {
         return projectEntry.stem();
      }
   }

   return "";
}
#endif

inline core::r_util::ProjectId toProjectId(const std::string& projectDir,
                             const core::FilePath& userScratchPath,
                             const core::FilePath& sharedStoragePath)
{
   // get the id map
   core::FilePath projectIdsPath = projectIdsFilePath(userScratchPath);
   std::map<std::string,std::string> idMap = projectIdsMap(projectIdsPath);

   std::string id;

   // look for this value
   std::map<std::string,std::string>::iterator it;
   for (it = idMap.begin(); it != idMap.end(); it++)
   {
      if (it->second == projectDir)
      {
         id = it->first;

         // if this ID includes both project and user information, we can
         // return it immediately
         if (id.length() == kUserIdLen + kProjectIdLen)
            return core::r_util::ProjectId(id);

         break;
      }
   }

   // if this project belongs to someone else, try to look up its shared
   // project ID (we currently have to do this even if we found the ID in the
   // map; see note below)
#ifndef _WIN32
   struct stat st;
   if (::stat(projectDir.c_str(), &st) == 0 &&
       st.st_uid != ::getuid())
   {
      // if we already found an ID but it doesn't contain any user information
      // (just a raw project ID), we need to do some fixup
      if (id.length() == kProjectIdLen)
      {
         // TODO: The following code is temporary. There was a period of time
         // during which it was possible to get your own project ID for a
         // project that did not belong to you.
         //
         // This is no longer possible, but mismatched project IDs cause
         // features such as distributed events to fail, and the project IDs
         // are cached in the mapping file, so until all the per-user IDs are
         // flushed and replaced with shared IDs, we need to leave this in; it
         // causes us to erase the per-user ID so it's replaced with a shared
         // ID.

         idMap.erase(it);
         it = idMap.end();
      }

      id = sharedProjectId(sharedStoragePath, projectDir);
   }
#endif

   // if we found a cached ID, return it now
   if (it != idMap.end() && !id.empty())
   {
      return core::r_util::ProjectId(id);
   }

   // if we didn't find it, and we don't already have an ID, then we need to
   // generate a new one (loop until we find one that isn't already in the map)
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
                                    const core::FilePath& userScratchPath,
                                    const core::FilePath& sharedStoragePath)
{
   return boost::bind(toProjectId, _1, userScratchPath, sharedStoragePath);
}

inline core::r_util::ProjectId projectToProjectId(
                            const core::FilePath& userScratchPath,
                            const core::FilePath& sharedStoragePath,
                            const std::string& project)
{
   if (project == kProjectNone)
      return core::r_util::ProjectId(kProjectNoneId);
   else
      return session::filePathToProjectId(userScratchPath, sharedStoragePath)
                                         (project);
}



} // namespace session
} // namespace rstudio

#endif /* SESSION_SCOPES_HPP */

