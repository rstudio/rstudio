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

#include <r/RExec.hpp>

#include <session/prefs/UserState.hpp>
#include <session/SessionModuleContext.hpp>

#include "session-config.h"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace zotero {
namespace collections {

namespace {

SEXP createCacheSpecSEXP( ZoteroCollectionSpec cacheSpec, r::sexp::Protect* pProtect)
{
   std::vector<std::string> names;
   names.push_back(kName);
   names.push_back(kVersion);
   SEXP cacheSpecSEXP = r::sexp::createList(names, pProtect);
   r::sexp::setNamedListElement(cacheSpecSEXP, kName, cacheSpec.name);
   r::sexp::setNamedListElement(cacheSpecSEXP, kVersion, cacheSpec.version);
   return cacheSpecSEXP;
}

void getLocalLibrary(std::string key,
                     ZoteroCollectionSpec cacheSpec,
                     ZoteroCollectionsHandler handler)
{
   r::sexp::Protect protect;
   std::string libraryJsonStr;
   Error error = r::exec::RFunction(".rs.zoteroGetLibrary", key,
                                    createCacheSpecSEXP(cacheSpec, &protect)).call(&libraryJsonStr);
   if (error)
   {
      handler(error, std::vector<ZoteroCollection>());
   }
   else
   {
      json::Object libraryJson;
      error = libraryJson.parse(libraryJsonStr);
      if (error)
      {
         handler(error, std::vector<ZoteroCollection>());
      }
      else
      {
         ZoteroCollection collection;
         collection.name = libraryJson[kName].getString();
         collection.version = libraryJson[kVersion].getInt();
         collection.items = libraryJson[kItems].getArray();
         handler(Success(), std::vector<ZoteroCollection>{ collection });
      }
   }
}


void getLocalCollections(std::string key,
                         std::vector<std::string> collections,
                         ZoteroCollectionSpecs cacheSpecs,
                         ZoteroCollectionsHandler handler)
{
    json::Array cacheSpecsJson;
    std::transform(cacheSpecs.begin(), cacheSpecs.end(), std::back_inserter(cacheSpecsJson), [](ZoteroCollectionSpec spec) {
       json::Object specJson;
       specJson[kName] = spec.name;
       specJson[kVersion] = spec.version;
       return specJson;
    });

    r::sexp::Protect protect;
    std::string collectionJsonStr;
    Error error = r::exec::RFunction(".rs.zoteroGetCollections", key, collections, cacheSpecsJson)
                                     .call(&collectionJsonStr);
    if (error)
    {
       handler(error, std::vector<ZoteroCollection>());
    }
    else
    {
       json::Array collectionsJson;
       error = collectionsJson.parse(collectionJsonStr);
       if (error)
       {
          handler(error, std::vector<ZoteroCollection>());
       }
       else
       {
           ZoteroCollections collections;
           std::transform(collectionsJson.begin(), collectionsJson.end(), std::back_inserter(collections), [](json::Value json) {
              ZoteroCollection collection;
              json::Object collectionJson = json.getObject();
              collection.name = collectionJson[kName].getString();
              collection.version = collectionJson[kVersion].getInt();
              collection.items = collectionJson[kItems].getArray();
              return collection;
           });
           handler(Success(), collections);
       }
    }
}


} // end anonymous namespace


bool localZoteroAvailable()
{
   // availability based on server vs. desktop
#ifdef RSTUDIO_SERVER
   bool local = false;
#else
   bool local = true;
#endif

   // however, also make it available in debug mode for local dev/test
#ifndef NDEBUG
   local = true;
#endif

   return local;
}


// Returns the zoteroDataDirectory (if any). This will return a valid FilePath
// if the user has specified a zotero data dir in the preferences; OR if
// a zotero data dir was detected on the system. In the former case the
// path may not exist (and this should be logged as an error)
FilePath zoteroDataDirectory()
{
   std::string dataDir = prefs::userState().zoteroDataDir();
   if (!dataDir.empty())
      return module_context::resolveAliasedPath(dataDir);
   else
      return detectedZoteroDataDirectory();
}

// Automatically detect the Zotero data directory and return it if it exists
FilePath detectedZoteroDataDirectory()
{
   if (localZoteroAvailable())
   {
      std::string homeEnv;
   #ifdef _WIN32
      homeEnv = "USERPROFILE";
   #else
      homeEnv = "HOME";
   #endif
      FilePath homeDir = FilePath(string_utils::systemToUtf8(core::system::getenv(homeEnv)));
      FilePath zoteroPath = homeDir.completeChildPath("Zotero");
      if (zoteroPath.exists())
         return zoteroPath;
      else
         return FilePath();
   }
   else
   {
      return FilePath();
   }
}


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
