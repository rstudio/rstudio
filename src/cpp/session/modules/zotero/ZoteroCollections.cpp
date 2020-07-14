/*
 * ZoteroCollections.cpp
 *
 * Copyright (C) 2009-20 by RStudio, Inc.
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

#include "ZoteroCollections.hpp"

#include <shared_core/Error.hpp>
#include <shared_core/json/Json.hpp>

#include <session/prefs/UserPrefs.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/projects/SessionProjects.hpp>

#include "ZoteroCollectionsWeb.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace zotero {
namespace collections {

namespace {



} // end anonymous namespace

void getCollections(const ZoteroCollectionSpecs& specs, ZoteroCollectionsHandler handler)
{
   // get the api key
   std::string apiKey = prefs::userPrefs().zoteroApiKey();

   // if we have an api key then request collections from the web
   if (!apiKey.empty())
   {
      // get collections
      ZoteroCollectionSource source = collections::webCollections();
      source.getCollections(apiKey, specs, handler);
   }
   else
   {
      handler(Success(), std::vector<ZoteroCollection>());
   }

}

} // end namespace collections
} // end namespace zotero
} // end namespace modules
} // end namespace session
} // end namespace rstudio
