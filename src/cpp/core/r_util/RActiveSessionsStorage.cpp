/*
 * RActiveSessionsStorage.cpp
 *
 * Copyright (C) 2022 by RStudio, PBC
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

#include <core/r_util/RActiveSessionsStorage.hpp>

#include <core/r_util/RActiveSessionStorage.hpp>
#include <core/r_util/RActiveSessions.hpp>

#include <algorithm>

namespace rstudio {
namespace core {
namespace r_util {

namespace
{

FilePath getSessionDirPath(const FilePath& storagePath, const std::string& sessionId)
{
   return storagePath.completeChildPath(kSessionDirPrefix + sessionId);
}

} // anonymous namespace

FileActiveSessionsStorage::FileActiveSessionsStorage(const FilePath& rootStoragePath)
{
   storagePath_ = ActiveSessions::storagePath(rootStoragePath);
   Error error = storagePath_.ensureDirectory();
   if (error)
      LOG_ERROR(error);
}

Error FileActiveSessionsStorage::hasSessionId(const std::string& sessionId, bool* pHasSessionId) const
{
   FilePath dir = getSessionDirPath(storagePath_, sessionId);
   *pHasSessionId = dir.exists();

   return Success();
}

std::vector<std::string> FileActiveSessionsStorage::listSessionIds() const
{
   // list to return
   std::vector<std::string> sessions;

   // enumerate children and check for sessions
   std::vector<FilePath> children;
   Error error = storagePath_.getChildren(children);
   if (error)
   {
      LOG_ERROR(error);
      return sessions;
   }
   std::string prefix = kSessionDirPrefix;
   for (const FilePath& child : children)
   {
      if (boost::algorithm::starts_with(child.getFilename(), prefix))
      {
         std::string id = child.getFilename().substr(prefix.length());
         sessions.push_back(id);
      }

   }

   // return
   return sessions;
}

size_t FileActiveSessionsStorage::getSessionCount() const
{
   return listSessionIds().size();
}

// Returns a shared pointer to the session storage, or an empty session session storage pointer if it does not exist
std::shared_ptr<IActiveSessionStorage> FileActiveSessionsStorage::getSessionStorage(const std::string& id) const
{
   FilePath scratchPath = storagePath_.completeChildPath(kSessionDirPrefix + id);
   return std::make_shared<FileActiveSessionStorage>(scratchPath);
}

} // namespace r_util
} // namsepace core
} // namespace rstudio
