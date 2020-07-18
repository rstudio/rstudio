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

#include <core/Hash.hpp>

#include <core/FileSerializer.hpp>

#include <session/prefs/UserState.hpp>
#include <session/prefs/UserPrefs.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/projects/SessionProjects.hpp>
#include <session/SessionAsyncDownloadFile.hpp>

#include "ZoteroCollectionsLocal.hpp"
#include "ZoteroCollectionsWeb.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace zotero {
namespace collections {

namespace {

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
   // update index
   FilePath cacheDir = collectionsCacheDir(type, context);
   auto index = collectionsCacheIndex(cacheDir);
   auto coll = index[name];
   if (coll.empty())
      coll.file = core::system::generateShortenedUuid();
   coll.version = collection.version;
   index[name] = coll;
   updateCollectionsCacheIndex(cacheDir, index);

   // write the collection
   json::Object collectionJson;
   collectionJson[kName] = collection.name;
   collectionJson[kVersion] = collection.version;
   collectionJson[kItems] = collection.items;
   Error error = core::writeStringToFile(cacheDir.completeChildPath(coll.file), collectionJson.writeFormatted());
   if (error)
      LOG_ERROR(error);
}


// repsond with either a collection from the server cache or just name/version if the client
// already has a more up to date verison
ZoteroCollection responseFromServerCache(const std::string& type,
                                         const std::string& apiKey,
                                         const std::string& collection,
                                         const ZoteroCollectionSpecs& clientCacheSpecs)
{
   ZoteroCollection cached = cachedCollection(type, apiKey, collection);
   if (!cached.empty() )
   {
      // see if the client specs already indicate an up to date version
      ZoteroCollectionSpecs::const_iterator clientIt = std::find_if(clientCacheSpecs.begin(), clientCacheSpecs.end(), [cached](ZoteroCollectionSpec spec) {
         return spec.name == cached.name && spec.version >= cached.version;
      });
      if (clientIt == clientCacheSpecs.end())
      {
         // client spec didn't match, return cached collection
         LOG("Returning server cache for " + collection);
         return cached;
      }
      else
      {
         // client had up to date version, just return the spec w/ no items
         LOG("Using client cache for " + collection);
         return *clientIt;
      }
   }
   else
   {
      return ZoteroCollection();
   }

}

struct Connection
{
   bool empty() const { return type.length() == 0; }
   std::string type;
   std::string context;
   std::string cacheContext;
   ZoteroCollectionSource source;
};

Connection zoteroConnection()
{
   // determine the zotero connection type (deafult to local)
   std::string type = prefs::userPrefs().zoteroConnectionType();
   if (type.empty())
       type = kZoteroConnectionTypeLocal;

   // force it to web if local is not available in this config
   if (!localZoteroAvailable())
      type = kZoteroConnectionTypeWeb;

   // initialize context
   std::string context;
   if (type == kZoteroConnectionTypeLocal)
   {
      FilePath localDataDir = zoteroDataDirectory();
      if (localDataDir.exists())
      {
         context = string_utils::utf8ToSystem(localDataDir.getAbsolutePath());
      }
      else
      {
         LOG_ERROR(core::fileNotFoundError(localDataDir, ERROR_LOCATION));
      }
   }
   else
   {
      context = prefs::userState().zoteroApiKey();
   }

   // if we have a context then proceed to fill out the connection, otherwise
   // just return an empty connection. we wouldn't have a context if we were
   // configured for a local connection (the default) but there was no zotero
   // data directory. we also woudln't have a context if we were configured
   // for a web connection and there was no zotero API key
   if (!context.empty())
   {
      Connection connection;
      connection.type = type;
      connection.context = context;
      // use a hash of the context for the cacheContext (as it might not be a valid directory name)
      connection.cacheContext = core::hash::crc32HexHash(context);
      connection.source = type == kZoteroConnectionTypeLocal ? collections::localCollections() : collections::webCollections();
      return connection;
   }
   else
   {
      return Connection();
   }
}


} // end anonymous namespace

const char * const kName = "name";
const char * const kVersion = "version";
const char * const kItems = "items";

const char * const kMyLibrary = "C5EC606F-5FF7-4CFD-8873-533D6C31DDF0";


void getLibrary(ZoteroCollectionSpec cacheSpec, ZoteroCollectionsHandler handler)
{
   // get connection if we have one
   Connection conn = zoteroConnection();
   if (!conn.empty())
   {    
      ZoteroCollectionSpec serverCacheSpec = cachedCollectionSpec(conn.type, conn.cacheContext, kMyLibrary);
      conn.source.getLibrary(conn.context, serverCacheSpec, [conn, handler, cacheSpec, serverCacheSpec](Error error, ZoteroCollections webLibrary) {

         ZoteroCollection collection;
         if (error)
         {
            // if it's a host error then see if we can use a cached version
            if (isHostError(core::errorDescription(error)))
            {
               collection = cachedCollection(conn.type, conn.cacheContext, kMyLibrary);
               if (collection.empty())
               {
                  handler(error, std::vector<ZoteroCollection>());
               }
            }
            else
            {
               handler(error, std::vector<ZoteroCollection>());
            }
         }
         else
         {
            collection = webLibrary[0];
         }

         // if we don't have a collection then we are done (handler has already been called w/ the error)
         if (collection.empty())
            return;

         // see if we need to update our server side cache. if we do then just return that version
         if (serverCacheSpec.empty() || serverCacheSpec.version < collection.version)
         {
            LOG("Updating server cache for <library>");
            updateCachedCollection(conn.type, conn.cacheContext, collection.name, collection);
            LOG("Returning server cache for <library>");
            handler(Success(), std::vector<ZoteroCollection>{ collection });
         }

         // see if the client already has the version we are serving. in that case
         // just return w/o items
         else if (cacheSpec.version >= collection.version)
         {
            ZoteroCollectionSpec spec(conn.type, cacheSpec.version);
            LOG("Using client cache for <library>");
            handler(Success(), std::vector<ZoteroCollection>{ spec });
         }

         // otherwise return the server cache (it's guaranteed to exist and be >=
         // the returned collection based on the first conditional)
         else
         {
            ZoteroCollection serverCache = cachedCollection(conn.type, conn.cacheContext, kMyLibrary);
            collection.items = serverCache.items;
            LOG("Returning server cache for <library>");
            handler(Success(), std::vector<ZoteroCollection>{ collection });
         }
      });
   }
   else
   {
      handler(Success(), std::vector<ZoteroCollection>());
   }
}

void getCollections(std::vector<std::string> collections,
                    ZoteroCollectionSpecs cacheSpecs,
                    ZoteroCollectionsHandler handler)
{   
   // get connection if we have o ne
   Connection conn = zoteroConnection();
   if (!conn.empty())
   {
      // create a set of specs based on what we have in our server cache (as we always want to keep our cache up to date)
      ZoteroCollectionSpecs serverCacheSpecs;

      // request for explicit list of collections, provide specs for matching collections from the server cache
      if (!collections.empty())
      {
         std::transform(collections.begin(), collections.end(), std::back_inserter(serverCacheSpecs), [conn](std::string name) {
            ZoteroCollectionSpec cacheSpec(name, 0);
            ZoteroCollectionSpec cached = cachedCollectionSpec(conn.type, conn.cacheContext, name);
            if (!cached.empty())
               cacheSpec.version = cached.version;
            return cacheSpec;
         });
      }

      // request for all collections, provide specs for all collections in the server cache
      else
      {
         serverCacheSpecs = cachedCollectionsSpecs(conn.type, conn.cacheContext);
      }

      // get collections
      conn.source.getCollections(conn.context, collections, serverCacheSpecs, [conn, collections, cacheSpecs, serverCacheSpecs, handler](Error error, ZoteroCollections webCollections) {

         // process response -- for any collection returned w/ a version higher than that in the
         // cache, update the cache. for any collection available (from cache or web) with a version
         // higher than that of the client request, return the updated items (else return no items) 
         if (!error)
         {
            ZoteroCollections responseCollections;
            for (auto webCollection : webCollections)
            {
               // see if the server side cache needs updating
               ZoteroCollectionSpecs::const_iterator it = std::find_if(serverCacheSpecs.begin(), serverCacheSpecs.end(), [webCollection](ZoteroCollectionSpec cacheSpec) {
                  return cacheSpec.name == webCollection.name && cacheSpec.version >= webCollection.version;
               });
               // need to update the cache -- do so and then return the just cached copy to the client
               if (it == serverCacheSpecs.end())
               {
                  LOG("Updating server cache for " + webCollection.name);
                  updateCachedCollection(conn.type, conn.cacheContext, webCollection.name, webCollection);
                  LOG("Returning server cache for " + webCollection.name);
                  responseCollections.push_back(webCollection);
               }

               // we have a cache for this collection, check to see if it is recent enough (and in that
               // case don't return the items to the client)
               else
               {
                  // see we can satisfy the request from our cache
                  ZoteroCollection cached = responseFromServerCache(conn.type, conn.cacheContext, webCollection.name, cacheSpecs);
                  if (!cached.empty())
                  {
                     responseCollections.push_back(cached);
                  }
                  else
                  {  
                     // shouldn't be possible to get here (as the initial condition tested in the loop ensures
                     // that we have a cached collection)
                     LOG("Unexpected failure to find cache for " + webCollection.name);
                  }
               }
            }

            handler(Success(), responseCollections);

         // for host errors try to serve from the cache
         } else if (isHostError(core::errorDescription(error))) {

            ZoteroCollections responseCollections;
            for (auto collection : collections)
            {
               ZoteroCollection cached = responseFromServerCache(conn.type, conn.cacheContext, collection, cacheSpecs);
               if (!cached.empty())
                  responseCollections.push_back(cached);
            }
            handler(Success(),responseCollections);

         // report error
         } else {
            handler(error, std::vector<ZoteroCollection>());
         }


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
