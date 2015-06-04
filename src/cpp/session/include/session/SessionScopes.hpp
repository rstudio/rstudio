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

#ifndef _WIN32
#include <core/system/FileMode.hpp>
#endif

#include <core/r_util/RSessionContext.hpp>

#include <session/projects/ProjectsSettings.hpp>

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

inline std::string toFilePath(const std::string& projectId,
                              const core::FilePath& userScratchPath)
{
   // get the id map
   core::FilePath projectIdsPath = projectIdsFilePath(userScratchPath);
   std::map<std::string,std::string> idMap = projectIdsMap(projectIdsPath);

   // return the file path (empty if not found)
   return idMap[projectId];
}


inline std::string toProjectId(const std::string& projectDir,
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
         return projId.first;
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
   return id;
}


} // anonymous namespace

inline core::r_util::ProjectIdToFilePath projectIdToFilePath(
                                    const core::FilePath& userScratchPath)
{
   return boost::bind(toFilePath, _1, userScratchPath);
}

inline core::r_util::FilePathToProjectId filePathToProjectId(
                                    const core::FilePath& userScratchPath)
{
   return boost::bind(toProjectId, _1, userScratchPath);
}

} // namespace session
} // namespace rstudio

#endif /* SESSION_SCOPES_HPP */
