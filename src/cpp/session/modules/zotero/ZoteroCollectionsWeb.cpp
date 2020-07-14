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

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace zotero {
namespace collections {

namespace {

const char * const kZoteroApiHost = "https://api.zotero.org";

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
      }
      else
      {
         queryParams.push_back(std::make_pair("key", key));
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


void zoteroItemRequest(const std::string& key, const std::string& path, const ZoteroJsonResponseHandler& handler)
{

   const char * const kItemSchema = R"(
   {
     "$id": "http://rstudio.org/schemas/zotero-items.json",
     "$schema": "http://json-schema.org/schema#",
     "title": "Zotero Items Metadata Schema",
     "type": "array",
     "items": {
       "type": "object",
       "properties" : {
           "key" : {
               "type": "string"
           },
           "version": {
               "type": "number"
           },
           "library": {
             "type": "object",
             "properties": {
                  "type": {
                        "type": "string"
                   },
                   "id": {
                     "type": "number"
                   },
                   "name": {
                     "type": "string"
                   }
             }
           },
           "csljson": {
             "type": "object"
            }
       }
     }
   })";


   http::Fields params;
   params.push_back(std::make_pair("format", "json"));
   params.push_back(std::make_pair("include", "csljson"));
   params.push_back(std::make_pair("itemType", "-attachment"));

   zoteroJsonRequest(key,
                     path,
                     params,
                     kItemSchema,
                     handler);
}

void zoteroKeyInfo(const std::string& key, const ZoteroJsonResponseHandler& handler)
{
   const char * const kKeySchema = R"(
   {
     "$id": "http://rstudio.org/schemas/zotero-key.json",
     "$schema": "http://json-schema.org/schema#",
     "title": "Zotero Key Metadata Schema",
     "type": "object",
     "properties": {
       "key": {
         "type": "string"
       },
       "userID": {
         "type": "number"
       },
       "username": {
         "type": "string"
       },
       "access": {
         "type": "object",
         "properties": {
           "user": {
             "type": "object",
             "properties": {
               "library": {
                 "type": "boolean"
               },
               "files": {
                 "type": "boolean"
               }
             }
           },
           "groups": {
             "type": "object",
             "properties": {
               "all": {
                 "type": "object",
                 "properties": {
                   "library": {
                     "type": "boolean"
                   },
                   "write": {
                     "type": "boolean"
                   }
                 }
               }
             }
           }
         }
       }
     }
   })";

   boost::format fmt("keys/%s");
   zoteroJsonRequest("",
                     boost::str(fmt % key),
                     http::Fields(),
                     kKeySchema,
                     handler);
}


void zoteroCollections(const std::string& key, int userID, const ZoteroJsonResponseHandler& handler)
{
   const char * const kCollectionsSchema = R"(
   {
     "$id": "http://rstudio.org/schemas/zotero-collections.json",
     "$schema": "http://json-schema.org/schema#",
     "title": "Zotero Collections Metadata Schema",
     "type": "array",
     "items": {
       "type": "object",
       "properties" : {
            "key" : {
               "type": "string"
           },
           "version": {
               "type": "number"
           },
           "library": {
             "type": "object",
             "properties": {
                  "type": {
                        "type": "string"
                   },
                   "id": {
                     "type": "number"
                   },
                   "name": {
                     "type": "string"
                   }
             }
           },
           "data": {
             "type": "object",
             "properties": {
               "key" : {
                 "type": "string"
               },
               "version": {
                 "type": "number"
               },
               "name": {
                 "type": "string"
               }
             }
           }
       }
     }
   })";

   http::Fields params;
   params.push_back(std::make_pair("format", "json"));

   boost::format fmt("users/%d/collections");
   zoteroJsonRequest(key,
                     boost::str(fmt % userID),
                     params,
                     kCollectionsSchema,
                     handler);
}


void zoteroItemsForCollection(const std::string& key, int userID, const std::string& collectionID, const ZoteroJsonResponseHandler& handler)
{
   boost::format fmt("users/%d/collections/%s/items");
   zoteroItemRequest(key, boost::str(fmt % userID % collectionID), handler);
}

/*
void zoteroDeleted(const std::string& key, int userID, int since, const ZoteroJsonResponseHandler& handler)
{
   const char * const kDeletedSchema = R"(
   {
     "$id": "http://rstudio.org/schemas/zotero-deleted.json",
     "$schema": "http://json-schema.org/schema#",
     "title": "Zotero Deleted Metadata Schema",
     "type": "object",
      "properties" : {
         "collections" : {
            "type": "array",
            "items": {
               "type": "string"
            }
         },
         "items" : {
            "type": "array",
            "items": {
               "type": "string"
            }
         }
      }
   })";

   http::Fields params;
   params.push_back(std::make_pair("since", std::to_string(since)));

   boost::format fmt("users/%d/deleted");
   zoteroJsonRequest(key,
                     boost::str(fmt % userID),
                     params,
                     kDeletedSchema,
                     handler);
}
*/


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
         ZoteroCollection collection;
         collection.name = spec.name;
         collection.version = spec.version;
         json::Array itemsJson;
         json::Array resultItemsJson = jsonValue.getArray();
         std::transform(resultItemsJson.begin(), resultItemsJson.end(), std::back_inserter(itemsJson), [](const json::Value& resultItemJson) {
            return resultItemJson.getObject()["csljson"];
         });
         collection.items = itemsJson;

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


void getWebCollectionsForUser(const std::string& key, int userID, const ZoteroCollectionSpecs& specs, ZoteroCollectionsHandler handler)
{
   // lookup all collections for the user
   zoteroCollections(key, userID, [key, userID, specs, handler](const Error& error,int,json::Value jsonValue) {

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

         // see if we need to do a download for this collection
         ZoteroCollectionSpecs::const_iterator it = std::find_if(
           specs.begin(), specs.end(), [name](ZoteroCollectionSpec spec) { return spec.name == name; }
         );
         if (it != specs.end())
         {
            ZoteroCollectionSpec collectionSpec(name, version);
            if (it->version < version)
               downloadCollections.push_back(std::make_pair(collectionID, collectionSpec));
            else
               upToDateCollections.push_back(collectionSpec);
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


void getWebCollections(const std::string& key, const ZoteroCollectionSpecs& specs, ZoteroCollectionsHandler handler)
{
   // short circuit for no collection specs
   if (specs.size() == 0)
   {
      handler(Success(), std::vector<ZoteroCollection>());
      return;
   }

   // see if we already have the user id
   if (s_userIdMap.contains(key))
   {
      getWebCollectionsForUser(key, s_userIdMap.getInt(key), specs, handler);
   }
   else
   {
      // get the user id for this key
      zoteroKeyInfo(key, [key, handler, specs](const Error& error,int,json::Value jsonValue) {

         if (error)
         {
            handler(error, std::vector<ZoteroCollection>());
            return;
         }

         // get the and cache it
         int userID = jsonValue.getObject()["userID"].getInt();
         s_userIdMap.set(key, userID);

         // get the collections
         getWebCollectionsForUser(key, userID, specs, handler);
      });
   }
}

} // end anonymous namespace

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
   source.getCollections = getWebCollections;
   return source;
}

} // end namespace collections
} // end namespace zotero
} // end namespace modules
} // end namespace session
} // end namespace rstudio
