/*
 * ZoteroCollectionsWeb.cpp
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

#include "ZoteroCollectionsWeb.hpp"

#include <boost/bind.hpp>
#include <boost/algorithm/algorithm.hpp>
#include <boost/enable_shared_from_this.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/json/Json.hpp>

#include <core/system/Process.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionAsyncDownloadFile.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace zotero {
namespace collections {

namespace {

const char * const kZoteroApiHost = "https://api.zotero.org";
const char * const kZoteroApiVersion = "3";

typedef boost::function<void(const core::Error&,int,core::json::Value)> ZoteroJsonResponseHandler;

void zoteroJsonRequest(const std::string& key,
                       const std::string& resource,
                       http::Fields queryParams,
                       const std::string& schema,
                       const ZoteroJsonResponseHandler& handler)
{
   // authorize using header or url param as required
   http::Fields headers;
   if (!key.empty())
   {
      if (module_context::hasMinimumRVersion("3.6"))
      {
         boost::format fmt("Bearer %s");
         headers.push_back(std::make_pair("Authorization", "Bearer " + key));
         headers.push_back(std::make_pair("Zotero-API-Version", kZoteroApiVersion));
      }
      else
      {
         queryParams.push_back(std::make_pair("key", key));
         queryParams.push_back(std::make_pair("v", kZoteroApiVersion));
      }
   }

   // build query string
   std::string queryString;
   core::http::util::buildQueryString(queryParams, &queryString);
   if (queryString.length() > 0)
      queryString = "?" + queryString;


   // build the url and make the request
   boost::format fmt("%s/%s%s");
   const std::string url = boost::str(fmt % kZoteroApiHost % resource % queryString);
   asyncDownloadFile(url, headers, [handler, schema](const core::system::ProcessResult& result) {
      if (result.exitStatus == EXIT_SUCCESS)
      {
         json::Value resultJson;
         Error error;
         if (schema.length() > 0)
            error = resultJson.parseAndValidate(result.stdOut, schema);
         else
            error = resultJson.parse(result.stdOut);

         if (error)
         {
            handler(error, 500, json::Value());
         }
         else
         {
            handler(Success(), 200, resultJson);
         }
      }
      else
      {
         Error error = systemError(boost::system::errc::state_not_recoverable, result.stdErr, ERROR_LOCATION);
         handler(error, 500, json::Value());
      }
   });
}

http::Fields zoteroItemRequestParams(int start, int limit)
{
   http::Fields params;
   params.push_back(std::make_pair("format", "json"));
   params.push_back(std::make_pair("include", "csljson"));
   params.push_back(std::make_pair("itemType", "-attachment"));
   params.push_back(std::make_pair("start", safe_convert::numberToString(start)));
   params.push_back(std::make_pair("limit", safe_convert::numberToString(limit)));
   return params;
}

void zoteroItemRequest(const std::string& key,
                       const std::string& path,
                       int start,
                       int limit,
                       json::Array accumulatedItems,
                       const ZoteroJsonResponseHandler& handler);

void zoteroItemRequestHandler(const std::string& key,
                              const std::string& path,
                              int start,
                              int limit,
                              json::Array accumulatedItems,
                              const ZoteroJsonResponseHandler& handler,
                              const core::Error& error,
                              int status,
                              core::json::Value json)
{
   if (error)
   {
      handler(error, status, json::Value());
   }
   else
   {
      // get the items and accumulate them
      json::Array itemsJson = json.getArray();
      std::copy(itemsJson.begin(), itemsJson.end(), std::back_inserter(accumulatedItems));

      // if the number of items returned is less than the limit then we are done
      if (itemsJson.getSize() < static_cast<size_t>(limit))
      {
         handler(Success(), 200, accumulatedItems);
      }
      // otherwise we need to make another request
      else
      {
         zoteroItemRequest(key, path, start + limit, limit, accumulatedItems, handler);
      }
   }
}

void zoteroItemRequest(const std::string& key,
                       const std::string& path,
                       int start,
                       int limit,
                       json::Array accumulatedItems,
                       const ZoteroJsonResponseHandler& handler)
{
   std::string schema = module_context::resourceFileAsString("schema/zotero-items.json");
   http::Fields params = zoteroItemRequestParams(start, limit);
   zoteroJsonRequest(key, path, params, schema,
                     boost::bind(zoteroItemRequestHandler,
                        key, path, start, limit, accumulatedItems, handler, _1, _2, _3
                     ));
}

void zoteroItemRequest(const std::string& key,
                       const std::string& path,
                       const ZoteroJsonResponseHandler& handler)
{
   zoteroItemRequest(key, path, 0, 100, json::Array(), handler);
}


void zoteroKeyInfo(const std::string& key, const ZoteroJsonResponseHandler& handler)
{
   std::string schema = module_context::resourceFileAsString("schema/zotero-key.json");

   boost::format fmt("keys/%s");
   zoteroJsonRequest("",
                     boost::str(fmt % key),
                     http::Fields(),
                     schema,
                     handler);
}


void zoteroCollections(const std::string& key, int userID, const ZoteroJsonResponseHandler& handler)
{
   std::string schema = module_context::resourceFileAsString("schema/zotero-collections.json");

   http::Fields params;
   params.push_back(std::make_pair("format", "json"));

   boost::format fmt("users/%d/collections");
   zoteroJsonRequest(key,
                     boost::str(fmt % userID),
                     params,
                     schema,
                     handler);
}

void zoteroItems(const std::string& key, int userID, const ZoteroJsonResponseHandler& handler)
{
   boost::format fmt("users/%d/items");
   zoteroItemRequest(key, boost::str(fmt % userID), handler);
}

void zoteroItemVersions(const std::string& key, int userID, int since, const ZoteroJsonResponseHandler& handler)
{
   http::Fields params;
   params.push_back(std::make_pair("format", "versions"));
   params.push_back(std::make_pair("since", safe_convert::numberToString(since)));
   boost::format fmt("users/%d/items");
   zoteroJsonRequest(key, boost::str(fmt % userID), params, "", handler);
}


void zoteroItemsForCollection(const std::string& key, int userID, const std::string& collectionID, const ZoteroJsonResponseHandler& handler)
{
   boost::format fmt("users/%d/collections/%s/items");
   zoteroItemRequest(key, boost::str(fmt % userID % collectionID), handler);
}


ZoteroCollection collectionFromItemsDownload(const ZoteroCollectionSpec& spec, const json::Value& json)
{
   ZoteroCollection collection(spec);
   json::Array itemsJson;
   json::Array resultItemsJson = json.getArray();
   std::transform(resultItemsJson.begin(), resultItemsJson.end(), std::back_inserter(itemsJson), [](const json::Value& resultItemJson) {
      return resultItemJson.getObject()["csljson"];
   });
   collection.items = itemsJson;
   return collection;
}

// keep a persistent mapping of apiKey to userId so we don't need to do the lookup each time
core::Settings s_userIdMap;


// utility class to download a set of zotero collections
class ZoteroCollectionsDownloader;
typedef boost::shared_ptr<ZoteroCollectionsDownloader> ZoteroCollectionsDownloaderPtr;

class ZoteroCollectionsDownloader : public boost::enable_shared_from_this<ZoteroCollectionsDownloader>
{
public:
   static ZoteroCollectionsDownloaderPtr create(const std::string& key, int userID,
                                                const std::vector<std::pair<std::string,ZoteroCollectionSpec> >& collections,
                                                ZoteroCollectionsHandler handler)
   {
       ZoteroCollectionsDownloaderPtr pDownloader(new ZoteroCollectionsDownloader(key, userID, collections, handler));
       pDownloader->downloadNextCollection();
       return pDownloader;
   }

public:

private:
   ZoteroCollectionsDownloader(const std::string& key, int userID,
                               const std::vector<std::pair<std::string,ZoteroCollectionSpec> >& collections, ZoteroCollectionsHandler handler)
      : key_(key), userID_(userID), handler_(handler)
   {
      for (auto collection : collections)
         collectionQueue_.push(collection);
   }

   void downloadNextCollection()
   {
      if (collectionQueue_.size() > 0)
      {
         // get next queue item
         auto next = collectionQueue_.front();
         collectionQueue_.pop();

         // download it
         zoteroItemsForCollection(key_, userID_, next.first, boost::bind(&ZoteroCollectionsDownloader::handleCollectionDownload,
                                                                          ZoteroCollectionsDownloader::shared_from_this(),
                                                                          next.second, _1, _2, _3));
      }
      else
      {
         handler_(Success(), collections_);
      }
   }

   void handleCollectionDownload(ZoteroCollectionSpec spec, const Error& error,int,json::Value jsonValue)
   {
      if (error)
      {
         handler_(error, std::vector<ZoteroCollection>());
      }
      else
      {
         // create collection
         ZoteroCollection collection = collectionFromItemsDownload(spec, jsonValue);

         // append to our collections
         collections_.push_back(collection);

         // next collection
         downloadNextCollection();
      }
   }

private:
   std::string key_;
   int userID_;
   ZoteroCollectionsHandler handler_;
   std::queue<std::pair<std::string,ZoteroCollectionSpec> > collectionQueue_;
   ZoteroCollections collections_;
};

void getWebLibraryForUser(const std::string& key,
                          int userID,
                          const ZoteroCollectionSpec& cacheSpec,
                          ZoteroCollectionsHandler handler)
{
   // first query for versions
   zoteroItemVersions(key, userID, cacheSpec.version, [key, userID, cacheSpec, handler](core::Error error, int, json::Value json) {
      if (error)
      {
         handler(error, std::vector<ZoteroCollection>());
      }
      else if (json.isObject() && json.getObject().getSize() == 0)
      {
         handler(Success(), std::vector<ZoteroCollection>{ cacheSpec });
      }
      else
      {
         zoteroItems(key, userID, [handler](Error error,int, json::Value json) {

            if (error)
            {
               handler(error, std::vector<ZoteroCollection>());
            }
            else
            {
               // calculate library version based on max version of downloded items
               int version = 0;
               json::Array itemsJson = json.getArray();
               std::for_each(itemsJson.begin(), itemsJson.end(), [&version](const json::Value& item) {
                  int itemVersion = item.getObject()[kVersion].getInt();
                  if (itemVersion > version)
                     version = itemVersion;
               });

               // create collection
               ZoteroCollectionSpec spec(kMyLibrary, version);
               ZoteroCollection collection = collectionFromItemsDownload(spec, json);

               // return it
               handler(Success(), std::vector<ZoteroCollection>{ collection });
            }
         });
      }
   });




}


void getWebCollectionsForUser(const std::string& key,
                              int userID,
                              const std::vector<std::string>& collections,
                              const ZoteroCollectionSpecs& cacheSpecs,
                              ZoteroCollectionsHandler handler)
{
   // lookup all collections for the user
   zoteroCollections(key, userID, [key, userID, collections, cacheSpecs, handler](const Error& error,int,json::Value jsonValue) {

      if (error)
      {
         handler(error, std::vector<ZoteroCollection>());
         return;
      }

      // divide collections into ones we need to do a download for, and one that
      // we already have an up to date version for
      ZoteroCollections upToDateCollections;
      std::vector<std::pair<std::string, ZoteroCollectionSpec>> downloadCollections;

      // download items for specified collections
      json::Array collectionsJson = jsonValue.getArray();
      for (auto json : collectionsJson)
      {
         json::Object collectionJson = json.getObject()["data"].getObject();
         std::string collectionID = collectionJson["key"].getString();
         std::string name = collectionJson[kName].getString();
         int version = collectionJson[kVersion].getInt();

         // see if this is a requested collection
         bool requested =
           collections.size() == 0 || // all collections requested
           std::count_if(collections.begin(),
                         collections.end(),
                         [name](const std::string& str) { return boost::algorithm::iequals(name, str); }) > 0 ;

         if (requested)
         {
            // see if we need to do a download for this collection
            ZoteroCollectionSpecs::const_iterator it = std::find_if(
              cacheSpecs.begin(), cacheSpecs.end(), [name](ZoteroCollectionSpec spec) { return boost::algorithm::iequals(spec.name,name); }
            );
            if (it != cacheSpecs.end())
            {
               ZoteroCollectionSpec collectionSpec(name, version);
               if (it->version < version)
                  downloadCollections.push_back(std::make_pair(collectionID, collectionSpec));
               else
                  upToDateCollections.push_back(collectionSpec);
            }
            else
            {
               downloadCollections.push_back(std::make_pair(collectionID, ZoteroCollectionSpec(name, version)));
            }
         }
      }

      // do the download
      ZoteroCollectionsDownloader::create(key, userID, downloadCollections, [handler, upToDateCollections](Error error,ZoteroCollections collections) {

         if (error)
         {
            handler(error, ZoteroCollections());
         }
         else
         {
            // append downloaded collections to already up to date collections and return them
            std::copy(upToDateCollections.begin(), upToDateCollections.end(), std::back_inserter(collections));
            handler(Success(), collections);
         }

      });
   });
}


void withUserId(const std::string& key, boost::function<void(const Error&,int)> handler)
{
   // see if we already have the user id
   if (s_userIdMap.contains(key))
   {
      // execute the handler
      handler(Success(), s_userIdMap.getInt(key));
   }
   else
   {
      // get the user id for this key
      zoteroKeyInfo(key, [key, handler](const Error& error,int,json::Value jsonValue) {

         if (error)
         {
            handler(error, 0);
            return;
         }

         // get the and cache it
         int userID = jsonValue.getObject()["userID"].getInt();
         s_userIdMap.set(key, userID);

         // execute the handler
         handler(Success(), userID);
      });
   }
}


void getWebLibrary(const std::string& key,
                   const ZoteroCollectionSpec& cacheSpec,
                   ZoteroCollectionsHandler handler)
{
   withUserId(key, [key, cacheSpec, handler](Error error, int userID) {
      if (error)
      {
         handler(error, std::vector<ZoteroCollection>());
         return;
      }
      else
      {
         getWebLibraryForUser(key, userID, cacheSpec, handler);
      }
   });
}


void getWebCollections(const std::string& key,
                       const std::vector<std::string>& collections,
                       const ZoteroCollectionSpecs& cacheSpecs,
                       ZoteroCollectionsHandler handler)
{
   withUserId(key, [key, collections, cacheSpecs, handler](Error error, int userID) {
      if (error)
      {
         handler(error, std::vector<ZoteroCollection>());
         return;
      }
      else
      {
         getWebCollectionsForUser(key, userID, collections, cacheSpecs, handler);
      }
   });
}

} // end anonymous namespace


void validateWebApiKey(const std::string& key, boost::function<void(bool)> handler)
{
   zoteroKeyInfo(key, [handler](const Error& error,int,json::Value) {
      if (error)
      {
         std::string err = core::errorDescription(error);
         if (!is404Error(err))
            LOG_ERROR(error);
         handler(false);
      }
      else
      {
         handler(true);
      }
   });
}

ZoteroCollectionSource webCollections()
{
   // one time initialization of user id map
   static bool initialized = false;
   if (!initialized)
   {
      Error error = s_userIdMap.initialize(module_context::userScratchPath().completeChildPath("zotero-userid"));
      if (error)
         LOG_ERROR(error);
   }

   ZoteroCollectionSource source;
   source.getLibrary = getWebLibrary;
   source.getCollections = getWebCollections;
   return source;
}

} // end namespace collections
} // end namespace zotero
} // end namespace modules
} // end namespace session
} // end namespace rstudio
