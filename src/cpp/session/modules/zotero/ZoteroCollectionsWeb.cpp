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
         {
            error = resultJson.parseAndValidate(result.stdOut, schema);
         } else {
            error = resultJson.parse(result.stdOut);
         }

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


void getWebCollections(const std::string& key, const ZoteroCollectionSpecs& specs, ZoteroCollectionsHandler handler)
{
   // short circuit for no collection specs
   if (specs.size() == 0)
   {
      handler(Success(), std::vector<ZoteroCollection>());
      return;
   }


   // get the requested collection(s)
   zoteroKeyInfo(key, [key, handler, specs](const Error& error,int,json::Value jsonValue) {

      if (error)
      {
         handler(error, std::vector<ZoteroCollection>());
         return;
      }

      int userID = jsonValue.getObject()["userID"].getInt();

      zoteroCollections(key, userID, [key, userID, handler, specs](const Error& error,int,json::Value jsonValue) {

         if (error)
         {
            handler(error, std::vector<ZoteroCollection>());
            return;
         }

         // TODO: support multiple collections
         std::string targetCollection = specs[0].name;
         json::Array jsonCollections = jsonValue.getArray();
         bool foundCollection = false;
         for (std::size_t i = 0; i<jsonCollections.getSize(); i++)
         {
            json::Object collectionJson = jsonCollections[i].getObject()["data"].getObject();
            std::string name = collectionJson["name"].getString();
            if (name == targetCollection)
            {
               std::string collectionID = collectionJson["key"].getString();
               int version = collectionJson["version"].getInt();
               zoteroItemsForCollection(key, userID, collectionID, [name, version, handler](const Error& error,int,json::Value jsonValue) {

                  if (error)
                  {
                     handler(error, std::vector<ZoteroCollection>());
                     return;
                  }

                  // create response object
                  ZoteroCollection collection;
                  collection.name = name;
                  collection.version = version;
                  json::Array itemsJson;
                  json::Array resultItemsJson = jsonValue.getArray();
                  std::transform(resultItemsJson.begin(), resultItemsJson.end(), std::back_inserter(itemsJson), [](const json::Value& resultItemJson) {
                     return resultItemJson.getObject()["csljson"];
                  });
                  collection.items = itemsJson;

                  // satisfy continutation
                  handler(Success(), std::vector<ZoteroCollection>{ collection });

               });

               foundCollection = true;
               break;
            }

         }

         // didn't find a target, so return empty array
         if (!foundCollection)
         {
            handler(Success(), std::vector<ZoteroCollection>());
         }
      });

   });

}

} // end anonymous namespace

ZoteroCollectionSource webCollections()
{
   ZoteroCollectionSource source;
   source.getCollections = getWebCollections;
   return source;
}

} // end namespace collections
} // end namespace zotero
} // end namespace modules
} // end namespace session
} // end namespace rstudio
