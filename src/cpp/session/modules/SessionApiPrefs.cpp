/*
 * SessionApiPrefs.cpp
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

#include "SessionApiPrefs.hpp"

#include <core/system/Xdg.hpp>

#include <session/prefs/PrefLayer.hpp>

using namespace rstudio::core;
using namespace rstudio::session::prefs;

namespace rstudio {
namespace session {
namespace modules {
namespace prefs {
namespace {

/**
 * A preference layer for user-defined preferences from rstudoapi.
 */
class ApiPrefLayer: public PrefLayer
{
public:
   ApiPrefLayer():
      PrefLayer("api")
   {
   }

   core::Error readPrefs()
   {
      prefsFile_ = core::system::xdg::userConfigDir().completePath(kApiPrefsFile);

      // Load prefs; there's no schema for these prefs since API users can write any prefs they like
      return loadPrefsFromFile(prefsFile_, core::FilePath());
   }

   core::Error writePrefs(const core::json::Object &prefs)
   {
      RECURSIVE_LOCK_MUTEX(mutex_)
      {
         *cache_ = prefs;
      }
      END_LOCK_MUTEX

      return writePrefsToFile(*cache_, prefsFile_);
   }

private:
   core::FilePath prefsFile_;
};

} // anonymous namespace

core::Error ApiPrefs::createLayers()
{
   RECURSIVE_LOCK_MUTEX(mutex_)
   {
      layers_.push_back(boost::make_shared<ApiPrefLayer>());
   }
   END_LOCK_MUTEX

   return Success();
}

int ApiPrefs::userLayer()
{
   // The API prefs have only one layer
   return 0;
}

int ApiPrefs::clientChangedEvent()
{
   // The client is not currently notified for API prefs changes
   return 0;
}

} // namespace prefs
} // namespace modules
} // namespace session
} // namespace rstudio
