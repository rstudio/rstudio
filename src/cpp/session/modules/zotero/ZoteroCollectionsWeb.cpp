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

#include <boost/enable_shared_from_this.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/json/Json.hpp>

#include <core/system/Process.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionAsyncDownloadFile.hpp>

#include "ZoteroUtil.hpp"
#include "ZoteroCSL.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace zotero {
namespace collections {

namespace {

const char * const kZoteroApiHost = "https://api.zotero.org";
const char * const kZoteroApiVersion = "3";

const char * const kUserScope = "users";
const char * const kGroupScope = "groups";

typedef boost::function<void(core::Error,int,core::json::Value)> ZoteroJsonResponseHandler;

void zoteroJsonRequest(std::string key,
                       std::string resource,
                       http::Fields queryParams,
                       std::string schema,
                       ZoteroJsonResponseHandler handler)
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
   TRACE(url);
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
   params.push_back(std::make_pair("include", "csljson,data"));
   params.push_back(std::make_pair("itemType", "-attachment"));
   params.push_back(std::make_pair("start", safe_convert::numberToString(start)));
   params.push_back(std::make_pair("limit", safe_convert::numberToString(limit)));
   return params;
}

void zoteroItemRequest(std::string key,
                       std::string path,
                       int start,
                       int limit,
                       json::Array accumulatedItems,
                       ZoteroJsonResponseHandler handler);

void zoteroItemRequestHandler(std::string key,
                              std::string path,
                              int start,
                              int limit,
                              json::Array accumulatedItems,
                              ZoteroJsonResponseHandler handler,
                              core::Error error,
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

void zoteroItemRequest(std::string key,
                       std::string path,
                       int start,
                       int limit,
                       json::Array accumulatedItems,
                       ZoteroJsonResponseHandler handler)
{
   std::string schema = module_context::resourceFileAsString("schema/zotero-items.json");
   http::Fields params = zoteroItemRequestParams(start, limit);
   zoteroJsonRequest(key, path, params, schema,
                     boost::bind(zoteroItemRequestHandler,
                        key, path, start, limit, accumulatedItems, handler, _1, _2, _3
                     ));
}

void zoteroItemRequest(std::string key,
                       std::string path,
                       ZoteroJsonResponseHandler handler)
{
   zoteroItemRequest(key, path, 0, 100, json::Array(), handler);
}


void zoteroKeyInfo(std::string key, ZoteroJsonResponseHandler handler)
{
   std::string schema = module_context::resourceFileAsString("schema/zotero-key.json");

   boost::format fmt("keys/%s");
   zoteroJsonRequest("",
                     boost::str(fmt % key),
                     http::Fields(),
                     schema,
                     handler);
}

void zoteroGroups(std::string key, int userID, ZoteroJsonResponseHandler handler)
{
   std::string schema = module_context::resourceFileAsString("schema/zotero-groups.json");

   boost::format fmt("users/%d/groups");
   zoteroJsonRequest(key,
                     boost::str(fmt % userID),
                     http::Fields(),
                     schema,
                     handler);
}

void zoteroCollections(std::string key, const std::string& scope, int id, ZoteroJsonResponseHandler handler)
{
   std::string schema = module_context::resourceFileAsString("schema/zotero-collections.json");

   http::Fields params;
   params.push_back(std::make_pair("format", "json"));

   boost::format fmt("%s/%d/collections");
   zoteroJsonRequest(key,
                     boost::str(fmt % scope % id),
                     params,
                     schema,
                     handler);
}

void zoteroItems(std::string key, const std::string& scope, int id, ZoteroJsonResponseHandler handler)
{
   boost::format fmt("%s/%d/items");
   zoteroItemRequest(key, boost::str(fmt % scope % id), handler);
}

void zoteroItemVersions(std::string key, const std::string& scope, int id, int since, ZoteroJsonResponseHandler handler)
{
   http::Fields params;
   params.push_back(std::make_pair("format", "versions"));
   params.push_back(std::make_pair("itemType", "-attachment"));
   params.push_back(std::make_pair("since", safe_convert::numberToString(since)));
   boost::format fmt("%s/%d/items");
   zoteroJsonRequest(key, boost::str(fmt % scope % id), params, "", handler);
}

ZoteroCollection collectionFromItemsDownload(ZoteroCollectionSpec spec, const json::Value& json)
{
   ZoteroCollection collection(spec);
   json::Array itemsJson;
   json::Array resultItemsJson = json.getArray();
   std::transform(resultItemsJson.begin(), resultItemsJson.end(), std::back_inserter(itemsJson), [spec](const json::Value& resultItemJson) {

      // The ids generated by Web Zotero are pretty rough, so just strip them
      // and allow the caller to generate an id if they'd like
      json::Object cslResult = resultItemJson.getObject()["csljson"].getObject();
      cslResult["id"] = "";

      if (resultItemJson.getObject().hasMember("data"))
      {
         json::Object dataObject = resultItemJson.getObject()["data"].getObject();
        // Deal with 'Cheater fields' embedded in note or extra field
        if (dataObject.hasMember("extra"))
        {
           const json::Value extraJson = dataObject["extra"];
           if (extraJson.isString())
           {
              convertCheaterKeysToCSLForValue(cslResult, extraJson.getString());
           }
        }

        // set libraryId
        cslResult["libraryID"] = spec.key;

        // get collectionKeys
        json::Array collectionKeys;
        collectionKeys.push_back(json::Value(spec.key));
        if (dataObject.hasMember("collections"))
        {
           json::Array collectionsJson = dataObject["collections"].getArray();
           std::copy(collectionsJson.begin(), collectionsJson.end(), std::back_inserter(collectionKeys));
        }
        cslResult["collectionKeys"] = collectionKeys;
      }
      convertCheaterKeysToCSLForField(cslResult, "note");
      return cslResult;
   });
   collection.items = itemsJson;
   return collection;
}


struct DownloadRequest
{
   DownloadRequest(const std::string& scope, int id, const ZoteroCollectionSpec& spec)
      : scope(scope), id(id), spec(spec) {}
   std::string scope;
   int id;
   ZoteroCollectionSpec spec;
};


// utility class to download a set of zotero collection specs
class ZoteroCollectionSpecsDownloader;
typedef boost::shared_ptr<ZoteroCollectionSpecsDownloader> ZoteroCollectionSpecsDownloaderPtr;

class ZoteroCollectionSpecsDownloader : public boost::enable_shared_from_this<ZoteroCollectionSpecsDownloader>
{
public:
   static ZoteroCollectionSpecsDownloaderPtr create(std::string key,
                                                    const std::vector<DownloadRequest>& downloads,
                                                    ZoteroCollectionSpecsHandler handler)
   {
       ZoteroCollectionSpecsDownloaderPtr pDownloader(new ZoteroCollectionSpecsDownloader(key, downloads, handler));
       pDownloader->nextDownload();
       return pDownloader;
   }

public:

private:
   ZoteroCollectionSpecsDownloader(std::string key,
                                   const std::vector<DownloadRequest>& downloads,
                                   ZoteroCollectionSpecsHandler handler)
      : key_(key), handler_(handler)
   {
      for (auto download : downloads)
      {
         collectionSpecs_.push_back(download.spec);
         downloadQueue_.push(download);
      }
   }

   void nextDownload()
   {
      if (downloadQueue_.size() > 0)
      {
         // get next queue item
         auto next = downloadQueue_.front();
         downloadQueue_.pop();

         // download the spec
         zoteroCollections(key_, next.scope, next.id,  boost::bind(&ZoteroCollectionSpecsDownloader::handleDownload,
                                                                   ZoteroCollectionSpecsDownloader::shared_from_this(),
                                                                   next.spec, _1, _2, _3));
      }
      else
      {
         handler_(Success(), collectionSpecs_);
      }
   }

   void handleDownload(ZoteroCollectionSpec spec, const Error& error,int,json::Value jsonValue)
   {
      if (error)
      {
         handler_(error, std::vector<ZoteroCollectionSpec>());
      }
      else
      {
         // read the sub-collection specs
         json::Array collectionSpecsJson = jsonValue.getArray();
         for (auto collectionSpecJson : collectionSpecsJson)
         {
            json::Object collectionJson = collectionSpecJson.getObject()["data"].getObject();
            int version = collectionJson[kVersion].getInt();
            std::string name = collectionJson[kName].getString();
            std::string collectionID = collectionJson[kKey].getString();

            // Parent collection will either be the collection key of the parent
            // or a boolean false (thanks guys). in the latter case us the key
            // of the parent spec
            std::string parentCollectionID = "";
            if (collectionJson["parentCollection"].isString())
            {
               parentCollectionID = collectionJson["parentCollection"].getString();
            }
            // at the top level of the root, use root key as parent and update version
            else
            {
               // set parent
               parentCollectionID = spec.key;

               // update the version
               ZoteroCollectionSpecs::iterator it = std::find_if(collectionSpecs_.begin(), collectionSpecs_.end(),
                                                                 [parentCollectionID](const ZoteroCollectionSpec& spec) {
                  return spec.key == parentCollectionID;
               });
               if (it != collectionSpecs_.end())
               {
                  it->version = std::max(it->version, version);
               }
            }

            collectionSpecs_.push_back(ZoteroCollectionSpec(name, collectionID, parentCollectionID, version));
         }

         // next collection
         nextDownload();
      }
   }

private:
   std::string key_;
   ZoteroCollectionSpecsHandler handler_;
   std::queue<DownloadRequest> downloadQueue_;
   ZoteroCollectionSpecs collectionSpecs_;
};


// utility class to download a set of zotero collections
class ZoteroCollectionsDownloader;
typedef boost::shared_ptr<ZoteroCollectionsDownloader> ZoteroCollectionsDownloaderPtr;

class ZoteroCollectionsDownloader : public boost::enable_shared_from_this<ZoteroCollectionsDownloader>
{
public:
   static ZoteroCollectionsDownloaderPtr create(std::string key,
                                                const std::vector<DownloadRequest>& downloads,
                                                ZoteroCollectionsHandler handler)
   {
       ZoteroCollectionsDownloaderPtr pDownloader(new ZoteroCollectionsDownloader(key, downloads, handler));
       pDownloader->nextDownload();
       return pDownloader;
   }

public:

private:
   ZoteroCollectionsDownloader(std::string key,
                               const std::vector<DownloadRequest>& downloads,
                               ZoteroCollectionsHandler handler)
      : key_(key), handler_(handler)
   {
      for (auto download : downloads)
         downloadQueue_.push(download);
   }

   void nextDownload()
   {
      if (downloadQueue_.size() > 0)
      {
         // get next queue item
         auto request = downloadQueue_.front();
         downloadQueue_.pop();

         // query for new/updated items
         zoteroItemVersions(key_, request.scope, request.id, request.spec.version,
                            boost::bind(&ZoteroCollectionsDownloader::handleItemVersions,
                            ZoteroCollectionsDownloader::shared_from_this(),
                            request, _1, _2, _3));
      }
      else
      {
         handler_(Success(), collections_, "");
      }
   }

   void handleItemVersions(DownloadRequest request, const Error& error,int,json::Value jsonValue)
   {
      if (error)
      {
         handler_(error, std::vector<ZoteroCollection>(), "");
      }
      else if (jsonValue.isObject() && jsonValue.getObject().getSize() == 0)
      {
         handler_(Success(), std::vector<ZoteroCollection>{ ZoteroCollection(request.spec) }, "");
      }
      else
      {
         zoteroItems(key_, request.scope, request.id, boost::bind(&ZoteroCollectionsDownloader::handleDownload,
                                                                  ZoteroCollectionsDownloader::shared_from_this(),
                                                                  request.spec, _1, _2, _3));
      }
   }

   void handleDownload(ZoteroCollectionSpec spec, const Error& error,int,json::Value jsonValue)
   {
      if (error)
      {
         handler_(error, std::vector<ZoteroCollection>(), "");
      }
      else
      {
         // calculate library version from max of items downloaed
         int version = 0;
         json::Array itemsJson = jsonValue.getArray();
         std::for_each(itemsJson.begin(), itemsJson.end(), [&version](const json::Value& item) {
            int itemVersion = item.getObject()[kVersion].getInt();
            if (itemVersion > version)
               version = itemVersion;
         });

         // update version
         spec.version = version;

         // create collection
         ZoteroCollection collection = collectionFromItemsDownload(spec, jsonValue);

         // append to our collections
         collections_.push_back(collection);

         // next collection
         nextDownload();
      }
   }

private:
   std::string key_;
   ZoteroCollectionsHandler handler_;
   std::queue<DownloadRequest> downloadQueue_;
   ZoteroCollections collections_;
};


// keep persistent mappings of apiKey to userId and group name to groupID
// so we don't need to do the lookups each time

// userId map is permanent and keys are globally unique
core::Settings s_userIdMap;

// groupId map is per-session as you could in theory remove a group then
// create new group of an identical name
std::map<std::string,int> s_groupIdMap;


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

void withGroupsIds(const std::string& key, int userID, const std::vector<std::string>& groups, bool forceAll,
                   boost::function<void(const Error&, std::map<std::string,int> groups)> handler)
{
   // short circuit for empty groups not forcing all
   if (groups.size() == 0 && !forceAll)
   {
      handler(Success(), s_groupIdMap);
      return;
   }

   // do we have any missing groups?
   bool missingGroups = false;
   for (auto group : groups)
   {
      if (s_groupIdMap.find(group) == s_groupIdMap.end())
      {
         missingGroups = true;
         break;
      }
   }

   // serve from cache if there are no missing groups and aren't getting all groups
   if (!missingGroups && !forceAll)
   {
      handler(Success(), s_groupIdMap);
   }
   else
   {
      zoteroGroups(key, userID, [handler](const Error& error,int,json::Value jsonValue) {

         if (error)
         {
            handler(error, std::map<std::string,int>());
            return;
         }

         // cache the results
         s_groupIdMap.clear();
         json::Array groupsJson = jsonValue.getArray();
         for (auto groupJsonValue : groupsJson)
         {
            auto groupJson = groupJsonValue.getObject();
            int id = groupJson["id"].getInt();
            std::string name = groupJson["data"].getObject()["name"].getString();
            s_groupIdMap[name] = id;
         }

         // execute the handler
         handler(Success(), s_groupIdMap);
      });
   }

}


void getWebCollectionsForUser(std::string key,
                              int userID,
                              std::vector<std::string> collections,
                              ZoteroCollectionSpecs cacheSpecs,
                              ZoteroCollectionsHandler handler)
{
   // download requests
   boost::shared_ptr<std::vector<DownloadRequest>> pDownloads(new std::vector<DownloadRequest>());

   // partition into 'My Library' and group requests
   auto it = std::find(collections.begin(), collections.end(), kMyLibrary);
   if (it != collections.end())
   {
      ZoteroCollectionSpec myLibrary(kMyLibrary, safe_convert::numberToString(userID), "");
      pDownloads->push_back(DownloadRequest(kUserScope, userID, myLibrary));
      collections.erase(it);
   }

   withGroupsIds(key, userID, collections, false, [key,cacheSpecs,handler,collections,pDownloads](Error error,std::map<std::string,int> groups) {

      if (error)
      {
         handler(error, ZoteroCollections(), "");
         return;
      }

      // create download requests for valid groups
      for (auto collection : collections)
      {
         auto it = groups.find(collection);
         if (it != groups.end())
         {
            int groupId = it->second;
            ZoteroCollectionSpec spec(collection, safe_convert::numberToString(groupId), "");
            pDownloads->push_back(DownloadRequest(kGroupScope, groupId, spec));
         }
      }

      // associate cache specs with download requests (so we can propogate the version)
      for (ZoteroCollectionSpec cacheSpec : cacheSpecs)
      {
         auto it = std::find_if(pDownloads->begin(), pDownloads->end(), [cacheSpec](const DownloadRequest& request) {
            return request.spec.name == cacheSpec.name;
         });
         if (it != pDownloads->end())
            it->spec = cacheSpec; // now we have the cache version
      }

      // perform the downloads
      ZoteroCollectionsDownloader::create(key, *pDownloads, handler);
   });

}

void getWebLibraryNames(std::string key, ZoteroLibrariesHandler handler)
{
   withUserId(key,[key,handler](Error error, int userID) {
      if (error)
      {
         handler(error, std::vector<std::string>());
         return;
      }
      else
      {
         withGroupsIds(key, userID, std::vector<std::string>(), true, [handler](Error error,std::map<std::string,int> groups) {

            if (error)
            {
               handler(error, std::vector<std::string>());
               return;
            }

            std::vector<std::string> libraries;
            for (auto group : groups)
               libraries.push_back(group.first);

            handler(Success(), libraries);

         });


      }
   });
}

void getWebCollectionSpecs(std::string key, std::vector<std::string> collections, ZoteroCollectionSpecsHandler handler)
{  
   withUserId(key, [key,collections,handler](Error error, int userID) {
      if (error)
      {
         handler(error, std::vector<ZoteroCollectionSpec>());
         return;
      }
      else
      {
         // force all if no collections are passed
         bool forceAll = collections.size() == 0;

         // create list of downloads
         boost::shared_ptr<std::vector<DownloadRequest>> pDownloads(new std::vector<DownloadRequest>());

         // partition into 'My Library' and group requests
         std::vector<std::string> specCollections = collections;
         auto it = std::find(specCollections.begin(), specCollections.end(), kMyLibrary);
         if (forceAll || it != specCollections.end())
         {
            ZoteroCollectionSpec myLibrary(kMyLibrary, safe_convert::numberToString(userID), "");
            pDownloads->push_back(DownloadRequest(kUserScope, userID, myLibrary));
            if (it != specCollections.end())
               specCollections.erase(it);
         }

         // query for group libraries if required
         withGroupsIds(key, userID, specCollections, forceAll, [key,collections,handler,pDownloads, forceAll](Error error,std::map<std::string,int> groups) {

            if (error)
            {
               handler(error, ZoteroCollectionSpecs());
               return;
            }

            // create specs (filter on collections if a whitelist was provided)
            for (auto group : groups)
            {
               std::string groupName = group.first;
               if (groupName != kMyLibrary)
               {
                  if (forceAll || std::find(collections.begin(), collections.end(), groupName) != collections.end())
                  {
                     int groupId = group.second;
                     ZoteroCollectionSpec spec(groupName, safe_convert::numberToString(groupId), "");
                     pDownloads->push_back(DownloadRequest(kGroupScope, groupId, spec));
                  }
               }
            }

            // perform the downloads
            ZoteroCollectionSpecsDownloader::create(key, *pDownloads, handler);
         });
      }
   });
}

void getWebCollections(std::string key,
                       std::vector<std::string> collections,
                       ZoteroCollectionSpecs cacheSpecs,
                       ZoteroCollectionsHandler handler)
{
   withUserId(key, [key, collections, cacheSpecs, handler](Error error, int userID) {
      if (error)
      {
         handler(error, std::vector<ZoteroCollection>(), "");
         return;
      }
      else
      {
         getWebCollectionsForUser(key, userID, collections, cacheSpecs, handler);
      }
   });
}

} // end anonymous namespace


void validateWebApiKey(std::string key, boost::function<void(bool)> handler)
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
      Error error = s_userIdMap.initialize(module_context::userScratchPath()
         .completeChildPath("zotero")
         .completeChildPath("userid"));
      if (error)
         LOG_ERROR(error);
   }

   ZoteroCollectionSource source;
   source.getCollections = getWebCollections;
   source.getLibraryNames = getWebLibraryNames;
   source.getCollectionSpecs = getWebCollectionSpecs;
   return source;
}

} // end namespace collections
} // end namespace zotero
} // end namespace modules
} // end namespace session
} // end namespace rstudio
