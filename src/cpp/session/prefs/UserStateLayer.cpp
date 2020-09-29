/*
 * UserStateLayer.cpp
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

#include "UserStateLayer.hpp"

#include <core/system/Xdg.hpp>

#include <session/SessionOptions.hpp>
#include <session/prefs/UserState.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace prefs {

UserStateLayer::UserStateLayer():
   PrefLayer(kUserStateUserLayer)
{
}

core::Error UserStateLayer::readPrefs()
{
   FilePath schemaFile = options().rResourcesPath().completePath("schema").completePath(kUserStateSchemaFile);

   // desktop and server versions of RStudio use separate state files so that mixing desktop and server
   // on the same machine is possible w/o side effects like sharing a source database
   stateFile_ = core::system::xdg::userDataDir().completePath(
         options().programMode() == kSessionProgramModeDesktop ? 
            kUserStateFileDesktop : 
            kUserStateFileServer);

   if (!stateFile_.exists())
   {
      // if there's no state file yet, check for a state file left by an older version of RStudio 1.3
      FilePath oldStateFile = core::system::xdg::userDataDir().completePath("rstudio-state.json");
      if (oldStateFile.exists())
      {
          // found an old file; attempt to migrate it
          Error error = oldStateFile.move(stateFile_);
          if (error)
              LOG_ERROR(error);
      }
   }

   return loadPrefsFromFile(stateFile_, schemaFile);
}

core::Error UserStateLayer::writePrefs(const core::json::Object &prefs)
{
   if (stateFile_.isEmpty())
   {
      return fileNotFoundError(ERROR_LOCATION);
   }

   // ensure state file can only be read/written by this user
#ifndef _WIN32
   if (stateFile_.exists())
   {
      Error error = stateFile_.changeFileMode(FileMode::USER_READ_WRITE);
      if (error)
         LOG_ERROR(error);
   }
#endif

   RECURSIVE_LOCK_MUTEX(mutex_)
   {
      *cache_ = prefs;
   }
   END_LOCK_MUTEX

   return writePrefsToFile(*cache_, stateFile_);
}

} // namespace prefs
} // namespace session
} // namespace rstudio

