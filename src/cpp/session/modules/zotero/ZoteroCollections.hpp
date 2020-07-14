/*
 * ZoteroCollections.hpp
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

#ifndef RSTUDIO_SESSION_MODULES_ZOTERO_COLLECTIONS_HPP
#define RSTUDIO_SESSION_MODULES_ZOTERO_COLLECTIONS_HPP

#include <string>

#include <boost/function.hpp>

#include <shared_core/json/Json.hpp>

namespace rstudio {
namespace core {
class Error;
}
}

namespace rstudio {
namespace session {
namespace modules {
namespace zotero {
namespace collections {

// collection spec
struct ZoteroCollectionSpec
{
   ZoteroCollectionSpec(const std::string& name = "", int version = 0)
      : name(name), version(version)
   {
   }
   std::string name;
   int version;
};
typedef std::vector<ZoteroCollectionSpec> ZoteroCollectionSpecs;
typedef boost::function<void(core::Error,ZoteroCollectionSpecs)> ZoteroCollectionSpecsHandler;

// collection
struct ZoteroCollection : ZoteroCollectionSpec
{
   core::json::Array items;
};
typedef std::vector<ZoteroCollection> ZoteroCollections;
typedef boost::function<void(core::Error,ZoteroCollections)> ZoteroCollectionsHandler;


// requirements for implementing a collection source
struct ZoteroCollectionSource
{
   boost::function<void(const std::string&, const ZoteroCollectionSpecs&, ZoteroCollectionsHandler)> getUpdates;
   boost::function<void(const std::string&, const ZoteroCollectionSpecs&, ZoteroCollectionsHandler)> getCollections;
};

// get collections using the currently configured source
void getCollections(const ZoteroCollectionSpecs& specs, ZoteroCollectionsHandler handler);


} // end namespace collections
} // end namespace zotero
} // end namespace modules
} // end namespace session
} // end namespace rstudio

#endif /* RSTUDIO_SESSION_MODULES_ZOTERO_COLLECTIONS_HPP */
