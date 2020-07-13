/*
 * SessionPanmirrorZotero.cpp
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

#include "SessionPanmirrorZotero.hpp"

#include <shared_core/Error.hpp>
#include <shared_core/json/Json.hpp>

#include <core/Exec.hpp>

#include <core/system/Process.hpp>

#include <session/prefs/UserPrefs.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/projects/SessionProjects.hpp>
#include <session/SessionAsyncDownloadFile.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace panmirror {
namespace zotero {

namespace {

const char * const kZoteroApiHost = "https://api.zotero.org";

std::string zoteroApiKey()
{
   return prefs::userPrefs().zoteroApiKey();
}

typedef boost::function<void(const Error&,int,json::Value)> ZoteroJsonResponseHandler;

void continuationError(const json::JsonRpcFunctionContinuation& cont, const Error& error)
{
   json::JsonRpcResponse response;
   LOG_ERROR(error);
   cont(error, &response);
}


void zoteroJsonRequest(const std::string& resource,
                       http::Fields queryParams,
                       bool authenticated,
                       const std::string& schema,
                       const ZoteroJsonResponseHandler& handler)
{
   // authorize using header or url param as required
   http::Fields headers;
   if (authenticated)
   {
      if (module_context::hasMinimumRVersion("3.6"))
      {
         boost::format fmt("Bearer %s");
         headers.push_back(std::make_pair("Authorization", "Bearer " + zoteroApiKey()));
      }
      else
      {
         queryParams.push_back(std::make_pair("key", zoteroApiKey()));
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

void zoteroJsonRequest(const std::string& resource,
                      http::Fields queryParams,
                      const ZoteroJsonResponseHandler& handler)
{
   zoteroJsonRequest(resource, queryParams, true, "", handler);
}



void zoteroKeyInfo(const ZoteroJsonResponseHandler& handler)
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
   zoteroJsonRequest(boost::str(fmt % zoteroApiKey()),
                     http::Fields(),
                     false,
                     kKeySchema,
                     handler);
}

void zoteroCollections(int userID, const ZoteroJsonResponseHandler& handler)
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
   zoteroJsonRequest(boost::str(fmt % userID),
                     params,
                     true,
                     kCollectionsSchema,
                     handler);
}

void zoteroItemRequest(const std::string& path, const ZoteroJsonResponseHandler& handler)
{

   const char * const kItemSchema = R"(
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
   params.push_back(std::make_pair("itemType", "-attachment"));

   zoteroJsonRequest(path,
                     params,
                     true,
                     kItemSchema,
                     handler);
}

void zoteroItemsForCollection(int userID, const std::string& collectionID, const ZoteroJsonResponseHandler& handler)
{
   boost::format fmt("users/%d/collections/%s/items");
   zoteroItemRequest(boost::str(fmt % userID % collectionID),
                     handler);
}

void zoteroDeleted(int userID, int since, const ZoteroJsonResponseHandler& handler)
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
   zoteroJsonRequest(boost::str(fmt % userID),
                     params,
                     true,
                     kDeletedSchema,
                     handler);
}

void onDeferredInit(bool)
{
   /*
   zoteroKeyInfo([](const Error& error,int status,json::Value jsonValue) {


      if (error)
      {
         LOG_ERROR(error);
         return;
      }

      int userID;
      json::Object jsonResult = jsonValue.getObject();
      Error jsonError = core::json::readObject(jsonResult, "userID", userID);
      if (jsonError)
      {
         LOG_ERROR(jsonError);
         return;
      }


      zoteroItemsForCollection(userID, "HFB52FJF", [](const Error& error,int status,json::Value jsonValue) {
          if (error)
          {
             LOG_ERROR(error);
             return;
          }
          jsonValue.writeFormatted(std::cerr);
        });

      zoteroItems(userID, [](const Error& error,int status,json::Value jsonValue) {
       if (error)
       {
          LOG_ERROR(error);
          return;
       }

       jsonValue.writeFormatted(std::cerr);
     });


      zoteroCollections(userID, [](const Error& error,int status,json::Value jsonValue) {
         if (error)
         {
            LOG_ERROR(error);
            return;
         }
         jsonValue.writeFormatted(std::cerr);

      });



   });
   */
}


void zoteroGetCollections(const json::JsonRpcRequest& request,
                          const json::JsonRpcFunctionContinuation& cont)
{
   // response object
   json::JsonRpcResponse response;

   // extract params
   std::string file;
   json::Array collectionsJson;
   Error error = json::readParams(request.params, &file, &collectionsJson);
   if (error)
   {
      json::setErrorResponse(error, &response);
      cont(Success(), &response);
      return;
   }

   // determine collections
   std::vector<std::string> collections;

   // determine whether the file the zotero collections are requested for is part of the current project
   bool isProjectFile = false;
   if (!file.empty() && projects::projectContext().hasProject())
   {
      FilePath filePath = module_context::resolveAliasedPath(file);
      if (filePath.isWithin(projects::projectContext().buildTargetPath()))
      {
         isProjectFile = true;
      }
   }

   if (collectionsJson.getSize() > 0)
   {
      std::transform(collectionsJson.begin(), collectionsJson.end(), std::back_inserter(collections), [](const json::Value& collection) {
         return collection.getObject()["name"].getString();
      });

   }
   else
   {
      collections = module_context::bookdownZoteroCollections();
   }

   // return empty array if no collections were requested
   if (collections.size() == 0)
   {
      json::JsonRpcResponse response;
      response.setResult(json::Array());
      cont(Success(), &response);
      return;
   }

   // get the requested collection(s)
   zoteroKeyInfo([cont, collections](const Error& error,int,json::Value jsonValue) {

      if (error)
      {
         continuationError(cont, error);
         return;
      }

      jsonValue.writeFormatted(std::cerr);

      int userID = jsonValue.getObject()["userID"].getInt();

      zoteroCollections(userID, [userID, cont, collections](const Error& error,int,json::Value jsonValue) {
         if (error)
         {
            continuationError(cont, error);
            return;
         }

           jsonValue.writeFormatted(std::cerr);


         // TODO: support multiple collections
         std::string targetCollection = collections[0];
         json::Array jsonCollections = jsonValue.getArray();
         bool foundCollection = false;
         for (std::size_t i = 0; i<jsonCollections.getSize(); i++)
         {
            json::Object collectionJson = jsonCollections[i].getObject()["data"].getObject();
            std::string name = collectionJson["name"].getString();
            if (name == targetCollection)
            {
               std::string key = collectionJson["key"].getString();
               int version = collectionJson["version"].getInt();
               zoteroItemsForCollection(userID, key, [name, version, cont](const Error& error,int,json::Value jsonValue) {

                  if (error)
                  {
                     continuationError(cont, error);
                     return;
                  }

                  jsonValue.writeFormatted(std::cerr);

                  // array of items
                  json::Array resultItemsJson = jsonValue.getArray();

                  // create response object
                  json::Object responseJson;
                  responseJson["name"] = name;
                  responseJson["version"] = version;
                  json::Array itemsJson;
                  std::transform(resultItemsJson.begin(), resultItemsJson.end(), std::back_inserter(itemsJson), [](const json::Value& resultItemJson) {
                     return resultItemJson.getObject()["data"];
                  });
                  responseJson["items"] = itemsJson;

                  // satisfy continutation
                  json::JsonRpcResponse response;
                  response.setResult(responseJson);
                  cont(Success(), &response);

               });

               foundCollection = true;
               break;
            }

         }

         // didn't find a target, so return empty array
         if (!foundCollection)
         {
            json::JsonRpcResponse response;
            response.setResult(json::Array());
            cont(Success(), &response);
         }
      });

   });

}


} // end anonymous namespace

Error initialize()
{
   module_context::events().onDeferredInit.connect(onDeferredInit);

   ExecBlock initBlock;
   initBlock.addFunctions()
       (boost::bind(module_context::registerAsyncRpcMethod, "zotero_get_collections", zoteroGetCollections))
   ;
   return initBlock.execute();
}

} // end namespace zotero
} // end namespace panmirror
} // end namespace modules
} // end namespace session
} // end namespace rstudio
