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
#include <session/projects/SessionProjects.hpp>

#include "ZoteroWebAPI.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace zotero {

namespace {

void continuationError(const json::JsonRpcFunctionContinuation& cont, const Error& error)
{
   json::JsonRpcResponse response;
   LOG_ERROR(error);
   cont(error, &response);
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
   web_api::zoteroKeyInfo([cont, collections](const Error& error,int,json::Value jsonValue) {

      if (error)
      {
         continuationError(cont, error);
         return;
      }

      int userID = jsonValue.getObject()["userID"].getInt();

      web_api::zoteroCollections(userID, [userID, cont, collections](const Error& error,int,json::Value jsonValue) {
         if (error)
         {
            continuationError(cont, error);
            return;
         }

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
               web_api::zoteroItemsForCollection(userID, key, [name, version, cont](const Error& error,int,json::Value jsonValue) {

                  if (error)
                  {
                     continuationError(cont, error);
                     return;
                  }

                  // array of items
                  json::Array resultItemsJson = jsonValue.getArray();

                  // create response object
                  json::Object zoteroCollectionJSON;
                  zoteroCollectionJSON["name"] = name;
                  zoteroCollectionJSON["version"] = version;
                  json::Array itemsJson;
                  std::transform(resultItemsJson.begin(), resultItemsJson.end(), std::back_inserter(itemsJson), [](const json::Value& resultItemJson) {
                     return resultItemJson.getObject()["csljson"];
                  });
                  zoteroCollectionJSON["items"] = itemsJson;

                  // satisfy continutation
                  json::JsonRpcResponse response;
                  json::Array responseJson;
                  responseJson.push_back(zoteroCollectionJSON);
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
   ExecBlock initBlock;
   initBlock.addFunctions()
       (boost::bind(module_context::registerAsyncRpcMethod, "zotero_get_collections", zoteroGetCollections))
   ;
   return initBlock.execute();
}

} // end namespace zotero
} // end namespace modules
} // end namespace session
} // end namespace rstudio
