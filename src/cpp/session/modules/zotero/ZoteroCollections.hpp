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


extern const char * const kName;
extern const char * const kVersion;
extern const char * const kKey;
extern const char * const kParentKey;
extern const char * const kItems;

extern const int kNoVersion;

// collection spec
struct ZoteroCollectionSpec
{
   explicit ZoteroCollectionSpec(const std::string& name = "",
                                 const std::string& key = "",
                                 const std::string& parentKey = "",
                                 int version = kNoVersion)
      : name(name), key(key), parentKey(parentKey), version(version)
   {
   }
   explicit ZoteroCollectionSpec(const std::string& colName, const std::string& key, int version)
       : ZoteroCollectionSpec(colName, key, "", version)
   {
   }

   bool empty() const { return name.empty(); }
   std::string name;
   std::string key;
   std::string parentKey;
   int version;
};
typedef std::vector<ZoteroCollectionSpec> ZoteroCollectionSpecs;
typedef boost::function<void(core::Error,ZoteroCollectionSpecs)> ZoteroCollectionSpecsHandler;

// collection
struct ZoteroCollection : ZoteroCollectionSpec
{
   ZoteroCollection() : ZoteroCollectionSpec("", "", "", kNoVersion) {}
   explicit ZoteroCollection(const std::string& colName)
      : ZoteroCollectionSpec(colName, "", "", kNoVersion)
   {
   }
   explicit ZoteroCollection(const ZoteroCollectionSpec& spec)
      : ZoteroCollectionSpec(spec.name, spec.key, spec.parentKey, spec.version)
   {
   }
   core::json::Array items;
};
typedef std::vector<ZoteroCollection> ZoteroCollections;
typedef boost::function<void(core::Error,ZoteroCollections,std::string)> ZoteroCollectionsHandler;

typedef boost::function<void(core::Error,std::vector<std::string>)> ZoteroLibrariesHandler;

// find the parent spec within a set of specs
ZoteroCollectionSpec findParentSpec(const ZoteroCollectionSpec& spec, const ZoteroCollectionSpecs& specs);


// requirements for implementing a collection source
struct ZoteroCollectionSource
{
   boost::function<void(std::string,
                        std::vector<std::string>,
                        ZoteroCollectionSpecs,
                        ZoteroCollectionsHandler)> getCollections;

   boost::function<void(std::string,ZoteroLibrariesHandler)> getLibraryNames;

   boost::function<void(std::string, std::vector<std::string>, ZoteroCollectionSpecsHandler)> getCollectionSpecs;
};

// get collections using the currently configured source
void getCollections(std::vector<std::string> collections,
                    ZoteroCollectionSpecs cacheSpecs,
                    bool useCache,
                    ZoteroCollectionsHandler handler);

// get library names for the currently configured source
void getLibraryNames(ZoteroLibrariesHandler handler);

// get the collection specs for the specified collections
void getCollectionSpecs(std::vector<std::string> collections, ZoteroCollectionSpecsHandler handler);


} // end namespace collections
} // end namespace zotero
} // end namespace modules
} // end namespace session
} // end namespace rstudio

#endif /* RSTUDIO_SESSION_MODULES_ZOTERO_COLLECTIONS_HPP */
