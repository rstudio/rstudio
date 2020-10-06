/*
 * SessionUserPrefsMigration.hpp
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

#ifndef SESSION_MODULE_USER_PREFS_MIGRATION_HPP
#define SESSION_MODULE_USER_PREFS_MIGRATION_HPP

#include <shared_core/FilePath.hpp>

namespace rstudio {
   namespace core {
      class Error;
   }
}

namespace rstudio {
namespace session {
namespace modules {
namespace prefs {

// Migrates preferences from RStudio 1.2 and prior into the new RStudio 1.3 system
core::Error migratePrefs(const core::FilePath& src);

} // namespace prefs
} // namespace modules
} // namespace session
} // namespace rstudio

#endif
