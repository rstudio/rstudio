/*
 * RUserData.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#include <core/r_util/RUserData.hpp>

#include <shared_core/FilePath.hpp>

#include <core/system/System.hpp>
#include <core/system/Xdg.hpp>
#include <core/FileLock.hpp>

#include <core/Algorithm.hpp>

#define kMigratedFile  "rs-14-migrated"
#define kMigrationLock "rs-14-migration.lock"

namespace rstudio {
namespace core {
namespace r_util {

// This routine migrates user state data from its home in RStudio 1.3 and prior
// (usually ~/.rstudio) to its XDG-compliant home in RStudio 1.4 and later
// (~/.local/share/rstudio or $XDG_DATA_HOME).
//
// This is a one-time migration that cleans up the old folder.
Error migrateUserStateIfNecessary(SessionType sessionType)
{
   Error error;

   // Check to see if state has already been migrated. The user data directory existed
   // in RStudio 1.3, but did not contain client state, so we use that as a sentry to
   // let us know if we've migrated.
   FilePath newPath = core::system::xdg::userDataDir();
   if (newPath.completeChildPath("client-state").exists())
   {
      // We already have data, don't migrate
      return Success();
   }

   // Compute the path to the old user state folder
   FilePath homePath = core::system::userHomePath("R_USER|HOME");
   std::string scratchPathName;
   if (sessionType == SessionTypeDesktop)
      scratchPathName = "RStudio-Desktop";
   else
      scratchPathName = "RStudio";
   FilePath oldScratchPath = core::system::userSettingsPath(homePath, 
         scratchPathName,
         false /* don't create */);

   // If the folder doesn't exist or has a migration flag, no migration is necessary.
   if (!oldScratchPath.exists())
       return Success();
   if (oldScratchPath.completeChildPath(kMigratedFile).exists())
       return Success();

   // If the new and old folders are the same, no migration is necessary (this
   // could happen if RSTUDIO_DATA_HOME is used to preserve the legacy folder
   // location)
   if (oldScratchPath.isEquivalentTo(newPath))
      return Success();

   // Create the new folder if necessary so we can move content there.
   error = newPath.ensureDirectory();
   if (error)
       return error;

   // Ensure file locks are initialized (this migration happens very early in the 
   // startup process
   if (FileLock::verifyInitialized()) 
      FileLock::initialize();

   // At this point we've decided to do a migration. Acquire a lock to ensure that
   // multiple processes don't attempt to migrate at once (not likely but possible)
   auto lock = FileLock::createDefault();
   ScopedFileLock migrationLock(lock, newPath.completeChildPath(kMigrationLock));
   if (migrationLock.error())
   {
       // Another process is probably migrating.
       return migrationLock.error();
   }

   // Clean up the sessions folder in the destinations. This can contain provisional
   // session data written very early in the bootstrapping process (before migration).
   error = newPath.completeChildPath("sessions").removeIfExists();
   if (error)
   {
       // Not fatal; we can migrate remainder safely
       LOG_ERROR(error);
   }

   // Move each of the files/directories from the old folder to the new one. We can't
   // just move the whole directory at once since the target (new folder) may not
   // be empty.
   //
   // It'd be safer to copy the files so that we can't wind up in a partially migrated
   // state, but some of these files and folders are very large (they contain all the
   // environment data from suspended R sessions), and many users in enterprise
   // settings run close to their disk quota, so having two copies of the data sitting
   // around isn't an option.
   std::vector<core::FilePath> files;
   error = oldScratchPath.getChildren(files);
   if (error)
       return error;
   std::vector<std::string> failures;
   for (const auto& f: files)
   {
      FilePath target = newPath.completeChildPath(f.getFilename());
      Error moveError = f.move(target);
      if (moveError)
      {
         // Log and continue; we still want to migrate as much data as we can
         failures.push_back(f.getFilename());
         LOG_ERROR(moveError);
      }
   }

   // If we were able to move all the content, clean up by removing the folder we
   // just migrated.
   if (failures.empty())
   {
      error = oldScratchPath.remove();
      // In the unlikely event that we aren't able to remove the old folder,
      // we can still probably write content inside it, so this is not fatal.
      if (error)
      {
          LOG_ERROR(error);
      }
   }
   else
   {
      LOG_WARNING_MESSAGE("Failed to migrate all user state from " +
                          oldScratchPath.getAbsolutePath() +
                          " to " +
                          newPath.getAbsolutePath() + "; could not move " +
                          algorithm::join(failures, ", "));
   }

   // If the old scratch path still exists, save a flag there indicating that we have
   // attempted to migrate it. We do this even if the migration failed, so we don't
   // keep trying to migrate
   if (oldScratchPath.exists())
   {
      error = oldScratchPath.completeChildPath(kMigratedFile).ensureFile();
   }

   return Success();
}


} // namespace r_util
} // namespace core 
} // namespace rstudio



