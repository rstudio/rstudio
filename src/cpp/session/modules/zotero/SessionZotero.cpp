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

#include "ZoteroCollections.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace zotero {

using namespace collections;

namespace {

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

   // collection specs
   ZoteroCollectionSpecs specs;
   std::transform(collections.begin(), collections.end(), std::back_inserter(specs), [](const std::string& collection) {
      return ZoteroCollectionSpec(collection);
   });

   // get the collections
   getCollections(specs, [cont](Error error, ZoteroCollections collections) {

      // response
      json::JsonRpcResponse response;

      if (!error)
      {
         json::Array collectionsJson;
         for (auto collection : collections)
         {
            json::Object collectionJson;
            collectionJson["name"] = collection.name;
            collectionJson["version"] = collection.version;
            collectionJson["items"] = collection.items;
            collectionsJson.push_back(collectionJson);
         }
         response.setResult(collectionsJson);
      }
      else
      {
         LOG_ERROR(error);
      }

      cont(error, &response);

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
