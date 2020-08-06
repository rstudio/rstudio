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
#include "ZoteroCollectionsLocal.hpp"
#include "ZoteroBetterBibTeX.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace zotero {

using namespace collections;

namespace {

const char * const kStatus = "status";
const char * const kMessage = "message";
const char * const kWarning = "warning";
const char * const kError = "error";

const char * const kStatusOK = "ok";
const char * const kStatusNoHost = "nohost";
const char * const kStatusNotFound = "notfound";
const char * const kStatusError = "error";

Error zoteroDetectLocalConfig(const json::JsonRpcRequest&,
                             json::JsonRpcResponse* pResponse)
{
   auto detectedConfig = collections::detectedLocalZoteroConfig();
   json::Object configJson;
   configJson["dataDirectory"] = !detectedConfig.dataDirectory.isEmpty()
      ?  module_context::createAliasedPath(detectedConfig.dataDirectory)
      : "";
   configJson["betterBibtex"] = detectedConfig.betterBibtex;
   pResponse->setResult(configJson);
   return Success();
}

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

void handleGetCollectionSpecs(Error error, ZoteroCollectionSpecs collectionSpecs, const json::JsonRpcFunctionContinuation& cont)
{
   // result defaults
   json::Object resultJson;
   resultJson[kMessage] = json::Value();
   resultJson[kError] = "";

   // handle success & error
   if (!error)
   {
      json::Array collectionSpecsJson;
      for (auto collectionSpec : collectionSpecs)
      {
         json::Object collectionSpecJson;
         collectionSpecJson[kName] = collectionSpec.name;
         collectionSpecJson[kVersion] = collectionSpec.version;
         collectionSpecJson[kKey] = collectionSpec.key;
         collectionSpecJson[kParentKey] = collectionSpec.parentKey;
         collectionSpecsJson.push_back(collectionSpecJson);
      }
      resultJson[kStatus] = kStatusOK;
      resultJson[kMessage] = collectionSpecsJson;
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

}

void handleGetCollections(Error error, ZoteroCollections collections, std::string warning, const json::JsonRpcFunctionContinuation& cont)
{
   // result defaults
   json::Object resultJson;
   resultJson[kMessage] = json::Value();
   resultJson[kWarning] = "";
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
         collectionJson[kKey] = collection.key;
         collectionJson[kParentKey] = collection.parentKey;
         collectionJson[kItems] = collection.items;
         collectionsJson.push_back(collectionJson);
      }
      resultJson[kStatus] = kStatusOK;
      resultJson[kWarning] = warning;
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
}

void zoteroGetCollectionSpecs(const json::JsonRpcRequest& request,
                              const json::JsonRpcFunctionContinuation& cont)
{
   json::JsonRpcResponse response;
   auto handler =  boost::bind(handleGetCollectionSpecs, _1, _2, cont);

   getCollectionSpecs(handler);
}

void zoteroGetCollections(const json::JsonRpcRequest& request,
                          const json::JsonRpcFunctionContinuation& cont)
{
   // response object
   json::JsonRpcResponse response;

   // extract params
   std::string file;
   json::Value collectionsJsonValue;
   json::Array collectionsJson, cachedJson;
   bool useCache;
   Error error = json::readParams(request.params, &file, &collectionsJsonValue, &cachedJson, &useCache);
   if (error)
   {
      json::setErrorResponse(error, &response);
      cont(Success(), &response);
      return;
   }

   // determine whether this is a request for all collections.
   bool allCollections = collectionsJsonValue.isNull();

   // if it isn't then cast to collectionsJson
   if (!allCollections)
      collectionsJson = collectionsJsonValue.getArray();

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
   std::vector<std::string> collections;

   // explicit request
   if (collectionsJson.getSize() > 0)
   {
      collectionsJson.toVectorString(collections);
   }
   // based on project
   else if (!allCollections && isProjectFile)
   {
      // look for special logical values
      const std::string kLogicalPrefix = "LOGICAL:";
      std::vector<std::string> bookdownCollections = module_context::bookdownZoteroCollections();
      if (bookdownCollections.size() == 1 && boost::algorithm::starts_with(bookdownCollections[0], kLogicalPrefix))
      {
         std::string value = bookdownCollections[0].substr(kLogicalPrefix.length());
         allCollections = value == "TRUE";
      }
      else
      {
         collections = bookdownCollections;
      }
   }

   // return empty array if no collections were requested and we aren't getting allCollections
   if (collections.size() == 0 && !allCollections)
   {
      ZoteroCollections noCollections;
      handleGetCollections(Success(), noCollections, "", cont);
      return;
   }

   // extract client cache specs
   ZoteroCollectionSpecs cacheSpecs;
   std::transform(cachedJson.begin(), cachedJson.end(), std::back_inserter(cacheSpecs), [](const json::Value json) {
      auto jsonSpec = json.getObject();
      ZoteroCollectionSpec cacheSpec;
      cacheSpec.name = jsonSpec[kName].getString();
      cacheSpec.version = jsonSpec[kVersion].getInt();
      cacheSpec.key = jsonSpec[kKey].getString();
      cacheSpec.parentKey = jsonSpec[kParentKey].getString();
      return cacheSpec;
   });

   // create handler for request
   auto handler =  boost::bind(handleGetCollections, _1, _2, _3, cont);

   // allCollections is a request for the library
   if (allCollections)
   {
      // we just need the kMyLibrary cache spec
      ZoteroCollectionSpec librarySpec(kMyLibrary);
      ZoteroCollectionSpecs::iterator it = std::find_if(cacheSpecs.begin(), cacheSpecs.end(), [](const ZoteroCollectionSpec& spec) {
         return spec.name == kMyLibrary;
      });
      if (it != cacheSpecs.end())
         librarySpec = *it;

      // get the library
      getLibrary(librarySpec, useCache, handler);

   }

   // otherwise get the requested collections
   else
   {
      getCollections(collections, cacheSpecs, useCache, handler);
   }
}


} // end anonymous namespace


Error initialize()
{
   using namespace module_context;
   ExecBlock initBlock;
   initBlock.addFunctions()
       (boost::bind(registerAsyncRpcMethod, "zotero_get_collections", zoteroGetCollections))
       (boost::bind(registerAsyncRpcMethod, "zotero_get_collection_specs", zoteroGetCollectionSpecs))
       (boost::bind(registerAsyncRpcMethod, "zotero_validate_web_api_key", zoteroValidateWebApiKey))
       (boost::bind(registerRpcMethod, "zotero_detect_local_config", zoteroDetectLocalConfig))
       (boost::bind(registerRpcMethod, "zotero_better_bibtex_export", betterBibtexExport))
   ;
   return initBlock.execute();
}

} // end namespace zotero
} // end namespace modules
} // end namespace session
} // end namespace rstudio
