/*
 * ZoteroCollectionsLocal.cpp
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

#include "ZoteroCollectionsLocal.hpp"

#include <boost/bind.hpp>
#include <boost/algorithm/algorithm.hpp>
#include <boost/enable_shared_from_this.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/json/Json.hpp>

#include <core/system/Process.hpp>

#include <session/SessionModuleContext.hpp>


using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace zotero {
namespace collections {

namespace {

void getLocalLibrary(std::string key,
                     ZoteroCollectionSpec, // no caching for now
                     ZoteroCollectionsHandler handler)
{
   std::cerr << "key: " << key << std::endl;

   ZoteroCollection myLibrary(ZoteroCollectionSpec(kMyLibrary, 0));
   handler(Success(), std::vector<ZoteroCollection>{ myLibrary });

}


void getLocalCollections(std::string key,
                         std::vector<std::string> collections,
                         ZoteroCollectionSpecs, // no caching for now
                         ZoteroCollectionsHandler handler)
{
    std::cerr << "key: " <<  key << std::endl;

    handler(Success(), std::vector<ZoteroCollection>());
}


} // end anonymous namespace



ZoteroCollectionSource localCollections()
{
   ZoteroCollectionSource source;
   source.getLibrary = getLocalLibrary;
   source.getCollections = getLocalCollections;
   return source;
}

} // end namespace collections
} // end namespace zotero
} // end namespace modules
} // end namespace session
} // end namespace rstudio
