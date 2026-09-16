/*
 * SessionSourceDatabaseSupervisor.hpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef SESSION_SOURCE_DATABASE_SUPERVISOR_HPP
#define SESSION_SOURCE_DATABASE_SUPERVISOR_HPP

#define kSessionSourceDatabasePrefix "sources"

namespace rstudio {
namespace core {
   class Error;
   class FilePath;
   class FileLock;
}
}

namespace rstudio {
namespace session {
namespace source_database {
namespace supervisor {

core::Error attachToSourceDatabase();

core::Error saveMostRecentDocuments();

core::Error detachFromSourceDatabase();

void suspendSourceDatabase(int status);

void resumeSourceDatabase();

core::FilePath sessionDirPath();

namespace detail {

core::Error isSessionDirLocked(
      const core::FilePath& sessionDir,
      const core::FileLock& advisoryLock,
      bool* pLocked);

core::Error acquireSessionDirLock(const core::FilePath& sessionDir, core::FileLock& lock);

core::Error reclaimOrphanedSession(
      const core::FilePath& sourceRoot,
      const core::FilePath& sessionDir,
      core::FileLock& lock,
      bool* pReclaimed);

} // namespace detail

} // namespace supervisor
} // namespace source_database
} // namespace session
} // namespace rstudio


#endif // SESSION_SOURCE_DATABASE_SUPERVISOR_HPP
