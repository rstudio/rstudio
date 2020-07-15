/*
 * SessionZotero.cpp
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

#include "SessionZotero.hpp"

#include <shared_core/Error.hpp>
#include <shared_core/json/Json.hpp>

#include <core/Exec.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionAsyncDownloadFile.hpp>
#include <session/projects/SessionProjects.hpp>

#include "ZoteroCollections.hpp"
#include "ZoteroCollectionsWeb.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace zotero {

using namespace collections;

namespace {

const char * const kStatus = "status";
const char * const kMessage = "message";
const char * const kError = "error";

const char * const kStatusOK = "ok";
const char * const kStatusNoHost = "nohost";
const char * const kStatusNotFound = "notfound";
const char * const kStatusError = "error";


void zoteroValidateWebApiKey(const json::JsonRpcRequest& request,
                             const json::JsonRpcFunctionContinuation& cont)
{
   // extract params
   std::string key;
   Error error = json::readParams(request.params, &key);
   if (error)
   {
      json::JsonRpcResponse response;
      json::setErrorResponse(error, &response);
      cont(Success(), &response);
      return;
   }

   validateWebApiKey(key, [cont](bool valid) {
      json::JsonRpcResponse response;
      response.setResult(valid);
      cont(Success(), &response);
   });
}


void zoteroGetCollections(const json::JsonRpcRequest& request,
                          const json::JsonRpcFunctionContinuation& cont)
{
   // response object
   json::JsonRpcResponse response;

   // extract params
   std::string file;
   json::Array collectionsJson, cachedJson;
   Error error = json::readParams(request.params, &file, &collectionsJson, &cachedJson);
   if (error)
   {
      json::setErrorResponse(error, &response);
      cont(Success(), &response);
      return;
   }

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


   // determine collections to request
   ZoteroCollectionSpecs collections;

   // explicit request
   if (collectionsJson.getSize() > 0)
   {
      std::transform(collectionsJson.begin(), collectionsJson.end(), std::back_inserter(collections), [](const json::Value& json) {
         return ZoteroCollectionSpec(json.getString());
      });
   }
   // based on project
   else if (isProjectFile)
   {
      std::vector<std::string> bookdownCollections = module_context::bookdownZoteroCollections();
      std::transform(bookdownCollections.begin(),bookdownCollections.end(), std::back_inserter(collections), [](std::string collection) {
         return ZoteroCollectionSpec(collection);
      });
   }

   // return empty array if no collections were requested
   if (collections.size() == 0)
   {
      json::JsonRpcResponse response;
      response.setResult(json::Array());
      cont(Success(), &response);
      return;
   }


   // note versions already cached on the client
   for (ZoteroCollectionSpec& collection : collections)
   {
      std::string name = collection.name;
      auto it = std::find_if(cachedJson.begin(), cachedJson.end(), [name](json::Value json) {
         return json.getObject()["name"].getString() == name;
      });
      if (it != cachedJson.end())
      {
         collection.version = (*it).getObject()["version"].getInt();
      }
   }

   // get the collections
   getCollections(collections, [cont](Error error, ZoteroCollections collections) {

      // result defaults
      json::Object resultJson;
      resultJson[kMessage] = json::Value();
      resultJson[kError] = "";

      // handle success & error
      if (!error)
      {
         json::Array collectionsJson;
         for (auto collection : collections)
         {
            json::Object collectionJson;
            collectionJson[kName] = collection.name;
            collectionJson[kVersion] = collection.version;
            collectionJson[kItems] = collection.items;
            collectionsJson.push_back(collectionJson);
         }
         resultJson[kStatus] = kStatusOK;
         resultJson[kMessage] = collectionsJson;
      }
      else
      {
         std::string err = core::errorDescription(error);
         if (is404Error(err))
         {
            resultJson[kStatus] = kStatusNotFound;
         }
         else if (isHostError(err))
         {
            resultJson[kStatus] = kStatusNoHost;
         }
         else
         {
            LOG_ERROR_MESSAGE(err);
            resultJson[kStatus] = kStatusError;
         }
      }

      json::JsonRpcResponse response;
      response.setResult(resultJson);
      cont(Success(), &response);

   });

}


} // end anonymous namespace

Error initialize()
{
   ExecBlock initBlock;
   initBlock.addFunctions()
       (boost::bind(module_context::registerAsyncRpcMethod, "zotero_get_collections", zoteroGetCollections))
       (boost::bind(module_context::registerAsyncRpcMethod, "zotero_validate_web_api_key", zoteroValidateWebApiKey))
   ;
   return initBlock.execute();
}

} // end namespace zotero
} // end namespace modules
} // end namespace session
} // end namespace rstudio
