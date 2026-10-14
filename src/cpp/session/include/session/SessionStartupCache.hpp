/*
 * SessionStartupCache.hpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

#ifndef SESSION_STARTUP_CACHE_HPP
#define SESSION_STARTUP_CACHE_HPP

#include <core/FileSerializer.hpp>
#include <shared_core/json/Json.hpp>

namespace rstudio {
namespace session {

// A disposable, atomically replaced cache. Missing, stale, and damaged entries
// all fall back to the real probe; concurrent sessions can safely replace it.
class StartupCache
{
public:
   explicit StartupCache(const core::FilePath& path) : path_(path) {}

   bool read(const core::json::Object& key, core::json::Object* pValue) const
   {
      std::string contents;
      if (!path_.exists() || core::readStringFromFile(path_, &contents))
         return false;

      core::json::Object entry;
      if (entry.parse(contents))
         return false;

      std::string storedKey;
      return !core::json::readObject(entry, "key", storedKey, "value", *pValue) &&
             storedKey == key.write();
   }

   void write(const core::json::Object& key, const core::json::Object& value) const
   {
      if (path_.getParent().ensureDirectory())
         return;

      core::json::Object entry;
      entry["key"] = key.write();
      entry["value"] = value;
      // Cache failures are harmless; retain the previous complete entry.
      core::writeStringToFileAtomic(path_, entry.write());
   }

private:
   core::FilePath path_;
};

} // namespace session
} // namespace rstudio

#endif
