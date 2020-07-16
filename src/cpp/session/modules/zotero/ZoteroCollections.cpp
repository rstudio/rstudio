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

#include <core/FileSerializer.hpp>

#include <session/prefs/UserState.hpp>
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

const char * const kWebAPIType = "web";

const char * const kIndexFile = "INDEX";

const char * const kFile = "file";

void LOG(const std::string&)
{
   // std::cerr << text << std::endl;
}

FilePath collectionsCacheDir(const std::string& type, const std::string& context)
{
   // ~/.local/share/rstudio/zotero-collections
   FilePath cachePath = module_context::userScratchPath()
      .completeChildPath("zotero-collections")
      .completeChildPath(type)
      .completeChildPath(context);
   Error error = cachePath.ensureDirectory();
   if (error)
      LOG_ERROR(error);
   return cachePath;
}

struct IndexedCollection
{
   bool empty() const { return file.empty(); }
   int version;
   std::string file;
};

std::map<std::string,IndexedCollection> collectionsCacheIndex(const FilePath& cacheDir)
{
   std::map<std::string,IndexedCollection> index;

   FilePath indexFile = cacheDir.completeChildPath(kIndexFile);
   if (indexFile.exists())
   {
      std::string indexContents;
      Error error = core::readStringFromFile(indexFile, &indexContents);
      if (!error)
      {
         json::Object indexJson;
         error = indexJson.parse(indexContents);
         if (!error)
         {
            std::for_each(indexJson.begin(), indexJson.end(), [&index](json::Object::Member member) {

               json::Object entryJson = member.getValue().getObject();
               IndexedCollection coll;
               coll.version = entryJson[kVersion].getInt();
               coll.file = entryJson[kFile].getString();
               index.insert(std::make_pair(member.getName(),coll));
            });
         }
      }

      if (error)
         LOG_ERROR(error);
   }

   return index;
}

void updateCollectionsCacheIndex(const FilePath& cacheDir, const std::map<std::string,IndexedCollection>& index)
{
   // create json for index
   json::Object indexJson;
   for (auto item : index)
   {
      json::Object collJson;
      collJson[kVersion] = item.second.version;
      collJson[kFile] = item.second.file;
      indexJson[item.first] = collJson;
   }

   // write index
   FilePath indexFile = cacheDir.completeChildPath(kIndexFile);
   Error error = core::writeStringToFile(indexFile, indexJson.writeFormatted());
   if (error)
      LOG_ERROR(error);
}

Error readCollection(const FilePath& filePath, ZoteroCollection* pCollection)
{
   std::string cacheContents;
   Error error = core::readStringFromFile(filePath, &cacheContents);
   if (error)
      return error;

   json::Object collectionJson;
   error = collectionJson.parse(cacheContents);
   if (error)
      return error;

   pCollection->name = collectionJson[kName].getString();
   pCollection->version = collectionJson[kVersion].getInt();
   pCollection->items = collectionJson[kItems].getArray();

   return Success();
}


ZoteroCollection cachedCollection(const std::string& type, const std::string& context, const std::string& name)
{
   ZoteroCollection collection;
   FilePath cacheDir = collectionsCacheDir(type, context);
   auto index = collectionsCacheIndex(cacheDir);
   auto coll = index[name];
   if (!coll.empty())
   {
      FilePath cachePath = cacheDir.completeChildPath(coll.file);
      Error error = readCollection(cachePath, &collection);
      if (error)
         LOG_ERROR(error);
   }
   return collection;
}

ZoteroCollectionSpec cachedCollectionSpec(const std::string& type, const std::string& context, const std::string& name)
{
   ZoteroCollectionSpec spec;
   FilePath cacheDir = collectionsCacheDir(type, context);
   auto index = collectionsCacheIndex(cacheDir);
   auto coll = index[name];
   if (!coll.empty())
   {
      spec.name = name;
      spec.version = coll.version;
   }
   return spec;
}

ZoteroCollectionSpecs cachedCollectionsSpecs(const std::string& type, const std::string& context)
{
   ZoteroCollectionSpecs specs;
   FilePath cacheDir = collectionsCacheDir(type, context);
   auto index = collectionsCacheIndex(cacheDir);
   for (auto entry : index)
   {
      ZoteroCollectionSpec spec(entry.first, entry.second.version);
      specs.push_back(spec);
   }
   return specs;
}

void updateCachedCollection(const std::string& type, const std::string& context, const std::string& name, const ZoteroCollection& collection)
{
   // see if we have the collection in our index (create an index entry if we don't)
   FilePath cacheDir = collectionsCacheDir(type, context);
   auto index = collectionsCacheIndex(cacheDir);
   auto coll = index[name];
   if (coll.empty())
   {
      coll.version = collection.version;
      coll.file = core::system::generateShortenedUuid();
      index[name] = coll;
      updateCollectionsCacheIndex(cacheDir, index);
   }

   // write the collection
   json::Object collectionJson;
   collectionJson[kName] = collection.name;
   collectionJson[kVersion] = collection.version;
   collectionJson[kItems] = collection.items;
   Error error = core::writeStringToFile(cacheDir.completeChildPath(coll.file), collectionJson.writeFormatted());
   if (error)
      LOG_ERROR(error);
}



} // end anonymous namespace

const char * const kName = "name";
const char * const kVersion = "version";
const char * const kItems = "items";


void getCollections(const std::vector<std::string>& collections,
                    const ZoteroCollectionSpecs& cacheSpecs,
                    ZoteroCollectionsHandler handler)
{
   // we only support the web api right now so hard-code to using that. the code below however
   // will work with other methods (e.g. local sqllite) once we implement them
   std::string apiKey = prefs::userState().zoteroApiKey();

   // if we have an api key then request collections from the web
   if (!apiKey.empty())
   {
      // create a set of specs based on what we have in our server cache (as we always want to keep our cache up to date)
      ZoteroCollectionSpecs serverCacheSpecs;

      // request for explicit list of collections, provide specs for matching collections from the server cache
      if (!collections.empty())
      {
         std::transform(collections.begin(), collections.end(), std::back_inserter(serverCacheSpecs), [apiKey](std::string name) {
            ZoteroCollectionSpec cacheSpec(name, 0);
            ZoteroCollectionSpec cached = cachedCollectionSpec(kWebAPIType, apiKey, name);
            if (!cached.empty())
               cacheSpec.version = cached.version;
            return cacheSpec;
         });
      }

      // request for all collections, provide specs for all collections in the server cache
      else
      {
         serverCacheSpecs = cachedCollectionsSpecs(kWebAPIType, apiKey);
      }

      // get collections
      ZoteroCollectionSource source = collections::webCollections();
      source.getCollections(apiKey, collections, serverCacheSpecs, [apiKey, cacheSpecs, serverCacheSpecs, handler](Error error, ZoteroCollections webCollections) {

         // process response -- for any collection returned w/ a version higher than that in the
         // cache, update the cache. for any collection available (from cache or web) with a version
         // higher than that of the client request, return the updated items (else return no items)
         ZoteroCollections collections;
         if (!error)
         {
            for (auto webCollection : webCollections)
            {
               // see if the server side cache needs updating
               ZoteroCollectionSpecs::const_iterator it = std::find_if(serverCacheSpecs.begin(), serverCacheSpecs.end(), [webCollection](ZoteroCollectionSpec cacheSpec) {
                  return cacheSpec.name == webCollection.name && cacheSpec.version >= webCollection.version;
               });
               // need to update the cache -- do so and then return the just cached copy to the client
               if (it == serverCacheSpecs.end())
               {
                  LOG("Updating and returning cache for " + webCollection.name);
                  updateCachedCollection(kWebAPIType, apiKey, webCollection.name, webCollection);
                  collections.push_back(webCollection);
               }

               // we have a cache for this collection, check to see if it is recent enough (and in that
               // case don't return the items to the client)
               else
               {
                  // see we can satisfy the request from our cache
                  ZoteroCollection cached = cachedCollection(kWebAPIType, apiKey, webCollection.name);
                  if (!cached.empty() )
                  {
                     // see if the client specs already indicate an up to date version
                     ZoteroCollectionSpecs::const_iterator clientIt = std::find_if(cacheSpecs.begin(), cacheSpecs.end(), [cached](ZoteroCollectionSpec spec) {
                        return spec.name == cached.name && spec.version >= cached.version;
                     });
                     if (clientIt == cacheSpecs.end())
                     {
                        // client spec didn't match, return cached collection
                        LOG("Returning existing cache for " + webCollection.name);
                        collections.push_back(cached);
                     }
                     else
                     {
                        // client had up to date version, just return the spec w/ no items
                        LOG("Returning no items for " + webCollection.name);
                        collections.push_back(*clientIt);
                     }
                  }
                  // shouldn't be possible to get here (as the initial condition tested in the loop ensures
                  // that we have a cached) but put this in just to never return incomplete results
                  else
                  {
                     LOG("Unexpected failure to find cache for " + webCollection.name);
                     updateCachedCollection(kWebAPIType, apiKey, webCollection.name, webCollection);
                     collections.push_back(webCollection);
                  }
               }
            }
         }

         handler(error, collections);

      });
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
